# Deck changes applied on 2026-09-29 — for review

Slide numbers are as the Slides editor shows them (1-based). Full rows, anchors and sources
are in each deck's change document.

## 1 — Java performance management overview

| Slide | Was | Now |
|---|---|---|
| 3 | Three new GCs (G1, ZGC & Shenandoah) | G1 (default since JDK 9), plus ZGC (generational mode only since JDK 24) & Shenandoah (generational mode a product feature since JDK 25) |
| 5 | Client class = any 32-bit JVM running on Microsoft Windows, or a machine with one CPU | fewer than 2 CPUs or less than 1792 MB of memory (cgroup limits count), regardless of OS |
| 5 | Server class machines - all other machines (including all 64-bit JVMs) | Server class machines - all other machines |
| 5 | Ex: the default Garbage Collector for a platform - determined by the machine class | Ex: the default Garbage Collector - Serial below 2 CPUs or 1792 MB of memory, G1 otherwise (ergonomics reads cgroup CPU and memory limits) |

Also removed the two empty bullets the deletions left behind on slide 5.

## 2.1 — Test the real application

| Slide | Was | Now |
|---|---|---|
| 8 | http://tutorials.jenkov.com/java-performance/jmh.html | https://github.com/openjdk/jmh — the JDK's own microbenchmark harness (OpenJDK Code Tools), with runnable sample benchmarks |
| 15 | measuring the response time of a REST request | … — exactly what this course's labs exercise: a REST endpoint under Gatling load, scored against NFR thresholds |

## 2.2 — Throughput, batching and response times

| Slide | Was | Now |
|---|---|---|
| 10 | risk that a client cannot send data fast enough to the server | risk that the client is the bottleneck, not the server |
| 13 | … large outliers have a large effect on the average response time | … large outliers pull the average; a percentile barely moves |
| 17 | If needed: 95th% / 99th% response time | removed (and its empty bullet) |

## 2.3 — Variability

| Slide | Was | Now |
|---|---|---|
| 5 | the baseline and specimen are each run 3 times | … — the same reason this course's lab harness reruns each scenario rather than measuring once |

## 2.4 — Test early, test often

| Slide | Was | Now |
|---|---|---|
| 3 | All code changes must be checked into at an early point in the release cycle | In continuous delivery there is no feature-freeze date — regressions must be caught per merge request, not at a checkpoint |
| 11 | 12 factor app? | In containers, this changes again: since JDK 10 (and 8u191+), the JVM reads the cgroup CPU quota and memory limit instead of the host's — "the target system" means the container's assigned limits, not the physical core count |

## 3 — A performance toolbox

| Slide | Was | Now |
|---|---|---|
| 3 | http://java.net | https://openjdk.org |
| 4 | vmstat, iostat, prstat | vmstat, iostat, pidstat, ss, prstat |

## 4.1 — Infrastructure, architecture and code improvements

| Slide | Was | Now |
|---|---|---|
| 16 | custom thread-pool | … — or, since JDK 21, virtual threads for blocking, I/O-bound tasks, which need no pool sizing at all |
| 18 | Threads | Threads — platform threads; virtual threads (final since JDK 21) are cheap per task and are not meant to be pooled |
| 19 | Investing time in optimizing the database access | … (see Lab 2, an N+1-query hunt) |
| 28 | Tomcat thread pool | … — a platform-thread pool; since JDK 21, virtual threads let each request run on its own thread instead |

## 6.2 — Memory leaks

| Slide | Was | Now |
|---|---|---|
| 9 | … preventing the object from being garbage collected | … This pooled-thread hazard changes shape with virtual threads (final since JDK 21): they are not pooled, so a virtual thread does not keep a ThreadLocal alive this way |
| 18 | Avoid using (/ implementing) the | finalize() is deprecated for removal since JDK 18 — use java.lang.ref.Cleaner instead. Avoid implementing the |
| 18 | Use the latest LTS version of Java | Use a supported LTS Java version — 17, 21 or 25 |

## Worth a second look

- Deck 1, slide 5, "The differences: the compiler used for a platform" — on a 64-bit JVM
  the machine class no longer changes the compiler (tiered compilation is on in both).
  Not changed; flagged.
- Deck 2.1 slide 15, 2.3 slide 5 and 4.1 slide 19 point at the labs, which are being built now.
- Deck 1, slide 5 — the Serial/G1 rule is confirmed in the 17u, 21u and 25u sources
  (`gcConfig.cpp`, `os.cpp`: 2 CPUs and 2 GB − 256 MB). OpenJDK mainline (after 25) now
  selects G1 regardless of machine class, so this slide will need a badge once a newer
  LTS is covered.
