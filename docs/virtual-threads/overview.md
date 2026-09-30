# Virtual threads - what they change for thread pools and connection pools

Follow-up to deck 4.1 (thread pools, "the DB is always the bottleneck") and to the lab fixes.
Virtual threads are final since JDK 21. In Spring Boot they are one switch:
`spring.threads.virtual.enabled=true`.

## What changes

| | Platform threads (before) | Virtual threads |
|---|---|---|
| Request threads | Tomcat pool, 200 by default (this app: 20) - the pool size caps concurrent requests | one new virtual thread per request, no pool, no cap |
| `@Async` / scheduling | a `ThreadPoolTaskExecutor` / `ThreadPoolTaskScheduler` you size | Spring Boot switches both to virtual threads and ignores their pool sizes |
| Your own `Executors.newFixedThreadPool(n)` | the way to run blocking work in parallel | still platform threads - replace with `Executors.newVirtualThreadPerTaskExecutor()` |
| Database connection pool (Hikari) | sized to what the database can take | **unchanged** - it is now the only limit left |

## Benefits

- **Waiting no longer ties up a thread.** While a request waits on the database, a REST call or a queue, the
  same CPU serves other requests. Services that mostly wait handle far more requests at once on the same machine.
- **No thread-pool sizing.** "How many Tomcat threads?" goes away; virtual threads are cheap and never pooled.
- **Plain blocking code stays.** You get the scalability of reactive code without rewriting to
  `CompletableFuture` chains or reactive types, and stack traces stay readable.
- **Cheap waiting.** Thousands of requests waiting on a lock, a socket or a queue cost heap memory, not OS threads.

## Drawbacks

- **The limit moves to the connection pool.** Without the thread limit, far more requests reach Hikari at once.
  The pool does not grow with them: requests wait for a connection and fail with `connectionTimeout` instead of
  waiting in Tomcat. Keep the pool sized to what the database can take, and limit concurrency on purpose where
  needed (a `Semaphore` in front of a scarce resource).
- **Spikes are no longer slowed down.** A fixed thread pool also capped memory, sockets and calls to other
  services. Without it, a traffic spike reaches every downstream service at full size.
- **Locks on Java 21-23 ("pinning").** A virtual thread that waits inside `synchronized` holds on to its CPU
  core, so at most as many requests as there are cores make progress. Java 24 and newer fix this for
  `synchronized`; calls into native code still pin on every version. Older libraries, JDBC drivers included,
  that use `synchronized` are the usual cause.
- **CPU-heavy work gains nothing.** Virtual threads help waiting, not computing.
- **`ThreadLocal` caches stop being caches.** Every task gets a new thread, so anything cached per thread
  (formatters, buffers) is rebuilt on every request.
- **More heap per request.** Each virtual thread keeps its stack on the heap: more requests waiting at once
  means more heap in use and more GC work.
- **Different tools for thread dumps.** `jstack` does not show virtual threads; use
  `jcmd <pid> Thread.dump_to_file -format=json <file>`, and the JFR event `jdk.VirtualThreadPinned` to find pinning.

## In our course

`PinningSimulation` (200 users, each request blocks 50 ms inside `synchronized`), Oracle JDK on a 36-core Mac Studio:

| Run | Requests/s | p95 |
|---|---|---|
| Platform threads (20 Tomcat threads) | 364 | 548 ms |
| Virtual threads, JDK 21 (pinned) | 256 | 789 ms |
| Virtual threads, JDK 25 | 3180 | 64 ms |

Virtual threads help when the code waits without holding a lock, and the database has the capacity to match.
