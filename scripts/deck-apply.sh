#!/usr/bin/env bash
#
# deck-apply.sh — apply approved rows from a change doc to the live deck.
#
# Usage: ./scripts/deck-apply.sh <change-doc.md> <presentation-id> [--dry-run]
#
# Only rows whose Category column does NOT contain 'skip' are applied.
#
# --- Slide scoping ---------------------------------------------------
# The Slide column (0-based, as `gslides.sh personal text` prints it) may
# name one or more slides, comma-separated (e.g. "18" or "18, 20"). When
# non-blank (and not a bare "-"/"—"), the row's replaceAllText is scoped to
# exactly those slides via the API's `pageObjectIds`, resolved from index to
# live page object ID with `gslides.sh personal slides <id>` (one objectId
# per line, in slide order — line N+1 is the object ID for 0-based index N).
# This is what makes "the same string occurs on two slides needing different
# fixes" scriptable: each row scopes to its own slide, so a whole-deck
# collision with the OTHER slide's occurrence never happens. It does NOT
# help when the collision is on the SAME slide (pageObjectIds is page-level,
# not shape-level) — that case is still genuinely unscriptable and must stay
# `manual`.
#
# `gslides.sh personal replace` has no pageObjectIds parameter, so a scoped
# row is sent via `gslides.sh personal batch` with a raw replaceAllText
# request carrying `pageObjectIds`. An unscoped row (blank/"-"/"—" Slide
# column) uses `gslides.sh personal replace` exactly as before — the legacy
# path is untouched.
#
# Occurrence counting (R, A below) for a scoped row is computed against
# ONLY that row's slide(s) — sliced locally out of the single up-front deck
# read using the "=== Slide N [...] ===" section markers `gslides.sh
# personal text` already prints, no extra network call per row. Counting
# scoped rows against the WHOLE deck would be wrong in both directions: an
# anchor legitimately reused elsewhere in the deck would inflate A past
# `expected` and wrongly abort as UNSAFE, and the reverse (only checking
# within scope) is what actually makes cross-slide collisions safe to apply
# at all instead of requiring an artificial xN.
#
# --- Deletion convention ----------------------------------------------
# A row whose Proposed text is EXACTLY the literal token `<DELETE>` means
# "replace the Anchor with nothing" (`replaceText: ""`) — confirmed by
# experiment to delete cleanly via the Slides API, no stray runs left
# behind. Previously such rows had to be left with an empty Proposed text
# cell, which both this script and deck-check.sh treated as "no
# replacement" and silently skipped; `<DELETE>` is unambiguous and IS
# applied. A deletion row has no "R" (there is nothing to count — you
# cannot search for the presence of an empty string) so it is classified on
# Anchor-presence alone: already-applied <=> A==0 after a previous run;
# safe-to-apply <=> A==expected before this run.
#
# Before applying, the deck is read ONCE and EVERY non-skipped row is
# classified in a fail-closed pre-flight pass, using TWO signals: R = true
# occurrence count of the Proposed text, A = true occurrence count of the
# Anchor (both computed within the row's slide scope, or deck-wide when
# unscoped), E = expected occurrence count (an `xN` token in Category,
# default 1). A row is "append-style" when the Proposed text contains the
# Anchor as a substring (so the Anchor legitimately survives a correct
# edit); otherwise it is "plain-style" (or "delete-style" for `<DELETE>`).
# Every row is classified as exactly one of:
#
#   ALREADY APPLIED  plain:   R == E  AND  A == 0
#                     append:  R == E  AND  A == R
#                     delete:  A == 0
#   SAFE TO APPLY     plain:   A == E  AND  R == 0
#                     append:  A == E  AND  R == 0
#                     delete:  A == E
#   UNSAFE            anything else (including: a Slide column that fails to
#                     resolve to a live slide index)
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
# `gslides.sh replace` or `gslides.sh batch`; it behaves identically under
# --dry-run.
#
# Already-applied rows are reported (`already  <anchor>  (found R=.., ..)`)
# and never sent to `gslides.sh replace`/`batch` — this keeps re-runs
# idempotent for append-style rows, where a naive re-apply would double the
# appended text.
#
# Re-verifies after applying, using the SAME corroborated rules:
#   - append-style rows: pass <=> R == E AND A == R.
#   - plain-style rows:  pass <=> R == E AND A == 0.
#   - delete-style rows: pass <=> A == 0 (there is no R to check).
set -uo pipefail

