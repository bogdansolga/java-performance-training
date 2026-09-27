# Deck 3.1 — Execution profiling tools & concepts

**STATUS: DRAFT — not yet applied.** Produced for trainer review. `deck-apply.sh` invoked
only `--dry-run`; live deck never modified.

Source read: `gslides.sh personal text 1sagmLntUl2W-3fkbL5_f_K15FL30cAy4iR1S_B8bVeg` on
2026-09-27, saved to `/tmp/deck-03-1-profiling.txt` (72 lines, 3 slides, slide indices 0–2).

This is **the one deck in the set that grows** (spec §6.4's deliberate exception to
"review and reduce"). Today it covers only in-code timing (`Logger`, Spring `StopWatch`),
AOP aspects, P6Spy and Hibernate Statistics. There is no JFR, no JMC, no async-profiler, no
flame graphs — the largest content gap in the deck set, and the one the hands-on labs lean
on hardest. Almost everything needed is a **new slide**, which `gslides.sh` cannot create;
all of it is in `## Manual actions` below, not in the (empty) rows table.

## Rows

No scripted rows. The three existing slides need no text edits to accommodate the new
material:

- **Slide 0** (title) — unchanged.
- **Slide 1** ("Execution profiling overview") — the brief says to keep the existing
  in-code timing, AOP and P6Spy/Hibernate Statistics material as-is, and it needs no edit
  to make room for the new slides: nothing on it claims or implies it is the *complete*
  list of profiling tools, so inserting new slides after it manufactures no
  contradiction. **Not editing this slide is a deliberate decision, not an oversight.**
- **Slide 2** ("Q & A session") — unchanged in content; it moves to the end of the new,
  longer running order (see below), which is a reorder, not a text edit — recorded in
  Manual actions.

No anchors were needed and no `deck-check.sh`/`deck-apply.sh` verification applies to this
document beyond the (trivial, empty) rows table — both scripts should report nothing to
do for the "Rows" section. See `## Manual actions` for the actual proposed content.

## Manual actions — new slides `gslides.sh` cannot create

