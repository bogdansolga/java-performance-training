# Deck 6.1 — Finding the largest heap objects

**STATUS: VERIFY DOCUMENT — dry-run only, nothing applied.**

This deck had never been read against the modernization checklist before this pass.
Source read: `gslides.sh personal text 1Rb3DA-XzvBavGVLNjGbIjv5zKwRsNdvXb_caRmk4Zds` on
2026-09-27, saved to `/tmp/deck-6-1.txt` (228 lines, 12 slides, indices 0–11). Every
anchor below was re-derived from this dump and verified with
`grep -o -F -- "<anchor>" /tmp/deck-6-1.txt | wc -l` printing exactly `1`, unless noted
otherwise. The task-7 brief's checklist column supplied intent, not verbatim text —
none of its phrases exist on-slide, so no brief phrases were usable as anchors verbatim
(0 of 6 checklist items had quotable text; see Notes).

## Checklist results

(Written as a list, not a table — this document's row-scanner treats any line
starting with `|` as a data row, so checklist results are kept out of pipe-table form.)

- **Any mention of `jhat`** — no hits: `jhat` does not appear anywhere in the deck.
- **Heap-dump capture instructions** — found (slide 3, `jmap -dump[:live]`). Confirmed
  `jmap` and its `-dump`/`-histo` options still ship and work unchanged on 17/21/25 — no
  correction needed.
- **Object-size / header discussion** — found (slide 8, class histogram with
  per-instance byte counts) — see manual row below (compact object headers).
- **Named tools or URLs — verify each still exists, replace dead links** — 7 links
  found (list in Notes) — all returned HTTP 200 on 2026-09-27. No hits: none dead,
  nothing to replace.
- **Any JDK version claim (11/17/21/25; JEP)** — no hits: the deck text contains no
  bare version numbers or JEP references anywhere.

## Rows

No automatable text-replacement rows. The one live-content gap found (compact object
headers) and the lab-1 intro slide requested in the brief are both new-slide, structural
work the harness cannot do — see Manual actions.

| Slide | Anchor | Current context | Proposed text | Category | Source |
|---|---|---|---|---|---|
| — | | — (new slide) | Manual: insert a new slide after slide 8 ("Class histogram sample") introducing compact object headers — see Manual actions #1 | addition, manual | https://openjdk.org/jeps/519, https://openjdk.org/jeps/450 |
| — | | — (new slide) | Manual: insert a new slide after slide 10 ("Analyzing memory usage in an IDE"), before the Q&A slide, introducing the retained-set lab — see Manual actions #2 | lab-slide, manual | — |

## Notes / exceptions

