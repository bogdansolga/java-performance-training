# Deck 6.2 — Memory leaks, understanding and troubleshooting them

**STATUS: VERIFY DOCUMENT — dry-run only, nothing applied.**

Source read: `gslides.sh personal text 1klwuS0Hk6atCBY6XVfQDHMz4icJHQVQ224Vc6v_rkAo` on
2026-09-27, saved to `/tmp/deck-6-2.txt` (423 lines, 19 slides, indices 0–18). Read in
full per brief instruction. It is mostly correct and largely timeless — `ThreadLocal` on
pooled threads, unclosed resources, `static` fields, `equals()`/`hashCode()`, inner
classes are all sound as written and are **not** touched below. Both of the brief's two
known-correction phrases exist verbatim on-slide (2 of 2 quoted phrases usable — see
Notes for why neither could be used as a literal anchor unmodified).

## Rows

| Slide | Anchor | Current context | Proposed text | Category | Source |
|---|---|---|---|---|---|
| 17 | Avoid using (/ implementing) the | "Avoiding memory leaks" slide, recommendations list, bullet: "Avoid using (/ implementing) the *finalize()* method" | finalize() is deprecated for removal since JDK 18 — use java.lang.ref.Cleaner instead. Avoid implementing the | correction | https://openjdk.org/jeps/421 |
| 17 | Use the latest LTS version of Java | Same slide, recommendations list, first bullet: "Use the latest LTS version of Java (if applicable in your context)" | Use the latest LTS Java version — 17, 21 or 25 | correction | https://www.oracle.com/java/technologies/java-se-support-roadmap.html |
| 8 | preventing the object from being garbage collected | `ThreadLocal` (continued) slide, closing line: "...then a copy of that object will remain on the worker Thread even after the web app is stopped → preventing the object from being garbage collected" | preventing the object from being garbage collected. This pooled-thread hazard changes shape with virtual threads (final since JDK 21): they are not pooled, so a virtual thread does not keep a ThreadLocal alive this way | addition | https://openjdk.org/jeps/444 |

## Notes / exceptions

- **Both brief phrases exist verbatim but neither is a usable single-run anchor as
  quoted.** `finalize()` itself is styled as its own text run and occurs 8 times
  deck-wide (not unique); the sentence "Avoid using (/ implementing) the finalize()
  method" spans three separate text runs (per `gslides.sh text`, each run is its own
  output line), and anchors cannot span runs. I used the lead-in run alone — "Avoid
  using (/ implementing) the" (trailing space trimmed) — which is unique deck-wide
  (`grep -o -F -- "Avoid using (/ implementing) the" /tmp/deck-6-2.txt | wc -l` → `1`)
  and lets the unchanged `finalize()` and `method` runs complete the sentence after
  replacement, reading: "finalize() is deprecated for removal since JDK 18 — use
  java.lang.ref.Cleaner instead. Avoid implementing the finalize() method".
- **"Use the latest LTS version of Java"** is a single run and matched verbatim;
  confirmed unique (`grep -o -F` → `1`).
- **finalize() anchor verification**:
  `grep -o -F -- "Avoid using (/ implementing) the" /tmp/deck-6-2.txt | wc -l` → `1`.
- **LTS anchor verification**:
  `grep -o -F -- "Use the latest LTS version of Java" /tmp/deck-6-2.txt | wc -l` → `1`.
- **ThreadLocal/virtual-threads addition anchor verification**:
  `grep -o -F -- "preventing the object from being garbage collected" /tmp/deck-6-2.txt | wc -l`
  → `1`. Chosen as an append-style addition (extends the existing closing sentence of
  the slide 8 bullet, does not create a new paragraph/bullet), so it is automatable —
  not a manual/structural row.
- **Version numbers verified before use, not taken from the brief on faith**: JEP 421
  ("Deprecate Finalization for Removal") — release **18**, confirmed by fetching the JEP
  text on 2026-09-27. JEP 444 ("Virtual Threads") — release **21**, Final, confirmed the
  same way. The Oracle LTS roadmap and both JEP URLs returned HTTP 200.
- **No `shorten-style` conflicts**: none of the three Proposed-text values above is a
  substring of its Anchor, so none is refused by the harness for that reason.
- **Rest of the deck confirmed sound, not rewritten**: read slides 1–16 and 18 in full.
  `ThreadLocal` (slides 7–8), unclosed resources / try-with-resources (slide 6), heavy
  `static` field usage (slide 9), `equals()`/`hashCode()` (slides 10–12), inner classes
  (slides 13–14) all match current JDK behavior on 11/17/21/25 with no version-specific
  drift — no rows proposed for them, per the brief's "do not rewrite what is already
  correct" instruction.
- **Lab 1 intro slide is not proposed here.** Per the brief, this decision is made once
  and stated in one deck only. It belongs in deck 6.1 (`06-1-heap-objects.md`, Manual
  actions #2): that deck's own checklist unconditionally requests it, and its subject
  (heap-dump/retained-set analysis tooling) is the natural home, whereas 6.2 covers leak
  *causes*, not dump-analysis technique. See `06-1-heap-objects.md` Notes for the full
  rationale. Not duplicated here.

## Manual actions (for trainer, in Slides editor — not scripted)

None. Every change identified for this deck (both corrections and the one addition) is
a same-paragraph text edit the harness can automate; no new slides, reordering, or
paragraph-list restructuring needed here.

## Verify

```
./scripts/deck-check.sh docs/deck-changes/06-2-memory-leaks.md 1klwuS0Hk6atCBY6XVfQDHMz4icJHQVQ224Vc6v_rkAo
./scripts/deck-apply.sh docs/deck-changes/06-2-memory-leaks.md 1klwuS0Hk6atCBY6XVfQDHMz4icJHQVQ224Vc6v_rkAo --dry-run
```

All three rows are automatable and slide-scoped (17, 17, 8); each anchor's deck-wide
occurrence count matches its declared scope (1 in, 0 out for all three), so no
`outside-scope-ok` marker is needed.
