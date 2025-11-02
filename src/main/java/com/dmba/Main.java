package com.dmba;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Main {
    public static void main(String[] args) {

    }
}

interface LoadBalancer {
    Backend getRequest();
    void releaseRequest(Backend backend);
}

interface Strategy {
    Backend getBackend(List<Backend> list);
}

class RandomStrategy implements Strategy {
    @Override
    public Backend getBackend(List<Backend> list) {
        return list.get(ThreadLocalRandom.current().nextInt(list.size()));
    }
}

class LeastConnectionsStrategy implements Strategy {

    @Override
    public Backend getBackend(List<Backend> list) {
        return list.stream().min(Comparator.comparingInt(x -> x.getActiveConnections().get())).orElseThrow(() -> new IllegalStateException("No backends available"));
    }
}

class LoadBalancerService implements LoadBalancer {

    private List<Backend> backendList;
    private final Strategy strategy;
    private final ReentrantReadWriteLock lock;

    public LoadBalancerService(Strategy strategy, List<Backend> backendList) {
        if (backendList == null || backendList.isEmpty()) {
            throw  new IllegalArgumentException("List of backends cannot be null or empty");
        }
        this.strategy = strategy;
        this.lock = new ReentrantReadWriteLock();
        this.backendList = backendList;
    }

    @Override
    public Backend getRequest() {
        this.lock.readLock().lock();
        try{
            var backend = this.strategy.getBackend(this.backendList);
            backend.getConnection();
            return backend;
        } finally {
            this.lock.readLock().unlock();
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

    public AtomicInteger getActiveConnections() {
        return activeConnections;
    }

    private AtomicInteger activeConnections  = new AtomicInteger(0);

    public void getConnection() {
        activeConnections.incrementAndGet();
    }

    public void releaseConnection() {
        activeConnections.decrementAndGet();
    }

}
