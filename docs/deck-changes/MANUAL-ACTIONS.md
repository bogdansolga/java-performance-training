# Manual actions for the trainer

> **SLIDE NUMBERS IN THIS FILE ARE GOOGLE SLIDES UI NUMBERS** — what the editor shows.
> The change documents use 0-based indices internally; those are one lower. Do not mix them.

Edits that `gslides.sh` cannot perform. It can only replace a literal substring within existing
text — it cannot create slides, insert images, split or merge text runs, or apply formatting.

Everything here is outstanding unless marked done.

> **STATUS 2026-09-26 — 7 of 14 items are DONE**, applied by script and verified against fresh deck dumps.
> They became scriptable once the harness gained slide scoping. Items struck through below need
> nothing further. The remaining 7 (2, 3, 4, 5, 12, 13, 14) are genuinely manual: a same-slide
> duplicate that page-level scoping cannot split, a new slide, a badge, image placement, screenshot
> deletion and a merge pass.
>
> **Two cosmetic residues to tidy while you are in the editor:** deleting text does not delete its
> bullet, so deck 7.1 slide 2 has an empty bullet where `jhat` was, and slide 39 has a blank line.
> No automated check can see these.


---

## Deck 5.1 — An introduction to Garbage Collection

Presentation: https://docs.google.com/presentation/d/1Y2xjPThug1WM2haytVv8M64tQDAIkI8TFl5CO4FU2lc/edit

**1. ~~DONE 2026-09-26~~ — Slide 23 — G1 default release is wrong**

Currently reads `Enabled by default since` / `JDK 11` across two separate text runs.
Change to a single line: **"Enabled by default since JDK 9"**.

Cannot be scripted: the wrong value `JDK 11` lives in its own text run, and the bare string
`JDK 11` also appears *correctly* on slides 29 and 31 (Epsilon). A whole-deck replace would
corrupt those.
Source: https://openjdk.org/jeps/248

**2. Slide 39 — ZGC summary line**

The ZGC bullet reads `Mainstream since Java 11`. Change **that line only** to **"Production-ready since JDK 15"**.

Cannot be scripted: the identical string appears immediately below under the **Epsilon** bullet, where it is **correct** and must not be touched.
Source: https://openjdk.org/jeps/377

**3. Slide 40 — collectors overview badge**

Add a version badge to the collectors overview. Per the revised convention, list only the
releases where each collector is available, e.g. `[11]` for CMS, `[11 · 17 · 21 · 25]` for G1.

---

## Deck 4.2 — Working with the JIT compiler

Presentation: https://docs.google.com/presentation/d/1aLuZ5zUYFn-IeoHERu4HdraXmSG3-XP_C6vKI2_oG0o/edit

**4. Insert a new slide on AOT, immediately after slide 13 ("Optimizing long-running apps")**

Slide 12 establishes that throughput is only meaningful after warm-up, then moves on. AOT is the
direct answer to that problem, so it belongs next to it rather than later in the deck.

Suggested content:

```
Ahead-of-Time compilation (AOT)                          [25]

The problem: the JIT needs warm-up. Early requests run interpreted or
only partly optimised — costly for short-lived and containerised workloads.

Project Leyden's AOT cache:
  JEP 483 (24)   AOT class loading & linking   -> faster startup
  JEP 515 (25)   AOT method profiling          -> faster warm-up
  JEP 514 (25)   AOT command-line ergonomics   -> one-step workflow

  -XX:AOTCacheOutput=app.aot     record
  -XX:AOTCache=app.aot           use

Careful - "AOT" meant something else before:
  jaotc and the Graal JIT were experimental in 9-16 and were REMOVED in 17
  (JEP 410). If you are on Java 11 you may have met that one. It is not this.
```

The closing block matters because Java 11 is a covered release: an attendee on 11 may know
`jaotc`, and "AOT" now means something unrelated.

Sources: https://openjdk.org/jeps/483, https://openjdk.org/jeps/514, https://openjdk.org/jeps/515,
https://openjdk.org/jeps/410

**5. Optional — slide 29 "Further information"**

Two link captions on that slide now carry appended AOT notes (JEP 514 and JEP 515), applied before
the dedicated AOT slide above was agreed. They still work as "what this link covers" pointers, but
once the dedicated slide exists you may prefer to trim them back to plain link captions. Cosmetic,
not a correctness issue.

---

## Convention note

