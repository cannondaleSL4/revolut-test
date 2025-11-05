package com.dmba;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

class ShorterServiceTest {


    private Shorter shorter;
    private RandomShorter randomShorter;
    private final int maxRequest = 10000;


    private CountDownLatch starSignal;
    private CountDownLatch endSignal;

    private ExecutorService executorService;

    @BeforeEach
    void setUp() {
        randomShorter = new RandomShorter(ThreadLocalRandom.current(), 6);
        shorter = new ShorterService(randomShorter);
        starSignal = new CountDownLatch(1);
        endSignal = new CountDownLatch(maxRequest);
        executorService = Executors.newFixedThreadPool(10);
    }


    @Test
    public void testStabilityWithParallelRunning() throws InterruptedException {
        String urlA = "https://www.site-a.com/page/long/address/1";
        String urlB = "https://www.site-b.com/page/long/address/2";

        final String shortCodeA = shorter.getShortCode(urlA);
        final String shortCodeB = shorter.getShortCode(urlB);

        for(int i =0 ; i < maxRequest ; i ++) {
            executorService.submit(() -> {
                try {
                    starSignal.await();

                    String newCodeA = shorter.getShortCode(urlA);
                    String newCodeB = shorter.getShortCode(urlB);

                    shorter.getFullAddress(newCodeA).ifPresent(full -> assertEquals(urlA, full));
                    shorter.getFullAddress(newCodeB).ifPresent(full -> assertEquals(urlB, full));

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endSignal.countDown();
                }
            });
        }

        starSignal.countDown();

        boolean result = endSignal.await(5, TimeUnit.SECONDS);
        assertTrue(result);

        assertEquals(shortCodeA, shorter.getShortCode(urlA));
        assertEquals(shortCodeB, shorter.getShortCode(urlB));
    }

    @AfterEach
    void tearDown() {
        executorService.shutdownNow();
    }
}