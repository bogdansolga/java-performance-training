# Kubernetes objects — simplified proposal

**Status:** proposal, awaiting trainer review. Nothing built.
**Reuses:** `kubernetes-capstone-project/k8s-reference/v1-full` — a known-working manifest set.
**Constraint:** the workshop is about **Java performance, not Kubernetes**. Every object has to earn
its place by teaching something about the JVM; anything that only teaches Kubernetes is cut.
**Target:** Windows 10/11 laptops, k3s inside a memory-capped WSL2.

---

## 1. The finding that shrinks the whole thing

**The humongous-object lab does not need a 2 GB heap.**

G1's region size is `heap / 2048`, clamped to a **1 MB minimum** and rounded to a power of two. So:

| Heap | Computed | Actual region |
|---|---|---|
| 2 GB | 1 MB | **1 MB** |
| 1 GB | 0.5 MB | **1 MB** (clamped) |
| 512 MB | 0.25 MB | **1 MB** (clamped) |

Any heap at or below 2 GB gives 1 MB regions, so the humongous threshold is 512 KB in every case.
A 5–6 MB response is humongous at 1 GB exactly as it was at 2 GB in their Cloud Run incident.

**The app container drops from 2 Gi to 1 Gi with no loss of teaching value** — and that is the
largest line in the budget.

## 2. Memory budget on a participant laptop

| Component | Request | Limit | Note |
|---|---|---|---|
| k3s control plane | — | ~700 Mi observed | API server, scheduler, controller, embedded etcd |
| App under test | 1 Gi | **1 Gi** | requests == limits → **Guaranteed** QoS, which is itself a lesson |
| Postgres | 128 Mi | 256 Mi | Trimmed from the capstone's 256/512 — the lab schema is tiny |
| Load `Job` | 64 Mi | 128 Mi | Short-lived |
| **Default total** | | **~2.1 Gi** | Comfortable inside a 4 GB `.wslconfig` cap |
| RabbitMQ *(not applied by default)* | 128 Mi | 256 Mi | Ships for a full setup on request |
| **With broker** | | **~2.4 Gi** | Still inside the cap |

The default set is **~2.1 Gi** — the broker is shipped but not applied. With it, ~2.4 Gi. Both fit
the 4 GB WSL2 cap from the setup plan, leaving headroom for Windows, an IDE and the JVM the
participant is profiling **outside** the cluster.

## 3. Message broker — infrastructure only, and Solace is not viable locally

**Decided: the broker is not a lesson.** It exists only as *the system that handles async
messages*, so a participant with an async-shaped application recognises their own architecture.
Nothing in the course teaches the broker, measures it, or tunes it. The manifest ships so a full
setup is available on request; it is not applied by default.

That lowers the bar considerably — the broker only has to start, accept a message and hand it on.

| Broker | Minimum | Runtime | Verdict |
|---|---|---|---|
| **Solace PubSub+** | **1 CPU + 3.4 GiB** minimum; 2 CPU + 4 GB for a non-HA pod; also wants `--shm-size=1g` | — | **Out.** Larger than the entire rest of the budget combined |
| **Kafka (KRaft)** | ~512 Mi–1 Gi | **JVM** | Avoid — see below |
| **RabbitMQ** | ~128–256 Mi | Erlang | **Recommended fallback** |

Two reasons for RabbitMQ over Kafka here, beyond size:

1. **Kafka is itself a JVM.** In a workshop whose entire subject is watching JVM memory, a second
   JVM in the cluster competes for the memory being measured and muddies every `kubectl top` reading.
   RabbitMQ runs on Erlang, so the JVM under test is the only JVM in the picture.
2. RabbitMQ is roughly a quarter of Kafka's footprint.

Solace matches their production stack, which is the argument for it — but at 3.4 GiB minimum it
cannot coexist with k3s, Postgres and the app on a laptop. Since the broker is scenery rather than
subject, RabbitMQ serves the purpose at a fraction of the cost. Worth saying in the room that
Solace is the same shape at a size that needs a server, so nobody assumes we avoided it by accident.

## 4. What we reuse from the capstone, and what we cut

The capstone's `v1-full` is proven and its resource values are sane. Reuse the shapes; cut anything
that teaches Kubernetes rather than the JVM.

| Capstone | Here | Why |
|---|---|---|
| `skyhop-fe` Next.js frontend, Service, Ingress, config | **Cut entirely** | No UI is needed to measure a JVM |
| `kind-cluster.yaml`, ingress setup | **Cut** | `kubectl port-forward` is one command and needs no cluster config |
| Postgres as `StatefulSet` + `volumeClaimTemplates` (1 Gi PVC) | **Plain workload + `emptyDir`** | Lab data is disposable; a PVC adds a storage class, a bound volume and a cleanup step, and teaches nothing about the JVM |
| `Secret` for DB credentials | **Folded into the ConfigMap** | Local lab, throwaway password. One object fewer |
| `replicas: 2` on the backend | **`replicas: 1`** | Two replicas halve the memory available per pod and add "which pod am I looking at?" confusion |
| `startupProbe` + readiness + liveness | **Keep** | Proven; the startup probe specifically prevents a restart while Postgres initialises |
| Postgres `readinessProbe` with `pg_isready` | **Keep** | Proven and cheap |

