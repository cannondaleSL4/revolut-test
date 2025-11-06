package com.dmba;

import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Main {
    public static void main(String[] args) {

    }
}

interface Strategy {
    Optional<Backend> selectBackend(@NotNull List<Backend> backends);
}

class LessConnection implements Strategy {
    @Override
    public Optional<Backend> selectBackend(@NotNull List<Backend> backends) {
        return backends.stream().min(Comparator.comparingInt(x -> x.getConnections().get()));
    }
}

interface RegisterBackend {
    void addServer(@NotNull Backend backend);
    void removeServer(@NotNull Backend backend);
}

interface LoadBalancer {
    void releaseConnection(@NotNull Backend backend);
    Optional<Backend> getConnection();
}

class LoadBalancerService implements  RegisterBackend, LoadBalancer {
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private final List<Backend> backends;

    private Strategy strategy;

    public LoadBalancerService(@NotNull Collection<Backend> backends, @NotNull Strategy strategy) {

        if (backends == null) {
            throw  new IllegalArgumentException("List of backends should not be null");
        }

        if (strategy == null) {
            throw  new IllegalArgumentException("Strategy should not be null");
        }

        this.strategy = strategy;
        this.backends = new ArrayList<>(backends);
    }

    public LoadBalancerService(@NotNull Collection<Backend> backends) {

        if (backends == null) {
            throw  new IllegalArgumentException("List of backends should not be null");
        }

        this.strategy = new LessConnection();
        this.backends = new ArrayList<>(backends);
    }


    @Override
    public void releaseConnection(@NotNull Backend backend) {
        backend.releaseConnection();
    }

    @Override
    public Optional<Backend> getConnection() {
        lock.readLock().lock();
        try {
            Optional<Backend> result = strategy.selectBackend(this.backends);
            result.ifPresent(Backend::getConnection);
            return result;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void addServer(@NotNull Backend backend) {
        if (backend == null) {
            throw  new IllegalArgumentException("Backend should not be null");
        }

        lock.writeLock().lock();
        try {
            if (!backends.contains(backend)) {
                this.backends.add(backend);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void removeServer(@NotNull Backend backend) {
        if (backend == null) {
            throw  new IllegalArgumentException("Backend should not be null");
        }
        lock.writeLock().lock();
        try {
            this.backends.remove(backend);
        } finally {
            lock.writeLock().unlock();
        }
    }
}


class Backend {

    private final String id;

    public Backend(@NotNull String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Id of the server could not be null or empty");
        }
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public AtomicInteger getConnections() {
        return connections;
    }

    private final AtomicInteger connections = new AtomicInteger();

    public void getConnection() {
        this.connections.incrementAndGet();
    }

    public void releaseConnection() {
        this.connections.decrementAndGet();
    }
}