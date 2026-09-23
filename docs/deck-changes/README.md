# Deck change documents

One file per deck. Each row is one proposed edit.

| Column | Meaning |
|---|---|
| Slide | Slide index as `gslides.sh text` prints it (0-based) |
| Anchor | Exact literal substring from the live deck, unique within it. Passed to `gslides.sh replace` |
| Current context | Human-readable surrounding text, for the reviewer |
| Proposed text | Replacement string |
| Category | `correction`, `removal`, `addition`, `reduction`, `lab-slide`; add `skip` to reject a row |
| Source | URL backing the claim. Required for `correction` and `addition` |

## Workflow

1. `./scripts/deck-check.sh <doc> <id>` — every anchor must verify before review.
2. Trainer reviews, marking rejected rows by adding `skip` to the Category column.
3. `./scripts/deck-apply.sh <doc> <id>` — applies and re-verifies.

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
