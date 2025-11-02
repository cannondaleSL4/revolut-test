package com.dmba;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Main {
        public static void main(String[] args) {
            final int KEY_LENGTH = 6;

            RandomCreator keyGenerator = new RandomCreator(KEY_LENGTH);
            ShortUrlService shortener = new ShortUrlService(keyGenerator);


            String longUrl1 = "https://www.revolut.com/careers/senior-software-developer/java";
            String key1 = shortener.getShortUrl(longUrl1);

            String fullUrl1 = shortener.getLongUrl(key1)
                    .orElseThrow(() -> new RuntimeException("Test setup failed for key 1"));
            System.out.println("URL 1 Key: " + key1 + " -> " + fullUrl1);

            String longUrl2 = "https://www.revolut.com/investing/stocks";
            String key2 = shortener.getShortUrl(longUrl2);

            String fullUrl2 = shortener.getLongUrl(key2)
                    .orElseThrow(() -> new RuntimeException("Test setup failed for key 2"));
            System.out.println("URL 2 Key: " + key2 + " -> " + fullUrl2);
        }
}

class CustomShortUrlException extends RuntimeException {
    public CustomShortUrlException(String message) {
        super(message);
    }
}

class RandomCreator {

    public final static String ALPHABET_NUMS = "qwertyuiopQWERTYUIOP1234567890";

    public Integer lengthOfShortCode;

    public SecureRandom secureRandom = new SecureRandom();

    public RandomCreator(Integer lengthOfShortCode) {
        this.lengthOfShortCode = lengthOfShortCode;
    }

    public String getShortCode() {
        StringBuilder shortCode = new StringBuilder();
        for(int i = 0; i < lengthOfShortCode; i++) {
            shortCode.append(ALPHABET_NUMS.charAt(secureRandom.nextInt(ALPHABET_NUMS.length())));
        }
        return shortCode.toString();
    }

    public Boolean validateShortCode(String shortCode) {
        var charArray = shortCode.toCharArray();

        for (char c : charArray) {
            if (ALPHABET_NUMS.indexOf(c) == -1) {
                return false;
            }
        }
        return true;
    }
}

interface ShortUrlInterface {
    String getShortUrl(String longUrl);
    Optional<String> getLongUrl(String shortUrl);
}

class ShortUrlService implements ShortUrlInterface {

    final private RandomCreator randomCreator;

    public Map<String, String> shortToLongMap = new HashMap<>();
    public Map<String, String> longToShortMap = new HashMap<>();

    ReentrantReadWriteLock reentrantReadWriteLock = new ReentrantReadWriteLock();

    public ShortUrlService(RandomCreator randomCreator) {
        this.randomCreator = randomCreator;
    }


    @Override
    public String getShortUrl(String fullUrl)  {
        if (fullUrl == null || fullUrl.isEmpty()) {
            throw new IllegalArgumentException("Long URL cannot be empty");
        }

        try {
            new URL(fullUrl);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }

        try {
            this.reentrantReadWriteLock.writeLock().lock();
            String checkExists = longToShortMap.get(fullUrl);
            if (checkExists != null) {
                return checkExists;
            }

            String shortCode = randomCreator.getShortCode();

            int max_attempts = 5;

            for (int i = 0; i <= max_attempts; i++) {
                if (!shortToLongMap.containsKey(shortCode)) {
                    break;
                }

                shortCode = randomCreator.getShortCode();

                if (i == max_attempts) {
                    throw new CustomShortUrlException("Unable to generate a unique short code");
                }
            }
            shortToLongMap.put(shortCode, fullUrl);
            longToShortMap.put(fullUrl, shortCode);
            return shortCode;
        } finally {
            this.reentrantReadWriteLock.writeLock().unlock();
        }
    }

    @Override
    public Optional<String> getLongUrl(String shortCode) {

        if (shortCode == null || shortCode.isEmpty() || !randomCreator.validateShortCode(shortCode)) {
            return Optional.empty();
        }

        try {
            this.reentrantReadWriteLock.readLock().lock();
            String result = this.shortToLongMap.get(shortCode);
            if (result != null) {
                return Optional.of(result);
            }
            return Optional.empty();
        } finally {
            this.reentrantReadWriteLock.readLock().unlock();
        }
    }
}
