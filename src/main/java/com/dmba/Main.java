package org.example;

import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Main {
    public static void main(String[] args) {
    }
}

interface ShortCode {
    Optional<String> getShortCode(@NotNull String fullAddress);

    Optional<String> getFullAddress(@NotNull String shortCode);
}

interface RandomShorter {
    Optional<String> getShortCode();

    boolean validateShortCode(@NotNull String shortCode);

}

class ShortCodeService implements ShortCode {

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final RandomShorter randomShorter;

    private final Map<String, String> shortCodeToFullAddress = new HashMap<>();
    private final Map<String, String> fullAddressToShortCode = new HashMap<>();

    public ShortCodeService(RandomShorter randomShorter) {
        this.randomShorter = randomShorter;
    }

    @Override
    public Optional<String> getShortCode(@NotNull String fullAddress) {
        if (fullAddress == null || fullAddress.isBlank()) {
            throw new IllegalArgumentException("Full address should be not null or empty");
        }

        try {
            new URI(fullAddress);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        lock.readLock().lock();
        try {
             if (fullAddressToShortCode.containsKey(fullAddress)) {
                 return Optional.of(fullAddressToShortCode.get(fullAddress));
             }
        } finally {
            lock.readLock().unlock();
        }

        lock.writeLock().lock();
        try {
            if (fullAddressToShortCode.containsKey(fullAddress)) {
                return Optional.of(fullAddressToShortCode.get(fullAddress));
            }

            int maxRetry = 5;
            String generatedCode = null;
            for (int i = 0; i < maxRetry; i++) {
                Optional<String> maybeCode = randomShorter.getShortCode();

                if (maybeCode.isPresent()) {
                    String code = maybeCode.get();
                    if (!shortCodeToFullAddress.containsKey(code)) {
                        generatedCode = code;
                        break;
                    }
                }

                if (i == maxRetry - 1) {
                    throw new IllegalStateException("Could not generate unique short code");
                }
            }

            fullAddressToShortCode.put(fullAddress, generatedCode);
            shortCodeToFullAddress.put(generatedCode, fullAddress);
            return Optional.of(generatedCode);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Optional<String> getFullAddress(@NotNull String shortCode) {
        if (!randomShorter.validateShortCode(shortCode)) {
            return Optional.empty();
        }

        lock.readLock().lock();
        try {
            return Optional.ofNullable(shortCodeToFullAddress.get(shortCode));
        } finally {
            lock.readLock().unlock();
        }
    }
}


class RandomShorterService implements RandomShorter {

    private final Random random;
    private final int maxLength;
    private final static String A_Z_0_9 = "qwertyuiopQWERTYUIOP1234567890";

    public RandomShorterService(Random random, int maxLength) {
        this.random = random;
        this.maxLength = maxLength;
    }


    @Override
    public Optional<String> getShortCode() {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < maxLength; i++) {
            stringBuilder.append(A_Z_0_9.charAt(random.nextInt(A_Z_0_9.length())));
        }
        return Optional.of(stringBuilder.toString());
    }

    @Override
    public boolean validateShortCode(@NotNull String shortCode) {
        if (shortCode == null || shortCode.length() != maxLength) {
            return false;
        }

        for (int i = 0; i < shortCode.length(); i++) {
            if (A_Z_0_9.indexOf(shortCode.charAt(i)) == -1) {
                return false;
            }
        }
        return true;
    }
}


