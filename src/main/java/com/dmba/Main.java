package com.dmba;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Main {
    public static void main(String[] args) {
        Backend server1 = new Backend("Server-A");
        Backend server2 = new Backend("Server-B");
        Backend server3 = new Backend("Server-C");

        List<Backend> backendList = new ArrayList<>();
        backendList.add(server1);
        backendList.add(server2);
        backendList.add(server3);

        SelectionStrategy lcStrategy = new LeastConnections();
        LoadBalancerService lcBalancer = new LoadBalancerService(backendList, lcStrategy);

        System.out.println("--- Least Connections (LC) Demonstration ---");

        System.out.println("Step 1: Sending 10 requests.");
        for (int i = 0; i < 10; i++) {
            Backend selected = lcBalancer.getRequest();

            if (i < 4) {
                lcBalancer.releaseRequest(selected);
            }
            System.out.printf("Request %d -> %s (Connections: %d)\n",
                    i,
                    selected.getId(),
                    selected.getActiveConnections().get());
        }

        System.out.println("\n--- Overview after 10 requests (4 released) ---");
        System.out.printf("A: %d, B: %d, C: %d\n",
                server1.getActiveConnections().get(),
                server2.getActiveConnections().get(),
                server3.getActiveConnections().get());

        System.out.println("\nStep 2: Choosing next (should select the least busy)");
        Backend next = lcBalancer.getRequest();
        System.out.printf("Next Request -> %s (Connections: %d)\n",
                next.getId(),
                next.getActiveConnections().get());

        System.out.println("\n--- Random Strategy Demonstration ---");

        SelectionStrategy rsStrategy = new RandomStrategy();
        LoadBalancerService rsBalancer = new LoadBalancerService(backendList, rsStrategy);

        System.out.println("Sending 3 requests via RandomStrategy:");
        rsBalancer.getRequest();
        rsBalancer.getRequest();
        rsBalancer.getRequest();

        System.out.printf("A: %d, B: %d, C: %d\n",
                server1.getActiveConnections().get(),
                server2.getActiveConnections().get(),
                server3.getActiveConnections().get());
    }
}

interface LoadBalancer {
    Backend getRequest();
    void releaseRequest(Backend backend);
}

interface SelectionStrategy {
    Backend getBackend(List<Backend> backends);
}

class LeastConnections implements SelectionStrategy {

    @Override
    public Backend getBackend(List<Backend> backends) {
        if (backends == null || backends.isEmpty()) {
            throw new IllegalStateException("Backend list cannot be null or empty.");
        }
        return backends.stream().min(Comparator.comparingInt(b -> b.getActiveConnections().get())).orElseThrow(() -> new IllegalArgumentException("Backend list corruption."));
    }
}

class RandomStrategy implements SelectionStrategy {

    @Override
    public Backend getBackend(List<Backend> backends) {
        int index = ThreadLocalRandom.current().nextInt(backends.size());
        return backends.get(index);
    }
}

class LoadBalancerService  implements LoadBalancer{

    public List<Backend> backendList;

    public final SelectionStrategy strategy;

    ReentrantReadWriteLock reentrantReadWriteLock = new ReentrantReadWriteLock();

    public LoadBalancerService(List<Backend> backendList, SelectionStrategy strategy) {

        if (backendList == null || backendList.isEmpty()) {
            throw new IllegalStateException("Backend list cannot be null or empty.");
        }

        this.backendList = backendList;
        this.strategy = strategy;
    }

    @Override
    public Backend getRequest() {
        try {
            this.reentrantReadWriteLock.readLock().lock();

            Backend selected =  strategy.getBackend(backendList);

            selected.getConnection();

            return selected;

        } finally {
            this.reentrantReadWriteLock.readLock().unlock();
        }
    }

    @Override
    public void releaseRequest(Backend backend) {
        backend.releaseConnection();
    }
}

class Backend {

    private final String id;

    public Backend(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public AtomicInteger activeConnections = new AtomicInteger(0);

    public AtomicInteger getActiveConnections() {
        return activeConnections;
    }

    public void getConnection() {
        activeConnections.incrementAndGet();
    }

    public void releaseConnection() {
        activeConnections.decrementAndGet();
    }
}