package com.dmba;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class LoadBalancerServiceConcurrencyTest {

    public LoadBalancerService loadBalancerService;
    public Strategy strategy;

    int totalRequests = 10000;

    private final ExecutorService executor = Executors.newFixedThreadPool(10);

    @BeforeEach
    void setUp() {
        strategy = new RandomRobin();
        List<Backend> initialBackends = new ArrayList<>();
        loadBalancerService = new LoadBalancerService(initialBackends, strategy);
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    @Test
    void shouldHandleConcurrentReadsWithoutErrors() throws InterruptedException {
        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch doneSignal = new CountDownLatch(totalRequests);

        Backend b1 = new Backend("b1");
        Backend b2 = new Backend("b2");

        loadBalancerService.addServer(b1);
        loadBalancerService.addServer(b2);

        for (int i = 0; i < totalRequests; i++) {
            executor.submit(() -> {
                try {
                    startSignal.await();
                    loadBalancerService.getRequest()
                            .ifPresent(loadBalancerService::releaseRequest);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneSignal.countDown();
                }
            });
        }

        startSignal.countDown();

        boolean allFinished = doneSignal.await(5, TimeUnit.SECONDS);

        assertTrue(allFinished, "Timeout occurred! Potential deadlock during concurrent reads.");
        assertEquals(0, b1.getConnections().get(), "Connection counter for B1 should be zero.");
        assertEquals(0, b2.getConnections().get(), "Connection counter for B2 should be zero.");
    }

    @Test
    void shouldReturnEmptyOptionalWhenNoServersAreRegistered() throws InterruptedException {

        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch doneSignal = new CountDownLatch(totalRequests);

        AtomicInteger emptyResponses = new AtomicInteger(0);

        for (int i = 0; i < totalRequests; i++) {
            executor.submit(() -> {
                try {
                    startSignal.await();

                    Optional<Backend> result = loadBalancerService.getRequest();

                    if (result.isEmpty()) {
                        emptyResponses.incrementAndGet();
                    } else {
                        loadBalancerService.releaseRequest(result.get());
                    }

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneSignal.countDown();
                }
            });
        }

        startSignal.countDown();

        boolean allFinished = doneSignal.await(5, TimeUnit.SECONDS);

        assertTrue(allFinished, "Timeout occurred! Threads should not deadlock.");
        assertDoesNotThrow(() -> allFinished);

        assertEquals(totalRequests, emptyResponses.get(),
                "All requests should return an empty Optional when no servers are available.");
    }
}