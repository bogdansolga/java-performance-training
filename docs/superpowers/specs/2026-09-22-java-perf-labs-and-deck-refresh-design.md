# Java Performance training — hands-on labs and deck modernization

**Date:** 2026-09-22

**Status:** Design, awaiting review

**Repos touched:** `java-performance-training` (this repo), the Google Slides deck set referenced from `Java performance training.md`

---

## 1. Why

Two inputs drive this work.

**Participant feedback** (`feedback.txt`, verbatim): the course was perceived as too oriented toward theory, slides, tools and configuration, and too little toward practically solving problems. The five specific asks:

1. more concrete and complex examples, including production cases from the trainer's experience
2. more code and examples of actual Java application optimization
3. real labs, not just running commands and reading tools
4. problems that are reproduced, investigated and then fixed, with before/after results
5. applications prepared by the trainer, so participants do not depend on their own projects

**Deck accuracy.** A read of the deck set found version claims that are wrong on current LTS releases, a large block of JIT content built on distinctions that no longer exist, and no coverage of anything added after Java 11. The course baseline is now Java 17–25.

Deck modernization is both the **top priority and the harder half**. Reviewing, correcting, cleaning and modernizing ~19 decks is more demanding than building the labs: the deck work is judgement-heavy and spread across presentations that cannot be compiled, tested or diffed, whereas the lab code is conventional engineering with a test suite behind it.

## 2. Goals

- Every version-dependent claim in the deck set is correct for **Java 11, 17, 21 and 25**, and says which release it applies to, ONLY if there are major differences.
- Five labs where a participant reproduces a defect, investigates it with real tools, fixes it, and sees a measured before/after.
- A measurement harness that produces comparable numbers rather than terminal output that scrolls away.
- Setup cost small enough that lab time goes to the work.

## 3. Non-goals

- Labs for decks 3.3 (Disk) and 3.4 (Network). Disk and network defects cannot be reproduced convincingly on a laptop; simulating them would reproduce exactly the "just running commands in tools" problem the feedback names. These two decks are reviewed and shortened, and stay tool-demo sections.
- We will simplify and shorten those decks, to state the minimal needed.
- Rewriting decks that are already correct. Deck 2.3 (Variability) needs almost nothing — t-tests and p-values do not age.
- Migrating the course off Google Slides.

## 4. Decisions

