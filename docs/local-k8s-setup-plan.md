# Local Kubernetes setup — proposal (Windows classroom)

**Status:** proposal, awaiting trainer review. Nothing built.
**Revised:** 2026-09-24, after the constraint that **participants run Windows 10 / 11**. The
trainer's own macOS setup is not the target.
**Driver:** `docs/k8s-prep-work.md` — participants asked how to configure Kubernetes resources and
how to tell whether they are sufficient, and reported a G1 humongous-object failure on a 2 GB Cloud
Run container that was *"hard to reproduce locally"*.

---

## 1. What this has to achieve

**Make the container-memory failure reproducible on a participant's laptop in under a minute**, so
the thing they could not reproduce in production becomes something they can break and fix at will.

## 2. The finding that simplifies everything

**G1's region size is derived from the heap size, not from the container.**

The default is `heap / 2048`, clamped to 1–32 MB and rounded to a power of two. A 2 GB heap gives
**1 MB regions** whether that heap is capped by `-Xmx2g` on bare Windows or by a 2 GB pod limit. An
object larger than half a region is humongous either way.

So **case A reproduces with no container, no WSL2, no Kubernetes and no Docker**:

```
java -Xmx2g -Xlog:gc+heap=debug -jar app.jar
java -Xmx2g -XX:+PrintFlagsFinal -version | findstr /I "G1HeapRegionSize"
java -Xmx2g -XX:G1HeapRegionSize=16m ...        the fix
```

That is the whole of lab 9's JVM teaching, on a stock Windows JDK, with **zero prerequisites beyond
the JDK participants already need**.

What a container adds is a *different* lesson: the JVM reading a cgroup limit, `MaxRAMPercentage`,
RSS exceeding `-Xmx`, and `OOMKilled`. That answers *"is my pod big enough?"* — which was also asked,
but is a separate question from *"why did humongous objects kill me?"*.

## 3. The Windows constraint that decides the rest

**Docker Desktop requires a paid subscription** for any organisation with **250+ employees or
>$10M revenue**. A participant company running 64 production pods on GCP with Solace and Oracle is
almost certainly over both thresholds. Docker Desktop should therefore be treated as **unavailable**
unless the trainer confirms they are licensed.

Free, no-company-size-limit options on Windows, all working through **WSL2** (built into Windows
10/11, no licence, real Linux kernel):

| Option | Licence | Gives you | Notes |
|---|---|---|---|
| **Rancher Desktop** | Apache 2.0, free at any size | Container runtime **and k3s**, GUI | Bundles k3s in its own WSL2 distro; provides the `docker` CLI via dockerd/moby |
| **Podman Desktop** | Free at any size | Container runtime, GUI | Docker-compatible CLI, daemonless/rootless. No bundled Kubernetes |
| **Docker Engine (CE) inside WSL2** | Apache 2.0, free | Container runtime | The subscription applies to Docker *Desktop*, not the engine. No GUI |
| **k3s inside WSL2** | Apache 2.0, free | Kubernetes, no Docker at all | Enable systemd in `/etc/wsl.conf`, then `curl -sfL https://get.k3s.io \| sh -` |
| Docker Desktop | **Paid** at their size | Everything | Assume unavailable |

**k3s and k0s become genuinely viable on Windows in a way they are not on macOS**, because WSL2
supplies a real Linux kernel. Your original instinct — that the others may not need Docker — is
correct *on Windows*. It was only wrong for macOS.

## 4. Proposed tiers

### Tier 0 — plain JDK. **Required. Zero new prerequisites.**

`java -Xmx2g` reproduces the humongous-allocation failure exactly, as shown in §2. Lab 9 runs here.

### Tier 1 — a memory-capped container. **Recommended, one install.**

Teaches container-awareness: `MaxRAMPercentage`, RSS vs heap, the kill.

```
podman run --rm --memory=2g ...        Podman Desktop
docker run --rm --memory=2g ...        Rancher Desktop or Docker CE in WSL2
```

### Tier 2 — a Kubernetes cluster. **Same install as Tier 1 if Rancher Desktop is chosen.**

Teaches what only a cluster shows: `requests` vs `limits`, `OOMKilled`, exit code 137, restart
loops, `kubectl top pod`.

**Rancher Desktop collapses Tiers 1 and 2 into a single free install** — it ships a container
runtime *and* k3s in one WSL2 distribution, with `kubectl` on the Windows path. That is the
strongest argument for it over Podman Desktop, which has no bundled Kubernetes.

Fallback for locked-down machines that permit WSL2 but not a desktop app: install k3s directly
inside a WSL2 Ubuntu. No GUI, no Docker, fully free.

### Tier 3 — Solace and a database. **Proposed out of scope.**

Case B needs a broker, a database and a load source — a day of setup for a lab we have not scoped,
on a schedule already at 6–7 hours of labs inside 17 hours of content.

## 5. Recommendation

- **Tier 0 required.** Lab 9 needs nothing but the JDK. This is the single most valuable thing the
  course hands them: the participant who said case A was *hard to reproduce locally* gets a one-line
  `java -Xmx2g` that reproduces it on their own Windows laptop.
- **Tier 1 + 2 via Rancher Desktop**, recommended but **not a hard prerequisite**. One free install
  covers both, and it is the option least likely to hit a licensing or procurement wall.
- **Tier 3 scoped out**, said out loud in the session.

Deliverables if approved: one `Dockerfile`, one `k8s/lab9-humongous.yaml`, and a short README
section. No cluster that everyone must install.

## 6. The real risk, and it is not technical

**Corporate Windows laptops frequently block WSL2, require admin rights, or restrict installing
desktop applications.** If that is the case here, Tiers 1 and 2 are dead on arrival regardless of
which tool we pick — and Tier 0 becomes not just the recommended path but the only one that works.

This is the question worth asking the participants *before* the course, not on day one. It is
exactly the kind of thing `scripts/preflight.ps1` should surface a week early.

## 7. Open questions

- Can participants install software at all, and is **WSL2 available** on their machines? This decides Tiers 1 and 2 entirely.
- Is the client **licensed for Docker Desktop**? If yes, much of §3 becomes moot.
- Given §2 — that lab 9 needs no container — is Tier 1/2 worth any classroom time, or does it become a demo on the trainer's machine only?
- Should lab 9 run bare-metal first and *then* containerised, so participants see identical code behave differently once a limit exists? Strong teaching beat, costs time.
- Confirm Tier 3 (Solace) is scoped out.
