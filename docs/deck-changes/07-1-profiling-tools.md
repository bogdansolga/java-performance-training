# Deck 7.1 — Java monitoring & profiling tools

**STATUS: DRAFT — not yet applied.** Steps 1–3 only (read, write, check). Step 4 (trainer approval
gate), Step 5 (`deck-apply.sh`), Step 6 (image placement), Step 7 (re-read / GlassFish sweep) and
Step 8 (README index update) intentionally **not done** by this pass — out of scope per Task 5
brief's scope limit. `deck-apply.sh` was never invoked; the live deck was never modified.

Source read: `gslides.sh personal text 1952R9NhvuYNuG9TujMfEPpy_w6cZ_yfMvl5zlZb9yoU` on
2026-09-24, saved to `/tmp/deck-7-1.txt` (990 lines, 44 slides, slide indices 0–43).

All anchors below were re-derived from the live read, not copied from the task-5 brief. Every
anchor in the brief turned out wrong in some way (non-existent verbatim phrasing, phrasing that
exists but is split across multiple text runs / output lines, or attached to the wrong slide). See
Notes for the full comparison.

Every non-blank Anchor below is verified with:

```
grep -o -F -- "<anchor>" /tmp/deck-7-1.txt | wc -l
```

printing exactly `1`. Verbatim output in `task-5-report.md`.

## Rows

| Slide | Anchor | Current context | Proposed text | Category | Source |
|---|---|---|---|---|---|
| 1 | | "Tools that come with the JDK" list — the `jhat` entry is two separate text runs: `jhat` (name) then `- reads and helps analyse memory heap dumps` (description) | Manual: delete both runs (tool name + description) — `jhat` was removed in JDK 9. | removal, manual | https://openjdk.org/jeps/241 |
| 16 | | "Processing" section — `JDK tools - including ` + `visualvm` + ` & ` + `jhat` (four runs); the bare `jhat` run is byte-identical to slide 1's occurrence, so a scripted whole-deck replace on `jhat` would hit both with the same text even though the two slides need different fixes | Manual: change to "JDK tools - including visualvm; for heap dumps use `jcmd <pid> GC.heap_dump`, then open in JMC or Eclipse MAT" — `jhat` was removed in JDK 9. | correction, manual | https://openjdk.org/jeps/241 |
| 18 | | Paid profilers list — `Java Flight Recorder` is its own line, between `Stackify Prefix` and `JProbe`; the identical substring also occurs, correctly, inside the slide-42 "Further info" link caption "Monitoring Java Applications with the Java Flight Recorder (JFR)" — a scripted replace on `Java Flight Recorder` would corrupt that link title too | Manual: move `Java Flight Recorder` from the Paid list to the Free list (alongside JMC, VisualVM, Async profiler). Free and open-source since JDK 11 (JEP 328); it was a commercial/paid feature only under Oracle JDK 8 — likely why this deck originally listed it as paid. Java 11 is a covered release, so it should read as free throughout. | correction, manual | https://openjdk.org/jeps/328 |
| 18 | JProbe | Paid profilers list — `JProbe` line, immediately followed by the deck's own doubtful parenthetical `(deprecated by the developing company?)` | JProbe — discontinued (no release since ~2008; not currently sold by Quest/Dell) | correction | https://jprobe.software.informer.com/8.3/, https://sumble.com/tech/jprobe (no official Quest/Dell end-of-life notice found; conclusion rests on the absence of any release since the 8.x line, ca. 2008) |
| 18 | | Paid profilers list — parenthetical right after `JProbe`: `(deprecated by ` + `the developing company` + `?)` (three runs) | Manual: delete the parenthetical "(deprecated by the developing company?)" — status is now confirmed inline on the `JProbe` line above, no need for the hedge. | removal, manual | n/a |
| 24 | A basic sampling profile: the startup of a GlassFish app server domain → shows that: | "A sampling profile example" — opening sentence of the walkthrough, followed by GlassFish-specific bullets (`defineClass1()` timings) and a live screenshot | How sampling attributes time to methods — see diagram | reduction | n/a |
| 27 | The same GlassFish startup profiling, using instrumented mode: | "Instrumented profilers" — opening sentence for the second half of the same walkthrough, with its own screenshot | Same workload, instrumented — see diagram | reduction | n/a |
| 34 | The GlassFish startup, using a different instrumented profiling tool - NetBeans profiler | "Blocking methods & thread timelines" — opening sentence, third screenshot (NetBeans profiler) | Blocked threads consume no CPU — park(), parkNanos(), read() | reduction | n/a |
| 37 | Only available for a few OS distributions | "Native profilers" — tabbed caveat line, describing the (superseded) Oracle Developer Studio native profiler | async-profiler and perf provide native visibility on Linux; async-profiler also supports macOS | correction | https://github.com/async-profiler/async-profiler |
| 37 | | "Native profilers" — `→ the GlassFish startup in ` + `Oracle Developer Studio` + ` → a native profiler` (three runs); Oracle Developer Studio is discontinued | Manual: rewrite as "→ native profiling of a Java process, using async-profiler or perf →". Oracle Developer Studio is discontinued; async-profiler samples without waiting for a safepoint, which is exactly the bias this deck describes on the safepoint slide (31). | removal, manual | https://github.com/async-profiler/async-profiler |
| 38 | | "Native profiling" — `The GlassFish startup profile, showed in the ` + `Oracle Developer Studio` + ` profiling tool` (three runs), plus a separate `Also works on Linux systems` run | Manual: rewrite the opening as "A native CPU profile, captured with async-profiler / perf" and delete "Also works on Linux systems" (redundant once the tool is Linux-native by default). This slide's screenshot is also being deleted — see Manual actions. | removal, manual | https://github.com/async-profiler/async-profiler |
| 42 | Monitoring Java Applications with the Java Flight Recorder (JFR) | "Further info" — closing resource-links slide, last link caption | Monitoring Java Applications with the Java Flight Recorder (JFR) — free since JDK 11 | addition | https://openjdk.org/jeps/328 |

