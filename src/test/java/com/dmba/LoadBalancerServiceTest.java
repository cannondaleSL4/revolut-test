package com.dmba;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class LoadBalancerServiceTest {

    private CountDownLatch startSignal;
    private CountDownLatch endSignal;
    private final int maxRequests = 10000;

    private ExecutorService executorService;
    private LoadBalancerService loadBalancerService;
    private Collection<Backend> backends;

    @BeforeEach
    void setUp() {
        startSignal = new CountDownLatch(1);
        endSignal = new CountDownLatch(maxRequests);
        executorService = Executors.newFixedThreadPool(10);
        backends = new ArrayList<>();
        loadBalancerService = new LoadBalancerService(backends);
    }

    @AfterEach
    void tearDown() {
        executorService.shutdownNow();
    }

    @Test
    public void testStabilityWithParallelRunning() throws InterruptedException {

        Backend b1 = new Backend("b1");
        Backend b2 = new Backend("b2");

        loadBalancerService.addServer(b1);
        loadBalancerService.addServer(b2);

        for (int i = 0; i < maxRequests; i++) {
            executorService.submit(() -> {
                try {
                    startSignal.await();


                    loadBalancerService.getConnection().ifPresent(loadBalancerService::releaseConnection);

                }  catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endSignal.countDown();
                }

            });
        }

        startSignal.countDown();

        boolean allFinished = endSignal.await(5, TimeUnit.SECONDS);

        assertTrue(allFinished);
        assertEquals(0, b1.getConnections().get());
        assertEquals(0, b2.getConnections().get());
    }
}