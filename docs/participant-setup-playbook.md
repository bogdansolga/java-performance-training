# Java Performance Training — local Kubernetes setup

**We do this together in the first hour of day one**, as the first hands-on exercise. If you get
through it beforehand, even better — come anyway and help your neighbour.

**Tested on:** Windows 10 and Windows 11.

### Two rules for the hour

**Do not get stuck.** If a step has not worked after five minutes, raise your hand. If we cannot fix
it quickly, move on — **none of this blocks the course.** I run every Kubernetes demonstration live,
so anyone who does not get a cluster running still sees everything.

**This is optional depth, not admission.** The hands-on labs need only the JDK and the project.
A local cluster lets you run the pod-level parts yourself rather than watching.

---

## What you already have

**JDK and IDE** — already on your machine as course prerequisites. We will use them as they are;
nothing to install. During the course you will switch between **Java 17 and Java 21** to see the
same code behave differently, so have both available.

---

## Step 1 — Get the training project

Pick a folder you can find again, then in **PowerShell**:

```
git clone https://github.com/bogdansolga/java-performance-training.git
cd java-performance-training
.\mvnw.cmd -q package -DskipTests
```

The first build downloads dependencies and may take a few minutes. **If you can run this before the
day, please do** — a room full of first-time Maven builds on one Wi-Fi connection is slow. If not,
start it now and read ahead while it works.

**Check it worked:** the command finishes without an error and a `target` folder appears.

---

## Step 2 — Enable WSL2

Both cluster options run on Linux, and WSL2 is how Windows provides it. It is built into
Windows 10 and 11, free, and needs no other container tooling.

In PowerShell **as Administrator**:

```
wsl --install -d Ubuntu
```

Restart when prompted. If it says WSL is already installed, run `wsl --update` instead.

Open **Ubuntu** from the Start menu and set a username and password when asked.

### 2.1 Cap WSL2's memory — please do not skip this

By default WSL2 helps itself to **half your RAM**, and under load it can climb further and make the
machine unresponsive. One small file prevents that.

In **Windows** (not Ubuntu), create the file `C:\Users\<your-name>\.wslconfig` containing exactly:

```
[wsl2]
memory=4GB
processors=2
```

*If your machine has 8 GB of RAM or less,* use `memory=3GB`.

### 2.2 Turn on systemd

Both k3s and k0s expect it. Inside **Ubuntu**:

```
sudo nano /etc/wsl.conf
```

Add these two lines, save with `Ctrl+O` then `Enter`, and exit with `Ctrl+X`:

```
[boot]
systemd=true
```

Then, back in **PowerShell**, apply both changes:

```
wsl --shutdown
```

Reopen Ubuntu. **Check it worked:**

```
systemctl is-system-running
```

`running` or `degraded` are both fine. An error means systemd did not start — check the spelling in
`/etc/wsl.conf` and run `wsl --shutdown` again.

---

## Step 3 — Install a cluster: k3s **or** k0s

Pick one. **You do not need both.** Neither requires Docker or any other container runtime — each
brings its own.

**k3s** is the simpler install and the one I will use in the session. **k0s** is the alternative if
you would rather try it; it is equally capable here.

### Option A — k3s

Inside Ubuntu:

```
curl -sfL https://get.k3s.io | sh -
sudo k3s kubectl get nodes
```

**Check it worked:** one node listed, status `Ready`.

Make `kubectl` easier to use:

```
mkdir -p ~/.kube
sudo cp /etc/rancher/k3s/k3s.yaml ~/.kube/config
sudo chown $(id -u):$(id -g) ~/.kube/config
kubectl get nodes
```

### Option B — k0s

Inside Ubuntu:

```
curl -sSLf https://get.k0s.sh | sudo sh
sudo k0s install controller --single
sudo k0s start
```

Give it a minute to come up, then:

```
sudo k0s kubectl get nodes
```

**Check it worked:** one node listed, status `Ready`.

---

## Final check

Show me the output of these before we move on.

In **PowerShell**, from the project folder:

```
java -version
.\mvnw.cmd -q package -DskipTests
```

In **Ubuntu**, whichever you installed:

```
kubectl get nodes          # k3s
sudo k0s kubectl get nodes # k0s
```

**If the build succeeds you are ready**, whatever happened with the cluster.

---

## If something goes wrong

**`wsl --install` fails, or mentions virtualisation.** Virtualisation is disabled in your BIOS/UEFI.
Worth fixing eventually, but skip the cluster for today and tell me.

**`systemctl` reports an error inside Ubuntu.** The `/etc/wsl.conf` edit did not take. Check the
file contents, then `wsl --shutdown` in PowerShell and reopen Ubuntu.

**`k3s kubectl get nodes` says the node is `NotReady`.** Wait a minute and try again — it takes a
moment on first start. If it persists, `sudo systemctl status k3s` will say why.

**WSL2 is eating memory.** The `.wslconfig` from 2.1 is missing or misplaced. It belongs in your
Windows user folder, not inside Ubuntu, and needs `wsl --shutdown` to take effect.

**The build fails behind a VPN or proxy.** Maven cannot reach the internet. Try without the VPN; if
it still fails, show me the error.

**Anything else** — show me the command and everything it printed. Do not spend more than five
minutes stuck on any single step; raise your hand instead. That is what the hour is for, and an
unfinished cluster costs you nothing.
