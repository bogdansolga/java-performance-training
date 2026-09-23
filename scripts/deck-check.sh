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

    cat="$(printf '%s' "$line" | awk -F'|' '{print $6}' \
           | sed 's/^[[:space:]]*//; s/[[:space:]]*$//')"

    # Category may declare an expected occurrence count via an `xN` token
    # (e.g. "correction x2"), for anchors that legitimately repeat verbatim
    # across slides and are meant to be fixed everywhere in one global
    # replace. No token present => expected count is 1 (default behaviour).
    expected=1
    xtoken="$(printf '%s' "$cat" | grep -oE '(^|[[:space:],])x[0-9]+' | head -1)"
    [ -n "$xtoken" ] && expected="${xtoken##*x}"

    # NOTE: this relies on the script NOT running under `set -e`. Under
    # `pipefail` alone, `grep -o` exiting 1 on zero matches only affects the
    # pipeline's overall status, which we don't check here — we only read
    # the captured stdout (empty on no match) via `wc -l`. Adding `set -e`
    # would abort the script here on a legitimate MISSING before it can be
    # reported. Do not add `set -e` to this script.
    count="$(grep -o -F -- "$anchor" "$DECK_TEXT" | wc -l | tr -d '[:space:]')"
    if [ "$count" -eq 0 ]; then
        echo "MISSING  $anchor"
        missing=$((missing + 1))
        rc=1
    elif [ "$count" -eq "$expected" ]; then
        echo "ok       $anchor"
        found=$((found + 1))
    elif [ "$expected" -eq 1 ]; then
        echo "AMBIGUOUS ($count hits)  $anchor"
        missing=$((missing + 1))
        rc=1
    else
        echo "COUNT MISMATCH (found $count, expected $expected)  $anchor"
        missing=$((missing + 1))
        rc=1
    fi
done < "$DOC"

echo
echo "$found anchor(s) verified, $missing problem(s)."
exit $rc
