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
    private final AtomicInteger sessions;

    public Backend(@NotNull String id) {
        if (id.isBlank()) {
            throw new IllegalArgumentException("Backend's id should be determined.");
        }
        this.id = id;
        this.sessions = new AtomicInteger(0);
    }

    public String getId() {
        return id;
    }

    public AtomicInteger getSessions() {
        return sessions;
    }

    public void addConnection() {
        sessions.incrementAndGet();
    }

    public void decreaseConnection() {
        sessions.decrementAndGet();
    }

}

interface LoadBalancer {
    Backend getRequest();
    void releaseRequest(@NotNull Backend backend);
}

interface Strategy {
    Backend getBackend(@NotNull List<Backend> backends);
}

class LessConnections implements Strategy {

    @Override
    public Backend getBackend(@NotNull List<Backend> backends) {
        return backends
                .stream()
                .min(Comparator.comparingInt(backend -> backend.getSessions().get())).orElseThrow(() -> new IllegalStateException("List of backends corrupt."));
    }
}

class Random implements Strategy {

    @Override
    public Backend getBackend(@NotNull List<Backend> backends) {
        return backends.get(ThreadLocalRandom.current().nextInt(backends.size()));
    }
}

class RoundRobinStrategy implements Strategy {

    private final AtomicInteger counter = new AtomicInteger(0);

    @Override
    public Backend getBackend(@NotNull List<Backend> backends) {
        if (backends.isEmpty()) {
            throw new IllegalStateException("Backend list is empty.");
        }

        int size = backends.size();

        int currentCount = counter.getAndIncrement();

        int index = currentCount % size;

        return backends.get(index);
    }
}

class LoadBalancerService implements LoadBalancer {


    private final Strategy strategy;
    private final ReentrantReadWriteLock lock;
    private final List<Backend> backends;


    public LoadBalancerService(@NotNull Strategy strategy, @NotNull Collection<Backend> backends) {
        if (backends.isEmpty()) {
            throw new IllegalArgumentException("Backends should be determined.");
        }
        this.strategy = strategy;
        this.lock = new ReentrantReadWriteLock();
        this.backends = new ArrayList<>(backends);
    }

    @Override
    public Backend getRequest() {
        this.lock.readLock().lock();

        try {
            Backend backend = strategy.getBackend(backends);
            backend.addConnection();
            return backend;
        } finally {
            this.lock.readLock().unlock();
        }
    }

    @Override
    public void releaseRequest(@NotNull Backend backend) {
        backend.decreaseConnection();
    }
}
