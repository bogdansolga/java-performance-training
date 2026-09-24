# Plan A — Deck Modernization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.
>
> **HUMAN GATE:** Most tasks contain a step marked **⛔ GATE**. Execution STOPS there until the trainer approves the change document. Never apply a deck edit that has not been approved.

**Goal:** Bring all ~19 Google Slides decks of the Java performance training to factual correctness for Java 11, 17, 21 and 25, reduce the over-long sections, expand the profiling deck, and replace deck 7.1's dated GlassFish screenshots with generated diagrams.

**Architecture:** Every deck is handled in two halves separated by a human gate. First a *change document* is produced — a table of proposed edits, each anchored to an exact literal substring read live from the deck and each factual claim carrying a source URL. The anchors are machine-verified against the live deck, which is this plan's substitute for a test suite. After trainer approval, the approved rows are applied via `gslides.sh personal replace`, and the deck is re-read to confirm each anchor is gone and each replacement is present.

**Tech Stack:** `gslides.sh` (bundled with the `nix:google` plugin skill, `personal` profile), bash, SVG authoring by hand, `rsvg-convert` or `sips` for PNG export.

**Spec:** `docs/superpowers/specs/2026-09-22-java-perf-labs-and-deck-refresh-design.md`

## Global Constraints

- **Covered releases:** Java **11, 17, 21, 25**. 11 is deck coverage only, never a lab runtime.
- **Narration baseline:** decks are narrated against **21**.
- **Version badge:** appears **only where the difference is major** (changed default, removed flag, new collector mode). It lists **only the releases where the claim holds**, e.g. `[17 · 21 · 25]` or `[11 · 17]`. Plain text, no colour, no full-set form — so badges are fully scriptable and need no manual styling pass.
- **Sources:** every `correction` and `addition` row carries a source URL (JEP, release notes, vendor docs).
- **Lab intro slides describe the issue, never the fix.**
- **No commits.** All work stays in the working tree for trainer review. Do not run `git commit` or `git push`.
- **Tool invocation:** `S="/Users/bogdan/.claude/plugins/cache/nix-config/nix/1.0.2/scripts/gslides.sh"` then `"$S" personal <cmd> <PRESENTATION_ID>`. Always the `personal` profile, never `nix`.
- **Anchors must be SINGLE-LINE substrings.** `gslides.sh text` emits a text box across multiple output lines, so a phrase that reads as one sentence on the slide is often split. `deck-check.sh` greps line by line, so a multi-line anchor can never match. Discovered in Task 1: the deck 5.1 phrase "Enabled by default since JDK 11" is split as `Enabled by default since` / `JDK 11`. **Every anchor quoted in this plan is a paraphrase from a summarizing reader and must be re-derived from a live `gslides.sh text` read before use** — pick the shortest substring that is unique within that deck and sits entirely on one output line. Verify with `grep -c -F -- "<anchor>" /tmp/deck-N.txt` returning exactly `1`.
- **Diagram house style:** 1280×720 viewBox, white ground, 22px/700 title `#1a1a1a`, 13px subtitle `#5e6680`, rounded containers `rx="14"`, 1px `#1a1a1a` strokes, secondary text `#5e6680`, one footer takeaway line.

## Deck ID Reference

| Deck | Presentation ID |
|---|---|
| Training overview | `1LbaJWLitcqeruFJaQ4dMkqWANdoKT0uiw2LTXxRAxeM` |
| 1. Perf management overview | `1wpiNHmcXNkXEmwF09xJS9bv4GAex6IQxiBzt_m0X15c` |
| 2. Improvements workflow | `10JpeuGMWWIKGsWBicQJwDwekBpDw-V2T83tPNSBbw8k` |
| 2.1 Test the real application | `1hft4X0terH2e7xcHIwiXSXc9FRxnkMtUesYdny80PoI` |
| 2.2 Throughput / response times | `1VN5hu2HWR25jmBczD4Vmugey5BIBd9ha_KDnKK968wA` |
| 2.3 Variability | `172hds2xzS_FOmA9674KBJRICOVCK2x-50R14-MF0g5Q` |
| 2.4 Test early, test often | `1y6l5OwUifzcdmI1WP7DidMg4h7aqs0lu9YTMYrkOiTc` |
| 3. Performance toolbox | `188yqDAwSq3oRqHIGOQprYPSezctdnM7VPBZAkxQgCYY` |
| 3.1 Execution profiling | `1sagmLntUl2W-3fkbL5_f_K15FL30cAy4iR1S_B8bVeg` |
| 3.2 CPU usage | `1_aLl1Zk0_Omh8y2LEmn0nMhZV8-EV03uBC8Hu6dcppM` |
| 3.3 Disk usage | `1RC2Hfv9UuUcmqdIooE1k80G4nFZtBP4nxDAHxVzt3vA` |
| 3.4 Network usage | `1w2U0gboZzFfo3IiNw2NU1SL2y724u03fakGaYQksYn4` |
| 4.1 Infra/arch/code improvements | `1QCPKhQt46v04nDqR-it6fJKDQR28k2xVANOaL0arcj4` |
| 4.2 JIT compiler | `1aLuZ5zUYFn-IeoHERu4HdraXmSG3-XP_C6vKI2_oG0o` |
| 5.1 Intro to GC | `1Y2xjPThug1WM2haytVv8M64tQDAIkI8TFl5CO4FU2lc` |
| 5.2 Choosing a GC algorithm | `1KfSLKXIGKmMRiqCeNYvR_-ZrSR8spcY5smiqvQrqb_k` |
| 5.3 Basic GC tuning | `128eswOqor8syZCg6LLBZAsAjxXcel-oW-EUuBLutRto` |
| 6.1 Largest heap objects | `1Rb3DA-XzvBavGVLNjGbIjv5zKwRsNdvXb_caRmk4Zds` |
| 6.2 Memory leaks | `1klwuS0Hk6atCBY6XVfQDHMz4icJHQVQ224Vc6v_rkAo` |
| 7.1 Monitoring & profiling tools | `1952R9NhvuYNuG9TujMfEPpy_w6cZ_yfMvl5zlZb9yoU` |
| 7.2 JMC & JFR | `1aA455d_wqeDuDrmLNeCCKr7p9W2wJHvY879YJv0vHrw` |

## File Structure

| File | Responsibility |
|---|---|
| `scripts/deck-check.sh` | Verifies every anchor in a change doc still exists in the live deck. The plan's test harness |
| `scripts/deck-apply.sh` | Applies approved rows from one change doc via `gslides.sh replace`, then re-verifies |
| `docs/deck-changes/README.md` | Index of change docs and the row format |
| `docs/deck-changes/NN-<slug>.md` | One change doc per deck — the reviewable proposal |
| `docs/diagrams/*.svg` | Generated diagram sources for deck 7.1 |
| `docs/diagrams/*.png` | Exported PNGs uploaded to the deck |
| `docs/deck-changes/CHANGE-REPORT.md` | Post-application report: slides changed and what changed |
| `docs/CODE-CHANGE-REPORT.md` | Post-application report: code and files changed |

**Change doc row format** (refines spec §6.2 — adds the `Anchor` column, which is what makes verification mechanical):

| Slide | Anchor | Current context | Proposed text | Category | Source |
|---|---|---|---|---|---|

`Anchor` is an exact literal substring present in the live deck, unique within that deck, and is the string passed to `gslides.sh replace`. `Current context` is human-readable surrounding text for the reviewer.

---

### Task 1: Verification harness

**Files:**
- Create: `scripts/deck-check.sh`
- Create: `scripts/deck-apply.sh`
- Create: `docs/deck-changes/README.md`
- Test: `docs/deck-changes/00-harness-selftest.md`

**Interfaces:**
- Consumes: nothing.
- Produces: `deck-check.sh <change-doc> <presentation-id>` exits 0 when every anchor is found, 1 with a report of missing anchors. `deck-apply.sh <change-doc> <presentation-id>` applies approved rows and re-verifies. Every later task calls both.

- [ ] **Step 1: Write the failing test — a self-test change doc with one anchor that exists and one that does not**

Create `docs/deck-changes/00-harness-selftest.md`:

```markdown
# Harness self-test (deck 5.1)

| Slide | Anchor | Current context | Proposed text | Category | Source |
|---|---|---|---|---|---|
| 22 | Enabled default since JDK 11 | G1 collector continued | Enabled by default since JDK 9 | correction | https://openjdk.org/jeps/248 |
| 99 | ZZZ_THIS_ANCHOR_DOES_NOT_EXIST | deliberate failure | n/a | correction | n/a |
```

- [ ] **Step 2: Run the check and verify it fails**

```bash
./scripts/deck-check.sh docs/deck-changes/00-harness-selftest.md 1Y2xjPThug1WM2haytVv8M64tQDAIkI8TFl5CO4FU2lc
```

Expected: exit code 1, output naming `ZZZ_THIS_ANCHOR_DOES_NOT_EXIST` as missing and the JDK 11 anchor as found.

- [ ] **Step 3: Write `scripts/deck-check.sh`**

```bash
#!/usr/bin/env bash
#
# deck-check.sh — verify every anchor in a change doc exists in the live deck.
#
# Usage: ./scripts/deck-check.sh <change-doc.md> <presentation-id>
#
# Exits 0 if every anchor is found exactly once, 1 otherwise.
set -uo pipefail

DOC="${1:?usage: deck-check.sh <change-doc.md> <presentation-id>}"
PID="${2:?usage: deck-check.sh <change-doc.md> <presentation-id>}"
GSLIDES="/Users/bogdan/.claude/plugins/cache/nix-config/nix/1.0.2/scripts/gslides.sh"

[ -f "$DOC" ] || { echo "Error: change doc not found: $DOC" >&2; exit 1; }

DECK_TEXT="$(mktemp)"
trap 'rm -f "$DECK_TEXT"' EXIT
"$GSLIDES" personal text "$PID" > "$DECK_TEXT" 2>/dev/null \
    || { echo "Error: could not read deck $PID" >&2; exit 1; }

rc=0
found=0
missing=0

# Table rows only: start with '|', skip header and separator rows.
while IFS= read -r line; do
    case "$line" in
        '|'*) ;;
        *) continue ;;
    esac
    case "$line" in
        *'---'*) continue ;;
        '| Slide '*) continue ;;
    esac

    anchor="$(printf '%s' "$line" | awk -F'|' '{print $3}' \
              | sed 's/^[[:space:]]*//; s/[[:space:]]*$//')"
    [ -n "$anchor" ] || continue
    [ "$anchor" = "Anchor" ] && continue

    count="$(grep -F -c -- "$anchor" "$DECK_TEXT" || true)"
    if [ "$count" -eq 0 ]; then
        echo "MISSING  $anchor"
        missing=$((missing + 1))
        rc=1
    elif [ "$count" -gt 1 ]; then
        echo "AMBIGUOUS ($count hits)  $anchor"
        missing=$((missing + 1))
        rc=1
    else
        echo "ok       $anchor"
        found=$((found + 1))
    fi
done < "$DOC"

echo
echo "$found anchor(s) verified, $missing problem(s)."
exit $rc
```

