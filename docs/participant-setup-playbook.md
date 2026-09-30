# Java Performance Training - local Kubernetes setup

**You have about 30 minutes to work through this on your own, then we regroup.**

Tested on Windows 10 and Windows 11. If you already did some of it beforehand, skip ahead - and if
you finish early, say so in the chat and I will point you at whoever is stuck.

### Three things to know before you start

**Do not get stuck.** If a step has not worked after five minutes, paste the command and its output
in the Zoom chat and move on to the next one. We are all working in parallel, so the chat is the
only way I can see what is happening on your machine. I will pick things up there as they appear.

**Nothing here blocks the course.** I run every Kubernetes demonstration live, so if you end the 30
minutes without a cluster you still see everything. The hands-on labs need only the JDK and the
project from Step 1 - how to run them: [gatling-labs.md](gatling-labs.md).

**Step 2 may ask you to restart Windows.** That is normal. You will drop off the call - just rejoin
when you are back, and carry on where you left off. If a restart will not fit in the time, skip to
the end and say so in the chat; we can finish it later.

---

## What you already have

**JDK and IDE** - already on your machine as course prerequisites. We will use them as they are;
nothing to install. **Whichever version you have is fine** - 17 or 21. A few points in the course
differ between them; where that happens I will say so and show both, so you lose nothing by having
one. The virtual-threads comparison needs JDK 21 and JDK 25, so I run that one on my machine.

---

## Step 1 - Get the training project

Pick a folder you can find again, then in **PowerShell**:

```
git clone https://github.com/bogdansolga/java-performance-training.git
cd java-performance-training
.\mvnw.cmd -q package -DskipTests
```

The first build downloads its dependencies and takes a few minutes. Start it now and read ahead
while it runs - there is no reason to sit and watch it.

**Check it worked:** the command finishes without an error and a `target` folder appears.

---

## Step 2 - Enable WSL2

Both cluster options run on Linux, and WSL2 is how Windows provides it. It is built into
Windows 10 and 11, free, and needs no other container tooling.

In PowerShell **as Administrator**:

```
wsl --install -d Ubuntu
```

Restart when prompted. If it says WSL is already installed, run `wsl --update` instead.

Open **Ubuntu** from the Start menu and set a username and password when asked.

### 2.1 Cap WSL2's memory - please do not skip this

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

`running` or `degraded` are both fine. An error means systemd did not start - check the spelling in
`/etc/wsl.conf` and run `wsl --shutdown` again.

---

## Step 3 - Install a cluster: k3s **or** k0s

Pick one. **You do not need both.** Neither requires Docker or any other container runtime - each
brings its own.

**k3s** is the simpler install and the one I will use in the session. **k0s** is the alternative if
you would rather try it; it is equally capable here.

### Option A - k3s

Inside Ubuntu:

```
curl -sfL https://get.k3s.io | K3S_KUBECONFIG_MODE="644" sh -
sudo k3s kubectl get nodes
```

**Check it worked:** one node listed, status `Ready`.

k3s installs `kubectl` for you, and `K3S_KUBECONFIG_MODE="644"` above made its config readable, so
it works straight away:

```
kubectl get nodes
```

**Check it worked:** the same node listed, this time with no `sudo` and no `k3s`.

**Why that env var matters.** k3s writes its kubeconfig to `/etc/rancher/k3s/k3s.yaml`, readable by
root only, and its bundled `kubectl` reads *that file* - not `~/.kube/config`. Without the mode
setting, plain `kubectl` fails with `permission denied` no matter what you copy where. If you
already installed k3s without it: `sudo chmod 644 /etc/rancher/k3s/k3s.yaml`.

### Option B - k0s

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

k0s does **not** install `kubectl` for you, and the rest of the course assumes plain `kubectl`
works. Install it and point it at your cluster:

```
curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
sudo install -o root -g root -m 0755 kubectl /usr/local/bin/kubectl

mkdir -p ~/.kube
sudo k0s kubeconfig admin | tee ~/.kube/config > /dev/null
chmod 600 ~/.kube/config
kubectl get nodes
```

**Check it worked:** the same node listed, this time with no `sudo` and no `k0s`.

---

## Final check

Paste the output of these in the Zoom chat when you have it.

In **PowerShell**, from the project folder:

```
java -version
.\mvnw.cmd -q package -DskipTests
```

In **Ubuntu**:

```
kubectl get nodes
```

Plain `kubectl`, no `sudo` - that is the thing to confirm, because everything later depends on it.

**If the build succeeds you are ready**, whatever happened with the cluster.

---

## If something goes wrong

**`wsl --install` fails, or mentions virtualisation.** Virtualisation is disabled in your BIOS/UEFI.
Worth fixing eventually, but skip the cluster for today and say so in the chat.

**`systemctl` reports an error inside Ubuntu.** The `/etc/wsl.conf` edit did not take. Check the
file contents, then `wsl --shutdown` in PowerShell and reopen Ubuntu.

**`kubectl` says `permission denied` on `/etc/rancher/k3s/k3s.yaml`.** You installed k3s without
`K3S_KUBECONFIG_MODE="644"`. Fix it without reinstalling: `sudo chmod 644 /etc/rancher/k3s/k3s.yaml`

**`k3s kubectl get nodes` says the node is `NotReady`.** Wait a minute and try again - it takes a
moment on first start. If it persists, `sudo systemctl status k3s` will say why.

**WSL2 is eating memory.** The `.wslconfig` from 2.1 is missing or misplaced. It belongs in your
Windows user folder, not inside Ubuntu, and needs `wsl --shutdown` to take effect.

**The build fails behind a VPN or proxy.** Maven cannot reach the internet. Try without the VPN; if
it still fails, paste the error in the Zoom chat.

**Anything else** - paste in the Zoom chat the command you ran and everything it printed. Do not
spend more than five minutes stuck on any single step; post it in the chat and carry on. I will
pick it up there. An unfinished cluster costs you nothing.