DOC="${1:?usage: deck-apply.sh <change-doc.md> <presentation-id> [--dry-run]}"
PID="${2:?usage: deck-apply.sh <change-doc.md> <presentation-id> [--dry-run]}"
DRY="${3:-}"
GSLIDES="${GSLIDES:-/Users/bogdan/.claude/plugins/cache/nix-config/nix/1.0.2/scripts/gslides.sh}"
DELETE_TOKEN='<DELETE>'

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

# Normalize the Slide column: blank, "-" or the em dash "—" all mean "no
# scoping, whole-deck behaviour" (today's behaviour, unchanged). Anything
# else is returned as-is — a single index ("18") or a comma-separated list
# ("18, 20") — for parse_indices/slide_slice/resolve_page_object_ids below.
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
# an "all slides" deck-text dump (as produced by `gslides.sh personal
# text`, whose per-slide sections are delimited by the
# "=== Slide N [objectId] ===" header lines it prints for EVERY slide,
# including ones with no text). Purely local text processing against the
# already-fetched dump — no extra network call, and the section markers use
# the exact same 0-based indexing as the change doc's Slide column.
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

# Occurrence count of a literal substring, scoped to the row's Slide(s) when
# non-blank, deck-wide otherwise (identical to the pre-scoping behaviour).
count_occurrences_scoped() {
    local pattern="$1" scope="$2" deck_file="$3"
    if [ -z "$scope" ]; then
        count_occurrences "$pattern" "$deck_file"
    else
        slide_slice "$scope" "$deck_file" | grep -o -F -- "$pattern" | wc -l | tr -d '[:space:]'
    fi
}

# Resolve a scope string to a JSON array of live page object IDs, e.g.
# ["gAbC...","gXyZ..."], by looking up each 0-based index as a line number
# (1-based) in SLIDE_IDS_FILE (`gslides.sh personal slides <id>` output,
# one objectId per line, in slide order). Prints nothing (empty stdout) if
# any index is out of range or non-numeric -- callers must treat that as a
# resolution failure, not an empty-but-valid scope.
resolve_page_object_ids() {
    local scope="$1" slide_ids_file="$2" idx total ids
    total="$(wc -l < "$slide_ids_file" | tr -d '[:space:]')"
    ids=()
    while IFS= read -r idx; do
        case "$idx" in ''|*[!0-9]*) return 1 ;; esac
        [ "$idx" -lt "$total" ] || return 1
        ids+=("$(sed -n "$((idx + 1))p" "$slide_ids_file")")
    done < <(parse_indices "$scope")
    [ "${#ids[@]}" -gt 0 ] || return 1
    printf '%s\n' "${ids[@]}" | jq -R . | jq -s -c .
}

# Read the deck once, up front, for the already-applied check (one network
# call for a ~1000-line deck, not one per row). Re-read after applying (below)
# for post-verify. A failed read must abort loudly — never be mistaken for
# "text not found" (which would make every anchor look absent/gone).
DECK_TEXT="$(mktemp)"; SLIDE_IDS_FILE="$(mktemp)"
trap 'rm -f "$DECK_TEXT" "$SLIDE_IDS_FILE"' EXIT
"$GSLIDES" personal text "$PID" > "$DECK_TEXT" 2>/dev/null \
    || { echo "Error: could not read deck $PID" >&2; exit 1; }

