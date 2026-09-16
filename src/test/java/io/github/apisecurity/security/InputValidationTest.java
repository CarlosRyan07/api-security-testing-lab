package io.github.apisecurity.security;

import io.github.apisecurity.client.AuthClient;
import io.github.apisecurity.data.UserFactory;
import io.github.apisecurity.model.SignupRequest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Tag("security")
@Tag("input-validation")
class InputValidationTest {

    private final AuthClient authClient = new AuthClient();

    @Test
    void shouldRejectDuplicateSignup() {
        SignupRequest user = UserFactory.uniqueUser();
        authClient.signup(user).then().statusCode(200);

        authClient.signup(user)
                .then()
                .statusCode(403);
    }

    @Test
    void shouldRejectSignupWithEmptyObject() {
        authClient.signup(Map.<String, Object>of())
                .then()
                .statusCode(400);
    }

    @Test
    void shouldRejectLoginWithEmptyObject() {
        authClient.login(Map.<String, Object>of())
                .then()
                .statusCode(400);
    }

    @ParameterizedTest(name = "signup with non-object payload {0}")
    @MethodSource("nonObjectPayloads")
    void shouldRejectSignupWithNonObjectPayload(String payloadType, Object payload) {
        authClient.signupInvalidPayload(payload)
                .then()
                .statusCode(400);
    }

    @ParameterizedTest(name = "login with non-object payload {0}")
    @MethodSource("nonObjectPayloads")
    void shouldRejectLoginWithNonObjectPayload(String payloadType, Object payload) {
        authClient.loginInvalidPayload(payload)
                .then()
                .statusCode(400);
    }

    @ParameterizedTest(name = "signup without required field {0}")
    @MethodSource("signupRequiredFields")
    void shouldRejectSignupWithoutRequiredField(String requiredField) {
        SignupRequest user = UserFactory.uniqueUser();
        Map<String, String> payload = signupPayload(user);
        payload.remove(requiredField);

        authClient.signup(payload)
                .then()
                .statusCode(400);
    }

    @ParameterizedTest(name = "signup with null required field {0}")
    @MethodSource("signupRequiredFields")
    void shouldRejectSignupWithNullRequiredField(String requiredField) {
        SignupRequest user = UserFactory.uniqueUser();
        Map<String, String> payload = signupPayload(user);
        payload.put(requiredField, null);

        authClient.signup(payload)
                .then()
                .statusCode(400);
    }

    @ParameterizedTest(name = "login without required field {0}")
    @MethodSource("loginRequiredFields")
    void shouldRejectLoginWithoutRequiredField(String requiredField) {
        SignupRequest user = UserFactory.uniqueUser();
        Map<String, String> payload = loginPayload(user);
        payload.remove(requiredField);

        authClient.login(payload)
                .then()
                .statusCode(400);
    }

    @ParameterizedTest(name = "login with null required field {0}")
    @MethodSource("loginRequiredFields")
    void shouldRejectLoginWithNullRequiredField(String requiredField) {
        SignupRequest user = UserFactory.uniqueUser();
        Map<String, String> payload = loginPayload(user);
        payload.put(requiredField, null);

        authClient.login(payload)
                .then()
                .statusCode(400);
    }

    private static Map<String, String> signupPayload(SignupRequest user) {
        return new HashMap<>(Map.of(
                "email", user.email(),
                "name", user.name(),
                "number", user.number(),
                "password", user.password()
        ));
    }

    private static Map<String, String> loginPayload(SignupRequest user) {
        return new HashMap<>(Map.of(
                "email", user.email(),
                "password", user.password()
        ));
    }

    private static Stream<String> signupRequiredFields() {
        return Stream.of("email", "name", "number", "password");
    }

    private static Stream<String> loginRequiredFields() {
        return Stream.of("email", "password");
    }

    private static Stream<Arguments> nonObjectPayloads() {
        return Stream.of(
                Arguments.of("array", List.of("invalid")),
                Arguments.of("string", "\"invalid\""),
                Arguments.of("number", 42),
                Arguments.of("boolean", true)
        );
    }
}
