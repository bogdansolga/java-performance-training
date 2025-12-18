# Gatling NFR Analyzer - Design Document

## Overview

A Java program that parses Gatling performance test reports (PDF format) and compares results against configurable Non-Functional Requirements (NFRs), producing a human-readable summary with PASS/WARN/FAIL verdicts.

## Requirements Summary

| Decision | Choice |
|----------|--------|
| Input format | PDF export of Gatling report |
| NFR definition | Java configuration (compile-time, type-safe) |
| Output format | Console only |
| Pass/Fail model | Tiered: PASS / WARN / FAIL |
| WARN thresholds | Configurable per NFR |
| Endpoint analysis | Per-endpoint (each gets own NFRs) |
| Percentile handling | Interpolate/extrapolate missing values |
| TPS validation | Average + sustained minimum |
| Duration check | Ignore (validate metrics only) |
| Report count | Single report per run |
| Error rate | Optional NFR |

## Data Model

### NFR Configuration

```
NFRConfig
├── EndpointNFR
│   ├── endpointName: String
│   ├── latencyThresholds: List<LatencyThreshold>
│   │   ├── percentile: double (e.g., 90.0, 95.0, 99.99)
│   │   ├── maxMs: int
│   │   └── warnMs: int (optional)
│   ├── throughput: ThroughputThreshold
│   │   ├── targetTps: double
│   │   ├── sustainedMinTps: double
│   │   └── warnTps: double (optional)
│   └── errorRate: ErrorRateThreshold (optional)
│       ├── maxPercent: double
│       └── warnPercent: double (optional)
```

### Gatling Report (Parsed)

```
GatlingReport
├── runInfo: RunInfo (date, duration, description)
└── endpoints: List<EndpointStats>
    ├── name: String
    ├── totalRequests, okCount, koCount, errorPercent
    ├── requestsPerSecond: double
    └── percentiles: Map<Integer, Integer> (50->val, 75->val, 95->val, 99->val)
```

## PDF Parsing Strategy

- Library: Apache PDFBox
- Strategy: Text extraction + regex parsing
- Extract run info section (date, duration, description)
- Parse requests table rows with pattern matching
- Handle column alignment and formatting variations
- Throw clear errors with context on parse failures

## Percentile Interpolation

### Available from Gatling
50th, 75th, 95th, 99th percentiles

### Algorithm

**Interpolation** (between known points):
```
p90 = p75 + (p95 - p75) * ((90 - 75) / (95 - 75))
```

**Extrapolation** (beyond known range, log-linear for long-tail):
```
p99.99 = p99 * (p99/p95)^((99.99 - 99) / (99 - 95))
```

### Accuracy Flags
- `measured` - directly from report
- `interpolated` - calculated between two known points
- `extrapolated` - estimated beyond known range

## Evaluation Logic

### Latency
- PASS: actual <= maxMs (and < warnMs if defined)
- WARN: actual <= maxMs but >= warnMs
- FAIL: actual > maxMs

### Throughput
- PASS: avgTps >= targetTps (and >= warnTps)
- WARN: avgTps >= targetTps but < warnTps
- FAIL: avgTps < targetTps OR minTps < sustainedMinTps

### Error Rate
- PASS: errorPercent <= maxPercent (and < warnPercent)
- WARN: errorPercent <= maxPercent but >= warnPercent
- FAIL: errorPercent > maxPercent

### Overall
- PASS: All NFRs pass
- WARN: No failures, at least one warning
- FAIL: At least one NFR failed

## Console Output Format

```
═══════════════════════════════════════════════════════════════════
GATLING REPORT ANALYSIS
═══════════════════════════════════════════════════════════════════
Report: VOPProvisioningAPISimulation
Date:   2025-10-06 12:04:51 GMT
Duration: 15m 2s

───────────────────────────────────────────────────────────────────
ENDPOINT: surepay-verify-account
───────────────────────────────────────────────────────────────────
Requests: 850,730 total | 0.00% errors

LATENCY:
  ✓ PASS  p90:  ~19ms   (limit: 150ms, warn: 120ms) [interpolated]
  ✓ PASS  p95:   21ms   (limit: 400ms, warn: 350ms) [measured]
  ✓ PASS  p99.99: ~58ms (limit: 1000ms)             [extrapolated]

THROUGHPUT:
  ⚠ WARN  Avg TPS: 942 req/s (target: 1500, warn: 1400)

ERROR RATE:
  ✓ PASS  0.00% (limit: 0.10%)

Endpoint Result: WARN
═══════════════════════════════════════════════════════════════════
OVERALL RESULT: WARN
  - 2 endpoints analyzed
  - 1 warning(s), 0 failure(s)
═══════════════════════════════════════════════════════════════════
```

## Java Class Structure

```
Package: net.safedata.performance.training.gatling

├── model/
│   ├── NFRConfig.java
│   ├── EndpointNFR.java
│   ├── LatencyThreshold.java
│   ├── ThroughputThreshold.java
│   ├── ErrorRateThreshold.java
│   ├── GatlingReport.java
│   ├── EndpointStats.java
│   └── EvaluationResult.java
│
├── parser/
│   └── GatlingPdfParser.java
│
├── analyzer/
│   ├── PercentileInterpolator.java
│   └── NFREvaluator.java
│
├── report/
│   └── ConsoleReportWriter.java
│
└── GatlingReportAnalyzer.java (facade)
```

## Usage Example

```java
NFRConfig nfr = NFRConfig.builder()
    .endpoint("surepay-verify-account")
        .latency(90).maxMs(150).warnMs(120)
        .latency(95).maxMs(400).warnMs(350)
        .latency(99.99).maxMs(1000)
        .throughput().targetTps(1500).warnTps(1400).sustainedMin(1350)
        .errorRate().maxPercent(0.1)
    .endpoint("eba-payee-information")
        .latency(95).maxMs(500)
        // ...
    .build();

EvaluationResult result = GatlingReportAnalyzer.analyze("report.pdf", nfr);
```

## Dependencies

- Apache PDFBox (PDF parsing)
- Existing project dependencies (Spring Boot, etc.)