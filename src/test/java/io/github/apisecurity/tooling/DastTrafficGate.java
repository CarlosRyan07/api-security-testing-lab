package io.github.apisecurity.tooling;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public final class DastTrafficGate implements AutoCloseable {

    private static final int MAX_BODY_BYTES = 65_536;
    private static final Duration MAX_ALLOWED_DURATION = Duration.ofMinutes(2);
    private static final Map<String, String> ALLOWED_OPERATIONS = Map.of(
            "/identity/api/auth/signup", "POST",
            "/identity/api/auth/login", "POST",
            "/identity/api/v2/user/dashboard", "GET"
    );

    private final Config config;
    private final HttpServer gatewayServer;
    private final HttpServer controlServer;
    private final HttpClient httpClient;
    private final ExecutorService gatewayExecutor;
    private final ExecutorService controlExecutor;
    private final ScheduledExecutorService timerExecutor;
    private final AtomicInteger requestCount = new AtomicInteger();
    private final AtomicReference<StopReason> stopReason = new AtomicReference<>(StopReason.RUNNING);
    private final CountDownLatch stopped = new CountDownLatch(1);

    public DastTrafficGate(Config config) throws IOException {
        this.config = Objects.requireNonNull(config, "config");
        this.gatewayServer = HttpServer.create(
                new InetSocketAddress(config.listenAddress(), config.listenPort()),
                0
        );
        this.controlServer = HttpServer.create(
                new InetSocketAddress(InetAddress.getLoopbackAddress(), config.controlPort()),
                0
        );
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        this.gatewayExecutor = Executors.newSingleThreadExecutor();
        this.controlExecutor = Executors.newSingleThreadExecutor();
        this.timerExecutor = Executors.newSingleThreadScheduledExecutor();

        gatewayServer.createContext("/", this::handleGatewayRequest);
        gatewayServer.setExecutor(gatewayExecutor);
        controlServer.createContext("/status", this::handleStatusRequest);
        controlServer.createContext("/complete", this::handleCompleteRequest);
        controlServer.setExecutor(controlExecutor);
    }

    public void start() throws IOException {
        writeStatus();
        gatewayServer.start();
        controlServer.start();
        timerExecutor.schedule(
                () -> requestStop(StopReason.TIME_LIMIT_EXCEEDED),
                config.maxDuration().toMillis(),
                TimeUnit.MILLISECONDS
        );
    }

    public StopReason awaitTermination() throws InterruptedException {
        stopped.await();
        return stopReason.get();
    }

    StopReason awaitTermination(Duration timeout) throws InterruptedException {
        if (!stopped.await(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
            throw new IllegalStateException("O gateway não encerrou dentro do tempo esperado");
        }
        return stopReason.get();
    }

    public void complete() {
        requestStop(StopReason.COMPLETED);
    }

    public int gatewayPort() {
        return gatewayServer.getAddress().getPort();
    }

    public int controlPort() {
        return controlServer.getAddress().getPort();
    }

    int requestCount() {
        return requestCount.get();
    }

    StopReason stopReason() {
        return stopReason.get();
    }

    private void handleGatewayRequest(HttpExchange exchange) throws IOException {
        int currentRequest = requestCount.incrementAndGet();
        writeStatus();

        if (currentRequest > config.maxRequests()) {
            sendJson(exchange, 429, "{\"error\":\"request budget exceeded\"}");
            requestStop(StopReason.REQUEST_BUDGET_EXCEEDED);
            return;
        }

        String path = exchange.getRequestURI().getPath();
        String expectedMethod = ALLOWED_OPERATIONS.get(path);
        boolean hasQuery = exchange.getRequestURI().getRawQuery() != null;
        if (expectedMethod == null || !expectedMethod.equalsIgnoreCase(exchange.getRequestMethod()) || hasQuery) {
            sendJson(exchange, 403, "{\"error\":\"operation outside allowed scope\"}");
            requestStop(StopReason.OUT_OF_SCOPE_REQUEST);
            return;
        }

        if (exchange.getRequestHeaders().containsKey("Authorization")) {
            sendJson(exchange, 403, "{\"error\":\"authorization header is not allowed\"}");
            requestStop(StopReason.AUTHORIZATION_HEADER_BLOCKED);
            return;
        }

        byte[] requestBody = exchange.getRequestBody().readNBytes(MAX_BODY_BYTES + 1);
        if (requestBody.length > MAX_BODY_BYTES) {
            sendJson(exchange, 413, "{\"error\":\"request body exceeds gate limit\"}");
            requestStop(StopReason.OUT_OF_SCOPE_REQUEST);
            return;
        }

        HttpRequest.Builder upstreamRequest = HttpRequest.newBuilder(config.upstreamBaseUrl().resolve(path))
                .timeout(Duration.ofSeconds(10))
                .method(exchange.getRequestMethod(), HttpRequest.BodyPublishers.ofByteArray(requestBody));
        copyHeader(exchange, upstreamRequest, "Accept");
        copyHeader(exchange, upstreamRequest, "Content-Type");

        try {
            HttpResponse<byte[]> upstreamResponse = httpClient.send(
                    upstreamRequest.build(),
                    HttpResponse.BodyHandlers.ofByteArray()
            );
            upstreamResponse.headers().firstValue("Content-Type")
                    .ifPresent(value -> exchange.getResponseHeaders().set("Content-Type", value));
            send(exchange, upstreamResponse.statusCode(), upstreamResponse.body());

            if (upstreamResponse.statusCode() >= 500) {
                requestStop(StopReason.UPSTREAM_SERVER_ERROR);
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            sendJson(exchange, 502, "{\"error\":\"upstream unavailable\"}");
            requestStop(StopReason.UPSTREAM_UNAVAILABLE);
        } catch (IOException exception) {
            sendJson(exchange, 502, "{\"error\":\"upstream unavailable\"}");
            requestStop(StopReason.UPSTREAM_UNAVAILABLE);
        }
    }

    private void handleStatusRequest(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 405, "{\"error\":\"method not allowed\"}");
            return;
        }
        sendJson(exchange, 200, statusJson());
    }

    private void handleCompleteRequest(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 405, "{\"error\":\"method not allowed\"}");
            return;
        }
        sendJson(exchange, 200, "{\"status\":\"completion accepted\"}");
        requestStop(StopReason.COMPLETED);
    }

    private void copyHeader(HttpExchange exchange, HttpRequest.Builder target, String headerName) {
        String value = exchange.getRequestHeaders().getFirst(headerName);
        if (value != null && !value.isBlank()) {
            target.header(headerName, value);
        }
    }

    private void requestStop(StopReason reason) {
        if (stopReason.compareAndSet(StopReason.RUNNING, reason)) {
            try {
                writeStatus();
            } catch (IOException ignored) {
                // O motivo original de interrupção deve ser preservado.
            }
            stopped.countDown();
        }
    }

    private synchronized void writeStatus() throws IOException {
        Files.createDirectories(config.statusFile().getParent());
        Files.writeString(
                config.statusFile(),
                statusJson(),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
        );
    }

    private String statusJson() {
        return String.format(
                Locale.ROOT,
                "{\"state\":\"%s\",\"requests\":%d,\"maxRequests\":%d}%n",
                stopReason.get(),
                requestCount.get(),
                config.maxRequests()
        );
    }

    private void sendJson(HttpExchange exchange, int statusCode, String body) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        send(exchange, statusCode, body.getBytes(StandardCharsets.UTF_8));
    }

    private void send(HttpExchange exchange, int statusCode, byte[] body) throws IOException {
        exchange.sendResponseHeaders(statusCode, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }

    @Override
    public void close() {
        gatewayServer.stop(0);
        controlServer.stop(0);
        timerExecutor.shutdownNow();
        gatewayExecutor.shutdownNow();
        controlExecutor.shutdownNow();
    }

    public static void main(String[] args) throws Exception {
        Map<String, String> arguments = parseArguments(args);
        Config config = new Config(
                URI.create(requiredArgument(arguments, "upstream")),
                InetAddress.getByName(arguments.getOrDefault("listen-address", "0.0.0.0")),
                Integer.parseInt(requiredArgument(arguments, "listen-port")),
                Integer.parseInt(requiredArgument(arguments, "control-port")),
                Integer.parseInt(requiredArgument(arguments, "max-requests")),
                Duration.ofSeconds(Long.parseLong(requiredArgument(arguments, "max-seconds"))),
                Path.of(requiredArgument(arguments, "status-file"))
        );

        StopReason reason;
        try (DastTrafficGate gate = new DastTrafficGate(config)) {
            gate.start();
            System.out.printf(
                    "Gateway DAST iniciado com limite de %d requests por %d segundos.%n",
                    config.maxRequests(),
                    config.maxDuration().toSeconds()
            );
            reason = gate.awaitTermination();
            System.out.printf("Gateway DAST encerrado: %s (%d/%d requests).%n",
                    reason,
                    gate.requestCount(),
                    config.maxRequests());
        }
        System.exit(reason.exitCode());
    }

    private static Map<String, String> parseArguments(String[] args) {
        Map<String, String> arguments = new HashMap<>();
        for (String argument : args) {
            if (!argument.startsWith("--") || !argument.contains("=")) {
                throw new IllegalArgumentException("Argumento inválido: " + argument);
            }
            int separator = argument.indexOf('=');
            String key = argument.substring(2, separator);
            String value = argument.substring(separator + 1);
            if (key.isBlank() || value.isBlank() || arguments.putIfAbsent(key, value) != null) {
                throw new IllegalArgumentException("Argumento inválido ou duplicado: " + argument);
            }
        }
        return arguments;
    }

    private static String requiredArgument(Map<String, String> arguments, String name) {
        String value = arguments.get(name);
        if (value == null) {
            throw new IllegalArgumentException("Argumento obrigatório ausente: --" + name);
        }
        return value;
    }

    public record Config(
            URI upstreamBaseUrl,
            InetAddress listenAddress,
            int listenPort,
            int controlPort,
            int maxRequests,
            Duration maxDuration,
            Path statusFile
    ) {
        public Config {
            Objects.requireNonNull(upstreamBaseUrl, "upstreamBaseUrl");
            Objects.requireNonNull(listenAddress, "listenAddress");
            Objects.requireNonNull(maxDuration, "maxDuration");
            Objects.requireNonNull(statusFile, "statusFile");

            String scheme = upstreamBaseUrl.getScheme();
            if (upstreamBaseUrl.getHost() == null
                    || !("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))) {
                throw new IllegalArgumentException("upstreamBaseUrl deve ser HTTP ou HTTPS");
            }
            String upstreamPath = upstreamBaseUrl.getPath();
            if (upstreamBaseUrl.getUserInfo() != null
                    || upstreamBaseUrl.getQuery() != null
                    || upstreamBaseUrl.getFragment() != null
                    || !(upstreamPath == null || upstreamPath.isBlank() || "/".equals(upstreamPath))) {
                throw new IllegalArgumentException("upstreamBaseUrl deve apontar para a raiz e não conter credenciais");
            }
            if (listenPort < 0 || listenPort > 65_535 || controlPort < 0 || controlPort > 65_535) {
                throw new IllegalArgumentException("Portas devem estar entre 0 e 65535");
            }
            if (listenPort != 0 && listenPort == controlPort) {
                throw new IllegalArgumentException("As portas do gateway e de controle devem ser diferentes");
            }
            if (maxRequests < 1) {
                throw new IllegalArgumentException("maxRequests deve ser positivo");
            }
            if (maxDuration.isZero() || maxDuration.isNegative()
                    || maxDuration.compareTo(MAX_ALLOWED_DURATION) > 0) {
                throw new IllegalArgumentException("maxDuration deve estar entre 1 ms e 2 minutos");
            }

            Path normalizedStatus = statusFile.toAbsolutePath().normalize();
            Path allowedDirectory = Path.of("target", "zap").toAbsolutePath().normalize();
            if (!normalizedStatus.startsWith(allowedDirectory)) {
                throw new IllegalArgumentException("statusFile deve permanecer em target/zap");
            }
            statusFile = normalizedStatus;
        }
    }

    public enum StopReason {
        RUNNING(19),
        COMPLETED(0),
        REQUEST_BUDGET_EXCEEDED(20),
        OUT_OF_SCOPE_REQUEST(21),
        AUTHORIZATION_HEADER_BLOCKED(22),
        UPSTREAM_SERVER_ERROR(23),
        UPSTREAM_UNAVAILABLE(24),
        TIME_LIMIT_EXCEEDED(25);

        private final int exitCode;

        StopReason(int exitCode) {
            this.exitCode = exitCode;
        }

        public int exitCode() {
            return exitCode;
        }
    }
}