- **No brief phrases were quotable.** The task-7 brief's checklist column for deck 6.1
  is written as search intent ("Any mention of `jhat`", "Heap-dump capture
  instructions", …), not as text to find on-slide — and indeed none of it appears
  verbatim in the deck. All rows above were derived from actually reading the dump, per
  convention.
- **jhat / heap-dump instructions**: `grep -o -F -- "jhat" /tmp/deck-6-1.txt` → 0 hits.
  Slide 3 already documents `jmap -histo[:live]` and `jmap -dump[:live]` — both flags
  are unchanged and functional through JDK 25, so the brief's proposed `jhat` →
  `jcmd <pid> GC.heap_dump` correction does not apply here (there is nothing to
  correct); no row needed.
- **Links checked** (all HTTP 200 on 2026-09-27, via `curl -s -o /dev/null -w
  "%{http_code}"`):
  - slide 2: `https://docs.oracle.com/en/java/javase/11/docs/specs/jvmti.html#whatIs`
  - slide 3: `https://docs.oracle.com/javase/7/docs/technotes/tools/share/jmap.html`
  - slide 4: `https://docs.oracle.com/en/java/javase/11/docs/specs/jvmti.html`
  - slide 5: `https://docs.oracle.com/en/java/javase/11/docs/specs/jvmti.html#EventSection`,
    `https://docs.oracle.com/en/java/javase/11/docs/specs/jvmti.html#FunctionSection`
  - slide 6: `https://docs.oracle.com/en/java/javase/11/docs/specs/jvmti.html#IterateThroughHeap`
  - slide 10: `https://www.jetbrains.com/help/idea/analyze-objects-in-the-jvm-heap.html`

  None dead; none replaced. The slide-3 link resolves under an old `javase/7` path but
  still serves current `jmap` documentation and returns 200 — left as-is (not a "dead
  link", so out of the brief's stated action).
- **Version numbers verified before use**: JEP 519 ("Compact Object Headers") is a
  product feature as of release **25** (was experimental in JDK 24 via JEP 450); JEP 450
  states 64-bit object headers occupy 96–128 bits today, compactable to 64 bits; JEP 519
  cites a SPECjbb2015 benchmark setting using ~22% less heap space with compact headers
  enabled. All three figures checked directly against the JEP text on 2026-09-27, not
  taken from the brief on faith.
- **Compact object headers has no safe automatable anchor.** It is new content (the
  deck currently contains no header/object-layout discussion to extend), so per
  convention ("Structural work... impossible") it is a manual new-slide row, not a
  text-replace row.
- **Lab 1 intro slide placed in 6.1, not 6.2.** See task-7-brief: 6.1's checklist
  explicitly asks for this row (no conditional); 6.2's brief instead asks "if 6.1 is not
  the better home." Deck 6.1 is specifically about finding/analyzing heap objects
  (histograms, heap dumps, IDE memory views) — a "find the retained set in a live heap
  dump" lab is a direct continuation of that material and of the deck's own heap-dump
  tooling (slide 3, slide 10), whereas deck 6.2 is about leak *causes* (ThreadLocal,
  static fields, equals/hashCode, etc.), not dump analysis technique. Placed only here;
  06-2-memory-leaks.md cross-references this decision and does not duplicate the row.
- **Observation, not a row (outside the brief's checklist, flagging for trainer
  attention only):** slide 9 documents an `-Xdump:stack:events` JVM flag. `-Xdump` is an
  IBM/OpenJ9 (Eclipse OpenJ9) diagnostic option family, not a HotSpot flag — it will not
  work on the Oracle/OpenJDK HotSpot builds this course otherwise targets. This wasn't
  one of the brief's checklist items ("Any JDK version claim", "Named tools or URLs")
  so no row was created for it per the "don't rewrite what wasn't asked" scope rule, but
  it likely needs a trainer decision (correct the flag, or state the slide is
  HotSpot-only/OpenJ9-only).

## Manual actions (for trainer, in Slides editor — not scripted)

1. **New slide after slide 8** ("Class histogram sample") — add a slide covering
   compact object headers: `-XX:+UseCompactObjectHeaders`, a product feature since JDK
   25 (experimental in JDK 24). Shrinks the 64-bit HotSpot object header from 96–128
   bits down to 64 bits; one SPECjbb2015 benchmark setting showed ~22% less heap used
   with it enabled. Version badge: `[25]` only (product feature only on 25; experimental
   on 24 is out of this course's covered-release set). Source: JEP 519
   (https://openjdk.org/jeps/519), JEP 450 (https://openjdk.org/jeps/450).
2. **New slide after slide 10** ("Analyzing memory usage in an IDE"), before the Q&A
   slide — lab 1 intro: state the exercise only, not the fix. Suggested content: "In
   this lab you'll take a live heap dump and identify its *retained set* — the objects
   an unexpectedly-large object graph is keeping alive and preventing the GC from
   reclaiming. Use the tools from this section (jmap, JMX heap histogram, or your IDE's
   memory view) to find what's holding the memory." Do not describe the fix/answer on
   this slide — that belongs in the lab walkthrough, not the intro.
3. Both new slides shift the Q&A slide from index 11 to 13; no other row in this
   document depends on slide indices past 10, so no renumbering conflicts.

## Verify

```
./scripts/deck-check.sh docs/deck-changes/06-1-heap-objects.md 1Rb3DA-XzvBavGVLNjGbIjv5zKwRsNdvXb_caRmk4Zds
./scripts/deck-apply.sh docs/deck-changes/06-1-heap-objects.md 1Rb3DA-XzvBavGVLNjGbIjv5zKwRsNdvXb_caRmk4Zds --dry-run
```

Both rows above have blank anchors (manual/structural), so both scripts skip them; this
document has no automatable rows to verify anchor-uniqueness for. Both commands should
exit 0 with 0 automatable rows processed.