Make it executable: `chmod +x scripts/deck-check.sh`

- [ ] **Step 4: Run the check again and verify it reports exactly one failure**

```bash
./scripts/deck-check.sh docs/deck-changes/00-harness-selftest.md 1Y2xjPThug1WM2haytVv8M64tQDAIkI8TFl5CO4FU2lc; echo "exit=$?"
```

Expected: `ok` for the JDK 11 anchor, `MISSING` for `ZZZ_THIS_ANCHOR_DOES_NOT_EXIST`, `exit=1`.

If the JDK 11 anchor reports `MISSING`, the deck's live wording differs from what was read during design — re-read the deck with `"$S" personal text` and correct the anchor before proceeding. That is the harness working as intended.

- [ ] **Step 5: Write `scripts/deck-apply.sh`**

```bash
#!/usr/bin/env bash
#
# deck-apply.sh — apply approved rows from a change doc to the live deck.
#
# Usage: ./scripts/deck-apply.sh <change-doc.md> <presentation-id> [--dry-run]
#
# Only rows whose Category column does NOT contain 'skip' are applied.
# Re-verifies after applying: every anchor must be GONE, every replacement PRESENT.
set -uo pipefail

DOC="${1:?usage: deck-apply.sh <change-doc.md> <presentation-id> [--dry-run]}"
PID="${2:?usage: deck-apply.sh <change-doc.md> <presentation-id> [--dry-run]}"
DRY="${3:-}"
GSLIDES="/Users/bogdan/.claude/plugins/cache/nix-config/nix/1.0.2/scripts/gslides.sh"

applied=0
while IFS= read -r line; do
    case "$line" in '|'*) ;; *) continue ;; esac
    case "$line" in *'---'*) continue ;; '| Slide '*) continue ;; esac

    anchor="$(printf '%s' "$line" | awk -F'|' '{print $3}' | sed 's/^[[:space:]]*//; s/[[:space:]]*$//')"
    repl="$(printf '%s' "$line" | awk -F'|' '{print $5}' | sed 's/^[[:space:]]*//; s/[[:space:]]*$//')"
    cat="$(printf '%s' "$line" | awk -F'|' '{print $6}' | sed 's/^[[:space:]]*//; s/[[:space:]]*$//')"

    [ -n "$anchor" ] || continue
    [ "$anchor" = "Anchor" ] && continue
    case "$cat" in *skip*) echo "skip     $anchor"; continue ;; esac
    [ -n "$repl" ] && [ "$repl" != "n/a" ] || { echo "skip     $anchor (no replacement)"; continue; }

    if [ "$DRY" = "--dry-run" ]; then
        echo "would replace: '$anchor' -> '$repl'"
    else
        "$GSLIDES" personal replace "$PID" "$anchor" "$repl" >/dev/null
        echo "applied  $anchor -> $repl"
        applied=$((applied + 1))
    fi
done < "$DOC"

[ "$DRY" = "--dry-run" ] && { echo "dry run, nothing applied."; exit 0; }

echo
echo "$applied row(s) applied. Re-reading deck to verify..."
DECK_TEXT="$(mktemp)"; trap 'rm -f "$DECK_TEXT"' EXIT
"$GSLIDES" personal text "$PID" > "$DECK_TEXT" 2>/dev/null

rc=0
while IFS= read -r line; do
    case "$line" in '|'*) ;; *) continue ;; esac
    case "$line" in *'---'*) continue ;; '| Slide '*) continue ;; esac
    anchor="$(printf '%s' "$line" | awk -F'|' '{print $3}' | sed 's/^[[:space:]]*//; s/[[:space:]]*$//')"
    repl="$(printf '%s' "$line" | awk -F'|' '{print $5}' | sed 's/^[[:space:]]*//; s/[[:space:]]*$//')"
    cat="$(printf '%s' "$line" | awk -F'|' '{print $6}' | sed 's/^[[:space:]]*//; s/[[:space:]]*$//')"
    [ -n "$anchor" ] && [ "$anchor" != "Anchor" ] || continue
    case "$cat" in *skip*) continue ;; esac
    [ -n "$repl" ] && [ "$repl" != "n/a" ] || continue

    if grep -F -q -- "$anchor" "$DECK_TEXT"; then
        echo "FAIL  old text still present: $anchor"; rc=1
    fi
    if ! grep -F -q -- "$repl" "$DECK_TEXT"; then
        echo "FAIL  new text not found: $repl"; rc=1
    fi
done < "$DOC"

[ $rc -eq 0 ] && echo "Verified: all replacements present, all anchors gone."
exit $rc
```

Make it executable: `chmod +x scripts/deck-apply.sh`

- [ ] **Step 6: Verify apply is safe in dry-run mode**

```bash
./scripts/deck-apply.sh docs/deck-changes/00-harness-selftest.md 1Y2xjPThug1WM2haytVv8M64tQDAIkI8TFl5CO4FU2lc --dry-run
```

Expected: prints `would replace: 'Enabled default since JDK 11' -> 'Enabled by default since JDK 9'`, applies nothing, exit 0. Confirm the deck is untouched by re-reading slide 22.

- [ ] **Step 7: Write `docs/deck-changes/README.md`**

```markdown
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
```

- [ ] **Step 8: Delete the self-test doc**

```bash
rm docs/deck-changes/00-harness-selftest.md
```

The harness is proven; the self-test doc must not be mistaken for a real proposal.

---

### Task 2: Deck 5.1 — Intro to Garbage Collection

Highest-risk deck: the most factual errors, all already researched. 44 slides.

**Files:**
- Create: `docs/deck-changes/05-1-gc-intro.md`
- Modify: `docs/deck-changes/README.md` (status column)

**Interfaces:**
- Consumes: `deck-check.sh`, `deck-apply.sh` from Task 1.
- Produces: an approved-and-applied deck 5.1. No code interface.

- [ ] **Step 1: Read the live deck and capture exact wording**

```bash
S="/Users/bogdan/.claude/plugins/cache/nix-config/nix/1.0.2/scripts/gslides.sh"
"$S" personal text 1Y2xjPThug1WM2haytVv8M64tQDAIkI8TFl5CO4FU2lc > /tmp/deck-5-1.txt
wc -l /tmp/deck-5-1.txt
```

Anchors MUST be copied from this output verbatim. Do not retype from the spec — the spec's quotes are paraphrases captured through a summarizing reader.

- [ ] **Step 2: Write `docs/deck-changes/05-1-gc-intro.md`**

Table rows to include, with anchors adjusted to the exact strings found in Step 1:

| Slide | Anchor | Current context | Proposed text | Category | Source |
|---|---|---|---|---|---|
| 22 | Enabled default since JDK 11 | G1 collector, continued | Enabled by default since JDK 9 | correction | https://openjdk.org/jeps/248 |
| 18 | no longer available since Java 17 | CMS deprecation note | removed in JDK 14 | correction | https://openjdk.org/jeps/363 |
| 20 | Deprecated since Java 9, no longer available since Java 17 | CMS collector, continued | Deprecated in JDK 9, removed in JDK 14 | correction | https://openjdk.org/jeps/363 |
| 24 | mainstream since JDK 15 | ZGC collector | production-ready since JDK 15; generational mode is the default since JDK 23 | correction | https://openjdk.org/jeps/474 |
| 38 | ZGC collector - scalable & low latency GC, very low & predictable pause times | Summary, continued | ZGC - scalable low-latency collector; generational since JDK 21, default mode since JDK 23, non-generational removed in JDK 24 | correction | https://openjdk.org/jeps/490 |
| 27 | introduced in JDK 12 | Shenandoah GC | introduced in JDK 12, production-ready since JDK 15 | correction | https://openjdk.org/jeps/404 |
| 37 | CMS - concurrently collects old generation, while app threads running | Summary slide | (removed - CMS no longer exists on any covered release) | removal | https://openjdk.org/jeps/363 |

Add two `addition` rows:

| Slide | Anchor | Current context | Proposed text | Category | Source |
|---|---|---|---|---|---|
| 27 | Shenandoah been around longer than ZGC | Shenandoah, continued | Shenandoah has a generational mode since JDK 24 (-XX:ShenandoahGCMode=generational) | addition | https://openjdk.org/jeps/521 |
| 39 | Collectors overview | Collectors overview slide | Collectors overview — availability by release: 11 · 17 · 21 · 25 | addition | https://openjdk.org/jeps/474 |

- [ ] **Step 3: Verify every anchor exists**

```bash
./scripts/deck-check.sh docs/deck-changes/05-1-gc-intro.md 1Y2xjPThug1WM2haytVv8M64tQDAIkI8TFl5CO4FU2lc
```

Expected: exit 0, every row `ok`. Any `MISSING` or `AMBIGUOUS` means the anchor is wrong — fix it against `/tmp/deck-5-1.txt` and re-run until clean.

- [ ] **Step 4: ⛔ GATE — trainer reviews `docs/deck-changes/05-1-gc-intro.md`**

STOP. Report to the trainer that the change doc is ready, state the row count, and wait. Rejected rows come back with `skip` added to the Category column. Do not proceed without explicit approval.

- [ ] **Step 5: Dry-run the application**

```bash
./scripts/deck-apply.sh docs/deck-changes/05-1-gc-intro.md 1Y2xjPThug1WM2haytVv8M64tQDAIkI8TFl5CO4FU2lc --dry-run
```

