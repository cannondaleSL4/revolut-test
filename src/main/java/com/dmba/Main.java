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

record TransactionEvent(UUID txId, UUID userId, BigDecimal amount, String mcc){}

class CashBackProcessor {

    private final Map<UUID, Boolean> processedTransaction = new ConcurrentHashMap<>();
    private final Map<UUID, AtomicLong>  monthlyAccruals = new ConcurrentHashMap<>();

    private final long maxMonthlyBonus;
    private final BigDecimal cashBackRate;

    public CashBackProcessor(long maxMonthlyBonus, BigDecimal cashBackRate) {
        this.maxMonthlyBonus = maxMonthlyBonus;
        this.cashBackRate = cashBackRate;
    }

    public void onTransactionReceived(TransactionEvent event) {
        if (processedTransaction.putIfAbsent(event.txId(), Boolean.TRUE) != null) {
            return;
        }

        try {
            long bonusCandidate = calculateBonus(event.amount());
            accrueWithLimit(event.userId(), bonusCandidate);
        } catch (Exception e) {
            processedTransaction.remove(event.txId());
            throw e;
        }
    }

    private void accrueWithLimit(UUID userId, long amount) {
        AtomicLong currentTotal = monthlyAccruals.computeIfAbsent(userId, k -> new AtomicLong(0));
        while (true) {
            long currentSum = currentTotal.get();
            if (currentSum >= maxMonthlyBonus) return;

            long canAdd = Math.min(amount, maxMonthlyBonus - currentSum);
            long nextSum = currentSum + canAdd;

            if (currentTotal.compareAndSet(currentSum, nextSum)) {
                break;
            }
        }
    }

    private long calculateBonus(BigDecimal amount) {
        return amount.multiply(cashBackRate)
                .multiply(BigDecimal.valueOf(100))
                .longValue();
    }

    public Map<UUID, Boolean> getProcessedTransaction() {
        return processedTransaction;
    }

    public Map<UUID, AtomicLong> getMonthlyAccruals() {
        return monthlyAccruals;
    }
}