# Gatling NFR Analyzer Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Build a Java tool that parses Gatling PDF reports and compares performance metrics against NFR thresholds, producing PASS/WARN/FAIL verdicts.

**Architecture:** PDF parsing via Apache PDFBox extracts table data. Model classes hold NFR config and parsed results. Evaluator compares actuals vs thresholds with interpolation for missing percentiles. Console writer formats human-readable output.

**Tech Stack:** Java 21, Spring Boot 4.0, Apache PDFBox 3.0, JUnit 5

---

## Task 1: Add PDFBox Dependency

**Files:**
- Modify: `pom.xml`

**Step 1: Add PDFBox dependency to pom.xml**

Add after the existing dependencies:

```xml
<dependency>
    <groupId>org.apache.pdfbox</groupId>
    <artifactId>pdfbox</artifactId>
    <version>3.0.3</version>
</dependency>
```

**Step 2: Verify dependency resolves**

Run: `mvn dependency:resolve -q`
Expected: No errors

**Step 3: Commit**

```bash
git add pom.xml
git commit -m "build: add Apache PDFBox dependency for PDF parsing"
```

---

## Task 2: Create Result Enum

**Files:**
- Create: `src/main/java/net/safedata/performance/training/gatling/model/Result.java`
- Test: `src/test/java/net/safedata/performance/training/gatling/model/ResultTest.java`

**Step 1: Write the failing test**

```java
package net.safedata.performance.training.gatling.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ResultTest {

    @Test
    void passIsMoreSevereThanPass() {
        assertEquals(Result.PASS, Result.PASS.worseOf(Result.PASS));
    }

    @Test
    void warnIsMoreSevereThanPass() {
        assertEquals(Result.WARN, Result.PASS.worseOf(Result.WARN));
        assertEquals(Result.WARN, Result.WARN.worseOf(Result.PASS));
    }

    @Test
    void failIsMoreSevereThanWarn() {
        assertEquals(Result.FAIL, Result.WARN.worseOf(Result.FAIL));
        assertEquals(Result.FAIL, Result.FAIL.worseOf(Result.WARN));
    }

    @Test
    void failIsMoreSevereThanPass() {
        assertEquals(Result.FAIL, Result.PASS.worseOf(Result.FAIL));
    }
}
```

**Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=ResultTest -q`
Expected: Compilation error - Result class not found

**Step 3: Write minimal implementation**

```java
package net.safedata.performance.training.gatling.model;

public enum Result {
    PASS,
    WARN,
    FAIL;

    public Result worseOf(Result other) {
        return this.ordinal() >= other.ordinal() ? this : other;
    }
}
```

**Step 4: Run test to verify it passes**

Run: `mvn test -Dtest=ResultTest -q`
Expected: All 4 tests pass

**Step 5: Commit**

```bash
git add src/main/java/net/safedata/performance/training/gatling/model/Result.java \
        src/test/java/net/safedata/performance/training/gatling/model/ResultTest.java
git commit -m "feat(gatling): add Result enum with severity comparison"
```

---

## Task 3: Create LatencyThreshold Model

**Files:**
- Create: `src/main/java/net/safedata/performance/training/gatling/model/LatencyThreshold.java`
- Test: `src/test/java/net/safedata/performance/training/gatling/model/LatencyThresholdTest.java`

**Step 1: Write the failing test**

```java
package net.safedata.performance.training.gatling.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LatencyThresholdTest {

    @Test
    void createWithAllParameters() {
        LatencyThreshold threshold = new LatencyThreshold(95.0, 400, 350);
        assertEquals(95.0, threshold.percentile());
        assertEquals(400, threshold.maxMs());
        assertEquals(350, threshold.warnMs().orElse(-1));
    }

    @Test
    void createWithoutWarnThreshold() {
        LatencyThreshold threshold = new LatencyThreshold(99.99, 1000, null);
        assertEquals(99.99, threshold.percentile());
        assertEquals(1000, threshold.maxMs());
        assertTrue(threshold.warnMs().isEmpty());
    }

    @Test
    void evaluatePass() {
        LatencyThreshold threshold = new LatencyThreshold(95.0, 400, 350);
        assertEquals(Result.PASS, threshold.evaluate(300));
    }

    @Test
    void evaluateWarn() {
        LatencyThreshold threshold = new LatencyThreshold(95.0, 400, 350);
        assertEquals(Result.WARN, threshold.evaluate(375));
    }

    @Test
    void evaluateFail() {
        LatencyThreshold threshold = new LatencyThreshold(95.0, 400, 350);
        assertEquals(Result.FAIL, threshold.evaluate(450));
    }

    @Test
    void evaluatePassWhenNoWarnThreshold() {
        LatencyThreshold threshold = new LatencyThreshold(95.0, 400, null);
        assertEquals(Result.PASS, threshold.evaluate(399));
    }
}
```

**Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=LatencyThresholdTest -q`
Expected: Compilation error - LatencyThreshold class not found

**Step 3: Write minimal implementation**

```java
package net.safedata.performance.training.gatling.model;

import java.util.Optional;

public record LatencyThreshold(double percentile, int maxMs, Integer warnMs) {

    public Optional<Integer> warnMs() {
        return Optional.ofNullable(warnMs);
    }

    public Result evaluate(double actualMs) {
        if (actualMs > maxMs) {
            return Result.FAIL;
        }
        if (warnMs != null && actualMs >= warnMs) {
            return Result.WARN;
        }
        return Result.PASS;
    }
}
```

**Step 4: Run test to verify it passes**

Run: `mvn test -Dtest=LatencyThresholdTest -q`
Expected: All 6 tests pass

**Step 5: Commit**

```bash
git add src/main/java/net/safedata/performance/training/gatling/model/LatencyThreshold.java \
        src/test/java/net/safedata/performance/training/gatling/model/LatencyThresholdTest.java
git commit -m "feat(gatling): add LatencyThreshold model with evaluation"
```

---

## Task 4: Create ThroughputThreshold Model

**Files:**
- Create: `src/main/java/net/safedata/performance/training/gatling/model/ThroughputThreshold.java`
- Test: `src/test/java/net/safedata/performance/training/gatling/model/ThroughputThresholdTest.java`

**Step 1: Write the failing test**

```java
package net.safedata.performance.training.gatling.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ThroughputThresholdTest {

    @Test
    void createWithAllParameters() {
        ThroughputThreshold threshold = new ThroughputThreshold(1500.0, 1350.0, 1400.0);
        assertEquals(1500.0, threshold.targetTps());
        assertEquals(1350.0, threshold.sustainedMinTps().orElse(-1.0));
        assertEquals(1400.0, threshold.warnTps().orElse(-1.0));
    }

    @Test
    void evaluatePass() {
        ThroughputThreshold threshold = new ThroughputThreshold(1500.0, 1350.0, 1400.0);
        assertEquals(Result.PASS, threshold.evaluate(1600.0, 1400.0));
    }

    @Test
    void evaluateWarnBelowWarnThreshold() {
        ThroughputThreshold threshold = new ThroughputThreshold(1500.0, 1350.0, 1400.0);
        assertEquals(Result.WARN, threshold.evaluate(1550.0, 1380.0));
    }

    @Test
    void evaluateFailBelowTarget() {
        ThroughputThreshold threshold = new ThroughputThreshold(1500.0, 1350.0, 1400.0);
        assertEquals(Result.FAIL, threshold.evaluate(1400.0, 1300.0));
    }

    @Test
    void evaluateFailBelowSustained() {
        ThroughputThreshold threshold = new ThroughputThreshold(1500.0, 1350.0, 1400.0);
        assertEquals(Result.FAIL, threshold.evaluate(1600.0, 1200.0));
    }

    @Test
    void evaluateWithoutSustainedCheck() {
        ThroughputThreshold threshold = new ThroughputThreshold(1500.0, null, null);
        assertEquals(Result.PASS, threshold.evaluate(1500.0, 100.0));
    }
}
```

**Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=ThroughputThresholdTest -q`
Expected: Compilation error - ThroughputThreshold class not found

**Step 3: Write minimal implementation**

```java
package net.safedata.performance.training.gatling.model;

import java.util.Optional;