Net: **11 manifest files → 5 applied**, plus the broker manifest that ships unapplied.

## 5. The object set

**Every workload is `replicas: 1`** — the app, Postgres and RabbitMQ alike. Two replicas halve the
memory available per pod, double the control-plane bookkeeping and introduce "which pod am I
looking at?" during a measurement. Note when copying from the capstone: its manifests use
`replicas: 2`, and that is the one value to change.

```
k8s/
  00-namespace.yaml      Namespace
  10-postgres.yaml       workload + Service + env (emptyDir, no PVC, no Secret)
  20-jvm-config.yaml     ConfigMap  <- the only file participants edit
  30-app.yaml            workload + Service (requests == limits == 1Gi)
  40-load-job.yaml       Job
  50-rabbitmq.yaml       shipped, not applied by default — async scenery for a full setup
```

`20-jvm-config.yaml` is the whole point — participants change one value and re-apply:

```yaml
apiVersion: v1
kind: ConfigMap
metadata: { name: jvm-opts, namespace: perf-lab }
data:
  JAVA_TOOL_OPTIONS: >-
    -XX:MaxRAMPercentage=75
    -Xlog:gc+heap=debug:file=/tmp/gc.log:tags,uptime
```

## 6. The progression, unchanged in substance

One workload, one ConfigMap, three edits — reproducing their Cloud Run incident at 1 Gi:

| # | `limits.memory` | JVM flags | Result | Lesson |
|---|---|---|---|---|
| 1 | `1Gi` | `-Xmx1g` | **OOMKilled**, exit 137 | The limit covers the whole process, not just the heap |
| 2 | `1Gi` | `-XX:MaxRAMPercentage=75` | Survives start; sawtooth and full GCs under load | Humongous allocations at a 1 MB region size |
| 3 | `1Gi` | `+ -XX:G1HeapRegionSize=16m` | Stable | The fix, invisible without the GC log |

The capstone's "no limits at all" step is dropped — it costs a cycle and teaches least.

```bash
kubectl top pod -n perf-lab
kubectl describe pod <name> -n perf-lab | grep -A5 'Last State'
kubectl get pod <name> -n perf-lab -o jsonpath='{.status.qosClass}'
kubectl logs <name> -n perf-lab --previous | grep -i humongous
```

`--previous` reads the log of the killed container, which removes the need for a PVC to survive an
OOMKill — that is why `emptyDir` is sufficient in §4.

## 6b. Image distribution — decided 2026-09-27

**The trainer builds and pushes; participants pull.** The image is
**`bogdansolga/java-perf-training`**, untagged — so `:latest`, deliberately, so participants always
receive the current build without anyone coordinating a tag.

This supersedes the earlier "participants build locally" assumption and removes its worst failure
mode: a locally-built image is invisible to the cluster until imported (`k3d image import`, or
`docker save | k3s ctr images import`), and a participant who misses that step gets a confusing
registry error. None of that applies now.

Consequences for the manifests:

- The workload references `bogdansolga/java-perf-training` with no tag.
- Kubernetes defaults `imagePullPolicy` to `Always` for an untagged or `:latest` image, which is
  what "always the latest" requires. State it explicitly rather than relying on the default.
- **`Always` means the kubelet contacts the registry on every pod start.** A participant who is
  offline, or on a connection that drops, cannot start a pod even with the image already cached —
  `IfNotPresent` would use the cache but could serve a stale build. The trainer's intent is
  freshness, so `Always` stands; the mitigation is a `docker pull` during the setup block so the
  first lab is not waiting on a download.
- A `Dockerfile` still belongs in the repo — multi-stage, JDK 21 — because it is what produces the
  pushed image and participants may want to read or rebuild it.

## 7. Open questions

- ~~Is the async/broker lesson in scope?~~ **Settled: no.** The broker is scenery only; the manifest ships unapplied.
- ~~Confirm Postgres only?~~ **Settled: Postgres only.** SQL Server is out — its container wants ~2 GB alone and would blow the budget as thoroughly as Solace.
- Do participants build the app image locally, or do we publish one as the capstone does with `bogdansolga/skyhop-be`? Publishing avoids a slow first build; building avoids a registry dependency.
- Does this run as part of lab 9, or as a trainer-only demo? Lab 9 itself still needs no cluster.