Expected: one `would replace` line per approved row, none for `skip` rows.

- [ ] **Step 6: Apply and verify**

```bash
./scripts/deck-apply.sh docs/deck-changes/05-1-gc-intro.md 1Y2xjPThug1WM2haytVv8M64tQDAIkI8TFl5CO4FU2lc
```

Expected: `Verified: all replacements present, all anchors gone.` and exit 0. On FAIL, re-read the deck and reconcile before moving on.

- [ ] **Step 7: Update the index**

In `docs/deck-changes/README.md`, change deck 5.1's status from `not started` to `applied YYYY-MM-DD`.

---

### Task 3: Deck 4.2 — JIT compiler

The split 32-bit treatment from spec §6.3, plus AOT additions. 30 slides.

**Files:**
- Create: `docs/deck-changes/04-2-jit.md`
- Modify: `docs/deck-changes/README.md`

**Interfaces:**
- Consumes: `deck-check.sh`, `deck-apply.sh`.
- Produces: applied deck 4.2.

- [ ] **Step 1: Read the live deck**

```bash
S="/Users/bogdan/.claude/plugins/cache/nix-config/nix/1.0.2/scripts/gslides.sh"
"$S" personal text 1aLuZ5zUYFn-IeoHERu4HdraXmSG3-XP_C6vKI2_oG0o > /tmp/deck-4-2.txt
```

- [ ] **Step 2: Write `docs/deck-changes/04-2-jit.md`**

The 32-bit block splits into two treatments. Port history is **retained and scoped**; the flags are **marked dead since 9**.

| Slide | Anchor | Current context | Proposed text | Category | Source |
|---|---|---|---|---|---|
| 13 | If heap size less than ~3 GB | Java & JIT compiler versions | [11 · 17 only] If heap size is less than ~3 GB | correction | https://openjdk.org/jeps/479 |
| 15 | may run 5-20% faster in a 32-bit JVM | benefits of 32-bit JVM | [11 · 17 only] historically ran 5-20% faster in a 32-bit JVM | correction | https://openjdk.org/jeps/479 |
| 13 | A 32-bit client version | three versions of the JIT compiler | Dead since JDK 9: -d64 was removed and -client is a no-op on any 64-bit JVM | correction | https://openjdk.org/jeps/479 |
| 8 | Names - command-line arg used select it | client or server compiler | Historical only. Since JDK 9 the JVM selects tiered C1+C2 automatically | correction | https://openjdk.org/jeps/165 |
| 16 | switch -client , -server or -XX:+TieredCompilation | selecting the compiler | Tiered compilation is the default; -client and -server no longer select a compiler | correction | https://openjdk.org/jeps/165 |

Additions — AOT, which is what gives this deck something current to teach:

| Slide | Anchor | Current context | Proposed text | Category | Source |
|---|---|---|---|---|---|
| 28 | Further information | closing links slide | AOT cache (JDK 24-25): -XX:AOTCacheOutput / -XX:AOTCache cut JIT warm-up | addition | https://openjdk.org/jeps/514 |
| 28 | Understanding JIT compiler | further information links | Ahead-of-Time Method Profiling (JEP 515) makes prior-run profiles available at startup | addition | https://openjdk.org/jeps/515 |

- [ ] **Step 3: Verify anchors**

```bash
./scripts/deck-check.sh docs/deck-changes/04-2-jit.md 1aLuZ5zUYFn-IeoHERu4HdraXmSG3-XP_C6vKI2_oG0o
```

Expected: exit 0.

- [ ] **Step 4: ⛔ GATE — trainer reviews `docs/deck-changes/04-2-jit.md`**

STOP. Call out explicitly that slides 8–15 are being *scoped*, not deleted, per the trainer's own revision to spec §6.3.

- [ ] **Step 5: Apply and verify**

```bash
./scripts/deck-apply.sh docs/deck-changes/04-2-jit.md 1aLuZ5zUYFn-IeoHERu4HdraXmSG3-XP_C6vKI2_oG0o
```

Expected: exit 0 with the verification line.

- [ ] **Step 6: Update the index**

Set deck 4.2's status to `applied YYYY-MM-DD` in `docs/deck-changes/README.md`.

---

### Task 4: Deck 7.1 diagrams — author and export

Three SVGs replacing six GlassFish screenshots. Authored and exported before any 7.1 text is touched, so the trainer sees the visuals first.

**Files:**
- Create: `docs/diagrams/7-1-01-sampling-vs-instrumenting.svg`
- Create: `docs/diagrams/7-1-02-safepoint-bias.svg`
- Create: `docs/diagrams/7-1-03-complementarity.svg`
- Create: `docs/diagrams/*.png` (exports)

**Interfaces:**
- Consumes: the house style in Global Constraints.
- Produces: three PNG files at 1280×720, referenced by Task 5.

- [ ] **Step 1: Confirm an SVG→PNG exporter is available**

```bash
which rsvg-convert || which inkscape || which sips
```

If none, install one: `brew install librsvg`. `sips` cannot rasterize SVG and is a fallback only for resizing PNGs.

- [ ] **Step 2: Author diagram 1 — what each mode records**

Create `docs/diagrams/7-1-01-sampling-vs-instrumenting.svg`:

```svg
<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1280 720" font-family="-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif">
  <rect width="1280" height="720" fill="#ffffff"/>
  <text x="40" y="40" font-size="22" font-weight="700" fill="#1a1a1a">What each profiling mode actually records</text>
  <text x="40" y="64" font-size="13" fill="#5e6680">Two ways of watching the same program - they do not collect the same data.</text>
  <rect x="40" y="88" width="1200" height="560" rx="16" fill="#cfe2f3" stroke="#1a1a1a" stroke-width="1"/>

  <rect x="100" y="130" width="490" height="500" rx="14" fill="#ffffff" stroke="#1a1a1a" stroke-width="1"/>
  <rect x="100" y="130" width="490" height="60" rx="14" fill="#d5f5e3"/>
  <rect x="100" y="174" width="490" height="16" fill="#d5f5e3"/>
  <text x="345" y="162" font-size="20" font-weight="700" fill="#0e6655" text-anchor="middle">SAMPLING</text>
  <text x="345" y="181" font-size="12" fill="#117a65" text-anchor="middle" font-style="italic">asks "where are you now?", periodically</text>

  <text x="140" y="236" font-size="16" font-weight="700" fill="#1a1a1a">A timer fires</text>
  <text x="140" y="258" font-size="13" fill="#5e6680">Every few milliseconds, nothing in between.</text>
  <line x1="130" y1="276" x2="560" y2="276" stroke="#1a1a1a" stroke-opacity="0.10"/>
  <text x="140" y="306" font-size="16" font-weight="700" fill="#1a1a1a">Each thread stack is read</text>
  <text x="140" y="328" font-size="13" fill="#5e6680">The top frame is recorded as "running".</text>
  <line x1="130" y1="346" x2="560" y2="346" stroke="#1a1a1a" stroke-opacity="0.10"/>
  <text x="140" y="376" font-size="16" font-weight="700" fill="#1a1a1a">The whole interval is attributed</text>
  <text x="140" y="398" font-size="13" fill="#5e6680">That method is credited with all the elapsed time.</text>
  <line x1="130" y1="416" x2="560" y2="416" stroke="#1a1a1a" stroke-opacity="0.10"/>
  <text x="140" y="446" font-size="16" font-weight="700" fill="#1a1a1a">The program is not modified</text>
  <text x="140" y="468" font-size="13" fill="#5e6680">No bytecode changes, so inlining is unaffected.</text>
  <line x1="130" y1="486" x2="560" y2="486" stroke="#1a1a1a" stroke-opacity="0.10"/>
  <text x="140" y="516" font-size="16" font-weight="700" fill="#1a1a1a">Overhead stays low</text>
  <text x="140" y="538" font-size="13" fill="#5e6680">Safe to leave running on a busy system.</text>
  <line x1="130" y1="556" x2="560" y2="556" stroke="#1a1a1a" stroke-opacity="0.10"/>
  <text x="140" y="586" font-size="16" font-weight="700" fill="#1a1a1a">Counts are unknown</text>
  <text x="140" y="608" font-size="13" fill="#5e6680">It never learns how often a method was called.</text>

  <rect x="690" y="130" width="490" height="500" rx="14" fill="#ffffff" stroke="#1a1a1a" stroke-width="1"/>
  <rect x="690" y="130" width="490" height="60" rx="14" fill="#e8daef"/>
  <rect x="690" y="174" width="490" height="16" fill="#e8daef"/>
  <text x="935" y="162" font-size="20" font-weight="700" fill="#4a235a" text-anchor="middle">INSTRUMENTING</text>
  <text x="935" y="181" font-size="12" fill="#5b2c6f" text-anchor="middle" font-style="italic">counts every entry and exit</text>

  <text x="730" y="236" font-size="16" font-weight="700" fill="#1a1a1a">Bytecode is rewritten on load</text>
  <text x="730" y="258" font-size="13" fill="#5e6680">Counters are inserted into each method.</text>
  <line x1="720" y1="276" x2="1150" y2="276" stroke="#1a1a1a" stroke-opacity="0.10"/>
  <text x="730" y="306" font-size="16" font-weight="700" fill="#1a1a1a">Every call is seen</text>
  <text x="730" y="328" font-size="13" fill="#5e6680">Nothing is sampled, nothing is missed.</text>
  <line x1="720" y1="346" x2="1150" y2="346" stroke="#1a1a1a" stroke-opacity="0.10"/>
  <text x="730" y="376" font-size="16" font-weight="700" fill="#1a1a1a">Invocation counts are exact</text>
  <text x="730" y="398" font-size="13" fill="#5e6680">4.7 million calls is a fact, not an estimate.</text>
  <line x1="720" y1="416" x2="1150" y2="416" stroke="#1a1a1a" stroke-opacity="0.10"/>
  <text x="730" y="446" font-size="16" font-weight="700" fill="#1a1a1a">The program is changed</text>
  <text x="730" y="468" font-size="13" fill="#5e6680">Small methods may no longer be inlined.</text>
  <line x1="720" y1="486" x2="1150" y2="486" stroke="#1a1a1a" stroke-opacity="0.10"/>
  <text x="730" y="516" font-size="16" font-weight="700" fill="#1a1a1a">Overhead is high</text>
  <text x="730" y="538" font-size="13" fill="#5e6680">Scope it to a few classes or packages.</text>
  <line x1="720" y1="556" x2="1150" y2="556" stroke="#1a1a1a" stroke-opacity="0.10"/>
  <text x="730" y="586" font-size="16" font-weight="700" fill="#1a1a1a">Timings shift</text>
  <text x="730" y="608" font-size="13" fill="#5e6680">The profile can overstate instrumented methods.</text>

  <text x="640" y="680" font-size="13" fill="#1a1a1a" text-anchor="middle">Sampling estimates where time goes. Instrumenting counts what happened. Neither answers the other's question.</text>
</svg>
```

