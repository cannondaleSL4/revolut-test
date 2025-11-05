package com.dmba;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Main {
    public static void main(String[] args) {

    }
}

interface Strategy {
    Optional<Backend> getBackend(@NotNull List<Backend> backends);
}

interface ServiceRegistration {
    void addServer(@NotNull Backend backend);
    void removeServer(@NotNull Backend backend);
}

interface LoadBalancer  {
    Optional<Backend> getRequest();
    void releaseRequest(@NotNull Backend backend);
}

class RandomRobin implements Strategy {

    private final AtomicInteger counter = new AtomicInteger();

    @Override
    public Optional<Backend> getBackend(@NotNull List<Backend> backends) {

        if (backends == null) {
            throw new IllegalArgumentException("List should not be null");
        }

        if ( backends.isEmpty()) {
            return Optional.empty();
        }

        int size = backends.size();

        int index = counter.getAndIncrement() % size;

        return Optional.of(backends.get(index));
    }
}

class Backend {

    private final AtomicInteger connections = new AtomicInteger();

    private final String id;

    public Backend(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public AtomicInteger getConnections() {
        return connections;
    }

    public void getConnection() {
        this.connections.incrementAndGet();
    }

    public void releaseConnection() {
        this.connections.decrementAndGet();
    }
}

class LoadBalancerService implements LoadBalancer, ServiceRegistration {

    private final ReentrantReadWriteLock lock  = new ReentrantReadWriteLock();

    private final Strategy strategy;

    private final List<Backend> backends;

    public LoadBalancerService(Strategy strategy, Collection<Backend> backends) {

        if (backends== null) {
            throw new IllegalArgumentException("Collection of backends should not be null");
        }

        if (strategy== null) {
            throw new IllegalArgumentException("Strategy should not be null");
        }

        this.strategy = strategy;
        this.backends = new ArrayList<>(backends);
    }

    @Override
    public Optional<Backend> getRequest() {
        this.lock.readLock().lock();
       try {
           Optional<Backend> result = strategy.getBackend(this.backends);
           if (result.isPresent()) {
               result.get().getConnection();
           } else {
               return Optional.empty();
           }
           return result;
       } finally {
           this.lock.readLock().unlock();
       }
    }

    @Override
    public void releaseRequest(@NotNull Backend backend) {
        backend.releaseConnection();
    }


    //trade-of of performance should be O(1)
    @Override
    public void addServer(@NotNull Backend backend) {

        if(backend == null) {
            throw new IllegalArgumentException("backend should not be null for adding");
        }

        this.lock.writeLock().lock();
        try {
            if (!backends.contains(backend)) {
                backends.add(backend);
            }
        } finally {
            this.lock.writeLock().unlock();
        }
    }


    //trade-of of performance should be O(1)
    @Override
    public void removeServer(@NotNull Backend backend) {
        if(backend == null) {
            throw new IllegalArgumentException("backend should not be null for remove");
        }
        this.lock.writeLock().lock();
        try {
            backends.remove(backend);
        } finally {
            this.lock.writeLock().unlock();
        }
    }
}
