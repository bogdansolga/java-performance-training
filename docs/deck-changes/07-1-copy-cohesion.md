# Deck 7.1 (trainer's working copy) — cohesion fixes

**Target: trainer's working copy only** — presentation
`17SQg1F2mSSzV8D4MxupaxumcP8WeDa15SOvqIVYb7P8`. The original deck
`1952R9NhvuYNuG9TujMfEPpy_w6cZ_yfMvl5zlZb9yoU` is never touched by this document or its
manual actions.

This is a cohesion pass, not a factual-correction pass: fixing duplication between newly
placed diagrams and the bullet text that was written before the diagrams existed, a
"see diagram" pointer to a diagram now on the slide, one slide made redundant by a
diagram that now lives on a neighboring slide, and two pairs of identically-titled
slides.

Source read: `gslides.sh personal text 17SQg1F2mSSzV8D4MxupaxumcP8WeDa15SOvqIVYb7P8` on
2026-09-27, saved to `/tmp/copy.txt` (798 lines, 34 slides, slide indices 0–33).

Every non-blank Anchor below was verified with:

```
grep -o -F -- "<anchor>" /tmp/copy.txt | wc -l   # deck-wide
awk 'NR==<slide-start>,NR==<slide-end>' /tmp/copy.txt | grep -o -F -- "<anchor>" | wc -l   # in-scope
```

Both counts are recorded per row in Notes below the table.

## Rows

| Slide | Anchor | Current context | Proposed text | Category | Source |
|---|---|---|---|---|---|
| 22 |  - the top method at 13% (vs 4% under sampling) | Instrumented-profilers bullet: `getPackageSourcesInternal()` + this run. Recites the exact 13%/4% figures now shown by the complementarity diagram on slide 20 |  - the top method under instrumenting; sampling ranked it far lower | correction | n/a |
| 23 | Although IM.get() uses 12% of the total time, it is called 4.7 million times | "For this analysis:" bullet — recites the exact 12%/4.7M figures also readable off the slide-20 diagram; the argument that follows ("bigger impact from reducing calls, not speeding up the impl") is what the slide should keep | Although IM.get() takes a meaningful share of the total time, it is called far more often than any other method | correction | n/a |
| 23 | Sampled vs instrumented profiling  | Slide 23 title run (note trailing space before the separate `(contd)` run below) — identical to slide 22's title, only distinguished by "(contd)", a real in-deck navigation problem in a 34-slide deck | Instrumented profiling: a call-count example | correction | n/a |
| 23 | (contd) | Slide 23 title, second run, immediately follows the run replaced above | <DELETE> | removal | n/a |

4 rows: 3 `correction`, 1 `removal`. No row required `outside-scope-ok` — all four anchors
are unique both within their declared slide scope and deck-wide (see Notes).

## Notes — anchor verification

- Row 1 anchor ` - the top method at 13% (vs 4% under sampling)` (slide 22, lines 547–580
  slice): in-scope 1, deck-wide 1.
- Row 2 anchor `Although IM.get() uses 12% of the total time, it is called 4.7 million
  times` (slide 23, lines 581–594 slice): in-scope 1, deck-wide 1.
- Row 3 anchor `Sampled vs instrumented profiling ` (with trailing space; slide 22's title
  run has no trailing space, so it does not collide): in-scope 1, deck-wide 1.
- Row 4 anchor `(contd)` (slide 23 slice): in-scope 1, deck-wide 1 — deck-wide because
  slide 28's own `(contd)` run was already removed by the Manual actions below (done by
  script, before this table's rows were checked/applied), so it does not inflate the
  deck-wide count here. Had slide 28 still had it, this row would have needed
  `outside-scope-ok`; it did not, by construction of the execution order.
- Proposed-text collision check across all four rows: none of the four Proposed-text
  strings contains another row's Anchor as a substring, so live sequential application
  cannot let one row's edit corrupt another row's target text.

## Manual actions (done by script — not left for the trainer)

`gslides.sh` cannot create/delete slides or images and cannot resize/reposition existing
image elements from the `replace`/`replace-on-slide` text-anchor path; three of this
pass's four cohesion fixes are pure text edits but are **not** expressible as safe
anchor-scoped rows, for a reason specific to this document (see "why not rows" under each
item). All three were applied directly via `gslides.sh` (`set-text` / `batch` /
`delete`), verified by re-reading the deck, and are recorded here rather than handed to
the trainer as follow-up work, per the task brief for this pass.

**Order used (matters):** the three `set-text`/`batch` edits below are all addressed by
object ID, so they are order-independent and safe at any point. The slide-17 deletion is
addressed by object ID too (`gslides.sh delete` takes an objectId, never an index), but
deleting a slide **renumbers every slide after it** for any *index-based* operation done
afterward — including this document's own Slide-scoped rows (22, 23). So the actual
order executed was: (1) slide 20 rewrite, (2) slide 18 fold-in text box, (3) slide 28
title fix — all three object-ID based, done first — **then** (4) this document's 4-row
table verified and applied via `deck-check.sh` / `deck-apply.sh` while slide indices
22/23 were still valid — **then**, last, (5) slide 17 deleted, since nothing downstream
of it needed an index anymore.