| # | Decision | Rationale |
|---|---|---|
| D1 | One app, defects behind Spring profiles | Matches the existing app; `load-test.sh` and `gc-analyze.sh` already work against it. Alternative (one module per lab) costs more repo than it returns |
| D2 | Harness captures and compares; counts asserted where deterministic | Before/after must survive the session as an artifact. Duration thresholds are flaky across laptops; counts (SQL statements, retained objects, threads) are not |
| D3 | Harness is **Gatling**, fallback **JMeter** | `origin/gatling` already has a tested NFR evaluator and threshold model. Retires `load-test.sh`. To be merged into `master` (the repo's default branch; there is no `main`) |
| D4 | Labs are 1, 2, 3, 5, 8 from the catalogue (§7) | One leak, one data-access, one concurrency, one GC, one JIT — covers the decks with the most airtime. We must have a slide for each of these issues, to describe the issue, not the fix for it |
| D5 | Deck edits: change-proposal doc first, then applied directly | Version claims need a second reader. The agent got CMS's removal release wrong while reviewing the deck that also had it wrong |
| D6 | Baseline JDK **17 and 21**, **25 optional**. 25 is the latest LTS version, unlikely that a lot of participants will use it. We will also update the slides with details that pertain just to Java 11, where they are major | Both LTS releases participants are likely to be on. 25 unlocks compact object headers as a live before/after. **11 is deck coverage only, never a lab runtime** — the lab app cannot run on it, since Spring Boot 4 requires 17+ |
| D7 | Compile once targeting 17, run on three JVMs | Same bytecode under different JVMs isolates the JVM as the variable — exactly what lab 5 teaches |
| D8 | Participants work on a local branch from a tag | No collaborator management on a personal repo, no stale branches to clean up |
| D9 | Virtual threads are in scope, with a runnable code example | Final in 21 and absent from every deck. A course baselined on 21 that teaches thread-pool sizing cannot omit them |
| D10 | **Two implementation plans**, not one | The workstreams share three touchpoints and no files. Splitting keeps deck work off the critical path of Gatling reconciliation |

## 5. Schedule

5 days × 4 hours = 20h, less ~10 min/hour of breaks ≈ **17h of content**.

| Lab | Duration |
|---|---|
| 1 Unbounded retention | 1h |
| 2 N+1 queries | 1h |
| 3 Lock contention | 1h |
| 5 GC mismatch | 2h |
| 8 Code cache exhaustion | 1h |
| **Total** | **5–6h (~29–35%)** |

Lab 5 might get two hours, because it sweeps three collectors across three JVMs. Fallback - 1h, with just one collector. The remaining ~11–12h covers the deck sections, which requires decks 2/2.1–2.4 and 3/3.1–3.4 to be reviewed, refined and reduced (§6.4).

## 6. Workstream 1 — deck modernization

### 6.1 Version convention

Decks are *narrated* against **21** as the spoken baseline. This is distinct from D6, which fixes the JDKs the labs *run on* (17 and 21, 25 optional): the slides tell one story, the labs execute on several runtimes. A slide carries a version badge **only where the difference is major** — a changed default, a removed flag, a new collector mode. Cosmetic or version-number-only differences are left unbadged. The badge is a signal, and badging every slide would spend it.

**Badge rendering.** A badge lists **only the releases where the claim holds**, e.g. `[17 · 21 · 25]` for something absent on 11, or `[11 · 17]` for advice that stopped applying after 17. There is no colour distinction and no full-set form.

This supersedes the earlier full-set-with-muted-colours convention. Two reasons it is better: the badge is readable without the audience decoding a contrast difference on a projector, and — decisively — `gslides.sh replace` can only insert plain text, so a colour-based badge would have required a manual styling pass on **every badged slide across the whole deck set**. A plain-text badge is fully scriptable.

Worked example — ZGC generational mode: `[21 · 25]`. The 32-bit heap-sizing advice: `[11 · 17]`.

### 6.2 Change-proposal process

One file per deck at `docs/deck-changes/NN-<deck-slug>.md`, plus `docs/deck-changes/README.md` as index. Row format:

| Slide | Current text | Proposed text | Category | Source |
|---|---|---|---|---|

Categories: `correction`, `removal`, `addition`, `reduction`, `lab-slide`.

Every `correction` and `addition` row carries a source URL (JEP, release notes, vendor docs). The trainer reviews and annotates with `[BS]` comments. Approved rows are then applied directly via `gslides.sh personal replace` / `set-text`, one pass per deck. The change files stay in the repo as the record of what changed and why.

### 6.3 Corrections found so far

| Deck | Slide | Current | Correct |
|---|---|---|---|
| 5.1 | 22 | "G1 enabled by default since JDK 11" | JDK 9 (JEP 248) |
| 5.1 | 18/20 | CMS "no longer available since Java 17" | Removed in JDK 14 (JEP 363) |
| 5.1 | 24 vs 38 | ZGC "mainstream since JDK 15" vs "since Java 11" | Introduced 11 (experimental), production 15; generational mode default in 23 (JEP 474); non-generational removed in 24 (JEP 490). **G1 remains the JVM default** |
| 5.1 | 27 vs 38 | Shenandoah "introduced in JDK 12" vs "mainstream since Java 15" | Introduced 12, production 15 |
| 5.1 | 37 | Summary lists CMS as a live option | Remove |
| 4.2 | 8–15 | 32-bit vs 64-bit trade-off, "heap < 3 GB → use 32-bit", "5–20% faster in a 32-bit JVM" | **Retain, scoped to "Java 17 and prior"**. JEP 449 deprecated 32-bit x86 in 21; JEP 479 removed the Windows port in 24 |
| 4.2 | 8–15 | `-client` / `-server` / `-d64` flag selection | **Mark dead since JDK 9** — `-d64` was removed in 9, and `-client` is a no-op on any 64-bit JVM. These were already false on 17, so the "17 and prior" scope does not cover them |
| 1 | 4 | Ergonomics framed as "client class = any 32-bit JVM on Windows" | Rewrite for container-aware ergonomics |
| 3 | 2 | `http://java.net` | Dead; replace |
| 5.3 | — | `-XX:+UseParallelOldGC`, `-XX:+UseParNewGC` | Deprecated in 14 / removed |
| 7.1 | 1, 16 | `jhat` listed as a JDK tool | Removed in JDK 9 (JEP 241). Replace with `jcmd GC.heap_dump` plus JMC / Eclipse MAT |
| 7.1 | 18 | Java Flight Recorder listed under **Paid** | Free and open-source since JDK 11 (JEP 328). Move to the free list |
| 7.1 | 18 | JProbe, XRebel, Stackify Prefix | Verify each is still current; JProbe is already flagged in-deck as doubtful |

### 6.4 Review, refine and modernize — decks 2.x and 3.x

| Deck | Slides | Assessment | Action |
|---|---|---|---|
| 2 Workflow | 3 | Index only | Keep |
| 2.1 Test the real application | 17 | Micro/macro/meso taxonomy is sound. JMH is one slide and a link | Expand JMH; connect meso-benchmarks explicitly to the Gatling labs |
| 2.2 Throughput, batching, response times | 18 | Sound but long | Simplify client-overload, average-vs-90th, and outlier slides (trainer request) |
| 2.3 Variability | 10 | Correct and timeless | Light trim of the statistics run; connect to the harness's repeated runs |
| 2.4 Test early, test often | 17 | CI framing is waterfall-era ("feature-freeze date") | Modernize to CI/CD and containers; reduce |
| 3 Toolbox | 5 | Index; dead `java.net` link | Fix link, keep |
| 3.1 Execution profiling | **3** | Thinnest deck in the set. Logger, `StopWatch`, AOP, P6Spy only. No JFR, async-profiler, flame graphs, JMC | **Expand substantially** — this is the deck the labs lean on hardest |
| 3.2 CPU usage | 12 | Sound on user/system time; predates containers | Add cgroup CPU limits, `UseContainerSupport`, `availableProcessors()` under limits; revisit thread-pool advice for virtual threads |
| 3.3 Disk usage | 11 | `iostat` and swapping; assumes spinning disks | Shorten; note NVMe and container storage |
| 3.4 Network usage | 6 | `netstat` is superseded by `ss` on Linux | Shorten; modernize tool names |

**Direction of travel.** Every deck in this range is reduced except **3.1, which is expanded**. That is a deliberate exception to "review and reduce": 3.1 is three slides, and it is where JFR, async-profiler, flame graphs and JMC belong — the tooling every lab depends on to investigate its defect. Reducing it would leave the labs without the deck that teaches how to look. The time it gains comes out of 2.1, 2.2, 2.4, 3.2, 3.3 and 3.4.

### 6.5 New content

| Topic | JEP | Lands in |
|---|---|---|
| Generational ZGC — opt-in 21, default mode 23, non-gen removed 24 | 439 / 474 / 490 | 5.1, 5.2 |
| Compact object headers — product in 25, ~22% heap reduction | 519 | 5.3, 6.1 |
| AOT class loading and linking, method profiling, CLI ergonomics | 483 / 515 / 514 | 4.2 |
| Generational Shenandoah | 521 | 5.1 |
| **Virtual threads** | 444 (final in 21) | threading stretch topic, 3.2, lab 3 |

Virtual threads appear nowhere in the current deck set. For a course baselined on 21 that spends a session on thread pools, this is a larger gap than any single version error.

Coverage is slides **plus a runnable example** in this repo (D9): platform threads against virtual threads on the same blocking workload, showing throughput under a fixed-size pool versus a virtual-thread executor, and the pinning case where a `synchronized` block negates the benefit. The pinning half attaches directly to **lab 3**, whose defect is lock contention — the same lesson in its modern form, which is why the example lives alongside the lab rather than in a deck appendix. The example itself is among the **cheapest items in the plan**; the effort in virtual-threads coverage sits in the deck side — reworking the thread-pool advice in 3.2 and the threading stretch topic so the platform-thread guidance and the virtual-thread guidance do not contradict each other.

### 6.6 New lab slides

- One **"The five labs"** slide in the Training overview deck, immediately after the objectives: defect, what is measured, which session.
- One **lab intro slide** inside each owning deck (6.2, 4.1, threading, 5.2, 4.2) at the point the lab begins. Per D4 it **describes the issue only, never the fix** — the slide sets up the investigation rather than short-circuiting it.
- One **prerequisites slide** mirroring `PREREQUISITES.md`.

### 6.7 Deck 7.1 — replacing the GlassFish screenshots

Deck 7.1 (*Java monitoring & profiling tools*, 44 slides) carries its central argument on screenshots of a **GlassFish** server startup, captured in tools that have all aged out:

| Slides | Screenshot | Tool |
|---|---|---|
| 24–25 | Sampling profile of GlassFish startup | unnamed sampler |
| 27–28 | Same startup, instrumented | unnamed instrumenting profiler |
| 34–35 | Thread timeline, blocking methods | NetBeans profiler |
| 38–40 | Native profile, JVM-System time | Oracle Developer Studio (discontinued) |

**Action.** Replace all of them with generated **SVG diagrams exported to PNG**, in the house style already established in `master-claude-code-course/docs/diagrams`: 1280×720 viewBox, white ground, 22px bold title with a 13px muted subtitle, rounded containers, two-column comparison with tinted header bands, `#5e6680` secondary text, and a single footer line carrying the takeaway. `A-12-agentic-search-vs-rag.svg` is a direct structural template for the sampling-vs-instrumenting pair.

**Diagrams to produce** (working set, three):

1. **What each mode actually records** — two columns. Sampling: timer fires, thread stacks read, method attributed for the whole interval. Instrumenting: bytecode rewritten on load, every entry and exit counted, exact invocation counts.
2. **Safepoint bias** — why a genuinely hot method can be nearly invisible to a sampler, and why the same method dominates an instrumented profile. This is the deck's strongest existing insight (slide 31) and it is currently carried by prose alone.
3. **Complementarity** — the same application profiled both ways yielding different top methods, with what each mode is trustworthy for: sampling for *where wall-clock goes*, instrumenting for *how often something is called*. This is the slide that carries the intent.

**Text changes alongside.** Narration moves off GlassFish to a tool-neutral framing; Oracle Developer Studio and the NetBeans profiler give way to **async-profiler** (which exists precisely to defeat the safepoint bias of slide 31) and JFR; and the run is **simplified and shortened** — 44 slides is the longest deck in the set, and the screenshot walkthroughs are the most cuttable part of it.

**Intent, stated for the plan:** attendees should see, visually, that sampling and instrumenting collect *different data about the same program*, and that the two are complementary rather than competing. Diagram 3 is the one that must land; 1 and 2 build to it.

### 6.8 Java 11 → 17/21 — differences worth stating

11 joins the covered set because participants may still be running it. It is a **deck-coverage release only** (D6): the lab app requires 17+, so nothing is executed on 11.

Only differences that change behaviour or advice get stated — the badge rule of §6.1 applies:

| Difference | 11 | 17 / 21 | Deck |
|---|---|---|---|
| **CMS** | Present and usable | Removed in 14 (JEP 363) | 5.1, 5.2 |
| **Elastic Metaspace** | Metaspace does not return memory to the OS | JEP 387 (16) reduces footprint and returns memory | 5.3 |
| **ZGC** | Experimental, 64-bit Linux only | Production since 15; generational mode default in 23 | 5.1, 5.2 |
| **Shenandoah** | Absent | Production since 15 | 5.1 |
| **JFR event streaming** | Absent | JEP 349 (14) enables continuous consumption | 7.2 |
| **Virtual threads** | Absent | Final in 21 | 3.2, threading |
| **`-XX:+UseParallelOldGC`** | Valid | Deprecated in 14 | 5.3 |

CMS and elastic Metaspace matter most. CMS because deck 5.1 devotes several slides to a collector an 11 user still has and a 17 user cannot get; elastic Metaspace because deck 5.3's Metaspace sizing advice is materially different before and after 16.

## 7. Workstream 2 — lab platform

### 7.1 Catalogue

| # | Defect | Present today | Measured by | Deck | Mechanism |
|---|---|---|---|---|---|
| 1 | Unbounded retention — `ProductService.products` grows every scheduled run | Yes, live | Heap after forced full GC; heap dump dominator tree | 6.1, 6.2 | capture + count |
| 2 | N+1 across `Store`/`Section`/`Discount`/`Product` | Entities and p6spy exist | SQL statement count | 4.1 | capture + count |
| 3 | Lock contention on `getSynchronizedProducts` | Yes | p90 under load; JFR *Monitor blocked* | threading | capture |
| 5 | GC mismatch — Serial (current default in `vm-options.txt`) vs G1 vs ZGC | Flags present | Pause distribution + p90 | 5.1–5.3 | capture |
| 8 | Code cache exhaustion via reduced `-XX:ReservedCodeCacheSize` | Flag present | Throughput cliff; JFR compilation events | 4.2 | capture |

Deferred to backlog: ForkJoinPool common-pool saturation, allocation pressure, `ThreadLocal` leak on pooled threads.

Lab 8 matters beyond its own session. Per the revised §6.3, the 32-bit JIT content is **retained and scoped to "Java 17 and prior"** rather than removed, so lab 8 is no longer a replacement for it — it is the *current-LTS* JIT failure mode sitting alongside the historical material, giving deck 4.2 something demonstrable on 21 and 25.

### 7.2 Harness

Built on `origin/gatling`, which already contains `NFREvaluator`, `LatencyThreshold`, `ThroughputThreshold`, `ErrorRateThreshold`, `EndpointNFR`, `NFRConfig`, `PercentileInterpolator` and `ConsoleReportWriter`, with 13 test classes.

Two changes on adoption:

1. Replace `GatlingPdfParser` with Gatling's `simulation.log` / JSON output. Parsing a generated PDF is a fragile seam.
2. Keep the threshold model unchanged — it is the count-assertion mechanism of D2.

Reconciliation: the branch diverged at `10988d8`; `master` has since gained eight commits including `load-test.sh` and `gc-analyze.sh`. The branch also deletes `vm-options.txt` and rewrites `ProductService`. **Decision point:** if reconciliation exceeds roughly one day, fall back to JMeter.

Per lab: a defect profile, a Gatling simulation, an `NFRConfig`, and a `lab.sh` / `lab.ps1` driver that runs baseline → captures → (participant fixes) → re-runs → prints a before/after table into `results/<branch>/<lab>/<timestamp>/`.

`gc-analyze.sh` / `.ps1` are kept — they serve lab 5 directly. `load-test.sh` is retired.

### 7.3 Branch workflow

Three commands, documented in a minimal `docs/LAB-WORKFLOW.md` written when the labs are prepared:

```bash
git switch -c firstname-lastname labs-2026-09     # start from the tagged baseline
# ... reproduce, investigate, fix, re-run ...
git diff firstname-lastname solution/lab-1-retention
```

- `master` stays clean; every defect sits behind a profile disabled by default.
- The trainer tags a start point per course run, so participants begin from a fixed commit while `master` keeps improving.
- Work stays local. Anyone wanting to keep or share it pushes to their own fork. No collaborator management, no stale branches.
- Solutions live on `solution/lab-N-<slug>` branches on origin, absent from `PREREQUISITES.md`, revealed at each lab's end.

Comparing a participant's fix against the trainer's is itself a before/after — at the code level, complementing the metrics level.

### 7.4 Prerequisites

- `scripts/preflight.sh` **and** `scripts/preflight.ps1`, matching the existing `gc-analyze` precedent.
- Checks: JDK 17 and 21 present and on `PATH`, Maven wrapper resolves, port 8080 free, dependencies pre-fetched, JMC available, disk space for heap dumps, optional JDK 25.
- `PREREQUISITES.md` — short, participant-facing.
- `docs/PREFLIGHT-TROUBLESHOOTING.md` — **every check preflight can fail has a named remedy**. A preflight script that reports failures it cannot help with creates more problems than it solves; the mapping doc is a hard requirement, not an addition.

Run before day one, so a red line costs nothing rather than costing the room twenty minutes at 09:15.

### 7.5 Repo hygiene

| Item | Action |
|---|---|
| `README.md` tells participants to set `-Xverify:none` | Obsolete; rejected or ignored on Java 17+. Rewrite the OOM instructions |
| `vm-options.txt` starts with `-server` | No-op on 17+, and the stale flag deck 4.2 teaches. Remove; split per-lab |
| `.DS_Store` — 7 untracked, absent from `.gitignore` | Add to `.gitignore`, delete existing |
| `pom.xml` targets 21 | Target 17 (D7); Spring Boot 4.0 requires 17+ |

## 8. Sequencing

| Phase | Work | Gate |
|---|---|---|
| 1 | Repo hygiene, `docs/`, deck change-proposal docs | **Trainer reviews, adds `[BS]` comments** |
| 2 | Harness reconciliation (runs during review) | Gatling-vs-JMeter decision point |
| 3 | Apply approved deck edits | — |
| 4 | Labs 1, 2, 3, 5, 8 | — |
| 5 | Prerequisites, preflight, troubleshooting map, lab slides | — |

Phase 1 starts first because deck work is top priority and the review gate is the long pole; phase 2 fills the waiting time.

### 8.1 Two implementation plans

Per D10, this spec produces two plans rather than one:

| Plan | Covers | Character |
|---|---|---|
| **A — Deck modernization** | §6 in full: corrections, removals, additions, the 2.x/3.x review, lab slides | The priority and the difficult half. Judgement-heavy, no compiler or test suite to catch mistakes, gated on trainer review |
| **B — Lab platform** | §7 in full: defect profiles, Gatling harness, branch workflow, preflight, hygiene | Conventional engineering, verifiable by tests, gated on Gatling reconciliation |

The two meet at four points only: the five-labs slide, the per-lab intro slides, lab 8 as deck 4.2's current-LTS JIT content, and the virtual-threads example referenced from the 3.2 / threading slides. Plan A does not wait on Plan B for any of them — each is a slide referencing work Plan B delivers, so the slide can be written before the code exists and verified afterwards.

Plan A is written and started first.

## 9. Risks

| Risk | Mitigation |
|---|---|
| Gatling reconciliation is worse than it looks | Time-boxed to ~1 day, JMeter fallback named in advance |
| Version claims wrong again | Every correction and addition carries a source URL; trainer review is a hard gate |
| Lab 5 runs long across 3 collectors × 3 JVMs | Allocated 2h; JDK 25 leg is optional |
| Deck edits applied by text replace hit false matches | Per-deck passes, slide numbers recorded in the change docs |
| Labs squeeze deck time below what the sections need | §6.4 reductions are agreed up front, not discovered mid-course |