public record ThroughputThreshold(double targetTps, Double sustainedMinTps, Double warnTps) {

    public Optional<Double> sustainedMinTps() {
        return Optional.ofNullable(sustainedMinTps);
    }

    public Optional<Double> warnTps() {
        return Optional.ofNullable(warnTps);
    }

    public Result evaluate(double avgTps, double minTps) {
        if (avgTps < targetTps) {
            return Result.FAIL;
        }
        if (sustainedMinTps != null && minTps < sustainedMinTps) {
            return Result.FAIL;
        }
        if (warnTps != null && avgTps < warnTps) {
            return Result.WARN;
        }
        return Result.PASS;
    }
}
```

**Step 4: Run test to verify it passes**

Run: `mvn test -Dtest=ThroughputThresholdTest -q`
Expected: All 6 tests pass

**Step 5: Commit**

```bash
git add src/main/java/net/safedata/performance/training/gatling/model/ThroughputThreshold.java \
        src/test/java/net/safedata/performance/training/gatling/model/ThroughputThresholdTest.java
git commit -m "feat(gatling): add ThroughputThreshold model with evaluation"
```

---

## Task 5: Create ErrorRateThreshold Model

**Files:**
- Create: `src/main/java/net/safedata/performance/training/gatling/model/ErrorRateThreshold.java`
- Test: `src/test/java/net/safedata/performance/training/gatling/model/ErrorRateThresholdTest.java`

**Step 1: Write the failing test**

```java
package net.safedata.performance.training.gatling.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ErrorRateThresholdTest {

    @Test
    void evaluatePass() {
        ErrorRateThreshold threshold = new ErrorRateThreshold(0.1, 0.05);
        assertEquals(Result.PASS, threshold.evaluate(0.03));
    }

    @Test
    void evaluateWarn() {
        ErrorRateThreshold threshold = new ErrorRateThreshold(0.1, 0.05);
        assertEquals(Result.WARN, threshold.evaluate(0.07));
    }

    @Test
    void evaluateFail() {
        ErrorRateThreshold threshold = new ErrorRateThreshold(0.1, 0.05);
        assertEquals(Result.FAIL, threshold.evaluate(0.15));
    }

    @Test
    void evaluatePassWhenNoWarnThreshold() {
        ErrorRateThreshold threshold = new ErrorRateThreshold(0.1, null);
        assertEquals(Result.PASS, threshold.evaluate(0.09));
    }
}
```

**Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=ErrorRateThresholdTest -q`
Expected: Compilation error - ErrorRateThreshold class not found

**Step 3: Write minimal implementation**

```java
package net.safedata.performance.training.gatling.model;

import java.util.Optional;

public record ErrorRateThreshold(double maxPercent, Double warnPercent) {

    public Optional<Double> warnPercent() {
        return Optional.ofNullable(warnPercent);
    }

    public Result evaluate(double actualPercent) {
        if (actualPercent > maxPercent) {
            return Result.FAIL;
        }
        if (warnPercent != null && actualPercent >= warnPercent) {
            return Result.WARN;
        }
        return Result.PASS;
    }
}
```

**Step 4: Run test to verify it passes**

Run: `mvn test -Dtest=ErrorRateThresholdTest -q`
Expected: All 4 tests pass

**Step 5: Commit**

```bash
git add src/main/java/net/safedata/performance/training/gatling/model/ErrorRateThreshold.java \
        src/test/java/net/safedata/performance/training/gatling/model/ErrorRateThresholdTest.java
git commit -m "feat(gatling): add ErrorRateThreshold model with evaluation"
```

---

## Task 6: Create EndpointNFR Model

**Files:**
- Create: `src/main/java/net/safedata/performance/training/gatling/model/EndpointNFR.java`
- Test: `src/test/java/net/safedata/performance/training/gatling/model/EndpointNFRTest.java`

**Step 1: Write the failing test**

```java
package net.safedata.performance.training.gatling.model;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class EndpointNFRTest {

    @Test
    void createWithAllThresholds() {
        EndpointNFR nfr = new EndpointNFR(
            "test-endpoint",
            List.of(new LatencyThreshold(95.0, 400, 350)),
            new ThroughputThreshold(1500.0, 1350.0, 1400.0),
            new ErrorRateThreshold(0.1, 0.05)
        );

        assertEquals("test-endpoint", nfr.endpointName());
        assertEquals(1, nfr.latencyThresholds().size());
        assertTrue(nfr.throughput().isPresent());
        assertTrue(nfr.errorRate().isPresent());
    }

    @Test
    void createWithOnlyLatency() {
        EndpointNFR nfr = new EndpointNFR(
            "test-endpoint",
            List.of(new LatencyThreshold(95.0, 400, null)),
            null,
            null
        );

        assertEquals("test-endpoint", nfr.endpointName());
        assertTrue(nfr.throughput().isEmpty());
        assertTrue(nfr.errorRate().isEmpty());
    }
}
```

**Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=EndpointNFRTest -q`
Expected: Compilation error - EndpointNFR class not found

**Step 3: Write minimal implementation**

```java
package net.safedata.performance.training.gatling.model;

import java.util.List;
import java.util.Optional;

public record EndpointNFR(
    String endpointName,
    List<LatencyThreshold> latencyThresholds,
    ThroughputThreshold throughputThreshold,
    ErrorRateThreshold errorRateThreshold
) {
    public Optional<ThroughputThreshold> throughput() {
        return Optional.ofNullable(throughputThreshold);
    }

    public Optional<ErrorRateThreshold> errorRate() {
        return Optional.ofNullable(errorRateThreshold);
    }
}
```

**Step 4: Run test to verify it passes**

Run: `mvn test -Dtest=EndpointNFRTest -q`
Expected: All 2 tests pass

**Step 5: Commit**

```bash
git add src/main/java/net/safedata/performance/training/gatling/model/EndpointNFR.java \
        src/test/java/net/safedata/performance/training/gatling/model/EndpointNFRTest.java
git commit -m "feat(gatling): add EndpointNFR model"
```

---

## Task 7: Create NFRConfig with Builder

**Files:**
- Create: `src/main/java/net/safedata/performance/training/gatling/model/NFRConfig.java`
- Test: `src/test/java/net/safedata/performance/training/gatling/model/NFRConfigTest.java`

**Step 1: Write the failing test**

```java
package net.safedata.performance.training.gatling.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NFRConfigTest {

    @Test
    void buildConfigWithFluentApi() {
        NFRConfig config = NFRConfig.builder()
            .endpoint("surepay-verify-account")
                .latency(90.0, 150, 120)
                .latency(95.0, 400, 350)
                .latency(99.99, 1000, null)
                .throughput(1500.0, 1350.0, 1400.0)
                .errorRate(0.1, 0.05)
            .endpoint("eba-payee-information")
                .latency(95.0, 500, null)
            .build();

        assertEquals(2, config.endpoints().size());

        EndpointNFR surepay = config.getEndpoint("surepay-verify-account").orElseThrow();
        assertEquals(3, surepay.latencyThresholds().size());
        assertTrue(surepay.throughput().isPresent());
        assertTrue(surepay.errorRate().isPresent());

        EndpointNFR eba = config.getEndpoint("eba-payee-information").orElseThrow();
        assertEquals(1, eba.latencyThresholds().size());
        assertTrue(eba.throughput().isEmpty());
    }

    @Test
    void getEndpointReturnsEmptyForUnknown() {
        NFRConfig config = NFRConfig.builder()
            .endpoint("known")
                .latency(95.0, 400, null)
            .build();

        assertTrue(config.getEndpoint("unknown").isEmpty());
    }
}
```

**Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=NFRConfigTest -q`
Expected: Compilation error - NFRConfig class not found

**Step 3: Write minimal implementation**

