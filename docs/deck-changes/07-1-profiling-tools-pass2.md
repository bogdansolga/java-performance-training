# Deck 7.1 — Java monitoring & profiling tools — pass 2

> **STATUS: APPLIED 2026-09-26** — confirmed on the live deck 2026-09-29. **DO NOT RE-RUN `deck-apply.sh` ON THIS FILE**; append-style rows would double.

~~STATUS: DRAFT — not yet applied.~~ Steps 1–3 only (read, write, check). Step 4 (trainer
approval gate) and Step 5 (`deck-apply.sh` for real) are intentionally **not done** by this pass
— out of scope per the pass-2 brief's scope limit. `deck-apply.sh` was invoked only with
`--dry-run`; the live deck was never modified.

This is a **second pass**, converting items 6, 7, 8, 9, 10 and 11 of `MANUAL-ACTIONS.md`'s Deck
7.1 section into scripted rows now that slide scoping and `outside-scope-ok` exist. The
first-pass document, `07-1-profiling-tools.md`, is marked APPLIED and is not touched here — a
second pass gets its own document. Items 12–14 of `MANUAL-ACTIONS.md` (image placement, screenshot
deletion, the slide-merge/cut shortening pass) are out of scope for this pass — `gslides.sh`
cannot insert/delete images or slides, so those stay manual regardless of scoping.

Source read: `gslides.sh personal text 1952R9NhvuYNuG9TujMfEPpy_w6cZ_yfMvl5zlZb9yoU` on
2026-09-24, saved to `/tmp/deck-7-1-now.txt` (990 lines, 44 slides, slide indices 0–43). Every
anchor below was also cross-checked against the raw Slides API JSON (per-text-run content,
fetched directly, not through the lossy `gslides.sh text` dump) to confirm real run boundaries
and rule out accidental line-splitting artifacts — see Notes.

Every non-blank Anchor below is verified with:

```
grep -o -F -- "<anchor>" /tmp/deck-7-1-now.txt | wc -l          # deck-wide
<slide-slice> | grep -o -F -- "<anchor>" | wc -l                 # in declared scope
```

Verbatim output for every row is in
`.superpowers/sdd/2026-09-23-plan-a-deck-modernization/pass2-rows-report.md`.

## Rows