# Only pay for the slide-ID listing call when the document actually scopes
# at least one row — most change docs so far scope none of their rows.
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
fi

# ---------------------------------------------------------------------
# Shared row classifier. Reads: $line. Sets: anchor, repl (display value —
# "<DELETE>" is shown as itself, never as an empty string), is_delete,
# scope, scope_err, style, expected, acount, rcount, already, safe.
# `apply_repl` holds the value actually sent to the API (empty string for
# delete rows). Pure function of $line and $deck_file — side-effect free,
# safe to call from the pre-flight pass, the apply pass and post-verify.
# ---------------------------------------------------------------------
classify_row() {
    local deck_file="$1"

    slide_raw="$(printf '%s' "$line" | awk -F'|' '{print $2}' | sed 's/^[[:space:]]*//; s/[[:space:]]*$//')"
    anchor="$(printf '%s' "$line" | awk -F'|' '{print $3}' | sed 's/^[[:space:]]*//; s/[[:space:]]*$//')"
    repl="$(printf '%s' "$line" | awk -F'|' '{print $5}' | sed 's/^[[:space:]]*//; s/[[:space:]]*$//')"
    cat="$(printf '%s' "$line" | awk -F'|' '{print $6}' | sed 's/^[[:space:]]*//; s/[[:space:]]*$//')"

    scope="$(normalize_scope "$slide_raw")"

    is_delete=0
    apply_repl="$repl"
    if [ "$repl" = "$DELETE_TOKEN" ]; then
        is_delete=1
        apply_repl=""
    fi

    scope_err=""
    if [ -n "$scope" ]; then
        if ! resolve_page_object_ids "$scope" "$SLIDE_IDS_FILE" >/dev/null 2>&1; then
            scope_err="Slide column \"$slide_raw\" does not resolve to $(wc -l < "$SLIDE_IDS_FILE" | tr -d '[:space:]') live slide index/indices"
        fi
    fi

    expected="$(expected_count "$cat")"
    acount="$(count_occurrences_scoped "$anchor" "$scope" "$deck_file")"

    style="plain"
    already=0
    safe=0
    if [ "$is_delete" -eq 1 ]; then
        style="delete"
        rcount=0
        [ "$acount" -eq 0 ] && already=1
        [ "$already" -ne 1 ] && [ "$acount" -eq "$expected" ] && safe=1
    else
        rcount="$(count_occurrences_scoped "$repl" "$scope" "$deck_file")"
        case "$repl" in *"$anchor"*) style="append" ;; esac
        if [ "$style" = "append" ]; then
            [ "$rcount" -eq "$expected" ] && [ "$acount" -eq "$rcount" ] && already=1
        else
            [ "$rcount" -eq "$expected" ] && [ "$acount" -eq 0 ] && already=1
        fi
        if [ "$already" -ne 1 ]; then
            [ "$acount" -eq "$expected" ] && [ "$rcount" -eq 0 ] && safe=1
        fi
    fi
}