```java
package net.safedata.performance.training.gatling.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record NFRConfig(List<EndpointNFR> endpoints) {

    public Optional<EndpointNFR> getEndpoint(String name) {
        return endpoints.stream()
            .filter(e -> e.endpointName().equals(name))
            .findFirst();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final List<EndpointNFR> endpoints = new ArrayList<>();

        public EndpointBuilder endpoint(String name) {
            return new EndpointBuilder(this, name);
        }

        private void addEndpoint(EndpointNFR endpoint) {
            endpoints.add(endpoint);
        }

        public NFRConfig build() {
            return new NFRConfig(List.copyOf(endpoints));
        }
    }

    public static class EndpointBuilder {
        private final Builder parent;
        private final String name;
        private final List<LatencyThreshold> latencyThresholds = new ArrayList<>();
        private ThroughputThreshold throughputThreshold;
        private ErrorRateThreshold errorRateThreshold;

        EndpointBuilder(Builder parent, String name) {
            this.parent = parent;
            this.name = name;
        }

        public EndpointBuilder latency(double percentile, int maxMs, Integer warnMs) {
            latencyThresholds.add(new LatencyThreshold(percentile, maxMs, warnMs));
            return this;
        }

        public EndpointBuilder throughput(double targetTps, Double sustainedMinTps, Double warnTps) {
            this.throughputThreshold = new ThroughputThreshold(targetTps, sustainedMinTps, warnTps);
            return this;
        }

        public EndpointBuilder errorRate(double maxPercent, Double warnPercent) {
            this.errorRateThreshold = new ErrorRateThreshold(maxPercent, warnPercent);
            return this;
        }

        public EndpointBuilder endpoint(String name) {
            finishCurrentEndpoint();
            return new EndpointBuilder(parent, name);
        }

        public NFRConfig build() {
            finishCurrentEndpoint();
            return parent.build();
        }

        private void finishCurrentEndpoint() {
            parent.addEndpoint(new EndpointNFR(
                name,
                List.copyOf(latencyThresholds),
                throughputThreshold,
                errorRateThreshold
            ));
        }
    }
}
```

**Step 4: Run test to verify it passes**

Run: `mvn test -Dtest=NFRConfigTest -q`
Expected: All 2 tests pass

**Step 5: Commit**

```bash
git add src/main/java/net/safedata/performance/training/gatling/model/NFRConfig.java \
        src/test/java/net/safedata/performance/training/gatling/model/NFRConfigTest.java
git commit -m "feat(gatling): add NFRConfig with fluent builder API"
```

---

## Task 8: Create EndpointStats Model

**Files:**
- Create: `src/main/java/net/safedata/performance/training/gatling/model/EndpointStats.java`
- Test: `src/test/java/net/safedata/performance/training/gatling/model/EndpointStatsTest.java`

**Step 1: Write the failing test**

```java
package net.safedata.performance.training.gatling.model;

import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class EndpointStatsTest {

    @Test
    void createAndAccessFields() {
        EndpointStats stats = new EndpointStats(
            "test-endpoint",
            1000L, 990L, 10L,
            1.0,
            100.5,
            Map.of(50, 12, 75, 15, 95, 21, 99, 44)
        );

        assertEquals("test-endpoint", stats.name());
        assertEquals(1000L, stats.totalRequests());
        assertEquals(990L, stats.okCount());
        assertEquals(10L, stats.koCount());
        assertEquals(1.0, stats.errorPercent());
        assertEquals(100.5, stats.requestsPerSecond());
        assertEquals(12, stats.percentiles().get(50));
    }

    @Test
    void getPercentileReturnsValueForKnown() {
        EndpointStats stats = new EndpointStats(
            "test", 100L, 100L, 0L, 0.0, 10.0,
            Map.of(50, 10, 75, 20, 95, 30, 99, 50)
        );

        assertEquals(30, stats.getPercentile(95).orElse(-1));
    }

    @Test
    void getPercentileReturnsEmptyForUnknown() {
        EndpointStats stats = new EndpointStats(
            "test", 100L, 100L, 0L, 0.0, 10.0,
            Map.of(50, 10)
        );

        assertTrue(stats.getPercentile(90).isEmpty());
    }
}
```

**Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=EndpointStatsTest -q`
Expected: Compilation error - EndpointStats class not found

**Step 3: Write minimal implementation**

```java
package net.safedata.performance.training.gatling.model;

import java.util.Map;
import java.util.Optional;

public record EndpointStats(
    String name,
    long totalRequests,
    long okCount,
    long koCount,
    double errorPercent,
    double requestsPerSecond,
    Map<Integer, Integer> percentiles
) {
    public Optional<Integer> getPercentile(int p) {
        return Optional.ofNullable(percentiles.get(p));
    }
}
```

**Step 4: Run test to verify it passes**

Run: `mvn test -Dtest=EndpointStatsTest -q`
Expected: All 3 tests pass

**Step 5: Commit**

```bash
git add src/main/java/net/safedata/performance/training/gatling/model/EndpointStats.java \
        src/test/java/net/safedata/performance/training/gatling/model/EndpointStatsTest.java
git commit -m "feat(gatling): add EndpointStats model for parsed report data"
```

---

## Task 9: Create GatlingReport Model

**Files:**
- Create: `src/main/java/net/safedata/performance/training/gatling/model/GatlingReport.java`
- Create: `src/main/java/net/safedata/performance/training/gatling/model/RunInfo.java`
- Test: `src/test/java/net/safedata/performance/training/gatling/model/GatlingReportTest.java`

**Step 1: Write the failing test**

```java
package net.safedata.performance.training.gatling.model;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class GatlingReportTest {

    @Test
    void createReportAndAccessFields() {
        RunInfo runInfo = new RunInfo(
            "TestSimulation",
            LocalDateTime.of(2025, 10, 6, 12, 4, 51),
            "15m 2s",
            "Performance test"
        );

        EndpointStats endpoint = new EndpointStats(
            "test-endpoint", 1000L, 1000L, 0L, 0.0, 100.0,
            Map.of(50, 10, 75, 15, 95, 20, 99, 40)
        );

        GatlingReport report = new GatlingReport(runInfo, List.of(endpoint));

        assertEquals("TestSimulation", report.runInfo().simulationName());
        assertEquals(1, report.endpoints().size());
    }

    @Test
    void getEndpointByName() {
        EndpointStats ep1 = new EndpointStats("ep1", 100L, 100L, 0L, 0.0, 10.0, Map.of());
        EndpointStats ep2 = new EndpointStats("ep2", 200L, 200L, 0L, 0.0, 20.0, Map.of());

        GatlingReport report = new GatlingReport(
            new RunInfo("Test", LocalDateTime.now(), "1m", "desc"),
            List.of(ep1, ep2)
        );

        assertTrue(report.getEndpoint("ep1").isPresent());
        assertTrue(report.getEndpoint("ep2").isPresent());
        assertTrue(report.getEndpoint("unknown").isEmpty());
    }
}
```

**Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=GatlingReportTest -q`
Expected: Compilation error - classes not found

**Step 3: Write RunInfo implementation**

```java
package net.safedata.performance.training.gatling.model;

import java.time.LocalDateTime;

public record RunInfo(
    String simulationName,
    LocalDateTime date,
    String duration,
    String description
) {}
```

**Step 4: Write GatlingReport implementation**

```java
package net.safedata.performance.training.gatling.model;

import java.util.List;
import java.util.Optional;

public record GatlingReport(
    RunInfo runInfo,
    List<EndpointStats> endpoints
) {
    public Optional<EndpointStats> getEndpoint(String name) {
        return endpoints.stream()
            .filter(e -> e.name().equals(name))
            .findFirst();
    }
}
```

**Step 5: Run test to verify it passes**

Run: `mvn test -Dtest=GatlingReportTest -q`
Expected: All 2 tests pass

**Step 6: Commit**

```bash
git add src/main/java/net/safedata/performance/training/gatling/model/RunInfo.java \
        src/main/java/net/safedata/performance/training/gatling/model/GatlingReport.java \
        src/test/java/net/safedata/performance/training/gatling/model/GatlingReportTest.java
git commit -m "feat(gatling): add GatlingReport and RunInfo models"
```

---

## Task 10: Create PercentileInterpolator

**Files:**
- Create: `src/main/java/net/safedata/performance/training/gatling/analyzer/PercentileInterpolator.java`
- Test: `src/test/java/net/safedata/performance/training/gatling/analyzer/PercentileInterpolatorTest.java`

**Step 1: Write the failing test**

