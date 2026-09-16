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
        SignupRequest user = UserFactory.uniqueUser();

        Response signupResponse = authClient.signup(user);
        assertWithinAuthorizedLimits(signupResponse, "signup (requisição 1/10)", startedAt);
        signupResponse.then()
                .statusCode(200)
                .contentType(ContentType.JSON);

        Response loginResponse = authClient.login(LoginRequest.from(user));
        assertWithinAuthorizedLimits(loginResponse, "login (requisição 2/10)", startedAt);
        loginResponse.then()
                .statusCode(200)
                .contentType(ContentType.JSON);

        JwtResponse jwt = loginResponse.as(JwtResponse.class);
        assertNotNull(jwt.token(), "A resposta de login deve conter um token");
        assertFalse(jwt.token().isBlank(), "O token de login não pode estar vazio");

        for (int requestNumber = 1; requestNumber <= DASHBOARD_REQUESTS; requestNumber++) {
            Response dashboardResponse = userClient.dashboard(jwt.token());
            assertWithinAuthorizedLimits(
                    dashboardResponse,
                    "dashboard (requisição " + (requestNumber + SETUP_REQUESTS) + "/10)",
                    startedAt
            );
            dashboardResponse.then()
                    .statusCode(200)
                    .contentType(ContentType.JSON);
        }
    }

    private void assertWithinAuthorizedLimits(Response response, String operation, long startedAt) {
        int statusCode = response.statusCode();
        long requestLatency = response.timeIn(TimeUnit.MILLISECONDS);
        long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);

        assertTrue(statusCode < 500, () -> operation + " retornou HTTP " + statusCode);
        assertTrue(
                requestLatency <= MAX_REQUEST_LATENCY_MILLIS,
                () -> operation + " excedeu a latência máxima: " + requestLatency + " ms"
        );
        assertTrue(
                elapsed <= WINDOW_MILLIS,
                () -> "A execução excedeu a janela autorizada: " + elapsed + " ms"
        );
    }
}