# ---------------------------------------------------------------------
# Pass 1: pre-flight classification. Read-only against DECK_TEXT (already
# fetched above); never calls `gslides.sh replace`/`batch`. Every
# non-skipped row is classified as ALREADY APPLIED, SAFE TO APPLY, or
# UNSAFE. If ANY row is UNSAFE, print why (naming A, R, E) for each one and
# abort non-zero before Pass 2 ever runs — nothing is applied, not even
# rows that are themselves safe. This is identical under --dry-run.
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
    if [ "$repl" != "$DELETE_TOKEN" ]; then
        [ -n "$repl" ] && [ "$repl" != "n/a" ] || continue
    fi

    classify_row "$DECK_TEXT"

    if [ -n "$scope_err" ]; then
        echo "UNSAFE  $anchor  (unresolvable Slide scope: $scope_err)"
        echo "        fix: correct the Slide column to a valid 0-based index (or comma-separated list), or clear it for whole-deck behaviour."
        had_unsafe=1
        continue
    fi

    if [ "$already" -eq 1 ] || [ "$safe" -eq 1 ]; then
        continue
    fi

    scope_note=""
    [ -n "$scope" ] && scope_note=" [scoped to slide(s) $scope]"
    if [ "$is_delete" -eq 1 ]; then
        echo "UNSAFE  $anchor  (delete-style$scope_note, found A=$acount, expected=$expected)"
        if [ "$acount" -gt "$expected" ]; then
            echo "        anchor is not unique within scope: occurs $acount time(s) but this row expects $expected -- deleting now would also remove $((acount - expected)) unrelated occurrence(s)."
        else
            echo "        neither already-applied (A==0) nor safe-to-apply (A==expected) holds."
        fi
    else
        echo "UNSAFE  $anchor  ($style-style$scope_note, found A=$acount, R=$rcount, expected=$expected)"
        if [ "$style" = "plain" ] && [ "$acount" -gt "$expected" ]; then
            echo "        anchor is not unique within scope: occurs $acount time(s) but this row expects $expected -- a replace would also rewrite $((acount - expected)) unrelated location(s)."
        else
            echo "        neither already-applied (R==E,A==0 / R==E,A==R) nor safe-to-apply (A==E,R==0) holds."
        fi
    fi
    echo "        fix: re-derive the anchor so it is unique within scope, narrow/add Slide scoping, or if changing every occurrence is genuinely intended, declare the true count with an xN token in Category."
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
    if [ "$repl" != "$DELETE_TOKEN" ]; then
        [ -n "$repl" ] && [ "$repl" != "n/a" ] || { echo "skip     $anchor (no replacement)"; continue; }
    fi

    classify_row "$DECK_TEXT"
    # Pass 1 already guarantees scope_err is empty and already/safe holds.

    if [ "$already" -eq 1 ]; then
        echo "already  $anchor  (found A=$acount, R=$rcount)"
        continue
    fi

    repl_display="$repl"
    [ "$is_delete" -eq 1 ] && repl_display="$DELETE_TOKEN"

    if [ "$DRY" = "--dry-run" ]; then
        if [ -n "$scope" ]; then
            echo "would replace [slide(s) $scope]: '$anchor' -> '$repl_display'"
        else
            echo "would replace: '$anchor' -> '$repl_display'"
        fi
        continue
    fi

    if [ -n "$scope" ]; then
        page_ids_json="$(resolve_page_object_ids "$scope" "$SLIDE_IDS_FILE")" \
            || { echo "Error: could not resolve Slide scope \"$slide_raw\" for anchor: $anchor" >&2; exit 1; }
        request="$(jq -n --arg a "$anchor" --arg r "$apply_repl" --argjson pids "$page_ids_json" \
            '[{replaceAllText:{containsText:{text:$a, matchCase:true}, replaceText:$r, pageObjectIds:$pids}}]')"
        "$GSLIDES" personal batch "$PID" "$request" >/dev/null \
            || { echo "Error: gslides batch replace failed for anchor: $anchor" >&2; exit 1; }
    else
        "$GSLIDES" personal replace "$PID" "$anchor" "$apply_repl" >/dev/null \
            || { echo "Error: gslides replace failed for anchor: $anchor" >&2; exit 1; }
    fi
    echo "applied  $anchor -> $repl_display"
    applied=$((applied + 1))
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
    if [ "$repl" != "$DELETE_TOKEN" ]; then
        [ -n "$repl" ] && [ "$repl" != "n/a" ] || continue
    fi

    classify_row "$DECK_TEXT"

    if [ "$is_delete" -eq 1 ]; then
        if [ "$acount" -ne 0 ]; then
            echo "FAIL  old text still present ($acount occurrence(s)): $anchor"; rc=1
        fi
        continue
    fi

    case "$style" in
        append)
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
