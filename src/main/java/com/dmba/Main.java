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

interface ShortCode {
    Optional<String> getShortCode(String fullAddress);
    Optional<String> getFullAddress(String shortCode);
}


class GenerateRandomShortCode {
    private final Random random;

    private final int max_len;

    /*
    ThreadLocalRandom.current();
     */
    public GenerateRandomShortCode(Random random, int max_len) {
        this.random = random;
        this.max_len = max_len;
    }

    private final static String A_Z_0_9 = "qwertyuiopQWERTYUIOP1234567890";

    public String generateShortCode() {
        StringBuilder stringBuilder = new StringBuilder();
        int size = A_Z_0_9.length();
        for (int i =0; i < max_len; i++) {
            stringBuilder.append(A_Z_0_9.charAt(random.nextInt(size)));
        }

        return stringBuilder.toString();
    }

    public boolean validateShortCode(@NotNull String shortCode) {
        if (shortCode.isBlank() || shortCode.length() != max_len) {
            return false;
        }

        for(int i = 0; i< shortCode.length(); i++) {
            if (A_Z_0_9.indexOf(shortCode.charAt(i)) == -1) {
                return false;
            }
        }

        return true;
    }
}

class ShortCodeService implements ShortCode {

    private final GenerateRandomShortCode randonShortCode;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private final Map<String, String> shortToFull = new HashMap<>();
    private final Map<String, String> fullToShort = new HashMap<>();

    public ShortCodeService(GenerateRandomShortCode randomShortCode) {
        this.randonShortCode = randomShortCode;
    }

    @Override
    public Optional<String> getShortCode(@NotNull String fullAddress) {


        if (fullAddress.isBlank()) {
            throw new IllegalArgumentException("Address is blank.");
        }

        try {
            new URI(fullAddress);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Address is not a valid.", e);
        }


        lock.readLock().lock();
        try {
            if (fullToShort.containsKey(fullAddress)) {
                return Optional.of(fullToShort.get(fullAddress));
            }
        } finally {
            lock.readLock().unlock();
        }

        lock.writeLock().lock();
        try {

            if (fullToShort.containsKey(fullAddress)) {
                return Optional.of(fullToShort.get(fullAddress));
            }

            String shortCode = randonShortCode.generateShortCode();

            int max_try = 5;

            for (int i =0; i < max_try; i++) {

                if (i == max_try -1) {
                    throw new IllegalStateException("could not generate unique code.");
                }

                if (!shortToFull.containsKey(shortCode)) {
                    break;
                }

                shortCode = randonShortCode.generateShortCode();

            }

            shortToFull.put(shortCode, fullAddress);
            fullToShort.put(fullAddress, shortCode);
            return Optional.of(shortCode);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Optional<String> getFullAddress(String shortCode) {
        if (!randonShortCode.validateShortCode(shortCode)) {
            return Optional.empty();
        }
        lock.readLock().lock();
        try {
            return Optional.ofNullable(shortToFull.get(shortCode));
        } finally {
            lock.readLock().unlock();
        }
    }
}