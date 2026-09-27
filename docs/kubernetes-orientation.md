# What we will see next — Kubernetes, just enough

For participants who do not use Kubernetes day to day.

**You do not need to learn Kubernetes for this course, and you do not need to install it.** Some of
what we look at runs inside a container managed by Kubernetes, and this page gives you the five
minutes of vocabulary that makes those demonstrations readable. Nothing more.

If you already work with Kubernetes, skip this.

---

## The one-sentence version

Kubernetes is an open-source platform for automating the deployment, scaling and management of
containerised applications.

For our purposes it does one thing that matters: **it runs your Java application inside a box with
a hard memory ceiling, and kills it without ceremony if it exceeds that ceiling.**

Almost everything surprising about Java memory in the cloud follows from that sentence.

---

## Five words you will hear

**Container** — your application plus everything it needs to run, packaged together. The JVM inside
a container does not automatically see the whole machine; it sees what the container was given.

**Pod** — the smallest thing Kubernetes runs. For us, a pod is one container: one JVM.

**Node** — a machine, physical or virtual, that pods run on.

**Cluster** — a set of nodes, managed as one. A *control plane* decides what runs where; *workers*
run it.

**Limit** — the memory and CPU ceiling set on a pod. This is the word that matters most.

---

## The mechanism that matters for us

When you give a pod a memory limit, you are setting a ceiling on the **whole process** — not on the
Java heap.

That distinction is the source of an enormous amount of confusion, and it is why a JVM can be
killed while every Java-level tool still reports a perfectly healthy heap. The limit has to cover:

- the heap (`-Xmx`)
- plus Metaspace, the code cache, thread stacks, direct byte buffers, garbage-collector bookkeeping,
  and the JVM itself

Set `-Xmx` equal to the pod's limit and there is nothing left for any of that. The kernel stops the
container. Kubernetes reports the reason as **`OOMKilled`** and an exit code of **137**, restarts
the pod, and the cycle repeats.

If you have ever seen a Java service in a restart loop with no obvious cause in the application
logs, this is a strong candidate.

---

## What you will actually see in the session

A single application, run four ways, with one setting changed each time:

1. **No limit** — it runs, and consumes whatever it likes.
2. **A 1 GB limit, with the heap set to 1 GB** — killed almost immediately. The limit covers more
   than the heap.
3. **A 1 GB limit, with the heap set as a percentage of it** — survives startup, then develops a
   sawtooth memory graph and increasingly frequent garbage collections under load.
4. **The same, plus one more flag** — stable.

Step 3 to step 4 is a real production incident, reproduced: a service on a 2 GB container, returning
5–6 MB responses, repeatedly killed, and described by the people who lived it as *hard to reproduce
locally*. We will reproduce it in about a minute.

You will also see the three commands that make the diagnosis, which are worth knowing even if you
never write a Kubernetes manifest:

```
kubectl top pod                    # what it is actually using
kubectl describe pod <name>        # why it was killed, and how often
kubectl logs <name> --previous     # the logs from before the last restart
```

That last one matters: when a container is killed and restarted, its logs go with it. `--previous`
is how you read the evidence from the run that died.

---

## What you do not need to do

- **You do not need to install Kubernetes.** These demonstrations run on the trainer's machine.
- **You do not need to install Docker** to follow them.
- **You will still do the hands-on work.** The lab that reproduces the failure above needs nothing
  but a JDK, because the underlying JVM behaviour is identical with or without a container. The
  container only changes *who* enforces the ceiling.

If you want to run the Kubernetes parts yourself, Part 3 of the setup playbook covers it. It is
genuinely optional.

---

## Diagrams to include

Take these from the existing *Kubernetes training* deck rather than redrawing them:

- **Slide 5** — Kubernetes architecture, components and interactions
- **Slide 8** — high-level architecture overview
- **Slide 9** — master and node processes

One architecture diagram is enough for this audience. The features list — rollouts, self-healing,
service discovery, secrets — is not needed here and will pull attention away from the memory story,
which is the only part of Kubernetes this course actually depends on.