The version badge lists **only the releases where the claim holds** — `[11 · 17]`, `[21 · 25]`,
`[25]`. Plain text, no colour. The earlier full-set-with-muted-colours form was dropped because it
would have required a hand styling pass on every badged slide across the deck set.

---

## Deck 7.1 — Java monitoring & profiling tools

Presentation: https://docs.google.com/presentation/d/1952R9NhvuYNuG9TujMfEPpy_w6cZ_yfMvl5zlZb9yoU/edit

The 6 scripted rows were applied on 2026-09-24 and verified. These are what the tool cannot do.

**6. ~~DONE 2026-09-26~~ — Slide 2 — delete the `jhat` entry**

Remove both runs: the tool name `jhat` and its description `- reads and helps analyse memory heap
dumps`. `jhat` was removed in JDK 9.
Not scriptable: the name and description are two separate text runs.
Source: https://openjdk.org/jeps/241

**7. ~~DONE 2026-09-26~~ — Slide 17 — replace the `jhat` mention**

Change "JDK tools - including visualvm & jhat" to "JDK tools - including visualvm; for heap dumps
use `jcmd <pid> GC.heap_dump`, then open in JMC or Eclipse MAT".
Not scriptable: the bare `jhat` run here is **byte-identical to slide 1's**, but the two need
different fixes — a whole-deck replace would give them the same text.
Source: https://openjdk.org/jeps/241

**8. ~~DONE 2026-09-26~~ — Slide 19 — move Java Flight Recorder from the Paid list to the Free list**

Place it alongside JMC, VisualVM and async-profiler. JFR has been free and open-source since
JDK 11 (JEP 328); it was a commercial feature only under Oracle JDK 8, which is likely why the
deck lists it as paid. Java 11 is a covered release, so it should read free throughout.
Not scriptable: the string `Java Flight Recorder` also appears **correctly** in slide 42's link
caption, which a whole-deck replace would corrupt.
Source: https://openjdk.org/jeps/328

**9. ~~DONE 2026-09-26~~ — Slide 19 — delete the stale parenthetical after `JProbe`**

Remove "(deprecated by the developing company?)". The JProbe line now states the status outright,
so the hedge is redundant.
Not scriptable: three text runs, and a genuine deletion.

**10. ~~DONE 2026-09-26~~ — Slide 38 — rewrite the native-profiler line**

Change "→ GlassFish startup in Oracle Developer Studio → native profiler" to
"→ native profiling of a Java process, using async-profiler or perf →". Oracle Developer Studio is
discontinued; async-profiler samples without waiting for a safepoint, which is exactly the bias
this deck describes on slide 31.
Not scriptable: three text runs.
Source: https://github.com/async-profiler/async-profiler

**11. ~~DONE 2026-09-26~~ — Slide 39 — rewrite the native-profile caption**

Change "The GlassFish startup profile, showed in Oracle Developer Studio profiling tool" to
"A native CPU profile, captured with async-profiler / perf", and delete the now-redundant
"Also works on Linux systems" line.
Not scriptable: three text runs, and `Oracle Developer Studio` occurs twice needing different
surrounding rewrites.

**12. ~~DONE 2026-09-29~~ (on the working copy `17SQg1F2…`) — Place the three approved diagrams** (2560x1440, in `docs/diagrams/`)

- `7-1-01-sampling-vs-instrumenting.png` → **slide 25**, replacing the sampling screenshot
- `7-1-02-safepoint-bias.png` → **slide 32**, adding to a prose-only explanation
- `7-1-03-complementarity.png` → **slide 30**, adding to a prose-only comparison

**13. ~~SUPERSEDED 2026-09-29~~ by the 33-slide working copy, which replaces the original — Delete the superseded screenshots** on slides **25, 28, 35, 39, 41**

Slide 41 has no text row above it — its screenshot is cut because it belongs to the same
walkthrough, not because its caption was wrong.

**14. ~~SUPERSEDED 2026-09-29~~ by the 33-slide working copy — Shortening pass — merge or cut 4 slides**

- **27** "Quick summary" (sampling) → merge into 25/26; the deck already has summaries at 12 and 41
- **34** "Instrumented profilers:" → merge into 33 "Conclusions"; both are short recaps of one example
- **37** "Quick summary" (blocking methods) → merge into 36
- **41** "The filtered native profiler" → cut; a third pass over a screenshot being deleted

44 slides today → ~40 after these. A further reduction pass is in progress separately.
