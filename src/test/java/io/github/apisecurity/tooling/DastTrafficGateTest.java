package io.github.apisecurity.tooling;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

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
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("unit")
class DastTrafficGateTest {

    private final HttpClient client = HttpClient.newHttpClient();
    private DastTrafficGate gate;
    private HttpServer upstream;
    private Path statusFile;

    @AfterEach
    void tearDown() throws IOException {
        if (gate != null) {
            gate.close();
        }
        if (upstream != null) {
            upstream.stop(0);
        }
        if (statusFile != null) {
            Files.deleteIfExists(statusFile);
        }
    }

    @Test
    void shouldForwardOnlyAllowedOperationAndPreserveResponse() throws Exception {
        AtomicInteger upstreamCalls = startUpstream(200, "{\"result\":\"ok\"}");
        startGate(3, Duration.ofSeconds(5));

        HttpResponse<String> response = sendToGateway("POST", "/identity/api/auth/signup", "{\"name\":\"test\"}", false);

        assertEquals(200, response.statusCode());
        assertEquals("{\"result\":\"ok\"}", response.body());
        assertEquals(1, upstreamCalls.get());
        assertEquals(1, gate.requestCount());
    }

    @Test
    void shouldStopBeforeForwardingRequestBeyondBudget() throws Exception {
        AtomicInteger upstreamCalls = startUpstream(200, "{}");
        startGate(1, Duration.ofSeconds(5));

        assertEquals(200, sendToGateway("GET", "/identity/api/v2/user/dashboard", "", false).statusCode());
        assertEquals(429, sendToGateway("GET", "/identity/api/v2/user/dashboard", "", false).statusCode());

        assertEquals(
                DastTrafficGate.StopReason.REQUEST_BUDGET_EXCEEDED,
                gate.awaitTermination(Duration.ofSeconds(1))
        );
        assertEquals(1, upstreamCalls.get());
        assertEquals(2, gate.requestCount());
    }

    @Test
    void shouldStopOnOperationOutsideScope() throws Exception {
        AtomicInteger upstreamCalls = startUpstream(200, "{}");
        startGate(3, Duration.ofSeconds(5));

        HttpResponse<String> response = sendToGateway("GET", "/identity/api/auth/signup", "", false);

        assertEquals(403, response.statusCode());
        assertEquals(DastTrafficGate.StopReason.OUT_OF_SCOPE_REQUEST, gate.awaitTermination(Duration.ofSeconds(1)));
        assertEquals(0, upstreamCalls.get());
    }

    @Test
    void shouldBlockAuthorizationHeaderWithoutRecordingItsValue() throws Exception {
        AtomicInteger upstreamCalls = startUpstream(200, "{}");
        startGate(3, Duration.ofSeconds(5));

        HttpResponse<String> response = sendToGateway(
                "GET",
                "/identity/api/v2/user/dashboard",
                "",
                true
        );

        assertEquals(403, response.statusCode());
        assertEquals(
                DastTrafficGate.StopReason.AUTHORIZATION_HEADER_BLOCKED,
                gate.awaitTermination(Duration.ofSeconds(1))
        );
        assertEquals(0, upstreamCalls.get());
        assertFalse(Files.readString(statusFile).contains("token-de-teste"));
    }

    @Test
    void shouldStopWhenUpstreamReturnsServerError() throws Exception {
        startUpstream(503, "{}");
        startGate(3, Duration.ofSeconds(5));

        HttpResponse<String> response = sendToGateway("POST", "/identity/api/auth/login", "{}", false);

        assertEquals(503, response.statusCode());
        assertEquals(
                DastTrafficGate.StopReason.UPSTREAM_SERVER_ERROR,
                gate.awaitTermination(Duration.ofSeconds(1))
        );
    }

    @Test
    void shouldStopWhenTimeLimitExpires() throws Exception {
        startUpstream(200, "{}");
        startGate(3, Duration.ofMillis(100));

        assertEquals(
                DastTrafficGate.StopReason.TIME_LIMIT_EXCEEDED,
                gate.awaitTermination(Duration.ofSeconds(2))
        );
        assertEquals(0, gate.requestCount());
    }

