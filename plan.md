# Core Restoration Completion Notes

## 1. Final Architecture

The adapter split remains, but the core vocabulary and discoverability now follow the 6.3-era
codebase wherever a platform-neutral implementation permits it.

```text
src/
├── core/                       NotQuests itself: behavior, state, persistence, commands, config
│   └── com/notquests/core/
│       ├── managers/
│       │   ├── ConfigurationManager.java
│       │   ├── DataManager.java
│       │   ├── LanguageManager.java
│       │   ├── LogManager.java
│       │   ├── QuestManager.java
│       │   ├── QuestPlayerManager.java
│       │   ├── CommandManager.java
│       │   ├── UpdateManager.java
│       │   ├── BackupManager.java
│       │   ├── UtilManager.java
│       │   ├── PlayerDatabase.java
│       │   ├── integrations/IntegrationsManager.java
│       │   └── tags/TagManager.java, TagType.java
│       ├── structs/
│       │   ├── Quest.java
│       │   ├── QuestPlayer.java
│       │   ├── ActiveObjective.java
│       │   ├── ActiveObjectives.java
│       │   ├── Category.java
│       │   └── PredefinedProgressOrder.java
│       ├── conversation/       ConversationManager, Conversation, ConversationLine, Speaker
│       ├── gui/                GuiService, GuiContext
│       ├── actions/            SavedActions
│       ├── conditions/         ConditionCheck
│       ├── triggers/           ActiveTrigger
│       ├── items/              SavedItem, SavedItems, ItemSelection, ItemStackSelection
│       ├── npc/                NpcAttachments, ArmorStandAttachments, NQNPCID
│       ├── config/             YamlConfig, CategoryFiles
│       ├── migrations/         ConfigurationMigrations + version-specific migrations
│       ├── commands/           one file per command feature
│       ├── commands/framework/ reusable command graph/compiler model
│       ├── registry/           internal registry implementation and field definitions
│       ├── platform/           explicit adapter and platform capability contracts
│       └── NotQuestsPlugin.java core facade/lifecycle composition
├── builtin/                    portable built-in actions, conditions, objectives, triggers, variables
├── paper/                      Bukkit/Paper leaves and Paper-only integrations
└── neoforge/                   NeoForge leaves
```

Paper no longer contains `DataManager`, `LanguageManager`, `LogManager`, `QuestManager`,
`QuestPlayerManager`, `UpdateManager`, or `UtilManager`. General configuration, database
connections, SQL transactions, YAML persistence, translations, logging policy, updates, quests,
profiles, progress, and commands are core-owned. Platform modules translate events and implement
atomic effects only.

## 2. Architecture Goal

- Keep the multi-platform adapter boundary, but retain the original NotQuests vocabulary.
- Prefer one substantial concept owner over a hierarchy of small pattern-named files.
- Keep state and behavior together when they describe the same plugin concept.
- Use larger files when that makes a complete feature readable in one place.
- Avoid `View`, `State`, `Simple`, `Engine`, `Identity`, `Provider`, `Resolver`, `Rules`, `Diff`,
  `Tick`, `Details`, `Flow`, `Plan`, and `Mirror` names unless they are part of the intentionally
  retained command framework or registry DSL.
- Keep Paper and NeoForge free of core orchestration, persistence decisions, command behavior,
  validation text, and shared state.
- Preserve 6.3 behavior and configuration compatibility through explicit one-off migrations; do
  not keep permanent legacy branches in normal runtime loaders.

## 3. Completed Checklist

### Configuration and persistence

- [x] Consolidated all general.yml defaults, comments, versioning, loading, saving, and runtime
      settings into core `ConfigurationManager`, restoring the released 6.3 owner name.
- [x] Consolidated configured YAML and YAML runtime persistence into core `DataManager`.
- [x] Moved SQL schema, connections, snapshots, transactional loading/saving, and tag persistence
      into core `PlayerDatabase`/`TagManager`.
- [x] Kept the released 6.3 `QuestPlayerData`, profile, quest, objective, trigger, and tag tables as
      the supported upgrade source, including nested objective holder paths; no unreleased v7 beta
      schema bridge is retained.
- [x] Restored database rows in explicit loading mode so unlock hooks receive `loading=true`, saved
      progress cannot be recalculated by integrations, and completed nested children cannot fire
      ordinary parent completion rewards/events while data is being reconstructed.
