# Deck review checklist

What changed, on which slide, and what to look at. **All slide numbers below are Google Slides UI numbers** — what you see at the bottom of the
editor. No conversion needed.

(The change documents under `docs/deck-changes/*.md` use 0-based indices in their `Slide` column
because the tooling requires it. Those are one lower. This file and `MANUAL-ACTIONS.md` are
human-facing and use UI numbers throughout.)

Status as of 2026-09-27: **3 decks applied, 2 awaiting your approval, 9 decks not yet started.**

---

## APPLIED — spot-check these

### Deck 5.1 — An introduction to Garbage Collection
https://docs.google.com/presentation/d/1Y2xjPThug1WM2haytVv8M64tQDAIkI8TFl5CO4FU2lc/edit

| Slide | What changed | What to check |
|---|---|---|
| 19, 21 | CMS "no longer available since Java 17" → "since JDK 14" | Both slides say JDK 14. CMS *does* still exist on Java 11 |
| 23 | `JDK 11` → `JDK 9` (G1 default) | Reads "Enabled by default since JDK 9". **This was live-wrong before** |
| 25 | ZGC "mainstream since JDK 15" → production-ready 15, generational default 23 | Does not claim ZGC is the JVM default |
| 28 | Shenandoah "introduced in JDK 12" → + production-ready 15 | |
| 29 | + generational mode in JDK 24 | |
| 38 | CMS summary → "(JDK 11 only; removed in JDK 14)" | CMS is scoped, **not deleted** — it exists on 11 |
| 39 | ZGC summary → generational 21, default 23, non-gen removed 24 | |

**Still manual here:** slide 39's ZGC bullet reads "Mainstream since Java 11" → should be
"Production-ready since JDK 15". The identical line sits under **Epsilon** just below, where it is
**correct** — change only the ZGC one. And slide 40 needs the availability badge.

### Deck 4.2 — Working with the JIT compiler
https://docs.google.com/presentation/d/1aLuZ5zUYFn-IeoHERu4HdraXmSG3-XP_C6vKI2_oG0o/edit

| Slide | What changed | What to check |
|---|---|---|
| 9 | Compiler selection → "Tiered compilation has been the JVM default since JDK 8" | |
| 14 | Heap<3GB advice badged `[11 · 17]`; `-client`/`-server`/`-d64` marked dead since JDK 9/10 | The **port history is retained and scoped**, the **flags are marked dead** — two different cut-offs, deliberately not merged |
| 16 | "5-20% faster in a 32-bit JVM" badged `[11 · 17]` | Retained as history, not deleted |
| 17 | Tiered is the default; `-client`/`-server` no longer select a compiler | |
| 29 | AOT cache (JEP 514) and AOT method profiling (JEP 515) added to link captions | Reads sensibly as a "further reading" pointer |

**Still manual here:** insert the **AOT slide after slide 13**, drafted in `MANUAL-ACTIONS.md`
item 4. It includes the `jaotc` disambiguation, which matters only because Java 11 is covered.

### Deck 7.1 — Java monitoring & profiling tools
https://docs.google.com/presentation/d/1952R9NhvuYNuG9TujMfEPpy_w6cZ_yfMvl5zlZb9yoU/edit

| Slide | What changed | What to check |
|---|---|---|
| 2 | `jhat` entry deleted | **Leaves an empty bullet** — tidy it |
| 17 | `jhat` → `jcmd <pid> GC.heap_dump`, then JMC or Eclipse MAT | |
| 19 | JProbe marked discontinued; stale "(deprecated by…?)" deleted; JFR removed from Paid; Free list now "Async profiler, JFR" | **Does the Free list read well?** JFR appears as an abbreviation; slide 43 still spells it out |
| 25 | GlassFish sampling caption → "How sampling attributes time to methods — see diagram" | Caption points at a diagram **not yet placed** |
| 28 | "Same workload, instrumented — see diagram" | Same |
| 35 | NetBeans caption → "Blocked threads consume no CPU — park(), parkNanos(), read()" | |
| 38 | Oracle Developer Studio → async-profiler / perf | |
| 39 | Caption → "A native CPU profile, captured with async-profiler / perf"; "Also works on Linux systems" deleted | **Leaves a blank line** — tidy it |
| 43 | JFR link caption → "+ free since JDK 11" | |

**Still manual here:** place the three diagrams (**25**, **32**, **30**), delete the superseded
screenshots (**25, 28, 35, 39, 41**), and the merge/cut pass (**27, 34, 37, 41**).
A fuller reduction proposal — **44 → 34 slides** — is in `07-1-reduction-proposal.md`.

---

## AWAITING YOUR APPROVAL — nothing applied

### Deck 5.2 — Choosing a GC algorithm
https://docs.google.com/presentation/d/1KfSLKXIGKmMRiqCeNYvR_-ZrSR8spcY5smiqvQrqb_k/edit

| Slide | Proposed | Judgement call for you |
|---|---|---|
| 2 | Serial → + "remains the JVM's default on a single-CPU cgroup" | Worth adding for containerised workloads? |
| 8 | ZGC "latest GC added" → generational 21 / default mode 23 / non-gen removed 24, **G1 remains the overall default**; trailing "(since JDK 15)" deleted | The deletion assumes the new text supersedes it |
| 9 | "eliminate pause times" → "sub-millisecond pause times, generational by default since JDK 23" | Is "sub-millisecond" the claim you want to make? |

### Deck 5.3 — Basic GC tuning
https://docs.google.com/presentation/d/128eswOqor8syZCg6LLBZAsAjxXcel-oW-EUuBLutRto/edit

| Slide | Proposed | Judgement call for you |
|---|---|---|
| 23 | Metaspace "rarely needs to be sized" → + elastic since JDK 16, **and on JDK 11 it does not release memory** | The 11-vs-17 split is the real teaching point |
| 26 | `-XX:+UseParallelOldGC` → + deprecated in JDK 14 | |
| 26 | `-XX:+UseParNewGC` → deleted | Will leave an empty bullet, as on 7.1 slide 2 |

**Two new slides proposed** (cannot be scripted):

1. **After slide 18** — compact object headers, `-XX:+UseCompactObjectHeaders`, product in JDK 25, up to ~22% heap reduction.
2. **After slide 21** — **G1 humongous objects.** Region = heap/2048 clamped to a 1 MB floor; an object over half a region goes straight to old gen; symptom is a sawtooth heap and a container killed while the heap looks healthy; fix is `-XX:G1HeapRegionSize`. **This is lab 6 and the Cloud Run incident from your pre-call.**

---

## NOT STARTED — 9 decks

Training overview, 1 (perf management), 2 + 2.1–2.4 (workflow group), 3 + 3.1 (toolbox &
profiling), 3.2–3.4 (CPU/disk/network), 4.1 (improvements), 6.1 (heap objects), 6.2 (memory
leaks), 7.2 (JMC & JFR — read during design and found accurate).

---

## One caveat on all of the above

Each document covers the ground truth I supplied for that deck. **No deck has had an independent
sweep for dated content I failed to list** — and I have made five version-number errors this
session, each caught by a source URL or an agent. Treat "what was changed" as reliable and
"nothing else needed changing" as unverified.