1. **Slide 20 ("A sampling profile example") — body rewrite.**
   Why not rows: the old body had 5 paragraphs re-reciting the diagram's numbers
   (`19%`, `defineClass1()`, `getPackageSourcesInternal()`); the brief's replacement is 3
   new paragraphs. Every fragment-level run that would need a `<DELETE>` row to remove
   (the bare `- `, the bare ` method` run reused on two old bullets, the bare `the `
   run) is a short/generic string that **also occurs inside the mandated replacement
   text** (e.g. the new text contains "...whichever **method** was running...", and
   "...time goes **-** not how often a **method** runs..."). Because `deck-apply.sh`'s
   delete rows do a page-scoped `replaceAllText`, a `<DELETE>` row for ` method` or `- `
   scoped to slide 20 would, once any row inserting the new wording had run, also strip
   that same fragment back out of the brand-new sentences — self-corrupting on this one
   slide regardless of row order. No re-wording of the mandated text removes this,
   since the words "method" and the dash are required by the brief's exact wording. This
   is a same-slide substring collision the anchor/row mechanism cannot express, not a
   correctness question — see `deck-task-conventions.md`'s "shapes the harness cannot
   do" for the general case; this is the same failure mode one level down (word, not
   whole line).
   Command used (`g41958e9e43_0_146` = slide 20's body shape, left column, does not
   touch the diagram image which is a separate element):
   ```
   gslides.sh personal set-text 17SQg1F2mSSzV8D4MxupaxumcP8WeDa15SOvqIVYb7P8 \
     g41958e9e43_0_146 $'Sampling credits the whole timer interval to whichever method was running when it fired.\nIt answers where wall-clock time goes - not how often a method runs.\nHere: class loading dominates startup, so tune class loading itself, not the individual methods it calls.'
   ```
   Title (`A sampling profile example`) left untouched, as instructed. Result verified
   by re-dump: 3 paragraphs present, no stray empty bullets (unlike a `<DELETE>` row,
   `set-text`'s insert-then-delete-trailing approach leaves no emptied paragraphs behind).

2. **Slide 17 → 18 merge, then delete slide 17.**
   Slide 17 ("Profiler types") opens by saying profiling has two modes, sampling and
   instrumenting — this part is now purely duplicated by slide 18's diagram
   ("Profilers - Sampling vs Instrumenting") and is not worth keeping. The rest of
   slide 17 is a real, non-duplicated point (sampling's low overhead vs. its
   measurement-pitfall risk; instrumenting's higher intrusiveness vs. its
   bytecode-level visibility) that slide 18 did not have — slide 18 previously had only
   a title and the diagram image, no body text.
   Why not a row: this is new content on a slide that previously had no body text
   placeholder at all (only a title shape and an image element) — there is no existing
   text run to anchor a row's replacement against; a new shape had to be created.
   Folded-in text added as a new narrow text box in slide 18's left margin (does not
   overlap the diagram, which occupies the right ~2/3 of the body area):
   ```
   gslides.sh personal batch 17SQg1F2mSSzV8D4MxupaxumcP8WeDa15SOvqIVYb7P8 @/tmp/slide18_batch.json
   ```
   where the batch created a `TEXT_BOX` (`objectId: cohesionFold18Note`, 1,500,000 ×
   3,400,000 EMU at x=80,000, y=1,600,000 on slide 18, 12pt) and inserted:
   > Sampling — smallest overhead, but can distort where time appears to go.
   >
   > Instrumenting — more intrusive (adds bytecode), but gives exact visibility into
   > what's running.

   Then slide 17 deleted:
   ```
   gslides.sh personal delete 17SQg1F2mSSzV8D4MxupaxumcP8WeDa15SOvqIVYb7P8 g41958e9e43_0_124
   ```
   **Concern for the trainer:** the new text box's placement/size (narrow left-margin
   column, 12pt) was chosen from shape geometry only — I have no way to render the slide
   and visually confirm it doesn't look cramped or collide with the diagram's actual
   rendered bounds (vs. its declared bounding box). Please eyeball slide 18 (now at
   index 17) once.

3. **Slide 28 ("Blocking methods & thread timelines (contd)") — distinct title.**
   Why not a row: the title box is 3 runs — the shared title text, then a standalone
   single-space run, then `(contd)`. The single-space run's own content (` `) is not a
   usable row anchor: `grep -o -F -- " "` against slide 28's full scoped text matches
   every inter-word space on the slide (41 hits), not just that one run, so no `<DELETE>`
   row can target it safely. (Slide 23's equivalent title has no such stray space run —
   its title text run already ends in a space directly followed by `(contd)` — so that
   one *was* safe as rows; see table above.)
   Command used (`hf6ce07614bd9ec0_1_2` = slide 28's whole title shape, all 3 runs
   replaced in one `set-text` call, sidestepping the run-boundary problem entirely):
   ```
   gslides.sh personal set-text 17SQg1F2mSSzV8D4MxupaxumcP8WeDa15SOvqIVYb7P8 \
     hf6ce07614bd9ec0_1_2 "Blocking methods — filtering startup noise"
   ```
   Slide 27's title (`Blocking methods & thread timelines`) is untouched and remains the
   first of the pair.

## Verification run (this document's 4 rows only)

```
./scripts/deck-check.sh docs/deck-changes/07-1-copy-cohesion.md 17SQg1F2mSSzV8D4MxupaxumcP8WeDa15SOvqIVYb7P8
./scripts/deck-apply.sh docs/deck-changes/07-1-copy-cohesion.md 17SQg1F2mSSzV8D4MxupaxumcP8WeDa15SOvqIVYb7P8 --dry-run
./scripts/deck-apply.sh docs/deck-changes/07-1-copy-cohesion.md 17SQg1F2mSSzV8D4MxupaxumcP8WeDa15SOvqIVYb7P8
```

Output and independent post-apply re-verification (re-dump + grep counts, not just the
apply script's own exit code) recorded in the task report.
