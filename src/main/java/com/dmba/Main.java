package com.dmba;

import org.jetbrains.annotations.NotNull;

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
    Optional<String> shortCode();
    boolean validateSHortCode(@NotNull String shortCode);
}

interface Shorter {
    Optional<String> getShortCode(@NotNull String fullAddress);
    Optional<String> getFullAddress(@NotNull String shortCode);
}

class ShorterService implements  Shorter {

    private final RandomShorter randomShorter;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private final Map<String, String> shortCodeToFull = new HashMap<>();
    private final Map<String, String> fullToShortCode = new HashMap<>();

    public ShorterService(RandomShorter randomShorter) {
        this.randomShorter = randomShorter;
    }

    @Override
    public Optional<String> getShortCode(@NotNull String fullAddress) {
        if (fullAddress == null || fullAddress.isBlank()) {
            throw new IllegalArgumentException("Address should not be null or blank");
        }


        lock.readLock().lock();
        try {
            if (this.fullToShortCode.containsKey(fullAddress)) {
                return Optional.of(fullToShortCode.get(fullAddress));
            }
        } finally {
            lock.readLock().unlock();
        }



        lock.writeLock().lock();
        try {
            if (this.fullToShortCode.containsKey(fullAddress)) {
                return Optional.of(fullToShortCode.get(fullAddress));
            }

            int maxRetry = 5;


            Optional<String> shortCodeOptional = Optional.empty();

            for (int i = 0; i < maxRetry; i ++) {

                shortCodeOptional = randomShorter.shortCode();

                if (shortCodeOptional.isEmpty()) {
                    throw new IllegalStateException("RandomShorter failed to generate code");
                }

                String shortCode = shortCodeOptional.get();

                if (!shortCodeToFull.containsKey(shortCode)) {
                    shortCodeToFull.put(shortCode, fullAddress);
                    fullToShortCode.put(fullAddress, shortCode);
                    return shortCodeOptional;
                }

                if (i == maxRetry - 1) {
                    throw new RuntimeException("Server could not generate unique short code after " + maxRetry + " retries");
                }
            }

            return Optional.empty();
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Optional<String> getFullAddress(@NotNull String shortCode) {
        if (!randomShorter.validateSHortCode(shortCode)) {
            return Optional.empty();
        }

        lock.readLock().lock();
        try {
            if (shortCodeToFull.containsKey(shortCode)) {
                return Optional.of(shortCodeToFull.get(shortCode));
            }
        } finally {
            lock.readLock().unlock();
        }

        return Optional.empty();
    }
}

class RandomShorterService implements RandomShorter {
    private final String A_Z_0_9 = "qwertyuioipQWERTYUIOP123456789";


    public RandomShorterService(Random random, int maxLength) {
        this.random = random;
        this.maxLength = maxLength;
    }

    private int maxLength;

    private final Random random;

    @Override
    public Optional<String> shortCode() {

        StringBuilder stringBuilder = new StringBuilder();
        for (int i=0; i < maxLength; i++) {
            stringBuilder.append(A_Z_0_9.charAt(random.nextInt(A_Z_0_9.length())));
        }

        return Optional.of(stringBuilder.toString());
    }

    @Override
    public boolean validateSHortCode(@NotNull String shortCode) {
        if (shortCode == null || shortCode.isBlank()) {
            return false;
        }

        if (shortCode.length() != maxLength) {
            return false;
        }

        for (int i  = 0; i < maxLength; i++) {
            if (A_Z_0_9.indexOf(shortCode.charAt(i)) == -1) {
                return false;
            }
        }

        return true;

    }

}
