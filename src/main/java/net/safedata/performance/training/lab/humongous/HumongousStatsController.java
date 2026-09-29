package net.safedata.performance.training.lab.humongous;

import com.sun.management.GarbageCollectionNotificationInfo;
import jakarta.annotation.PostConstruct;
import net.safedata.performance.training.RunProfiles;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import javax.management.NotificationEmitter;
import javax.management.openmbean.CompositeData;
import java.lang.management.ManagementFactory;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

// Load goes to GET /product/humongous; this counts the collections it caused.
@RestController
@Profile(RunProfiles.HUMONGOUS_ALLOCATIONS)
@RequestMapping("/lab/humongous")
public class HumongousStatsController {
    private static final String HUMONGOUS_CAUSE = "G1 Humongous Allocation";
    private final AtomicLong humongousGcEvents = new AtomicLong();
    @PostConstruct
    void countHumongousCollections() {
        ManagementFactory.getGarbageCollectorMXBeans().stream()
                .filter(NotificationEmitter.class::isInstance)
                .forEach(bean -> ((NotificationEmitter) bean).addNotificationListener((n, h) -> {
                    var info = GarbageCollectionNotificationInfo.from((CompositeData) n.getUserData());
                    if (HUMONGOUS_CAUSE.equals(info.getGcCause())) {
                        humongousGcEvents.incrementAndGet();
                    }
                }, n -> GarbageCollectionNotificationInfo.GARBAGE_COLLECTION_NOTIFICATION.equals(n.getType()), null));
    }

    @GetMapping("/stats")
    public Map<String, Long> stats() {
        return Map.of("humongousGcEvents", humongousGcEvents.get());
    }
}
