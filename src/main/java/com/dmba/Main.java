package com.dmba;

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

interface RandomShorter {
    Optional<String> getShortCode();
    boolean validateShortCode(@NotNull String shortCode);
}

interface ShortCode {
    Optional<String> getShortCode(@NotNull String fullAddress);
    Optional<String> getFullAddress(@NotNull String shortCode);
}

class ShortCodeService implements ShortCode {


    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private final RandomShorter randomShorter;


    private final Map<String, String> shortCodeToFull = new HashMap<>();
    private final Map<String, String> fullAddressToShort = new HashMap<>();

    ShortCodeService(RandomShorter randomShorter) {
        this.randomShorter = randomShorter;
    }

    @Override
    public Optional<String> getShortCode(@NotNull String fullAddress) {
        if (fullAddress == null || fullAddress.isBlank()) {
            throw new IllegalArgumentException("Address code should not be null or empty");
        }

        try {
            new URI(fullAddress);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid address", e);
        }


        lock.readLock().lock();
        try {
            if (fullAddressToShort.containsKey(fullAddress)) {
                return Optional.of(fullAddressToShort.get(fullAddress));
            }
        } finally {
            lock.readLock().unlock();
        }


        lock.writeLock().lock();
        try {
            if (fullAddressToShort.containsKey(fullAddress)) {
                return Optional.of(fullAddressToShort.get(fullAddress));
            }


            Optional<String> shortCode = randomShorter.getShortCode();

            int maxRetry = 5;
            for (int i = 0; i < maxRetry; i++) {
                if (!shortCodeToFull.containsKey(shortCode.get())) {
                    break;
                }

                if (i == maxRetry-1) {
                    throw new IllegalStateException("Could not generate unique short code");
                }

                shortCode = randomShorter.getShortCode();
            }

            fullAddressToShort.put(fullAddress, shortCode.get());
            shortCodeToFull.put(shortCode.get(), fullAddress);
            return shortCode;
        } finally {
            lock.writeLock().unlock();

        }
    }

    @Override
    public Optional<String> getFullAddress(@NotNull String shortCode) {
        if (shortCode == null || shortCode.isBlank()) {
            throw new IllegalArgumentException("Short code should not be null or empty");
        }

        if (!randomShorter.validateShortCode(shortCode)) {
            throw new IllegalStateException("Short code not valid");

        }

        lock.readLock().lock();
        try {

            if (this.shortCodeToFull.containsKey(shortCode)){
                return Optional.of(shortCodeToFull.get(shortCode));
            }

            return Optional.empty();
        } finally {
            lock.readLock().unlock();
        }
    }
}

class RandomShorterService implements RandomShorter {
    private final Random random;

    private final int maxLength;

    private final String A_Z_0_9 = "qwertyuiopQWERTYUIOOP1234567890";


    RandomShorterService(Random random, int maxLength) {
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
        if (shortCode == null || shortCode.isBlank()) {
            throw new IllegalArgumentException("Short code should not be null or empty");
        }

        for (int i = 0; i < shortCode.length(); i++) {
            if (A_Z_0_9.indexOf(shortCode.charAt(i)) == -1) {
                return false;
            }
        }

        return true;
    }
}