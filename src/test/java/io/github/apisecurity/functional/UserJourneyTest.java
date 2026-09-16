package io.github.apisecurity.functional;

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

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("functional")
class UserJourneyTest {

    private final AuthClient authClient = new AuthClient();
    private final UserClient userClient = new UserClient();

    @Test
    void shouldRegisterUniqueUser() {
        SignupRequest user = UserFactory.uniqueUser();

        authClient.signup(user)
                .then()
                .statusCode(200);
    }

    @Test
    void shouldLoginRegisteredUserAndReturnJwt() {
        SignupRequest user = UserFactory.uniqueUser();
        register(user);

        JwtResponse jwt = login(user);

        assertNotNull(jwt.token(), "Login response must include a token");
        assertFalse(jwt.token().isBlank(), "Login response token must not be blank");
    }

    @Test
    void shouldAccessOwnDashboardWithFreshJwt() {
        SignupRequest user = UserFactory.uniqueUser();
        register(user);
        JwtResponse jwt = login(user);

        Response dashboardResponse = userClient.dashboard(jwt.token());

        dashboardResponse.then()
                .statusCode(200)
                .contentType(ContentType.JSON);
        Map<String, Object> dashboard = dashboardResponse.jsonPath().getMap("$");

        assertAll(
                () -> assertEquals(user.email(), dashboard.get("email")),
                () -> assertEquals(user.name(), dashboard.get("name")),
                () -> assertEquals(user.number(), dashboard.get("number")),
                () -> assertEquals("ROLE_USER", dashboard.get("role")),
                () -> assertInstanceOf(Number.class, dashboard.get("id")),
                () -> assertInstanceOf(Number.class, dashboard.get("available_credit")),
                () -> assertInstanceOf(Number.class, dashboard.get("video_id")),
                () -> assertTrue(dashboard.containsKey("picture_url"), "Dashboard must include picture_url"),
                () -> assertTrue(dashboard.containsKey("video_name"), "Dashboard must include video_name"),
                () -> assertTrue(dashboard.containsKey("video_url"), "Dashboard must include video_url")
        );
    }

    private void register(SignupRequest user) {
        authClient.signup(user)
                .then()
                .statusCode(200);
    }

    private JwtResponse login(SignupRequest user) {
        Response response = authClient.login(LoginRequest.from(user));
        response.then().statusCode(200);
        return response.as(JwtResponse.class);
    }
}
