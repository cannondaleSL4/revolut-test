package com.dmba;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class UrlShorterServiceTest {

    UrlShorterService shortener = new UrlShorterService(new RandomKeyGenerator(6));

    @Test
    void testBasicShorteningAndDeduplication() {
        String longUrlA = "https://example.com/very/long/path/a";
        String longUrlB = "https://example.com/very/long/path/b";

        String keyA1 = shortener.getShortUrl(longUrlA);
        assertNotNull(keyA1);

        assertEquals(longUrlA, shortener.getFullUrl(keyA1).get());
        String keyB = shortener.getShortUrl(longUrlB);
        assertNotEquals(keyA1, keyB);

        String keyA2 = shortener.getShortUrl(longUrlA);

        assertEquals(keyA1, keyA2, "Service must return the existing key for the same URL.");
    }

    @Test
    void testNonExistentKeyReturnsEmptyOptional() {
        String nonExistentKey = "ZXCVBN";

        Optional<String> result = shortener.getFullUrl(nonExistentKey);

        assertTrue(result.isEmpty(), "Service must return Optional.empty() for a non-existent key.");
    }
}