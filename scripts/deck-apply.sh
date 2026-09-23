#!/usr/bin/env bash
#
# deck-apply.sh — apply approved rows from a change doc to the live deck.
#
# Usage: ./scripts/deck-apply.sh <change-doc.md> <presentation-id> [--dry-run]
#
# Only rows whose Category column does NOT contain 'skip' are applied.
#
# Before applying, the deck is read ONCE and EVERY non-skipped row is
# classified in a fail-closed pre-flight pass, using TWO signals: R = true
# occurrence count of the Proposed text, A = true occurrence count of the
# Anchor, E = expected occurrence count (an `xN` token in Category, default
# 1). A row is "append-style" when the Proposed text contains the Anchor as
# a substring (so the Anchor legitimately survives a correct edit);
# otherwise it is "plain-style". Every row is classified as exactly one of:
#
#   ALREADY APPLIED  plain:   R == E  AND  A == 0
#                     append:  R == E  AND  A == R
#   SAFE TO APPLY     plain:   A == E  AND  R == 0
#                     append:  A == E  AND  R == 0
#   UNSAFE            anything else
#
# Checking R alone (the old behaviour) is NOT sufficient: the Proposed text
# can coincidentally exist elsewhere in the deck while the real target is
# still unedited, which would falsely report "already" and skip a row that
# actually needs editing. And checking only "R <= E" for safe-to-apply (the
# earlier, gapped behaviour) is ALSO not sufficient: it never looked at A,
# so a plain-style row whose Anchor is not unique (A > E) would be reported
# safe and `replaceAllText` would silently rewrite every occurrence,
# including ones never intended as a target. On a Google Slides deck (no
# version history) that mutation is permanent and cannot be told apart from
# a correct edit until a human notices — the whole point of corroborating
# with A is to catch this before it happens, not after.
#
# THE FIX IS ALL-OR-NOTHING: every row is classified before any row is
# applied. If even one row is UNSAFE, the script prints why (naming A, R,
# E) for each such row and ABORTS with a non-zero exit WITHOUT applying
# ANYTHING AT ALL — including rows that are individually safe. A change
# document is the unit of the operation; applying half of it would leave
# the deck in a state no document describes. There is no flag to bypass
# this gate. This pre-flight pass is side-effect free and never calls
# `gslides.sh replace`; it behaves identically under --dry-run.
#
# Already-applied rows are reported (`already  <anchor>  (found R=.., ..)`)
# and never sent to `gslides.sh replace` — this keeps re-runs idempotent for
# append-style rows, where a naive re-apply would double the appended text.
#
# Re-verifies after applying, using the SAME corroborated rules:
#   - append-style rows: pass <=> R == E AND A == R.
#   - plain-style rows:  pass <=> R == E AND A == 0.
set -uo pipefail

DOC="${1:?usage: deck-apply.sh <change-doc.md> <presentation-id> [--dry-run]}"
PID="${2:?usage: deck-apply.sh <change-doc.md> <presentation-id> [--dry-run]}"
DRY="${3:-}"
GSLIDES="${GSLIDES:-/Users/bogdan/.claude/plugins/cache/nix-config/nix/1.0.2/scripts/gslides.sh}"

# Count true occurrences of a literal substring in a file (same method as
# deck-check.sh): `grep -o` prints one line per match, so `wc -l` gives the
# occurrence count, not the matching-line count. `grep -o` exits 1 on zero
# matches; that's fine here since the script runs under `set -uo pipefail`
# without `set -e`, and only the captured stdout (empty on no match) is used.
count_occurrences() {
    grep -o -F -- "$1" "$2" | wc -l | tr -d '[:space:]'
}

# Expected occurrence count from the Category column's `xN` token, default 1.
# The token must be an isolated field, not digits following any "x" inside a
# word (e.g. Category text "appendix2" must NOT parse as expected=2) — so it
# must sit at the start of the field or be preceded by whitespace/comma.
expected_count() {
    local tok
    tok="$(printf '%s' "$1" | grep -oE '(^|[[:space:],])x[0-9]+' | head -1)"
    if [ -n "$tok" ]; then
        printf '%s' "${tok##*x}"
    else
        printf '1'
    fi
}

