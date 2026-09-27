# Deck 5.3 — Basic GC tuning

**STATUS: DRAFT — not yet applied.** Produced for trainer review. `deck-apply.sh` was invoked
only with `--dry-run`; the live deck was never modified.

Source read: `gslides.sh personal text 128eswOqor8syZCg6LLBZAsAjxXcel-oW-EUuBLutRto` on
2026-09-27, saved to `/tmp/deck-5-3.txt` (860 lines, 37 slides, slide indices 0–36).

Every non-blank Anchor below is verified with:

```
grep -o -F -- "<anchor>" /tmp/deck-5-3.txt | wc -l          # deck-wide
<slide-slice> | grep -o -F -- "<anchor>" | wc -l              # in declared scope
```

Verbatim output for every row is in this document's "Anchor verification" section below.

## Rows

| Slide | Anchor | Current context | Proposed text | Category | Source |
|---|---|---|---|---|---|
| 22 | rarely needs to be sized | "Sizing Permgen and Metaspace (continued)" slide — `The metaspace ` + this run + ` - uses (by default) as much space as it needs`; the advice is directionally right but for the wrong reason, and hides a real 11-vs-17+ difference | rarely needs to be sized — elastic since JDK 16 (JEP 387), which returns unused Metaspace memory to the OS [17 · 21 · 25]; on JDK 11 it does not release memory back [11] | correction | https://openjdk.org/jeps/387 |
| 25 | -XX:+UseParallelOldGC | "Controlling parallelism" slide — "Collection of the old generation when using" bullet, own run on the line below it; still listed as if current | -XX:+UseParallelOldGC — deprecated in JDK 14 (JEP 366) [17 · 21 · 25]; the young-generation flag above now also collects the old generation | correction | https://openjdk.org/jeps/366 |
| 25 | -XX:+UseParNewGC | Same slide — "Collection of the young generation when using:" list, between `-XX:+UseParallelGC` and `-XX:+UseG1GC`; removed along with CMS | <DELETE> | removal | https://openjdk.org/jeps/363 |

No row in this document needed `outside-scope-ok`: every anchor above is unique both within its
declared scope and deck-wide.

## Anchor verification

Computed against `/tmp/deck-5-3.txt` (`gslides.sh personal text`, 2026-09-27) using the exact
`grep -o -F | wc -l` method `deck-check.sh`/`deck-apply.sh` use. (Plain list, not a table — this
document's row table above is the only pipe-table in the file; both scripts scan every line
starting with `|`, so a second table here would be misread as extra rows.)

```
rarely needs to be sized      slide 22   in-scope=1   deck-wide=1
-XX:+UseParallelOldGC          slide 25   in-scope=1   deck-wide=1
-XX:+UseParNewGC               slide 25   in-scope=1   deck-wide=1
```

## Manual actions — new slides `gslides.sh` cannot create

Neither item below has an existing anchor to attach to (no run in the deck mentions object
headers, G1 regions, or humongous objects), so each is proposed as a genuinely new slide rather
than a table row. `gslides.sh` can only replace text within an existing run; inserting a slide is
a manual step for the trainer in the Slides editor.

### A. New slide — Compact object headers `[25]`

**Insert after slide 17** ("Heap sizing - quick summary"), **before slide 18** ("2. Sizing the
generations") — it belongs with the heap-sizing material since it directly reduces how much heap
an app needs.

Suggested content:

```
Compact object headers                                    [25]

-XX:+UseCompactObjectHeaders — product feature in JDK 25 (JEP 519)

Shrinks every object's header from 96-128 bits down to 64 bits

Can reduce overall heap usage by up to ~22%, with no code changes
```

Source: https://openjdk.org/jeps/519

### B. New slide — G1 humongous objects (feeds lab 6)

**Insert after slide 20** ("Sizing the generations - quick summary"), **before slide 21** ("3.
Sizing the Permgen and Metaspace areas") — the deck's closest existing home for G1-specific
generation/region sizing; no slide currently discusses G1 regions at all. This is the most
important addition in this pass: it reproduces a real production incident a participant reported
(see `docs/k8s-prep-work.md`, Case A) and feeds lab 6 directly.

Suggested content:

```
G1 regions and humongous objects

G1 divides the heap into equal-sized regions.
Default region size = heap / 2048, clamped to 1-32 MB, rounded to a power of two.
  -> a 2 GB heap (or anything <= 2 GB) gets 1 MB regions, because of the 1 MB floor.

An object larger than half a region is a HUMONGOUS allocation:
  allocated directly into the old generation, across contiguous regions,
  bypassing the normal young-generation path.

Real incident: 2 GB Cloud Run container, 5-6 MB responses, 1 MB regions
  -> sawtooth heap graph, rising full-GC frequency, container OOM-killed
     near its memory limit while the heap still looks healthy in
     JVM-level tools -- "hard to reproduce locally"

Fix: raise -XX:G1HeapRegionSize (8m or 16m), or shrink the responses
Observe: -Xlog:gc+heap=debug reports humongous regions
```

Source: https://docs.oracle.com/en/java/javase/21/gctuning/garbage-first-g1-garbage-collector1.html

## Notes / exceptions

- **No `outside-scope-ok` rows.** All three anchors are unique deck-wide, so no row needed the
  escape hatch.
- **Rows 2 (`UseParallelOldGC`) and 3 (`UseParNewGC`) are both scoped to slide 25** but do not
  collide with each other: their anchors are disjoint substrings and neither row's Proposed text
  contains the other row's Anchor.
- **Row 2 is append-style** (Proposed text begins with the Anchor verbatim); row 3 is
  delete-style. Deleting row 3's anchor leaves an empty bullet on slide 25 (cosmetic residue, not
  caught by any automated check — same pattern documented in `MANUAL-ACTIONS.md` for deck 7.1).
- **Compact object headers and the G1 humongous-objects material are both genuinely new content**
  with no existing anchor, so both are proposed as new slides in "Manual actions" above rather
  than as scripted rows. This mirrors how `MANUAL-ACTIONS.md` item 4 (deck 4.2's AOT slide) was
  handled: `gslides.sh` cannot insert slides regardless of scoping.
- **This document does not modify `docs/deck-changes/MANUAL-ACTIONS.md`.** The two new-slide
  proposals live only in this file's "Manual actions" section, per the constraint against editing
  existing files under `docs/deck-changes/`.
