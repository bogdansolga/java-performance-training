# Deck 5.2 — Choosing a GC algorithm

**STATUS: DRAFT — not yet applied.** Produced for trainer review. `deck-apply.sh` was invoked
only with `--dry-run`; the live deck was never modified.

Source read: `gslides.sh personal text 1KfSLKXIGKmMRiqCeNYvR_-ZrSR8spcY5smiqvQrqb_k` on
2026-09-27, saved to `/tmp/deck-5-2.txt` (303 lines, 13 slides, slide indices 0–12).

Every non-blank Anchor below is verified with:

```
grep -o -F -- "<anchor>" /tmp/deck-5-2.txt | wc -l          # deck-wide
<slide-slice> | grep -o -F -- "<anchor>" | wc -l              # in declared scope
```

Verbatim output for every row is in this document's "Anchor verification" section below.

## Rows

| Slide | Anchor | Current context | Proposed text | Category | Source |
|---|---|---|---|---|---|
| 7 | ZGC - latest GC added in the JVM | "A few maturity considerations" slide — closing bullet on ZGC, immediately followed by a separate `(since JDK 15)` run (next row); wrongly frames ZGC as the newest addition and implies it is "the" GC to reach for | ZGC — generational mode added in JDK 21 (JEP 439), became the default mode in JDK 23 (JEP 474); non-generational mode removed in JDK 24 (JEP 490). G1 remains the JVM's overall default [21 · 25] | correction | https://openjdk.org/jeps/439, https://openjdk.org/jeps/474, https://openjdk.org/jeps/490 |
| 7 | (since JDK 15) | Same bullet — separate trailing run immediately after the run above; now redundant/superseded once the row above states the full generational-mode timeline | <DELETE> | removal | n/a |
| 8 | eliminate pause times, | "Throughput improving parameters" slide — `ZGC:` bullet, `designed to ` + this run + ` for most apps`; overstates ZGC as eliminating pauses outright | achieve sub-millisecond pause times, and is generational by default since JDK 23 (JEP 474) [25], | correction | https://openjdk.org/jeps/474 |
| 1 | best used when the app uses less than 100 MB | "Overview" slide — Serial collector bullet; guidance is correct but incomplete for containerised workloads | best used when the app uses less than 100 MB, and remains the JVM's default on a single-CPU cgroup — a live case for containerised workloads | addition | https://docs.oracle.com/en/java/javase/25/gctuning/ergonomics.html |

No row in this document needed `outside-scope-ok`: every anchor above is unique both within its
declared scope and deck-wide (in-scope count equals deck-wide count for all four rows).

## Anchor verification

Computed against `/tmp/deck-5-2.txt` (`gslides.sh personal text`, 2026-09-27) using the exact
`grep -o -F | wc -l` method `deck-check.sh`/`deck-apply.sh` use. (Plain list, not a table — this
document's row table above is the only pipe-table in the file; both scripts scan every line
starting with `|`, so a second table here would be misread as extra rows.)

```
ZGC - latest GC added in the JVM        slide 7   in-scope=1   deck-wide=1
(since JDK 15)                          slide 7   in-scope=1   deck-wide=1
eliminate pause times,                  slide 8   in-scope=1   deck-wide=1
best used when the app uses less than 100 MB   slide 1   in-scope=1   deck-wide=1
```

## Notes / exceptions

- **No `outside-scope-ok` rows.** All four anchors are unique deck-wide, so no row needed the
  escape hatch — each in-scope count already equals its deck-wide count.
- **Row 4 is append-style.** Its Proposed text begins with the Anchor verbatim and appends the
  cgroup clause; `deck-apply.sh` classifies this as an append (the Anchor legitimately survives
  inside the replacement), not a plain replace.
- **Row 1's Proposed text deliberately avoids repeating `G1` or `ZGC` version numbers already
  covered elsewhere** to keep the badge on the single claim that changed (generational-mode
  timeline), per the brief's rule that a badge should attach only to a major, version-specific
  difference.
- **Deck 5.2 has no G1-region or humongous-object content and none was added here.** Per the
  brief, that content lands in deck 5.3 (see `05-3-gc-tuning.md`), where the deck already has a
  generation-sizing section to attach it to.
- **No row needed a new slide in this deck.** All four ground-truth items in the brief map onto
  existing bullets/runs.