12 rows: 5 scripted (`correction` ×3, `reduction` ×3 — note `reduction` rows carry no Source per
README convention; `addition` ×1), 7 `manual` (all `removal, manual` or `correction, manual`,
blank Anchor, un-automatable — multi-run spans or genuine anchor collisions with incompatible
required fixes).

## Notes / exceptions

- **Brief anchors, verified wrong:** all 5 of the brief's original rows had non-verbatim anchors.
  - Slide 1 `jhat`: brief's anchor "jhat - reads and helps analyse memory heap dumps" does not
    exist as a single string — it is two separate text runs on two separate output lines (`jhat`
    then `- reads and helps analyse memory heap dumps`, also missing the word "and" from the
    brief's paraphrase). Multi-run span — re-derived as a manual row.
  - Slide 18 `Java Flight Recorder`: brief's anchor text is accurate as a substring, but treating
    it as a simple scripted correction misses that the identical substring also occurs, correctly,
    in slide 42's link caption — a whole-deck replace would have corrupted that. Re-derived as
    manual with an explicit collision note.
  - Slide 16 `Heap dump post-processing`: brief pointed at the slide *title*, not a sensible
    attachment point for the jhat fix; the actual jhat mention on that slide is a bare `jhat` run
    later in the slide, which additionally collides byte-for-byte with slide 1's occurrence.
    Re-derived and forced to `manual` for that reason.
  - Slide 24/27/34 GlassFish captions: brief's anchors were close paraphrases ("basic sampling
    profile: startup GlassFish app server domain", "same GlassFish startup profiling, using
    instrumented mode", "GlassFish startup, using different instrumented profiling tool -
    NetBeans profiler") that needed minor wording restoration (missing articles/verbs) to become
    exact verbatim substrings, but were otherwise usable once corrected against the live read.
  - Slide 37/38 native profiling: brief's anchor "GlassFish available on only a few OS
    distributions" does not exist verbatim anywhere in the deck — the real, unique, single-line
    text is "Only available for a few OS distributions" (no "GlassFish" in it at all; it's a
    separate tabbed run). The brief also collapsed what are, on the live deck, two different
    slides (37 intro, 38 detail) each built from 3-run sentences mentioning `Oracle Developer
    Studio`, which occurs exactly twice and needs different surrounding rewrites on each slide —
    genuinely un-automatable as a single anchor/replacement pair.
  - **Net count: 0 of 5 brief anchor+text+source triples usable completely unchanged.** On this
    deck the paraphrase-vs-verbatim gap was total, not partial (contrast deck 4.2, where 2 of 7
    were usable near-verbatim).

- **GlassFish — full hit list and disposition.** Per the resolved instructions, ran
  `grep -in glassfish /tmp/deck-7-1.txt` after writing the rows above. Result: **exactly 5 hits**,
  all already covered:
  ```
  583:A basic sampling profile: the startup of a GlassFish app server domain → shows that:   (slide 24, row above)
  645:The same GlassFish startup profiling, using instrumented mode:                          (slide 27, row above)
  787:The GlassFish startup, using a different instrumented profiling tool - NetBeans profiler (slide 34, row above)
  864:→ the GlassFish startup in                                                               (slide 37, manual row above)
  876:The GlassFish startup profile, showed in the                                             (slide 38, manual row above)
  ```
  The brief's Step 7 claimed slides 35, 39 and 40 "carry further GlassFish prose that no row
  addresses" — **this is wrong**. A direct grep over the full live text shows zero GlassFish
  mentions on slides 35, 39 or 40 (confirmed by slide-boundary line ranges: 35 = lines 816–836,
  39 = lines 894–918, 40 = lines 918–949 — none contain the string, case-insensitive). No
  additional rows were needed beyond the 5 already covering the 5 real hits. Slide 40's screenshot
  is still slated for deletion (Manual actions) because it is part of the same now-superseded
  walkthrough (`defineClass1()` / `inflateBytes()` figures tied to the deleted screenshot), even
  though its caption text never says the word "GlassFish".

- **NetBeans**: only one occurrence in the deck (slide 34, covered by the row above); no other
  tool-specific branding of that kind elsewhere.

- **Paid-profiler product-status check (per resolved instructions):**
  - **JProbe** — discontinued. No release found since the 8.x line (~2008); download-aggregator
    listings (informer.com, soft112.com) are mirrors of old installers, not an active vendor page.
    No authoritative Quest/Dell end-of-life notice was found, so the correction row states this
    as "no release since ~2008" rather than citing a formal EOL date. See row above.
  - **XRebel** — still an actively maintained, current product. WebSearch (2026-09-24) shows
    XRebel 2026.3.1 (28 Jul 2026) and the Perforce/JRebel JetBrains Marketplace plugin at
    2026.3.2 (1 Sep 2026, 4.5M+ downloads), plus JDK 25 compatibility fixes in the 2025.4.x line.
    ZeroTurnaround → Rogue Wave (2017) → Perforce (2019); Perforce revived/re-released XRebel in
    2025 after a quiet period, which likely explains any impression it was discontinued. **No
    change proposed.** Sources: https://www.jrebel.com/products/xrebel/whats-new,
    https://plugins.jetbrains.com/plugin/4441-jrebel-and-xrebel
  - **Stackify Prefix** — still a current product. No source found announcing a 2025/2026
    discontinuation; Netreo (which acquired Stackify in 2021, later acquired by BMC in 2023)
    still lists "Prefix" and "Prefix Premium" as active offerings, and a 2026-dated GetApp listing
    describes it as current. **No change proposed.** Sources:
    https://stackify.com/netreo-launches-prefix-premium-real-time-application-profiler/,
    https://www.getapp.com/it-management-software/a/prefix/

- **Cross-row collision check.** The 5 non-blank, non-manual anchors used in this document are:
  `JProbe`; the 3 full-sentence GlassFish captions (slides 24, 27, 34); `Only available for a few
  OS distributions`; and the slide-42 link caption. Checked each Proposed text against every
  *other* row's Anchor: none of the 6 scripted rows' Proposed text contains another scripted row's
  Anchor as a substring. The only self-containment is the expected append-style case (`JProbe` row
  and the slide-42 row both carry their own Anchor as a prefix of their own Proposed text — by
  design, not a collision). `Oracle Developer Studio` (2 hits, lines 865 and 877) and the bare
  `jhat` run (2 hits, lines 21 and 415) are **not** used as scripted anchors anywhere in this
  document precisely because they are genuine collisions needing different fixes per occurrence —
  both are routed to manual rows instead. No `xN` tokens were needed in this document (no anchor
  is deliberately reused with an identical fix).

- **Screenshot replacement / image placement (manual, `gslides.sh` cannot insert or delete
  images):**
  - `docs/diagrams/7-1-01-sampling-vs-instrumenting.png` → slide 24, replacing the existing
    sampling-profile screenshot.
  - `docs/diagrams/7-1-02-safepoint-bias.png` → slide 31, replacing the prose-only safepoint
    explanation (no existing screenshot on that slide to delete).
  - `docs/diagrams/7-1-03-complementarity.png` → slide 29, replacing the prose-only
    sampled-vs-instrumented comparison (no existing screenshot on that slide to delete).
  - Delete the superseded GlassFish/tool screenshots on slides 24, 27, 34, 38 and 40. Note slide
    40 has no text row above (see GlassFish hit-list note) — its screenshot deletion is proposed
    purely because it is part of the same now-cut walkthrough, not because of a text correction.

- **Shortening proposal (course feedback: too many slides, too much tool-walkthrough).** The
  GlassFish/instrumented/native-profiler walkthrough runs slides 17–42 (26 slides) inside a
  44-slide deck. Beyond the row-level text edits above, recommend the trainer delete/merge these
  slides in the Slides editor (Step 6+, not scripted here):
  - **Slide 26** ("Quick summary" — sampling) — merge into slide 24/25; redundant with the
    deck-level "Quick summary" slides at 12/41.
  - **Slide 33** ("Instrumented profilers:" quick summary) — merge into slide 32 ("Conclusions");
    both are short recap slides for the same example.
  - **Slide 36** ("Quick summary" — blocking methods) — merge into slide 35.
  - **Slide 40** ("The filtered native profiler") — cut; it is a third, more granular pass over
    the same now-deleted native-profiler screenshot (Oracle Developer Studio), adding little
    beyond slides 38–39.
  - **Slide count: 44 today → a realistic ~40 after these 4 cuts/merges.** This is a conservative
    estimate — slides 25, 28, 29 and 35 also carry screenshot-specific figures (`19%`, `632
    seconds`, `4.7 million times`, etc.) that could be trimmed further once the screenshots are
    gone, but that is left to the trainer's judgement rather than proposed as a hard cut, since
    those figures still illustrate generically-true points (e.g. "the top method may be only
    2-3% of total time").

## Manual actions (for trainer, in Slides editor — not scripted)

1. **Slide 1** — delete the `jhat` tool entry (two runs: name + description). Removed in JDK 9.
   Source: https://openjdk.org/jeps/241
2. **Slide 16** — change "JDK tools - including visualvm & jhat" to "JDK tools - including
   visualvm; for heap dumps use `jcmd <pid> GC.heap_dump`, then open in JMC or Eclipse MAT".
   Source: https://openjdk.org/jeps/241
3. **Slide 18** — move `Java Flight Recorder` from the Paid list to the Free list. Free since
   JDK 11 (JEP 328); was a paid feature only under Oracle JDK 8. Source:
   https://openjdk.org/jeps/328
4. **Slide 18** — delete the stale parenthetical "(deprecated by the developing company?)" after
   `JProbe`, now superseded by the confirmed correction (row above).
5. **Slide 37** — rewrite "→ the GlassFish startup in Oracle Developer Studio → a native
   profiler" as "→ native profiling of a Java process, using async-profiler or perf →". Source:
   https://github.com/async-profiler/async-profiler
6. **Slide 38** — rewrite "The GlassFish startup profile, showed in the Oracle Developer Studio
   profiling tool" as "A native CPU profile, captured with async-profiler / perf"; delete the
   now-redundant "Also works on Linux systems" line.
7. **Image placement** (three trainer-approved diagrams from `docs/diagrams/`):
   - `7-1-01-sampling-vs-instrumenting.png` → slide 24 (replaces sampling screenshot)
   - `7-1-02-safepoint-bias.png` → slide 31 (adds diagram; no screenshot to remove)
   - `7-1-03-complementarity.png` → slide 29 (adds diagram; no screenshot to remove)
8. **Delete superseded screenshots** on slides 24, 27, 34, 38, 40.
9. **Shortening pass** — merge/cut slides 26, 33, 36, 40 (see Notes); realistic slide count after:
   ~40 (from 44).