```java
package net.safedata.performance.training.gatling.analyzer;

import net.safedata.performance.training.gatling.model.EndpointStats;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class PercentileInterpolatorTest {

    private final PercentileInterpolator interpolator = new PercentileInterpolator();

    @Test
    void returnMeasuredValueForKnownPercentile() {
        EndpointStats stats = createStats(Map.of(50, 12, 75, 15, 95, 21, 99, 44));

        PercentileInterpolator.InterpolatedValue result = interpolator.getPercentile(stats, 95);

        assertEquals(21.0, result.value(), 0.01);
        assertEquals(PercentileInterpolator.Accuracy.MEASURED, result.accuracy());
    }

    @Test
    void interpolateP90BetweenP75AndP95() {
        EndpointStats stats = createStats(Map.of(50, 12, 75, 15, 95, 21, 99, 44));

        PercentileInterpolator.InterpolatedValue result = interpolator.getPercentile(stats, 90);

        // p90 = p75 + (p95 - p75) * ((90 - 75) / (95 - 75)) = 15 + 6 * 0.75 = 19.5
        assertEquals(19.5, result.value(), 0.01);
        assertEquals(PercentileInterpolator.Accuracy.INTERPOLATED, result.accuracy());
    }

    @Test
    void extrapolateP9999BeyondP99() {
        EndpointStats stats = createStats(Map.of(50, 12, 75, 15, 95, 21, 99, 44));

        PercentileInterpolator.InterpolatedValue result = interpolator.getPercentile(stats, 99.99);

        // Log-linear extrapolation from p95->p99 trend
        // p99.99 = p99 * (p99/p95)^((99.99 - 99) / (99 - 95))
        // = 44 * (44/21)^(0.99/4) = 44 * 2.095^0.2475 ≈ 53.5
        assertTrue(result.value() > 44);
        assertTrue(result.value() < 100);
        assertEquals(PercentileInterpolator.Accuracy.EXTRAPOLATED, result.accuracy());
    }

    @Test
    void interpolateP60BetweenP50AndP75() {
        EndpointStats stats = createStats(Map.of(50, 10, 75, 25, 95, 40, 99, 60));

        PercentileInterpolator.InterpolatedValue result = interpolator.getPercentile(stats, 60);

        // p60 = p50 + (p75 - p50) * ((60 - 50) / (75 - 50)) = 10 + 15 * 0.4 = 16
        assertEquals(16.0, result.value(), 0.01);
        assertEquals(PercentileInterpolator.Accuracy.INTERPOLATED, result.accuracy());
    }

    private EndpointStats createStats(Map<Integer, Integer> percentiles) {
        return new EndpointStats("test", 1000L, 1000L, 0L, 0.0, 100.0, percentiles);
    }
}
```

**Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=PercentileInterpolatorTest -q`
Expected: Compilation error - PercentileInterpolator class not found

**Step 3: Write minimal implementation**

```java
package net.safedata.performance.training.gatling.analyzer;

import net.safedata.performance.training.gatling.model.EndpointStats;

import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

public class PercentileInterpolator {

    public enum Accuracy {
        MEASURED,
        INTERPOLATED,
        EXTRAPOLATED
    }

    public record InterpolatedValue(double value, Accuracy accuracy) {}

    public InterpolatedValue getPercentile(EndpointStats stats, double targetPercentile) {
        Map<Integer, Integer> percentiles = stats.percentiles();

        // Check if we have the exact percentile
        int targetInt = (int) targetPercentile;
        if (targetPercentile == targetInt && percentiles.containsKey(targetInt)) {
            return new InterpolatedValue(percentiles.get(targetInt), Accuracy.MEASURED);
        }

        NavigableMap<Integer, Integer> sorted = new TreeMap<>(percentiles);

        Integer lowerKey = sorted.floorKey((int) targetPercentile);
        Integer upperKey = sorted.ceilingKey((int) Math.ceil(targetPercentile));

        // Handle extrapolation beyond known range
        if (upperKey == null || targetPercentile > sorted.lastKey()) {
            return extrapolateBeyond(sorted, targetPercentile);
        }

        if (lowerKey == null) {
            return extrapolateBelow(sorted, targetPercentile);
        }

        // Interpolate between two known points
        double lowerValue = sorted.get(lowerKey);
        double upperValue = sorted.get(upperKey);

        double ratio = (targetPercentile - lowerKey) / (upperKey - lowerKey);
        double interpolated = lowerValue + (upperValue - lowerValue) * ratio;

        return new InterpolatedValue(interpolated, Accuracy.INTERPOLATED);
    }

    private InterpolatedValue extrapolateBeyond(NavigableMap<Integer, Integer> sorted, double targetPercentile) {
        // Use log-linear extrapolation from the last two known percentiles
        Integer p95Key = sorted.floorKey(95);
        Integer p99Key = sorted.floorKey(99);

        if (p95Key == null || p99Key == null || p95Key.equals(p99Key)) {
            // Fall back to linear extrapolation from last two points
            var entries = sorted.descendingMap().entrySet().iterator();
            var last = entries.next();
            var secondLast = entries.next();

            double slope = (double)(last.getValue() - secondLast.getValue()) /
                          (last.getKey() - secondLast.getKey());
            double extrapolated = last.getValue() + slope * (targetPercentile - last.getKey());
            return new InterpolatedValue(extrapolated, Accuracy.EXTRAPOLATED);
        }

        double p95Value = sorted.get(p95Key);
        double p99Value = sorted.get(p99Key);

        // Log-linear extrapolation: p_target = p99 * (p99/p95)^((target - 99) / (99 - 95))
        double ratio = p99Value / p95Value;
        double exponent = (targetPercentile - p99Key) / (p99Key - p95Key);
        double extrapolated = p99Value * Math.pow(ratio, exponent);

        return new InterpolatedValue(extrapolated, Accuracy.EXTRAPOLATED);
    }

    private InterpolatedValue extrapolateBelow(NavigableMap<Integer, Integer> sorted, double targetPercentile) {
        var entries = sorted.entrySet().iterator();
        var first = entries.next();
        var second = entries.next();

        double slope = (double)(second.getValue() - first.getValue()) /
                      (second.getKey() - first.getKey());
        double extrapolated = first.getValue() - slope * (first.getKey() - targetPercentile);

        return new InterpolatedValue(Math.max(0, extrapolated), Accuracy.EXTRAPOLATED);
    }
}
```

**Step 4: Run test to verify it passes**

Run: `mvn test -Dtest=PercentileInterpolatorTest -q`
Expected: All 4 tests pass

**Step 5: Commit**

```bash
git add src/main/java/net/safedata/performance/training/gatling/analyzer/PercentileInterpolator.java \
        src/test/java/net/safedata/performance/training/gatling/analyzer/PercentileInterpolatorTest.java
git commit -m "feat(gatling): add PercentileInterpolator with linear interpolation and log-linear extrapolation"
```

---

## Task 11: Create EvaluationResult Models

**Files:**
- Create: `src/main/java/net/safedata/performance/training/gatling/model/LatencyEvaluation.java`
- Create: `src/main/java/net/safedata/performance/training/gatling/model/EndpointEvaluation.java`
- Create: `src/main/java/net/safedata/performance/training/gatling/model/EvaluationResult.java`
- Test: `src/test/java/net/safedata/performance/training/gatling/model/EvaluationResultTest.java`

**Step 1: Write the failing test**

```java
package net.safedata.performance.training.gatling.model;

