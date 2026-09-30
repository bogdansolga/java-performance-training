# Kubernetes memory lab - playbook

This reproduces, on your own laptop cluster, a real production incident: a
container capped at a fixed memory limit, killed repeatedly, root-caused to G1 allocating large
API responses as "humongous" objects. Four steps, one value changed each time; the last one is yours to solve.

**Needs a working cluster and plain `kubectl`** - from [participant-setup-playbook.md](participant-setup-playbook.md).

All commands below assume you are in the project folder, with `kubectl` working and pointed at
your cluster (`kubectl get nodes` shows one `Ready` node).

### The diagnosis toolkit

You will use these four commands throughout. Worth knowing even outside this lab:

```bash
kubectl top pod -n perf-lab
kubectl describe pod <name> -n perf-lab | grep -A5 'Last State'
kubectl get pod <name> -n perf-lab -o jsonpath='{.status.qosClass}'
kubectl logs <name> -n perf-lab --previous
```

The last one matters most: when a container is killed and restarted, its logs go with it.
`--previous` is how you read the evidence from the run that died.

---

## Step 0 - the image

The image is published as `bogdansolga/java-perf-training`, so there is nothing to build. The
manifests use it with `imagePullPolicy: Always`: k3s downloads it the first time the pod starts,
and checks for a newer build on every start after that. So stay online for this lab, and expect the
first start to take a minute longer.

*If you want to read or rebuild it,* the `Dockerfile` is in the repository root. You do not need to.

---

## Step 1 - apply the baseline

```bash
kubectl apply -f kubernetes/00-namespace.yaml
kubectl apply -f kubernetes/10-postgres.yaml
kubectl apply -f kubernetes/20-jvm-config.yaml
kubectl apply -f kubernetes/30-app.yaml
```

**Check it worked:**

```bash
kubectl get pods -n perf-lab
```

