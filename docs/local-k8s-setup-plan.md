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
java -XX:+UseG1GC -Xmx1g -Xlog:gc+heap=debug -jar app.jar
java -XX:+UseG1GC -Xmx1g -XX:+PrintFlagsFinal -version | findstr /I "G1HeapRegionSize"
java -XX:+UseG1GC -Xmx1g -XX:G1HeapRegionSize=16m ...        the fix
```

**`-XX:+UseG1GC` is not optional, and this was found the hard way.** Humongous allocations are a G1
concept, and JVM ergonomics select **Serial GC** below the ~2 GB "server-class" threshold. Inside a
1 Gi container the JVM runs Serial, not G1, and the lesson silently does not reproduce — confirmed
on a live cluster, where step 3 of the progression failed until the flag was added.

On a laptop with plenty of RAM, ergonomics read the *machine's* memory rather than `-Xmx`, so G1 is
usually chosen anyway. Set the flag regardless: it costs nothing and removes the gap between "works
on my machine" and "works in the container".

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
| **k3s inside WSL2** | Apache 2.0, free | Kubernetes, no Docker at all | **Lightest.** A few hundred MB. Enable systemd in `/etc/wsl.conf`, then `curl -sfL https://get.k3s.io \| sh -` |
| Rancher Desktop | Apache 2.0, free at any size | Container runtime **and k3s**, GUI | **~2 GB idle, 4 GB default VM.** See §4c — too heavy for this classroom |
| **Podman Desktop** | Free at any size | Container runtime, GUI | Docker-compatible CLI, daemonless/rootless. No bundled Kubernetes |
| **Docker Engine (CE) inside WSL2** | Apache 2.0, free | Container runtime | The subscription applies to Docker *Desktop*, not the engine. No GUI |
| Docker Desktop | **Paid** at their size | Everything | Assume unavailable |

**k3s and k0s become genuinely viable on Windows in a way they are not on macOS**, because WSL2
supplies a real Linux kernel. Your original instinct — that the others may not need Docker — is
correct *on Windows*. It was only wrong for macOS.

## 4. Proposed tiers

### Tier 0 — plain JDK. **Required. Zero new prerequisites.**

`java -XX:+UseG1GC -Xmx1g` reproduces the humongous-allocation failure exactly, as shown in §2. Lab 9 runs here.

### Tier 1 — a memory-capped container. **Recommended, one install.**

Teaches container-awareness: `MaxRAMPercentage`, RSS vs heap, the kill.

```
podman run --rm --memory=2g ...        Podman Desktop
docker run --rm --memory=2g ...        Rancher Desktop or Docker CE in WSL2
```

### Tier 2 — a Kubernetes cluster. **Same install as Tier 1 if Rancher Desktop is chosen.**

Teaches what only a cluster shows: `requests` vs `limits`, `OOMKilled`, exit code 137, restart
loops, `kubectl top pod`.

**Use bare k3s inside WSL2, not Rancher Desktop** — see §4c. Rancher Desktop would collapse
Tiers 1 and 2 into one install, but at a memory cost this particular classroom cannot afford.

```
wsl --install -d Ubuntu
# in /etc/wsl.conf:  [boot]  systemd=true
wsl --shutdown
curl -sfL https://get.k3s.io | sh -
```

### Tier 3 — Solace and a database. **Proposed out of scope.**

Case B needs a broker, a database and a load source — a day of setup for a lab we have not scoped,
on a schedule already at 6–7 hours of labs inside 17 hours of content.

## 4c. Memory budget — why not Rancher Desktop

The trainer's instinct that Rancher Desktop is heavier than k3s is correct, and the gap is large
enough to decide the question:

| | Idle | Under load |
|---|---|---|
| **Bare k3s in WSL2** | a few hundred MB; runs on a 2 GB VPS | control plane ~500 MB–1.5 GB |
| **Rancher Desktop** | **~2 GB**, on a 4 GB default VM | 2.9 GB for a six-service stack; 4–8 GB for heavier Kubernetes work |

Two reasons this matters more here than it would elsewhere:

**1. Lab 9's entire subject is a 2 GB memory ceiling.** Running a tool that idles at ~2 GB, on a
laptop, while teaching what happens when a 2 GB limit is exceeded, spends the very resource the
lesson is about. On a 16 GB corporate laptop also running an IDE and the JVM under test, that is a
bad classroom.

**2. WSL2 defaults to half the host's RAM**, and all WSL2 distributions share one pool. There are
documented reports of container workloads on Windows/WSL2 escalating to ~99% of RAM and freezing the
machine when no cap is set. Rancher Desktop runs in its own *additional* `rancher-desktop` distro,
competing with any Ubuntu distro a participant already has.

**Therefore, if WSL2 is used at all, capping it is a mandatory prerequisite step**, not advice:

```ini
# %USERPROFILE%\.wslconfig
[wsl2]
memory=4GB
processors=2
```

This belongs in `preflight.ps1` and in `PREREQUISITES.md`. Freezing a participant's laptop on day
one is a worse outcome than skipping the Kubernetes tier entirely.

## 5. Recommendation

- **Tier 0 required.** Lab 9 needs nothing but the JDK. This is the single most valuable thing the
  course hands them: the participant who said case A was *hard to reproduce locally* gets a one-line
  `java -XX:+UseG1GC -Xmx1g` that reproduces it on their own Windows laptop.
- **Tier 1 + 2 via bare k3s in WSL2**, optional and **not a hard prerequisite**. Lightest of the
  free options, no licensing exposure, and no second WSL2 distro competing for the shared pool.
  Podman Desktop is the alternative if a GUI is wanted for Tier 1 alone.
- **A `.wslconfig` memory cap is mandatory wherever WSL2 is used**, per §4c.
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
