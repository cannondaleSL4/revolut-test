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

import static org.junit.jupiter.api.Assertions.*;

class WalletServiceTest {

    private WalletService walletService;
    private ExecutorService executorService;
    private final int threads = 10;
    private final int iterations = 1000;

    @BeforeEach
    void setUp() {
        walletService = new WalletService();
        executorService = Executors.newFixedThreadPool(threads);
    }

    @AfterEach
    void tearDown() {
        executorService.shutdown();
    }

    @Test
    void shouldHandleConcurrentTransfersAndPreventDeadlocks() throws InterruptedException {
        UUID userA = UUID.randomUUID();
        UUID userB = UUID.randomUUID();

        walletService.addWallet(new Wallet(userA, new BigDecimal("1000.00")));
        walletService.addWallet(new Wallet(userB, new BigDecimal("1000.00")));

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(iterations * 2);

        // When
        for (int i = 0; i < iterations; i++) {
            executorService.submit(() -> {
                try {
                    startLatch.await();
                    walletService.transfer(UUID.randomUUID(), userA, userB, new BigDecimal("1.00"));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endLatch.countDown();
                }
            });

            executorService.submit(() -> {
                try {
                    startLatch.await();
                    walletService.transfer(UUID.randomUUID(), userB, userA, new BigDecimal("1.00"));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean finished = endLatch.await(10, TimeUnit.SECONDS);

        assertTrue(finished, "Test timed out! Possible Deadlock detected.");

        BigDecimal balanceA = walletService.getBalance(userA);
        BigDecimal balanceB = walletService.getBalance(userB);

        assertEquals(new BigDecimal("1000.00"), balanceA, "Final balance for User A is incorrect");
        assertEquals(new BigDecimal("1000.00"), balanceB, "Final balance for User B is incorrect");

        System.out.println("Final Balance A: " + balanceA);
        System.out.println("Final Balance B: " + balanceB);
    }
}