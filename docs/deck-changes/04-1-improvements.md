# Deck 4.1 — Infrastructure, architecture and code improvements

**STATUS: VERIFY DOCUMENT — dry-run only, nothing applied to the live deck.**

Source read: `gslides.sh personal text 1QCPKhQt46v04nDqR-it6fJKDQR28k2xVANOaL0arcj4` on 2026-09-27,
saved to `/tmp/deck-04-1.txt` (658 lines, 30 slides, indices 0–29). This deck had never been read
during design — no anchors existed before this pass. All rows below were derived from this live
dump, not from any prior brief text.

**Read-tool caveat**: the coding assistant's Read tool, on its first render of this dump, silently
dropped and reordered lines (slide 8's title and several bullets vanished; slide 13/14 content was
merged into garbage). Per the conventions doc's warning, every anchor and every row's "current
context" below was re-checked against the raw file with `sed -n` / `grep -F`, not eyeballed from
that Read-tool view.

## Rows

| Slide | Anchor | Current context | Proposed text | Category | Source |
|---|---|---|---|---|---|
| 15 | custom thread-pool | "[C] Async & parallel ops, whenever possible" slide, "Classes, APIs & libraries" bullet: "CompletableFuture, using the ForkJoinPool or a custom thread-pool" | custom thread-pool — or, since JDK 21, virtual threads for blocking, I/O-bound tasks, which need no pool sizing at all | correction | https://openjdk.org/jeps/444 |
| 17 | Threads | "[C] Other performance improvements" slide, resource-pooling bullet list: "Use resources pooling as often as possible - avoid creating & destroying reusable res:" → "Threads" / "Database connections" | Threads — platform threads; virtual threads (final since JDK 21) are cheap per task and are not meant to be pooled | correction | https://openjdk.org/jeps/444 |
| 27 | Tomcat thread pool | "'Look elsewhere, the DB is always(?) the bottleneck'" (contd) slide, "'reqs. per connections' impedance" bullet: "Tomcat thread pool: 200 threads" | Tomcat thread pool — a platform-thread pool; since JDK 21, virtual threads let each request run on its own thread instead | correction | https://openjdk.org/jeps/444 |
| 18 | Investing time in optimizing the database access  | "[C] Performance improvement practices" slide, practices bullet: "Investing time in optimizing the database access (read & write operations) - The main bottleneck (usually)" | Investing time in optimizing the database access (see Lab 2, an N+1-query hunt)  | addition | task-6b-brief.md checklist item "Caching or data-access advice → connect to lab 2" |

## Checklist results

- **Any JDK version claim** — no hits: the deck has no explicit Java/JDK version numbers anywhere
  (`grep -n -E "Java (8|9|1[0-9]|2[0-9])\b|JDK ?[0-9]+" /tmp/deck-04-1.txt` → no matches).
- **Thread-pool or concurrency advice** — 3 rows above (slides 15, 17, 27), all qualified for
  virtual threads per JEP 444.
- **Sizing advice keyed to host CPUs or RAM** — no hits: the only CPU mention in the deck is
  "Multi-core CPUs" as one of TornadoVM's acceleration targets (slide 20), not sizing advice; no
  RAM-sizing language anywhere.
- **Caching or data-access advice** — 1 row above (slide 18), connects the "database access is the
  bottleneck" practice bullet to Lab 2.
- **Named tools, products or URLs** — no hits proposed, and flagged as a concern: `gslides.sh text`
  exposes text runs only, not the underlying hyperlink URLs, so the "Source" link placeholders on
  slides 3, 7, 12 and 13 (and any other linked text) cannot be checked for dead links from this
  dump. The visible tool/product names — PMD, CheckStyle, SpotBugs (slide 14), TornadoVM (slide
  20), Project Reactor, RxJava (slide 15), Kubernetes, OpenShift (slide 7), Tomcat (slide 27) — are
  all still active products as of current knowledge, so no replacement rows are proposed. Needs a
  manual link check in the Slides editor.
- **Streams API performance claims** — no hits: Streams API appears twice (slide 15, listed as a
  parallel/async option; slide 17, listed as a functional-programming idiom) but the deck makes no
  explicit performance claim about it to reconcile against Deck 1. Deck 1 itself was not read
  (out of scope for this task), so consistency could not be checked either way — flagged as a
  concern, not assumed.

## Manual actions

1. **New slide — Lab 2 intro.** This deck owns Lab 2 (N+1 queries). New-slide insertion is
   structural and out of scope for the harness. Insert near the DB-bottleneck / practices material
   (around slides 18/26–28). Per the brief, it must describe the issue only, never the fix:

   ```
   Lab 2 - N+1 queries

   Symptom:  one request, hundreds of SQL statements
   Duration: 1 hour
   You get:  p6spy statement logs and a Gatling run
   Find:     which access path multiplies the statement count
   ```

## Notes

- Anchor uniqueness, verified against the raw dump (not the Read-tool rendering):
  - `grep -o -F -- "custom thread-pool" /tmp/deck-04-1.txt | wc -l` → `1`
  - `grep -o -F -- "Threads" /tmp/deck-04-1.txt | wc -l` → `1`
  - `grep -o -F -- "Tomcat thread pool" /tmp/deck-04-1.txt | wc -l` → `1`
  - `grep -o -F -- "Investing time in optimizing the database access " /tmp/deck-04-1.txt | wc -l` → `1`
- The row-18 anchor line ends with a trailing space before the next run, `(read & write
  operations)`, begins; the proposed text preserves that trailing space so the sentence still
  flows into the following run unchanged.
- No `outside-scope-ok` rows were needed — every chosen anchor is unique deck-wide, which is also
  the full scope here (single-slide-index restriction was not needed).
- No shorten-style / substring-deletion rows applied for this deck; all four rows are additive
  corrections, not deletions, so the "Proposed text is a substring of Anchor" refusal case does not
  apply here.
