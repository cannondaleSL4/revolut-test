package com.dmba;

import org.jetbrains.annotations.NotNull;

import javax.swing.text.html.Option;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Main {
    public static void main(String[] args) {

    }
}


interface Strategy {
    Optional<Backend> selectBackend(@NotNull List<Backend> backendList);
}

interface RegisterBackend {
    void addServer(@NotNull Backend backend);
    void removeServer(@NotNull Backend backend);
}

interface LoadBalancer {
    Optional<Backend> getConnection();
    void releaseConnection(@NotNull Backend backend);
}

class LoadBalancerService implements LoadBalancer, RegisterBackend {

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public LoadBalancerService(@NotNull Strategy strategy, @NotNull List<Backend> backends) {

        if (strategy == null) {
            throw new IllegalArgumentException("Strategy should not be bull");
        }

        if (backends == null) {
            throw new IllegalArgumentException("Backend list should not be null");
        }

        this.strategy = strategy;
        this.backends = backends;
    }

    private final Strategy strategy;
    private final List<Backend> backends;

    @Override
    public Optional<Backend> getConnection() {

        if (backends.isEmpty()) {
            return Optional.empty();
        }

        lock.readLock().lock();
        try {

            Optional<Backend> result = strategy.selectBackend(backends);

            result.ifPresent(Backend::addConnection);

            return result;

        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void releaseConnection(@NotNull Backend backend) {
        if (backend == null) {
            throw new IllegalArgumentException("Backend should not be null");
        }

        backend.releaseConnection();
    }

    @Override
    public void addServer(@NotNull Backend backend) {

        if (backend == null) {
            throw new IllegalArgumentException("Backend should not be null");
        }

        lock.writeLock().lock();
        try {
            if (!backends.contains(backend)) {
                backends.add(backend);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void removeServer(@NotNull Backend backend) {

        if (backend == null) {
            throw new IllegalArgumentException("Backend should not be null");
        }


        lock.writeLock().lock();
        try {
            backends.remove(backend);
        } finally {
            lock.writeLock().unlock();
        }
    }
}

class RandomRobin implements Strategy {

    private final AtomicInteger counter = new AtomicInteger();

    @Override
    public Optional<Backend> selectBackend(@NotNull List<Backend> backendList) {
        if (backendList == null || backendList.isEmpty()) {
            throw new IllegalArgumentException("List of backend should not be null or empty");
        }
        int size = backendList.size();

        int index = counter.incrementAndGet() % size;

        return Optional.of(backendList.get(index));
    }
}

class Backend {

    private final String id;

    public AtomicInteger getAtomicInteger() {
        return atomicInteger;
    }

    private final AtomicInteger atomicInteger = new AtomicInteger();


    Backend(String id) {
        this.id = id;
    }

    public void addConnection() {
        atomicInteger.incrementAndGet();
    }

    public void releaseConnection() {
        atomicInteger.decrementAndGet();
    }
}