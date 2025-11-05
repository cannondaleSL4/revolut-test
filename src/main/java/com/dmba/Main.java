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
    Optional<Backend> getServer(@NotNull List<Backend> backends);
}

class RandomRobin implements  Strategy {

    private final AtomicInteger counter = new AtomicInteger();

    @Override
    public Optional<Backend> getServer(@NotNull List<Backend> backends) {
        if (backends == null) {
            throw new IllegalArgumentException("Server's collection is corrupt.");
        }

        if (backends.isEmpty()) {
            return Optional.empty();
        }

        int size = backends.size();

        int index = counter.incrementAndGet() % size;

        return Optional.of(backends.get(index));
    }
}

interface LoadBalancer {
    Optional<Backend> getRequest();
    void releaseRequest(@NotNull Backend backend);
}

interface ServerRegister {
    void addServer(@NotNull Backend backend);
    void removeServer(@NotNull Backend backend);
}

class LoadBalancerService implements LoadBalancer, ServerRegister {

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private final List<Backend> backends;

    private final Strategy strategy;

    public LoadBalancerService(Collection<Backend> backends, Strategy strategy) {

        if (strategy == null) {
            throw new IllegalArgumentException("Strategy should be determined.");
        }

        if (backends == null) {
            throw new IllegalArgumentException("Backends should be determined.");
        }


        this.backends = new ArrayList<>(backends);
        this.strategy = strategy;
    }

    @Override
    public Optional<Backend> getRequest() {
        this.lock.readLock().lock();
        try{
            Optional<Backend> optionalBackend = this.strategy.getServer(this.backends);
            optionalBackend.ifPresent(Backend::increaseConnection);
            return optionalBackend;
        } finally {
            this.lock.readLock().unlock();
        }
    }

    @Override
    public void releaseRequest(@NotNull Backend backend) {
        backend.decreaseConnection();
    }

    @Override
    public void addServer(@NotNull Backend backend) {
        if (backend == null) {
            throw new IllegalArgumentException("Server should be specified.");
        }

        /*
        trade-of with unique Backends in List and O(n)
         */
        this.lock.writeLock().lock();
        try{
            this.backends.add(backend);
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    @Override
    public void removeServer(@NotNull Backend backend) {
        if (backend == null) {
            throw new IllegalArgumentException("Server should be specified.");
        }

        /*
        trade-of with unique Backends in List and O(n)
         */
        this.lock.writeLock().lock();
        try{
            this.backends.remove(backend);
        } finally {
            this.lock.writeLock().unlock();
        }

    }

    public List<Backend> getBackends() {
        return backends;
    }
}



class Backend {

    private final String id;

    private final AtomicInteger connections = new AtomicInteger();

    public Backend(String id) {
        this.id = id;
    }

    public AtomicInteger getConnections() {
        return connections;
    }

    public void increaseConnection() {
        this.connections.incrementAndGet();
    }

    public void decreaseConnection() {
        this.connections.decrementAndGet();
    }
}
