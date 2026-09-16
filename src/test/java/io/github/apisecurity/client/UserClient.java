package io.github.apisecurity.client;

import io.github.apisecurity.config.TestConfig;
import io.restassured.response.Response;

import static io.restassured.RestAssured.given;

public final class UserClient {

    private static final String DASHBOARD_PATH = "/identity/api/v2/user/dashboard";

    public Response dashboard(String token) {
        return given()
                .spec(TestConfig.requestSpecification())
                .auth()
                .oauth2(token)
                .when()
                .get(DASHBOARD_PATH);
    }

    public Response dashboardWithoutAuthentication() {
        return given()
                .spec(TestConfig.requestSpecification())
                .when()
                .get(DASHBOARD_PATH);
    }
}
