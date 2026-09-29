# Deck 2.3 — Variability

> **STATUS: APPLIED 2026-09-29** — applied by `deck-apply.sh` and independently verified on a fresh dump (each replacement present exactly once, each deleted anchor gone). **DO NOT RE-RUN `deck-apply.sh` ON THIS FILE.**

~~STATUS: VERIFY DOCUMENT — dry-run only, nothing applied.~~

Source read: `gslides.sh personal text 172hds2xzS_FOmA9674KBJRICOVCK2x-50R14-MF0g5Q` on
2026-09-27, saved to `/tmp/deck-02-3-variability.txt` (170 lines, 10 slides, indices 0–9).
Read in full per brief instruction. Per the brief this deck is "correct and timeless" —
t-tests and p-values don't age — so it gets a light trim only, plus the one addition the
brief calls out: the deck never says *why* the lab harness reruns each scenario rather
than measuring once.

## Rows

| Slide | Anchor | Current context | Proposed text | Category | Source |
|---|---|---|---|---|---|
| 4 | the baseline and specimen are each run 3 times | "Regression testing" slide, closing run: "A batch program where the baseline and specimen are each run 3 times → following times:" | the baseline and specimen are each run 3 times — the same reason this course's lab harness reruns each scenario rather than measuring once → following times: | addition | docs/superpowers/specs/2026-09-22-java-perf-labs-and-deck-refresh-design.md |

## Notes

- **Anchor verification**: `grep -o -F -- "the baseline and specimen are each run 3 times" /tmp/deck-02-3-variability.txt | wc -l` → `1`. Single run; the trailing
  "→ following times:" (leading into the results table on the same slide) is
  unchanged, so the addition is inserted mid-run, not appended past the arrow.
- **Why slide 4, not slide 8.** The brief's own paraphrase pointed at "Further
  continued - if needed" (slide 8), but that slide is an unelaborated stub — three bare
  topic lines ("Further continued - if needed" / "t-tests and α-values" / "Statistical
  significance") with no sentence to extend. Slide 4 is where the deck already shows a
  concrete "run 3 times" example, which is the natural, single-run anchor for connecting
  to the lab harness's repeated runs — and it is the anchor the brief's underlying spec
  points at (`docs/superpowers/specs/2026-09-22-java-perf-labs-and-deck-refresh-design.md`
  line 120: "Light trim of the statistics run; connect to the harness's repeated runs").
- **Claim grounded, not asserted on faith**: the same spec, §7.2, states each lab's
  `lab.sh`/`lab.ps1` driver "runs baseline → captures → (participant fixes) → re-runs →
  prints a before/after table" — i.e. the harness itself is built around repeated,
  compared runs, for the same statistical-confidence reason this slide's regression-test
  example gives.
- **No light-trim rows proposed.** The rest of the deck (variability causes, t-test/
  p-value mechanics, null-hypothesis framing) reads as tight as the memory-leaks deck's
  already-timeless material — no bullet was long enough on inspection to justify a
  `reduction` row without cutting into the statistical content itself, which the brief
  says not to touch. Trim is therefore limited to what the one addition above already
  achieves implicitly (no separate cut needed).
- No version numbers introduced; no `outside-scope-ok` needed (1 in-scope / 0 out-of-scope).

## Manual actions (trainer, in Slides editor — not scripted)

None.

## Verify

```
./scripts/deck-check.sh docs/deck-changes/02-3-variability.md 172hds2xzS_FOmA9674KBJRICOVCK2x-50R14-MF0g5Q
./scripts/deck-apply.sh docs/deck-changes/02-3-variability.md 172hds2xzS_FOmA9674KBJRICOVCK2x-50R14-MF0g5Q --dry-run
```

## Before/after slide count

10 → 10 (no change — the brief asked for a light trim, not a slide-count cut, and this
deck's content did not surface a structural reduction worth flagging).
