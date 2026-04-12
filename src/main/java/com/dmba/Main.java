package com.dmba;


import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class Main {
    public static void main(String[] args) {
    }
}

class Wallet {

    @Getter
    private final UUID userId;

    @Getter
    @Setter
    private BigDecimal balance;
    public final ReentrantLock lock = new ReentrantLock();

    public Wallet(UUID userId, BigDecimal balance) {
        this.userId = userId;
        this.balance = balance;
    }

    public UUID getUserId() { return userId; }
}

class WalletService {
    private final Map<UUID, Wallet> wallets = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> processedRequests = new ConcurrentHashMap<>();

    public void addWallet(Wallet wallet) {
        wallets.put(wallet.getUserId(), wallet);
    }

    public void transfer(UUID requestId, UUID fromId, UUID toId, BigDecimal amount) {
        if (processedRequests.putIfAbsent(requestId, Boolean.TRUE) != null) {
            return;
        }

        if (fromId.equals(toId)) {
            return;
        }

        Wallet from = wallets.get(fromId);
        Wallet to = wallets.get(toId);

        if (from == null || to == null) {
            processedRequests.remove(requestId);
            throw new IllegalArgumentException("Wallet not found");
        }

        Wallet firstLock = fromId.compareTo(toId) < 0 ? from : to;
        Wallet secondLock = fromId.compareTo(toId) < 0 ? to : from;

        firstLock.lock.lock();
        try {
            secondLock.lock.lock();
            try {
                if (from.getBalance().compareTo(amount) < 0) {
                    processedRequests.remove(requestId);
                    throw new IllegalStateException("Insufficient funds for user: " + fromId);
                }

                from.setBalance(from.getBalance().subtract(amount));
                to.setBalance(to.getBalance().add(amount));

            } finally {
                secondLock.lock.unlock();
            }
        } finally {
            firstLock.lock.unlock();
        }
    }

    public BigDecimal getBalance(UUID userId) {
        Wallet w = wallets.get(userId);
        return (w != null) ? w.getBalance() : BigDecimal.ZERO;
    }
}