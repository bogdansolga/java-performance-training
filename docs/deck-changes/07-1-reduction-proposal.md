# Deck 7.1 — slide-count reduction proposal

**STATUS: PROPOSAL — analysis only.** Nothing in this document is scripted or applied.
No slide has been touched, `deck-apply.sh` was not run, and `07-1-profiling-tools.md`
was not edited. This is a structural reduction proposal that goes further than the
"Shortening proposal" note already in `07-1-profiling-tools.md` (which identifies
slides 26, 33, 36, 40 for cut/merge and stops at ~40 slides).

Source of the slide-by-slide content: `gslides.sh personal text
1952R9NhvuYNuG9TujMfEPpy_w6cZ_yfMvl5zlZb9yoU` dumped to `/tmp/deck-7-1-now.txt` on
2026-09-24 (990 lines, 44 slides, indices 0–43; reflects the 6 scripted corrections
already applied to the live deck, per that file's STATUS line). All slide text below
was extracted from that dump with `awk`/`grep`, not eyeballed from a `Read` of the raw
file, per the environment's lossy-compression warning on large tool output.

Policy basis: `docs/superpowers/specs/2026-09-22-java-perf-labs-and-deck-refresh-design.md`
§6.4 ("every deck in range is reduced except the one the labs lean on hardest, which is
*expanded*, not cut") and §6.7 (deck 7.1's three replacement diagrams, and which slide
each one anchors). Feedback basis: participants wanted fewer slides, fewer tool
walkthroughs, more reproduce → investigate → fix with before/after results.

## 1. Slide-by-slide table (all 44 slides)

Verdict legend: **KEEP** (stays as its own slide), **MERGE-INTO-N** (content folds into
slide N, this slide disappears), **CUT** (dropped; see §3 for anything worth rescuing).

| # | One-line description | Verdict | Reason |
|---|---|---|---|
| 0 | Title slide | KEEP | Required. |
| 1 | JDK tool list (jcmd, jconsole, jhat, jmap, jinfo, jstack, jstat, visualvm) | KEEP | Reference index for the whole deck; `jhat` fix is a pending manual row, not a cut. |
| 2 | 6 tool categories (roadmap) | KEEP | One slide, orients the whole session. |
| 3 | Basic VM info via `jcmd` (uptime, system_properties) | KEEP (absorbs 4) | Has a `Hands-on →` cue; natural host for slide 4's commands. |
| 4 | Other VM info via `jcmd` (version, command_line, flags) | MERGE-INTO-3 | Same tool, same command family, same "Hands-on →" pattern as slide 3 — two slides for one list of `jcmd` one-liners. |
| 5 | Why tuning flags matter; command_line vs flags vs `-all` | KEEP | Hands-on slide, sets up 6–11. |
| 6 | `-XX:+PrintFlagsFinal` example output | KEEP | Concrete, hands-on, the only slide showing real flag output. |
| 7 | "Using the flag" — restates "hundreds of flags, used by support engineers" | MERGE-INTO-8 | No new content beyond slide 5; no `Hands-on →` cue (only front-matter slide without one except 9/12/16); pure restatement. |
| 8 | Colon vs no-colon meaning; `product` vs `pd product` | KEEP (absorbs 7) | Hands-on slide with real diagnostic content; natural host for 7's one sentence. |
| 9 | `manageable` / `C2 diagnostic` columns; intro to `jinfo` | KEEP | Defines "manageable," which slide 11's caveat depends on. |
| 10 | Using `jinfo -flags` / `-flag` | KEEP | Hands-on slide, concrete commands. |
| 11 | `jinfo` can *set* a flag but the JVM may ignore it | KEEP | Hands-on slide; this caveat is exactly the "practical problem-solving" content feedback asked for — don't cut it. |
| 12 | Quick summary — JVM info / flags section | KEEP | Genuine section-level wrap (11 slides), not a duplicate sub-summary like 26/33/36. |
| 13 | Thread information pointer (`jstack`, `jcmd Thread.print`) | KEEP (absorbs 14) | Hands-on slide; natural host for 14. |
| 14 | Class information pointer ("we'll study this further") | MERGE-INTO-13 | Two-line slide, no technical content of its own, same "pointer to later material" role as 13. |
| 15 | Live GC analysis via jconsole/jcmd/jmap/jstat | KEEP | Hands-on slide, distinct tool set. |
| 16 | Heap dump post-processing (visualvm, jcmd/jmap capture) | KEEP | Has a pending manual `jhat` correction; real content, no hands-on redundancy with neighbors. |
| 17 | Profiling tools — section intro | KEEP | One-slide framing for the 26-slide profiling run; needed. |
| 18 | Profiler list (paid/free), with pending JProbe/JFR corrections | KEEP | Tool *awareness*, not tool *walkthrough* — the feedback targets the latter. One slide is cheap. |
| 19 | How profilers attach (socket, heap sizing) | KEEP | Short, real operational caveat. |
| 20 | Sampling vs instrumented — definitions | KEEP | Foundational; diagram 1 (sampling-vs-instrumenting) depends on this framing existing first. |
| 21 | How sampling profilers work; the common error, described | KEEP | Sets up the worked example on 22; needed for diagram 1. |
| 22 | The common sampling error, worked (methodA/methodB bars) | KEEP | This *is* a worked example — cutting it fights the "more worked problem-solving" feedback, not serves it. Not GlassFish-specific. |
| 23 | Minimising sampling error — the interval/impact trade-off | KEEP | Short, real trade-off; not tool-walkthrough. |
| 24 | Sampling profile example — `defineClass1()` 19% | KEEP | **Diagram 1 anchor** (`7-1-01-sampling-vs-instrumenting.png` lands here per the change doc's manual actions). Protected. |
| 25 | Further analysis — the generic "top method may be 2–3%" lesson | KEEP | Protected: this is the deck's clearest transferable, non-tool-specific lesson — exactly what "more practical problem-solving" is asking for. |
| 26 | Quick summary — sampling | CUT | Already proposed in `07-1-profiling-tools.md`. Duplicates 12/41-style wrap-ups; content is fully covered by 24/25. |
| 27 | Instrumented profilers — one-sentence intro + diagram pointer | MERGE-INTO-28 | With its screenshot gone (deletion already proposed), this is 3 lines of pure transition; 28 is where the actual content is. |
| 28 | Apparent differences — `getPackageSourcesInternal()` 13%, invocation counts | KEEP (absorbs 27) | Concrete instrumented-vs-sampling contrast; feeds directly into 29. |
| 29 | Sampled vs instrumented — complementarity, `IM.get()` 4.7M calls | KEEP | **Diagram 3 anchor**, and per the design spec "the slide that carries the intent" — the single most protected slide in the deck. |
| 30 | Code-altering warning — instrumentation biases inlining | KEEP | The *other* reason profiler numbers mislead (bytecode rewriting), distinct from safepoint bias on 31. Short, no screenshot, no redundancy. |
| 31 | Safepoint bias — why `get()` is invisible to sampling | KEEP | **Diagram 2 anchor**; per the design spec, "the deck's strongest existing insight." Protected. |
| 32 | Conclusions — profilers are estimators, not oracles | KEEP (absorbs 33) | Real closing point for the sampling/instrumenting run. |
| 33 | "Instrumented profilers:" quick summary | CUT | Already proposed. Duplicates 32. |
| 34 | Blocking methods & thread timelines — `park()`/`parkNanos()`, 632s | KEEP (absorbs 35, receives 36's point) | Now the single "blocking methods" slide after the merges below. Keep the 632s figure — it's the one concrete number in this sub-run. |
| 35 | Further analysis — parked threads are normal at startup; filtering | MERGE-INTO-34 | **Carries a distinct, important point** (see §3) — must survive as a bullet on 34, not be silently dropped. |
| 36 | Quick summary — blocking methods | CUT | Already proposed. With 35 folded into 34, this has nothing left to summarize that 34 doesn't already say. |
| 37 | Native profilers — intro; async-profiler/perf correction pending | KEEP | Protected: this is the narrative home of async-profiler, one of the three protected tools. |
| 38 | Native profiling intro — 25.1s total / 20s in JVM-System | MERGE-INTO-39 | With its screenshot gone, this is now an intro line plus one stat; 39 is where that stat is actually used. |
| 39 | The GC time — GC-bound vs compiler-thread-bound, what to do about each | KEEP (absorbs 38, receives 40's point) | Protected: the most actionable "what do you do next" slide in the deck — textbook reproduce→investigate→fix framing. |
| 40 | The filtered native profiler — `inflateBytes()`, 0.67/5.041s ≈ 11% | CUT | Already proposed. Third, more granular pass over an already-deleted screenshot; **one insight worth rescuing, see §3.** |
| 41 | Quick summary — profiling section overall | KEEP | Distinct role from 26/33/36: this is the *deck-level* close (mirrors 12), and a natural lab-transition point per §6.6. Not a duplicate. |
| 42 | Further info / resource links (incl. pending JFR-free addition) | KEEP | Cheap, high-value takeaway slide; carries the corrected JFR link. |
| 43 | Q&A | KEEP | Standard close. |

## 2. Target count and cut sequence

**Current: 44 slides. Proposed target: 34 slides (−10, ≈23%).**

| Category | Count | Slides |
|---|---|---|
| KEEP | 34 | 0,1,2,3,5,6,8,9,10,11,12,13,15,16,17,18,19,20,21,22,23,24,25,28,29,30,31,32,34,37,39,41,42,43 |
| MERGE-INTO | 6 | 4→3, 7→8, 14→13, 27→28, 35→34, 38→39 |
| CUT | 4 | 26, 33, 36, 40 |

Apply in this order (each step is independent, so the trainer can stop at any point and
still have a coherent deck):

1. **CUT 26, 33, 36** — already-approved redundant sub-summaries (no content risk).
2. **CUT 40**, after moving its one salvageable point to slide 39 (§3).
3. **MERGE 27→28** and **MERGE 38→39** — both are "intro sentence + one stat" slides
   left thin once their screenshots are deleted per the pending manual actions; fold
   into the slide that actually uses the content.
4. **MERGE 35→34** — keep 35's "parked threads are normal at startup" point as an
   explicit bullet on the merged slide, not a casualty (§3).
5. **MERGE 4→3, 7→8, 14→13** — front-matter tightening, independent of the walkthrough
   cuts above. Preserve the `Hands-on →` cue from 4 and 14 on the merged slides (both
   carried one; see the note in §3).

Net effect on the two halves of the deck: the JVM-info/flags front-matter (slides 1–16)
goes from 16 to 13 slides; the profiling walkthrough (slides 17–42) goes from 26 to 19
slides. The walkthrough absorbs 7 of the 10 cuts/merges, which matches the feedback —
it is also the half of the deck with **zero** `Hands-on →` checkpoints (all 10 occur in
slides 3–16; none in 17–43), so it was already the more lecture-heavy, less
practice-anchored half before this pass.

### Stretch tier (not recommended without trainer sign-off)

If more than 34 is still too many, the next candidate is **merge slide 9 into 10**
(→33 slides): 9's "manageable"/"C2 diagnostic" definitions and jinfo intro could sit as
a preamble on 10's command slide. Marginal — 9 is short but not redundant, so I'm not
folding it into the main proposal.

Two things I looked at and am explicitly **not** recommending, because they'd cross from
"cut the walkthrough" into "cut the protected core":
- **Merging 30 into 31** would save one slide but crams two distinct failure modes
  (bytecode-rewrite bias, safepoint bias) onto the deck's single most protected slide.
- **Cutting 22** (the methodA/methodB worked example) looks like a walkthrough slide but
  is actually a worked example with no tool/screenshot dependency — cutting it would
  directly contradict the "more worked problem-solving" half of the feedback.

## 3. What the cuts would lose, and where to preserve it

- **Slide 40's `inflateBytes()` / JAR-decompression point.** This is the one place in
  the deck where a native profiler surfaces something a Java-based profiler structurally
  cannot see (I/O treated as a blocking call, filtered out of the CPU view). It exists
  only on this slide. **Preserve it as a one-line bullet added to slide 39** (its new
  merge host), stripped of the screenshot-tied numbers (`0.67s from 5.041s ≈ 11%`),
  keeping only the generic claim: "native profilers can also surface I/O/library time
  invisible to Java-based tools (e.g. ZIP/JAR decompression), since it's filtered out as
  a blocking call." This is consistent with the existing shortening note's own logic —
  screenshot-specific numbers are droppable, the generic lesson isn't.

- **Slide 35's "parked threads at startup are normal, not a bug" distinction.** This is
  a genuine false-positive-avoidance lesson (how to tell healthy blocking from a real
  problem) — exactly the diagnostic judgment the feedback wants more of, not less.
  Folding 35 into 34 must keep this as an explicit bullet on the merged slide, not let
  it evaporate into "blocked threads consume no CPU." Also keep 35's closing note that
  most profilers have filtering options for these threads — it's the actionable half of
  the point.

- **Slide 38's 25.1s / 20s JVM-System split.** The concrete number underpinning 39's
  GC-vs-compiler-thread argument. When 38 merges into 39, this stat becomes 39's opening
  evidence rather than being dropped for a vaguer "some time is in the JVM itself."

- **Slides 4 and 14's `Hands-on →` cues.** These two merged-away slides are two of the
  ten slides in the deck carrying an embedded hands-on checkpoint marker. Merging them
  into 3 and 13 must keep at least one `Hands-on →` cue on each resulting slide —
  losing the marker (as opposed to losing the slide) would silently remove a
  facilitation checkpoint from the session, not just tidy the deck.

- **Nothing of substance is lost from 26, 33, or 36** — verified each against its
  claimed duplicate (12/41, 32, 34 respectively): all three are pure restatement with no
  figure, tool name, or claim not already present on the slide they'd have summarized.

## 4. What was deliberately NOT cut, and why

- **The diagram-anchor sequence: 24 → 29 → 31.** These three slides exist to host the
  three new diagrams the change doc already commits to (`7-1-01-sampling-vs-
  instrumenting.png`, `7-1-03-complementarity.png`, `7-1-02-safepoint-bias.png`). Per
  the design spec, 29 is "the slide that carries the intent" and 31 carries "the deck's
  strongest existing insight." Cutting or merging any of the three would undercut the
  diagram investment this deck is already committed to.
- **Slides 20–23**, the sampling-mechanics run leading up to diagram 1. These aren't
  GlassFish/screenshot content — they're the conceptual setup diagram 1 needs to land,
  including a genuine worked example (22) rather than a screenshot walkthrough.
- **Slide 30**, the inlining/code-altering bias — the second (non-safepoint) reason
  profiler numbers can mislead. Short and non-redundant; kept standalone rather than
  folded into 31 to avoid diluting the protected slide (see Stretch tier).
- **Slide 39**, the GC-vs-compiler-thread slide — the most directly actionable
  "reproduce → investigate → fix" content in the deck (what to do next, conditioned on
  what the native profile shows). Strengthened, not cut, by absorbing 38 and 40's point.
- **Slide 37**, and the JFR/JMC/async-profiler thread running through 18, 29, 31, 37,
  39, 42. Per the framing for this pass, these three tools are protected *in the
  walkthrough narrative*, not merely as list entries on slide 18 — none of the proposed
  cuts touch a sentence where one of the three is doing explanatory work.
- **Slides 12 and 41**, the two section-level "quick summary" slides (as opposed to the
  three sub-topic summaries that are cut). Distinct role: deck-level retention aid and,
  per §6.6 of the design spec, a natural point for a future lab-intro slide to attach.
- **All ten `Hands-on →` slides (3–16 range)** were left otherwise untouched. This half
  of the deck is where the session's embedded practice already lives; cutting further
  into it would work against the "more practical problem-solving" half of the feedback,
  not for it.
- **Slide 18**, the profiler list. Reads as "tool walkthrough" at a glance, but it's a
  single reference slide (tool *awareness*), not a multi-slide *walkthrough* — the
  distinction the feedback and the task framing both draw explicitly.

## 5. On the 40–50% course-level overage

This pass gets deck 7.1 from 44 to 34 slides (≈23%), concentrated almost entirely in the
screenshot-driven walkthrough (17–42 loses 7 of its 26 slides) plus three genuinely thin
front-matter slides. Going meaningfully lower than 34 without a stretch-tier trainer
call means cutting into the diagram-anchor sequence, the JFR/JMC/async-profiler
narrative, the one worked example in the deck (22), or the ten hands-on checkpoints —
i.e. exactly the content this pass was told to protect. **Deck 7.1 alone cannot close a
40–50% course-level gap without giving up tool coverage the labs need; per design-spec
§6.4's "direction of travel," the remaining time has to come out of decks 2.x/3.2–3.4,
the same way 3.1 was protected (expanded, even) while its budget came from elsewhere in
the set.**
