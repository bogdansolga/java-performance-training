# Kubernetes / production context — participant pre-call

**Date of call:** 2026-09-24
**Status:** captured, not yet actioned. Working notes for a later pass.

Notes from a pre-call with some of the participants. Original Romanian notes are preserved
verbatim in the appendix; the body below is the structured reading plus a first pass at mapping
each reported problem onto the training.

---

## 1. Their environment

| | |
|---|---|
| Language / stack | Java, Spring Boot |
| Architecture | Several microservices, plus one legacy application from the 2000s |
| Platform | GCP — Kubernetes and Cloud Run. Previously OpenShift; the migration did not change the performance problems |
| Messaging | Solace queues. No UI on the application under test |
| Databases | Oracle on-prem (monolithic, shared by several services), SQL Server in GCP, 3 databases reachable over ODBC with much redundant data. Postgres migration wanted, not started |
| Local setup | mini-k8s, Solace, Oracle |
| Perf tooling | A **custom Java 8 harness**, in use for years. Not JMeter, not Gatling |
| Scale | Many pods in production — **64**, driven by the Spring Boot app's memory consumption |

Cost is a live constraint: GCP hosting is expensive, which is why they moved off Oracle in the
cloud to SQL Server and why Postgres is wanted.

## 2. What they asked for

- Debugging **memory and OOM**, specifically for Kubernetes: how to configure, and how to test whether the allocated resources are actually sufficient.
- **Throughput** — requests, and the processing speed of a single request.
- How to find out **how much a given machine can take** — "how many transactions could my laptop support".

## 3. The two concrete production cases

These are the most valuable part of the call. Both are real, reproducible-in-principle failures,
which is exactly what the course feedback asked for.

### Case A — humongous objects in G1 on Cloud Run

Two applications, a few microservices, deployed to Cloud Run. Symptom: sawtooth lines in the
logs, container repeatedly killed.

- Container capped at **2 GB**.
- Responses were **5–6 MB** each.
- G1's default region size at that heap works out around **1 MB**.
- An object larger than half a region is a **humongous** allocation — it bypasses the normal path, is allocated directly into old gen across contiguous regions, and is only reclaimed at specific points.
- Memory filled, the container was configured to be killed around 90% usage, leaving roughly 200 MB of headroom, so it died quickly.
- **Hard to reproduce locally** — which is the whole difficulty.

This is a textbook G1 humongous-allocation problem and the fix is usually
`-XX:G1HeapRegionSize` raised to 8m or 16m, or shrinking the responses. It is also a near-perfect
teaching case: small cause, dramatic symptom, invisible without the right tool.

### Case B — resource exhaustion under load, no recovery after restart

- A performance test sent **10,000 transactions** through the Spring Boot wrapper via Solace.
- Everything became unreachable. Resources were exhausted.
- **Even a restart did not restore normal operation.**
- The stated want: to see what happened *inside the JVM*, and how to avoid it.

The "did not recover after restart" detail is the interesting one — it points away from a simple
heap exhaustion and toward something that survives the process, such as queue backlog, connection
or thread pool starvation upstream, or the database being the real bottleneck.

## 4. Mapping onto the current training

| Their problem | Covered by | Confidence |
|---|---|---|
| G1 humongous objects (Case A) | Deck 5.1 / 5.3 and **lab 5 (GC mismatch)** | Partial — humongous allocations are **not currently mentioned in any deck** |
| Container memory limits, OOMKilled | Deck 3.2 container additions (`UseContainerSupport`, cgroup limits) | Planned, not yet written |
| "Is 2 GB enough?" sizing | Deck 5.3 heap sizing | Partial — the deck sizes a heap, it does not size a *container* |
| Throughput and per-request time | Decks 2.2, 3.2 | Good |
| Memory / OOM debugging | Decks 6.1, 6.2 and **lab 1** | Good |
| Async / queue-based perf testing | — | **Gap** — the whole course assumes an HTTP request/response shape |
| Database as the real bottleneck | Deck 4.1 and **lab 2 (N+1)** | Partial — lab 2 is ORM-shaped, theirs is ODBC and legacy |

## 5. Gaps this call exposes

1. **Humongous objects are absent from the deck set.** Given a participant has already been burned by them in production, this should be added to deck 5.1 or 5.3. It is also a strong candidate for a **sixth lab**, or an extension of lab 5 — a defect profile that allocates oversized responses under a small heap reproduces Case A directly and is easy to plant.

