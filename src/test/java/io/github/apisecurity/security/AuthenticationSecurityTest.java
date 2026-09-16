package io.github.apisecurity.security;

import io.github.apisecurity.client.AuthClient;
import io.github.apisecurity.client.UserClient;
import io.github.apisecurity.data.UserFactory;
import io.github.apisecurity.model.LoginRequest;
import io.github.apisecurity.model.SignupRequest;
import io.restassured.response.Response;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNull;

@Tag("security")
@Tag("authentication")
class AuthenticationSecurityTest {

    private final AuthClient authClient = new AuthClient();
    private final UserClient userClient = new UserClient();

    @Test
    void shouldRejectUnknownCredentials() {
        SignupRequest unknownUser = UserFactory.uniqueUser();

        Response response = authClient.login(new LoginRequest(unknownUser.email(), unknownUser.password()));

        assertRejectedLogin(response);
    }

    @Test
    void shouldRejectIncorrectPasswordForRegisteredUser() {
        SignupRequest user = UserFactory.uniqueUser();
        authClient.signup(user).then().statusCode(200);

        Response response = authClient.login(new LoginRequest(user.email(), user.password() + "-incorrect"));

        assertRejectedLogin(response);
    }

    @Test
    void shouldRejectDashboardWithoutToken() {
        Response response = userClient.dashboardWithoutAuthentication();

        assertRejectedDashboard(response);
    }

    @Test
    void shouldRejectDashboardWithMalformedToken() {
        Response response = userClient.dashboard("not-a-jwt");

        assertRejectedDashboard(response);
    }

    private void assertRejectedLogin(Response response) {
        response.then().statusCode(401);
        assertNull(response.jsonPath().getString("token"), "Rejected login must not return a token");
    }

    private void assertRejectedDashboard(Response response) {
        response.then().statusCode(404);
        assertAll(
                () -> assertNull(response.jsonPath().getString("email")),
                () -> assertNull(response.jsonPath().getString("name")),
                () -> assertNull(response.jsonPath().getString("number")),
                () -> assertNull(response.jsonPath().getString("role"))
        );
    }
}
