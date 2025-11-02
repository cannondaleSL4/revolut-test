package com.dmba;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class UrlShorterService implements ShorterUrl {
    public RandomKeyGenerator randomKeyGenerator;

    public ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public Map<String, String> shortTolong = new HashMap<>();
    public Map<String, String> longToShort = new HashMap<>();

    public UrlShorterService(RandomKeyGenerator randomKeyGenerator) {
        this.randomKeyGenerator = randomKeyGenerator;
    }

    @Override
    public String getShortUrl(String fullUrl) throws ShortUrlCustomException {
        if (fullUrl == null || fullUrl.isEmpty()) {
            throw new ShortUrlCustomException("Your URL is empty");
        }

        try {
            lock.writeLock().lock();
            String checkExists = longToShort.get(fullUrl);
            if (checkExists != null) {
                return checkExists;
            }

            String shortKey = null;

            final int MAX_ATTEMPTS = 5;
            for (int i = 0; i < MAX_ATTEMPTS; i++) {
                shortKey = this.randomKeyGenerator.randomGenerator();

                if (!shortTolong.containsKey(shortKey)) {
                    break;
                }

                if (i == MAX_ATTEMPTS - 1) {
                    throw new ShortUrlCustomException("Failed to generate a unique key after " + MAX_ATTEMPTS + " attempts.");
                }
            }
            longToShort.put(fullUrl, shortKey);
            shortTolong.put(shortKey, fullUrl);
            return shortKey;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Optional<String> getFullUrl(String shortUrl) {
        try {
            lock.readLock().lock();
            String result = shortTolong.get(shortUrl);
            return Optional.ofNullable(result);
        } finally {
            lock.readLock().unlock();
        }
    }
}
