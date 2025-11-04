package com.dmba;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.*;
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
    void releaseRequest(@NotNull Backend backend);
}

interface ServiceRegistration {
    boolean addServer(@NotNull Backend backend);
    boolean removerServer(@NotNull Backend backend);
}

interface Strategy {
    Backend getBackend(@NotNull List<Backend> backends);
}

class RoundRobin implements Strategy {
    
    private final AtomicInteger counter = new AtomicInteger();
    
    @Override
    public Backend getBackend(@NotNull List<Backend> backends) {
        if (backends.isEmpty()) {
            throw new IllegalStateException("LIst of backend is empty");
        }

        int size = backends.size();
        int currentCount = counter.getAndIncrement();
        int index = currentCount % size;
        return backends.get(index);
    }
}

class LessConnection implements Strategy {

    @Override
    public Backend getBackend(@NotNull List<Backend> backends) {
        return backends.stream()
                .min(Comparator.comparingInt(
                        x -> x.getConnections().get()))
                .orElseThrow(() -> new IllegalStateException("The collection of backend is corrupt"));
    }
}

class RandomRobin implements Strategy {

    @Override
    public Backend getBackend(@NotNull List<Backend> backends) {
        return backends.get(ThreadLocalRandom.current().nextInt(backends.size()));
    }
}

class Backend {
    @Getter
    private final AtomicInteger connections = new AtomicInteger();

    public void addConnection() {
        this.connections.incrementAndGet();
    }

    public void releaseConnection() {
        this.connections.decrementAndGet();
    }
}

class LoadBalancerService implements LoadBalancer, ServiceRegistration {

    private final Strategy strategy;

    private final List<Backend> backends = new ArrayList<>();

    private final ReentrantReadWriteLock serversLock  = new ReentrantReadWriteLock();

    public LoadBalancerService(Strategy strategy) {
        this.strategy = strategy;
    }


    @Override
    public Backend getRequest() {
        serversLock.readLock().lock();

        try {
            return strategy.getBackend(this.backends);
        } finally {
            serversLock.readLock().unlock();
        }
    }

    @Override
    public void releaseRequest(Backend backend) {
        backend.releaseConnection();
    }


    /* a lot of questions about imp of methods addServer and removerServer*/
    @Override
    public boolean addServer(@NotNull Backend backend) {
        serversLock.writeLock().lock();
        try{
           return this.backends.add(backend);
        } finally {
            serversLock.writeLock().unlock();
        }
    }

    @Override
    public boolean removerServer(@NotNull Backend backend) {
        serversLock.writeLock().lock();
        try{
            return this.backends.remove(backend);
        } finally {
            serversLock.writeLock().unlock();
        }
    }
}