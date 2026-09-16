package io.github.apisecurity.client;

import io.github.apisecurity.config.TestConfig;
import io.github.apisecurity.model.LoginRequest;
import io.github.apisecurity.model.SignupRequest;
import io.restassured.response.Response;

import java.util.Map;

import static io.restassured.RestAssured.given;

public final class AuthClient {

    private static final String SIGNUP_PATH = "/identity/api/auth/signup";
    private static final String LOGIN_PATH = "/identity/api/auth/login";

    public Response signup(SignupRequest request) {
        return post(SIGNUP_PATH, request);
    }

    public Response signup(Map<String, ?> request) {
        return post(SIGNUP_PATH, request);
    }

    public Response signupInvalidPayload(Object request) {
        return post(SIGNUP_PATH, request);
    }

    public Response login(LoginRequest request) {
        return post(LOGIN_PATH, request);
    }

    public Response login(Map<String, ?> request) {
        return post(LOGIN_PATH, request);
    }

    public Response loginInvalidPayload(Object request) {
        return post(LOGIN_PATH, request);
    }

    private Response post(String path, Object request) {
        return given()
                .spec(TestConfig.requestSpecification())
                .body(request)
                .when()
                .post(path);
    }
}
