package com.dmba;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class CashbackProcessorTest {

    private CashbackProcessor processor;
    private CashbackProcessor spyProcessor;
    private UUID userId;

    @BeforeEach
    void setUp() {
        processor = new CashbackProcessor();
        // Spying allows us to keep the real logic but intercept 'saveToDb'
        spyProcessor = Mockito.spy(processor);
        userId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Should accrue correct cashback for a single transaction")
    void shouldAccrueCorrectCashback() {
        TransactionEvent event = new TransactionEvent(UUID.randomUUID(), userId, new BigDecimal("100.00"), "5411");

        spyProcessor.onTransactionReceived(event);

        // 100.00 * 0.05 * 100 = 500 units
        verify(spyProcessor, times(1)).saveToDb(userId, 500L);
    }

    @Test
    @DisplayName("Should skip processing for duplicate transaction IDs (Idempotency)")
    void shouldHandleDuplicateTransactions() {
        UUID txId = UUID.randomUUID();
        TransactionEvent event = new TransactionEvent(txId, userId, new BigDecimal("100.00"), "5411");

        spyProcessor.onTransactionReceived(event);
        spyProcessor.onTransactionReceived(event);

        verify(spyProcessor, times(1)).saveToDb(eq(userId), anyLong());
    }

    @Test
    @DisplayName("Should cap accruals at MAX_MONTHLY_BONUS")
    void shouldCapAccrualsAtLimit() {
        // 100,000.00 * 0.05 = 5,000.00 (500,000 units), but limit is 300,000 units
        TransactionEvent hugeEvent = new TransactionEvent(UUID.randomUUID(), userId, new BigDecimal("100000.00"), "5411");

        spyProcessor.onTransactionReceived(hugeEvent);

        verify(spyProcessor, times(1)).saveToDb(userId, 300000L);
    }

    @Test
    @DisplayName("Should handle race conditions and respect limit in concurrent environment")
    void shouldHandleConcurrencyCorrectly() throws InterruptedException {
        int threads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startLatch = new CountDownLatch(1);

        // Use a BIG amount so we definitely hit the 300,000 limit
        // 100,000.00 * 0.05 = 5,000.00 (500,000 units)
        BigDecimal bigAmount = new BigDecimal("100000.00");

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    TransactionEvent event = new TransactionEvent(UUID.randomUUID(), userId, bigAmount, "5411");
                    spyProcessor.onTransactionReceived(event);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        startLatch.countDown();
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        ArgumentCaptor<Long> amountCaptor = ArgumentCaptor.forClass(Long.class);
        verify(spyProcessor, atLeastOnce()).saveToDb(eq(userId), amountCaptor.capture());

        long totalAccrued = amountCaptor.getAllValues().stream().mapToLong(Long::longValue).sum();
        assertEquals(300000L, totalAccrued, "Total accrued cashback must exactly match the limit");
    }
}