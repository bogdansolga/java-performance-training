package net.safedata.performance.training.lab.common;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public final class GcCounters {

    private GcCounters() {
    }

    /**
     * Collections and accumulated collection time so far, per collector:
     * {@code gc.<bean name lowercased, non-alphanumerics → _>.count} and {@code .timeMs}.
     */
    public static Map<String, Long> snapshot() {
        Map<String, Long> snapshot = new TreeMap<>();
        for (GarbageCollectorMXBean bean : ManagementFactory.getGarbageCollectorMXBeans()) {
            String key = "gc." + bean.getName().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "_");
            snapshot.put(key + ".count", Math.max(0, bean.getCollectionCount()));
            snapshot.put(key + ".timeMs", Math.max(0, bean.getCollectionTime()));
        }
        return snapshot;
    }
}