# Read the deck once, up front, for the already-applied check (one network
# call for a ~1000-line deck, not one per row). Re-read after applying (below)
# for post-verify. A failed read must abort loudly — never be mistaken for
# "text not found" (which would make every anchor look absent/gone).
DECK_TEXT="$(mktemp)"; trap 'rm -f "$DECK_TEXT"' EXIT
"$GSLIDES" personal text "$PID" > "$DECK_TEXT" 2>/dev/null \
    || { echo "Error: could not read deck $PID" >&2; exit 1; }

# ---------------------------------------------------------------------
# Pass 1: pre-flight classification. Read-only against DECK_TEXT (already
# fetched above); never calls `gslides.sh replace`. Every non-skipped row is
# classified as ALREADY APPLIED, SAFE TO APPLY, or UNSAFE. If ANY row is
# UNSAFE, print why (naming A, R, E) for each one and abort non-zero before
# Pass 2 ever runs — nothing is applied, not even rows that are themselves
# safe. This is identical under --dry-run.
# ---------------------------------------------------------------------
had_unsafe=0
while IFS= read -r line; do
    case "$line" in '|'*) ;; *) continue ;; esac
    case "$line" in *'---'*) continue ;; '| Slide '*) continue ;; esac

    anchor="$(printf '%s' "$line" | awk -F'|' '{print $3}' | sed 's/^[[:space:]]*//; s/[[:space:]]*$//')"
    repl="$(printf '%s' "$line" | awk -F'|' '{print $5}' | sed 's/^[[:space:]]*//; s/[[:space:]]*$//')"
    cat="$(printf '%s' "$line" | awk -F'|' '{print $6}' | sed 's/^[[:space:]]*//; s/[[:space:]]*$//')"

    [ -n "$anchor" ] || continue
    [ "$anchor" = "Anchor" ] && continue
    case "$cat" in *skip*) continue ;; esac
    case "$cat" in *manual*) continue ;; esac
    [ -n "$repl" ] && [ "$repl" != "n/a" ] || continue

    expected="$(expected_count "$cat")"
    rcount="$(count_occurrences "$repl" "$DECK_TEXT")"
    acount="$(count_occurrences "$anchor" "$DECK_TEXT")"

    append_style=0
    case "$repl" in *"$anchor"*) append_style=1 ;; esac

    already=0
    if [ "$append_style" -eq 1 ]; then
        [ "$rcount" -eq "$expected" ] && [ "$acount" -eq "$rcount" ] && already=1
    else
        [ "$rcount" -eq "$expected" ] && [ "$acount" -eq 0 ] && already=1
    fi

    # SAFE TO APPLY, for BOTH plain- and append-style rows: the Anchor
    # occurs exactly the expected number of times and the Proposed text is
    # not present at all yet. This is the check that closes the gap: it
    # looks at A, not just R, so a plain-style row whose Anchor is not
    # unique (A > E) is UNSAFE, not silently accepted.
    safe=0
    if [ "$already" -ne 1 ]; then
        [ "$acount" -eq "$expected" ] && [ "$rcount" -eq 0 ] && safe=1
    fi

    if [ "$already" -eq 1 ] || [ "$safe" -eq 1 ]; then
        continue
    fi

    style="plain"
    [ "$append_style" -eq 1 ] && style="append"
    echo "UNSAFE  $anchor  ($style-style, found A=$acount, R=$rcount, expected=$expected)"
    if [ "$append_style" -eq 0 ] && [ "$acount" -gt "$expected" ]; then
        echo "        anchor is not unique: occurs $acount time(s) but this row expects $expected -- a whole-deck replace would also rewrite $((acount - expected)) unrelated location(s)."
    else
        echo "        neither already-applied (R==E,A==0 / R==E,A==R) nor safe-to-apply (A==E,R==0) holds."
    fi
    echo "        fix: re-derive the anchor so it is unique in the deck, or if changing every occurrence is genuinely intended, declare the true count with an xN token in Category."
    had_unsafe=1
done < "$DOC"

if [ "$had_unsafe" -ne 0 ]; then
    echo
    echo "Aborting: one or more rows are UNSAFE to apply (see above). Nothing was sent to the live deck -- zero replace calls were made, including for rows that are individually safe."
    echo "This change document is applied as one unit; applying part of it would leave the deck in a state no document describes. Fix the row(s) above and re-run."
    exit 1
fi

