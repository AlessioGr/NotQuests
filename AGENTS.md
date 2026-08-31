# NotQuests Agent Notes

## Core vs Adapter Boundary

Core is the plugin. Platform modules are adapters.

`src/core` owns behavior, orchestration, persistent state shape, command definitions, command handlers,
registry metadata, docs JSON generation, quest runtime, conversations, GUI configuration, migrations,
and shared user-facing messages.

`src/paper` and `src/neoforge` are libraries that connect core to a platform. They should translate
platform events/objects into core calls and implement small platform capabilities that core cannot do
without platform APIs.

## Core Organization Style

Prefer one clear concept owner over many tiny extracted classes. Core classes should read like
NotQuests concepts: `Quest`, `QuestPlayer`, `ActiveObjective`, `ActiveObjectives`,
`Conversations`, `SavedActions`, `ConditionCheck`, `ActiveTrigger`, `GeneralConfig`,
`PlayerDatabase`, `NpcAttachments`, `ArmorStandAttachments`, `Tags`, and similar names.

Avoid new files whose names describe a pattern instead of the game/plugin concept: `View`, `State`,
`Simple`, `Engine`, `Identity`, `Provider`, `Resolver`, `Rules`, `Diff`, `Tick`, `Details`,
`Result(s)`, `Flow`, `Plan`, or `Mirror`. Use those words only when they are already part of a
kept framework surface, such as the command framework or registry builder DSL, or when there is a
specific domain reason. If a helper only makes sense while reading one owner class, make it a
private method or nested record of that owner.

Larger files are acceptable when they own a real NotQuests concept end to end. Do not split a
single concept into interface/base/state/result/helper files just to make the files smaller.

## Command Rules

- Commands are defined in core.
- Command handlers are implemented in core.
- Command output/messages are produced in core.
- Platform modules may only compile/register the core command graph into the platform command API
  (Paper Brigadier, NeoForge Brigadier, etc.).
- Do not add `PaperCommandEffects`, `NeoForgeCommandEffects`, platform command handlers, or any
  command-specific effect class in an adapter.
- If a command needs platform information, add a small generic core adapter method for the exact
  platform fact/action needed, then keep the handler in core.
- Shared adapter leaves that every platform must implement should be abstract interface methods.
  Do not add default implementations that warn "not supported"; those hide missing parity until a
  user hits the command.

## Command Organization Style

Keep each command feature self-contained. Do not split one command across a names enum, an operation
enum, a central switch, and tiny handler methods in a separate file.

Bad:

```java
enum AdminEditOperation { SET_OBJECTIVE_LOCATION_HERE }

command.literal("location").literal("set").literal("here")
        .handler(ctx -> handlers.run(operation));

return switch (operation) {
    case SET_OBJECTIVE_LOCATION_HERE -> setObjectiveLocationHere(ctx);
};
```

Good:

```java
command.literal("location", NQDescription.of("Manages this objective's guiding marker."))
        .literal("set", NQDescription.of("Sets the marker location."))
        .literal("here", NQDescription.of("Uses your current in-game block position."))
        .handler(ctx -> List.of(runner.setObjectiveLocation(
                quest(ctx), objectiveId(ctx), player(ctx).worldName(), player(ctx).blockX(), player(ctx).blockY(), player(ctx).blockZ())));
```

Small reusable helpers are fine when they remove real duplication, such as `objectiveId(context)`,
`player(context)`, or `conditionGroup(group)`. Do not introduce command-operation enums, central
command switches, command-specific effect classes, or “layout” files that make the behavior hard to
read from the command registration itself.

Do not create a generic `CommandHandlers`, `CommandOperations`, `CommandRunner`, or similarly named
dump where dozens of unrelated commands live as tiny public methods. If a method only exists because
one command branch calls it, keep it next to that command branch as a lambda or a nearby private
helper. A shared runtime/helper class is acceptable only for genuinely shared state queries or
domain operations used by multiple features, not for collecting random command output methods.

