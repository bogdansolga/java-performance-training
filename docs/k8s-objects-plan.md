# Kubernetes objects needed — proposal

**Status:** proposal, awaiting trainer review. Nothing built.
**Reads from:** `docs/k8s-prep-work.md` (participant pre-call), `docs/local-k8s-setup-plan.md` (Tier 2 — bare k3s in WSL2).

---

## 1. What they actually asked for

Three asks from the pre-call, in their words, and what each one needs from Kubernetes:

| Their ask | What it really is | Needs |
|---|---|---|
| *"pt k8s - cum configurăm"* | How to set pod resources | `requests` / `limits`, and the QoS class those produce |
| *"cum testăm dacă avem resursele necesare"* | How to know whether the allocation is enough | live usage vs limit, `OOMKilled`, restart counts, behaviour under load |
| *"aplicația SB consumă foarte multă memorie, de aceea sunt 64 de pod-uri"* | Why memory forces horizontal scale | the relationship between `-Xmx`, non-heap memory and the container limit |

That third one is the most valuable and the least likely to be self-diagnosed. It is also where the
money is: 64 pods driven by memory consumption is a recurring cloud bill.

## 2. The teaching point the objects have to carry

**A JVM sized to its container limit gets OOMKilled even when the heap never fills.**

The container limit covers the *whole process*: heap **plus** Metaspace, code cache, thread stacks,
direct byte buffers, GC bookkeeping and the JVM itself. Setting `-Xmx2g` inside a 2 Gi limit leaves
nothing for any of that, so the kernel kills the container while the heap still looks healthy in
every JVM-level tool. This is very likely a contributor to their 64-pod situation, and it is
invisible unless you look at the pod and the JVM *at the same time*.

Their reported incident was on **Cloud Run**, not Kubernetes — but the mechanism is identical, since
both enforce a cgroup memory limit. The objects below teach it in a form they can inspect.

## 3. Proposed object set

### Core — required

| Object | Why |
|---|---|
| `Namespace` | Isolates the lab; scopes the quota and limit-range below |
| `ConfigMap` | Holds `JAVA_TOOL_OPTIONS`. **The main teaching lever** — change JVM flags and redeploy without rebuilding an image |
| `Deployment` | Carries `resources.requests` / `resources.limits` and the probes. The object they will actually edit |
| `Service` (ClusterIP) | Lets the load generator reach the app by name |
| `Job` | Runs the load generator. Matches their own "send 10k transactions" scenario better than a long-lived pod |

### Supporting — recommended

| Object | Why |
|---|---|
| `LimitRange` | Shows what a pod gets when it declares nothing. Directly answers "what happens if we forget?" |
| `ResourceQuota` | Caps the namespace. Makes the 64-pods economics visible in miniature |
| `HorizontalPodAutoscaler` | They scaled to 64 pods *because of memory*. Worth showing what memory-driven autoscaling does and does not fix |

### Already present in k3s

`metrics-server` ships enabled in k3s, so `kubectl top pod` works with no extra install. **Verify on
the day** — it can be disabled with `--disable=metrics-server`, and `kubectl top` failing at the
front of the room is a bad look.

### Deliberately excluded

`Ingress` (port-forward is enough), `PodDisruptionBudget`, `NetworkPolicy`, `StatefulSet`,
`PersistentVolumeClaim`, Helm. None of them teach anything about memory, and each is a prerequisite
and a failure mode we would be adding for nothing.

## 4. The progression — four states of one Deployment

This is the spine. One `Deployment`, one `ConfigMap`, four edits, each producing a visibly different
failure or success. Every step reproduces something they have actually hit.

| # | `limits.memory` | JVM flags | What happens | Lesson |
|---|---|---|---|---|
| 1 | *(none)* | default | Runs; consumes what it likes | No limit means no protection — and no QoS guarantee |
| 2 | `2Gi` | `-Xmx2g` | **OOMKilled**, exit 137, restart loop | The limit covers the *whole process*, not just the heap |
| 3 | `2Gi` | `-XX:MaxRAMPercentage=75` | Survives startup, then full GCs and a sawtooth heap under load | Humongous allocations — the 1 MB default region at this heap |
| 4 | `2Gi` | `+ -XX:G1HeapRegionSize=16m` | Stable | The fix, and why it was invisible without the GC log |

Step 2 → 3 answers *"how do I size it?"*. Step 3 → 4 is their Cloud Run incident, reproduced.

Observation commands at each step:

```bash
kubectl top pod -n perf-lab
kubectl describe pod <name> -n perf-lab | grep -A5 'Last State'   # OOMKilled, exit 137
kubectl get pod <name> -n perf-lab -o jsonpath='{.status.qosClass}'
kubectl logs <name> -n perf-lab | grep -i humongous
```

## 5. Sketch

`k8s/` in the training repo:

```
k8s/
  00-namespace.yaml
  10-configmap-jvm.yaml        # the file participants edit
  20-deployment.yaml           # requests/limits live here
  30-service.yaml
  40-job-load.yaml
  50-limitrange.yaml           # optional
  51-resourcequota.yaml        # optional
  52-hpa.yaml                  # optional
  README.md                    # the four steps above, as commands
```

```yaml
# 10-configmap-jvm.yaml — step 2 of the progression
apiVersion: v1
kind: ConfigMap
metadata: { name: jvm-opts, namespace: perf-lab }
data:
  JAVA_TOOL_OPTIONS: "-Xmx2g -Xlog:gc+heap=debug:file=/tmp/gc.log:tags,uptime"
```

```yaml
# 20-deployment.yaml — excerpt
spec:
  containers:
    - name: app
      envFrom: [{ configMapRef: { name: jvm-opts } }]
      resources:
        requests: { memory: "2Gi", cpu: "500m" }
        limits:   { memory: "2Gi", cpu: "1" }      # requests == limits -> Guaranteed QoS
      readinessProbe:
        httpGet: { path: /actuator/health/readiness, port: 8080 }
```

Requests equal to limits gives **Guaranteed** QoS, which is worth showing explicitly — it changes
which pods the kubelet evicts first under node pressure, and it is a lever they can pull on a
memory-constrained cluster.

The app already exposes Actuator, so the probes need no new code.

## 6. What this does not cover

- **Solace / async load.** The `Job` drives HTTP. Their real system is queue-driven with no UI. Reproducing that needs a broker (Tier 3, proposed out of scope).
- **Cloud Run specifically.** Not Kubernetes. The mechanism transfers; the objects do not.
- **Multi-node behaviour.** Single-node k3s cannot show real scheduling pressure or node eviction. `ResourceQuota` and the HPA demonstrate the shapes, not the reality.

## 7. Open questions

- Does the Kubernetes material become part of **lab 9**, or a separate trainer-run demo? §2 of the setup plan showed lab 9 itself needs no container, so this is genuinely optional — and the schedule is already at 6–7 hours of labs inside 17.
- Include the optional three (`LimitRange`, `ResourceQuota`, `HPA`), or keep to the five core objects? The HPA is the most directly relevant to their 64 pods; the other two are cheap but add reading.
- Should the progression run **all four steps**, or start at step 2? Step 1 teaches little and costs a redeploy.
- Do we want the GC log written to a `PersistentVolume` so it survives an OOMKill, or is `kubectl logs --previous` enough? The latter is simpler and avoids a PVC.
