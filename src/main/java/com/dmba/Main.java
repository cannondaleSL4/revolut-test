package com.dmba;

import java.security.SecureRandom;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {

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
        return "";
    }

    @Override
    public String getFullUrl(String shortUrl) {
        String tempUrl =  randomKeyGenerator.randomGenerator();

        shorterMap.computeIfAbsent();
    }
}