package net.safedata.performance.training.lab.common;

import java.lang.management.ManagementFactory;

public final class LabMemory {

    private LabMemory() {
    }

    /** Heap in use after explicit full GC: the retained set, not garbage. */
    public static long heapUsedAfterGcMb() {
        System.gc();
        return ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed() / (1024 * 1024);
    }
}
