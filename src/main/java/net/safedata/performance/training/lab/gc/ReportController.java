package net.safedata.performance.training.lab.gc;

import net.safedata.performance.training.RunProfiles;
import net.safedata.performance.training.lab.common.GcCounters;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@Profile(RunProfiles.GC_MISMATCH)
@RequestMapping("/lab/gc")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/report")
    public ReportService.Report report() {
        return reportService.report();
    }

    /** Collections and collection time per collector since startup. */
    @GetMapping("/stats")
    public Map<String, Long> stats() {
        return GcCounters.snapshot();
    }
}
