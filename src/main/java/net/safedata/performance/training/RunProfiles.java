package net.safedata.performance.training;

public final class RunProfiles {

    public static final String UNBOUNDED_RETENTION = "unbounded-retention";
    public static final String N_PLUS_ONE_QUERIES = "n-plus-one-queries";
    public static final String LOCK_CONTENTION = "lock-contention";
    public static final String GC_MISMATCH = "gc-mismatch";
    public static final String CODE_CACHE_EXHAUSTION = "code-cache-exhaustion";
    public static final String HUMONGOUS_ALLOCATIONS = "humongous-allocations";

    private RunProfiles() {
    }
}
