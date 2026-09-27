# Deck change documents

One file per deck. Each row is one proposed edit.

| Column | Meaning |
|---|---|
| Slide | Slide index as `gslides.sh text` prints it (0-based), single index or comma-separated list (e.g. `18, 20`). **Populating this field restricts counting and replacement to the named slide(s)** — it is no longer documentation only. Blank/`-`/`—` means unscoped (whole-deck, the pre-scoping default). See "Slide scoping" below before ever putting a value here. |
| Anchor | Exact literal substring from the live deck, unique within its declared scope (or the whole deck if unscoped). Passed to `gslides.sh replace`/`batch` |
| Current context | Human-readable surrounding text, for the reviewer |
| Proposed text | Replacement string. The literal token `<DELETE>` means "delete this anchor" (replace with the empty string) — a genuine deletion, applied and reported explicitly, not a silently-skipped blank cell |
| Category | `correction`, `removal`, `addition`, `reduction`, `lab-slide`; add `skip` to reject a row; add `manual` for edits the trainer performs by hand in the Slides editor (not scripted); add `outside-scope-ok` on a scoped row whose anchor legitimately also occurs outside the declared scope (see "Slide scoping" below) |
| Source | URL backing the claim. Required for `correction` and `addition` |

Category may also carry an `xN` token declaring how many times the anchor is
*expected* to occur — **within the row's declared scope if the row is
scoped, or deck-wide if it is unscoped** — e.g. `correction x2`. Use this
when the same incorrect text legitimately appears more than once (on one
slide, or across multiple slides within an explicit Slide scope, or
deck-wide for an unscoped row) and all instances need the same fix — a
single `replaceAllText` (scoped or whole-deck) is exactly what you want in
that case. With no `xN` token, the expected count is 1 (existing default
behaviour): `deck-check.sh` reports `AMBIGUOUS` if such an anchor is found
more than once within scope, since it can't tell an intentional duplicate
from an accidental collision. With an `xN` token, `deck-check.sh` compares
the true in-scope occurrence count against N: it reports `ok` on a match,
`MISSING` on zero hits, and `COUNT MISMATCH` naming both numbers otherwise.

## Slide scoping

Populating the Slide column narrows both occurrence counting and the
`replace`/`replaceAllText` call itself to the named slide(s), via
`gslides.sh`'s `pageObjectIds`. Before slide scoping existed, `Slide` was
pure documentation and every count was always deck-wide, so an accidental
duplicate anchor anywhere in the deck was always caught (`AMBIGUOUS` in
`deck-check.sh`, `UNSAFE` in `deck-apply.sh`'s fail-closed pre-flight). That
is no longer automatically true once a row is scoped — narrowing the count
to a slide says nothing, by itself, about whether the same anchor also
exists elsewhere in the deck.

Both scripts guard against this with a **deck-wide cross-check**: every
scoped row's in-scope occurrence count is compared against the anchor's
true deck-wide count. If they disagree, the row is reported `UNSAFE`
(`deck-check.sh`) or aborts the whole run (`deck-apply.sh`'s fail-closed
pre-flight) — the message names both counts and states that the anchor also
appears outside the declared scope. **Always populate Slide expecting this
cross-check to run** — narrowing the Slide column does not, by itself,
exempt a row from the deck-wide safety net.

The escape hatch is the explicit Category token **`outside-scope-ok`**, for
the legitimate case where narrowing really is correct — e.g. the same
string occurs correctly, and must stay untouched, on another slide (see
`MANUAL-ACTIONS.md` item 8: `Java Flight Recorder` belongs on slide 18 but
is also correct, unrelated, on slide 42's link caption). With this token,
the deck-wide/in-scope mismatch is allowed and the row proceeds normally,
but the output still states how many occurrences lie outside the declared
scope — visible, never silent.

**Caveats**

- Anchor and Proposed text fields are parsed with `awk -F'|'`. A literal `|`
  character in either field will break the table's field alignment — never
  put a literal `|` in an Anchor or Proposed text cell.
- Both scripts strip leading/trailing whitespace from every field before use
  (`sed 's/^[[:space:]]*//; s/[[:space:]]*$//'`). Never rely on a boundary
  space to separate the anchor (or the replacement) from neighboring text —
  always choose an anchor that begins and ends on a real word. If the anchor
  is a fragment that depends on an adjacent space to read correctly once
  replaced, that space is silently dropped and the applied text can run
  together (e.g. producing "JDK 9JDK 11") even though `deck-check.sh` still
  reports `ok`, because the checker only verifies the anchor is found — not
  how it reads once replaced.

## Workflow

1. `./scripts/deck-check.sh <doc> <id>` — every anchor must verify before review.
2. Trainer reviews, marking rejected rows by adding `skip` to the Category column.
3. `./scripts/deck-apply.sh <doc> <id>` — applies and re-verifies. Rows marked `skip` or `manual` are not applied.

## Index

| Deck | Change doc | Status |
|---|---|---|
| 5.1 Intro to GC | `05-1-gc-intro.md` | applied 2026-09-23 |
| 4.2 JIT compiler | `04-2-jit.md` | applied 2026-09-24 |
| 7.1 Monitoring & profiling | `07-1-profiling-tools.md` | applied 2026-09-24 |
| 5.2 Choosing a GC | `05-2-choosing-gc.md` | not started |
| 5.3 Basic GC tuning | `05-3-gc-tuning.md` | not started |
| 6.1 Largest heap objects | `06-1-heap-objects.md` | not started |
| 6.2 Memory leaks | `06-2-memory-leaks.md` | not started |
| 2 Performance improvements workflow | `02-workflow.md` | verify doc ready 2026-09-27 |
| 2.1 Test the real application | `02-1-real-application.md` | verify doc ready 2026-09-27 |
| 2.2 Throughput, batching, response times | `02-2-throughput.md` | verify doc ready 2026-09-27 |
| 2.3 Variability | `02-3-variability.md` | verify doc ready 2026-09-27 |
| 2.4 Test early, test often | `02-4-test-early.md` | verify doc ready 2026-09-27 |
| 3 + 3.1 Toolbox & profiling | `03-toolbox-profiling.md` | not started |
| 3.2–3.4 CPU/Disk/Network | `03-2-4-cpu-disk-network.md` | not started |
| 1 + Overview | `01-overview.md` | not started |
| 5.1 pass 2 (items 1) | `05-1-gc-intro-pass2.md` | applied 2026-09-26 |
| 7.1 pass 2 (items 6-11) | `07-1-profiling-tools-pass2.md` | applied 2026-09-26 |
| Lab & prerequisite slides | `99-lab-slides.md` | not started |
