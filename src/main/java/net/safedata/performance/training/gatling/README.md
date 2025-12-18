# Gatling NFR Analyzer

A Java tool that parses Gatling PDF reports and compares performance metrics against configurable NFR (Non-Functional Requirements) thresholds.

## Quick Start

```java
NFRConfig nfr = NFRConfig.builder()
    .endpoint("my-api-endpoint")
        .latency(95.0, 400, 350)      // p95 < 400ms (warn at 350ms)
        .throughput(1500.0, null, null) // target 1500 TPS
        .errorRate(0.1, 0.05)          // < 0.1% errors (warn at 0.05%)
    .build();

EvaluationResult result = GatlingReportAnalyzer.analyze("report.pdf", nfr);
```

## NFR Configuration

### Latency Thresholds
```java
.latency(percentile, maxMs, warnMs)
```
- `percentile` - Target percentile (e.g., 90.0, 95.0, 99.99)
- `maxMs` - Maximum allowed latency in ms (FAIL threshold)
- `warnMs` - Warning threshold in ms (optional, use `null` to skip)

### Throughput Thresholds
```java
.throughput(targetTps, sustainedMinTps, warnTps)
```
- `targetTps` - Target transactions per second
- `sustainedMinTps` - Minimum sustained TPS (optional)
- `warnTps` - Warning threshold TPS (optional)

### Error Rate Thresholds
```java
.errorRate(maxPercent, warnPercent)
```
- `maxPercent` - Maximum allowed error percentage
- `warnPercent` - Warning threshold percentage (optional)

## Multi-Endpoint Configuration

```java
NFRConfig nfr = NFRConfig.builder()
    .endpoint("surepay-verify-account")
        .latency(90.0, 150, 120)
        .latency(95.0, 400, 350)
        .latency(99.99, 1000, null)
        .throughput(1500.0, 1350.0, 1400.0)
        .errorRate(0.1, 0.05)
    .endpoint("eba-payee-information")
        .latency(95.0, 500, null)
        .throughput(500.0, null, null)
    .build();
```

## Running from Command Line

```bash
mvn exec:java \
  -Dexec.mainClass="net.safedata.performance.training.gatling.GatlingNFRAnalyzerExample" \
  -Dexec.args="path/to/report.pdf" -q
```

## Output Format

```
══════════════════════════════════════════════════════════════════════
GATLING REPORT ANALYSIS
══════════════════════════════════════════════════════════════════════
Report:   VOPProvisioningAPISimulation
Date:     2025-10-06 12:04:51
Duration: 15m 2s

──────────────────────────────────────────────────────────────────────
ENDPOINT: surepay-verify-account
──────────────────────────────────────────────────────────────────────
Requests: 850,730 total | 0.00% errors

LATENCY:
  ✓ PASS  p90: ~14ms (limit: 150ms, warn: 120ms) [interpolated]
  ✓ PASS  p95: 15ms (limit: 400ms, warn: 350ms) [measured]

THROUGHPUT:
  ✗ FAIL  Avg TPS: 942 req/s (target: 1500, warn: 1400)

ERROR RATE:
  ✓ PASS  0.00% (limit: 0.10%, warn: 0.05%)

Endpoint Result: FAIL
══════════════════════════════════════════════════════════════════════
OVERALL RESULT: FAIL
  - 1 endpoint(s) analyzed
  - 0 warning(s), 1 failure(s)
══════════════════════════════════════════════════════════════════════
```

## Result Indicators

| Symbol | Result | Meaning |
|--------|--------|---------|
| ✓ | PASS | Metric within acceptable range |
| ⚠ | WARN | Metric approaching threshold |
| ✗ | FAIL | Metric exceeded threshold |

## Percentile Accuracy

| Label | Meaning |
|-------|---------|
| `[measured]` | Value directly from Gatling report |
| `[interpolated]` | Calculated between known percentiles |
| `[extrapolated]` | Estimated beyond known range |

## Exit Codes

- `0` - All NFRs pass (or only warnings)
- `1` - At least one NFR failed

## Programmatic Usage

```java
EvaluationResult result = GatlingReportAnalyzer.analyze("report.pdf", nfr);

if (result.overallResult() == Result.FAIL) {
    System.out.println("NFR validation failed!");
    System.out.println("Failures: " + result.failureCount());
    System.out.println("Warnings: " + result.warningCount());
}
```
