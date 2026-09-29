# Lab issues

Five problems that show up again and again in production Java services. Each lab reproduces one of them in this application, so you can find it with the tools from the course. This page describes the problem and the symptoms, not the way out.

How to switch a lab on and run it: [gatling-labs.md](gatling-labs.md) (start the application with the lab's profile, then run its Gatling simulation). The profile and JVM options are in the table there.

## 1. Data that is kept forever (memory leak)

- **What is it?** A service keeps every object it ever created, for example an audit trail of every price quote, in a map that is never trimmed.
- **Why does it happen?** Keeping "just in case" data is easy to add and hard to notice: nothing fails, the map only grows. Caches, audit lists and listener registries without a size or age limit are the usual places.
- **When does it show up?** After hours or days of steady traffic, not in a short test. In the course: deck 6.1 (finding the largest heap objects) and 6.2 (memory leaks), after you have seen how to take a heap dump.
- **Where do you see it?** Heap used after a full GC keeps rising, GC runs more and more often, and eventually the process ends with `OutOfMemoryError`. Tools: `jstat`/GC log, heap dump and its dominator tree, JFR old-object samples. The lab also reports how many quotes it retains at `/lab/retention/stats`.
- **Who notices?** Operations first (memory alerts, restarts), then users (slow responses while the GC struggles, then errors).
- **Switch it on:** profile `unbounded-retention`, `-Xms256m -Xmx256m -XX:+UseG1GC`, `RetentionSimulation`.

## 2. One request, hundreds of SQL statements (N+1 queries)

- **What is it?** Loading a list of entities and then touching each one's children makes the ORM run one query for the list plus one more per parent, per child of that parent, and so on.
- **Why does it happen?** Lazy loading hides the queries: the code looks like plain getter calls. It is fast with the ten rows on a developer's machine.
- **When does it show up?** As soon as the data set grows or the database is no longer on the same machine. In the course: deck 4.1 (infrastructure, architecture and code improvements), at the point where data access is discussed.
- **Where do you see it?** Response time grows with the amount of data while CPU stays low; the database shows a flood of tiny identical queries. Tools: SQL log (p6spy is on in this lab), Hibernate statistics, a slow-query view of the database. The lab reports statements per request at `/lab/nplus1/stats`.
- **Who notices?** Users (a page that gets slower every month), the DBA (connection and query load), and the team that pays for the database.
- **Switch it on:** profile `n-plus-one-queries`, `-Xms512m -Xmx512m -XX:+UseG1GC`, `NPlusOneSimulation`.

## 3. Requests queuing behind one lock (contention)

- **What is it?** Every request has to pass through one `synchronized` section, and inside it the thread waits for something slow, here a 10 ms call to a remote compliance service.
- **Why does it happen?** A single lock is the simplest way to keep shared state consistent, and it is correct. The cost only appears when many threads want it at once, and the more slowly the holder works, the longer everybody waits.
- **When does it show up?** Under concurrent load, never in a single-user test. Adding CPUs or threads does not help. In the course: the threading and synchronization topic, after the toolbox decks on profiling.
- **Where do you see it?** Latency far above the work each request does, throughput flat while CPU is nearly idle. Tools: thread dump (many threads `BLOCKED` on the same monitor), JFR *Monitor Blocked* events, `/lab/contention/stats` (blocked count of the HTTP threads).
- **Who notices?** Users (slow responses in busy hours), and the on-call engineer who sees idle CPUs and a slow service.
- **Switch it on:** profile `lock-contention`, `-Xms256m -Xmx256m -XX:+UseG1GC`, `ContentionSimulation`.

### Extra run: waiting inside a lock with virtual threads

The same profile has a second endpoint, `/lab/contention/pinned`: each request waits 50 ms inside a `synchronized` block. Run `PinningSimulation` three times and compare requests per second and p95: with the application started normally, then with `--spring.threads.virtual.enabled=true` on JDK 21, then with it on JDK 24 or newer. Only the `java` that starts the application matters. The point is to compare the three results and explain the difference.

## 4. The wrong garbage collector for the job (GC mismatch)

- **What is it?** A service that builds large short-lived objects while it holds a big live set, running on the collector it happened to get (here Serial).
- **Why does it happen?** The collector is picked once, often by the JVM itself from the machine size, and nobody revisits it. Containers with few CPUs and small memory push the JVM towards Serial.
- **When does it show up?** When the live heap gets big enough for a full collection to take noticeable time, that is, months after the code was written. In the course: deck 5.2 (choosing a GC algorithm), after the introduction in 5.1.
- **Where do you see it?** Most requests are fast but the slowest ones (p99) are several times slower; the GC log shows stop-the-world pauses and full collections. Tools: GC log (`-Xlog:gc*`), JFR GC events, `/lab/gc/stats` (collections and time per collector).
- **Who notices?** The users who hit the pauses, and anyone with a latency target on the 99th percentile.
- **Switch it on:** profile `gc-mismatch`, `-Xms1g -Xmx1g -XX:+UseSerialGC`, `GcSimulation`.

## 5. Big responses under a small heap (humongous allocations)

- **What is it?** Each request allocates one very large object, here a 6 MB response body, in a JVM using G1 with a heap of about 1 GB.
- **Why does it happen?** G1 splits the heap into equal regions, about 1 MB each for a heap this size. An object bigger than half a region is not allocated in the young generation but in its own contiguous regions of the old generation. Endpoints that return whole files, exports or reports build such arrays without anybody noticing.
- **When does it show up?** With a modest heap, typically a container with a memory limit, and enough traffic of large responses. It is hard to reproduce on a developer laptop with a large default heap. In the course: deck 5.1 (introduction to GC) and 5.3 (basic GC tuning).
- **Where do you see it?** GC log lines with the cause *G1 Humongous Allocation*, repeated GC cycles that free little, memory that saws up and down, and in a container, the process being killed. Tools: `-Xlog:gc*`, JFR, `/lab/humongous/stats` (collections caused by large allocations).
- **Who notices?** Operations (restarts, out-of-memory kills), and users (slow or failed requests).
- **Switch it on:** profile `humongous-allocations`, `-Xms1g -Xmx1g -XX:+UseG1GC`, `HumongousSimulation`; the load goes to `GET /product/humongous`.
