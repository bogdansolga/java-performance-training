#!/usr/bin/env bash
#
# deck-apply.sh — apply approved rows from a change doc to the live deck.
#
# Usage: ./scripts/deck-apply.sh <change-doc.md> <presentation-id> [--dry-run]
#
# Only rows whose Category column does NOT contain 'skip' are applied.
#
# Before applying, the deck is read ONCE and each row is checked for whether
# it is already applied: the full Proposed text is already present at the
# expected occurrence count (an `xN` token in Category, default 1). Already-
# applied rows are reported (`already  <anchor>`) and never sent to
# `gslides.sh replace` — this keeps re-runs idempotent for append-style rows,
# where the Proposed text contains the Anchor (so the Anchor legitimately
# survives the edit) and a naive re-apply would double the appended text.
# This check runs in --dry-run too.
#
# Re-verifies after applying:
#   - append-style rows (Proposed text contains Anchor): the Anchor is
#     expected to still be present (it's part of the new text), so we assert
#     the full Proposed text is present at the expected count instead of
#     asserting the Anchor is gone.
#   - all other rows: Anchor must be GONE, Proposed text must be PRESENT
#     (unchanged behaviour).
set -uo pipefail

DOC="${1:?usage: deck-apply.sh <change-doc.md> <presentation-id> [--dry-run]}"
PID="${2:?usage: deck-apply.sh <change-doc.md> <presentation-id> [--dry-run]}"
DRY="${3:-}"
GSLIDES="/Users/bogdan/.claude/plugins/cache/nix-config/nix/1.0.2/scripts/gslides.sh"

# Count true occurrences of a literal substring in a file (same method as
# deck-check.sh): `grep -o` prints one line per match, so `wc -l` gives the
# occurrence count, not the matching-line count. `grep -o` exits 1 on zero
# matches; that's fine here since the script runs under `set -uo pipefail`
# without `set -e`, and only the captured stdout (empty on no match) is used.
count_occurrences() {
    grep -o -F -- "$1" "$2" | wc -l | tr -d '[:space:]'
}

# Expected occurrence count from the Category column's `xN` token, default 1.
expected_count() {
    local tok
    tok="$(printf '%s' "$1" | grep -oE 'x[0-9]+' | head -1)"
    if [ -n "$tok" ]; then
        printf '%s' "${tok#x}"
    else
        printf '1'
    fi
}

# Read the deck once, up front, for the already-applied check (one network
# call for a ~1000-line deck, not one per row). Re-read after applying (below)
# for post-verify.
DECK_TEXT="$(mktemp)"; trap 'rm -f "$DECK_TEXT"' EXIT
"$GSLIDES" personal text "$PID" > "$DECK_TEXT" 2>/dev/null

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
    replcount="$(count_occurrences "$repl" "$DECK_TEXT")"
    if [ "$replcount" -eq "$expected" ]; then
        echo "already  $anchor"
        continue
    fi

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
    case "$cat" in *manual*) continue ;; esac
    [ -n "$repl" ] && [ "$repl" != "n/a" ] || continue

    case "$repl" in
        *"$anchor"*)
            # Append-style: the Anchor legitimately survives inside the new
            # text, so its mere presence is not a failure. Assert the full
            # Proposed text is present at the expected count instead.
            expected="$(expected_count "$cat")"
            count="$(count_occurrences "$repl" "$DECK_TEXT")"
            if [ "$count" -ne "$expected" ]; then
                echo "FAIL  replacement not present as expected (found $count, expected $expected): $repl"; rc=1
            fi
            ;;
        *)
            if grep -F -q -- "$anchor" "$DECK_TEXT"; then
                echo "FAIL  old text still present: $anchor"; rc=1
            fi
            if ! grep -F -q -- "$repl" "$DECK_TEXT"; then
                echo "FAIL  new text not found: $repl"; rc=1
            fi
            ;;
    esac
done < "$DOC"

[ $rc -eq 0 ] && echo "Verified: all replacements present, all anchors gone."
exit $rc
