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
| RabbitMQ *(optional)* | 128 Mi | 256 Mi | Only if the async lesson is wanted |
| **Total** | | **~2.4 Gi** | Comfortable inside a 4 GB `.wslconfig` cap |

Without the broker it is ~2.1 Gi. Both fit the 4 GB WSL2 cap from the setup plan, leaving headroom
for Windows, an IDE and the JVM the participant is profiling **outside** the cluster.

## 3. Message broker — Solace is not viable locally

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
cannot coexist with k3s, Postgres and the app on a laptop. Recommend demonstrating the *pattern*
with RabbitMQ and saying plainly that Solace is the same shape at a size that needs a server.

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

Net: **11 manifest files → 5**, and one of those is optional.

## 5. The object set

```
k8s/
  00-namespace.yaml      Namespace
  10-postgres.yaml       workload + Service + env (emptyDir, no PVC, no Secret)
  20-jvm-config.yaml     ConfigMap  <- the only file participants edit
  30-app.yaml            workload + Service (requests == limits == 1Gi)
  40-load-job.yaml       Job
  50-rabbitmq.yaml       optional, only if the async lesson is in scope
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

## 7. Open questions

- Is the async/broker lesson in scope at all? If not, drop `50-rabbitmq.yaml` and the budget falls to ~2.1 Gi. Given the workshop is about Java performance and the schedule already carries 6–7 hours of labs, dropping it is defensible.
- SQL Server was mentioned as unlikely. Confirm Postgres only — SQL Server's container wants ~2 GB on its own and would blow the budget as thoroughly as Solace.
- Do participants build the app image locally, or do we publish one as the capstone does with `bogdansolga/skyhop-be`? Publishing avoids a slow first build; building avoids a registry dependency.
- Does this run as part of lab 9, or as a trainer-only demo? Lab 9 itself still needs no cluster.