    @Test
    void shouldExposeLoopbackControlAndAcceptCompletion() throws Exception {
        startUpstream(200, "{}");
        startGate(3, Duration.ofSeconds(5));

        URI statusUri = URI.create("http://127.0.0.1:" + gate.controlPort() + "/status");
        HttpResponse<String> statusResponse = client.send(
                HttpRequest.newBuilder(statusUri).GET().build(),
                HttpResponse.BodyHandlers.ofString()
        );
        HttpResponse<String> completionResponse = client.send(
                HttpRequest.newBuilder(statusUri.resolve("/complete"))
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );

        assertEquals(200, statusResponse.statusCode());
        assertTrue(statusResponse.body().contains("\"state\":\"RUNNING\""));
        assertEquals(200, completionResponse.statusCode());
        assertEquals(DastTrafficGate.StopReason.COMPLETED, gate.awaitTermination(Duration.ofSeconds(1)));
    }

    @Test
    void shouldRejectUnsafeConfiguration() throws Exception {
        Path validStatusFile = Path.of("target", "zap", "gate-validation.json");
        InetAddress loopback = InetAddress.getLoopbackAddress();
        URI validUpstream = URI.create("http://localhost:8888");

        assertThrows(IllegalArgumentException.class, () -> new DastTrafficGate.Config(
                URI.create("ftp://localhost:8888"), loopback, 18080, 18081, 1, Duration.ofSeconds(1), validStatusFile
        ));
        assertThrows(IllegalArgumentException.class, () -> new DastTrafficGate.Config(
                URI.create("http://user:secret@localhost:8888/api"),
                loopback, 18080, 18081, 1, Duration.ofSeconds(1), validStatusFile
        ));
        assertThrows(IllegalArgumentException.class, () -> new DastTrafficGate.Config(
                validUpstream, loopback, 18080, 18080, 1, Duration.ofSeconds(1), validStatusFile
        ));
        assertThrows(IllegalArgumentException.class, () -> new DastTrafficGate.Config(
                validUpstream, loopback, 18080, 18081, 0, Duration.ofSeconds(1), validStatusFile
        ));
        assertThrows(IllegalArgumentException.class, () -> new DastTrafficGate.Config(
                validUpstream, loopback, 18080, 18081, 1, Duration.ofSeconds(121), validStatusFile
        ));
        assertThrows(IllegalArgumentException.class, () -> new DastTrafficGate.Config(
                validUpstream, loopback, 18080, 18081, 1, Duration.ofSeconds(1), Path.of("gate-status.json")
        ));
    }

    private AtomicInteger startUpstream(int statusCode, String responseBody) throws IOException {
        AtomicInteger calls = new AtomicInteger();
        upstream = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        upstream.createContext("/", exchange -> respond(exchange, statusCode, responseBody, calls));
        upstream.start();
        return calls;
    }

    private void startGate(int maxRequests, Duration duration) throws IOException {
        statusFile = Path.of("target", "zap", "gate-test-" + UUID.randomUUID() + ".json");
        DastTrafficGate.Config config = new DastTrafficGate.Config(
                URI.create("http://127.0.0.1:" + upstream.getAddress().getPort()),
                InetAddress.getLoopbackAddress(),
                0,
                0,
                maxRequests,
                duration,
                statusFile
        );
        gate = new DastTrafficGate(config);
        gate.start();
    }

    private HttpResponse<String> sendToGateway(String method, String path, String body, boolean withAuthorization)
            throws IOException, InterruptedException {
        HttpRequest.Builder request = HttpRequest.newBuilder(
                        URI.create("http://127.0.0.1:" + gate.gatewayPort() + path)
                )
                .header("Content-Type", "application/json")
                .method(method, HttpRequest.BodyPublishers.ofString(body));
        if (withAuthorization) {
            request.header("Authorization", "Bearer token-de-teste");
        }
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private void respond(HttpExchange exchange, int statusCode, String body, AtomicInteger calls) throws IOException {
        calls.incrementAndGet();
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
