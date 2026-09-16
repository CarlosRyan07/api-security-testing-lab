package io.github.apisecurity.model;

public record SignupRequest(
        String email,
        String name,
        String number,
        String password
) {
}
