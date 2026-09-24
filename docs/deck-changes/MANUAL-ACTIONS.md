# Manual actions for the trainer

Edits that `gslides.sh` cannot perform. It can only replace a literal substring within existing
text — it cannot create slides, insert images, split or merge text runs, or apply formatting.

Everything here is outstanding unless marked done.

---

## Deck 5.1 — An introduction to Garbage Collection

Presentation: https://docs.google.com/presentation/d/1Y2xjPThug1WM2haytVv8M64tQDAIkI8TFl5CO4FU2lc/edit

**1. Slide 22 — G1 default release is wrong**

Currently reads `Enabled by default since` / `JDK 11` across two separate text runs.
Change to a single line: **"Enabled by default since JDK 9"**.

Cannot be scripted: the wrong value `JDK 11` lives in its own text run, and the bare string
`JDK 11` also appears *correctly* on slides 29 and 31 (Epsilon). A whole-deck replace would
corrupt those.
Source: https://openjdk.org/jeps/248

**2. Slide 38 — ZGC summary line**

The ZGC bullet reads `Mainstream since Java 11`. Change **that line only** to
**"Production-ready since JDK 15"**.

Cannot be scripted: the identical string appears immediately below under the **Epsilon** bullet,
where it is **correct** and must not be touched.
Source: https://openjdk.org/jeps/377

**3. Slide 39 — collectors overview badge**

Add a version badge to the collectors overview. Per the revised convention, list only the
releases where each collector is available, e.g. `[11]` for CMS, `[11 · 17 · 21 · 25]` for G1.

---

## Deck 4.2 — Working with the JIT compiler

Presentation: https://docs.google.com/presentation/d/1aLuZ5zUYFn-IeoHERu4HdraXmSG3-XP_C6vKI2_oG0o/edit

**4. Insert a new slide on AOT, immediately after slide 12 ("Optimizing long-running apps")**

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

**5. Optional — slide 28 "Further information"**

Two link captions on that slide now carry appended AOT notes (JEP 514 and JEP 515), applied before
the dedicated AOT slide above was agreed. They still work as "what this link covers" pointers, but
once the dedicated slide exists you may prefer to trim them back to plain link captions. Cosmetic,
not a correctness issue.

---

## Convention note

The version badge lists **only the releases where the claim holds** — `[11 · 17]`, `[21 · 25]`,
`[25]`. Plain text, no colour. The earlier full-set-with-muted-colours form was dropped because it
would have required a hand styling pass on every badged slide across the deck set.
