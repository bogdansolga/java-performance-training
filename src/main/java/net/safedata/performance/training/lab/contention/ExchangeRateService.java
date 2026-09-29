package net.safedata.performance.training.lab.contention;

import net.safedata.performance.training.RunProfiles;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.LockSupport;

@Service
@Profile(RunProfiles.LOCK_CONTENTION)
public class ExchangeRateService {

    private static final long COMPLIANCE_CHECK_NANOS = Duration.ofMillis(10).toNanos();

    private final Map<String, BigDecimal> rates = new HashMap<>();
    private final LongAdder conversions = new LongAdder();

    // One lock keeps the rate cache consistent.
    public synchronized BigDecimal convert(String currency, BigDecimal amount) {
        BigDecimal rate = rates.computeIfAbsent(currency, ExchangeRateService::loadRate);
        complianceCheck();
        conversions.increment();
        return amount.multiply(rate).setScale(2, RoundingMode.HALF_EVEN);
    }

    public long conversions() {
        return conversions.sum();
    }

    private static BigDecimal loadRate(String currency) {
        return BigDecimal.valueOf(1 + Math.floorMod(currency.hashCode(), 400) / 100.0);
    }

    // Stands in for a remote compliance service: blocks, burns no CPU.
    private static void complianceCheck() {
        LockSupport.parkNanos(COMPLIANCE_CHECK_NANOS);
    }
}