| Slide | Anchor | Current context | Proposed text | Category | Source |
|---|---|---|---|---|---|
| 1 | jhat | "Tools that come with the JDK" list — the `jhat` entry's tool-name text run (own run, separate from its description run below); `jhat` was removed in JDK 9 | <DELETE> | removal, outside-scope-ok | https://openjdk.org/jeps/241 |
| 1 | - reads and helps analyse memory heap dumps | Same `jhat` entry — the description text run immediately following the name run above | <DELETE> | removal | https://openjdk.org/jeps/241 |
| 16 | jhat | "Processing" section — `JDK tools - including visualvm & ` + a bare `jhat` run; this run is byte-identical to slide 1's `jhat` run but needs a different fix (a correction here, a deletion on slide 1) | `jcmd <pid> GC.heap_dump` for heap dumps, then JMC or Eclipse MAT | correction, outside-scope-ok | https://openjdk.org/jeps/241 |
| 18 | Java Flight Recorder | Paid profilers list — `Java Flight Recorder` is its own line, between `Stackify Prefix` and the (already-corrected) `JProbe` line; free and open-source since JDK 11 (JEP 328), so it does not belong on the Paid list | <DELETE> | removal, outside-scope-ok | https://openjdk.org/jeps/328 |
| 18 | Async profiler | Free profilers list — last line, `Async profiler`; `Java Flight Recorder` is added here (abbreviated `JFR`, matching the deck's existing `JMC` abbreviation style) to complete the move out of the Paid list | Async profiler, JFR | addition | https://openjdk.org/jeps/328 |
| 18 | (deprecated by | Paid list, `JProbe` line — first run of the stale parenthetical `(deprecated by the developing company?)` that follows the already-corrected `JProbe — discontinued …` text; the parenthetical is now a redundant hedge | <DELETE> | removal | n/a |
| 18 | the developing company | Same parenthetical — middle run | <DELETE> | removal | n/a |
| 18 | ?) | Same parenthetical — closing run | <DELETE> | removal | n/a |
| 37 | → the GlassFish startup in | "Native profilers" slide — tabbed caveat sentence, opening run: `→ the GlassFish startup in ` + `Oracle Developer Studio` + ` → a native profiler`; Oracle Developer Studio is discontinued | → native profiling of a Java process, using | correction | https://github.com/async-profiler/async-profiler |
| 37 | Oracle Developer Studio | Same sentence — middle run; this exact run text also occurs on slide 38, needing a different rewrite there | async-profiler or perf | correction, outside-scope-ok | https://github.com/async-profiler/async-profiler |
| 37 | → a native profiler | Same sentence — closing run; deleted rather than replaced with a trailing `→`, because a bare `→` already occurs twice within slide 37 before this row applies (once in this same anchor, once in the row above's target text), which would make the replacement text non-empty ("safe to apply") check fail — the sentence reads fine without a trailing arrow once the row above ends in "using " | <DELETE> | removal | https://github.com/async-profiler/async-profiler |
| 38 | The GlassFish startup profile, showed in the | "Native profiling" slide — opening sentence, first run: `The GlassFish startup profile, showed in the ` + `Oracle Developer Studio` + ` profiling tool`; this walkthrough's screenshot is separately slated for manual deletion (see `MANUAL-ACTIONS.md`) | A native CPU profile, captured with async-profiler / perf | correction | https://github.com/async-profiler/async-profiler |
| 38 | Oracle Developer Studio | Same sentence — middle run; this exact run text also occurs on slide 37, needing a different rewrite there | <DELETE> | removal, outside-scope-ok | https://github.com/async-profiler/async-profiler |
| 38 | profiling tool | Same sentence — closing run (` profiling tool`); the phrase "profiling tool(s)" is common elsewhere in the deck (slides 0, 19, 23, 37) describing other, unrelated tools | <DELETE> | removal, outside-scope-ok | https://github.com/async-profiler/async-profiler |
| 38 | Also works on Linux systems | Same slide — separate paragraph directly below the sentence above; redundant once the described tool (async-profiler) is Linux-native by default | <DELETE> | removal | https://github.com/async-profiler/async-profiler |

15 rows: 4 `correction`, 1 `addition`, 10 `removal` (of which 6 carry `outside-scope-ok`).

## Notes / exceptions

- **Every multi-run phrase was split into one row per text run, not one row per phrase.** All six
  items (6, 7, 8, 9, 10, 11 in the brief's numbering — item 1 is Deck 5.1's and lives in the sibling
  `05-1-gc-intro-pass2.md`) involve a target phrase that the live Slides API returns as 2–4
  separate `textRun` elements. Read directly from the raw API JSON (not
  the `gslides.sh text` dump — see next note), these runs are almost always continuous text with
  no real newline between them, so `replaceAllText` would plausibly match a cross-run anchor. But
  `deck-check.sh`/`deck-apply.sh` compute their occurrence counts via `grep -o -F` against a
  `gslides.sh personal text` dump, and that dump prints **one line per text run**, inserting an
  artificial line break at every run boundary regardless of whether a real newline exists in the
  source (confirmed by fetching the raw JSON directly for slides 1, 16, 18, 37 and 38 and
  diffing run content against the dump). A cross-run anchor can therefore never be found by
  `grep -F` in that dump, so `deck-check.sh` would report it `MISSING` even if the live API might
  have matched it. Splitting into one row per run sidesteps this: every anchor above is either an
  entire run or the non-whitespace core of one run (leading/trailing whitespace is stripped by
  both scripts' field parsing regardless of what's written in the cell, per `README.md`), so every
  anchor is verifiable by `grep -F` against the dump exactly as the scripts do it. The resulting
  edits were checked by hand to still read as one coherent sentence/line once every row in a group
  is applied (see the per-item breakdown below).
- **Item 8 — "move" implemented as delete + append, using the deck's own abbreviation style.**
  `gslides.sh` cannot reposition a paragraph between two lists (no split/merge/move of text runs,
  per its own header comment), so a literal "move" is not directly scriptable. Deleting
  `Java Flight Recorder` from the Paid list and then *appending the same literal string* to a Free
  list line was tried first and rejected: it would make the Paid-list deletion row's own anchor
  (`Java Flight Recorder`) reappear elsewhere on slide 18, which breaks that row's own
  already-applied/verify check (`deck-apply.sh` requires the anchor's in-scope count to reach 0)
  and also violates this repo's no-cross-row-collision rule (a row's Proposed text must not contain
  a *different* row's Anchor). Using the abbreviation `JFR` for the addition — consistent with the
  deck's own unexpanded `JMC` abbreviation two lines above it — avoids the collision entirely while
  still completing the functional move (JFR is gone from Paid, present on Free).
- **Item 10's third row was caught by `deck-apply.sh --dry-run`, not `deck-check.sh`.** The first
  draft replaced `→ a native profiler` with a trailing `→`. `deck-check.sh` passed (the anchor
  itself is unique in scope), but `deck-apply.sh --dry-run` reported it `UNSAFE`: a bare `→`
  already occurs twice on slide 37 before any row applies (once inside this row's own anchor, once
  inside the row above's target text), so the plain-style "safe to apply" precondition
  (replacement text occurs zero times pre-edit) never holds. Changed to `<DELETE>` — the sentence
  still reads correctly because the row above's replacement already ends in "using ". This is why
  the brief's four `--dry-run`/`deck-check.sh` commands matter even for rows that look obviously
  correct: `deck-check.sh` alone does not catch a Proposed-text collision with pre-existing deck
  content, only `deck-apply.sh`'s classifier does.
- **Item 9 — three runs, three rows, no `outside-scope-ok` needed.** `(deprecated by `,
  `the developing company` and `?)` (a lone space run between the corrected `JProbe` text and the
  parenthetical is left untouched — it's invisible whitespace, not visible stray punctuation) each
  verify as unique both within slide 18 and deck-wide, so none needed `outside-scope-ok`. After all
  three deletions the line ends "…not currently sold by Quest/Dell)" followed only by invisible
  trailing whitespace.
- **Items 10 and 11 — `Oracle Developer Studio` occurs exactly twice deck-wide, once per slide,
  needing different rewrites.** Exactly as the brief anticipated: both rows are scoped to their own
  slide (37, 38) and both carry `outside-scope-ok`, each pointing at the other slide's occurrence.
- **Cross-row collision check (this document).** For every row, its Proposed text was checked
  against every *other* row's Anchor in this document: no row's Proposed text contains a different
  row's Anchor as a substring (the two `Async profiler`/`JProbe`-style append rows — slide 18's
  `Async profiler, JFR` — only contain their *own* Anchor, which is the expected append pattern,
  not a collision). `Oracle Developer Studio` and `jhat` are each used as an Anchor on two
  different, mutually-scoped rows by design (see above), never as an unscoped/whole-deck anchor.
- **Not scriptable after all:** none of items 6, 7, 8, 9, 10, 11 turned out to be genuinely
  unscriptable once split per-run; all six are covered by the 15 rows above. `MANUAL-ACTIONS.md`
  items 12–14 (image placement, screenshot deletion, slide merge/cut) remain manual — out of scope
  for this pass, not because of the run-splitting/scoping techniques used here, but because
  `gslides.sh` cannot create, delete or reorder slides or images at all.
