# Deck 5.1 — Introduction to Garbage Collection — pass 2

**STATUS: DRAFT — not yet applied.** Steps 1–3 only (read, write, check). Step 4 (trainer
approval gate) and Step 5 (`deck-apply.sh` for real) are intentionally **not done** by this pass
— out of scope per the pass-2 brief's scope limit. `deck-apply.sh` was invoked only with
`--dry-run`; the live deck was never modified.

This is a **second pass**, converting item 1 of `MANUAL-ACTIONS.md`'s Deck 5.1 section into a
scripted row now that slide scoping and `outside-scope-ok` exist. The first-pass document,
`05-1-gc-intro.md`, is marked APPLIED and is not touched here — a second pass gets its own
document.

Source read: `gslides.sh personal text 1Y2xjPThug1WM2haytVv8M64tQDAIkI8TFl5CO4FU2lc` on
2026-09-24, saved to `/tmp/deck-5-1-now.txt` (1026 lines, 44 slides, slide indices 0–43).

The anchor below was re-derived from the live read, not copied from `MANUAL-ACTIONS.md` (whose
phrasing is a paraphrase — see Notes). Verified with:

```
grep -o -F -- "JDK 11" /tmp/deck-5-1-now.txt | wc -l
```

## Rows

| Slide | Anchor | Current context | Proposed text | Category | Source |
|---|---|---|---|---|---|
| 22 | JDK 11 | "The G1 collector (continued)" — the "Usage" bullet ends with `Enabled by default since ` (one text run) followed by `JDK 11` (a second, separately-styled text run); G1 actually became the default collector in JDK 9, not JDK 11 | JDK 9 | correction, outside-scope-ok | https://openjdk.org/jeps/248 |

1 row: 1 `correction`.

## Notes / exceptions

- **Why the anchor is `JDK 11`, not the full phrase.** `MANUAL-ACTIONS.md` item 1 (and the pass-2
  brief paraphrasing it) frames this as "the full phrase, not `JDK 11`", because a *whole-deck*,
  unscoped replace of the bare string `JDK 11` would also corrupt slides 29, 30 and 37, where
  `JDK 11` appears correctly (Epsilon GC introduced in JDK 11 / disabled by default since JDK 11;
  CMS "JDK 11 only"). That was true before slide scoping existed. It no longer forces the anchor
  to be the full phrase: scoping this row to **slide 22 only** (via `pageObjectIds`) makes the
  narrower anchor `JDK 11` safe to use, because `replaceAllText` only touches slide 22's shapes,
  and `outside-scope-ok` documents (and `deck-check.sh`/`deck-apply.sh` re-verify) that the
  other 3 deck-wide occurrences are legitimate and must survive untouched.
- **The full phrase was tried and rejected.** `Enabled by default since JDK 11` was the first
  candidate anchor, matching the brief's paraphrase. It was rejected: the live read shows `Enabled
  by default since ` and `JDK 11` as two separate text runs with no real newline between them (the
  underlying shape text is continuous — confirmed by reading the raw Slides API JSON directly, not
  just the `gslides.sh text` dump). `replaceAllText` itself would likely match across that run
  boundary (this is the "replaceAllText matches across text runs" premise of the pass-2 brief), but
  `deck-check.sh`/`deck-apply.sh` compute their occurrence counts via `grep -o -F` against the
  *same* `gslides.sh personal text` dump this document verifies against — and that dump prints
  every text run on its own line (one `jq` output value per run, regardless of whether a real
  newline exists in the source), so a cross-run anchor can never be found by `grep -F` in that
  dump even though the API might match it. Using `JDK 11` (a single, whole run) sidesteps this
  entirely and is fully verifiable by both `deck-check.sh` and this document's own `grep` command
  above. After the replace, the line reads "Enabled by default since JDK 9" as intended.
- **Deck-wide occurrences of `JDK 11` (4 total, 1 in scope + 3 outside):**
  ```
  22: JDK 11                                                                        (this row)
  29: Introduced in JDK 11 as                                                       (Epsilon — correct, untouched)
  30: The Epsilon GC is disabled by default since JDK 11                            (Epsilon — correct, untouched)
  37:  - concurrently collects the old generation, while app threads are running (JDK 11 only; removed in JDK 14)   (CMS — correct, untouched)
  ```
- **Not scriptable / turned out NOT to need scripting:** none. All of item 1 is covered by the
  single row above.
