package com.dmba;

public class Main {
    public static void main(String[] args) {
        final int KEY_LENGTH = 6;

        RandomKeyGenerator keyGenerator = new RandomKeyGenerator(KEY_LENGTH);
        UrlShorterService shortener = new UrlShorterService(keyGenerator);

        System.out.println("--- Упрощенный URL Shortener (In-Memory, Random Key) ---");

        String longUrl1 = "https://www.revolut.com/careers/senior-software-developer/java";
        String key1 = shortener.getShortUrl(longUrl1);
        System.out.println("URL 1 Key: " + key1 + " -> " + shortener.getFullUrl(key1).get());

        String longUrl2 = "https://www.revolut.com/investing/stocks";
        String key2 = shortener.getShortUrl(longUrl2);
        System.out.println("URL 2 Key: " + key2 + " -> " + shortener.getFullUrl(key2).get());
    }
}

//class RandomKeyGenerator {
//    public static final String ALPHABET = "qwertyuiopQWERTYUIOP123456789";
//
//    public final SecureRandom secRandom = new SecureRandom();
//
//    public Integer keyLength;
//
//    public RandomKeyGenerator(Integer keyLength) {
//        this.keyLength = keyLength;
//    }
//
//    public String randomGenerator() {
//        StringBuilder sb = new StringBuilder();
//
//        for (int i=0; i < this.keyLength; i++) {
//            sb.append(ALPHABET.charAt(secRandom.nextInt(ALPHABET.length())));
//        }
//
//        return sb.toString();
//    }
//
//}

//interface ShorterUrl {
//    String getShortUrl(String fullUrl) throws ShortUrlCustomException;
//    Optional<String> getFullUrl(String shortUrl);
//}

//class UrlShorterService implements ShorterUrl{
//
//    public RandomKeyGenerator randomKeyGenerator;
//
//    public ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
//
//    public Map<String, String> shortTolong = new HashMap<>();
//    public Map<String, String> longToShort = new HashMap<>();
//
//    public UrlShorterService(RandomKeyGenerator randomKeyGenerator) {
//        this.randomKeyGenerator = randomKeyGenerator;
//    }
//
//    @Override
//    public String getShortUrl(String fullUrl) throws ShortUrlCustomException {
//        if (fullUrl == null || fullUrl.isEmpty()) {
//            throw new ShortUrlCustomException("Your URL is empty");
//        }
//
//        try {
//            lock.writeLock().lock();
//            String checkExists = longToShort.get(fullUrl);
//            if (checkExists != null) {
//                return checkExists;
//            }
//
//            String shortKey = null;
//
//            final int MAX_ATTEMPTS = 5;
//            for (int i = 0; i < MAX_ATTEMPTS; i++) {
//                shortKey = this.randomKeyGenerator.randomGenerator();
//
//                if (!shortTolong.containsKey(shortKey)) {
//                    break;
//                }
//
//                if (i == MAX_ATTEMPTS - 1) {
//                    throw new ShortUrlCustomException("Failed to generate a unique key after " + MAX_ATTEMPTS + " attempts.");
//                }
//            }
//            longToShort.put(fullUrl, shortKey);
//            shortTolong.put(shortKey, fullUrl);
//            return shortKey;
//        } finally {
//            lock.writeLock().unlock();
//        }
//    }
//
//    @Override
//    public Optional<String> getFullUrl(String shortUrl) {
//        try {
//            lock.readLock().lock();
//            String result = shortTolong.get(shortUrl);
//            return Optional.ofNullable(result);
//        } finally {
//            lock.readLock().unlock();
//        }
//    }
//}
//
//class ShortUrlCustomException extends RuntimeException {
//    public ShortUrlCustomException(String message) {
//        super(message);
//    }
//}