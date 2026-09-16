package io.github.apisecurity.model;

public record LoginRequest(String email, String password) {

    public static LoginRequest from(SignupRequest user) {
        return new LoginRequest(user.email(), user.password());
    }
}
