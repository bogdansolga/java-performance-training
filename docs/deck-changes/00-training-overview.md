# Deck 0 — Training overview

**STATUS: VERIFY DOCUMENT — dry-run only, nothing applied.**

Source read: `gslides.sh personal text 1LbaJWLitcqeruFJaQ4dMkqWANdoKT0uiw2LTXxRAxeM` on
2026-09-27, saved to `/tmp/deck-00-training-overview.txt` (174 lines, 11 slides, indices
0–10 — more than the brief's "~6 slides" estimate). Both proposed additions in this pass
are brand-new slides with no existing on-slide text; the harness cannot script structural
work (new slides), so both are `manual` rows per the conventions and the brief, with the
exact content to add given in full below.

## Course structure — matches the agreed shape

The brief asks to confirm the deck's stated course structure against the agreed **5 days
× 4 hours**, and to flag (not silently fix) any mismatch. Slide 1 ("Training overview"),
under "Structure", reads verbatim:

```
5 days, ~4 hours / day
```

**This matches.** No mismatch found (unlike the brief's cited earlier example of a "5
days, ~4 hours/day" vs "3 days × 7 hours" discrepancy) — no row proposed, nothing to fix.

## Rows

| Slide | Anchor | Current context | Proposed text | Category | Source |
|---|---|---|---|---|---|
| — |  | — (new slide) | Manual: insert a new slide immediately after slide 2 ("Training objectives"), before slide 3 ("Please present yourself") — the six labs slide, see Manual actions #1 | addition, manual | — |
| — |  | — (new slide) | Manual: insert a new slide immediately after the six-labs slide (above), still before slide 3 ("Please present yourself") — the prerequisites slide, see Manual actions #2 | addition, manual | — |

Both rows have a blank Anchor (there is no existing on-slide text to anchor a new slide
to), so `deck-check.sh` skips both (blank-anchor lines are not counted) and
`deck-apply.sh` skips both as `manual`. No anchor-uniqueness check applies to either row.

## Manual actions (for trainer, in Slides editor — not scripted)

**1. New slide after slide 2 ("Training objectives"), before slide 3 ("Please present
yourself") — the six labs.**

There are six labs, not five: an earlier draft of this slide listed five; "humongous
allocations" was added after a participant pre-call surfaced it as a live production
failure. Content describes issues only, never fixes:

```
The six labs

1. Unbounded retention   - the heap grows every cycle and never comes back
2. N+1 queries           - one page view, hundreds of statements
3. Lock contention       - throughput stops scaling with threads
4. GC mismatch           - the wrong collector for the workload
5. Code cache exhaustion - the JIT stops compiling, throughput collapses
6. Humongous allocations - large objects bypass the young generation

Each lab: reproduce it, investigate it with real tools, fix it, measure before and after.
```

**2. New slide immediately after the six-labs slide (above), still before slide 3
("Please present yourself") — prerequisites.**

Mirrors `PREREQUISITES.md`. Participants are on Windows 10/11:

```
Before day one

Required: JDK 17 and JDK 21 on your PATH
Optional: JDK 25 (used in the GC lab)
Also:     Git, and port 8080 free

Run scripts\preflight.ps1 and bring the output.
Anything red is in docs/PREFLIGHT-TROUBLESHOOTING.md.
```

**3. Both new slides shift every slide from index 3 onward down by two** (e.g. "Please
present yourself" moves from index 3 to 5, "Question day" from 10 to 12). No other row in
this document depends on slide indices past 2, so no renumbering conflicts.

## Notes / exceptions

- **`PREREQUISITES.md`, `scripts\preflight.ps1` and `docs/PREFLIGHT-TROUBLESHOOTING.md`
  were not found in this repository** (checked with `find` across the whole tree). The
  prerequisites slide content above is reproduced verbatim from the brief's draft and
  could not be cross-checked against a mirrored source file in-repo. Flagged for the
  trainer to confirm the draft still matches whatever `PREREQUISITES.md` says wherever it
  actually lives.
- **Prerequisites slide placement was not specified by the brief** (only the labs slide's
  placement — "immediately after the objectives slide" — was). Placed it directly after
  the new labs slide, before "Please present yourself", so the deck's opening run reads:
  overview → objectives → the six labs → prerequisites → introductions. This ordering is
  a proposal, not a brief requirement; flagged for trainer confirmation like any other
  structural choice.
- **JDK version numbers in the prerequisites draft (17, 21, 25) match the covered-release
  set** from the conventions (11, 17, 21, 25 — 11 is deck-coverage only, never a lab
  runtime) and the "decks are narrated against 21" rule; JDK 25 correctly marked
  Optional/lab-only. No correction needed.
- **Course structure check**: see the dedicated section above — matches 5 days × 4 hours,
  no mismatch, no row.

## Verify

```
./scripts/deck-check.sh docs/deck-changes/00-training-overview.md 1LbaJWLitcqeruFJaQ4dMkqWANdoKT0uiw2LTXxRAxeM
./scripts/deck-apply.sh docs/deck-changes/00-training-overview.md 1LbaJWLitcqeruFJaQ4dMkqWANdoKT0uiw2LTXxRAxeM --dry-run
```

Both rows above have blank anchors (structural/manual), so both scripts skip them; this
document has no automatable rows to verify anchor-uniqueness for. Both commands should
exit 0 with 0 automatable rows processed.
