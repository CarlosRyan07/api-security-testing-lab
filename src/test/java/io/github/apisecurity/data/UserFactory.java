package io.github.apisecurity.data;

import io.github.apisecurity.model.SignupRequest;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

public final class UserFactory {

    private static final long MAX_PHONE_NUMBER = 9_999_999_999L;
    private static final AtomicLong UNIQUE_SEQUENCE = new AtomicLong(
            ThreadLocalRandom.current().nextLong(1_000_000_000L, 9_000_000_000L)
    );

    private UserFactory() {
    }

    public static SignupRequest uniqueUser() {
        long sequence = UNIQUE_SEQUENCE.getAndIncrement();
        if (sequence > MAX_PHONE_NUMBER) {
            throw new IllegalStateException("UserFactory exhausted its per-execution phone number range");
        }

        String suffix = Long.toString(sequence, 36);
        String phoneNumber = Long.toString(sequence);

        return new SignupRequest(
                "qa." + suffix + "@example.com",
                "qa." + suffix,
                phoneNumber,
                "Crapi!" + suffix
        );
    }
}
