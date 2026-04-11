package com.dmba;


import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class CashBackProcessorTest {

   private CashBackProcessor spyProcessor;
   private ExecutorService executorService;
   private final int threads = 10;
   private final int iterations = 50;

    @BeforeEach
    void setUp() {
        spyProcessor = Mockito.spy(new CashBackProcessor());
        executorService = Executors.newFixedThreadPool(threads);
    }

    @AfterEach
    void tearDown() {
        executorService.shutdown();
    }

    @Test
    void onTransactionReceived() throws InterruptedException {

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threads);
        UUID userId = UUID.randomUUID();
        UUID duplicateId = UUID.randomUUID();

        for (int i=0 ; i < iterations; i++) {
            executorService.submit(() -> {
                try {
                    startLatch.await();

                    spyProcessor.onTransactionReceived(
                            new TransactionEvent(duplicateId, userId, new BigDecimal("10.00"), "5411")
                    );

                    spyProcessor.onTransactionReceived(
                            new TransactionEvent(UUID.randomUUID(), userId, new BigDecimal("100000.00"), "5411")
                    );

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();

        boolean finished = endLatch.await(5, TimeUnit.SECONDS);
        assertTrue(finished);
        ArgumentCaptor<Long> captor = ArgumentCaptor.forClass(Long.class);
        verify(spyProcessor, atLeastOnce()).saveToDb(eq(userId), captor.capture());

        long totalAccrued = captor.getAllValues().stream()
                .mapToLong(Long::longValue)
                .sum();

        assertEquals(300000L, totalAccrued, "The final sum must match the monthly limit exactly");
    }
}