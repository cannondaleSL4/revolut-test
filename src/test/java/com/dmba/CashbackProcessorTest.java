package com.dmba;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class CashbackProcessorTest {

    private CashbackProcessor spyProcessor;
    private ExecutorService executorService;
    private final int threads = 10;
    private final int iterations = 50;

    @BeforeEach
    void setUp() {
        spyProcessor = Mockito.spy(new CashbackProcessor());
        executorService = Executors.newFixedThreadPool(threads);
    }

    @AfterEach
    void tearDown() {
        executorService.shutdown();
    }

    @Test
    void shouldHandleStabilityAndLimitsUnderConcurrency() throws InterruptedException {
        UUID userId = UUID.randomUUID();
        UUID duplicateTxId = UUID.randomUUID();

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(iterations);

        for (int i = 0; i < iterations; i++) {
            executorService.submit(() -> {
                try {
                    startLatch.await();

                    spyProcessor.onTransactionReceived(
                        new TransactionEvent(duplicateTxId, userId, new BigDecimal("10.00"), "5411")
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
        assertTrue(finished, "Threads did not complete in time");

        ArgumentCaptor<Long> captor = ArgumentCaptor.forClass(Long.class);
        verify(spyProcessor, atLeastOnce()).saveToDb(eq(userId), captor.capture());

        long totalAccrued = captor.getAllValues().stream()
                .mapToLong(Long::longValue)
                .sum();

        assertEquals(300000L, totalAccrued, "The final sum must match the monthly limit exactly");
    }
}