import net.safedata.performance.training.gatling.analyzer.PercentileInterpolator.Accuracy;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class EvaluationResultTest {

    @Test
    void overallResultIsFailWhenAnyEndpointFails() {
        EndpointEvaluation ep1 = new EndpointEvaluation("ep1", Result.PASS, List.of(), null, null);
        EndpointEvaluation ep2 = new EndpointEvaluation("ep2", Result.FAIL, List.of(), null, null);

        EvaluationResult result = new EvaluationResult(List.of(ep1, ep2));

        assertEquals(Result.FAIL, result.overallResult());
    }

    @Test
    void overallResultIsWarnWhenNoFailButHasWarn() {
        EndpointEvaluation ep1 = new EndpointEvaluation("ep1", Result.PASS, List.of(), null, null);
        EndpointEvaluation ep2 = new EndpointEvaluation("ep2", Result.WARN, List.of(), null, null);

        EvaluationResult result = new EvaluationResult(List.of(ep1, ep2));

        assertEquals(Result.WARN, result.overallResult());
    }

    @Test
    void overallResultIsPassWhenAllPass() {
        EndpointEvaluation ep1 = new EndpointEvaluation("ep1", Result.PASS, List.of(), null, null);
        EndpointEvaluation ep2 = new EndpointEvaluation("ep2", Result.PASS, List.of(), null, null);

        EvaluationResult result = new EvaluationResult(List.of(ep1, ep2));

        assertEquals(Result.PASS, result.overallResult());
    }

    @Test
    void latencyEvaluationHoldsAllFields() {
        LatencyEvaluation eval = new LatencyEvaluation(
            95.0, 21.0, Accuracy.MEASURED, 400, 350, Result.PASS
        );

        assertEquals(95.0, eval.percentile());
        assertEquals(21.0, eval.actualMs());
        assertEquals(Accuracy.MEASURED, eval.accuracy());
        assertEquals(400, eval.limitMs());
        assertEquals(350, eval.warnMs().orElse(-1));
        assertEquals(Result.PASS, eval.result());
    }
}
```

**Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=EvaluationResultTest -q`
Expected: Compilation error - classes not found

**Step 3: Write LatencyEvaluation implementation**

```java
package net.safedata.performance.training.gatling.model;

import net.safedata.performance.training.gatling.analyzer.PercentileInterpolator.Accuracy;
import java.util.Optional;

public record LatencyEvaluation(
    double percentile,
    double actualMs,
    Accuracy accuracy,
    int limitMs,
    Integer warnMs,
    Result result
) {
    public Optional<Integer> warnMs() {
        return Optional.ofNullable(warnMs);
    }
}
```

**Step 4: Write ThroughputEvaluation implementation**

```java
package net.safedata.performance.training.gatling.model;

import java.util.Optional;

public record ThroughputEvaluation(
    double actualTps,
    double targetTps,
    Double warnTps,
    Result result
) {
    public Optional<Double> warnTps() {
        return Optional.ofNullable(warnTps);
    }
}
```

**Step 5: Write ErrorRateEvaluation implementation**

```java
package net.safedata.performance.training.gatling.model;

import java.util.Optional;

public record ErrorRateEvaluation(
    double actualPercent,
    double limitPercent,
    Double warnPercent,
    Result result
) {
    public Optional<Double> warnPercent() {
        return Optional.ofNullable(warnPercent);
    }
}
```

**Step 6: Write EndpointEvaluation implementation**

```java
package net.safedata.performance.training.gatling.model;

import java.util.List;
import java.util.Optional;

public record EndpointEvaluation(
    String endpointName,
    Result result,
    List<LatencyEvaluation> latencyEvaluations,
    ThroughputEvaluation throughputEvaluation,
    ErrorRateEvaluation errorRateEvaluation
) {
    public Optional<ThroughputEvaluation> throughput() {
        return Optional.ofNullable(throughputEvaluation);
    }

    public Optional<ErrorRateEvaluation> errorRate() {
        return Optional.ofNullable(errorRateEvaluation);
    }
}
```

**Step 7: Write EvaluationResult implementation**

```java
package net.safedata.performance.training.gatling.model;

import java.util.List;

public record EvaluationResult(List<EndpointEvaluation> endpointEvaluations) {

    public Result overallResult() {
        return endpointEvaluations.stream()
            .map(EndpointEvaluation::result)
            .reduce(Result.PASS, Result::worseOf);
    }

    public long failureCount() {
        return endpointEvaluations.stream()
            .filter(e -> e.result() == Result.FAIL)
            .count();
    }

    public long warningCount() {
        return endpointEvaluations.stream()
            .filter(e -> e.result() == Result.WARN)
            .count();
    }
}
```

**Step 8: Run test to verify it passes**

Run: `mvn test -Dtest=EvaluationResultTest -q`
Expected: All 4 tests pass

**Step 9: Commit**

```bash
git add src/main/java/net/safedata/performance/training/gatling/model/LatencyEvaluation.java \
        src/main/java/net/safedata/performance/training/gatling/model/ThroughputEvaluation.java \
        src/main/java/net/safedata/performance/training/gatling/model/ErrorRateEvaluation.java \
        src/main/java/net/safedata/performance/training/gatling/model/EndpointEvaluation.java \
        src/main/java/net/safedata/performance/training/gatling/model/EvaluationResult.java \
        src/test/java/net/safedata/performance/training/gatling/model/EvaluationResultTest.java
git commit -m "feat(gatling): add evaluation result models"
```

---

## Task 12: Create NFREvaluator

**Files:**
- Create: `src/main/java/net/safedata/performance/training/gatling/analyzer/NFREvaluator.java`
- Test: `src/test/java/net/safedata/performance/training/gatling/analyzer/NFREvaluatorTest.java`

**Step 1: Write the failing test**

```java
package net.safedata.performance.training.gatling.analyzer;

import net.safedata.performance.training.gatling.model.*;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class NFREvaluatorTest {

    private final NFREvaluator evaluator = new NFREvaluator();

    @Test
    void evaluatePassingEndpoint() {
        GatlingReport report = createReport("test-endpoint", 1000.0, 0.0,
            Map.of(50, 10, 75, 15, 95, 20, 99, 40));

        NFRConfig nfr = NFRConfig.builder()
            .endpoint("test-endpoint")
                .latency(95.0, 100, 80)
                .throughput(500.0, null, null)
                .errorRate(1.0, null)
            .build();

        EvaluationResult result = evaluator.evaluate(report, nfr);

        assertEquals(Result.PASS, result.overallResult());
        assertEquals(1, result.endpointEvaluations().size());
    }

    @Test
    void evaluateFailingLatency() {
        GatlingReport report = createReport("test-endpoint", 1000.0, 0.0,
            Map.of(50, 10, 75, 15, 95, 200, 99, 400));

        NFRConfig nfr = NFRConfig.builder()
            .endpoint("test-endpoint")
                .latency(95.0, 100, 80)
            .build();

        EvaluationResult result = evaluator.evaluate(report, nfr);

        assertEquals(Result.FAIL, result.overallResult());
    }

    @Test
    void evaluateWarningThroughput() {
        GatlingReport report = createReport("test-endpoint", 550.0, 0.0,
            Map.of(50, 10, 75, 15, 95, 20, 99, 40));

        NFRConfig nfr = NFRConfig.builder()
            .endpoint("test-endpoint")
                .latency(95.0, 100, null)
                .throughput(500.0, null, 600.0)
            .build();

        EvaluationResult result = evaluator.evaluate(report, nfr);

        assertEquals(Result.WARN, result.overallResult());
    }

    @Test
    void skipEndpointsNotInNFR() {
        GatlingReport report = new GatlingReport(
            new RunInfo("Test", LocalDateTime.now(), "1m", "desc"),
            List.of(
                new EndpointStats("configured", 100L, 100L, 0L, 0.0, 100.0, Map.of(95, 20)),
                new EndpointStats("not-configured", 200L, 200L, 0L, 0.0, 200.0, Map.of(95, 30))
            )
        );

        NFRConfig nfr = NFRConfig.builder()
            .endpoint("configured")
                .latency(95.0, 100, null)
            .build();

        EvaluationResult result = evaluator.evaluate(report, nfr);

        assertEquals(1, result.endpointEvaluations().size());
        assertEquals("configured", result.endpointEvaluations().get(0).endpointName());
    }

    private GatlingReport createReport(String endpointName, double tps, double errorPct,
                                       Map<Integer, Integer> percentiles) {
        long total = 1000L;
        long ko = (long)(total * errorPct / 100);
        EndpointStats stats = new EndpointStats(endpointName, total, total - ko, ko, errorPct, tps, percentiles);
        return new GatlingReport(
            new RunInfo("Test", LocalDateTime.now(), "1m", "desc"),
            List.of(stats)
        );
    }
}
```

**Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=NFREvaluatorTest -q`
Expected: Compilation error - NFREvaluator class not found

**Step 3: Write minimal implementation**

```java
package net.safedata.performance.training.gatling.analyzer;

import net.safedata.performance.training.gatling.model.*;

import java.util.ArrayList;
import java.util.List;

