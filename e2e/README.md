# Server-side E2E command sweep

Boots **real Paper and NeoForge servers** with freshly built artifacts and runs the same core-owned
command list through each **console** (no Minecraft client involved), then asserts two things:

- **Coverage** — every objective / action / condition / trigger type registered in the source
  (minus documented integration-gated and variable-based types) has at least one test command in
  [`../src/core/e2e/commands.txt`](../src/core/e2e/commands.txt). Add a new type without a test
  command and the sweep fails.
- **Correctness** — no command produces a parser/handler error, exception, or stack trace, except
  commands explicitly annotated as tolerated.
- **Parity** — the command graph and portable registry metadata generated at runtime by Paper and
  NeoForge must match exactly. Integration-only entries are intentionally excluded from that
  comparison.

## Run it locally

```bash
./e2e/run-sweep.sh            # boots :paper:runServer, drives the console, then analyzes the log
./e2e/run-neoforge-sweep.sh   # boots :neoforge:runServer with the exact same command list
./src/core/e2e/compare-platform-metadata.py
```

Exit code 0 = pass. The full server logs are written to `/tmp/nq-e2e-paper-server.log` and
`/tmp/nq-e2e-neoforge-server.log`.

In CI it runs as the **E2E command sweep** workflow on every push/PR.

## Files

- `../src/core/e2e/commands.txt` — the command list, one per line, dependency-ordered
  (tags → actions → quests). This is the canonical list used by Paper and NeoForge.
  Append `# PLAYER-ONLY`, `# NEEDS-ECONOMY`, `# UNSURE`, or `# OBJECTIVE-SCOPED` to a line to mark it
  *tolerated*: it still runs (for completeness) but its errors don't fail the build.
- `../src/core/e2e/run-server-sweep.sh` — shared platform-aware runner.
- `../src/core/e2e/analyze.py` — checks coverage (against runtime metadata), correctness (against
  the log), and the runtime metadata bundle written by the platform.
- `../src/core/e2e/compare-platform-metadata.py` — compares Paper and NeoForge's generated command
  graph and portable registry metadata.
- `run-sweep.sh` and `run-neoforge-sweep.sh` — thin compatibility wrappers around the core runner.
- `run-betonquest-sweep.sh` — boots the server with the vendored BetonQuest 3.0.0 jar, loads a
  tiny BetonQuest package containing every restored `nq_*` hook, and verifies the NotQuests
  BetonQuest action / reward / objective / variable commands from the console.

## What it does *not* cover

Anything that only a connected client exercises: client-side tab-completion rendering, inventory/GUI
clicks, the action-bar hint, clicking NPCs, chat buttons, or progressing an objective by really
performing it. Those need a headless client bot, which currently has no library supporting this
server's protocol version — see the discussion in the project notes.
