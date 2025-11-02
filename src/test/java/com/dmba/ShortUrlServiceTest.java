package com.dmba;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ShortUrlServiceTest {

    RandomCreator randomCreator;
    ShortUrlInterface shortener;


    @BeforeEach
    public void prepareShortUrlInterface() {
        randomCreator = new RandomCreator(6);
        shortener = new ShortUrlService(randomCreator);
    }

    @Test
    void getShortUrl() {
        String shortCode = shortener.getShortUrl("https://www.revolut.com/careers/senior-software-developer/java");
        assertNotNull(shortCode);
        assertEquals(6, shortCode.length());
    }

    @Test
    void getShortUrlLoop() {
        String shortCode = shortener.getShortUrl("https://www.revolut.com/careers/senior-software-developer/java");
        assertNotNull(shortCode);
        assertEquals(6, shortCode.length());

        for(int i = 0; i < 100; i++) {
            shortCode = shortener.getShortUrl("https://www.revolut.com/careers/senior-software-developer/java");
            assertNotNull(shortCode);
            assertEquals(6, shortCode.length());
        }
    }


    @Test
    void getShortUrlNull() {
        assertThrows(IllegalArgumentException.class, () -> shortener.getShortUrl(null));
    }

    @Test
    void getShortUrlNotValidUrl() {
        assertThrows(IllegalArgumentException.class, () -> shortener.getShortUrl(";alalalal"));
    }


    @Test
    void getLongUrl() {
        String shortCode = shortener.getShortUrl("https://www.revolut.com/careers/senior-software-developer/java");
        assertNotNull(shortCode);
        assertEquals(6, shortCode.length());
        assertEquals(shortener.getLongUrl(shortCode).get(), "https://www.revolut.com/careers/senior-software-developer/java");
    }

    @Test
    void getLongUrlNotValidShortCode() {
        String shortCode = shortener.getShortUrl("https://www.revolut.com/careers/senior-software-developer/java");
        assertNotNull(shortCode);
        assertEquals(6, shortCode.length());
        assertTrue(shortener.getLongUrl("qwqwqw").isEmpty());
    }

    @Test
    void getLongUrlNotValidShortCodeNull() {
        String shortCode = shortener.getShortUrl("https://www.revolut.com/careers/senior-software-developer/java");
        assertNotNull(shortCode);
        assertEquals(6, shortCode.length());
        assertTrue(shortener.getLongUrl(null).isEmpty());
    }

    @Test
    void getLongUrlNotValidShortCodeEmptyCode() {
        String shortCode = shortener.getShortUrl("https://www.revolut.com/careers/senior-software-developer/java");
        assertNotNull(shortCode);
        assertEquals(6, shortCode.length());
        assertTrue(shortener.getLongUrl("").isEmpty());
    }
}