- [x] Removed the generic persistence interface and platform persistence orchestration.
- [x] Made normal reload preserve live quest/player/database runtime; kept full reload behind the
      explicit unsafe debug operation.
- [x] Kept profiles isolated for quests, objectives, triggers, loading, saving, and switching.
- [x] Preserved YAML block, inline, leading, blank-line, and trailing comments during normal saves
      and migrations.
- [x] Persisted registry fields through their declared config paths, including inverted booleans.

### One-off 6.3 migration and lossless current formats

- [x] Added an idempotent 7.0 migration that first normalizes every remaining 6.3 quest/action
      field shape, even when the stored migration version is already 6.3.0.
- [x] Migrated legacy Bukkit locations to the portable location format with world, x/y/z, yaw,
      and pitch.
- [x] Made Paper and NeoForge consume every portable location implementation and preserve its
      yaw/pitch when an action does not provide an explicit rotation override.
- [x] Migrated legacy material, nq-item, and exact Bukkit ItemStack selections to the canonical
      item-selection shape.
- [x] Preserved complete exact Paper item payloads, including amount and metadata, rather than
      reducing them to a material name.
- [x] Kept exact saved-item payloads round-trippable where native item identity is the feature,
      while converting the 6.3 journal completely to one canonical cross-platform description.
- [x] Migrated legacy action-chain, GiveItem, block/item objective, and NPC selector paths.
- [x] Migrated pre-6.3 quest limits/cooldowns and structured 6.3 completion-NPC maps into the one
      canonical v7 shape, then removed their aliases from normal runtime loaders.
- [x] Corrected and guarded every active 6.3 builtin/Paper integration config path.
- [x] Made changed files migrate once, left unchanged files and backups byte-for-byte untouched,
      and preserved administrator comments.

### Restored behavior

- [x] Restored custom GUI action execution, registry conditions, saved actions/conditions, and
      placeholders while Paper remains only the renderer.
- [x] Restored the complete GUI context (target player, quest/category, NPC type/id, tab and page),
      tab selection, multiline lore tokens, and pagination controls for tabbed and ordinary GUIs.
- [x] Restored synchronous cancellable objective-unlock events and server-thread NPC integration
      operations.
- [x] Restored configured conversation/speaker/line delays through the core scheduler, including
      stale-session cancellation and delayed terminal lines.
- [x] Restored forced quest completion semantics, unfinished-objective cleanup, forced event flag,
      and silent forced GiveQuest behavior.
- [x] Restored cow bucket/hand filtering, prevention of off-hand double counting, and cancellable
      milking/shearing behavior.
- [x] Restored BroadcastMessage placeholder resolution.
- [x] Restored Enchant min/max expressions and human-readable range task text.
- [x] Restored NPC display names in DeliverItems/TalkToNPC task text with safe fallbacks.
- [x] Restored number/boolean expressions in variable actions, conditions, and typed variable
      arguments, including the 6.3 boolean threshold.
- [x] Restored exact ItemStack-list comparison through native Paper/NeoForge item similarity,
      including metadata, aggregate amounts, `any`, `equals`, and `contains` behavior.
- [x] Restored NPCDeath trigger field matching and KillMobs self-kill filtering from explicit
      platform event facts.
- [x] Restored lifecycle-safe `ActiveQuests` and nested completed-objective variable mutation,
      including validation, forced-silent acceptance, conditions, rewards, triggers, and cleanup.
- [x] Restored independent cooldown/max-accept/max-completion/max-fail variables and the QuestPoints
      `notifyPlayer` flag.
- [x] Restored the four independently translated actionbar/bossbar objective-progress messages.
- [x] Restored familiar Paper event getters as direct read-only core-backed event data,
      without recreating Paper quest/player/objective mirrors.

### Naming and organization

- [x] Restored `structs` domain names and renamed the platform capability to `PlatformPlayer`.
- [x] Added real core `QuestManager` and `QuestPlayerManager` owners; removed duplicate maps from
      `NotQuestsPlugin`.
- [x] Restored `ConversationManager`, `GuiService`, `ItemStackSelection`, and familiar manager
      names; renamed the vague `NQItem` to `SavedItem` and made `SavedItems` its core owner.
- [x] Replaced the catch-all quest entry object with concrete `Objective`, `Condition`, `Action`,
      and `Trigger` domain classes that own their values and implement registry callbacks directly.
