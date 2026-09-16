package io.github.apisecurity.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record JwtResponse(
        String token,
        String type,
        String message,
        String role
) {
}
