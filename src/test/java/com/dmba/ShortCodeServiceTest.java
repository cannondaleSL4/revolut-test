package com.dmba;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

class ShortCodeServiceTest {



    private final int maxRequet = 100000;

    private CountDownLatch endFlag;
    private CountDownLatch runIteration;
    private ExecutorService executorService;


    private ShortCode shortCode;
    private RandomShorter randomShorter;

    @BeforeEach
    void setUp() {
        endFlag = new CountDownLatch(1);
        runIteration = new CountDownLatch(maxRequet);
        executorService = Executors.newFixedThreadPool(10);

        randomShorter = new RandomShorterService(ThreadLocalRandom.current(), 6);
        shortCode = new ShortCodeService(randomShorter);
    }

    @AfterEach
    void tearDown() {
        executorService.shutdownNow();
    }

    @Test
    public void testStabilityWithParallelRunning() throws InterruptedException {

        String google = "http://google.com";
        String apple = "http://apple.com";


        Optional<String> googleShortCode = shortCode.getShortCode(google);
        Optional<String> appleShortCode = shortCode.getShortCode(apple);



        for (int i=0; i < maxRequet; i++) {
            executorService.submit(() -> {
                try {
                    endFlag.await();

                    Optional<String> shG = shortCode.getShortCode(google);
                    Optional<String> shA = shortCode.getShortCode(apple);

                    assertTrue(shG.isPresent());
                    assertTrue(shA.isPresent());


                    assertEquals(googleShortCode, shG.get());
                    assertEquals(appleShortCode, shA.get());

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    runIteration.countDown();
                }
            });
        }


        endFlag.countDown();

        boolean allPassed = runIteration.await(5, TimeUnit.SECONDS);
        assertTrue(allPassed);
    }
}