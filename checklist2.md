# Superseded Paper → Core Checklist

This was the intermediate migration checklist. Its work has been reconciled into the exhaustive
[adapter boundary checklist](adapter-boundary-violations.md) and the final [completion plan](plan.md).

- [x] Core owns NotQuests behavior, state, persistence, configuration, database access, language,
      logging, updates, commands, GUI decisions, conversations, migrations, and shared messages.
- [x] Paper and NeoForge contain only atomic native operations, observations, event translation,
      rendering, external-plugin calls, and dependency wiring.
- [x] Paper contains no NotQuests manager layer or Paper quest/objective mirror graph.
- [x] Paper and NeoForge implement every required shared capability explicitly.
- [x] All 6.3 compatibility conversion is isolated in `core/migrations/v6_3_0` and the current
      runtime reads only the canonical v7 format.
- [x] Every violation from the complete adapter declaration audit is addressed and checked off.
- [x] Full cross-module build and architecture verification pass.

Do not add new migration work here; update `plan.md` and the architecture tests instead.
