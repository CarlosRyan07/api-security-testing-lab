package io.github.apisecurity.data;

import io.github.apisecurity.model.SignupRequest;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class UserFactory {

    private UserFactory() {
    }

    public static SignupRequest uniqueUser() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String phoneNumber = Long.toString(ThreadLocalRandom.current().nextLong(1_000_000_000L, 10_000_000_000L));

        return new SignupRequest(
                "qa." + suffix + "@example.com",
                "qa." + suffix,
                phoneNumber,
                "Crapi!" + suffix
        );
    }
}
