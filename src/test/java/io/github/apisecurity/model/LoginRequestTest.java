package io.github.apisecurity.model;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("unit")
class LoginRequestTest {

    @Test
    void shouldCopyCredentialsFromSignupRequest() {
        SignupRequest signup = new SignupRequest(
                "qa.user@example.com",
                "qa.user",
                "1234567890",
                "Crapi!password"
        );

        LoginRequest login = LoginRequest.from(signup);

        assertEquals(signup.email(), login.email());
        assertEquals(signup.password(), login.password());
    }
}
