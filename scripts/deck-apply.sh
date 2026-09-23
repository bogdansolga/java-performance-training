#!/usr/bin/env bash
#
# deck-apply.sh — apply approved rows from a change doc to the live deck.
#
# Usage: ./scripts/deck-apply.sh <change-doc.md> <presentation-id> [--dry-run]
#
# Only rows whose Category column does NOT contain 'skip' are applied.
#
# Before applying, the deck is read ONCE and each row is corroborated against
# TWO signals: R = true occurrence count of the Proposed text, A = true
# occurrence count of the Anchor, E = expected occurrence count (an `xN`
# token in Category, default 1). A row is "append-style" when the Proposed
# text contains the Anchor as a substring (so the Anchor legitimately
# survives a correct edit); otherwise it is "plain-style".
#
#   plain-style   already applied  <=>  R == E  AND  A == 0
#   append-style  already applied  <=>  R == E  AND  A == R
#
# Checking R alone (the old behaviour) is NOT sufficient: the Proposed text
# can coincidentally exist elsewhere in the deck while the real target is
# still unedited, which would falsely report "already" and skip a row that
# actually needs editing — on a Google Slides deck (no version history) a
# skipped edit is indistinguishable from a correctly-applied one until a
# human notices. Corroborating with A (the Anchor must be fully accounted
# for) closes that hole. This check runs identically under --dry-run, is
# side-effect free, and never calls `gslides.sh replace`.
#
# A row that is neither "already applied" nor in a clean, safe-to-apply
# state (R <= E, and for append-style rows A >= R) is NOT silently applied
# and NOT silently skipped: it is reported as UNRESOLVED and the script
# exits non-zero, so a human decides before any further live edit is made.
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

applied=0
had_ambiguous=0
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

    if [ "$append_style" -eq 1 ]; then
        already=0
        [ "$rcount" -eq "$expected" ] && [ "$acount" -eq "$rcount" ] && already=1
    else
        already=0
        [ "$rcount" -eq "$expected" ] && [ "$acount" -eq 0 ] && already=1
    fi

    if [ "$already" -eq 1 ]; then
        echo "already  $anchor  (found R=$rcount, anchors=$acount)"
        continue
    fi

    # Not already applied. Only proceed if the counts describe a clean,
    # safe-to-apply state: the replacement isn't already over-present
    # (R <= E), and for append-style rows every Anchor occurrence found so
    # far is structurally accounted for by an existing replacement instance
    # (A >= R) — i.e. nothing suggests a prior partial/duplicated apply that
    # a fresh whole-deck replaceAllText could corrupt further. Anything else
    # is reported distinctly and blocks this row rather than guessing.
    clean=0
    if [ "$append_style" -eq 1 ]; then
        [ "$rcount" -le "$expected" ] && [ "$acount" -ge "$rcount" ] && clean=1
    else
        [ "$rcount" -le "$expected" ] && clean=1
    fi

    if [ "$clean" -ne 1 ]; then
        echo "UNRESOLVED  $anchor  (found R=$rcount, expected=$expected, anchors=$acount) -- ambiguous state, not applying"
        had_ambiguous=1
        continue
    fi

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
    [ "$had_ambiguous" -eq 0 ]; exit $?
fi

echo
echo "$applied row(s) applied. Re-reading deck to verify..."
"$GSLIDES" personal text "$PID" > "$DECK_TEXT" 2>/dev/null \
    || { echo "Error: could not read deck $PID" >&2; exit 1; }

rc=0
[ "$had_ambiguous" -eq 0 ] || rc=1
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
