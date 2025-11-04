package com.dmba;

import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Main {
    public static void main(String[] args) {

    }
}

interface LoadBalancer {
    Optional<Backend> getRequest();
    void releaseRequest(@NotNull Backend backend);

}

interface ServiceRegistration {
    void addServer(@NotNull Backend backend);
    void removerServer(@NotNull Backend backend);
}

interface Strategy {
    Optional<Backend> getBackend(@NotNull List<Backend> backendList);
}


class Backend {

    private final String id;

    private final AtomicInteger connection = new AtomicInteger();

    public Backend(String id) {
        this.id = id;
    }

    public AtomicInteger getConnection() {
        return connection;
    }

    public void releaseConnection() {
        this.connection.decrementAndGet();
    }

    public void increaseConnection() {
        this.connection.getAndIncrement();
    }
}

class RandoRobinStrategy implements Strategy {

    private final AtomicInteger counter = new AtomicInteger();

    @Override
    public Optional<Backend> getBackend(@NotNull List<Backend> backendList) {
        if (backendList.isEmpty()) {
            return Optional.empty();
        }
        int size = backendList.size();

        int index = counter.incrementAndGet() % size;

        return Optional.of(backendList.get(index));
    }
}

class LoadBalancerService implements LoadBalancer, ServiceRegistration {


    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private final List<Backend> backends;

    private final Strategy strategy;

    public LoadBalancerService(Collection<Backend> backends, Strategy strategy) {

        if (backends == null || backends.isEmpty()) {
            throw  new IllegalArgumentException("Server collection is corrupted");
        }

        if (strategy == null) {
            throw  new IllegalArgumentException("strategy should be specified");
        }


        this.backends = new ArrayList<>(backends);
        this.strategy = strategy;
    }


    @Override
    public void addServer(@NotNull Backend backend) {
        if (backend == null) {
            throw  new IllegalArgumentException("Server should be specified");
        }

        this.lock.writeLock().lock();
        try {
            this.backends.add(backend);
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    @Override
    public void removerServer(@NotNull Backend backend) {
        if (backend == null) {
            throw  new IllegalArgumentException("Server should be specified");
        }

        this.lock.writeLock().lock();
        try {
            this.backends.remove(backend);
        } finally {
            this.lock.writeLock().unlock();
        }

    }

    @Override
    public Optional<Backend> getRequest() {

        this.lock.readLock().lock();
        try {
            Optional<Backend> backend = this.strategy.getBackend(backends);

            backend.ifPresent(Backend::increaseConnection);

            return backend;
        } finally {
            this.lock.readLock().unlock();
        }
    }

    @Override
    public void releaseRequest(@NotNull Backend backend) {
        if (backend == null) {
            throw  new IllegalArgumentException("Server should be specified");
        }
        backend.releaseConnection();
    }
}