public class NFREvaluator {

    private final PercentileInterpolator interpolator = new PercentileInterpolator();

    public EvaluationResult evaluate(GatlingReport report, NFRConfig nfrConfig) {
        List<EndpointEvaluation> evaluations = new ArrayList<>();

        for (EndpointNFR nfr : nfrConfig.endpoints()) {
            report.getEndpoint(nfr.endpointName())
                .ifPresent(stats -> evaluations.add(evaluateEndpoint(stats, nfr)));
        }

        return new EvaluationResult(evaluations);
    }

    private EndpointEvaluation evaluateEndpoint(EndpointStats stats, EndpointNFR nfr) {
        List<LatencyEvaluation> latencyEvals = evaluateLatencies(stats, nfr);
        ThroughputEvaluation throughputEval = evaluateThroughput(stats, nfr);
        ErrorRateEvaluation errorRateEval = evaluateErrorRate(stats, nfr);

        Result endpointResult = Result.PASS;
        for (LatencyEvaluation eval : latencyEvals) {
            endpointResult = endpointResult.worseOf(eval.result());
        }
        if (throughputEval != null) {
            endpointResult = endpointResult.worseOf(throughputEval.result());
        }
        if (errorRateEval != null) {
            endpointResult = endpointResult.worseOf(errorRateEval.result());
        }

        return new EndpointEvaluation(
            nfr.endpointName(),
            endpointResult,
            latencyEvals,
            throughputEval,
            errorRateEval
        );
    }

    private List<LatencyEvaluation> evaluateLatencies(EndpointStats stats, EndpointNFR nfr) {
        List<LatencyEvaluation> evaluations = new ArrayList<>();

        for (LatencyThreshold threshold : nfr.latencyThresholds()) {
            PercentileInterpolator.InterpolatedValue interpolated =
                interpolator.getPercentile(stats, threshold.percentile());

            Result result = threshold.evaluate(interpolated.value());

            evaluations.add(new LatencyEvaluation(
                threshold.percentile(),
                interpolated.value(),
                interpolated.accuracy(),
                threshold.maxMs(),
                threshold.warnMs().orElse(null),
                result
            ));
        }

        return evaluations;
    }

    private ThroughputEvaluation evaluateThroughput(EndpointStats stats, EndpointNFR nfr) {
        return nfr.throughput().map(threshold -> {
            // For PDF parsing, we only have average TPS, not min TPS
            // Use average as both for now
            Result result = threshold.evaluate(stats.requestsPerSecond(), stats.requestsPerSecond());

            return new ThroughputEvaluation(
                stats.requestsPerSecond(),
                threshold.targetTps(),
                threshold.warnTps().orElse(null),
                result
            );
        }).orElse(null);
    }

    private ErrorRateEvaluation evaluateErrorRate(EndpointStats stats, EndpointNFR nfr) {
        return nfr.errorRate().map(threshold -> {
            Result result = threshold.evaluate(stats.errorPercent());

            return new ErrorRateEvaluation(
                stats.errorPercent(),
                threshold.maxPercent(),
                threshold.warnPercent().orElse(null),
                result
            );
        }).orElse(null);
    }
}
```

**Step 4: Run test to verify it passes**

Run: `mvn test -Dtest=NFREvaluatorTest -q`
Expected: All 4 tests pass

**Step 5: Commit**

```bash
git add src/main/java/net/safedata/performance/training/gatling/analyzer/NFREvaluator.java \
        src/test/java/net/safedata/performance/training/gatling/analyzer/NFREvaluatorTest.java
git commit -m "feat(gatling): add NFREvaluator to compare report against thresholds"
```

---

## Task 13: Create GatlingPdfParser

**Files:**
- Create: `src/main/java/net/safedata/performance/training/gatling/parser/GatlingPdfParser.java`
- Test: `src/test/java/net/safedata/performance/training/gatling/parser/GatlingPdfParserTest.java`
- Test resource: Copy the sample PDF to `src/test/resources/sample-gatling-report.pdf`

**Step 1: Write the failing test**

```java
package net.safedata.performance.training.gatling.parser;

import net.safedata.performance.training.gatling.model.EndpointStats;
import net.safedata.performance.training.gatling.model.GatlingReport;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class GatlingPdfParserTest {

    private final GatlingPdfParser parser = new GatlingPdfParser();

    @Test
    void parseReportFromPdf() throws IOException {
        Path pdfPath = Path.of("VopProvisioning-07.10.2025.pdf");

        // Skip test if file doesn't exist (CI environment)
        if (!pdfPath.toFile().exists()) {
            return;
        }

        GatlingReport report = parser.parse(pdfPath);

        assertNotNull(report.runInfo());
        assertEquals("VOPProvisioningAPISimulation", report.runInfo().simulationName());
        assertFalse(report.endpoints().isEmpty());

        // Verify surepay endpoint was parsed
        EndpointStats surepay = report.getEndpoint("surepay-verify-account").orElseThrow();
        assertEquals(850730, surepay.totalRequests());
        assertTrue(surepay.requestsPerSecond() > 900);

        // Verify percentiles were parsed
        assertTrue(surepay.percentiles().containsKey(50));
        assertTrue(surepay.percentiles().containsKey(95));
        assertTrue(surepay.percentiles().containsKey(99));
    }

    @Test
    void throwExceptionForNonExistentFile() {
        Path nonExistent = Path.of("non-existent.pdf");

        assertThrows(IOException.class, () -> parser.parse(nonExistent));
    }
}
```

**Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=GatlingPdfParserTest -q`
Expected: Compilation error - GatlingPdfParser class not found

**Step 3: Write minimal implementation**

```java
package net.safedata.performance.training.gatling.parser;

import net.safedata.performance.training.gatling.model.EndpointStats;
import net.safedata.performance.training.gatling.model.GatlingReport;
import net.safedata.performance.training.gatling.model.RunInfo;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GatlingPdfParser {

    private static final Pattern SIMULATION_NAME_PATTERN =
        Pattern.compile("^([A-Za-z0-9]+Simulation)$", Pattern.MULTILINE);

    private static final Pattern DATE_PATTERN =
        Pattern.compile("Date:\\s*(\\d{4}-\\d{2}-\\d{2}\\s+\\d{2}:\\d{2}:\\d{2})");

    private static final Pattern DURATION_PATTERN =
        Pattern.compile("Duration:\\s*([\\d]+m\\s*[\\d]*s?)");

    private static final Pattern DESCRIPTION_PATTERN =
        Pattern.compile("Description:\\s*(.+?)(?=\\n|Expand)");

    // Pattern for endpoint stats rows
    // Format: endpoint-name total ok ko %ko cnt/s min p50 p75 p95 p99 max mean stddev
    private static final Pattern ENDPOINT_STATS_PATTERN =
        Pattern.compile("([a-z][a-z0-9-]+)\\s+(\\d+)\\s+(\\d+)\\s*(\\d+)\\s*(\\d+)\\s*%?\\s*([\\d.]+)\\s*(\\d+)\\s*(\\d+)\\s*(\\d+)\\s*(\\d+)\\s*(\\d+)\\s*(\\d+)\\s*(\\d+)\\s*(\\d+)");

    private static final DateTimeFormatter DATE_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public GatlingReport parse(Path pdfPath) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdfPath.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);

            RunInfo runInfo = parseRunInfo(text);
            List<EndpointStats> endpoints = parseEndpoints(text);

            return new GatlingReport(runInfo, endpoints);
        }
    }

    private RunInfo parseRunInfo(String text) {
        String simulationName = extractPattern(SIMULATION_NAME_PATTERN, text, 1)
            .orElse("Unknown");

        LocalDateTime date = extractPattern(DATE_PATTERN, text, 1)
            .map(s -> s.replace(" GMT", "").trim())
            .map(s -> LocalDateTime.parse(s, DATE_FORMATTER))
            .orElse(LocalDateTime.now());

        String duration = extractPattern(DURATION_PATTERN, text, 1)
            .orElse("Unknown");

        String description = extractPattern(DESCRIPTION_PATTERN, text, 1)
            .orElse("");

        return new RunInfo(simulationName, date, duration, description);
    }

    private List<EndpointStats> parseEndpoints(String text) {
        List<EndpointStats> endpoints = new ArrayList<>();

        // Clean up text - remove extra whitespace while preserving structure
        String cleanedText = text.replaceAll("\\s+", " ");

        Matcher matcher = ENDPOINT_STATS_PATTERN.matcher(cleanedText);

        while (matcher.find()) {
            String name = matcher.group(1);

            // Skip "All Requests" aggregate row and header-like matches
            if (name.equals("all") || name.equals("requests") || name.equals("pct")) {
                continue;
            }

            try {
                long total = Long.parseLong(matcher.group(2));
                long ok = Long.parseLong(matcher.group(3));
                long ko = Long.parseLong(matcher.group(4));
                double errorPercent = ko > 0 ? (ko * 100.0 / total) : 0.0;
                double tps = Double.parseDouble(matcher.group(6));
                int min = Integer.parseInt(matcher.group(7));
                int p50 = Integer.parseInt(matcher.group(8));
                int p75 = Integer.parseInt(matcher.group(9));
                int p95 = Integer.parseInt(matcher.group(10));
                int p99 = Integer.parseInt(matcher.group(11));
                int max = Integer.parseInt(matcher.group(12));

                Map<Integer, Integer> percentiles = Map.of(
                    50, p50,
                    75, p75,
                    95, p95,
                    99, p99
                );

                endpoints.add(new EndpointStats(name, total, ok, ko, errorPercent, tps, percentiles));
            } catch (NumberFormatException e) {
                // Skip malformed rows
            }
        }

        return endpoints;
    }

    private Optional<String> extractPattern(Pattern pattern, String text, int group) {
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return Optional.of(matcher.group(group));
        }
        return Optional.empty();
    }
}
```

