# Virtual threads - quick intro

**What they are:** a lighter kind of Java thread, available from Java 21. In Spring Boot you switch them on
with one property: `spring.threads.virtual.enabled=true`.

**Why they matter:** most requests spend their time *waiting* - for the database, another service or a queue.
With classic threads, every waiting request keeps a thread busy, and the server has a fixed number of them
(Tomcat: 200). When all are waiting, the next request stands in line even though the CPU is idle.
A virtual thread gives its place back while it waits, so thousands of requests can wait at the same time.

## Benefits

- **More requests at once** for services that mostly wait: database calls, REST calls, message queues.
- **No more guessing thread-pool sizes.**
- **Your code stays the same.** Normal step-by-step Java; no need to rewrite it in a reactive style.

## Watch out for

- **The database does not get faster.** More requests now reach it at the same time. If the connection pool
  has 10 connections, the rest wait for one and can time out. The limit moved; it did not disappear.
- **Traffic spikes pass straight through.** The old thread limit also slowed down a spike; now every
  service behind yours gets the full load.
- **Locks on Java 21.** Waiting inside a `synchronized` block blocks a whole CPU core. On Java 21 this can be
  *slower* than before (see the numbers below). Java 24 and newer fix it.
- **No help for heavy computing.** Parsing big documents or calculating prices speeds up only while the code
  waits, not while it computes.
- **Memory.** Every waiting request still uses memory. Thousands at once means a bigger heap - worth watching
  if a service has had `OutOfMemoryError`s before.

## Good fit / poor fit

- **Good fit:** web services, services that call a database or other services, message consumers.
- **Poor fit:** CPU-heavy batch jobs, and code that holds a lock around a slow call (on Java 21).

## Measured in this course

200 users, each request waits 50 ms inside a `synchronized` block:

| Run | Requests/s | p95 |
|---|---|---|
| Classic threads (20 Tomcat threads) | 364 | 548 ms |
| Virtual threads, Java 21 | 256 | 789 ms |
| Virtual threads, Java 25 | 3180 | 64 ms |

More detail: [overview.md](overview.md).