# ---------------------------------------------------------------------
# Pass 2: reached only when every non-skipped row is ALREADY APPLIED or
# SAFE TO APPLY (Pass 1 guarantees this). Applies the safe rows (or, under
# --dry-run, reports what would be applied) and leaves already-applied rows
# untouched, exactly as before.
# ---------------------------------------------------------------------
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
    case "$cat" in *manual*) echo "skip     $anchor (manual)"; continue ;; esac
    [ -n "$repl" ] && [ "$repl" != "n/a" ] || { echo "skip     $anchor (no replacement)"; continue; }

    expected="$(expected_count "$cat")"
    rcount="$(count_occurrences "$repl" "$DECK_TEXT")"
    acount="$(count_occurrences "$anchor" "$DECK_TEXT")"

    append_style=0
    case "$repl" in *"$anchor"*) append_style=1 ;; esac

    already=0
    if [ "$append_style" -eq 1 ]; then
        [ "$rcount" -eq "$expected" ] && [ "$acount" -eq "$rcount" ] && already=1
    else
        [ "$rcount" -eq "$expected" ] && [ "$acount" -eq 0 ] && already=1
    fi

    if [ "$already" -eq 1 ]; then
        echo "already  $anchor  (found R=$rcount, anchors=$acount)"
        continue
    fi

    # Pass 1 already guaranteed this row is SAFE TO APPLY (A == E, R == 0).
    if [ "$DRY" = "--dry-run" ]; then
        echo "would replace: '$anchor' -> '$repl'"
    else
        "$GSLIDES" personal replace "$PID" "$anchor" "$repl" >/dev/null \
            || { echo "Error: gslides replace failed for anchor: $anchor" >&2; exit 1; }
        echo "applied  $anchor -> $repl"
        applied=$((applied + 1))
    fi
done < "$DOC"

if [ "$DRY" = "--dry-run" ]; then
    echo "dry run, nothing applied."
    exit 0
fi

echo
echo "$applied row(s) applied. Re-reading deck to verify..."
"$GSLIDES" personal text "$PID" > "$DECK_TEXT" 2>/dev/null \
    || { echo "Error: could not read deck $PID" >&2; exit 1; }

rc=0
while IFS= read -r line; do
    case "$line" in '|'*) ;; *) continue ;; esac
    case "$line" in *'---'*) continue ;; '| Slide '*) continue ;; esac
    anchor="$(printf '%s' "$line" | awk -F'|' '{print $3}' | sed 's/^[[:space:]]*//; s/[[:space:]]*$//')"
    repl="$(printf '%s' "$line" | awk -F'|' '{print $5}' | sed 's/^[[:space:]]*//; s/[[:space:]]*$//')"
    cat="$(printf '%s' "$line" | awk -F'|' '{print $6}' | sed 's/^[[:space:]]*//; s/[[:space:]]*$//')"
    [ -n "$anchor" ] && [ "$anchor" != "Anchor" ] || continue
    case "$cat" in *skip*) continue ;; esac
    case "$cat" in *manual*) continue ;; esac
    [ -n "$repl" ] && [ "$repl" != "n/a" ] || continue

    expected="$(expected_count "$cat")"
    rcount="$(count_occurrences "$repl" "$DECK_TEXT")"
    acount="$(count_occurrences "$anchor" "$DECK_TEXT")"

    case "$repl" in
        *"$anchor"*)
            # Append-style: the Anchor legitimately survives inside the new
            # text, so its mere presence is not a failure. Pass requires the
            # full Proposed text at the expected count AND every Anchor
            # occurrence accounted for inside a replacement occurrence.
            if [ "$rcount" -ne "$expected" ] || [ "$acount" -ne "$rcount" ]; then
                echo "FAIL  replacement not present as expected (found R=$rcount, expected=$expected, anchors=$acount): $repl"; rc=1
            fi
            ;;
        *)
            if [ "$acount" -ne 0 ]; then
                echo "FAIL  old text still present ($acount occurrence(s)): $anchor"; rc=1
            fi
            if [ "$rcount" -ne "$expected" ]; then
                echo "FAIL  new text not present as expected (found $rcount, expected $expected): $repl"; rc=1
            fi
            ;;
    esac
done < "$DOC"

[ $rc -eq 0 ] && echo "Verified: all replacements present, all anchors gone."
exit $rc
