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

class RandomShorter {

    private static final String A_Z_0_9 = "qwertyuiopQWERTYUIOP1234567890";

    private final Random random;

    private final Integer LENGTH_SHORTER;

    public RandomShorter(Random random, Integer LENGTH_SHORTER) {
        this.random = random;
        this.LENGTH_SHORTER = LENGTH_SHORTER;
    }

    public String getShorterCode() {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < LENGTH_SHORTER; i++) {
            stringBuilder.append(A_Z_0_9.charAt(random.nextInt(A_Z_0_9.length())));
        }
        return stringBuilder.toString();
    }

    public Boolean isShorterValid(@NotNull String shortCode) {
        char [] charArray = shortCode.toCharArray();


        for (char ch: charArray) {
            if (A_Z_0_9.indexOf(ch) == -1) {
                return false;
            }
        }

        return true;
    }
}

interface Shorter {
    String getShortCode(String fullAddress);
    Optional<String> getFullAddress(String shortCode);
}

class ShorterService implements Shorter {

    private final RandomShorter randomShorter;

    private final ReentrantReadWriteLock readWriteLock;

    private final Map<String, String> shortToFull = new HashMap<>();
    private final Map<String, String> fullToShort = new HashMap<>();

    public ShorterService(RandomShorter randomShorter) {
        this.randomShorter = randomShorter;
        this.readWriteLock = new ReentrantReadWriteLock();
    }

    @Override
    public String getShortCode(@NotNull String fullAddress) {
        if (fullAddress.isEmpty()) {
            throw new IllegalArgumentException("Your address is empty.");
        }

        try {
            new URI(fullAddress);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Your address is not valid",e);
        }

        readWriteLock.readLock().lock();
        try {
            if (fullToShort.containsKey(fullAddress)) {
                return fullToShort.get(fullAddress);
            }
        } finally {
            readWriteLock.readLock().unlock();
        }

        readWriteLock.writeLock().lock();
        try {

            int MAX_TRY = 5;
            String shortCode = randomShorter.getShorterCode();
            for(int i=0; i< MAX_TRY; i++) {
                if (!shortToFull.containsKey(shortCode)) {
                    break;
                }

                shortCode = randomShorter.getShorterCode();
            }

            shortToFull.put(shortCode, fullAddress);
            fullToShort.put(fullAddress, shortCode);
            return shortCode;
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    @Override
    public Optional<String> getFullAddress(@NotNull String shortCode) {
        if (!randomShorter.isShorterValid(shortCode)) {
            return Optional.empty();
        }

        readWriteLock.readLock().lock();
        try {
           if (this.shortToFull.containsKey(shortCode)) {
               return Optional.of(shortToFull.get(shortCode));
           }
        } finally {
            readWriteLock.readLock().unlock();
        }

        return Optional.empty();
    }
}
