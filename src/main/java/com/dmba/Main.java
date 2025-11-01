package com.dmba;

import java.security.SecureRandom;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {
        final int KEY_LENGTH = 6;

        RandomKeyGenerator keyGenerator = new RandomKeyGenerator(KEY_LENGTH);
        UrlShorterService shortener = new UrlShorterService(keyGenerator);

        System.out.println("--- Упрощенный URL Shortener (In-Memory, Random Key) ---");

        // Тест 1
        String longUrl1 = "https://www.revolut.com/careers/senior-software-developer/java";
        String key1 = shortener.getShortUrl(longUrl1);
        System.out.println("URL 1 Key: " + key1 + " -> " + shortener.getFullUrl(key1));

        // Тест 2
        String longUrl2 = "https://www.revolut.com/investing/stocks";
        String key2 = shortener.getShortUrl(longUrl2);
        System.out.println("URL 2 Key: " + key2 + " -> " + shortener.getFullUrl(key2));
    }
}

class RandomKeyGenerator {
    public static final String ALPHABET = "qwertyuiopQWERTYUIOP123456789";

    public final SecureRandom secRandom = new SecureRandom();

    public Integer keyLength;

    public RandomKeyGenerator(Integer keyLength) {
        this.keyLength = keyLength;
    }

    public String randomGenerator() {
        StringBuilder sb = new StringBuilder();

        for (int i=0; i < this.keyLength; i++) {
            sb.append(ALPHABET.charAt(secRandom.nextInt(ALPHABET.length())));
        }

        return sb.toString();
    }

}

interface ShorterUrl {
    String getShortUrl(String fullUrl);
    String getFullUrl(String shortUrl);
}

class UrlShorterService implements ShorterUrl{

    public RandomKeyGenerator randomKeyGenerator;

    public ConcurrentMap<String, String> shorterMap = new ConcurrentHashMap<>();

    public UrlShorterService(RandomKeyGenerator randomKeyGenerator) {
        this.randomKeyGenerator = randomKeyGenerator;
    }

    @Override
    public String getShortUrl(String fullUrl) {
        if (fullUrl == null || fullUrl.isEmpty()) {
            throw new RuntimeException("Your URL is empty");
        }

        String shortUtl = this.randomKeyGenerator.randomGenerator();

        String temp = shorterMap.putIfAbsent(shortUtl, fullUrl);

        if (temp != null) {
            throw new RuntimeException("Your URL already in the map");
        }

        return shortUtl;
    }

    @Override
    public String getFullUrl(String shortUrl) {

        String result = shorterMap.get(shortUrl);

        if (result == null) {
            throw  new RuntimeException("Your url not in list");
        }
        return result;
    }
}