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
#
# Deck-wide cross-check (safety-critical, kept in lockstep with
# deck-apply.sh's Fix 1): a scoped row's in-scope count alone cannot say
# whether the anchor ALSO occurs outside the declared scope. Populating
# Slide narrows counting with no corroborating check otherwise — and the
# repo's own change docs populate Slide on nearly every row as
# documentation, so this must be checked HERE too, before the trainer ever
# reviews the document, not only in deck-apply.sh's pre-flight. Every
# scoped row is also counted deck-wide (unsliced); a mismatch is reported
# as UNSAFE unless the Category column carries the explicit
# `outside-scope-ok` token, in which case the row proceeds through the
# normal ok/MISSING/AMBIGUOUS/COUNT MISMATCH logic below (using the
# in-scope count) and the out-of-scope occurrence count is still printed.
#
# Slide-boundary hardening (kept in lockstep with deck-apply.sh's Fix 3):
# slide_slice trusts any line matching the boundary regex, with no guard
# against slide content that merely looks like one. When any row is
# scoped, the boundaries parsed out of the deck-text dump are verified
# against the presentation's real slides (`gslides.sh personal slides`)
# before any scoped count is trusted; disagreement hard-fails the run.
set -uo pipefail

DOC="${1:?usage: deck-check.sh <change-doc.md> <presentation-id>}"
PID="${2:?usage: deck-check.sh <change-doc.md> <presentation-id>}"
GSLIDES="${GSLIDES:-/Users/bogdan/.claude/plugins/cache/nix-config/nix/1.0.2/scripts/gslides.sh}"

[ -f "$DOC" ] || { echo "Error: change doc not found: $DOC" >&2; exit 1; }

DECK_TEXT="$(mktemp)"; SLIDE_IDS_FILE="$(mktemp)"
trap 'rm -f "$DECK_TEXT" "$SLIDE_IDS_FILE"' EXIT
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

# Whether the Category column carries the explicit `outside-scope-ok`
# token. Kept byte-for-byte in step with the same helper in deck-apply.sh.
has_outside_scope_ok() {
    printf '%s' "$1" | grep -qE '(^|[[:space:],])outside-scope-ok([[:space:],]|$)'
}

# Hard safety gate for scoped counting — kept byte-for-byte in step with
# deck-apply.sh's verify_slide_boundaries. See that script's header comment
# for the full rationale.
verify_slide_boundaries() {
    local deck_file="$1" slide_ids_file="$2"
    local real_total parsed_total boundaries
    real_total="$(wc -l < "$slide_ids_file" | tr -d '[:space:]')"
    boundaries="$(mktemp)"
    grep -E '^=== Slide [0-9]+ \[[^]]*\] ===' "$deck_file" > "$boundaries"
    parsed_total="$(wc -l < "$boundaries" | tr -d '[:space:]')"
    if [ "$parsed_total" -ne "$real_total" ]; then
        echo "Error: slide boundary count mismatch -- parsed $parsed_total \"=== Slide N [...] ===\" marker(s) out of the deck-text dump, but $GSLIDES personal slides reports $real_total real slide(s). Refusing to trust any scoped count. This can happen if a slide's own text content contains a line shaped like a slide-boundary header." >&2
        rm -f "$boundaries"
        return 1
    fi
    local expect_idx=0 bline idx objid real_id
    while IFS= read -r bline; do
        idx="$(printf '%s' "$bline" | awk '{print $3}')"
        objid="$(printf '%s' "$bline" | sed -E 's/^=== Slide [0-9]+ \[([^]]*)\] ===.*/\1/')"
        if [ "$idx" != "$expect_idx" ]; then
            echo "Error: slide boundaries out of sequence -- expected index $expect_idx next, found $idx. Refusing to trust any scoped count." >&2
            rm -f "$boundaries"
            return 1
        fi
        real_id="$(sed -n "$((expect_idx + 1))p" "$slide_ids_file")"
        if [ "$objid" != "$real_id" ]; then
            echo "Error: slide boundary objectId mismatch at index $expect_idx -- parsed \"$objid\" from the deck-text dump, but the live presentation's slide $expect_idx has objectId \"$real_id\". Refusing to trust any scoped count." >&2
            rm -f "$boundaries"
            return 1
        fi
        expect_idx=$((expect_idx + 1))
    done < "$boundaries"
    rm -f "$boundaries"
    return 0
}

# Only pay for the slide-ID listing call when the document actually scopes
# at least one row — kept in step with deck-apply.sh's same optimization.
needs_scope=0
while IFS= read -r line; do
    case "$line" in '|'*) ;; *) continue ;; esac
    case "$line" in *'---'*) continue ;; '| Slide '*) continue ;; esac
    slide_raw="$(printf '%s' "$line" | awk -F'|' '{print $2}' | sed 's/^[[:space:]]*//; s/[[:space:]]*$//')"
    [ -n "$(normalize_scope "$slide_raw")" ] && { needs_scope=1; break; }
done < "$DOC"

if [ "$needs_scope" -eq 1 ]; then
    "$GSLIDES" personal slides "$PID" > "$SLIDE_IDS_FILE" 2>/dev/null \
        || { echo "Error: could not list slide object IDs for $PID" >&2; exit 1; }
    verify_slide_boundaries "$DECK_TEXT" "$SLIDE_IDS_FILE" \
        || exit 1
fi

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

        # Deck-wide cross-check (Fix 1, kept in lockstep with
        # deck-apply.sh): a scoped in-scope count alone can't say whether
        # the anchor also occurs outside the declared scope.
        deckwide_count="$(grep -o -F -- "$anchor" "$DECK_TEXT" | wc -l | tr -d '[:space:]')"
        if [ "$deckwide_count" -ne "$count" ]; then
            outside_count=$((deckwide_count - count))
            if has_outside_scope_ok "$cat"; then
                echo "note     outside-scope-ok: $outside_count occurrence(s) outside slide(s) $scope  $label"
            else
                echo "UNSAFE (scope mismatch: $count within scope, $deckwide_count deck-wide -- $outside_count time(s) outside declared scope)  $label"
                missing=$((missing + 1))
                rc=1
                continue
            fi
        fi
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
