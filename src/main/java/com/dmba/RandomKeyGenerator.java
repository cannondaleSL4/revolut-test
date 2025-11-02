package com.dmba;

import java.security.SecureRandom;

public class RandomKeyGenerator {

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
