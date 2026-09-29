package net.safedata.performance.training.lab.gc;

import net.safedata.performance.training.RunProfiles;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
@Profile(RunProfiles.GC_MISMATCH)
public class ReportService {

    static final int ROWS_PER_REPORT = 5_000;
    static final int RETAINED_REPORTS = 2_500;

    // The most recent reports, kept for re-download.
    private final Deque<byte[]> recentReports = new ArrayDeque<>();

    public Report report() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        List<Row> rows = new ArrayList<>(ROWS_PER_REPORT);
        for (int i = 0; i < ROWS_PER_REPORT; i++) {
            rows.add(new Row(i, "account-" + random.nextInt(1_000_000), random.nextDouble() * 10_000));
        }
        byte[] csv = rows.stream()
                .map(Row::toCsv)
                .collect(Collectors.joining("\n", "id,account,amount\n", "\n"))
                .getBytes(StandardCharsets.UTF_8);
        synchronized (recentReports) {
            recentReports.addLast(csv);
            if (recentReports.size() > RETAINED_REPORTS) {
                recentReports.removeFirst();
            }
        }
        return new Report(rows.size(), csv.length);
    }

    public int retainedReports() {
        synchronized (recentReports) {
            return recentReports.size();
        }
    }

    public record Report(int rows, long bytes) {
    }

    record Row(int id, String account, double amount) {
        String toCsv() {
            return id + "," + account + "," + amount;
        }
    }
}
