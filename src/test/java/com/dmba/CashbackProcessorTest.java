package com.dmba;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CashBackProcessorTest {

    private CashBackProcessor processor;
    private ExecutorService executorService;
    private final int threads = 10;
    private final int iterations = 50;

    private final long cashBackLimit = 300000L;

    @BeforeEach
    void setUp() {
        processor = new CashBackProcessor(cashBackLimit, new BigDecimal("0.05"));
        executorService = Executors.newFixedThreadPool(threads);
    }

    @AfterEach
    void tearDown() {
        executorService.shutdown();
    }

    @Test
    void shouldMaintainCorrectStateUnderConcurrency() throws InterruptedException {
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(iterations); // 50 tasks

        UUID userId = UUID.randomUUID();
        UUID duplicateId = UUID.randomUUID();

        for (int i = 0; i < iterations; i++) {
            executorService.submit(() -> {
                try {
                    startLatch.await();

                    processor.onTransactionReceived(
                            new TransactionEvent(duplicateId, userId, new BigDecimal("10.00"), "5411")
                    );

                    processor.onTransactionReceived(
                            new TransactionEvent(UUID.randomUUID(), userId, new BigDecimal("2000.00"), "5411")
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

        long actualBalance = processor.getMonthlyAccruals().get(userId).get();
        assertEquals(cashBackLimit, actualBalance, "Final balance mismatch");

        int processedSize = processor.getProcessedTransaction().size();
        assertEquals(51, processedSize, "Idempotency check failed");
    }
}