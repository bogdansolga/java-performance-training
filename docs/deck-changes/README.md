# Deck change documents

One file per deck. Each row is one proposed edit.

| Column | Meaning |
|---|---|
| Slide | Slide index as `gslides.sh text` prints it (0-based) |
| Anchor | Exact literal substring from the live deck, unique within it. Passed to `gslides.sh replace` |
| Current context | Human-readable surrounding text, for the reviewer |
| Proposed text | Replacement string |
| Category | `correction`, `removal`, `addition`, `reduction`, `lab-slide`; add `skip` to reject a row; add `manual` for edits the trainer performs by hand in the Slides editor (not scripted) |
| Source | URL backing the claim. Required for `correction` and `addition` |

Category may also carry an `xN` token declaring how many times the anchor is
*expected* to occur in the deck verbatim, e.g. `correction x2`. Use this when
the same incorrect text legitimately appears on multiple slides and all
instances need the same fix — a single whole-deck `replaceAllText` is exactly
what you want in that case. With no `xN` token, the expected count is 1
(existing default behaviour): `deck-check.sh` reports `AMBIGUOUS` if such an
anchor is found more than once, since it can't tell an intentional duplicate
from an accidental collision. With an `xN` token, `deck-check.sh` compares
the true occurrence count against N: it reports `ok` on a match, `MISSING`
on zero hits, and `COUNT MISMATCH` naming both numbers otherwise.

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
| 5.1 Intro to GC | `05-1-gc-intro.md` | not started |
| 4.2 JIT compiler | `04-2-jit.md` | not started |
| 7.1 Monitoring & profiling | `07-1-profiling-tools.md` | not started |
| 5.2 Choosing a GC | `05-2-choosing-gc.md` | not started |
| 5.3 Basic GC tuning | `05-3-gc-tuning.md` | not started |
| 6.1 Largest heap objects | `06-1-heap-objects.md` | not started |
| 6.2 Memory leaks | `06-2-memory-leaks.md` | not started |
| 2.x Workflow group | `02-workflow-group.md` | not started |
| 3 + 3.1 Toolbox & profiling | `03-toolbox-profiling.md` | not started |
| 3.2–3.4 CPU/Disk/Network | `03-2-4-cpu-disk-network.md` | not started |
| 1 + Overview | `01-overview.md` | not started |
| Lab & prerequisite slides | `99-lab-slides.md` | not started |
