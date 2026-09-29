# Deck 2.2 — Throughput, batching & response times

> **STATUS: APPLIED 2026-09-29** — applied by `deck-apply.sh` and independently verified on a fresh dump (each replacement present exactly once, each deleted anchor gone). **DO NOT RE-RUN `deck-apply.sh` ON THIS FILE.**

~~STATUS: VERIFY DOCUMENT — dry-run only, nothing applied.~~

Source read: `gslides.sh personal text 1VN5hu2HWR25jmBczD4Vmugey5BIBd9ha_KDnKK968wA` on
2026-09-27, saved to `/tmp/deck-02-2-throughput.txt` (361 lines, 18 slides, indices 0–17).
Read in full per brief instruction. The elapsed-time/warm-up material, the throughput
definitions (TPS/RPS/OPS) and the response-time/think-time material are correct and
timeless — not touched. Per the brief, this deck gets the group's heaviest text
simplification: client-overload (slide 9), average-vs-90th-percentile (slide 12) and the
outlier walkthrough (slides 13–16) are all correct but long. The harness can shorten text
in place; it cannot delete or merge slides, so the slide-count reduction for the outlier
walkthrough is flagged as a Manual action below, not a row.

## Rows

| Slide | Anchor | Current context | Proposed text | Category | Source |
|---|---|---|---|---|---|
| 9 | risk that a client cannot send data fast enough to the server | "Client overloading risk" slide, opening run: "Client-server tests" + ": risk that a client cannot send data fast enough to the server" + " → may occur from several causes:" | risk that the client is the bottleneck, not the server | reduction | n/a |
| 12 | Outliers affect the calculation - large outliers have a large effect on the average response time | "Measuring response times" slide (average vs. percentile), closing bullet before "Further discussed" | Outliers affect the calculation - large outliers pull the average; a percentile barely moves | reduction | n/a |
| 16 | If needed: 95th% / 99th% response time | "Outliers and percentiles" slide, secondary bullet between the 90th%-focus line and the "Focusing on a number" line | <DELETE> | reduction | n/a |

## Notes

- **Slide 9 anchor verification**: `grep -o -F -- "risk that a client cannot send data fast enough to the server" /tmp/deck-02-2-throughput.txt | wc -l` → `1`. Single run; the
  unchanged "Client-server tests" lead-in and "→ may occur from several causes:"
  trailer still frame it correctly after the edit.
- **Slide 12 anchor verification**: `grep -o -F -- "Outliers affect the calculation - large outliers have a large effect on the average response time" /tmp/deck-02-2-throughput.txt | wc -l` → `1`. This row keeps the brief's required lesson —
  "outliers move average and percentile differently" — on the slide that first
  introduces average vs. percentile, just stated tighter.
- **Slide 16 anchor verification**: `grep -o -F -- "If needed: 95th% / 99th% response time" /tmp/deck-02-2-throughput.txt | wc -l` → `1`. This is the one row using
  `<DELETE>` (not a shorten-style substring row): it removes a secondary elaboration
  bullet while leaving the slide's actual payoff intact — "Even better - look at both: /
  The average response time / At least one percentile-based response time → do not miss
  the case of large outliers" is untouched and is exactly the "report average *and*
  percentile" lesson the brief says to keep.
- **Deleting slide 16's bullet leaves an empty bullet** (harness deletes text, not the
  paragraph) — see Manual actions.
- **Both brief's quoted phrases usable, after re-deriving against the actual dump** — the
  brief's own paraphrases for slides 9/16 were not verbatim; all anchors above were
  re-grepped from the dump, not taken from the brief or the modernization plan's earlier
  draft (whose slide-9/15 anchors also did not match the live text verbatim — both
  spanned multiple text runs).
- No `outside-scope-ok` rows needed — all three anchors are 1-in-scope/0-out-of-scope.

## Manual actions (trainer, in Slides editor — not scripted)

1. **Slide count reduction (structural — cannot be scripted).** Slides 13 ("Average
   response time"), 14 ("Average response time with an outlier") and 15 ("Measuring
   response times" — the numeric walkthrough of both charts) together retell one idea
   three times: an average-only view, then the same view with one 100-second outlier
   added. Recommend deleting slide 14 (title-only image slide) and trimming slide 15 to
   the single outlier example (drop the no-outlier numbers, since slide 13's image
   already shows that case) — collapsing a 3-slide walkthrough to 2 slides without losing
   the worked example. Left as a recommendation for trainer review, not applied.
2. After the `<DELETE>` on slide 16 applies, remove the now-empty bullet in the Slides
   editor (the harness clears the text but not the paragraph).

## Verify

```
./scripts/deck-check.sh docs/deck-changes/02-2-throughput.md 1VN5hu2HWR25jmBczD4Vmugey5BIBd9ha_KDnKK968wA
./scripts/deck-apply.sh docs/deck-changes/02-2-throughput.md 1VN5hu2HWR25jmBczD4Vmugey5BIBd9ha_KDnKK968wA --dry-run
```

## Before/after slide count

18 → 18 via scripted rows (text-only; no slides removed by the harness). 18 → 16 if the
trainer accepts Manual action 1 (deleting slide 14, folding slide 15 down to one worked
example) — that reduction is a human decision, not applied here.
