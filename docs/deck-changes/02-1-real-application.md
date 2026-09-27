# Deck 2.1 — Test real application

**STATUS: VERIFY DOCUMENT — dry-run only, nothing applied.**

Source read: `gslides.sh personal text 1hft4X0terH2e7xcHIwiXSXc9FRxnkMtUesYdny80PoI` on
2026-09-27, saved to `/tmp/deck-02-1-real-application.txt` (401 lines, 17 slides, indices
0–16). Read in full per brief instruction. The micro/macro/meso-benchmark taxonomy, the
threaded-microbenchmark caution, the warm-up/JIT-profiling material and the full-system
(multi-JVM) reasoning are all sound and timeless — none of it is touched below. Only the
two points the brief calls out are changed: the JMH slide's single dead-feeling tutorial
link, and the meso-benchmark (REST-request) slide, which never tells participants this is
literally what the labs do.

## Rows

| Slide | Anchor | Current context | Proposed text | Category | Source |
|---|---|---|---|---|---|
| 7 | http://tutorials.jenkov.com/java-performance/jmh.html | "JMH - the most used microbenchmark" slide, closing line: "A short intro - " + this URL run | https://github.com/openjdk/jmh — the JDK's own microbenchmark harness (OpenJDK Code Tools), with runnable sample benchmarks | correction | https://github.com/openjdk/jmh |
| 15 | measuring the response time of a REST request | "3. Meso-benchmarks" slide, second bullet run: "Frequent benchmark" + " - measuring the response time of a REST request" | measuring the response time of a REST request — exactly what this course's labs exercise: a REST endpoint under Gatling load, scored against NFR thresholds | addition | src/main/java/net/safedata/performance/training/gatling/README.md |

## Notes

- **JMH anchor verification**: `grep -o -F -- "http://tutorials.jenkov.com/java-performance/jmh.html" /tmp/deck-02-1-real-application.txt | wc -l` → `1`. The URL is its own text
  run (separate from the "A short intro - " lead-in run), so it can be replaced without
  touching the lead-in sentence.
- **REST anchor verification**: `grep -o -F -- "measuring the response time of a REST request" /tmp/deck-02-1-real-application.txt | wc -l` → `1`. This phrase is a single run (the
  bold "Frequent benchmark" lead-in is a separate run), so the addition appends onto the
  end of an existing sentence rather than creating a new bullet.
- **Lab-connection claim grounded in repo, not asserted on faith**: the repo carries a
  Gatling NFR analyzer (`src/main/java/net/safedata/performance/training/gatling/`, 13
  test classes under `src/test/.../gatling/`) that parses Gatling load-test output and
  evaluates per-endpoint latency/throughput/error-rate against configurable NFR
  thresholds — i.e. the course's labs do measure REST endpoints under load, which is
  exactly what the deck's meso-benchmark example describes. Also documented in
  `docs/superpowers/specs/2026-09-22-java-perf-labs-and-deck-refresh-design.md` §7.2
  ("Per lab: a defect profile, a Gatling simulation, an `NFRConfig`, and a `lab.sh` /
  `lab.ps1` driver that runs baseline → captures → ... → re-runs").
- **Both brief's quoted phrases usable, after re-deriving against the actual dump.** The
  brief's own paraphrases ("JMH third-party", "meso-benchmark REST") were not verbatim
  substrings — both rows above use anchors re-derived from the grepped dump, not the
  brief's wording.
- **No structural rows.** Neither change adds a slide, bullet, or paragraph — both are
  same-run text replacements/extensions, so no Manual actions are needed here.
- **No version numbers introduced**, so no additional citation risk beyond the JMH repo
  URL and the internal lab reference above.

## Manual actions (trainer, in Slides editor — not scripted)

None.

## Verify

```
./scripts/deck-check.sh docs/deck-changes/02-1-real-application.md 1hft4X0terH2e7xcHIwiXSXc9FRxnkMtUesYdny80PoI
./scripts/deck-apply.sh docs/deck-changes/02-1-real-application.md 1hft4X0terH2e7xcHIwiXSXc9FRxnkMtUesYdny80PoI --dry-run
```

Both rows are slide-scoped (7, 15); each anchor's deck-wide occurrence count matches its
declared scope (1 in, 0 out for both), so no `outside-scope-ok` marker is needed.

## Before/after slide count

17 → 17 (no change — both edits are in-place text swaps, nothing structural was
identified worth flagging).