2. **Sizing a container, not just a heap.** The decks size the JVM heap. Nobody asked how to pick a heap; they asked how to know whether the *pod* has enough. That means `-XX:MaxRAMPercentage`, non-heap memory (Metaspace, code cache, thread stacks, direct buffers), and why the JVM's footprint exceeds `-Xmx`. The 64-pods-because-of-memory figure suggests this is costing them real money.

3. **No async/queue-based testing anywhere.** Their application has no UI and is driven by Solace. Our whole harness is HTTP-shaped. Worth deciding whether to address it or scope it out explicitly.

4. **Harness mismatch.** They use a custom Java 8 tool and stated they are **not open to JMeter** — and we chose Gatling. The labs will still teach the method, but participants cannot take the tooling home. Worth being upfront about that in the session rather than letting it surface as a complaint.

5. **"Hard to reproduce locally"** was said explicitly about Case A. That is precisely the skill the labs should build, and it argues for keeping the container-limit material concrete rather than theoretical.

## 6. Open questions for the trainer

- Do we add humongous objects as deck content only, or as a sixth lab? It maps cleanly onto the existing lab 5 app and would need one defect profile.
- Is container sizing (`MaxRAMPercentage`, non-heap footprint) in scope for this run, given the schedule is already tight at 5×4 hours?
- Do we say anything about Solace / async testing, or scope it out loud?
- k0s and k3s were raised on the call as lightweight local Kubernetes options: https://k0sproject.io/ and https://k3s.io/ — are these for the participants' own local setup, or something we demonstrate?
- One participant was noted as able to run Solace and PostgreSQL locally. Relevant to whether any lab could use their environment.

---

## Appendix — original notes, verbatim

```
Java, Spring Boot
Mai multe microservicii, GCP & k8s
Partea de debugging - memorie, OOM, pt k8s - cum configurăm, cum testăm dacă avem resursele necesare
Performanță - req, vit de procesare a unui request (throughput)

Testare de performanță - aplicația folosește Solace queues, nu are UI
Sunt probleme de perf - baza de date etc
Sunt 2 apps - un SB wrapper, în care trimitem mesaje prin Solace (în GCP)
Am făcut un test de perf - n-am mai putut accesa nimic, am trimis 10k tranzacții. Nici măcar după restart nu a funcționat ok, resursele au fost epuizate. Am vrut să văd ce s-a întâmplat în JVM, cum aș putea să evit - câte tranzacții ar putea suporta laptop-ul meu
Rulăm teste de perf în GCP
Avem un proiect vechi, am început unul nou, nu e gata
Nu folosim nici Jmeter, nici Gatling. Folosim ceva custom - un proiect scris în Java, folosit de câțiva ani de zile. Java 8

În producție sunt multe Pod-uri, nu știu care e raportul
Au fost multe pb de memorie, aplicația SB consumă foarte multă memorie, de aceea sunt 64 de pod-uri

Jmeter merge pt testare de perf pt async processing; nu sunt deschiși să folosească
Avem un wrapper (SB), avem o aplicație veche (anii 2000), se conectează prin ODBC, sunt 3 DB, foarte multe info redundante. Mulțimea datelor și vechimea ODBC, business-ul este dependent de această aplicație. Se dorește să se dezvolte ceva intern.
Problemele sunt multe - NPEs dese, complicat de modificat

Local setup - mini-k8s, Solace, db (Oracle). în gcp costa foarte mult, acum folosim SQLServer. se dorește trecerea la postgres, nu s-a făcut modificarea. Găzduirea în GCP costă foarte mult
App a rulat în OpenShift, s-a migrat la GCP; problemele au rămas aceleași. Problemele de perf sunt multe

Anda ar putea rula Solace și Postgresql;

https://k0sproject.io/ + https://k3s.io/

---

Valentin: două aplicații, câteva microservicii, s-a ales Cloud Run (GCP), a fost o pb de perf, am făcut un setup vechi cu Jmeter, am creat o imagine docker. Cloud Run - container-ul era restricționat la 2GB, apăreau în loguri linii de fierăstrău - problema era cu G1, avea răspunsuri foarte mari, se creau humongous blocks. G1 este nou, lasă obiectele mari pt mai târziu; default-ul era de 1MB, răspunsurile noastre erau de 5-6 MB. Se umpla memoria, container-ul era setat la 90% - mai rămâneau aprox 200 MB, era omorât repede. greu de reprodus local

DB: Oracle, on prem. cu migrare la ceva în GCP - Oracle sau Postgres, nu încă. o bază de date monolitică, câteva aplicații (ms) care se conectează la ea
```
