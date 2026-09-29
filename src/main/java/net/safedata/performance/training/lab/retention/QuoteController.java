package net.safedata.performance.training.lab.retention;

import net.safedata.performance.training.RunProfiles;
import net.safedata.performance.training.lab.common.LabMemory;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@Profile(RunProfiles.UNBOUNDED_RETENTION)
@RequestMapping("/lab/retention")
public class QuoteController {

    private final QuoteService quoteService;

    public QuoteController(QuoteService quoteService) {
        this.quoteService = quoteService;
    }

    @GetMapping("/quote/{sku}")
    public QuoteResponse quote(@PathVariable String sku) {
        Quote quote = quoteService.quote(sku);
        return new QuoteResponse(quote.id(), quote.sku(), quote.price());
    }

    @GetMapping("/stats")
    public Map<String, Long> stats() {
        return Map.of("retainedQuotes", (long) quoteService.auditedQuotes(),
                "heapUsedAfterGcMb", LabMemory.heapUsedAfterGcMb());
    }
}
