# Local Kubernetes setup — proposal

**Status:** proposal, awaiting trainer review. Nothing built.
**Driver:** `docs/k8s-prep-work.md` — participants asked how to configure Kubernetes resources and
how to tell whether they are sufficient, and reported a G1 humongous-object failure on a 2 GB
Cloud Run container that was *"hard to reproduce locally"*.

---

## 1. What this setup has to achieve

One sentence: **make the container-memory failure reproducible on a participant's laptop in under
a minute, so the thing they could not reproduce in production becomes something they can break and
fix at will.**

Concretely it must support lab 9 (humongous allocations):

- run the training app with a hard memory ceiling, the way a pod or Cloud Run container does
- let the JVM see that ceiling and size itself from it
- make G1's region size and humongous-region count observable
- let a participant blow the limit, watch the process die, change one flag, and watch it survive

## 2. The question worth settling first: is Kubernetes needed at all?

Two different asks came out of the pre-call and they have different answers.

**"Why did my container die?"** — this is a **JVM-in-a-cgroup** question. Kubernetes contributes
nothing to it. A plain memory-capped container reproduces the JVM's container awareness, its
ergonomic heap sizing, G1's region sizing, humongous allocation and the kill, exactly.

**"How do I configure the pod, and is 2 GB enough?"** — this *is* a Kubernetes question. It needs
requests and limits, `OOMKilled` status, restart behaviour and `kubectl top`.

So the setup splits in two, and only the second needs a cluster. That matters because prerequisites
are already a stated constraint — the spec requires setup small enough that lab time goes to the
work, and asking fifteen people to install a Kubernetes distribution the morning of day one is the
opposite of that.

## 3. Proposed tiers

### Tier 0 — Docker only. **Required for lab 9.**

```bash
# reproduce the failure: 2 GB ceiling, default region size
docker run --rm --memory=2g --memory-swap=2g \
  -e JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -Xlog:gc+heap=debug" \
  java-perf-training:latest

# what the JVM thinks it has
docker run --rm --memory=2g eclipse-temurin:21 \
  java -XX:MaxRAMPercentage=75 -XX:+PrintFlagsFinal -version \
  | grep -Ei 'MaxHeapSize|G1HeapRegionSize'

# the fix
docker run --rm --memory=2g ... -XX:G1HeapRegionSize=16m ...
```

Everything lab 9 teaches is visible here: the JVM reading the cgroup limit, the ~1 MB default region
at a small heap, humongous regions appearing in the GC log, the sawtooth, the kill, and the flag that
fixes it. One prerequisite — Docker — which most participants already have.

### Tier 1 — a real cluster. **Optional; trainer-run demo, not a prerequisite.**

Adds what Tier 0 genuinely cannot show: `requests` vs `limits`, the `OOMKilled` reason and exit code
137, restart loops, and `kubectl top pod`.

Recommended distribution: **k3d** (k3s inside Docker).

```bash
brew install k3d
k3d cluster create perf --agents 1
kubectl apply -f k8s/lab9-humongous.yaml   # limits: memory 2Gi
kubectl top pod
kubectl describe pod <name> | grep -A3 'Last State'   # OOMKilled, exit 137
```

| Option | Verdict |
|---|---|
| **k3d** | Recommended. Smallest and fastest to create/destroy, needs only Docker |
| **kind** | Good alternative, closest to upstream Kubernetes, slightly slower to start |
| Docker Desktop built-in | Zero install if they already run it, but heavy and licence-encumbered at some employers |
| **k0s** | Raised on the call. Production/edge oriented; more setup than a laptop demo warrants |
| minikube | Heaviest of the set. No reason to prefer it here |

Deliverable if approved: one `k8s/lab9-humongous.yaml` manifest plus about ten lines of README. Not
a cluster everyone must install.

### Tier 2 — Solace and a database. **Proposed out of scope.**

