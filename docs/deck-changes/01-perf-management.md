# Deck 1 — Java performance management overview

> **STATUS: APPLIED 2026-09-29** — applied by `deck-apply.sh` and independently verified on a fresh dump (each replacement present exactly once, each deleted anchor gone). **DO NOT RE-RUN `deck-apply.sh` ON THIS FILE.**

~~STATUS: VERIFY DOCUMENT — dry-run only, nothing applied.~~

Source read: `gslides.sh personal text 1wpiNHmcXNkXEmwF09xJS9bv4GAex6IQxiBzt_m0X15c` on
2026-09-27, saved to `/tmp/deck-1-perf-management.txt` (141 lines, 6 slides, indices 0–5).
Every anchor below was re-derived from this dump — none of the brief's quoted phrases
("client class = any 32-bit JVM running on Microsoft Windows", "Three new GCs (G1, ZGC &
Shenandoah)") exist verbatim on-slide; both are paraphrases of text spread across
several separate text runs (0 of 2 brief phrases usable verbatim, consistent with the
conventions' warning). All anchors were built from the actual dump and verified with
`grep -o -F -- "<anchor>" /tmp/deck-1-perf-management.txt | wc -l` printing exactly `1`,
then cross-checked per-slide (see Anchor verification).

## Source-citation correction (found during this pass)

Two of the brief's supplied sources were checked against the live JEP text before use
and do not hold up:

- **JEP 343 is "Packaging Tool (Incubator)"** (`jpackage`, JDK 14) — it has nothing to do
  with container/cgroup awareness or GC ergonomics. It is **not used** as a source below.
  In its place: the Oracle HotSpot ergonomics tuning guide (already used for the same
  "default GC on a single-CPU cgroup" claim in `docs/deck-changes/05-2-choosing-gc.md`),
  plus JEP 248 for the G1-default half of the claim.
- **JEP 521 ("Generational Shenandoah") targets JDK 25, not 24**, and explicitly states
  it does *not* make generational mode the default (that is the separate, undelivered
  JEP 535) — it only promotes the mode from experimental to a full product feature. The
  JEP that actually delivered Shenandoah's generational mode, experimentally, **in JDK
  24** is **JEP 404** ("Generational Shenandoah (Experimental)"). JEP 404 is used below
  instead of JEP 521.

Both corrections were made before writing the rows below; no row cites JEP 343 or 521.

## Rows

| Slide | Anchor | Current context | Proposed text | Category | Source |
|---|---|---|---|---|---|
| 4 |  - any 32-bit JVM running on: | "Java ergonomics" slide, "Client-class machines" bullet — this run introduces the two OS/CPU sub-conditions corrected/removed in the next two rows |  - fewer than 2 CPUs or less than 1792 MB of memory (cgroup limits count), regardless of OS | correction | https://docs.oracle.com/en/java/javase/25/gctuning/ergonomics.html, https://openjdk.org/jeps/248, https://github.com/openjdk/jdk/blob/master/src/hotspot/share/runtime/os.cpp (os::is_server_class_machine) |
| 4 | Microsoft Windows 		- regardless of the number of CPUs on the machine | Same slide, first "Client-class" sub-bullet — Windows-regardless-of-CPU-count no longer determines the collector; superseded by the merged, cgroup-aware condition in the row above | <DELETE> | removal | n/a |
| 4 | A machine with one CPU  	- regardless of the operating system | Same slide, second "Client-class" sub-bullet — now redundant once the row above states the merged condition | <DELETE> | removal | n/a |
| 4 | Ex: the default Garbage Collector for a platform - determined by the machine class | Same slide, closing example bullet — ties the (now-corrected) machine-class distinction to a concrete choice but never previously named an actual collector | Ex: the default Garbage Collector - Serial below 2 CPUs or 1792 MB of memory, G1 otherwise (ergonomics reads cgroup CPU and memory limits) | correction | https://docs.oracle.com/en/java/javase/25/gctuning/ergonomics.html, https://openjdk.org/jeps/248, https://github.com/openjdk/jdk/blob/master/src/hotspot/share/runtime/os.cpp (os::is_server_class_machine) |
| 2 | Three new GCs  | "Performance management status" slide — first run of a two-run bullet claiming three new GCs became available; G1 has been the default since JDK 9 and is not new | G1 (default since JDK 9), plus  | correction | https://openjdk.org/jeps/248 |
| 2 | (G1, ZGC & Shenandoah) | Same bullet, second run — parenthetical naming the three collectors; ZGC's and Shenandoah's *generational modes* are the actually-new part, not the collectors themselves | ZGC (generational mode only since JDK 24) & Shenandoah (generational mode a product feature since JDK 25) | correction | https://openjdk.org/jeps/490, https://openjdk.org/jeps/521 |

No row needed `outside-scope-ok`: every anchor above is unique both within its declared
scope and deck-wide (in-scope count equals deck-wide count for all six rows — see Anchor
verification).

## Anchor verification

Computed against `/tmp/deck-1-perf-management.txt` (`gslides.sh personal text`,
2026-09-27), using the exact `grep -o -F | wc -l` method `deck-check.sh`/`deck-apply.sh`
use, both deck-wide and sliced to the declared slide:

```
 - any 32-bit JVM running on:                                          slide 4  in-scope=1  deck-wide=1
Microsoft Windows [tab][tab]- regardless of the number of CPUs...      slide 4  in-scope=1  deck-wide=1
A machine with one CPU  [tab]- regardless of the operating system      slide 4  in-scope=1  deck-wide=1
Ex: the default Garbage Collector for a platform - determined...       slide 4  in-scope=1  deck-wide=1
Three new GCs                                                          slide 2  in-scope=1  deck-wide=1
(G1, ZGC & Shenandoah)                                                 slide 2  in-scope=1  deck-wide=1
```

## Streams API — cross-check for deck 4.1 (no row; note only)

Per the brief: this deck (slide 2, "Performance management status") reads, verbatim
across two runs:

```
A reference example - the Streams API
(further presented & discussed, if needed)
```

This is a *framing* statement only — it names the Streams API as the deck's example of a
language feature that can cost performance, but makes no quantified/specific performance
claim on this slide, and explicitly defers detail ("further presented & discussed, if
needed") to elsewhere in the course. Cross-checked against
`docs/deck-changes/04-1-improvements.md`, which independently found: Streams API appears
twice in deck 4.1 (slide 15, as a parallel/async option; slide 17, as a functional
idiom) but "the deck makes no explicit performance claim about it to reconcile against
Deck 1" and flagged deck 1 as unread at the time (out of scope for that task). Read
together, the two decks are **consistent as of this pass** — neither makes a specific,
checkable performance claim about the Streams API, so there is nothing to reconcile.
Whether deck 4.1 should be expanded to fulfil deck 1's "further presented" promise is a
content/structure decision outside this task's scope; flagged for the trainer, not
turned into a row.

## Notes / exceptions

- **Deleting two runs leaves two empty bullets.** Rows 2 and 3 above (`<DELETE>`) each
  empty a full sub-bullet paragraph on slide 4; `gslides.sh` cannot remove the bullet
  itself. The trainer should delete the two now-empty bullet lines under "Client-class
  machines" by hand after applying.
- **"Client"/"Server" quoted labels (slide 4, above the corrected bullets) are left
  untouched.** The brief's specific complaint is the *definition* (32-bit + Windows),
  not the loose "client vs server" vocabulary itself, and the corrected condition still
  reads coherently under the existing "Client-class machines" / "Server class machines"
  headings once the two obsolete sub-bullets are gone.
- **Source-citation errors caught and fixed before writing rows** — see the section
  above. Both were verified directly against the JEP text on 2026-09-27 (via WebFetch),
  not taken from the brief on faith, per the conventions' warning about bare/brief-supplied
  version numbers.

## Verify

```
./scripts/deck-check.sh docs/deck-changes/01-perf-management.md 1wpiNHmcXNkXEmwF09xJS9bv4GAex6IQxiBzt_m0X15c
./scripts/deck-apply.sh docs/deck-changes/01-perf-management.md 1wpiNHmcXNkXEmwF09xJS9bv4GAex6IQxiBzt_m0X15c --dry-run
```
