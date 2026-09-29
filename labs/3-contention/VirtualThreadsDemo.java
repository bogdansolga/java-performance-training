import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Same blocking workload three ways. Run on JDK 21, then on 25:
 * the third line is slow on 21 (a virtual thread blocking inside synchronized pins its carrier)
 * and fast on 24+ (no pinning there).
 */
public class VirtualThreadsDemo {
    private static final int TASKS = 2_000;
    private static final Duration BLOCKING_CALL = Duration.ofMillis(50);

    public static void main(String[] args) throws Exception {
        System.out.println("JDK " + Runtime.version() + ", " + Runtime.getRuntime().availableProcessors() + " CPUs");
        run("fixed pool of 50 platform threads", Executors.newFixedThreadPool(50), false);
        run("one virtual thread per task", Executors.newVirtualThreadPerTaskExecutor(), false);
        run("virtual threads, blocking inside synchronized", Executors.newVirtualThreadPerTaskExecutor(), true);
    }

    private static void run(String label, ExecutorService executor, boolean insideSynchronized) {
        long start = System.nanoTime();
        try (executor) {
            for (int i = 0; i < TASKS; i++) {
                Object lock = new Object();  // one lock per task: no contention, only pinning
                executor.submit(() -> {
                    if (insideSynchronized) {
                        synchronized (lock) {
                            block();
                        }
                    } else {
                        block();
                    }
                });
            }
        }
        System.out.printf("%-50s %6d ms%n", label, Duration.ofNanos(System.nanoTime() - start).toMillis());
    }

    private static void block() {
        try {
            Thread.sleep(BLOCKING_CALL);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