## What Counts As An Adapter Leaf

A leaf is an atomic platform operation or observation. It should not know which command called it and
should not contain command flow, validation, storage decisions, or user-facing command text.

If a whole method can be implemented with core state and core abstractions, the whole method belongs
in core. Do not leave the method in Paper/NeoForge just because it eventually needs one platform
fact. Move the method to core, then add the smallest needed platform leaf for that fact. For example,
cooldown formatting, quest preview decisions, conversation option progression, NPC attachment
lookup, and objective marker decisions are core behavior; adapters may only supply facts like player
yaw, a clicked armor-stand id, or the rendered GUI/open-chat operation.

Good leaves:

- `onlineQuestPlayer(playerName)`
- `worldNames()`
- `parseItemSelection(input)`
- `location(world, x, y, z)`
- `QuestPlayer.lookingAtBlock(maxDistance)`
- `QuestPlayer.showTemporaryBeam(name, location, duration)`
- `dispatchConsoleCommand(command)`
- `broadcast(miniMessage)`

Persistence is not a normal adapter leaf. Core owns save/reload command behavior and the shared
persistent state shape. Until every file store is fully core-owned, a platform module may provide the
low-level file/database IO implementation that core calls into, but command handlers must call core
persistence methods instead of an adapter `saveData()` / `reloadData()` method.

Bad leaves:

- `setObjectiveLocationHere(commandContext, questName, objectiveId)`
- `addBreakBlocksObjective(...)`
- `handleGiveQuestCommand(...)`
- `attachQuestToNpc(...)` if it validates command arguments and returns final command text
- `setQuestGuiItem(...)` if it mutates quest config directly in Paper
- Any method whose name is a whole command branch or whose implementation builds command output

If the method would make different Paper and NeoForge command behavior likely, it is too large for
an adapter leaf.

## Item, GUI, Quest, Conversation, And Registry State

Shared state belongs in core, even when one platform has a native type for it.

- Store GUI item choices in core using the platform-neutral `ItemSelection`
  or its canonical material/custom-item string.
- Do not store Bukkit `ItemStack`, Minecraft `ItemStack`, Paper `Location`, or NeoForge objects in
  core state.
- Adapters may convert core item/location/player abstractions into platform objects only when
  rendering, executing an event, or talking to the platform API.
- Conversations, speakers, categories, quest options, and command metadata belong in core.

## Integration-Specific Features

Paper-only plugin integrations such as Vault, Citizens, FancyNPCs, Towny, Jobs, or PlaceholderAPI
may live in `src/paper`, but the boundary still applies:

- Generic NotQuests command shapes stay in core. For example, an objective added by a Paper-only
  integration should still use the core objective registry/API so the command graph, argument
  metadata, docs JSON, and handler behavior are generated through the same path as every objective.
- Generic NPC commands and command handlers stay in core. Citizens and FancyNPCs are Paper NPC
  adapters. Armor stands are a shared Minecraft concept and must have Paper and NeoForge adapter
  implementations.
- Commands that exist solely because of a Paper-only integration may live in `src/paper` when there
  is no meaningful portable command shape. Do not register those branches on NeoForge.
- Gate integration-specific command branches by platform capability.
- The adapter leaf should perform the smallest integration operation possible.
- Do not fake parity on NeoForge with “not supported on this platform” runtime messages for shared
  commands. If the feature is truly integration-specific, do not register that branch there.

## Tests And Guardrails

- Architecture tests should fail if platform production code imports or constructs core command
  handlers/effects directly.
- Core/builtin must not import Bukkit, Paper, NeoForge, or Minecraft server/client classes.
- When moving Paper behavior into core, prefer copying the old working Paper logic into core first,
  then replace platform types line by line with adapter leaves. This prevents accidentally losing
  Paper behavior while adding NeoForge parity.
