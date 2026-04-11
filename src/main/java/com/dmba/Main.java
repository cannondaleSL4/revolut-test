package com.dmba;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

public class Main {
    public static void main(String[] args) {
    }
}

class CashbackProcessor {

    private final Map<UUID, Boolean> processedTransactions = new ConcurrentHashMap<>();
    private final Map<UUID, LongAdder> monthlyAccruals = new ConcurrentHashMap<>();

    private static final long MAX_MONTHLY_BONUS = 3000_00L;
    private static final double CASHBACK_RATE = 0.05;

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
        monthlyAccruals.compute(userId, (id, currentTotal) -> {
            if (currentTotal == null) {
                currentTotal = new LongAdder();
            }

            long currentSum = currentTotal.sum();

            if (currentSum >= MAX_MONTHLY_BONUS) {
                return currentTotal;
            }

            long actualAccrual = Math.min(amount, MAX_MONTHLY_BONUS - currentSum);
            currentTotal.add(actualAccrual);

            saveToDb(userId, actualAccrual);
            return currentTotal;
        });
    }

    private long calculateBonus(BigDecimal amount) {
        return amount.multiply(BigDecimal.valueOf(CASHBACK_RATE))
                .multiply(BigDecimal.valueOf(100))
                .longValue();
    }

    public void saveToDb(UUID userId, long amount) {
        System.out.printf("User %s earned %d units\n", userId, amount);
    }
}

record TransactionEvent(UUID txId, UUID userId, BigDecimal amount, String mcc) {}