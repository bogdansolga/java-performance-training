package net.safedata.performance.training.lab.harness;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/** A deterministic pass criterion: the named counter must not exceed {@code max}. */
public record CountThreshold(String counter, long max) {

    static final String KEY_PREFIX = "threshold.count.";

    /** A counter that was not recorded fails. */
    public boolean passes(Map<String, Long> counters) {
        Long value = counters.get(counter);
        return value != null && value <= max;
    }

    public static List<CountThreshold> fromProperties(Properties p) {
        List<CountThreshold> result = new ArrayList<>();
        for (String key : p.stringPropertyNames().stream().sorted().toList()) {
            if (key.startsWith(KEY_PREFIX) && key.length() > KEY_PREFIX.length()) {
                result.add(new CountThreshold(key.substring(KEY_PREFIX.length()), Long.parseLong(p.getProperty(key).trim())));
            }
        }
        return result;
    }
}
