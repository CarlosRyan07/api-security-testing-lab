package io.github.apisecurity.config;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

import java.net.URI;

public final class TestConfig {

    private static final String DEFAULT_BASE_URL = "http://localhost:8888";

    private TestConfig() {
    }

    public static RequestSpecification requestSpecification() {
        return new RequestSpecBuilder()
                .setBaseUri(baseUrl())
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .build();
    }

    public static String baseUrl() {
        return resolveBaseUrl(System.getenv("BASE_URL"));
    }

    static String resolveBaseUrl(String configuredUrl) {
        String candidate = configuredUrl == null ? DEFAULT_BASE_URL : configuredUrl.trim();

        if (candidate.isBlank()) {
            throw invalidBaseUrl();
        }

        URI uri;
        try {
            uri = URI.create(candidate);
        } catch (IllegalArgumentException ignored) {
            throw invalidBaseUrl();
        }

        if (uri.getHost() == null
                || !("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))) {
            throw invalidBaseUrl();
        }

        return candidate.replaceAll("/+$", "");
    }

    private static IllegalArgumentException invalidBaseUrl() {
        return new IllegalArgumentException("BASE_URL must be a valid HTTP or HTTPS URL");
    }
}