- [ ] **Step 3: Author diagram 2 — safepoint bias**

Create `docs/diagrams/7-1-02-safepoint-bias.svg`. Same frame as diagram 1 (white ground, title strip at y=40/64, `#cfe2f3` container at y=88 height 560, footer at y=680), with this content instead of the two columns:

```svg
<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1280 720" font-family="-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif">
  <rect width="1280" height="720" fill="#ffffff"/>
  <text x="40" y="40" font-size="22" font-weight="700" fill="#1a1a1a">Safepoint bias: why a hot method can be invisible</text>
  <text x="40" y="64" font-size="13" fill="#5e6680">A sampler can only read a thread when that thread has reached a safepoint.</text>
  <rect x="40" y="88" width="1200" height="560" rx="16" fill="#cfe2f3" stroke="#1a1a1a" stroke-width="1"/>

  <rect x="90" y="130" width="1100" height="200" rx="14" fill="#ffffff" stroke="#1a1a1a" stroke-width="1"/>
  <text x="120" y="166" font-size="16" font-weight="700" fill="#1a1a1a">Thread timeline - where the thread really is</text>
  <rect x="120" y="190" width="200" height="44" rx="6" fill="#d5f5e3" stroke="#1a1a1a" stroke-width="1"/>
  <text x="220" y="217" font-size="13" fill="#0e6655" text-anchor="middle">methodA()</text>
  <rect x="320" y="190" width="300" height="44" rx="6" fill="#fdebd0" stroke="#1a1a1a" stroke-width="1"/>
  <text x="470" y="217" font-size="13" fill="#7e5109" text-anchor="middle">get() - no safepoint inside</text>
  <rect x="620" y="190" width="180" height="44" rx="6" fill="#d5f5e3" stroke="#1a1a1a" stroke-width="1"/>
  <text x="710" y="217" font-size="13" fill="#0e6655" text-anchor="middle">methodA()</text>
  <rect x="800" y="190" width="330" height="44" rx="6" fill="#fdebd0" stroke="#1a1a1a" stroke-width="1"/>
  <text x="965" y="217" font-size="13" fill="#7e5109" text-anchor="middle">get() - no safepoint inside</text>
  <text x="120" y="268" font-size="13" fill="#5e6680">Sampler ticks (only land where a safepoint exists):</text>
  <text x="240" y="300" font-size="20" fill="#1a1a1a" text-anchor="middle">&#9660;</text>
  <text x="660" y="300" font-size="20" fill="#1a1a1a" text-anchor="middle">&#9660;</text>
  <text x="770" y="300" font-size="20" fill="#1a1a1a" text-anchor="middle">&#9660;</text>

  <rect x="90" y="360" width="530" height="240" rx="14" fill="#ffffff" stroke="#1a1a1a" stroke-width="1"/>
  <rect x="90" y="360" width="530" height="54" rx="14" fill="#d5f5e3"/>
  <rect x="90" y="400" width="530" height="14" fill="#d5f5e3"/>
  <text x="355" y="393" font-size="18" font-weight="700" fill="#0e6655" text-anchor="middle">What the sampler reports</text>
  <text x="130" y="452" font-size="15" font-weight="700" fill="#1a1a1a">methodA() - 100%</text>
  <text x="130" y="476" font-size="13" fill="#5e6680">Every tick landed inside methodA.</text>
  <text x="130" y="516" font-size="15" font-weight="700" fill="#1a1a1a">get() - 0%</text>
  <text x="130" y="540" font-size="13" fill="#5e6680">Never sampled, so it does not appear at all.</text>
  <text x="130" y="576" font-size="13" fill="#7e5109" font-style="italic">The sampler is not wrong - it was never allowed to look.</text>

  <rect x="660" y="360" width="530" height="240" rx="14" fill="#ffffff" stroke="#1a1a1a" stroke-width="1"/>
  <rect x="660" y="360" width="530" height="54" rx="14" fill="#e8daef"/>
  <rect x="660" y="400" width="530" height="14" fill="#e8daef"/>
  <text x="925" y="393" font-size="18" font-weight="700" fill="#4a235a" text-anchor="middle">What instrumenting reports</text>
  <text x="700" y="452" font-size="15" font-weight="700" fill="#1a1a1a">get() - 12% of total time</text>
  <text x="700" y="476" font-size="13" fill="#5e6680">Called 4.7 million times.</text>
  <text x="700" y="516" font-size="15" font-weight="700" fill="#1a1a1a">methodA() - the remainder</text>
  <text x="700" y="540" font-size="13" fill="#5e6680">Fewer calls, more time in each.</text>
  <text x="700" y="576" font-size="13" fill="#4a235a" font-style="italic">The fix is call count, not a faster get().</text>

  <text x="640" y="680" font-size="13" fill="#1a1a1a" text-anchor="middle">async-profiler avoids this by sampling without waiting for a safepoint.</text>
</svg>
```

- [ ] **Step 4: Author diagram 3 — complementarity**

Create `docs/diagrams/7-1-03-complementarity.svg`. This is the diagram that must land; it states the deck's intent.

```svg
<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1280 720" font-family="-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif">
  <rect width="1280" height="720" fill="#ffffff"/>
  <text x="40" y="40" font-size="22" font-weight="700" fill="#1a1a1a">The same application, profiled two ways</text>
  <text x="40" y="64" font-size="13" fill="#5e6680">Different top methods is the expected result, not a contradiction to resolve.</text>
  <rect x="40" y="88" width="1200" height="560" rx="16" fill="#cfe2f3" stroke="#1a1a1a" stroke-width="1"/>

  <rect x="90" y="126" width="530" height="290" rx="14" fill="#ffffff" stroke="#1a1a1a" stroke-width="1"/>
  <rect x="90" y="126" width="530" height="54" rx="14" fill="#d5f5e3"/>
  <rect x="90" y="166" width="530" height="14" fill="#d5f5e3"/>
  <text x="355" y="159" font-size="18" font-weight="700" fill="#0e6655" text-anchor="middle">Sampling profile - top methods</text>
  <text x="120" y="212" font-size="14" fill="#1a1a1a">defineClass1()</text>
  <rect x="300" y="198" width="240" height="18" rx="4" fill="#0e6655"/>
  <text x="556" y="212" font-size="13" fill="#5e6680">19%</text>
  <text x="120" y="248" font-size="14" fill="#1a1a1a">getPackageSources()</text>
  <rect x="300" y="234" width="120" height="18" rx="4" fill="#0e6655" fill-opacity="0.75"/>
  <text x="556" y="248" font-size="13" fill="#5e6680">9%</text>
  <text x="120" y="284" font-size="14" fill="#1a1a1a">inflateBytes()</text>
  <rect x="300" y="270" width="70" height="18" rx="4" fill="#0e6655" fill-opacity="0.55"/>
  <text x="556" y="284" font-size="13" fill="#5e6680">5%</text>
  <text x="120" y="320" font-size="14" fill="#8a8f9e">get()</text>
  <text x="300" y="320" font-size="13" fill="#8a8f9e" font-style="italic">absent - never sampled</text>
  <text x="120" y="368" font-size="13" font-weight="700" fill="#0e6655">Trust it for: where wall-clock time goes</text>
  <text x="120" y="390" font-size="13" fill="#5e6680">Low overhead, safe in production, honest about cost.</text>

  <rect x="660" y="126" width="530" height="290" rx="14" fill="#ffffff" stroke="#1a1a1a" stroke-width="1"/>
  <rect x="660" y="126" width="530" height="54" rx="14" fill="#e8daef"/>
  <rect x="660" y="166" width="530" height="14" fill="#e8daef"/>
  <text x="925" y="159" font-size="18" font-weight="700" fill="#4a235a" text-anchor="middle">Instrumented profile - top methods</text>
  <text x="690" y="212" font-size="14" fill="#1a1a1a">getPackageSources()</text>
  <rect x="880" y="198" width="170" height="18" rx="4" fill="#4a235a"/>
  <text x="1066" y="212" font-size="13" fill="#5e6680">13%</text>
  <text x="690" y="248" font-size="14" fill="#1a1a1a">get()</text>
  <rect x="880" y="234" width="155" height="18" rx="4" fill="#4a235a" fill-opacity="0.8"/>
  <text x="1066" y="248" font-size="13" fill="#5e6680">12%</text>
  <text x="690" y="284" font-size="14" fill="#1a1a1a">defineClass1()</text>
  <rect x="880" y="270" width="50" height="18" rx="4" fill="#4a235a" fill-opacity="0.55"/>
  <text x="1066" y="284" font-size="13" fill="#5e6680">4%</text>
  <text x="690" y="320" font-size="13" fill="#4a235a">get() called 4,700,000 times</text>
  <text x="690" y="368" font-size="13" font-weight="700" fill="#4a235a">Trust it for: how often something is called</text>
  <text x="690" y="390" font-size="13" fill="#5e6680">Exact counts; timings distorted by the instrumentation.</text>

  <rect x="90" y="446" width="1100" height="150" rx="14" fill="#ffffff" stroke="#1a1a1a" stroke-width="1"/>
  <text x="120" y="482" font-size="16" font-weight="700" fill="#1a1a1a">Read them together</text>
  <text x="120" y="512" font-size="14" fill="#5e6680">Sampling says class loading dominates startup - so tune class loading.</text>
  <text x="120" y="540" font-size="14" fill="#5e6680">Instrumenting says get() runs 4.7 million times - so cut the call count, do not micro-optimise the body.</text>
  <text x="120" y="574" font-size="14" fill="#5e6680">Each profiler answers a question the other cannot. Run both before deciding what to change.</text>

  <text x="640" y="680" font-size="13" fill="#1a1a1a" text-anchor="middle">Profilers are estimators. Two disagreeing estimates tell you more than one confident one.</text>
</svg>
```

