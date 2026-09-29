package net.safedata.performance.training.lab.nplus1;

import net.safedata.performance.training.RunProfiles;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@Profile(RunProfiles.N_PLUS_ONE_QUERIES)
@RequestMapping("/lab/nplus1")
public class StoreSummaryController {

    private final StoreSummaryService storeSummaryService;
    private final Statistics statistics;

    public StoreSummaryController(StoreSummaryService storeSummaryService, EntityManagerFactory entityManagerFactory) {
        this.storeSummaryService = storeSummaryService;
        this.statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
    }

    @GetMapping("/stores")
    public List<StoreSummary> stores() {
        return storeSummaryService.summaries();
    }

    /** Serves exactly one request and reports how many SQL statements it prepared. */
    @GetMapping("/stats")
    public Map<String, Long> stats() {
        statistics.clear();
        storeSummaryService.summaries();
        return Map.of("sqlStatementsPerRequest", statistics.getPrepareStatementCount());
    }
}
