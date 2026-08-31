# Paper → Core Migration Checklist

Status: complete.

This is the historical mirror-layer checklist. The implementation has since moved beyond the paths
and names used by the original notes: core owns the domain objects and manager concepts, while Paper
contains only Bukkit/Paper implementations and integration wiring.

## 1. Mirror layer eliminated

- [x] Deleted Paper `Quest`, `ActiveQuest`, `ActiveObjective`, and `ActiveObjectiveHolder` mirrors.
- [x] Deleted Paper `Objective` and `ObjectiveHolder` mirrors.
- [x] Replaced Paper `QuestPlayer` mirror state with the native-only `PaperPlayer` implementation of
  core's explicit `PlatformPlayer` contract.
- [x] Core `QuestPlayer`, `ActiveQuest`, `ActiveObjective`, and `ActiveObjectives` own runtime state.
- [x] NeoForge and Paper use the same core quest/objective state; neither adapter maintains a second
  quest graph.

## 2. Platform manager layer eliminated

- [x] Deleted Paper `BackupManager`, `QuestManager`, `QuestPlayerManager`, `DataManager`,
  `LanguageManager`, `LogManager`, `UpdateManager`, `PerformanceManager`, `UtilManager`,
  `CommandManager`, and tag-manager wrappers.
- [x] Moved configuration, database, language, logging, updates, quest loading, player loading, and
  save/reload behavior into their familiar core manager owners.
- [x] Paper has no `managers` package and no production type whose name ends in `Manager`.
- [x] NeoForge has no manager layer.

## 3. Platform adapters reduced to native leaves

- [x] Paper and NeoForge event listeners translate native events into core calls.
- [x] Paper and NeoForge GUI code only renders core-owned GUI data and applies native clicks.
- [x] Conversation renderers only materialize core-owned display messages/options.
- [x] Command compilers only translate the core command graph into native Brigadier nodes.
- [x] NPC and armor-stand adapters only perform native lookup, metadata, rendering, and integration
  effects; core owns attachment decisions and messages.
- [x] Optional Paper plugin integrations remain platform-local, but shared NotQuests definitions and
  behavior are registered through the core registry.
- [x] Shared platform contracts have no fallback implementations; adding a required capability
  causes both adapters to fail compilation until parity is supplied.

## 4. Persistence and compatibility

- [x] Core owns `general.yml`, language files, configured-data persistence, player database schema,
  connection lifecycle, and save/reload orchestration.
- [x] The released 6.3.0 player tables are still the canonical schema, so an unmodified 6.3.0
  database loads directly without an upgrade branch or permanent compatibility code.
- [x] Released 6.3.0 YAML/item/location conversion is isolated in
  `core/migrations/v6_3_0/Version630Migration.java`.
- [x] Current production readers consume only the canonical v7 format; there is no permanent legacy
  fallback in adapters or normal core code.
- [x] Both adapters materialize the same canonical journal fields, and both protect journal items in
  normal and creative inventory paths.

## 5. Guardrails and verification

- [x] Architecture tests reject platform manager layers, platform persistence policy, portable
  command behavior, direct progress-engine access, and platform API imports in core/builtin.
- [x] Paper and NeoForge capability parity is compile-time enforced and architecture-tested.
- [x] The exhaustive historical findings are checked in
  [`adapter-boundary-violations.md`](adapter-boundary-violations.md).
- [x] The final architecture checklist is [`plan.md`](plan.md).
- [x] Final verification is a clean `./gradlew build` plus `git diff --check`.
