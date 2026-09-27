# Java Performance Training — setup before we start

**We do this together in the first hour of day one**, as the first hands-on exercise. If you get
through it beforehand, even better — come anyway, and help your neighbour.

**Tested on:** Windows 10 and Windows 11.

### How the first hour runs

| | |
|---|---|
| **Part 1 — the essentials** | ~20 minutes. Everyone. This is the part that matters |
| **Part 2 — containers** | ~20 minutes. Recommended, not required |
| **Part 3 — Kubernetes** | ~15 minutes. Optional — I will run these demos live regardless |

Two rules for the hour:

**Do not get stuck.** If a step has not worked after five minutes, raise your hand. If we cannot
fix it quickly, move to the next part and come back — everything after Part 1 is optional, and no
step blocks the course.

**Stopping after Part 1 is a complete result.** Part 1 alone is enough for the hands-on labs,
including the one that reproduces a real production failure. Parts 2 and 3 add depth, not
admission.

---

## What you are installing, and why

The course is hands-on: you will reproduce a performance failure, investigate it with real tools,
fix it, and measure the difference. That needs three things, in decreasing order of importance.

**Part 1 — the JDK. Everyone needs this.**
It is enough on its own for most of the labs, including the one that reproduces a real production
incident: a container killed repeatedly because large responses were bypassing the garbage
collector's normal path. You will make that happen on your own machine with a single command.

**Part 2 — a container runtime. Recommended.**
Lets you see what changes when the JVM runs under a hard memory limit, which is where most
cloud-hosted Java surprises come from.

**Part 3 — Kubernetes. Optional.**
Only needed if you want to run the pod-level demonstrations yourself. **You do not need it to
follow along** — I will run these live, and you can watch. Install it if you are curious or if your
own work is on Kubernetes.

If you only finish Part 1, you will be fine.

---

## Part 1 — The essentials (everyone)

### 1.1 Install two JDKs

We use **Java 17 and Java 21**. Several things the course covers changed between them, and seeing
the same code behave differently is part of the point.

Open **PowerShell** and run:

```
winget install EclipseAdoptium.Temurin.17.JDK
winget install EclipseAdoptium.Temurin.21.JDK
```

*No `winget`?* Download the MSI installers from https://adoptium.net/temurin/releases/ — choose
Windows, x64, JDK, versions 17 and 21. Accept the default options.

**Check it worked.** Close PowerShell, open a new one, and run:

```
java -version
```

You should see a version line. Either 17 or 21 is fine at this stage.

### 1.2 Find where each JDK lives

```
Get-ChildItem "C:\Program Files\Eclipse Adoptium"
```

You should see two folders, one per version. Note the full paths — you will switch between them
during the course like this:

```
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.x.x-hotspot"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
java -version
```

Substitute the exact folder name you saw. Run it once now with the 21 path so you know it works.

*Optional:* **Java 25** is used in one comparison. Install it the same way
(`winget install EclipseAdoptium.Temurin.25.JDK`) if you would like to follow that part hands-on.
Skipping it costs you nothing else.

### 1.3 Install Git

```
winget install Git.Git
```

Check: `git --version`

### 1.4 Get the training project

Pick a folder you can find again, then:

```
git clone https://github.com/bogdansolga/java-performance-training.git
cd java-performance-training
.\mvnw.cmd -q package -DskipTests
```

The first build downloads dependencies and may take a few minutes. **If you can run this before
the day, please do** — a room full of first-time Maven builds on one Wi-Fi connection is slow. If
not, start it now and read ahead while it works.

**Check it worked:** the command finishes without an error, and a `target` folder appears.

### 1.5 Confirm the interesting bit actually runs

This is the one command that matters most. It starts the app with a deliberately small heap and
asks the JVM to explain what the garbage collector is doing:

```
java -Xmx1g -Xlog:gc+heap=debug -version
```

You should see several lines of GC configuration. If you do, Part 1 is complete and **you are ready
for the course.**

---

## Part 2 — A container runtime (recommended)

This is what lets you see the JVM discover it is inside a container with a hard memory ceiling.

### 2.1 Enable WSL2

Windows runs Linux containers through WSL2. In PowerShell **as Administrator**:

```
wsl --install
```

Restart when prompted. If it says WSL is already installed, run `wsl --update`.

### 2.2 Cap WSL2's memory — please do not skip this

By default WSL2 will help itself to **half your RAM**, and under load it can climb further and make
the machine unresponsive. One small file prevents that.

Create `C:\Users\<you>\.wslconfig` with exactly this content:

```
[wsl2]
memory=4GB
processors=2
```

Then apply it:

```
wsl --shutdown
```

*If you have 8 GB of RAM or less,* use `memory=3GB` instead.

### 2.3 Install a container runtime

**Podman Desktop** is free for any use and is the lightest option:
https://podman-desktop.io/downloads — download, install, accept the defaults.

*Alternative:* Docker Desktop works too, and is free for personal use. If your employer is large
and you would be using it for work, they may need a licence — Podman avoids that question entirely.

**Check it worked:**

```
podman run --rm hello-world
```

(or `docker run --rm hello-world`)

### 2.4 Confirm the JVM sees a container limit

```
podman run --rm -m 1g eclipse-temurin:21 java -XX:+PrintFlagsFinal -version | findstr MaxHeapSize
```

The number printed should be **smaller than your physical RAM** — that is the JVM reading the
container's limit rather than the machine's. That behaviour is the subject of a whole session.

---

## Part 3 — Kubernetes (optional)

Only if you want to run the pod-level demonstrations yourself. **Skipping this costs you nothing —
I will run them live.**

In PowerShell:

```
wsl --install -d Ubuntu
```

Open Ubuntu from the Start menu, set a username and password, then inside Ubuntu:

```
sudo nano /etc/wsl.conf
```

Add these two lines, save with `Ctrl+O`, `Enter`, exit with `Ctrl+X`:

```
[boot]
systemd=true
```

Back in PowerShell:

```
wsl --shutdown
```

Reopen Ubuntu and install a small Kubernetes:

```
curl -sfL https://get.k3s.io | sh -
sudo k3s kubectl get nodes
```

One node listed as `Ready` means it works.

---

## Final check

Run these and show me the output before we move on.

```
java -version
git --version
podman --version
```

Then, in the project folder:

```
.\mvnw.cmd -q package -DskipTests
```

If `java -version` and the build both succeed, **you are ready**, whatever happened in Parts 2 and 3.

---

## If something goes wrong

**`winget` is not recognised.** You are on an older Windows 10. Use the installers from
https://adoptium.net/temurin/releases/ instead.

**`java -version` still shows an old Java after installing.** Close every PowerShell window and open
a new one — the PATH is only read at startup.

**`wsl --install` fails or asks for virtualisation.** Virtualisation is disabled in your BIOS/UEFI.
This is worth fixing but not urgent — skip Parts 2 and 3 and tell me.

**The build fails on a corporate VPN or proxy.** Maven cannot reach the internet. Try off the VPN;
if it still fails, send me the error.

**WSL2 is using a lot of memory.** The `.wslconfig` from 2.2 is missing or was not applied. Check
the file location and spelling, then run `wsl --shutdown` again.

**Anything else** — show me the command you ran and everything it printed. Do not spend more than
five minutes stuck on any single step during the session; raise your hand instead. That is what the
hour is for, and an unfinished Part 2 costs you nothing.