Five new slides, all genuinely new content with no existing anchor to attach to (no run in
the deck mentions JFR, JMC, async-profiler, or flame graphs). Each is a manual insert in
the Slides editor. **Insert all five as slides 2–6**, i.e. after slide 1 ("Execution
profiling overview") and before the current slide 2 ("Q & A session"), which becomes the
new slide 7.

### A. New slide — JFR (Java Flight Recorder)

**Insert as new slide 2**, immediately after "Execution profiling overview" — it is the
natural next step up from the in-code/AOP techniques on slide 1: an always-on, built-in
alternative that needs no code changes.

Suggested content:

```
JFR — the JDK Flight Recorder

Java Flight Recorder (JFR) — a low-overhead, always-on event recorder
built into the JDK

Free & open source in the JDK since 11 (JEP 328) — previously a
commercial-only Oracle JDK feature

Overhead is typically well under 1% — safe to leave running continuously,
even in production

Start on a running process:
  jcmd <pid> JFR.start

Or record from JVM startup:
  -XX:StartFlightRecording

Produces a .jfr recording file — opened and analysed in JMC
```

Source: https://openjdk.org/jeps/328

### B. New slide — JMC (Java Mission Control)

**Insert as new slide 3**, directly after the JFR slide — JMC is the tool that opens what
JFR records, so it follows immediately.

Suggested content:

```
JMC — Java Mission Control

JMC opens and analyses JFR recordings — the viewing half of JFR

A separate download since Java 11 — no longer bundled with the JDK

One recording, many views: CPU, memory, GC, threads, I/O, exceptions

Workflow: jcmd <pid> JFR.start  ->  .jfr file  ->  open in JMC
```

Source: https://openjdk.org/projects/jmc/

### C. New slide — async-profiler

**Insert as new slide 4**, after JMC — introduces the sampling-profiler alternative before
the flame-graph slide that depends on it.

Suggested content:

```
async-profiler

A sampling profiler that does not wait for a JVM safepoint to take a
sample

Removes the safepoint-bias problem — deck 7.1 names this flaw in
safepoint-limited profilers; this is the remedy it has no answer for
otherwise

Samples CPU time, allocations, and lock contention

Produces flame graphs directly

Open source, actively maintained
```

Source: https://github.com/async-profiler/async-profiler

### D. New slide — Flame graphs: how to read one

**Insert as new slide 5**, right after async-profiler, since flame graphs are its output —
participants need to read one before they can use it in the labs.

Suggested content:

```
Flame graphs — how to read one

Each box is a stack frame (one method call)

Width = how often that frame was on the stack while sampling
  -> wider is "hotter", not "slower" and not "called more recently"

The x-axis is not time — frames are ordered alphabetically, not
chronologically

Height = stack depth, growing upward into the methods each frame called

Start at the wide plateaus near the top of the graph — self-contained,
hot methods worth investigating first
```

Source: — (general profiling-tool convention; no version-specific claim).

### E. New slide — Choosing a tool: which one, when

**Insert as new slide 6**, last of the new material, immediately before "Q & A session"
(now slide 7) — a closing decision guide over everything the deck has covered, old and
new.

Suggested content:

```
Choosing a tool — which one, when

Logger / StopWatch — a quick, one-off timing check on a method you
already suspect

AOP aspects — timing across many methods without touching their code

P6Spy / Hibernate Statistics — is the database the problem?
  (this is how lab 2's N+1 queries get measured)

JFR — the default first step: always-on, low overhead, "what is this
app doing?"

JMC — open a JFR recording and drill into it

async-profiler + flame graphs — CPU/allocation/lock hotspots, with no
safepoint bias

Three tools, three different questions — not a ranking
```

Source: — (synthesis of the deck's own tools; no external claim).

## Proposed final slide count and running order

**3 slides -> 8 slides** (+5 new).

Plain list, not a table — this document's Rows section (empty, above) is the only place a
pipe-table belongs in this file; both scripts scan every line starting with `|`, and a
second pipe-table here would be misread as extra (bogus) anchor rows. (This bit a first
draft of this exact section: `deck-check.sh` reported nine spurious `MISSING`/`UNSAFE`
problems, one per row of a table that used to be here, before it was rewritten as this
list.)

```
new index 0  Title                                                       unchanged
new index 1  Execution profiling overview (in-code timing, AOP, P6Spy,
             Hibernate Statistics)                                       unchanged
new index 2  JFR                                                         new
new index 3  JMC                                                         new
new index 4  async-profiler                                              new
new index 5  Flame graphs -- how to read one                             new
new index 6  Choosing a tool -- which one, when                          new
new index 7  Q & A session                                               unchanged, moved from index 2
```

## Notes / exceptions

- **No `outside-scope-ok` rows** — there are no scripted rows at all in this document; the
  entire proposal is structural (new slides) and belongs in Manual actions per convention.
- **No empty-bullet residue risk** — nothing is deleted from slides 0/1/2, only inserted
  after them, so the "deleted text leaves an empty bullet" caveat does not apply here.
- **Version-badge check** — JFR-since-11 and JMC-separate-since-11 are stated in prose, not
  as `[11 · 17 · 21 · 25]`-style badges, because the claim holds uniformly across all four
  covered releases (11, 17, 21, 25); per convention, a badge marks a *difference* between
  releases, and there is none here to flag.
- **Cross-deck consistency** — the async-profiler/safepoint-bias framing here matches the
  wording already applied to deck 7.1 (`docs/deck-changes/07-1-profiling-tools.md`, row on
  slide 37/38, and `MANUAL-ACTIONS.md` items 8 and 10), so the two decks tell the same story
  about why async-profiler exists.
- **This document alone cannot be verified by `deck-check.sh`/`deck-apply.sh` for its
  substantive content** — those scripts only validate the (empty) Rows table; the new-slide
  content in Manual actions is inherently manual, per the "structural work" exception in
  `deck-task-conventions.md`.
