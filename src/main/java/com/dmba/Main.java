package com.dmba;


import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class Main {
    public static void main(String[] args) {
    }
}

record TransactionEvent(UUID txId, UUID userId, BigDecimal amount, String mcc) {}

class CashBackProcessor {
    private final Map<UUID, Boolean> processedTransactions = new ConcurrentHashMap<>();
    private final Map<UUID, AtomicLong> monthlyAccruals = new ConcurrentHashMap<>();

    private static final long MAX_MONTH_BONUS = 3000_00L;
    private static final BigDecimal CASHBACK_RATE = new BigDecimal("0.05");

    public void onTransactionReceived(TransactionEvent event) {
        if (processedTransactions.putIfAbsent(event.txId(), Boolean.TRUE) != null) {
            return;
        }

        try {
            long bonusCandidate = calculateBonus(event.amount());
            accrueWithLimit(event.userId(), bonusCandidate);
        } catch (Exception e) {
            processedTransactions.remove(event.txId());
            throw e;
        }
    }

    private void accrueWithLimit(UUID userId, long amount) {
        AtomicLong currentTotal = monthlyAccruals.computeIfAbsent(userId, k -> new AtomicLong(0));
        while(true) {
            long currentSum = currentTotal.get();
            if (currentSum >= MAX_MONTH_BONUS) {
                return;
            }

            long canAdd = Math.min(amount, MAX_MONTH_BONUS - currentSum);
            long nextSum = currentSum + canAdd;

            if (currentTotal.compareAndSet(currentSum, nextSum)) {
                if (canAdd > 0) {
                    saveToDb(userId, canAdd);
                }
                break;
            }
        }
    }

    public void saveToDb(UUID userId, long amount) {
        System.out.printf("User %s earned %d units\n", userId, amount);
    }

    private long calculateBonus(BigDecimal amount) {
        return amount.multiply(CASHBACK_RATE)
                .multiply(BigDecimal.valueOf(100))
                .longValue();
    }
}