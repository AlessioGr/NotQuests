# Server-side E2E command sweep

Boots a **real Paper server** with the freshly built plugin and runs every registered command type
through the **console** (no Minecraft client involved), then asserts two things:

- **Coverage** — every objective / action / condition / trigger type registered in the source
  (minus documented integration-gated and variable-based types) has at least one test command in
  [`commands.txt`](commands.txt). Add a new type without a test command and the sweep fails.
- **Correctness** — no command produces a parser/handler error, exception, or stack trace, except
  commands explicitly annotated as tolerated.

## Run it locally

```bash
./e2e/run-sweep.sh        # boots :plugin:runServer, drives the console, then analyzes the log
```

Exit code 0 = pass. The full server log is written to `/tmp/nq-e2e-server.log`.

In CI it runs as the **E2E command sweep** workflow on every push/PR.

## Files

- `commands.txt` — the command list, one per line, dependency-ordered (tags → actions → quests).
  Append `# PLAYER-ONLY`, `# NEEDS-ECONOMY`, `# UNSURE`, or `# OBJECTIVE-SCOPED` to a line to mark it
  *tolerated*: it still runs (for completeness) but its errors don't fail the build.
- `run-sweep.sh` — boots the server, feeds the commands (each tagged with an `NQE2E_MK` marker so
  output maps back to its command), stops, and calls the analyzer.
- `analyze.py` — checks coverage (against the source registries) and correctness (against the log).

## What it does *not* cover

Anything that only a connected client exercises: client-side tab-completion rendering, inventory/GUI
clicks, the action-bar hint, clicking NPCs, chat buttons, or progressing an objective by really
performing it. Those need a headless client bot, which currently has no library supporting this
server's protocol version — see the discussion in the project notes.
