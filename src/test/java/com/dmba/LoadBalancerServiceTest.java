package com.dmba;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class LoadBalancerServiceTest {


    private final int maxRequest = 10000;

    private ExecutorService executorService;
    private Strategy strategy;
    private Collection<Backend> backends;
    private LoadBalancerService loadBalancer;

    private CountDownLatch startFlag;
    private CountDownLatch endFlag;

    @BeforeEach
    void setUp() {
        executorService = Executors.newFixedThreadPool(10);
        startFlag = new CountDownLatch(1);
        endFlag = new CountDownLatch(maxRequest);
        backends = new ArrayList<>();
        strategy = new RandomRobin();
        loadBalancer = new LoadBalancerService(strategy, backends);
    }

    @AfterEach
    void tearDown() {
        this.executorService.shutdownNow();
    }

    @Test
    public void testStabilityWithParallelRunning() throws InterruptedException {


        Backend b1 = new Backend("b1");
        Backend b2 = new Backend("b2");

        loadBalancer.addServer(b1);
        loadBalancer.addServer(b2);

        for (int i = 0; i < maxRequest; i++) {
           executorService.submit(() -> {
               try {
                   startFlag.await();

                   loadBalancer.getRequest().ifPresent(loadBalancer::releaseRequest);

               } catch (InterruptedException e) {
                   Thread.currentThread().interrupt();
               } finally {
                   endFlag.countDown();
               }
           });
        }


        startFlag.countDown();


        boolean allEnd = endFlag.await(5, TimeUnit.SECONDS);


        assertTrue(allEnd);
        assertEquals(0,b1.getConnections().get());
        assertEquals(0,b2.getConnections().get());
    }

    @Test
    public void testStabilityWithParallelRunningWithoutBackends() throws InterruptedException {

        AtomicInteger counter = new AtomicInteger();

        for (int i = 0; i < maxRequest; i++) {
            executorService.submit(() -> {
                try {
                    startFlag.await();

                    Optional<Backend> result = loadBalancer.getRequest();

                    if (result.isEmpty()) {
                        counter.incrementAndGet();
                    }

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endFlag.countDown();
                }
            });
        }


        startFlag.countDown();


        boolean allEnd = endFlag.await(5, TimeUnit.SECONDS);


        assertTrue(allEnd);
        assertEquals(maxRequest,counter.get());
    }
}