**Step 4: Run test to verify it passes**

Run: `mvn test -Dtest=GatlingPdfParserTest -q`
Expected: Tests pass (or skip if PDF not present)

**Step 5: Commit**

```bash
git add src/main/java/net/safedata/performance/training/gatling/parser/GatlingPdfParser.java \
        src/test/java/net/safedata/performance/training/gatling/parser/GatlingPdfParserTest.java
git commit -m "feat(gatling): add GatlingPdfParser using PDFBox"
```

---

## Task 14: Create ConsoleReportWriter

**Files:**
- Create: `src/main/java/net/safedata/performance/training/gatling/report/ConsoleReportWriter.java`
- Test: `src/test/java/net/safedata/performance/training/gatling/report/ConsoleReportWriterTest.java`

**Step 1: Write the failing test**

```java
package net.safedata.performance.training.gatling.report;

import net.safedata.performance.training.gatling.analyzer.PercentileInterpolator.Accuracy;
import net.safedata.performance.training.gatling.model.*;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ConsoleReportWriterTest {

    @Test
    void writeReportContainsKeyElements() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(baos);
        ConsoleReportWriter writer = new ConsoleReportWriter(out);

        RunInfo runInfo = new RunInfo(
            "TestSimulation",
            LocalDateTime.of(2025, 10, 6, 12, 4, 51),
            "15m 2s",
            "Test description"
        );

        GatlingReport report = new GatlingReport(runInfo, List.of());

        LatencyEvaluation latency = new LatencyEvaluation(
            95.0, 21.0, Accuracy.MEASURED, 400, 350, Result.PASS
        );

        ThroughputEvaluation throughput = new ThroughputEvaluation(
            942.0, 1500.0, 1400.0, Result.WARN
        );

        EndpointEvaluation endpoint = new EndpointEvaluation(
            "test-endpoint",
            Result.WARN,
            List.of(latency),
            throughput,
            null
        );

        EvaluationResult result = new EvaluationResult(List.of(endpoint));

        writer.write(report, result);

        String output = baos.toString();

        assertTrue(output.contains("TestSimulation"));
        assertTrue(output.contains("test-endpoint"));
        assertTrue(output.contains("PASS") || output.contains("✓"));
        assertTrue(output.contains("WARN") || output.contains("⚠"));
        assertTrue(output.contains("p95"));
        assertTrue(output.contains("942"));
    }
}
```

**Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=ConsoleReportWriterTest -q`
Expected: Compilation error - ConsoleReportWriter class not found

**Step 3: Write minimal implementation**

```java
package net.safedata.performance.training.gatling.report;

import net.safedata.performance.training.gatling.analyzer.PercentileInterpolator.Accuracy;
import net.safedata.performance.training.gatling.model.*;

