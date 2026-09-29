# Deck 1 — Java performance management overview — pass 2

> **STATUS: APPLIED 2026-09-29** — applied by `deck-apply.sh` and independently verified on a fresh dump (each replacement present exactly once, each deleted anchor gone). **DO NOT RE-RUN `deck-apply.sh` ON THIS FILE.**

Target: `1wpiNHmcXNkXEmwF09xJS9bv4GAex6IQxiBzt_m0X15c`. Follow-up to `01-perf-management.md`
(applied 2026-09-29). That pass redefined client class by CPU count and memory instead of
32-bit-ness, which left slide 4's server-class line claiming "all 64-bit JVMs" — false now
that a 64-bit JVM in a 1-CPU cgroup is client class. Anchor verified unique deck-wide
against a live read on 2026-09-29.

| Slide | Anchor | Current context | Proposed text | Category | Source |
|---|---|---|---|---|---|
| 4 | (including all 64-bit JVMs) | Server class machines - all other machines (including all 64-bit JVMs) | <DELETE> | removal | https://github.com/openjdk/jdk/blob/master/src/hotspot/share/runtime/os.cpp (os::is_server_class_machine) |
