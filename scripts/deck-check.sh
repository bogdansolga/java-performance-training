#!/usr/bin/env bash
#
# deck-check.sh — verify every anchor in a change doc exists in the live deck.
#
# Usage: ./scripts/deck-check.sh <change-doc.md> <presentation-id>
#
# Exits 0 if every anchor is found exactly once, 1 otherwise.
#
# Slide scoping: when the Slide column names one or more 0-based slide
# indices (e.g. "18" or "18, 20"), the occurrence count is computed against
# ONLY those slides' text — sliced locally out of the single up-front deck
# read using the "=== Slide N [...] ===" section markers `gslides.sh
# personal text` prints, the same convention `deck-apply.sh` uses for its
# scoped pre-flight counts, so the two scripts never disagree about what a
# row's true occurrence count is. Blank, "-" or "—" in the Slide column
# means "no scoping" — the count is against the whole deck, exactly as
# before this feature existed.
set -uo pipefail

DOC="${1:?usage: deck-check.sh <change-doc.md> <presentation-id>}"
PID="${2:?usage: deck-check.sh <change-doc.md> <presentation-id>}"
GSLIDES="${GSLIDES:-/Users/bogdan/.claude/plugins/cache/nix-config/nix/1.0.2/scripts/gslides.sh}"

[ -f "$DOC" ] || { echo "Error: change doc not found: $DOC" >&2; exit 1; }

DECK_TEXT="$(mktemp)"
trap 'rm -f "$DECK_TEXT"' EXIT
"$GSLIDES" personal text "$PID" > "$DECK_TEXT" 2>/dev/null \
    || { echo "Error: could not read deck $PID" >&2; exit 1; }

# Normalize the Slide column: blank, "-" or the em dash "—" all mean "no
# scoping, whole-deck behaviour". Anything else (a single index, or a
# comma-separated list) is returned as-is. Kept byte-for-byte in step with
# the same helper in deck-apply.sh.
normalize_scope() {
    case "$1" in
        ''|'-'|'—') printf '' ;;
        *) printf '%s' "$1" ;;
    esac
}

# A scope string ("18" or "18, 20") to a newline-separated list of trimmed
# indices.
parse_indices() {
    printf '%s' "$1" | tr ',' '\n' | sed 's/^[[:space:]]*//; s/[[:space:]]*$//' | grep -v '^$'
}

# Extract the concatenated text of one or more 0-based slide indices out of
# an "all slides" deck-text dump. See deck-apply.sh's slide_slice for the
# full rationale; identical implementation, kept in agreement on purpose.
slide_slice() {
    local indices="$1" deck_file="$2"
    awk -v want="$(parse_indices "$indices" | tr '\n' ' ')" '
        BEGIN {
            n = split(want, arr, /[ \t]+/)
            for (i = 1; i <= n; i++) if (arr[i] != "") wantset[arr[i]] = 1
        }
        /^=== Slide [0-9]+ / {
            printing = ($3 in wantset)
            next
        }
        { if (printing) print }
    ' "$deck_file"
}

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

    slide_raw="$(printf '%s' "$line" | awk -F'|' '{print $2}' \
                 | sed 's/^[[:space:]]*//; s/[[:space:]]*$//')"
    scope="$(normalize_scope "$slide_raw")"

    cat="$(printf '%s' "$line" | awk -F'|' '{print $6}' \
           | sed 's/^[[:space:]]*//; s/[[:space:]]*$//')"

    # Category may declare an expected occurrence count via an `xN` token
    # (e.g. "correction x2"), for anchors that legitimately repeat verbatim
    # across slides and are meant to be fixed everywhere in one global
    # replace. No token present => expected count is 1 (default behaviour).
    expected=1
    xtoken="$(printf '%s' "$cat" | grep -oE '(^|[[:space:],])x[0-9]+' | head -1)"
    [ -n "$xtoken" ] && expected="${xtoken##*x}"

    label="$anchor"
    [ -n "$scope" ] && label="$anchor  [slide(s) $scope]"

    # NOTE: this relies on the script NOT running under `set -e`. Under
    # `pipefail` alone, `grep -o` exiting 1 on zero matches only affects the
    # pipeline's overall status, which we don't check here — we only read
    # the captured stdout (empty on no match) via `wc -l`. Adding `set -e`
    # would abort the script here on a legitimate MISSING before it can be
    # reported. Do not add `set -e` to this script.
    if [ -n "$scope" ]; then
        count="$(slide_slice "$scope" "$DECK_TEXT" | grep -o -F -- "$anchor" | wc -l | tr -d '[:space:]')"
    else
        count="$(grep -o -F -- "$anchor" "$DECK_TEXT" | wc -l | tr -d '[:space:]')"
    fi
    if [ "$count" -eq 0 ]; then
        echo "MISSING  $label"
        missing=$((missing + 1))
        rc=1
    elif [ "$count" -eq "$expected" ]; then
        echo "ok       $label"
        found=$((found + 1))
    elif [ "$expected" -eq 1 ]; then
        echo "AMBIGUOUS ($count hits)  $label"
        missing=$((missing + 1))
        rc=1
    else
        echo "COUNT MISMATCH (found $count, expected $expected)  $label"
        missing=$((missing + 1))
        rc=1
    fi
done < "$DOC"

echo
echo "$found anchor(s) verified, $missing problem(s)."
exit $rc
