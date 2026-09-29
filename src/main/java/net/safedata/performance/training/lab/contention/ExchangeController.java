package net.safedata.performance.training.lab.contention;

import net.safedata.performance.training.RunProfiles;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@Profile(RunProfiles.LOCK_CONTENTION)
@RequestMapping("/lab/contention")
public class ExchangeController {

    private static final long PINNED_SLEEP_MILLIS = 50;

    // Shared locks, so the monitor escapes: the JIT would elide a method-local lock and nothing would pin
    private static final Object[] PINNING_LOCKS = new Object[1024];
    private static final AtomicInteger PINNING_LOCK_COUNTER = new AtomicInteger();

    static {
        for (int i = 0; i < PINNING_LOCKS.length; i++) {
            PINNING_LOCKS[i] = new Object();
        }
    }

    private final ExchangeRateService exchangeRateService;

    public ExchangeController(ExchangeRateService exchangeRateService) {
        this.exchangeRateService = exchangeRateService;
    }

    @GetMapping("/convert/{currency}")
    public Map<String, BigDecimal> convert(@PathVariable String currency, @RequestParam BigDecimal amount) {
        return Map.of("amount", exchangeRateService.convert(currency, amount));
    }

    /**
     * Holds one of 1024 round-robin locks while it waits 50 ms. With far fewer than 1024 concurrent requests there is no contention: with
     * virtual threads enabled, what this shows is whether blocking inside synchronized pins the carrier thread.
     */
    @GetMapping("/pinned")
    public Map<String, Long> pinned() throws InterruptedException {
        Object lock = PINNING_LOCKS[Math.floorMod(PINNING_LOCK_COUNTER.getAndIncrement(), PINNING_LOCKS.length)];
        long start = System.nanoTime();
        synchronized (lock) {
            Thread.sleep(PINNED_SLEEP_MILLIS);
        }
        return Map.of("elapsedMs", Duration.ofNanos(System.nanoTime() - start).toMillis());
    }

    /** Conversions so far, and how often the HTTP worker threads have blocked on a monitor. */
    @GetMapping("/stats")
    public Map<String, Long> stats() {
        long blocked = 0;
        for (ThreadInfo info : ManagementFactory.getThreadMXBean().dumpAllThreads(false, false)) {
            if (info.getThreadName().startsWith("http-nio-")) {
                blocked += info.getBlockedCount();
            }
        }
        return Map.of("conversions", exchangeRateService.conversions(), "httpThreadsBlockedCount", blocked);
    }
}
