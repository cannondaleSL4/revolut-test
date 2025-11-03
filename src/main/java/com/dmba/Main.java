package com.dmba;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Main {
    public static void main(String[] args) {

    }
}

class Backend {

    private final String id;

    public Backend(@NotNull String id) {
        this.id = id;
    }

    private final AtomicInteger connections = new AtomicInteger();

    public AtomicInteger getConnections() {
        return connections;
    }

    public void incrementConnection() {
        connections.incrementAndGet();
    }

    public void decrementConnection() {
        connections.decrementAndGet();
    }
}

interface Strategy {
    Backend getBackend(@NotNull List<Backend> backends);
}

interface LoadBalancer {
    Backend getRequest();
    void releaseRequest(@NotNull Backend backend);
}

class RandomRobin implements Strategy {

    @Override
    public Backend getBackend(@NotNull List<Backend> backends) {
        if (backends.isEmpty()) {
            throw  new IllegalStateException("Backend collection is empty");
        }
        return backends.get(ThreadLocalRandom.current().nextInt(backends.size()));
    }
}

class RoundRobin implements Strategy {

    private final AtomicInteger counter = new AtomicInteger();

    @Override
    public Backend getBackend(@NotNull List<Backend> backends) {

        if (backends.isEmpty()) {
            throw  new IllegalStateException("Backend collection is empty");
        }

        int size  = backends.size();

        int index  = counter.getAndIncrement() % size;

        return backends.get(index);

    }
}

class LessConnection implements Strategy {

    @Override
    public Backend getBackend(@NotNull List<Backend> backends) {

        if (backends.isEmpty()) {
            throw  new IllegalStateException("Backend collection is empty");
        }

        return backends.stream().min(Comparator.comparingInt(x -> x.getConnections().get())).orElseThrow(() -> new IllegalStateException("List of backend is corrupt/"));
    }
}

class LoadBalancerService implements LoadBalancer {

    private final ReentrantReadWriteLock reentrantReadWriteLock;

    private final Strategy strategy;

    private final List<Backend> backends;


    public LoadBalancerService(@NotNull Strategy strategy, @NotNull Collection<Backend> backends) {
        this.strategy = strategy;
        if (backends.isEmpty()) {
            throw  new IllegalStateException("Backend collection is empty");
        }
        this.backends = new ArrayList<>(backends);
        this.reentrantReadWriteLock = new ReentrantReadWriteLock();

    }

    @Override
    public Backend getRequest() {
        reentrantReadWriteLock.readLock().lock();
        try {
            Backend backend = strategy.getBackend(backends);
            backend.incrementConnection();
            return backend;
        } finally {
            reentrantReadWriteLock.readLock().unlock();
        }
    }

    @Override
    public void releaseRequest(@NotNull Backend backend) {
        backend.decrementConnection();
    }
}
