# Deck 4.2 — JIT compiler, pass 3 (simplify the 32-bit annotation)

**STATUS: DRAFT — not yet applied.**

Source read: `gslides.sh personal text 1aLuZ5zUYFn-IeoHERu4HdraXmSG3-XP_C6vKI2_oG0o` on 2026-09-27,
31 slides. Note the deck gained the AOT slide at 0-based index 13, so every later slide shifted
by one from earlier documents.

## Why

Pass 1 annotated the flag list inline:

> A 32-bit client version **[dead since JDK 9/10 — -d64 removed, -client a no-op on any 64-bit JVM]**

Accurate but unspeakable — nobody retains that from a slide. Two changes replace it with one short
qualifier on the heading, and restore the bullet to its original plain form.

The distinction pass 1 was protecting still holds and is not lost: the **port** history stays badged
`[11 · 17]` further down the slide, while the **flags** die much earlier. Putting the flag cut-off
on the heading says that once, in four words, instead of in the middle of a bullet.

`-d64` was removed in JDK 10 (deprecated in 9); `-client` is a no-op on any 64-bit JVM. So these
flags are dead on **every** covered release — 11, 17, 21 and 25 — which is why no version badge
applies to them.

## Rows

| Slide | Anchor | Current context | Proposed text | Category | Source |
|---|---|---|---|---|---|
| 14 | Three versions of the JIT compiler | "Java & JIT compiler versions" — the heading above the flag list | Three versions of the JIT compiler (historical — these flags are gone since JDK 10) | correction | https://www.oracle.com/java/technologies/javase/10-relnote-issues.html |
| 14 | [dead since JDK 9/10 — -d64 removed, -client a no-op on any 64-bit JVM] | Same slide — first bullet of the flag list; this is pass 1's inline annotation, which is being removed so the bullet reads plainly again | <DELETE> | removal | https://www.oracle.com/java/technologies/javase/10-relnote-issues.html |

## Notes

- Both anchors are scoped to slide 14 (0-based) — UI slide 15.
- The second row **deletes** the annotation rather than shortening the bullet. A first draft used
  the whole bullet as the anchor with the shortened text as the replacement, and `deck-apply.sh`
  correctly refused it: `A=1, R=1`, neither already-applied nor safe-to-apply. The replacement was a
  prefix of its own anchor, so it already occurred once. **The harness handles append-style rows
  (replacement contains anchor) but has no rule for shorten-style rows (anchor contains
  replacement)** — anchoring the removed fragment alone sidesteps it entirely and is clearer anyway.
- Row order matters only in that neither row's replacement contains the other's anchor — verified.
- The `[11 · 17]` badge on the heap-size advice further down the same slide is untouched.
