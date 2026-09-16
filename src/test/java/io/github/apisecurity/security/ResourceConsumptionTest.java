package io.github.apisecurity.security;

import io.github.apisecurity.client.AuthClient;
import io.github.apisecurity.client.UserClient;
import io.github.apisecurity.data.UserFactory;
import io.github.apisecurity.model.JwtResponse;
import io.github.apisecurity.model.LoginRequest;
import io.github.apisecurity.model.SignupRequest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("security")
@Tag("resource-abuse")
class ResourceConsumptionTest {

    private static final int TOTAL_REQUEST_BUDGET = 10;
    private static final int SETUP_REQUESTS = 2;
    private static final int DASHBOARD_REQUESTS = TOTAL_REQUEST_BUDGET - SETUP_REQUESTS;
    private static final long WINDOW_MILLIS = 10_000;
    private static final long MAX_REQUEST_LATENCY_MILLIS = 2_000;

    private final AuthClient authClient = new AuthClient();
    private final UserClient userClient = new UserClient();

    @Test
    void shouldRemainStableWithinAuthorizedLowVolumeBudget() {
        long startedAt = System.nanoTime();
        List<RequestMetric> metrics = new ArrayList<>(TOTAL_REQUEST_BUDGET);

        try {
            SignupRequest user = UserFactory.uniqueUser();

            Response signupResponse = authClient.signup(user);
            assertWithinAuthorizedLimits(signupResponse, 1, "signup", startedAt, metrics);
            signupResponse.then()
                    .statusCode(200)
                    .contentType(ContentType.JSON);

            Response loginResponse = authClient.login(LoginRequest.from(user));
            assertWithinAuthorizedLimits(loginResponse, 2, "login", startedAt, metrics);
            loginResponse.then()
                    .statusCode(200)
                    .contentType(ContentType.JSON);

            JwtResponse jwt = loginResponse.as(JwtResponse.class);
            assertNotNull(jwt.token(), "A resposta de login deve conter um token");
            assertFalse(jwt.token().isBlank(), "O token de login não pode estar vazio");

            for (int dashboardRequest = 1; dashboardRequest <= DASHBOARD_REQUESTS; dashboardRequest++) {
                int requestNumber = dashboardRequest + SETUP_REQUESTS;
                Response dashboardResponse = userClient.dashboard(jwt.token());
                assertWithinAuthorizedLimits(
                        dashboardResponse,
                        requestNumber,
                        "dashboard",
                        startedAt,
                        metrics
                );
                dashboardResponse.then()
                        .statusCode(200)
                        .contentType(ContentType.JSON);
            }
        } finally {
            printMetrics(metrics);
        }
    }

    private void assertWithinAuthorizedLimits(
            Response response,
            int requestNumber,
            String operation,
            long startedAt,
            List<RequestMetric> metrics
    ) {
        int statusCode = response.statusCode();
        long requestLatency = response.timeIn(TimeUnit.MILLISECONDS);
        long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
        String step = operation + " (requisição " + requestNumber + "/" + TOTAL_REQUEST_BUDGET + ")";

        metrics.add(new RequestMetric(requestNumber, operation, statusCode, requestLatency, elapsed));

        assertTrue(statusCode < 500, () -> step + " retornou HTTP " + statusCode);
        assertTrue(
                requestLatency <= MAX_REQUEST_LATENCY_MILLIS,
                () -> step + " excedeu a latência máxima: " + requestLatency + " ms"
        );
        assertTrue(
                elapsed <= WINDOW_MILLIS,
                () -> "A execução excedeu a janela autorizada: " + elapsed + " ms"
        );
    }

    private void printMetrics(List<RequestMetric> metrics) {
        metrics.forEach(metric -> System.out.printf(
                "METRICA resource-abuse requisicao=%d/%d operacao=%s status=%d latencia_ms=%d decorrido_ms=%d%n",
                metric.requestNumber(),
                TOTAL_REQUEST_BUDGET,
                metric.operation(),
                metric.statusCode(),
                metric.latencyMillis(),
                metric.elapsedMillis()
        ));
    }

    private record RequestMetric(
            int requestNumber,
            String operation,
            int statusCode,
            long latencyMillis,
            long elapsedMillis
    ) {
    }
}
