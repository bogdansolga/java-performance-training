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
