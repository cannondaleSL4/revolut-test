package com.dmba;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

class LoadBalancerServiceTest {


    private CountDownLatch endFlag;
    private CountDownLatch runFlag;

    private final int maxResuest = 10000;

    private ExecutorService executorService;

    private List<Backend> backendList;

    private LoadBalancerService loadBalancerService;

    private Strategy strategy;

    @BeforeEach
    void setUp() {
        backendList = new ArrayList<>();
        executorService = Executors.newFixedThreadPool(10);
        endFlag  = new CountDownLatch(1);
        runFlag = new CountDownLatch(maxResuest);

        strategy = new RandomRobin();
        loadBalancerService = new LoadBalancerService(strategy, backendList);
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

        for (int i = 0; i < maxResuest ; i++) {
            executorService.submit(() -> {
                try {
                    endFlag.await();

                    loadBalancerService.getConnection().ifPresent(Backend::releaseConnection);

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    runFlag.countDown();
                }
            });
        }

        endFlag.countDown();

        boolean allRigth = runFlag.await(5, TimeUnit.SECONDS);

        assertTrue(allRigth);

        assertEquals(0, b1.getAtomicInteger().get());
        assertEquals(0, b2.getAtomicInteger().get());
    }
}