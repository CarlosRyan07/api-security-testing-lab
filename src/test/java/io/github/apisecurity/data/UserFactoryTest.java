package io.github.apisecurity.data;

import io.github.apisecurity.model.SignupRequest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("unit")
class UserFactoryTest {

    @Test
    void shouldCreateValidUserData() {
        SignupRequest user = UserFactory.uniqueUser();
        String suffix = user.name().substring("qa.".length());

        assertAll(
                () -> assertTrue(user.name().matches("qa\\.[a-z0-9]+")),
                () -> assertEquals(user.name() + "@example.com", user.email()),
                () -> assertTrue(user.number().matches("\\d{10}")),
                () -> assertEquals("Crapi!" + suffix, user.password())
        );
    }

    @Test
    void shouldCreateUniqueDataForEachUser() {
        SignupRequest first = UserFactory.uniqueUser();
        SignupRequest second = UserFactory.uniqueUser();

        assertAll(
                () -> assertNotEquals(first.email(), second.email()),
                () -> assertNotEquals(first.name(), second.name()),
                () -> assertNotEquals(first.number(), second.number()),
                () -> assertNotEquals(first.password(), second.password())
        );
    }
}