- [x] Restored core-owned `ActiveQuest` objects and made each `QuestPlayer` own its accepted quests
      and their active-objective trees without a Paper mirror or global objective-state map.
- [x] Consolidated action, condition, objective, trigger, and variable metadata, builders, parsers,
      callbacks, and registered types into `NotQuestsRegistry`, as selected during the registry
      review. Builtins retain the familiar `*Action`, `*Condition`, `*Objective`, `*Trigger`, and
      `*Variable` names; there are no extra catalog classes.
- [x] Restored old-style `*Action`, `*Condition`, and `*Objective` builtin class names while keeping
      registry IDs unchanged.
- [x] Kept command helpers/framework separate from actual command definitions, restored the 6.3
      `NQArgumentType`, `NQCommandContext`, `NQFlag`, and `NQSuggestionProvider` names, and folded
      tiny argument/flag/node/root/resolver files into `NQCommandSchema` and `NQCommandBuilder`.
- [x] Removed the fake command player; command targets now carry core `QuestPlayer` state and an
      optional real `PlatformPlayer` capability explicitly.
- [x] Moved platform contracts out of `registry/runtime` into `core.platform` and moved item
      selection beside the other item concepts.
- [x] Merged the root command definition into `NotQuestsCommands`, moved command metadata/messages
      to `CommandManager`, and removed unnecessary `Core` prefixes from feature command classes.
- [x] Removed `NotQuestsMainAbstract`, standalone plugin status/disable fragments, the tiny migration
      interface, and the standalone NPC marker interface by folding them into concept owners.
- [x] Removed obsolete compatibility shim classes and empty source directories.
- [x] Removed the public `ConversationData`, `LineData`, and `SpeakerData` DTO vocabulary. Core now
      exposes the familiar `Conversation`, `ConversationLine`, and `Speaker` concepts directly from
      `ConversationManager` while keeping mutable storage private.
- [x] Removed the generic `PlayerRuntimeInfo` adapter DTO. Platform player creation is now the
      atomic `createPlayer(playerIdentifier)` leaf; profiles, quest points, and loading remain in
      core.
- [x] Restored `ConfigurationManager`, `CompletedObjectiveIDsOfQuestVariable`, `*Trigger` builtin
      suffixes, and database/test package names that had drifted without a functional benefit.
- [x] Replaced vague nested `*Details`, `*Result`, metadata `*Info`, resolver, plan, view, state, and
      identity vocabulary with concrete settings, operations, metadata members, player data, GUI,
      attachments, and item keys wherever those words did not describe a real framework contract.
- [x] Folded one-use support/version helpers into their concept owners and removed support-only
      source directories.

### Verification

- [x] Added migration, exact-item, config-path, profile isolation, reload, conversation timing,
      forced completion, livestock event, GUI, current event-context, and architecture tests.
- [x] Core/builtin contain no Bukkit, Paper, NeoForge, or Minecraft platform imports.
- [x] Paper and NeoForge architecture tests guard the adapter boundary.
- [x] Removed fallback implementations from `PlatformPlayer`, `NotQuestsAdapter`, and platform
      objective-event capabilities so every adapter must implement parity explicitly. Raw
      location, item-selection, objective-progress, conversation-display, YAML-codec, and database
      logging contracts are explicit too; only platform-independent derived helpers retain defaults.
- [x] `./gradlew build` passes across core, builtin, Paper, and NeoForge.
- [x] `git diff --check` passes.

## 4. Deliberate Exceptions

- The command framework keeps the familiar 6.3 `NQ*` names; suggestion resolution is now a method
  of `NQCommandBuilder` instead of a standalone helper.
- `CommandSupport` is one package-private home for the small markup and player-target helpers shared
  by eleven command features. Keeping those 88 lines together avoids duplicating the same command
  text and online-player lookup without introducing a public framework or command-operation layer.
- The registry builder DSL keeps builder/handler interfaces because builtins and both adapters use
  them as the supported extension API.
- `ConditionCheck.Result`, `DoubleDashFlagParser.ParseResult`, and the command/registry suggestion
  provider contracts retain those names because they describe an actual check/parse/callback
  contract rather than a split domain object.
- `SimpleGradientTag` keeps its upstream MiniMessage-style name.
- `NotQuestsPlugin` remains the stable public core facade, but authoritative repositories and
  cohesive behavior now live in the named managers/concept owners above.