- [ ] **Step 5: Export all three to PNG**

```bash
cd docs/diagrams
for f in 7-1-01-sampling-vs-instrumenting 7-1-02-safepoint-bias 7-1-03-complementarity; do
    rsvg-convert -w 1280 -h 720 "$f.svg" -o "$f.png"
done
ls -la 7-1-*.png
```

Expected: three PNGs, each non-zero and 1280×720. Verify with `sips -g pixelWidth -g pixelHeight 7-1-01-sampling-vs-instrumenting.png`.

- [ ] **Step 6: Visually inspect every PNG**

Open each PNG and confirm: no clipped text, no overlapping boxes, all text legible at slide size, colours match the house style. Text overflow is the common failure and the exporter will not report it.

- [ ] **Step 7: ⛔ GATE — trainer reviews the three PNGs**

STOP. Show the trainer the three images before any 7.1 text is touched. Diagram 3 carries the stated intent; if it does not land, revise before proceeding.

---

### Task 5: Deck 7.1 — text, screenshot replacement, simplification

44 slides, the longest deck in the set.

**Files:**
- Create: `docs/deck-changes/07-1-profiling-tools.md`
- Modify: `docs/deck-changes/README.md`

**Interfaces:**
- Consumes: the three PNGs from Task 4; `deck-check.sh`, `deck-apply.sh`.
- Produces: applied deck 7.1.

- [ ] **Step 1: Read the live deck**

```bash
S="/Users/bogdan/.claude/plugins/cache/nix-config/nix/1.0.2/scripts/gslides.sh"
"$S" personal text 1952R9NhvuYNuG9TujMfEPpy_w6cZ_yfMvl5zlZb9yoU > /tmp/deck-7-1.txt
"$S" personal slides 1952R9NhvuYNuG9TujMfEPpy_w6cZ_yfMvl5zlZb9yoU
```

The second command lists slide objectIds, needed to place images.

- [ ] **Step 2: Write `docs/deck-changes/07-1-profiling-tools.md`**

Corrections:

| Slide | Anchor | Current context | Proposed text | Category | Source |
|---|---|---|---|---|---|
| 1 | jhat - reads helps analyse memory heap dumps | JDK tools overview | (removed - jhat was removed in JDK 9) | correction | https://openjdk.org/jeps/241 |
| 18 | Java Flight Recorder | paid profilers list | (move to the free list) | correction | https://openjdk.org/jeps/328 |
| 16 | Heap dump post-processing | heap dump slide | Heap dump post-processing - jcmd GC.heap_dump, then JMC or Eclipse MAT | correction | https://openjdk.org/jeps/241 |

Screenshot replacements — each is a `removal` of the GlassFish narration plus an image insert:

| Slide | Anchor | Current context | Proposed text | Category | Source |
|---|---|---|---|---|---|
| 24 | A basic sampling profile: startup GlassFish app server domain | sampling profile example | How sampling attributes time - see diagram | reduction | n/a |
| 27 | same GlassFish startup profiling, using instrumented mode | instrumented profilers | The same workload, instrumented - see diagram | reduction | n/a |
| 34 | GlassFish startup, using different instrumented profiling tool - NetBeans profiler | blocking methods | Blocked threads consume no CPU - park(), parkNanos(), read() | reduction | n/a |
| 38 | GlassFish startup profile, showed in Oracle Developer Studio profiling tool | native profiling | Native profilers show JVM-internal time - GC and compiler threads | reduction | n/a |
| 37 | GlassFish available few OS distributions | native profilers | async-profiler and perf provide native visibility on Linux and macOS | correction | https://github.com/async-profiler/async-profiler |

- [ ] **Step 3: Verify anchors**

```bash
./scripts/deck-check.sh docs/deck-changes/07-1-profiling-tools.md 1952R9NhvuYNuG9TujMfEPpy_w6cZ_yfMvl5zlZb9yoU
```

Expected: exit 0.

- [ ] **Step 4: ⛔ GATE — trainer reviews `docs/deck-changes/07-1-profiling-tools.md`**

STOP. Flag that this task also *shortens* the deck and name which slides collapse.

- [ ] **Step 5: Apply the text changes**

```bash
./scripts/deck-apply.sh docs/deck-changes/07-1-profiling-tools.md 1952R9NhvuYNuG9TujMfEPpy_w6cZ_yfMvl5zlZb9yoU
```

Expected: exit 0 with the verification line.

- [ ] **Step 6: Place the three PNGs on their slides**

`gslides.sh` has no image-insert verb, so images are placed manually. Report to the trainer:

- `7-1-01-sampling-vs-instrumenting.png` → slide 24, replacing the sampling screenshot
- `7-1-02-safepoint-bias.png` → slide 31, replacing the prose-only safepoint explanation
- `7-1-03-complementarity.png` → slide 29, replacing the sampled-vs-instrumented comparison

Delete the superseded GlassFish screenshots on slides 24, 27, 34, 38, 40 in the same pass.

- [ ] **Step 7: Re-read and confirm**

```bash
S="/Users/bogdan/.claude/plugins/cache/nix-config/nix/1.0.2/scripts/gslides.sh"
"$S" personal text 1952R9NhvuYNuG9TujMfEPpy_w6cZ_yfMvl5zlZb9yoU | grep -i -c glassfish
```

Expected: a small number, not necessarily `0`. The five rows in Step 2 cover the screenshot captions; slides 35, 39 and 40 carry further GlassFish prose that no row addresses. Print each remaining hit:

```bash
"$S" personal text 1952R9NhvuYNuG9TujMfEPpy_w6cZ_yfMvl5zlZb9yoU | grep -in glassfish
```

Add a `reduction` row for every hit, re-run Steps 3-5 for those rows, and only then continue. Do not leave a GlassFish reference pointing at a screenshot that no longer exists.

- [ ] **Step 8: Update the index**

Set deck 7.1's status to `applied YYYY-MM-DD`.

---

### Task 6: Decks 5.2 and 5.3 — GC choice and tuning

**Files:**
- Create: `docs/deck-changes/05-2-choosing-gc.md`
- Create: `docs/deck-changes/05-3-gc-tuning.md`
- Modify: `docs/deck-changes/README.md`

**Interfaces:**
- Consumes: `deck-check.sh`, `deck-apply.sh`.
- Produces: applied decks 5.2 and 5.3.

- [ ] **Step 1: Read both live decks**

```bash
S="/Users/bogdan/.claude/plugins/cache/nix-config/nix/1.0.2/scripts/gslides.sh"
"$S" personal text 1KfSLKXIGKmMRiqCeNYvR_-ZrSR8spcY5smiqvQrqb_k > /tmp/deck-5-2.txt
"$S" personal text 128eswOqor8syZCg6LLBZAsAjxXcel-oW-EUuBLutRto > /tmp/deck-5-3.txt
```

- [ ] **Step 2: Write `docs/deck-changes/05-2-choosing-gc.md`**

| Slide | Anchor | Current context | Proposed text | Category | Source |
|---|---|---|---|---|---|
| 7 | ZGC - latest GC added in the JVM | maturity considerations | ZGC - generational since JDK 21, default mode since JDK 23 | correction | https://openjdk.org/jeps/474 |
| 8 | ZGC:		designed to eliminate pause times | choosing the collector | ZGC: sub-millisecond pauses, generational by default since JDK 23 | correction | https://openjdk.org/jeps/474 |
| 1 | Serial collector (*) - best used when the app uses less than 100 MB | overview | Serial - small heaps and single-CPU containers; still the default on one-core cgroups | addition | https://openjdk.org/jeps/248 |

- [ ] **Step 3: Write `docs/deck-changes/05-3-gc-tuning.md`**

| Slide | Anchor | Current context | Proposed text | Category | Source |
|---|---|---|---|---|---|
| 21 | Until Java 7 - called Permgen | sizing Permgen/Metaspace | [11 · 17 · 21 · 25] Metaspace since Java 8; elastic since JDK 16, returning memory to the OS | addition | https://openjdk.org/jeps/387 |
| 22 | metaspace rarely needs be sized | sizing Metaspace | Since JDK 16 Metaspace returns unused memory to the OS, so sizing it is rarely needed | correction | https://openjdk.org/jeps/387 |
| 25 | -XX:+UseParallelOldGC | controlling parallelism | -XX:+UseParallelGC (UseParallelOldGC deprecated in JDK 14) | correction | https://openjdk.org/jeps/366 |
| 25 | -XX:+UseParNewGC | controlling parallelism | (removed - UseParNewGC went with CMS) | removal | https://openjdk.org/jeps/363 |
| 34 | Summary | summary slide | Compact object headers (-XX:+UseCompactObjectHeaders, product in JDK 25) cut heap by up to 22% | addition | https://openjdk.org/jeps/519 |

- [ ] **Step 4: Verify anchors for both**

```bash
./scripts/deck-check.sh docs/deck-changes/05-2-choosing-gc.md 1KfSLKXIGKmMRiqCeNYvR_-ZrSR8spcY5smiqvQrqb_k
./scripts/deck-check.sh docs/deck-changes/05-3-gc-tuning.md 128eswOqor8syZCg6LLBZAsAjxXcel-oW-EUuBLutRto
```

Expected: exit 0 for both.

- [ ] **Step 5: ⛔ GATE — trainer reviews both change docs**

STOP.

- [ ] **Step 6: Apply both and verify**

```bash
./scripts/deck-apply.sh docs/deck-changes/05-2-choosing-gc.md 1KfSLKXIGKmMRiqCeNYvR_-ZrSR8spcY5smiqvQrqb_k
./scripts/deck-apply.sh docs/deck-changes/05-3-gc-tuning.md 128eswOqor8syZCg6LLBZAsAjxXcel-oW-EUuBLutRto
```

