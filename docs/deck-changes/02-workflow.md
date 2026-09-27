# Deck 2 — Performance improvements workflow

**STATUS: VERIFY DOCUMENT — dry-run only, nothing applied.**

Source read: `gslides.sh personal text 10JpeuGMWWIKGsWBicQJwDwekBpDw-V2T83tPNSBbw8k` on
2026-09-27, saved to `/tmp/deck-02-workflow.txt` (42 lines, 3 slides, indices 0–2). Read in
full per brief instruction.

## Rows

None. This deck is a 3-slide agenda/index (title, animated workflow diagram, animated
"four principles" list) with no prose to shorten and no factual claims to correct. It is
already the smallest deck in the group — there is nothing left to reduce without deleting
the index itself, which is out of scope. Slide 2's four bullets ("Test real application",
"Understand throughput, batching & response time", "Understand variability", "Test early,
test often") are simply the titles of decks 2.1–2.4 and stay as-is so the index continues
to match the group.

## Manual actions (trainer, in Slides editor — not scripted)

None.

## Verify

```
./scripts/deck-check.sh docs/deck-changes/02-workflow.md 10JpeuGMWWIKGsWBicQJwDwekBpDw-V2T83tPNSBbw8k
./scripts/deck-apply.sh docs/deck-changes/02-workflow.md 10JpeuGMWWIKGsWBicQJwDwekBpDw-V2T83tPNSBbw8k --dry-run
```

Zero rows — both scripts have nothing to check or apply; this document exists to record
that the deck was read and deliberately left untouched.

## Before/after slide count

3 → 3 (no change).