Both `app-...` and `postgres-...` reach `1/1 Running` within about a minute (Postgres needs a
few seconds before the app's health check settles).

```bash
kubectl get pod -n perf-lab -l app=app -o jsonpath='{.status.qosClass}'
```

**Expected - and measured:** `BestEffort`. No `resources:` block is set on the app container yet,
so Kubernetes gives it no memory protection and no guarantee. That is step 1's entire point: it
runs, uses whatever it likes, and nothing is watching.

*If pods stay `Pending`,* your cluster likely does not have enough free memory - check
`kubectl describe node` for `Allocated resources`.

*If `app-...` shows `ImagePullBackOff`,* the cluster cannot reach Docker Hub - check your network
or VPN, then `kubectl delete pod -n perf-lab -l app=app` to retry.

---

## Step 2 - add a 1Gi limit and `-Xmx1g`: OOMKilled

Two files change together here.

Open `kubernetes/20-jvm-config.yaml` and change the flags line to:

```yaml
  JAVA_TOOL_OPTIONS: "-Xmx1g"
```

Open `kubernetes/30-app.yaml` and add a `resources:` block to the `app` container (right after
`envFrom:`, before `startupProbe:`):

```yaml
          resources:
            requests:
              memory: 1Gi
            limits:
              memory: 1Gi
```

Apply both, then generate load:

```bash
kubectl apply -f kubernetes/20-jvm-config.yaml
kubectl apply -f kubernetes/30-app.yaml
kubectl apply -f kubernetes/40-load-job.yaml
```

Watch it:

```bash
kubectl get pods -n perf-lab -w
```

**Expected - and measured:** the pod does **not** die immediately. In testing it ran fine for
several minutes, climbing slowly under load (`kubectl top pod -n perf-lab`) from around 500Mi
toward 1Gi, then was genuinely killed: `RESTARTS` incremented, and

```bash
kubectl describe pod <app-pod-name> -n perf-lab | grep -A5 'Last State'
```

showed `Reason: OOMKilled`, `Exit Code: 137` - confirmed, not hypothetical. `-Xmx1g` only bounds
the Java heap; it leaves nothing in the 1Gi container budget for Metaspace, thread stacks, GC
bookkeeping, or the JVM itself, so the kernel kills the whole process once the container's total
footprint - not just the heap - crosses 1Gi.

*If it has not restarted after five minutes,* check `kubectl top pod -n perf-lab` - if memory is
still climbing, give it more time; the load Job takes a few minutes to push it over. If memory is
flat and well under 1Gi, re-check the ConfigMap actually applied (`kubectl get configmap jvm-opts
-n perf-lab -o yaml`) and that the pod restarted after it (`kubectl rollout restart deployment/app
-n perf-lab`).

Once you have seen the restart, clean up the finished load Job before the next step (its pod
template can't be edited in place):

```bash
kubectl delete job load-generator -n perf-lab
```

---

## Step 3 - replace the fixed heap with `-XX:MaxRAMPercentage=75`

Open `kubernetes/20-jvm-config.yaml` again and change the flags line to:

```yaml
  JAVA_TOOL_OPTIONS: "-XX:+UseG1GC -XX:MaxRAMPercentage=75 -Xlog:gc+heap=debug:stdout:uptime,tags"
```

**Note the `-XX:+UseG1GC`.** It was not expected to be necessary, but testing on the live cluster
found that at a 1Gi container limit, JDK 21's ergonomics quietly pick **Serial GC**, not G1 -
`-XX:+PrintFlagsFinal` showed `UseSerialGC = true {ergonomic}`. The JVM's "server-class machine"
check looks at the cgroup memory limit, and 1Gi falls under its 2GB threshold. Without this flag,
step 3 and step 4 do not exercise G1 at all, and the whole humongous-object story silently does
not apply. `kubernetes/20-jvm-config.yaml` carries a comment explaining this.

`-Xmx1g` is gone - `limits.memory: 1Gi` in `kubernetes/30-app.yaml` from step 2 stays as-is.

```bash
kubectl apply -f kubernetes/20-jvm-config.yaml
kubectl rollout restart deployment/app -n perf-lab
kubectl apply -f kubernetes/40-load-job.yaml
```

Watch the app's own logs (not `--previous` this time - it should survive):

```bash
kubectl logs -n perf-lab -l app=app -f | grep -i humongous
```

**Expected and measured:** the app survives. Under load, the log fills with lines like:

```
[157.377s][gc,heap] GC(71) Humongous regions: 560->56
[183.119s][gc,heap] GC(72) Humongous regions: 518->119
```

confirming real humongous allocations - the 6 MB `/product/humongous` responses are each well
over G1's 512 KB threshold at the default 1 MB region size (`-XX:+PrintFlagsFinal` shows
`G1HeapRegionSize = 1048576`). In testing, memory
plateaued close to the 1Gi limit (around 950-965Mi) rather than climbing past it and OOMKilling -
G1's eager reclamation of humongous regions during young/mixed collections kept up with this
lab's load level, and no `Pause Full` events appeared in the log. Under a substantially heavier,
hand-driven load (double the concurrency of `40-load-job.yaml`) the pod did restart once, but the
`--previous` log showed `Exit Code: 143` (a liveness-probe-triggered restart from GC/CPU
contention), not `137` (OOMKilled) - worth knowing if your run looks different from a neighbour's.

*If you see no `humongous` lines at all,* confirm the flags actually changed
(`kubectl exec -n perf-lab <app-pod> -- env | grep JAVA_TOOL_OPTIONS`) and that the deployment
picked up the new ConfigMap (`kubectl rollout restart deployment/app -n perf-lab` again).

```bash
kubectl delete job load-generator -n perf-lab
```

---

## Step 4 - your turn: make it stable

Keep the 1Gi limit and `-XX:MaxRAMPercentage=75`. Change **one** more JVM flag in
`kubernetes/20-jvm-config.yaml` so that the 6 MB responses stop being humongous, then re-apply as in
step 3 and run the load again.

**How you know you are done:** `kubectl logs -n perf-lab -l app=app | grep -i humongous` shows
`Humongous regions: 0->0` on almost every line, there are no `Pause Full` events, and the pod does
not restart during a full run of `40-load-job.yaml`.

Hint: step 3's log already tells you the region size G1 picked, and the size of a response.

---

## Cleanup

```bash
kubectl delete namespace perf-lab
```

This removes everything you applied. The cluster itself keeps running.
