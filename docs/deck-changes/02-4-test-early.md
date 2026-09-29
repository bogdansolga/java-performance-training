# Deck 2.4 — Test early, test often

> **STATUS: APPLIED 2026-09-29** — applied by `deck-apply.sh` and independently verified on a fresh dump (each replacement present exactly once, each deleted anchor gone). **DO NOT RE-RUN `deck-apply.sh` ON THIS FILE.**

~~STATUS: VERIFY DOCUMENT — dry-run only, nothing applied.~~

Source read: `gslides.sh personal text 1y6l5OwUifzcdmI1WP7DidMg4h7aqs0lu9YTMYrkOiTc` on
2026-09-27, saved to `/tmp/deck-02-4-test-early.txt` (375 lines, 17 slides, indices 0–16).
Read in full per brief instruction. Per the brief this is "the most reducible material" in
the group, and its CI framing is waterfall-era — built around a "feature-freeze date"
release cycle instead of continuous delivery, and its "run on target system" guidance
predates containers. Both are fixed below. The "automate everything" / "measure
everything" pipeline guidance, the client/staging/prod parity material, and the
"very important starting questions" slide are all still sound and untouched.

## Rows

| Slide | Anchor | Current context | Proposed text | Category | Source |
|---|---|---|---|---|---|
| 2 | All code changes must be checked into at an early point in the release cycle | "Conflicting forces & directions" slide, under "A typical development cycle entails a feature-freeze date:" | In continuous delivery there is no feature-freeze date — regressions must be caught per merge request, not at a checkpoint | correction | https://continuousdelivery.com/ |
| 10 | 12 factor app? | "3. Run on the target system (continued)" slide, trailing orphan line after "The performance of a target environment - can only be known by testing on the same env" | In containers, this changes again: since JDK 10 (and 8u191+), the JVM reads the cgroup CPU quota and memory limit instead of the host's — "the target system" means the container's assigned limits, not the physical core count | addition | https://bugs.openjdk.org/browse/JDK-8146115 |

## Notes

- **Slide 2 anchor verification**: `grep -o -F -- "All code changes must be checked into at an early point in the release cycle" /tmp/deck-02-4-test-early.txt | wc -l` → `1`.
  Single run, the whole bullet under the "feature-freeze date:" lead-in (itself split
  across three runs — "A typical development cycle entails a " / "feature-freeze" /
  " date: " — so that lead-in phrase can't be used as an anchor; this bullet is the
  clean single-run target). Both the modernization plan's earlier draft anchor and the
  brief's own paraphrase for this row spanned multiple runs and were not usable
  verbatim — re-derived from the grepped dump.
- **Slide 10 anchor verification**: `grep -o -F -- "12 factor app?" /tmp/deck-02-4-test-early.txt | wc -l` → `1` deck-wide. This is a standalone, unelaborated line on
  slide 10 — reads like a question the deck author never followed up on — which is
  exactly the placeholder the brief's "modernize to containers, cgroup limits, not just
  core count" instruction belongs on. Chosen over the earlier plan draft's anchor (a
  fragment of slide 9's "behaves very differently" sentence, which spans three separate
  runs and isn't a single-run substring). **First `deck-check.sh` run correctly caught a
  mis-scoping**: the row was initially declared as slide 9 by a by-eye slip while
  drafting — `deck-check.sh` reported `UNSAFE (0 within scope, 1 deck-wide)`, i.e. the
  anchor is real but not on slide 9. Re-checked against the dump: the "12 factor app?"
  line is on slide 10 ("3. Run on the target system (continued)"), not slide 9 ("3. Run
  on the target system"). Corrected to slide 10 and re-verified — `ok`.
- **Container claim verified, not taken on faith**: fetched JDK-8146115
  ("Improve docker container detection resource configuration usage") on 2026-09-27 —
  container-aware CPU/memory sizing landed in JDK 10 (backported to 8u191+), enabled by
  default (`-XX:+UseContainerSupport`). All four covered releases (11, 17, 21, 25) post-date
  this, so the claim holds for every version this course teaches — no version badge
  needed since it applies to the whole covered range.
- **CD/feature-freeze claim**: an industry-practice framing correction, not a
  version-specific technical fact, so it carries a canonical reference
  (continuousdelivery.com) rather than a JEP citation.
- No `outside-scope-ok` rows needed — both anchors are 1-in-scope/0-out-of-scope.

## Manual actions (trainer, in Slides editor — not scripted)

1. **Slide 3 ("The challenges in detail")** re-tells the same feature-freeze scenario at
   length — "Committing code in the evening before the feature freeze - might cause a 20%
   regression ... otherwise may be considered 'temporary'" is a worked example of the
   waterfall framing slide 2 now corrects, and reads oddly once slide 2 says there is no
   feature-freeze date. This spans multiple text runs, so it can't be shortened with a
   single-run row without leaving a grammatically broken fragment; recommend the trainer
   either delete this bullet chain or rewrite it around a CD example (e.g. "a merge
   request causing a 1.5% regression") during review.
2. **Slide 13** ("'I don't always test my code, but when I do…'") is a meme slide with no
   informational content. Given this deck is flagged as the group's most reducible,
   consider cutting it — a purely structural (slide-delete) decision, left to the
   trainer.

## Verify

```
./scripts/deck-check.sh docs/deck-changes/02-4-test-early.md 1y6l5OwUifzcdmI1WP7DidMg4h7aqs0lu9YTMYrkOiTc
./scripts/deck-apply.sh docs/deck-changes/02-4-test-early.md 1y6l5OwUifzcdmI1WP7DidMg4h7aqs0lu9YTMYrkOiTc --dry-run
```

## Before/after slide count

17 → 17 via scripted rows (text-only; no slides removed by the harness). 17 → 16 if the
trainer accepts Manual action 2 (deleting slide 13's meme); potentially 17 → 15 if slide
3 is also folded into slide 2 per Manual action 1. Both are human decisions, not applied
here.
