# Deck 3 — A performance toolbox

> **STATUS: APPLIED 2026-09-29** — applied by `deck-apply.sh` and independently verified on a fresh dump (each replacement present exactly once, each deleted anchor gone). **DO NOT RE-RUN `deck-apply.sh` ON THIS FILE.**

~~STATUS: DRAFT — not yet applied.~~ Produced for trainer review. `deck-apply.sh` invoked
only `--dry-run`; live deck never modified.

Source read: `gslides.sh personal text 188yqDAwSq3oRqHIGOQprYPSezctdnM7VPBZAkxQgCYY` on
2026-09-27, saved to `/tmp/deck-03-toolbox.txt` (104 lines, 5 slides, slide indices 0–4).

Every non-blank Anchor below verified with:

```
grep -o -F -- "<anchor>" /tmp/deck-03-toolbox.txt | wc -l   # deck-wide
<slide-slice> | grep -o -F -- "<anchor>" | wc -l            # in declared scope
```

This is a small index deck with exactly two known problems (per brief) — it stays short.
No padding, no manufactured rows.

## Rows

| Slide | Anchor | Current context | Proposed text | Category | Source |
|---|---|---|---|---|---|
| 2 | http://java.net | "Tools and utilities" slide — "Are developed in open source at " + run; the link has been dead for years | https://openjdk.org | correction | https://openjdk.org |
| 3 | vmstat, iostat, prstat | "Operating System tools and analysis" slide — *nix bullet, own run (with a trailing space, stripped by field-parsing — see Notes), followed by `etc` on the next run/line | vmstat, iostat, pidstat, ss, prstat | correction | — |

## Anchor verification

Verbatim output from `/tmp/deck-03-toolbox.txt` (`gslides.sh text`, 2026-09-27), using the
exact `grep -o -F | wc -l` method `deck-check.sh`/`deck-apply.sh` use. (Plain list, not a
table — this document's row table above is the only pipe-table in the file; both scripts
scan every line starting with `|`, so a second table would be misread as extra rows.)

```
http://java.net              slide 2  in-scope=1  deck-wide=1
vmstat, iostat, prstat        slide 3  in-scope=1  deck-wide=1
```

## Notes / exceptions

- **No `outside-scope-ok` rows.** Both anchors are unique deck-wide; no row needed the
  escape hatch.
- **No backticks in Anchor/Proposed text cells.** Both scripts take those fields
  literally (only leading/trailing whitespace is trimmed) — a markdown-style backtick
  around a value would be searched/written as a literal backtick character and the row
  would fail as `MISSING`. Confirmed the hard way: an earlier draft of this document wrapped
  both anchors in backticks and `deck-check.sh` reported `MISSING` for both rows even
  though the exact same strings, un-backticked, verify at count 1 above.
- **Row 3's anchor has no trailing space**, even though the live run's text is
  `vmstat, iostat, prstat ` (with one). The field-parsing in both scripts strips
  leading/trailing whitespace off every table cell, so a trailing space in the Anchor
  column can never survive into the actual match — per convention, never rely on a
  boundary space. This is safe here: the space sits *after* the anchor's last real word
  (`prstat`) and is not part of the match, so it is left untouched in the slide by the
  replace and still correctly separates `prstat` from the following `etc` run.
- **Row 2 wording.** The brief says "modernise the Linux set — `ss` has superseded
  `netstat`, and `pidstat` is worth naming." The current slide's `*nix` bullet does not
  mention `netstat` at all (there is nothing to replace on that count), so this row is
  purely additive: `pidstat` (per-process companion to `sar`) and `ss` (the modern
  socket-stats tool) are added to the existing `vmstat, iostat, prstat` list. `prstat`
  itself is Solaris-specific, not Linux, but it was left untouched — the brief's problem
  table lists it under "still broadly valid," not under the tools to add.
- **No source for row 2.** The brief's own problem table gives `—` as the source for this
  item (it is a tool-name modernization, not a version-number or behavioural claim), so no
  citation is asserted here either, per the content rule that only bare *version numbers*
  strictly require a citation.
- **Nothing else changed.** Slides 0, 1, and 4 need no edits — the brief names only these
  two problems, and the deck's brief says explicitly to keep it short and not pad it.
- **No manual actions.** Both changes are ordinary text substitutions; nothing structural
  (no new slides, no reordering) is needed on this deck.