Reproducing case B (10k transactions, no recovery after restart) needs a broker, a database and a
load source. That is a day of setup for one lab we have not scoped, on a schedule already at
6–7 hours of labs inside 17 hours of content. Recommend scoping it out loud in the session rather
than half-building it.

## 4. What this adds to prerequisites

| Tier | Prerequisite | Preflight check |
|---|---|---|
| 0 (required) | Docker running | `docker info` succeeds; `docker run --rm --memory=512m hello-world` |
| 1 (optional) | k3d + kubectl | `k3d version`, `kubectl version --client` |

`scripts/preflight.sh` / `.ps1` gain a Docker check. Every failure needs an entry in
`docs/PREFLIGHT-TROUBLESHOOTING.md` — that rule already holds and applies here.

## 4b. Which distribution is actually lightest — research, 2026-09-24

The premise "kind requires Docker, the others may not" is **true on Linux and inverts everywhere
else**, which decides this for a mixed-platform classroom.

| | What it is | Needs Docker | Runs natively on macOS | Reported idle RAM |
|---|---|---|---|---|
| **k3d** | k3s running *inside* Docker | Yes | Yes, via Docker | **~500 MB** |
| **k3s** | Lightweight distro, single binary | No, on Linux | **No** | ~512 MB advertised |
| **k0s** | Lightweight distro, single binary | No, on Linux | **No** | ~510 MB advertised, ~600 MB measured idle |
| **kind** | Upstream Kubernetes in Docker | Yes | Yes, via Docker | **~800 MB** |

Two findings:

**1. k3d is the lightest of the four by measured idle memory** — roughly 500 MB, against ~600 MB for
k0s and ~800 MB for kind. It inherits k3s's footprint and adds only the Docker layer.

**2. The "no Docker needed" advantage of k3s and k0s exists only on Linux.** Neither runs natively on
macOS: k3s relies on Linux-specific syscalls and will not execute on Darwin, so it needs Lima,
Colima or multipass — a full Linux VM, which is *heavier* than Docker, not lighter. k0s documents
Linux and Windows Server support with **no macOS support at all**, and its Windows support is for
Server worker nodes joining a Linux-controlled cluster, not a standalone laptop cluster. It also
requires cgroup v2 and rejects cgroup v1.

So for anyone not on Linux, choosing k3s or k0s means installing a VM *on top of* the Docker they
already need for Tier 0 — strictly more setup for strictly more memory.

**Conclusion: k3d.** It is simultaneously the lightest measured and the most portable, and it adds
no new dependency because Tier 0 already requires Docker. kind costs ~300 MB more for no benefit
here; its advantage is fidelity to upstream Kubernetes, which matters for conformance testing and
not for demonstrating a memory limit.

*Caveat: these are reported figures from published comparisons, not measured on this hardware. The
ordering is consistent across sources; treat the absolute numbers as indicative.*

## 5. Recommendation

Build **Tier 0 only** as required lab infrastructure. Build **Tier 1 as a trainer-run demo** with a
manifest participants can take home. **Scope out Tier 2** explicitly.

Reasoning: Tier 0 delivers the whole of lab 9's teaching for one prerequisite most people already
have. Tier 1 answers the configuration question that was actually asked, without making fifteen
laptops install a cluster. And the participant who said the problem was *hard to reproduce locally*
gets a one-line `docker run` that reproduces it — which is the single most useful thing this course
can hand them.

## 6. Open questions

- Approve the tiering, or do you want a cluster on every laptop?
- ~~k3d or kind for Tier 1?~~ **Settled by §4b: k3d.** Lightest of the four and needs no dependency beyond the Docker that Tier 0 already requires. k3s and k0s are Linux-only in practice.
- Does lab 9 run **only** in a container, or also bare-metal first so participants see the same code behave differently once a limit exists? The contrast is a strong teaching beat but costs time.
- Is a prebuilt image published, or do participants build locally? Building avoids a registry but costs minutes on day one.
- Case B (Solace) — confirm scoping it out, or keep it as a stretch topic?