import java.io.PrintStream;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class ConsoleReportWriter {

    private static final String DOUBLE_LINE = "═".repeat(70);
    private static final String SINGLE_LINE = "─".repeat(70);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final NumberFormat NUM_FMT = NumberFormat.getInstance(Locale.US);

    private final PrintStream out;

    public ConsoleReportWriter() {
        this(System.out);
    }

    public ConsoleReportWriter(PrintStream out) {
        this.out = out;
    }

    public void write(GatlingReport report, EvaluationResult result) {
        writeHeader(report);

        for (EndpointEvaluation endpoint : result.endpointEvaluations()) {
            writeEndpoint(endpoint, report);
        }

        writeSummary(result);
    }

    private void writeHeader(GatlingReport report) {
        out.println(DOUBLE_LINE);
        out.println("GATLING REPORT ANALYSIS");
        out.println(DOUBLE_LINE);
        out.printf("Report:   %s%n", report.runInfo().simulationName());
        out.printf("Date:     %s%n", report.runInfo().date().format(DATE_FMT));
        out.printf("Duration: %s%n", report.runInfo().duration());
        out.println();
    }

    private void writeEndpoint(EndpointEvaluation endpoint, GatlingReport report) {
        out.println(SINGLE_LINE);
        out.printf("ENDPOINT: %s%n", endpoint.endpointName());
        out.println(SINGLE_LINE);

        // Get request stats from report
        report.getEndpoint(endpoint.endpointName()).ifPresent(stats -> {
            out.printf("Requests: %s total | %.2f%% errors%n%n",
                NUM_FMT.format(stats.totalRequests()),
                stats.errorPercent());
        });

        writeLatencySection(endpoint);
        writeThroughputSection(endpoint);
        writeErrorRateSection(endpoint);

        out.printf("%nEndpoint Result: %s%n", formatResult(endpoint.result()));
    }

    private void writeLatencySection(EndpointEvaluation endpoint) {
        if (endpoint.latencyEvaluations().isEmpty()) {
            return;
        }

        out.println("LATENCY:");
        for (LatencyEvaluation eval : endpoint.latencyEvaluations()) {
            String icon = getResultIcon(eval.result());
            String percentileLabel = formatPercentile(eval.percentile());
            String valueStr = formatLatencyValue(eval.actualMs(), eval.accuracy());
            String limitStr = formatLimits(eval.limitMs(), eval.warnMs().orElse(null));
            String accuracyStr = formatAccuracy(eval.accuracy());

            out.printf("  %s %s  %s: %s %s %s%n",
                icon,
                formatResult(eval.result()),
                percentileLabel,
                valueStr,
                limitStr,
                accuracyStr);
        }
        out.println();
    }

    private void writeThroughputSection(EndpointEvaluation endpoint) {
        endpoint.throughput().ifPresent(eval -> {
            out.println("THROUGHPUT:");
            String icon = getResultIcon(eval.result());

            String warnStr = eval.warnTps()
                .map(w -> String.format(", warn: %.0f", w))
                .orElse("");

            out.printf("  %s %s  Avg TPS: %.0f req/s (target: %.0f%s)%n",
                icon,
                formatResult(eval.result()),
                eval.actualTps(),
                eval.targetTps(),
                warnStr);
            out.println();
        });
    }

    private void writeErrorRateSection(EndpointEvaluation endpoint) {
        endpoint.errorRate().ifPresent(eval -> {
            out.println("ERROR RATE:");
            String icon = getResultIcon(eval.result());

            String warnStr = eval.warnPercent()
                .map(w -> String.format(", warn: %.2f%%", w))
                .orElse("");

            out.printf("  %s %s  %.2f%% (limit: %.2f%%%s)%n",
                icon,
                formatResult(eval.result()),
                eval.actualPercent(),
                eval.limitPercent(),
                warnStr);
            out.println();
        });
    }

    private void writeSummary(EvaluationResult result) {
        out.println(DOUBLE_LINE);
        out.printf("OVERALL RESULT: %s%n", formatResult(result.overallResult()));
        out.printf("  - %d endpoint(s) analyzed%n", result.endpointEvaluations().size());
        out.printf("  - %d warning(s), %d failure(s)%n",
            result.warningCount(), result.failureCount());
        out.println(DOUBLE_LINE);
    }

    private String getResultIcon(Result result) {
        return switch (result) {
            case PASS -> "✓";
            case WARN -> "⚠";
            case FAIL -> "✗";
        };
    }

    private String formatResult(Result result) {
        return result.name();
    }

    private String formatPercentile(double percentile) {
        if (percentile == (int) percentile) {
            return String.format("p%d", (int) percentile);
        }
        return String.format("p%.2f", percentile).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    private String formatLatencyValue(double value, Accuracy accuracy) {
        String prefix = accuracy == Accuracy.MEASURED ? "" : "~";
        return String.format("%s%.0fms", prefix, value);
    }

    private String formatLimits(int limitMs, Integer warnMs) {
        if (warnMs != null) {
            return String.format("(limit: %dms, warn: %dms)", limitMs, warnMs);
        }
        return String.format("(limit: %dms)", limitMs);
    }

    private String formatAccuracy(Accuracy accuracy) {
        return switch (accuracy) {
            case MEASURED -> "[measured]";
            case INTERPOLATED -> "[interpolated]";
            case EXTRAPOLATED -> "[extrapolated]";
        };
    }
}
```

**Step 4: Run test to verify it passes**

Run: `mvn test -Dtest=ConsoleReportWriterTest -q`
Expected: Test passes

**Step 5: Commit**

```bash
git add src/main/java/net/safedata/performance/training/gatling/report/ConsoleReportWriter.java \
        src/test/java/net/safedata/performance/training/gatling/report/ConsoleReportWriterTest.java
git commit -m "feat(gatling): add ConsoleReportWriter for formatted output"
```

---

## Task 15: Create GatlingReportAnalyzer Facade

**Files:**
- Create: `src/main/java/net/safedata/performance/training/gatling/GatlingReportAnalyzer.java`
- Test: `src/test/java/net/safedata/performance/training/gatling/GatlingReportAnalyzerTest.java`

**Step 1: Write the failing test**

```java
package net.safedata.performance.training.gatling;

import net.safedata.performance.training.gatling.model.*;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class GatlingReportAnalyzerTest {

    @Test
    void analyzeIntegrationTest() throws Exception {
        Path pdfPath = Path.of("VopProvisioning-07.10.2025.pdf");

        // Skip if PDF not available
        if (!pdfPath.toFile().exists()) {
            return;
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(baos);

        NFRConfig nfr = NFRConfig.builder()
            .endpoint("surepay-verify-account")
                .latency(90.0, 150, 120)
                .latency(95.0, 400, 350)
                .latency(99.99, 1000, null)
                .throughput(1500.0, 1350.0, 1400.0)
                .errorRate(0.1, 0.05)
            .endpoint("eba-payee-information")
                .latency(95.0, 500, null)
            .build();

        EvaluationResult result = GatlingReportAnalyzer.analyze(pdfPath, nfr, out);

        assertNotNull(result);
        assertEquals(2, result.endpointEvaluations().size());

        String output = baos.toString();
        assertTrue(output.contains("GATLING REPORT ANALYSIS"));
        assertTrue(output.contains("surepay-verify-account"));
        assertTrue(output.contains("OVERALL RESULT"));
    }
}
```

**Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=GatlingReportAnalyzerTest -q`
Expected: Compilation error - GatlingReportAnalyzer class not found

**Step 3: Write minimal implementation**

```java
package net.safedata.performance.training.gatling;

import net.safedata.performance.training.gatling.analyzer.NFREvaluator;
import net.safedata.performance.training.gatling.model.EvaluationResult;
import net.safedata.performance.training.gatling.model.GatlingReport;
import net.safedata.performance.training.gatling.model.NFRConfig;
import net.safedata.performance.training.gatling.parser.GatlingPdfParser;
import net.safedata.performance.training.gatling.report.ConsoleReportWriter;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;

public class GatlingReportAnalyzer {

    private final GatlingPdfParser parser;
    private final NFREvaluator evaluator;
    private final ConsoleReportWriter writer;

    public GatlingReportAnalyzer() {
        this(System.out);
    }

    public GatlingReportAnalyzer(PrintStream out) {
        this.parser = new GatlingPdfParser();
        this.evaluator = new NFREvaluator();
        this.writer = new ConsoleReportWriter(out);
    }

    public EvaluationResult analyze(Path pdfPath, NFRConfig nfrConfig) throws IOException {
        GatlingReport report = parser.parse(pdfPath);
        EvaluationResult result = evaluator.evaluate(report, nfrConfig);
        writer.write(report, result);
        return result;
    }

    // Static convenience method
    public static EvaluationResult analyze(Path pdfPath, NFRConfig nfrConfig, PrintStream out) throws IOException {
        return new GatlingReportAnalyzer(out).analyze(pdfPath, nfrConfig);
    }

    public static EvaluationResult analyze(String pdfPath, NFRConfig nfrConfig) throws IOException {
        return new GatlingReportAnalyzer().analyze(Path.of(pdfPath), nfrConfig);
    }
}
```

**Step 4: Run test to verify it passes**

Run: `mvn test -Dtest=GatlingReportAnalyzerTest -q`
Expected: Test passes (or skips if PDF not available)

**Step 5: Commit**

```bash
git add src/main/java/net/safedata/performance/training/gatling/GatlingReportAnalyzer.java \
        src/test/java/net/safedata/performance/training/gatling/GatlingReportAnalyzerTest.java
git commit -m "feat(gatling): add GatlingReportAnalyzer facade"
```

---

## Task 16: Create Example Main Class

**Files:**
- Create: `src/main/java/net/safedata/performance/training/gatling/GatlingNFRAnalyzerExample.java`

**Step 1: Write the example class**

```java
package net.safedata.performance.training.gatling;

import net.safedata.performance.training.gatling.model.EvaluationResult;
import net.safedata.performance.training.gatling.model.NFRConfig;
import net.safedata.performance.training.gatling.model.Result;

import java.nio.file.Path;

public class GatlingNFRAnalyzerExample {

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: GatlingNFRAnalyzerExample <path-to-pdf>");
            System.exit(1);
        }

        Path pdfPath = Path.of(args[0]);

        // Define NFRs
        NFRConfig nfr = NFRConfig.builder()
            .endpoint("surepay-verify-account")
                .latency(90.0, 150, 120)
                .latency(95.0, 400, 350)
                .latency(99.99, 1000, null)
                .throughput(1500.0, 1350.0, 1400.0)
                .errorRate(0.1, 0.05)
            .endpoint("eba-payee-information")
                .latency(90.0, 150, 120)
                .latency(95.0, 400, 350)
                .latency(99.99, 1000, null)
                .throughput(500.0, null, null)
                .errorRate(0.1, null)
            .build();

        // Analyze and print report
        EvaluationResult result = GatlingReportAnalyzer.analyze(pdfPath.toString(), nfr);

        // Exit with appropriate code
        System.exit(result.overallResult() == Result.FAIL ? 1 : 0);
    }
}
```

**Step 2: Verify compilation**

Run: `mvn compile -q`
Expected: No errors

**Step 3: Commit**

```bash
git add src/main/java/net/safedata/performance/training/gatling/GatlingNFRAnalyzerExample.java
git commit -m "feat(gatling): add example main class demonstrating usage"
```

---

## Task 17: Run Full Test Suite and Verify

**Step 1: Run all tests**

Run: `mvn test -q`
Expected: All tests pass

**Step 2: Run the example (if PDF exists)**

Run: `mvn exec:java -Dexec.mainClass="net.safedata.performance.training.gatling.GatlingNFRAnalyzerExample" -Dexec.args="VopProvisioning-07.10.2025.pdf" -q`
Expected: Report analysis output displayed

**Step 3: Final commit if any adjustments needed**

```bash
git status
# If clean, implementation is complete
```