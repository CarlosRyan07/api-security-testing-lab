package io.github.apisecurity.config;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("unit")
class TestConfigTest {

    @Test
    void shouldUseLocalDefaultWhenBaseUrlIsNotConfigured() {
        assertEquals("http://localhost:8888", TestConfig.resolveBaseUrl(null));
    }

    @Test
    void shouldTrimWhitespaceAndTrailingSlashes() {
        assertEquals("https://example.com/crapi", TestConfig.resolveBaseUrl("  https://example.com/crapi///  "));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "http://example.com",
            "https://example.com"
    })
    void shouldAcceptHttpAndHttpsUrls(String baseUrl) {
        assertEquals(baseUrl, TestConfig.resolveBaseUrl(baseUrl));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "   ",
            "localhost:8888",
            "ftp://example.com",
            "https:///missing-host",
            "https://invalid host"
    })
    void shouldRejectInvalidUrls(String baseUrl) {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> TestConfig.resolveBaseUrl(baseUrl)
        );

        assertEquals("BASE_URL must be a valid HTTP or HTTPS URL", exception.getMessage());
    }
}