Expected: exit 0 for both.

- [ ] **Step 7: Update the index**

---

### Task 6b: Deck 4.1 — Infrastructure, architecture and code improvements

Deck 4.1 was never read during design, so it has no anchors yet. It is also where Task 12 places lab 2's intro slide, so it cannot be skipped. Read first, derive rows second — the same approach Task 7 uses for deck 6.1.

**Files:**
- Create: `docs/deck-changes/04-1-improvements.md`
- Modify: `docs/deck-changes/README.md`

**Interfaces:**
- Consumes: `deck-check.sh`, `deck-apply.sh` from Task 1.
- Produces: applied deck 4.1, ready to receive lab 2's intro slide in Task 12.

- [ ] **Step 1: Read the live deck**

```bash
S="/Users/bogdan/.claude/plugins/cache/nix-config/nix/1.0.2/scripts/gslides.sh"
"$S" personal text 1QCPKhQt46v04nDqR-it6fJKDQR28k2xVANOaL0arcj4 > /tmp/deck-4-1.txt
wc -l /tmp/deck-4-1.txt
```

- [ ] **Step 2: Derive rows against this checklist**

Write `docs/deck-changes/04-1-improvements.md`. Go through `/tmp/deck-4-1.txt` and produce a row for each hit:

| Look for | Action | Source |
|---|---|---|
| Any JDK version claim | Check against 11 / 17 / 21 / 25; badge if the difference is major | the relevant JEP |
| Thread pool or concurrency advice | Add the virtual-threads qualifier — a blocking task no longer needs a platform thread on 21+ | https://openjdk.org/jeps/444 |
| Sizing advice keyed to host CPUs or RAM | Add the container qualifier — the JVM reads the cgroup quota | https://openjdk.org/jeps/343 |
| Caching or data-access advice | Connect to lab 2, which is the N+1 defect | n/a |
| Named tools, products or URLs | Verify each still exists; replace dead links | the live URL |
| Streams API performance claims | Deck 1 slide 2 flags Streams as a case where a language feature can cost performance — keep the two decks consistent | n/a |

If a checklist row finds nothing in this deck, record it in the change doc as a line reading `no hits: <checklist item>`. An empty result that was actually checked is useful; a silently skipped check is not.

- [ ] **Step 3: Verify anchors**

```bash
./scripts/deck-check.sh docs/deck-changes/04-1-improvements.md 1QCPKhQt46v04nDqR-it6fJKDQR28k2xVANOaL0arcj4
```

Expected: exit 0, every row `ok`.

- [ ] **Step 4: ⛔ GATE — trainer reviews `docs/deck-changes/04-1-improvements.md`**

STOP. State the deck's slide count and how many rows were derived, and list any checklist item that returned `no hits` — the trainer knows this deck and can say whether an empty result is right.

- [ ] **Step 5: Apply and verify**

```bash
./scripts/deck-apply.sh docs/deck-changes/04-1-improvements.md 1QCPKhQt46v04nDqR-it6fJKDQR28k2xVANOaL0arcj4
```

Expected: exit 0 with the verification line.

- [ ] **Step 6: Update the index**

Add a row for deck 4.1 to `docs/deck-changes/README.md` and set its status to `applied YYYY-MM-DD`.

---

### Task 7: Decks 6.1 and 6.2 — heap objects and memory leaks

**Files:**
- Create: `docs/deck-changes/06-1-heap-objects.md`
- Create: `docs/deck-changes/06-2-memory-leaks.md`
- Modify: `docs/deck-changes/README.md`

**Interfaces:**
- Consumes: `deck-check.sh`, `deck-apply.sh`.
- Produces: applied decks 6.1 and 6.2.

- [ ] **Step 1: Read both live decks**

```bash
S="/Users/bogdan/.claude/plugins/cache/nix-config/nix/1.0.2/scripts/gslides.sh"
"$S" personal text 1Rb3DA-XzvBavGVLNjGbIjv5zKwRsNdvXb_caRmk4Zds > /tmp/deck-6-1.txt
"$S" personal text 1klwuS0Hk6atCBY6XVfQDHMz4icJHQVQ224Vc6v_rkAo > /tmp/deck-6-2.txt
```

- [ ] **Step 2: Write `docs/deck-changes/06-1-heap-objects.md`**

Deck 6.1 was not read in full during design. Produce rows from the Step 1 output covering:

- any `jhat` reference (removed in JDK 9 — https://openjdk.org/jeps/241)
- an `addition` row for compact object headers, since object size is this deck's subject: `Compact object headers (JDK 25) shrink every object by 4-8 bytes` — https://openjdk.org/jeps/519
- an `addition` row pointing at lab 1: `Lab 1 - find the retained set in a live heap dump` (issue only, never the fix)

- [ ] **Step 3: Write `docs/deck-changes/06-2-memory-leaks.md`**

| Slide | Anchor | Current context | Proposed text | Category | Source |
|---|---|---|---|---|---|
| 17 | Use the latest LTS version of Java | avoiding memory leaks | Use a current LTS - 17, 21 or 25 | correction | https://www.oracle.com/java/technologies/java-se-support-roadmap.html |
| 15 | Using the finalize() method | finalize slide | finalize() is deprecated for removal since JDK 18 - use Cleaner | correction | https://openjdk.org/jeps/421 |
| 17 | Avoid using (/ implementing) the finalize() method | avoidance checklist | Never implement finalize() - deprecated for removal since JDK 18; use java.lang.ref.Cleaner | correction | https://openjdk.org/jeps/421 |

- [ ] **Step 4: Verify anchors for both**

```bash
./scripts/deck-check.sh docs/deck-changes/06-1-heap-objects.md 1Rb3DA-XzvBavGVLNjGbIjv5zKwRsNdvXb_caRmk4Zds
./scripts/deck-check.sh docs/deck-changes/06-2-memory-leaks.md 1klwuS0Hk6atCBY6XVfQDHMz4icJHQVQ224Vc6v_rkAo
```

Expected: exit 0 for both.

- [ ] **Step 5: ⛔ GATE — trainer reviews both**

STOP.

- [ ] **Step 6: Apply both and verify**

```bash
./scripts/deck-apply.sh docs/deck-changes/06-1-heap-objects.md 1Rb3DA-XzvBavGVLNjGbIjv5zKwRsNdvXb_caRmk4Zds
./scripts/deck-apply.sh docs/deck-changes/06-2-memory-leaks.md 1klwuS0Hk6atCBY6XVfQDHMz4icJHQVQ224Vc6v_rkAo
```

- [ ] **Step 7: Update the index**

---

### Task 8: Decks 2, 2.1–2.4 — review, refine, reduce

Spec §6.4. These decks are mostly *correct* — the work is shortening and connecting them to the labs, not fixing facts.

**Files:**
- Create: `docs/deck-changes/02-1.md`, `02-2.md`, `02-3.md`, `02-4.md` (one per deck)
- Modify: `docs/deck-changes/README.md`

**Interfaces:**
- Consumes: `deck-check.sh`, `deck-apply.sh`.
- Produces: applied decks 2, 2.1, 2.2, 2.3, 2.4.

- [ ] **Step 1: Read all five live decks**

```bash
S="/Users/bogdan/.claude/plugins/cache/nix-config/nix/1.0.2/scripts/gslides.sh"
"$S" personal text 10JpeuGMWWIKGsWBicQJwDwekBpDw-V2T83tPNSBbw8k > /tmp/deck-2.txt
"$S" personal text 1hft4X0terH2e7xcHIwiXSXc9FRxnkMtUesYdny80PoI > /tmp/deck-2-1.txt
"$S" personal text 1VN5hu2HWR25jmBczD4Vmugey5BIBd9ha_KDnKK968wA > /tmp/deck-2-2.txt
"$S" personal text 172hds2xzS_FOmA9674KBJRICOVCK2x-50R14-MF0g5Q > /tmp/deck-2-3.txt
"$S" personal text 1y6l5OwUifzcdmI1WP7DidMg4h7aqs0lu9YTMYrkOiTc > /tmp/deck-2-4.txt
```

- [ ] **Step 2: Write one change doc per deck**

One file each: `02-1.md`, `02-2.md`, `02-3.md`, `02-4.md`. `deck-check.sh` reads one presentation at a time, so a combined file would report every other deck's anchors as MISSING and bury real failures. Required rows:

**2.1** — expand JMH beyond a single link, and connect meso-benchmarks to the labs:

| Slide | Anchor | Current context | Proposed text | Category | Source |
|---|---|---|---|---|---|
| 7 | http://tutorials.jenkov.com/java-performance/jmh.html | JMH intro slide | JMH - the JDK's own microbenchmark harness: https://github.com/openjdk/jmh | correction | https://github.com/openjdk/jmh |
| 15 | Frequent benchmark - measuring response time REST request | meso-benchmarks | This is exactly what the course labs measure - a REST request under Gatling load | addition | n/a |

**2.2** — simplify, per the trainer's explicit request. Collapse the client-overload, average-vs-percentile and outlier slides:

| Slide | Anchor | Current context | Proposed text | Category | Source |
|---|---|---|---|---|---|
| 9 | Client-server tests : risk client cannot send data fast enough server | client overloading risk | If the load generator saturates first, you are measuring the generator | reduction | n/a |
| 16 | Performance testing focus - 90th% response time (usually) | outliers and percentiles | Report an average and at least one percentile - the pair catches outliers | reduction | n/a |

**2.3** — trim the statistics run, connect to the harness:

| Slide | Anchor | Current context | Proposed text | Category | Source |
|---|---|---|---|---|---|
| 8 | Further continued - if needed | t-test links slide | The lab harness runs each scenario repeatedly for this reason | addition | n/a |

**2.4** — modernize the CI framing off the waterfall release cycle:

| Slide | Anchor | Current context | Proposed text | Category | Source |
|---|---|---|---|---|---|
| 2 | A typical development cycle entails feature-freeze date | conflicting forces | In continuous delivery there is no feature freeze - regressions must be caught per merge request | correction | n/a |
| 10 | test ran on 2 or 4-cores machine behaves very differently | run on target system | Container CPU limits change this again - the JVM sees the cgroup quota, not the host | addition | https://openjdk.org/jeps/343 |

- [ ] **Step 3: Verify anchors, one deck at a time**

```bash
./scripts/deck-check.sh docs/deck-changes/02-1.md 1hft4X0terH2e7xcHIwiXSXc9FRxnkMtUesYdny80PoI
./scripts/deck-check.sh docs/deck-changes/02-2.md 1VN5hu2HWR25jmBczD4Vmugey5BIBd9ha_KDnKK968wA
./scripts/deck-check.sh docs/deck-changes/02-3.md 172hds2xzS_FOmA9674KBJRICOVCK2x-50R14-MF0g5Q
./scripts/deck-check.sh docs/deck-changes/02-4.md 1y6l5OwUifzcdmI1WP7DidMg4h7aqs0lu9YTMYrkOiTc
```

Expected: exit 0 for all four.

- [ ] **Step 4: ⛔ GATE — trainer reviews**

STOP. Name the slide count before and after for each deck, since reduction is the point.

- [ ] **Step 5: Apply per deck and verify**

```bash
./scripts/deck-apply.sh docs/deck-changes/02-1.md 1hft4X0terH2e7xcHIwiXSXc9FRxnkMtUesYdny80PoI
./scripts/deck-apply.sh docs/deck-changes/02-2.md 1VN5hu2HWR25jmBczD4Vmugey5BIBd9ha_KDnKK968wA
./scripts/deck-apply.sh docs/deck-changes/02-3.md 172hds2xzS_FOmA9674KBJRICOVCK2x-50R14-MF0g5Q
./scripts/deck-apply.sh docs/deck-changes/02-4.md 1y6l5OwUifzcdmI1WP7DidMg4h7aqs0lu9YTMYrkOiTc
```

- [ ] **Step 6: Update the index**

---

### Task 9: Decks 3 and 3.1 — toolbox and the profiling expansion

3.1 is three slides and is the one deck that grows. Spec §6.4.

**Files:**
- Create: `docs/deck-changes/03-toolbox-profiling.md`
- Modify: `docs/deck-changes/README.md`

**Interfaces:**
- Consumes: `deck-check.sh`, `deck-apply.sh`.
- Produces: applied decks 3 and 3.1.

- [ ] **Step 1: Read both live decks**

```bash
S="/Users/bogdan/.claude/plugins/cache/nix-config/nix/1.0.2/scripts/gslides.sh"
"$S" personal text 188yqDAwSq3oRqHIGOQprYPSezctdnM7VPBZAkxQgCYY > /tmp/deck-3.txt
"$S" personal text 1sagmLntUl2W-3fkbL5_f_K15FL30cAy4iR1S_B8bVeg > /tmp/deck-3-1.txt
```

- [ ] **Step 2: Write the deck 3 rows**

| Slide | Anchor | Current context | Proposed text | Category | Source |
|---|---|---|---|---|---|
| 2 | http://java.net | tools and utilities | https://openjdk.org | correction | https://openjdk.org |
| 3 | sar (System Accounting Report) tools: vmstat, iostat, prstat etc | OS tools | vmstat, iostat, ss and pidstat on Linux; Instruments on macOS | correction | n/a |

- [ ] **Step 3: Write the deck 3.1 expansion rows**

3.1 currently covers only in-code timing, AOP and P6Spy. Add the tooling every lab depends on. Each is an `addition` anchored to the single content slide:

| Slide | Anchor | Current context | Proposed text | Category | Source |
|---|---|---|---|---|---|
| 1 | Tracing the SQL and/or JPA database calls | profiling overview | JFR - always-on, sub-1% overhead, built into the JDK since 11 | addition | https://openjdk.org/jeps/328 |
| 1 | Hibernate Statistics | profiling overview | async-profiler - samples without waiting for a safepoint; produces flame graphs | addition | https://github.com/async-profiler/async-profiler |
| 1 | Spring StopWatch object | profiling overview | JMC - opens JFR recordings; jcmd JFR.start for headless capture | addition | https://openjdk.org/projects/jmc/ |

Because the deck has only three slides, most of this becomes *new slides* rather than replacements. Record those rows with category `addition` and an explicit note that they require manual slide creation — `gslides.sh` has `duplicate` but placing new content is manual.

- [ ] **Step 4: Verify anchors**

```bash
./scripts/deck-check.sh docs/deck-changes/03-toolbox-profiling.md 1sagmLntUl2W-3fkbL5_f_K15FL30cAy4iR1S_B8bVeg
```

- [ ] **Step 5: ⛔ GATE — trainer reviews**

STOP. This is the one deck that grows; confirm the intended final slide count before building it.

- [ ] **Step 6: Apply, create the new slides, verify**

```bash
./scripts/deck-apply.sh docs/deck-changes/03-toolbox-profiling.md 188yqDAwSq3oRqHIGOQprYPSezctdnM7VPBZAkxQgCYY
S="/Users/bogdan/.claude/plugins/cache/nix-config/nix/1.0.2/scripts/gslides.sh"
"$S" personal text 1sagmLntUl2W-3fkbL5_f_K15FL30cAy4iR1S_B8bVeg | grep -c "async-profiler"
```

Expected: at least `1`.

- [ ] **Step 7: Update the index**

---

### Task 10: Decks 3.2, 3.3, 3.4 — CPU, disk, network

Containers for 3.2; shorten 3.3 and 3.4. No labs for disk or network (spec §3).

**Files:**
- Create: `docs/deck-changes/03-2.md`, `03-3.md`, `03-4.md` (one per deck)
- Modify: `docs/deck-changes/README.md`

**Interfaces:**
- Consumes: `deck-check.sh`, `deck-apply.sh`.
- Produces: applied decks 3.2, 3.3, 3.4.

- [ ] **Step 1: Read all three live decks**

```bash
S="/Users/bogdan/.claude/plugins/cache/nix-config/nix/1.0.2/scripts/gslides.sh"
"$S" personal text 1_aLl1Zk0_Omh8y2LEmn0nMhZV8-EV03uBC8Hu6dcppM > /tmp/deck-3-2.txt
"$S" personal text 1RC2Hfv9UuUcmqdIooE1k80G4nFZtBP4nxDAHxVzt3vA > /tmp/deck-3-3.txt
"$S" personal text 1w2U0gboZzFfo3IiNw2NU1SL2y724u03fakGaYQksYn4 > /tmp/deck-3-4.txt
```

- [ ] **Step 2: Write `docs/deck-changes/03-2.md` — containers and virtual threads**

| Slide | Anchor | Current context | Proposed text | Category | Source |
|---|---|---|---|---|---|
| 9 | typical case - an app with fixed-size thread pool, running various tasks | multi-CPU multithreaded | With virtual threads (21+) a blocking task no longer holds a platform thread | addition | https://openjdk.org/jeps/444 |
| 10 | first step - size thread pool should increased | keep in mind | Before enlarging the pool, check whether the JVM sees the container's CPU quota | addition | https://openjdk.org/jeps/343 |
| 2 | goal in performance - driving CPU usage high possible short possible | performance tuning end-goal | Under a cgroup quota, availableProcessors() reports the limit, not the host's cores | addition | https://openjdk.org/jeps/343 |

- [ ] **Step 3: Write `docs/deck-changes/03-3.md` and `03-4.md` — reductions**

3.3 — modernize the storage assumptions and shorten:

| Slide | Anchor | Current context | Proposed text | Category | Source |
|---|---|---|---|---|---|
| 3 | disk is only 1.04% utilised - acceptable values depend on used disk type | iostat example | On NVMe, utilisation percentages mislead - watch await and queue depth | correction | n/a |

3.4 — `netstat` is superseded:

| Slide | Anchor | Current context | Proposed text | Category | Source |
|---|---|---|---|---|---|
| 2 | On *nix 		- the basic network monitoring tool is netstat | monitoring network traffic | On Linux use ss (netstat is deprecated); nicstat for per-interface utilisation | correction | n/a |

- [ ] **Step 4: Verify anchors per deck**

```bash
./scripts/deck-check.sh docs/deck-changes/03-2.md 1_aLl1Zk0_Omh8y2LEmn0nMhZV8-EV03uBC8Hu6dcppM
./scripts/deck-check.sh docs/deck-changes/03-3.md 1RC2Hfv9UuUcmqdIooE1k80G4nFZtBP4nxDAHxVzt3vA
./scripts/deck-check.sh docs/deck-changes/03-4.md 1w2U0gboZzFfo3IiNw2NU1SL2y724u03fakGaYQksYn4
```

- [ ] **Step 5: ⛔ GATE — trainer reviews**

STOP.

- [ ] **Step 6: Apply all three and verify**

```bash
./scripts/deck-apply.sh docs/deck-changes/03-2.md 1_aLl1Zk0_Omh8y2LEmn0nMhZV8-EV03uBC8Hu6dcppM
./scripts/deck-apply.sh docs/deck-changes/03-3.md 1RC2Hfv9UuUcmqdIooE1k80G4nFZtBP4nxDAHxVzt3vA
./scripts/deck-apply.sh docs/deck-changes/03-4.md 1w2U0gboZzFfo3IiNw2NU1SL2y724u03fakGaYQksYn4
```

- [ ] **Step 7: Update the index**

---

### Task 11: Deck 1 and the Training overview — ergonomics and the five-labs slide

**Files:**
- Create: `docs/deck-changes/01-overview.md`
- Modify: `docs/deck-changes/README.md`

**Interfaces:**
- Consumes: `deck-check.sh`, `deck-apply.sh`.
- Produces: applied deck 1 and the Training overview deck, including the five-labs slide.

- [ ] **Step 1: Read both live decks**

```bash
S="/Users/bogdan/.claude/plugins/cache/nix-config/nix/1.0.2/scripts/gslides.sh"
"$S" personal text 1wpiNHmcXNkXEmwF09xJS9bv4GAex6IQxiBzt_m0X15c > /tmp/deck-1.txt
"$S" personal text 1LbaJWLitcqeruFJaQ4dMkqWANdoKT0uiw2LTXxRAxeM > /tmp/deck-overview.txt
```

- [ ] **Step 2: Write the deck 1 ergonomics rows**

| Slide | Anchor | Current context | Proposed text | Category | Source |
|---|---|---|---|---|---|
| 4 | Client-class machines - any 32-bit JVM running on | Java ergonomics | Ergonomics today keys off available CPUs and memory - including container limits | correction | https://openjdk.org/jeps/343 |
| 4 | Ex: default Garbage Collector platform - determined by machine class | ergonomics | Default collector: Serial on a single-CPU cgroup, G1 otherwise | correction | https://openjdk.org/jeps/248 |
| 2 | Three new GCs (G1, ZGC & Shenandoah) | performance management status | G1 (default since 9), ZGC (generational, default mode since 23) and Shenandoah | correction | https://openjdk.org/jeps/474 |

- [ ] **Step 3: Write the Training overview five-labs slide row**

Per spec §6.6, placed immediately after the objectives slide (slide 2). Content for the new slide, describing issues only, never fixes:

```
The five labs

1. Unbounded retention   - the heap grows every cycle and never comes back
2. N+1 queries           - one page view, hundreds of statements
3. Lock contention       - throughput stops scaling with threads
4. GC mismatch           - the wrong collector for the workload
5. Code cache exhaustion - the JIT stops compiling, throughput collapses

Each lab: reproduce it, investigate it with real tools, fix it, measure before and after.
```

Record this as one `lab-slide` row with a note that it requires manual slide creation after the objectives slide.

- [ ] **Step 4: Verify anchors**

```bash
./scripts/deck-check.sh docs/deck-changes/01-overview.md 1wpiNHmcXNkXEmwF09xJS9bv4GAex6IQxiBzt_m0X15c
```

- [ ] **Step 5: ⛔ GATE — trainer reviews**

STOP. The five-labs slide is participant-facing on day one; confirm the wording.

- [ ] **Step 6: Apply, create the five-labs slide, verify**

```bash
./scripts/deck-apply.sh docs/deck-changes/01-overview.md 1wpiNHmcXNkXEmwF09xJS9bv4GAex6IQxiBzt_m0X15c
S="/Users/bogdan/.claude/plugins/cache/nix-config/nix/1.0.2/scripts/gslides.sh"
"$S" personal text 1LbaJWLitcqeruFJaQ4dMkqWANdoKT0uiw2LTXxRAxeM | grep -c "The five labs"
```

Expected: `1`.

- [ ] **Step 7: Update the index**

---

### Task 12: Lab intro slides and the prerequisites slide

Spec §6.6. One intro slide per owning deck, plus a prerequisites slide. Each describes the issue only (D4).

**Files:**
- Create: `docs/deck-changes/99-lab-slides.md`
- Modify: `docs/deck-changes/README.md`

**Interfaces:**
- Consumes: `deck-check.sh`; the lab catalogue from spec §7.1.
- Produces: five lab intro slides and one prerequisites slide. Referenced by Plan B, which builds the labs themselves.

- [ ] **Step 1: Write `docs/deck-changes/99-lab-slides.md`**

Five `lab-slide` rows, each naming its target deck and the content. No fixes appear anywhere.

**Lab 1 → deck 6.2 (`1klwuS0Hk6atCBY6XVfQDHMz4icJHQVQ224Vc6v_rkAo`):**

```
Lab 1 - Unbounded retention

Symptom:  heap occupancy after every full GC is higher than the last
Duration: 1 hour
You get:  a running app, a load script, and a heap dump
Find:     what is being retained, and which reference keeps it alive
```

**Lab 2 → deck 4.1 (`1QCPKhQt46v04nDqR-it6fJKDQR28k2xVANOaL0arcj4`):**

```
Lab 2 - N+1 queries

Symptom:  one request, hundreds of SQL statements
Duration: 1 hour
You get:  p6spy statement logs and a Gatling run
Find:     which access path multiplies the statement count
```

**Lab 3 → deck 3.2 (`1_aLl1Zk0_Omh8y2LEmn0nMhZV8-EV03uBC8Hu6dcppM`):**

The threading stretch deck has no ID in the course index, so lab 3 lands in 3.2, which already owns thread-pool sizing and receives the virtual-threads content.

```
Lab 3 - Lock contention

Symptom:  adding threads stops improving throughput
Duration: 1 hour
You get:  a JFR recording with Monitor Blocked events
Find:     which monitor serialises the work
```

**Lab 5 → deck 5.2 (`1KfSLKXIGKmMRiqCeNYvR_-ZrSR8spcY5smiqvQrqb_k`):**

```
Lab 5 - GC mismatch

Symptom:  p99 latency spikes that track GC pauses
Duration: 2 hours (1 hour with a single collector)
You get:  the same workload under Serial, G1 and ZGC
Find:     which collector fits this workload, and what it costs
```

**Lab 8 → deck 4.2 (`1aLuZ5zUYFn-IeoHERu4HdraXmSG3-XP_C6vKI2_oG0o`):**

```
Lab 8 - Code cache exhaustion

Symptom:  throughput collapses partway through the run and never recovers
Duration: 1 hour
You get:  JFR compilation events and the JVM's own warning
Find:     why the JIT stopped compiling
```

**Prerequisites slide → Training overview deck:**

```
Before day one

Required: JDK 17 and JDK 21 on your PATH
Optional: JDK 25 (used in the GC lab)
Also:     Git, and port 8080 free

Run ./scripts/preflight.sh (or preflight.ps1 on Windows) and bring the output.
Anything red is in docs/PREFLIGHT-TROUBLESHOOTING.md.
```

- [ ] **Step 2: Confirm no fix appears on any slide**

```bash
grep -iE "fix:|solution|answer|use .* instead|remove the" docs/deck-changes/99-lab-slides.md
```

Expected: no output. Any hit violates D4 and must be reworded before review.

- [ ] **Step 3: ⛔ GATE — trainer reviews all six slides**

STOP. These are the slides participants read before attempting each lab; wording matters most here.

- [ ] **Step 4: Create the six slides manually and verify**

```bash
S="/Users/bogdan/.claude/plugins/cache/nix-config/nix/1.0.2/scripts/gslides.sh"
for id in 1klwuS0Hk6atCBY6XVfQDHMz4icJHQVQ224Vc6v_rkAo \
          1QCPKhQt46v04nDqR-it6fJKDQR28k2xVANOaL0arcj4 \
          1_aLl1Zk0_Omh8y2LEmn0nMhZV8-EV03uBC8Hu6dcppM \
          1KfSLKXIGKmMRiqCeNYvR_-ZrSR8spcY5smiqvQrqb_k \
          1aLuZ5zUYFn-IeoHERu4HdraXmSG3-XP_C6vKI2_oG0o; do
    echo "--- $id"
    "$S" personal text "$id" | grep -c "Duration:"
done
```

Expected: `1` for each.

- [ ] **Step 5: Update the index**

---

### Task 13: Change reports

The two review documents requested by the trainer. Written last, from what actually changed.

**Files:**
- Create: `docs/deck-changes/CHANGE-REPORT.md`
- Create: `docs/CODE-CHANGE-REPORT.md`

**Interfaces:**
- Consumes: every change doc from Tasks 2–12.
- Produces: the trainer's review surface. Nothing depends on it.

- [ ] **Step 1: Generate the slide change report**

Write `docs/deck-changes/CHANGE-REPORT.md` with one section per deck, listing every applied row: slide number, what it said, what it says now, and why. Group by category so corrections can be reviewed separately from reductions. Include a summary table at the top:

| Deck | Corrections | Removals | Additions | Reductions | New slides |
|---|---|---|---|---|---|

Rows marked `skip` during review appear in a "Proposed but rejected" section at the end — the record is more useful with the rejections than without.

- [ ] **Step 2: Generate the code and files change report**

Write `docs/CODE-CHANGE-REPORT.md` covering everything Plan A touched on disk:

- `scripts/deck-check.sh` — new, verification harness
- `scripts/deck-apply.sh` — new, applies approved rows
- `docs/deck-changes/*.md` — new, one proposal per deck plus the index
- `docs/diagrams/7-1-*.svg` and `.png` — new, three diagrams replacing six screenshots

For each: what it is, why it exists, and how to run it. State explicitly that nothing was committed.

- [ ] **Step 3: Verify both reports against reality**

```bash
cd /Volumes/NVMe/Development/IdeaProjects/training/java-performance-training
git status --short
ls docs/deck-changes/ docs/diagrams/
```

Every untracked file must appear in one of the two reports. Any file in a report that does not exist on disk is a reporting error — fix it.

- [ ] **Step 4: ⛔ GATE — trainer reviews both reports**

STOP. This is the final review of Plan A.

---

## Self-Review

**Spec coverage.** Every section of spec §6 maps to a task: §6.1 badge convention → Global Constraints, applied per deck; §6.2 change-proposal process → Task 1; §6.3 corrections → Tasks 2, 3, 5, 6, 7, 9, 11; §6.4 the 2.x/3.x review → Tasks 8, 9, 10; §6.5 new content → Tasks 3, 6 (and virtual threads in Task 10); §6.6 lab slides → Tasks 11, 12; §6.7 deck 7.1 diagrams → Tasks 4, 5; §6.8 Java 11 differences → Tasks 6 and 7 rows.

**Known gaps, stated rather than hidden:**

1. **Deck 6.1, the "From customers to code" deck and deck 7.2 were never read in full** during design. Task 7 handles 6.1 by reading it first and deriving rows; Task 6b does the same for deck 4.1. Deck 7.2 was read and found accurate, so it has no task. The "From customers to code" deck is marked optional in the course index and is out of scope.
2. **`gslides.sh` has no image-insert or slide-create verb.** Tasks 5, 9, 11 and 12 all require manual work in the browser. These steps are marked but cannot be automated with the current tooling.
3. **The threading stretch deck has no ID** in the course index — it is listed as a bare bullet. Ruled during pre-flight: lab 3's intro slide goes to deck 3.2, which already owns thread-pool sizing and receives the virtual-threads content. It moves if a threading deck is later created.

**Type consistency.** `deck-check.sh` and `deck-apply.sh` both parse column 3 as Anchor and column 5 as Proposed text, matching the six-column format defined in File Structure and documented in `docs/deck-changes/README.md`. Task 8 and Task 10 split their multi-deck docs into per-deck files; the apply commands in those tasks use the split filenames.
