# Adapter Boundary Violation Checklist

This is the actionable subset of [the complete Paper and NeoForge adapter leaf audit](adapter-leaf-audit.md). It contains every row rated `V1`, `GAP`, or `V2`; rows rated `PASS`, `KEEP`, or `N/A` are intentionally excluded.

Audit revision: `4b67bcd934a744ab880e26a459860ab2e992e1d2`.

## Status: complete

All 389 findings from the historical audit have been addressed. The paths and line numbers below
remain as traceability to the before-state; deleted and moved declarations are intentionally left in
place so the scope of the cleanup remains reviewable.

| Completion evidence | Current state |
|---|---|
| Adapter source review | The remediated adapters contain 76 Paper and 22 NeoForge production files; the rows below preserve the larger before-state audit |
| Platform ownership | No platform `manager` package/classes and no shared behavior owner in either adapter |
| Capability parity | Shared platform interfaces contain no production default implementations |
| Dependency boundary | Core/builtin contain no Bukkit, Paper, NeoForge, or Minecraft imports |
| Compatibility isolation | All 6.3 decoding/conversion lives in `core/migrations/v6_3_0` |
| Journal parity | Both adapters consume one canonical core item; normal and creative inventory paths are protected |
| Verification | `./gradlew build` and `git diff --check` |

The final architecture and implementation checklist is [plan.md](plan.md).

## How to use this checklist

- Work in priority order: `V1`, then `GAP`, then `V2`.
- For `V1`, move the complete decision, state, or orchestration into an existing core concept owner. Keep only atomic native observations/effects in the adapter.
- For `GAP`, implement the capability correctly on that platform or remove/capability-gate the feature before registration. Do not hide missing parity behind a stub or unsafe fallback.
- For `V2`, fold portable parsing, formatting, validation, metadata, or helper policy into an existing core owner. Do not create another tiny pattern-named helper solely to move the code.
- Check an item only after the adapter method is an actual leaf (or has been deleted), Paper and NeoForge parity has been considered, and relevant tests pass.

## Totals

| Priority | Paper | NeoForge | Total |
|---|---:|---:|---:|
| `V1` | 79 | 26 | 105 |
| `GAP` | 3 | 1 | 4 |
| `V2` | 209 | 71 | 280 |
| **Total** | **291** | **98** | **389** |

## V1 — Major boundary violations

### Paper (79)

#### [`src/paper/src/main/java/com/notquests/Main.java`](src/paper/src/main/java/com/notquests/Main.java)

- [x] **V1** [line 35](src/paper/src/main/java/com/notquests/Main.java#L35) — `onEnable()` in `Main`
  - Why it violates the boundary: Broad startup flow owns platform validation, logging, construction, and lifecycle sequencing.
  - Guidance: This is a major boundary violation: broad startup flow owns platform validation, logging, construction, and lifecycle sequencing. The method decides NotQuests behavior or coordinates several domain steps, so Paper and NeoForge can diverge. Move that decision, state, and sequencing into the core plugin lifecycle owner. Leave behind the smallest native fact or effect needed by core. Do this before cosmetic cleanup because it affects behavior parity and testability.

#### [`src/paper/src/main/java/com/notquests/paper/NotQuests.java`](src/paper/src/main/java/com/notquests/paper/NotQuests.java)

- [x] **V1** [line 116](src/paper/src/main/java/com/notquests/paper/NotQuests.java#L116) — `loadPlatform()` in `NotQuests`
  - Why it violates the boundary: Broad lifecycle or service orchestration; core should own the sequencing and policy.
  - Guidance: This is a major boundary violation: broad lifecycle or service orchestration; core should own the sequencing and policy. The method decides NotQuests behavior or coordinates several domain steps, so Paper and NeoForge can diverge. Move that decision, state, and sequencing into the core plugin lifecycle owner. Leave behind the smallest native fact or effect needed by core. Do this before cosmetic cleanup because it affects behavior parity and testability.

- [x] **V1** [line 159](src/paper/src/main/java/com/notquests/paper/NotQuests.java#L159) — `preparePlatform()` in `NotQuests`
  - Why it violates the boundary: Broad lifecycle or service orchestration; core should own the sequencing and policy.
  - Guidance: This is a major boundary violation: broad lifecycle or service orchestration; core should own the sequencing and policy. The method decides NotQuests behavior or coordinates several domain steps, so Paper and NeoForge can diverge. Move that decision, state, and sequencing into the core plugin lifecycle owner. Leave behind the smallest native fact or effect needed by core. Do this before cosmetic cleanup because it affects behavior parity and testability.

- [x] **V1** [line 180](src/paper/src/main/java/com/notquests/paper/NotQuests.java#L180) — `registerPlatformContent()` in `NotQuests`
  - Why it violates the boundary: Broad lifecycle or service orchestration; core should own the sequencing and policy.
  - Guidance: This is a major boundary violation: broad lifecycle or service orchestration; core should own the sequencing and policy. The method decides NotQuests behavior or coordinates several domain steps, so Paper and NeoForge can diverge. Move that decision, state, and sequencing into the core plugin lifecycle owner. Leave behind the smallest native fact or effect needed by core. Do this before cosmetic cleanup because it affects behavior parity and testability.

- [x] **V1** [line 196](src/paper/src/main/java/com/notquests/paper/NotQuests.java#L196) — `registerPlatformCommands()` in `NotQuests`
  - Why it violates the boundary: Broad lifecycle or service orchestration; core should own the sequencing and policy.
  - Guidance: This is a major boundary violation: broad lifecycle or service orchestration; core should own the sequencing and policy. The method decides NotQuests behavior or coordinates several domain steps, so Paper and NeoForge can diverge. Move that decision, state, and sequencing into the core plugin lifecycle owner. Leave behind the smallest native fact or effect needed by core. Do this before cosmetic cleanup because it affects behavior parity and testability.

- [x] **V1** [line 257](src/paper/src/main/java/com/notquests/paper/NotQuests.java#L257) — `finishPlatformStart()` in `NotQuests`
  - Why it violates the boundary: Broad lifecycle or service orchestration; core should own the sequencing and policy.
  - Guidance: This is a major boundary violation: broad lifecycle or service orchestration; core should own the sequencing and policy. The method decides NotQuests behavior or coordinates several domain steps, so Paper and NeoForge can diverge. Move that decision, state, and sequencing into the core plugin lifecycle owner. Leave behind the smallest native fact or effect needed by core. Do this before cosmetic cleanup because it affects behavior parity and testability.

- [x] **V1** [line 286](src/paper/src/main/java/com/notquests/paper/NotQuests.java#L286) — `setupBStats()` in `NotQuests`
  - Why it violates the boundary: Broad lifecycle or service orchestration; core should own the sequencing and policy.
  - Guidance: This is a major boundary violation: broad lifecycle or service orchestration; core should own the sequencing and policy. The method decides NotQuests behavior or coordinates several domain steps, so Paper and NeoForge can diverge. Move that decision, state, and sequencing into the core plugin lifecycle owner. Leave behind the smallest native fact or effect needed by core. Do this before cosmetic cleanup because it affects behavior parity and testability.

- [x] **V1** [line 349](src/paper/src/main/java/com/notquests/paper/NotQuests.java#L349) — `stopPlatform()` in `NotQuests`
  - Why it violates the boundary: Broad lifecycle or service orchestration; core should own the sequencing and policy.
  - Guidance: This is a major boundary violation: broad lifecycle or service orchestration; core should own the sequencing and policy. The method decides NotQuests behavior or coordinates several domain steps, so Paper and NeoForge can diverge. Move that decision, state, and sequencing into the core plugin lifecycle owner. Leave behind the smallest native fact or effect needed by core. Do this before cosmetic cleanup because it affects behavior parity and testability.

#### [`src/paper/src/main/java/com/notquests/paper/adapter/PaperNotQuestsAdapter.java`](src/paper/src/main/java/com/notquests/paper/adapter/PaperNotQuestsAdapter.java)

- [x] **V1** [line 145](src/paper/src/main/java/com/notquests/paper/adapter/PaperNotQuestsAdapter.java#L145) — `getOrCreatePaperPlayer(UUID playerId)` in `PaperNotQuestsAdapter`
  - Why it violates the boundary: Owns quest-player creation/loading/registration state flow rather than one platform fact.
  - Guidance: This is a major boundary violation: owns quest-player creation/loading/registration state flow rather than one platform fact. The method decides NotQuests behavior or coordinates several domain steps, so Paper and NeoForge can diverge. Move that decision, state, and sequencing into core QuestPlayer, ActiveObjectives, or the concrete quest/objective feature owner. Leave behind the smallest native fact or effect needed by core. Do this before cosmetic cleanup because it affects behavior parity and testability.

- [x] **V1** [line 158](src/paper/src/main/java/com/notquests/paper/adapter/PaperNotQuestsAdapter.java#L158) — `registerPaperPlayer(UUID playerId, String profile, boolean active)` in `PaperNotQuestsAdapter`
  - Why it violates the boundary: Owns quest-player creation/loading/registration state flow rather than one platform fact.
  - Guidance: This is a major boundary violation: owns quest-player creation/loading/registration state flow rather than one platform fact. The method decides NotQuests behavior or coordinates several domain steps, so Paper and NeoForge can diverge. Move that decision, state, and sequencing into core QuestPlayer, ActiveObjectives, or the concrete quest/objective feature owner. Leave behind the smallest native fact or effect needed by core. Do this before cosmetic cleanup because it affects behavior parity and testability.

- [x] **V1** [line 170](src/paper/src/main/java/com/notquests/paper/adapter/PaperNotQuestsAdapter.java#L170) — `loadedPaperPlayer(PlayerRuntimeInfo loaded)` in `PaperNotQuestsAdapter`
  - Why it violates the boundary: Owns quest-player creation/loading/registration state flow rather than one platform fact.
  - Guidance: This is a major boundary violation: owns quest-player creation/loading/registration state flow rather than one platform fact. The method decides NotQuests behavior or coordinates several domain steps, so Paper and NeoForge can diverge. Move that decision, state, and sequencing into core QuestPlayer, ActiveObjectives, or the concrete quest/objective feature owner. Leave behind the smallest native fact or effect needed by core. Do this before cosmetic cleanup because it affects behavior parity and testability.

- [x] **V1** [line 180](src/paper/src/main/java/com/notquests/paper/adapter/PaperNotQuestsAdapter.java#L180) — `getOrLoadPaperPlayer(UUID playerId)` in `PaperNotQuestsAdapter`
  - Why it violates the boundary: Owns quest-player creation/loading/registration state flow rather than one platform fact.
  - Guidance: This is a major boundary violation: owns quest-player creation/loading/registration state flow rather than one platform fact. The method decides NotQuests behavior or coordinates several domain steps, so Paper and NeoForge can diverge. Move that decision, state, and sequencing into core QuestPlayer, ActiveObjectives, or the concrete quest/objective feature owner. Leave behind the smallest native fact or effect needed by core. Do this before cosmetic cleanup because it affects behavior parity and testability.

- [x] **V1** [line 255](src/paper/src/main/java/com/notquests/paper/adapter/PaperNotQuestsAdapter.java#L255) — `questPlayer(String playerIdentifier, String profile)` in `PaperNotQuestsAdapter`
  - Why it violates the boundary: Owns quest-player creation/loading/registration state flow rather than one platform fact.
  - Guidance: This is a major boundary violation: owns quest-player creation/loading/registration state flow rather than one platform fact. The method decides NotQuests behavior or coordinates several domain steps, so Paper and NeoForge can diverge. Move that decision, state, and sequencing into core QuestPlayer, ActiveObjectives, or the concrete quest/objective feature owner. Leave behind the smallest native fact or effect needed by core. Do this before cosmetic cleanup because it affects behavior parity and testability.

- [x] **V1** [line 721](src/paper/src/main/java/com/notquests/paper/adapter/PaperNotQuestsAdapter.java#L721) — `npcSelection(String npcSelector)` in `PaperNotQuestsAdapter`
  - Why it violates the boundary: Owns NPC lookup/validation or detachment orchestration that core should decide.
  - Guidance: This is a major boundary violation: owns NPC lookup/validation or detachment orchestration that core should decide. The method decides NotQuests behavior or coordinates several domain steps, so Paper and NeoForge can diverge. Move that decision, state, and sequencing into core NpcAttachments or the core conversation/quest owner that requested the NPC operation. Leave behind the smallest native fact or effect needed by core. Do this before cosmetic cleanup because it affects behavior parity and testability.

- [x] **V1** [line 748](src/paper/src/main/java/com/notquests/paper/adapter/PaperNotQuestsAdapter.java#L748) — `applyQuestNpcDetachments(Detachments detachments)` in `PaperNotQuestsAdapter`
  - Why it violates the boundary: Owns NPC lookup/validation or detachment orchestration that core should decide.
  - Guidance: This is a major boundary violation: owns NPC lookup/validation or detachment orchestration that core should decide. The method decides NotQuests behavior or coordinates several domain steps, so Paper and NeoForge can diverge. Move that decision, state, and sequencing into core NpcAttachments or the core conversation/quest owner that requested the NPC operation. Leave behind the smallest native fact or effect needed by core. Do this before cosmetic cleanup because it affects behavior parity and testability.

- [x] **V1** [line 1042](src/paper/src/main/java/com/notquests/paper/adapter/PaperNotQuestsAdapter.java#L1042) — `paperNpc(String selector)` in `PaperNotQuestsAdapter`
  - Why it violates the boundary: Owns NPC lookup/validation or detachment orchestration that core should decide.
  - Guidance: This is a major boundary violation: owns NPC lookup/validation or detachment orchestration that core should decide. The method decides NotQuests behavior or coordinates several domain steps, so Paper and NeoForge can diverge. Move that decision, state, and sequencing into core NpcAttachments or the core conversation/quest owner that requested the NPC operation. Leave behind the smallest native fact or effect needed by core. Do this before cosmetic cleanup because it affects behavior parity and testability.

- [x] **V1** [line 1060](src/paper/src/main/java/com/notquests/paper/adapter/PaperNotQuestsAdapter.java#L1060) — `canonicalNpcSelector(NQNPC npc)` in `PaperNotQuestsAdapter`
  - Why it violates the boundary: Owns NPC lookup/validation or detachment orchestration that core should decide.
  - Guidance: This is a major boundary violation: owns NPC lookup/validation or detachment orchestration that core should decide. The method decides NotQuests behavior or coordinates several domain steps, so Paper and NeoForge can diverge. Move that decision, state, and sequencing into core NpcAttachments or the core conversation/quest owner that requested the NPC operation. Leave behind the smallest native fact or effect needed by core. Do this before cosmetic cleanup because it affects behavior parity and testability.

- [x] **V1** [line 1280](src/paper/src/main/java/com/notquests/paper/adapter/PaperNotQuestsAdapter.java#L1280) — `randomLocation(Location baseLocation, int radiusX, int radiusY, int radiusZ)` in `PaperNotQuestsAdapter`
  - Why it violates the boundary: Portable random-placement gameplay policy rather than a platform effect.
  - Guidance: This is a major boundary violation: portable random-placement gameplay policy rather than a platform effect. The method decides NotQuests behavior or coordinates several domain steps, so Paper and NeoForge can diverge. Move that decision, state, and sequencing into the matching core concept owner. Leave behind the smallest native fact or effect needed by core. Do this before cosmetic cleanup because it affects behavior parity and testability.

- [x] **V1** [line 1288](src/paper/src/main/java/com/notquests/paper/adapter/PaperNotQuestsAdapter.java#L1288) — `randomOffset(int radius)` in `PaperNotQuestsAdapter`
  - Why it violates the boundary: Portable random-placement gameplay policy rather than a platform effect.
  - Guidance: This is a major boundary violation: portable random-placement gameplay policy rather than a platform effect. The method decides NotQuests behavior or coordinates several domain steps, so Paper and NeoForge can diverge. Move that decision, state, and sequencing into the matching core concept owner. Leave behind the smallest native fact or effect needed by core. Do this before cosmetic cleanup because it affects behavior parity and testability.

#### [`src/paper/src/main/java/com/notquests/paper/commands/arguments/wrappers/ItemStackSelection.java`](src/paper/src/main/java/com/notquests/paper/commands/arguments/wrappers/ItemStackSelection.java)

- [x] **V1** [line 22](src/paper/src/main/java/com/notquests/paper/commands/arguments/wrappers/ItemStackSelection.java#L22) — `ItemStackSelection(NotQuests main)` in `ItemStackSelection`
  - Why it violates the boundary: Owns shared mutable item-selection state, validation, presentation, or portable state logic in Paper.
  - Guidance: This is a major boundary violation: owns shared mutable item-selection state, validation, presentation, or portable state logic in Paper. The method decides NotQuests behavior or coordinates several domain steps, so Paper and NeoForge can diverge. Move that decision, state, and sequencing into the relevant core command definition or the core command framework. Leave behind the smallest native fact or effect needed by core. Do this before cosmetic cleanup because it affects behavior parity and testability.

- [x] **V1** [line 30](src/paper/src/main/java/com/notquests/paper/commands/arguments/wrappers/ItemStackSelection.java#L30) — `isAny()` in `ItemStackSelection`
  - Why it violates the boundary: Owns shared mutable item-selection state, validation, presentation, or portable state logic in Paper.
  - Guidance: This is a major boundary violation: owns shared mutable item-selection state, validation, presentation, or portable state logic in Paper. The method decides NotQuests behavior or coordinates several domain steps, so Paper and NeoForge can diverge. Move that decision, state, and sequencing into the relevant core command definition or the core command framework. Leave behind the smallest native fact or effect needed by core. Do this before cosmetic cleanup because it affects behavior parity and testability.

- [x] **V1** [line 34](src/paper/src/main/java/com/notquests/paper/commands/arguments/wrappers/ItemStackSelection.java#L34) — `setAny(boolean any)` in `ItemStackSelection`
  - Why it violates the boundary: Owns shared mutable item-selection state, validation, presentation, or portable state logic in Paper.
  - Guidance: This is a major boundary violation: owns shared mutable item-selection state, validation, presentation, or portable state logic in Paper. The method decides NotQuests behavior or coordinates several domain steps, so Paper and NeoForge can diverge. Move that decision, state, and sequencing into the relevant core command definition or the core command framework. Leave behind the smallest native fact or effect needed by core. Do this before cosmetic cleanup because it affects behavior parity and testability.

- [x] **V1** [line 38](src/paper/src/main/java/com/notquests/paper/commands/arguments/wrappers/ItemStackSelection.java#L38) — `addNqItemName(String nqItemName)` in `ItemStackSelection`
  - Why it violates the boundary: Owns shared mutable item-selection state, validation, presentation, or portable state logic in Paper.
  - Guidance: This is a major boundary violation: owns shared mutable item-selection state, validation, presentation, or portable state logic in Paper. The method decides NotQuests behavior or coordinates several domain steps, so Paper and NeoForge can diverge. Move that decision, state, and sequencing into the relevant core command definition or the core command framework. Leave behind the smallest native fact or effect needed by core. Do this before cosmetic cleanup because it affects behavior parity and testability.

- [x] **V1** [line 51](src/paper/src/main/java/com/notquests/paper/commands/arguments/wrappers/ItemStackSelection.java#L51) — `addItemStack(ItemStack itemStack)` in `ItemStackSelection`
  - Why it violates the boundary: Owns shared mutable item-selection state, validation, presentation, or portable state logic in Paper.
  - Guidance: This is a major boundary violation: owns shared mutable item-selection state, validation, presentation, or portable state logic in Paper. The method decides NotQuests behavior or coordinates several domain steps, so Paper and NeoForge can diverge. Move that decision, state, and sequencing into the relevant core command definition or the core command framework. Leave behind the smallest native fact or effect needed by core. Do this before cosmetic cleanup because it affects behavior parity and testability.

- [x] **V1** [line 57](src/paper/src/main/java/com/notquests/paper/commands/arguments/wrappers/ItemStackSelection.java#L57) — `addMaterial(Material material)` in `ItemStackSelection`
  - Why it violates the boundary: Owns shared mutable item-selection state, validation, presentation, or portable state logic in Paper.
  - Guidance: This is a major boundary violation: owns shared mutable item-selection state, validation, presentation, or portable state logic in Paper. The method decides NotQuests behavior or coordinates several domain steps, so Paper and NeoForge can diverge. Move that decision, state, and sequencing into the relevant core command definition or the core command framework. Leave behind the smallest native fact or effect needed by core. Do this before cosmetic cleanup because it affects behavior parity and testability.

- [x] **V1** [line 63](src/paper/src/main/java/com/notquests/paper/commands/arguments/wrappers/ItemStackSelection.java#L63) — `addMaterialName(String materialName)` in `ItemStackSelection`
  - Why it violates the boundary: Owns shared mutable item-selection state, validation, presentation, or portable state logic in Paper.
  - Guidance: This is a major boundary violation: owns shared mutable item-selection state, validation, presentation, or portable state logic in Paper. The method decides NotQuests behavior or coordinates several domain steps, so Paper and NeoForge can diverge. Move that decision, state, and sequencing into the relevant core command definition or the core command framework. Leave behind the smallest native fact or effect needed by core. Do this before cosmetic cleanup because it affects behavior parity and testability.

- [x] **V1** [line 71](src/paper/src/main/java/com/notquests/paper/commands/arguments/wrappers/ItemStackSelection.java#L71) — `getAllMaterialsListedTranslated(String tag)` in `ItemStackSelection`
  - Why it violates the boundary: Owns shared mutable item-selection state, validation, presentation, or portable state logic in Paper.
  - Guidance: This is a major boundary violation: owns shared mutable item-selection state, validation, presentation, or portable state logic in Paper. The method decides NotQuests behavior or coordinates several domain steps, so Paper and NeoForge can diverge. Move that decision, state, and sequencing into the relevant core command definition or the core command framework. Leave behind the smallest native fact or effect needed by core. Do this before cosmetic cleanup because it affects behavior parity and testability.

- [x] **V1** [line 90](src/paper/src/main/java/com/notquests/paper/commands/arguments/wrappers/ItemStackSelection.java#L90) — `getAllMaterialsListed()` in `ItemStackSelection`
  - Why it violates the boundary: Owns shared mutable item-selection state, validation, presentation, or portable state logic in Paper.
  - Guidance: This is a major boundary violation: owns shared mutable item-selection state, validation, presentation, or portable state logic in Paper. The method decides NotQuests behavior or coordinates several domain steps, so Paper and NeoForge can diverge. Move that decision, state, and sequencing into the relevant core command definition or the core command framework. Leave behind the smallest native fact or effect needed by core. Do this before cosmetic cleanup because it affects behavior parity and testability.

- [x] **V1** [line 137](src/paper/src/main/java/com/notquests/paper/commands/arguments/wrappers/ItemStackSelection.java#L137) — `toString()` in `ItemStackSelection`
  - Why it violates the boundary: Owns shared mutable item-selection state, validation, presentation, or portable state logic in Paper.
  - Guidance: This is a major boundary violation: owns shared mutable item-selection state, validation, presentation, or portable state logic in Paper. The method decides NotQuests behavior or coordinates several domain steps, so Paper and NeoForge can diverge. Move that decision, state, and sequencing into the relevant core command definition or the core command framework. Leave behind the smallest native fact or effect needed by core. Do this before cosmetic cleanup because it affects behavior parity and testability.

- [x] **V1** [line 214](src/paper/src/main/java/com/notquests/paper/commands/arguments/wrappers/ItemStackSelection.java#L214) — `hasSavedItem()` in `ItemStackSelection`
  - Why it violates the boundary: Owns shared mutable item-selection state, validation, presentation, or portable state logic in Paper.
  - Guidance: This is a major boundary violation: owns shared mutable item-selection state, validation, presentation, or portable state logic in Paper. The method decides NotQuests behavior or coordinates several domain steps, so Paper and NeoForge can diverge. Move that decision, state, and sequencing into the relevant core command definition or the core command framework. Leave behind the smallest native fact or effect needed by core. Do this before cosmetic cleanup because it affects behavior parity and testability.

- [x] **V1** [line 218](src/paper/src/main/java/com/notquests/paper/commands/arguments/wrappers/ItemStackSelection.java#L218) — `getNqItemNames()` in `ItemStackSelection`
  - Why it violates the boundary: Owns shared mutable item-selection state, validation, presentation, or portable state logic in Paper.
  - Guidance: This is a major boundary violation: owns shared mutable item-selection state, validation, presentation, or portable state logic in Paper. The method decides NotQuests behavior or coordinates several domain steps, so Paper and NeoForge can diverge. Move that decision, state, and sequencing into the relevant core command definition or the core command framework. Leave behind the smallest native fact or effect needed by core. Do this before cosmetic cleanup because it affects behavior parity and testability.

- [x] **V1** [line 271](src/paper/src/main/java/com/notquests/paper/commands/arguments/wrappers/ItemStackSelection.java#L271) — `isEmptyOrAny()` in `ItemStackSelection`
  - Why it violates the boundary: Owns shared mutable item-selection state, validation, presentation, or portable state logic in Paper.
  - Guidance: This is a major boundary violation: owns shared mutable item-selection state, validation, presentation, or portable state logic in Paper. The method decides NotQuests behavior or coordinates several domain steps, so Paper and NeoForge can diverge. Move that decision, state, and sequencing into the relevant core command definition or the core command framework. Leave behind the smallest native fact or effect needed by core. Do this before cosmetic cleanup because it affects behavior parity and testability.

#### [`src/paper/src/main/java/com/notquests/paper/commands/brigadier/PaperCoreCommandCompiler.java`](src/paper/src/main/java/com/notquests/paper/commands/brigadier/PaperCoreCommandCompiler.java)

- [x] **V1** [line 487](src/paper/src/main/java/com/notquests/paper/commands/brigadier/PaperCoreCommandCompiler.java#L487) — `execute(NQCommandTree.Node<NQArgumentType, NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>, NQSuggestionProvider<NQCommandContext>, NQCommandHandler> node, CommandContext<CommandSourceStack> context, String flagString, List<NQCommandTree.Node<NQArgumentType, NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>, NQSuggestionProvider<NQCommandContext>, NQCommandHandler>> path)` in `PaperCoreCommandCompiler`
  - Why it violates the boundary: Owns command permission/sender validation, handler flow, success aggregation, and output sequencing.
  - Guidance: This is a major boundary violation: owns command permission/sender validation, handler flow, success aggregation, and output sequencing. The method decides NotQuests behavior or coordinates several domain steps, so Paper and NeoForge can diverge. Move that decision, state, and sequencing into the relevant core command definition or the core command framework. Leave behind the smallest native fact or effect needed by core. Do this before cosmetic cleanup because it affects behavior parity and testability.

- [x] **V1** [line 530](src/paper/src/main/java/com/notquests/paper/commands/brigadier/PaperCoreCommandCompiler.java#L530) — `printBranchHelp(NQCommandTree.Node<NQArgumentType, NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>, NQSuggestionProvider<NQCommandContext>, NQCommandHandler> node, List<NQCommandTree.Node<NQArgumentType, NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>, NQSuggestionProvider<NQCommandContext>, NQCommandHandler>> path, CommandContext<CommandSourceStack> context)` in `PaperCoreCommandCompiler`
  - Why it violates the boundary: Owns command help/output flow; platform should only render core-produced output.
  - Guidance: This is a major boundary violation: owns command help/output flow; platform should only render core-produced output. The method decides NotQuests behavior or coordinates several domain steps, so Paper and NeoForge can diverge. Move that decision, state, and sequencing into the relevant core command definition or the core command framework. Leave behind the smallest native fact or effect needed by core. Do this before cosmetic cleanup because it affects behavior parity and testability.

- [x] **V1** [line 550](src/paper/src/main/java/com/notquests/paper/commands/brigadier/PaperCoreCommandCompiler.java#L550) — `sendUsageLines(CommandSender sender, NQCommandTree.Node<NQArgumentType, NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>, NQSuggestionProvider<NQCommandContext>, NQCommandHandler> node, List<NQCommandTree.Node<NQArgumentType, NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>, NQSuggestionProvider<NQCommandContext>, NQCommandHandler>> path)` in `PaperCoreCommandCompiler`
  - Why it violates the boundary: Owns command help/output flow; platform should only render core-produced output.
  - Guidance: This is a major boundary violation: owns command help/output flow; platform should only render core-produced output. The method decides NotQuests behavior or coordinates several domain steps, so Paper and NeoForge can diverge. Move that decision, state, and sequencing into the relevant core command definition or the core command framework. Leave behind the smallest native fact or effect needed by core. Do this before cosmetic cleanup because it affects behavior parity and testability.

#### [`src/paper/src/main/java/com/notquests/paper/conversation/PaperConversationDisplay.java`](src/paper/src/main/java/com/notquests/paper/conversation/PaperConversationDisplay.java)

- [x] **V1** [line 23](src/paper/src/main/java/com/notquests/paper/conversation/PaperConversationDisplay.java#L23) — `line(PlatformPlayer questPlayer, ConversationManager.DisplayLine line)` in `PaperConversationDisplay`
  - Why it violates the boundary: Mixes chat rendering with conversation-history/configuration policy.
  - Guidance: This is a major boundary violation: mixes chat rendering with conversation-history/configuration policy. The method decides NotQuests behavior or coordinates several domain steps, so Paper and NeoForge can diverge. Move that decision, state, and sequencing into core Conversations/chat-history ownership. Leave behind the smallest native fact or effect needed by core. Do this before cosmetic cleanup because it affects behavior parity and testability.

- [x] **V1** [line 44](src/paper/src/main/java/com/notquests/paper/conversation/PaperConversationDisplay.java#L44) — `chooseAnswerPrefix(PlatformPlayer questPlayer)` in `PaperConversationDisplay`
  - Why it violates the boundary: Mixes chat rendering with conversation-history/configuration policy.
  - Guidance: This is a major boundary violation: mixes chat rendering with conversation-history/configuration policy. The method decides NotQuests behavior or coordinates several domain steps, so Paper and NeoForge can diverge. Move that decision, state, and sequencing into core Conversations/chat-history ownership. Leave behind the smallest native fact or effect needed by core. Do this before cosmetic cleanup because it affects behavior parity and testability.

- [x] **V1** [line 57](src/paper/src/main/java/com/notquests/paper/conversation/PaperConversationDisplay.java#L57) — `option(PlatformPlayer questPlayer, ConversationManager.DisplayOption option)` in `PaperConversationDisplay`
  - Why it violates the boundary: Mixes chat rendering with conversation-history/configuration policy.
  - Guidance: This is a major boundary violation: mixes chat rendering with conversation-history/configuration policy. The method decides NotQuests behavior or coordinates several domain steps, so Paper and NeoForge can diverge. Move that decision, state, and sequencing into core Conversations/chat-history ownership. Leave behind the smallest native fact or effect needed by core. Do this before cosmetic cleanup because it affects behavior parity and testability.

- [x] **V1** [line 78](src/paper/src/main/java/com/notquests/paper/conversation/PaperConversationDisplay.java#L78) — `optionsFinished(PlatformPlayer questPlayer)` in `PaperConversationDisplay`
  - Why it violates the boundary: Mixes chat rendering with conversation-history/configuration policy.
  - Guidance: This is a major boundary violation: mixes chat rendering with conversation-history/configuration policy. The method decides NotQuests behavior or coordinates several domain steps, so Paper and NeoForge can diverge. Move that decision, state, and sequencing into core Conversations/chat-history ownership. Leave behind the smallest native fact or effect needed by core. Do this before cosmetic cleanup because it affects behavior parity and testability.

#### [`src/paper/src/main/java/com/notquests/paper/events/ArmorStandEvents.java`](src/paper/src/main/java/com/notquests/paper/events/ArmorStandEvents.java)

- [x] **V1** [line 132](src/paper/src/main/java/com/notquests/paper/events/ArmorStandEvents.java#L132) — `showQuestOrHandleObjectivesOfArmorStands(Player player, ArmorStand armorStand, NQNPC armorStandNQNPC, PlayerInteractAtEntityEvent event)` in `ArmorStandEvents`
  - Why it violates the boundary: Combines objective/NPC handling with cancellation policy.
  - Guidance: This is a major boundary violation: combines objective/NPC handling with cancellation policy. The method decides NotQuests behavior or coordinates several domain steps, so Paper and NeoForge can diverge. Move that decision, state, and sequencing into core NpcAttachments or the core conversation/quest owner that requested the NPC operation. Leave behind the smallest native fact or effect needed by core. Do this before cosmetic cleanup because it affects behavior parity and testability.

- [x] **V1** [line 147](src/paper/src/main/java/com/notquests/paper/events/ArmorStandEvents.java#L147) — `onArmorStandLoad(EntitiesLoadEvent event)` in `ArmorStandEvents`
  - Why it violates the boundary: Owns attachment filtering and indicator-cache lifecycle policy.
  - Guidance: This is a major boundary violation: owns attachment filtering and indicator-cache lifecycle policy. The method decides NotQuests behavior or coordinates several domain steps, so Paper and NeoForge can diverge. Move that decision, state, and sequencing into the matching core concept owner. Leave behind the smallest native fact or effect needed by core. Do this before cosmetic cleanup because it affects behavior parity and testability.

- [x] **V1** [line 165](src/paper/src/main/java/com/notquests/paper/events/ArmorStandEvents.java#L165) — `onArmorStandUnload(EntitiesUnloadEvent event)` in `ArmorStandEvents`
  - Why it violates the boundary: Owns attachment filtering and indicator-cache lifecycle policy.
  - Guidance: This is a major boundary violation: owns attachment filtering and indicator-cache lifecycle policy. The method decides NotQuests behavior or coordinates several domain steps, so Paper and NeoForge can diverge. Move that decision, state, and sequencing into the matching core concept owner. Leave behind the smallest native fact or effect needed by core. Do this before cosmetic cleanup because it affects behavior parity and testability.

- [x] **V1** [line 183](src/paper/src/main/java/com/notquests/paper/events/ArmorStandEvents.java#L183) — `onArmorStandSpawn(EntitySpawnEvent event)` in `ArmorStandEvents`
  - Why it violates the boundary: Owns attachment filtering and indicator-cache lifecycle policy.
  - Guidance: This is a major boundary violation: owns attachment filtering and indicator-cache lifecycle policy. The method decides NotQuests behavior or coordinates several domain steps, so Paper and NeoForge can diverge. Move that decision, state, and sequencing into the matching core concept owner. Leave behind the smallest native fact or effect needed by core. Do this before cosmetic cleanup because it affects behavior parity and testability.

- [x] **V1** [line 199](src/paper/src/main/java/com/notquests/paper/events/ArmorStandEvents.java#L199) — `onArmorStandDeath(EntityDeathEvent event)` in `ArmorStandEvents`
  - Why it violates the boundary: Owns attachment filtering and indicator-cache lifecycle policy.
  - Guidance: This is a major boundary violation: owns attachment filtering and indicator-cache lifecycle policy. The method decides NotQuests behavior or coordinates several domain steps, so Paper and NeoForge can diverge. Move that decision, state, and sequencing into the matching core concept owner. Leave behind the smallest native fact or effect needed by core. Do this before cosmetic cleanup because it affects behavior parity and testability.

#### [`src/paper/src/main/java/com/notquests/paper/events/QuestEvents.java`](src/paper/src/main/java/com/notquests/paper/events/QuestEvents.java)

- [x] **V1** [line 67](src/paper/src/main/java/com/notquests/paper/events/QuestEvents.java#L67) — `QuestEvents(NotQuests main)` in `QuestEvents`
  - Why it violates the boundary: Schedules shared quest cadence and owns marker refresh orchestration.
  - Guidance: This is a major boundary violation: schedules shared quest cadence and owns marker refresh orchestration. The method decides NotQuests behavior or coordinates several domain steps, so Paper and NeoForge can diverge. Move that decision, state, and sequencing into core QuestPlayer, ActiveObjectives, or the concrete quest/objective feature owner. Leave behind the smallest native fact or effect needed by core. Do this before cosmetic cleanup because it affects behavior parity and testability.

- [x] **V1** [line 136](src/paper/src/main/java/com/notquests/paper/events/QuestEvents.java#L136) — `interactEvent(PlayerInteractEvent e)` in `QuestEvents`
  - Why it violates the boundary: Combines quest gating, conversation policy, block interaction, and buried-treasure behavior.
  - Guidance: This is a major boundary violation: combines quest gating, conversation policy, block interaction, and buried-treasure behavior. The method decides NotQuests behavior or coordinates several domain steps, so Paper and NeoForge can diverge. Move that decision, state, and sequencing into core Conversations/chat-history ownership. Leave behind the smallest native fact or effect needed by core. Do this before cosmetic cleanup because it affects behavior parity and testability.

- [x] **V1** [line 312](src/paper/src/main/java/com/notquests/paper/events/QuestEvents.java#L312) — `consumeFreshBrewedAmount(String brewingStandKey, ItemStack takenItem)` in `QuestEvents`
  - Why it violates the boundary: Owns matching/consumption algorithm and mutable fresh-brew state.
  - Guidance: This is a major boundary violation: owns matching/consumption algorithm and mutable fresh-brew state. The method decides NotQuests behavior or coordinates several domain steps, so Paper and NeoForge can diverge. Move that decision, state, and sequencing into the matching core concept owner. Leave behind the smallest native fact or effect needed by core. Do this before cosmetic cleanup because it affects behavior parity and testability.

- [x] **V1** [line 649](src/paper/src/main/java/com/notquests/paper/events/QuestEvents.java#L649) — `onInventoryClick(InventoryClickEvent e)` in `QuestEvents`
  - Why it violates the boundary: Coordinates three distinct objective event flows in one adapter handler.
  - Guidance: This is a major boundary violation: coordinates three distinct objective event flows in one adapter handler. The method decides NotQuests behavior or coordinates several domain steps, so Paper and NeoForge can diverge. Move that decision, state, and sequencing into core QuestPlayer, ActiveObjectives, or the concrete quest/objective feature owner. Leave behind the smallest native fact or effect needed by core. Do this before cosmetic cleanup because it affects behavior parity and testability.

- [x] **V1** [line 872](src/paper/src/main/java/com/notquests/paper/events/QuestEvents.java#L872) — `onDisconnectEvent(PlayerQuitEvent e)` in `QuestEvents`
  - Why it violates the boundary: Combines player lifecycle, detach, asynchronous persistence, and scheduling.
  - Guidance: This is a major boundary violation: combines player lifecycle, detach, asynchronous persistence, and scheduling. The method decides NotQuests behavior or coordinates several domain steps, so Paper and NeoForge can diverge. Move that decision, state, and sequencing into the matching core concept owner. Leave behind the smallest native fact or effect needed by core. Do this before cosmetic cleanup because it affects behavior parity and testability.

- [x] **V1** [line 920](src/paper/src/main/java/com/notquests/paper/events/QuestEvents.java#L920) — `asyncChatEvent(AsyncChatEvent e)` in `QuestEvents`
  - Why it violates the boundary: Owns conversation answer parsing/permission policy and chat-history fan-out.
  - Guidance: This is a major boundary violation: owns conversation answer parsing/permission policy and chat-history fan-out. The method decides NotQuests behavior or coordinates several domain steps, so Paper and NeoForge can diverge. Move that decision, state, and sequencing into core Conversations/chat-history ownership. Leave behind the smallest native fact or effect needed by core. Do this before cosmetic cleanup because it affects behavior parity and testability.

#### [`src/paper/src/main/java/com/notquests/paper/events/hooks/BetonQuestEvents.java`](src/paper/src/main/java/com/notquests/paper/events/hooks/BetonQuestEvents.java)

- [x] **V1** [line 30](src/paper/src/main/java/com/notquests/paper/events/hooks/BetonQuestEvents.java#L30) — `onConversationOption(ConversationOptionEvent event)` in `BetonQuestEvents`
  - Why it violates the boundary: Owns conversation-history deletion/replay policy.
  - Guidance: This is a major boundary violation: owns conversation-history deletion/replay policy. The method decides NotQuests behavior or coordinates several domain steps, so Paper and NeoForge can diverge. Move that decision, state, and sequencing into core Conversations/chat-history ownership. Leave behind the smallest native fact or effect needed by core. Do this before cosmetic cleanup because it affects behavior parity and testability.

#### [`src/paper/src/main/java/com/notquests/paper/events/hooks/CitizensEvents.java`](src/paper/src/main/java/com/notquests/paper/events/hooks/CitizensEvents.java)

- [x] **V1** [line 58](src/paper/src/main/java/com/notquests/paper/events/hooks/CitizensEvents.java#L58) — `onNPCClickEvent(NPCRightClickEvent event)` in `CitizensEvents`
  - Why it violates the boundary: Owns selector tools, objective handling, previews, conversations, navigation, focus, and messages.
  - Guidance: This is a major boundary violation: owns selector tools, objective handling, previews, conversations, navigation, focus, and messages. The method decides NotQuests behavior or coordinates several domain steps, so Paper and NeoForge can diverge. Move that decision, state, and sequencing into core Conversations/chat-history ownership. Leave behind the smallest native fact or effect needed by core. Do this before cosmetic cleanup because it affects behavior parity and testability.

#### [`src/paper/src/main/java/com/notquests/paper/events/hooks/EliteMobsEvents.java`](src/paper/src/main/java/com/notquests/paper/events/hooks/EliteMobsEvents.java)

- [x] **V1** [line 18](src/paper/src/main/java/com/notquests/paper/events/hooks/EliteMobsEvents.java#L18) — `onEliteMobDeath(EliteMobDeathEvent event)` in `EliteMobsEvents`
  - Why it violates the boundary: Chooses credited players and objective matching/progress policy in Paper.
  - Guidance: This is a major boundary violation: chooses credited players and objective matching/progress policy in Paper. The method decides NotQuests behavior or coordinates several domain steps, so Paper and NeoForge can diverge. Move that decision, state, and sequencing into core QuestPlayer, ActiveObjectives, or the concrete quest/objective feature owner. Leave behind the smallest native fact or effect needed by core. Do this before cosmetic cleanup because it affects behavior parity and testability.

#### [`src/paper/src/main/java/com/notquests/paper/events/hooks/FancyNPCsEvents.java`](src/paper/src/main/java/com/notquests/paper/events/hooks/FancyNPCsEvents.java)

- [x] **V1** [line 33](src/paper/src/main/java/com/notquests/paper/events/hooks/FancyNPCsEvents.java#L33) — `onNpcInteract(NpcInteractEvent event)` in `FancyNPCsEvents`
  - Why it violates the boundary: Owns selector tools, objective flow, preview, and conversation orchestration.
  - Guidance: This is a major boundary violation: owns selector tools, objective flow, preview, and conversation orchestration. The method decides NotQuests behavior or coordinates several domain steps, so Paper and NeoForge can diverge. Move that decision, state, and sequencing into core Conversations/chat-history ownership. Leave behind the smallest native fact or effect needed by core. Do this before cosmetic cleanup because it affects behavior parity and testability.

#### [`src/paper/src/main/java/com/notquests/paper/events/hooks/JobsRebornEvents.java`](src/paper/src/main/java/com/notquests/paper/events/hooks/JobsRebornEvents.java)

- [x] **V1** [line 49](src/paper/src/main/java/com/notquests/paper/events/hooks/JobsRebornEvents.java#L49) — `syncObjectives(PaperPlayer questPlayer)` in `JobsRebornEvents`
  - Why it violates the boundary: Computes and applies shared objective progress policy.
  - Guidance: This is a major boundary violation: computes and applies shared objective progress policy. The method decides NotQuests behavior or coordinates several domain steps, so Paper and NeoForge can diverge. Move that decision, state, and sequencing into core QuestPlayer, ActiveObjectives, or the concrete quest/objective feature owner. Leave behind the smallest native fact or effect needed by core. Do this before cosmetic cleanup because it affects behavior parity and testability.

- [x] **V1** [line 59](src/paper/src/main/java/com/notquests/paper/events/hooks/JobsRebornEvents.java#L59) — `onJobsLevelUp(JobsLevelUpEvent e)` in `JobsRebornEvents`
  - Why it violates the boundary: Chooses set-vs-add progress semantics and objective matching.
  - Guidance: This is a major boundary violation: chooses set-vs-add progress semantics and objective matching. The method decides NotQuests behavior or coordinates several domain steps, so Paper and NeoForge can diverge. Move that decision, state, and sequencing into core QuestPlayer, ActiveObjectives, or the concrete quest/objective feature owner. Leave behind the smallest native fact or effect needed by core. Do this before cosmetic cleanup because it affects behavior parity and testability.

#### [`src/paper/src/main/java/com/notquests/paper/events/hooks/TownyEvents.java`](src/paper/src/main/java/com/notquests/paper/events/hooks/TownyEvents.java)

- [x] **V1** [line 20](src/paper/src/main/java/com/notquests/paper/events/hooks/TownyEvents.java#L20) — `onTownAddToNation(NationAddTownEvent e)` in `TownyEvents`
  - Why it violates the boundary: Chooses which residents receive shared objective progress and by how much.
  - Guidance: This is a major boundary violation: chooses which residents receive shared objective progress and by how much. The method decides NotQuests behavior or coordinates several domain steps, so Paper and NeoForge can diverge. Move that decision, state, and sequencing into core QuestPlayer, ActiveObjectives, or the concrete quest/objective feature owner. Leave behind the smallest native fact or effect needed by core. Do this before cosmetic cleanup because it affects behavior parity and testability.

- [x] **V1** [line 27](src/paper/src/main/java/com/notquests/paper/events/hooks/TownyEvents.java#L27) — `onTownRemoveFromNation(NationAddTownEvent e)` in `TownyEvents`
  - Why it violates the boundary: Chooses which residents receive shared objective progress and by how much.
  - Guidance: This is a major boundary violation: chooses which residents receive shared objective progress and by how much. The method decides NotQuests behavior or coordinates several domain steps, so Paper and NeoForge can diverge. Move that decision, state, and sequencing into core QuestPlayer, ActiveObjectives, or the concrete quest/objective feature owner. Leave behind the smallest native fact or effect needed by core. Do this before cosmetic cleanup because it affects behavior parity and testability.

- [x] **V1** [line 34](src/paper/src/main/java/com/notquests/paper/events/hooks/TownyEvents.java#L34) — `onResidentAdd(TownAddResidentEvent e)` in `TownyEvents`
  - Why it violates the boundary: Chooses which residents receive shared objective progress and by how much.
  - Guidance: This is a major boundary violation: chooses which residents receive shared objective progress and by how much. The method decides NotQuests behavior or coordinates several domain steps, so Paper and NeoForge can diverge. Move that decision, state, and sequencing into core QuestPlayer, ActiveObjectives, or the concrete quest/objective feature owner. Leave behind the smallest native fact or effect needed by core. Do this before cosmetic cleanup because it affects behavior parity and testability.

- [x] **V1** [line 41](src/paper/src/main/java/com/notquests/paper/events/hooks/TownyEvents.java#L41) — `onResidentRemove(TownRemoveResidentEvent e)` in `TownyEvents`
  - Why it violates the boundary: Chooses which residents receive shared objective progress and by how much.
  - Guidance: This is a major boundary violation: chooses which residents receive shared objective progress and by how much. The method decides NotQuests behavior or coordinates several domain steps, so Paper and NeoForge can diverge. Move that decision, state, and sequencing into core QuestPlayer, ActiveObjectives, or the concrete quest/objective feature owner. Leave behind the smallest native fact or effect needed by core. Do this before cosmetic cleanup because it affects behavior parity and testability.

- [x] **V1** [line 48](src/paper/src/main/java/com/notquests/paper/events/hooks/TownyEvents.java#L48) — `updateProgress(Resident resident, String objectiveTypeId, int amount)` in `TownyEvents`
  - Why it violates the boundary: Owns add/remove progress policy and validation for Towny objectives.
  - Guidance: This is a major boundary violation: owns add/remove progress policy and validation for Towny objectives. The method decides NotQuests behavior or coordinates several domain steps, so Paper and NeoForge can diverge. Move that decision, state, and sequencing into core QuestPlayer, ActiveObjectives, or the concrete quest/objective feature owner. Leave behind the smallest native fact or effect needed by core. Do this before cosmetic cleanup because it affects behavior parity and testability.

#### [`src/paper/src/main/java/com/notquests/paper/gui/PaperGuiRenderer.java`](src/paper/src/main/java/com/notquests/paper/gui/PaperGuiRenderer.java)

- [x] **V1** [line 37](src/paper/src/main/java/com/notquests/paper/gui/PaperGuiRenderer.java#L37) — `showGui(String guiName, Player player, GuiContext guiContext)` in `PaperGuiRenderer`
  - Why it violates the boundary: Combines validation, core GUI construction, failure logging, rendering, and opening.
  - Guidance: This is a major boundary violation: combines validation, core GUI construction, failure logging, rendering, and opening. The method decides NotQuests behavior or coordinates several domain steps, so Paper and NeoForge can diverge. Move that decision, state, and sequencing into the core GUI owner. Leave behind the smallest native fact or effect needed by core. Do this before cosmetic cleanup because it affects behavior parity and testability.

#### [`src/paper/src/main/java/com/notquests/paper/managers/ArmorStandManager.java`](src/paper/src/main/java/com/notquests/paper/managers/ArmorStandManager.java)

- [x] **V1** [line 34](src/paper/src/main/java/com/notquests/paper/managers/ArmorStandManager.java#L34) — `hasCoreAttachment(ArmorStand armorStand)` in `ArmorStandManager`
  - Why it violates the boundary: Performs core NPC attachment lookup/combination in Paper.
  - Guidance: This is a major boundary violation: performs core NPC attachment lookup/combination in Paper. The method decides NotQuests behavior or coordinates several domain steps, so Paper and NeoForge can diverge. Move that decision, state, and sequencing into core NpcAttachments or the core conversation/quest owner that requested the NPC operation. Leave behind the smallest native fact or effect needed by core. Do this before cosmetic cleanup because it affects behavior parity and testability.

#### [`src/paper/src/main/java/com/notquests/paper/managers/integrations/betonquest/conversationInterceptors/NotQuestsInterceptor.java`](src/paper/src/main/java/com/notquests/paper/managers/integrations/betonquest/conversationInterceptors/NotQuestsInterceptor.java)

- [x] **V1** [line 23](src/paper/src/main/java/com/notquests/paper/managers/integrations/betonquest/conversationInterceptors/NotQuestsInterceptor.java#L23) — `sendMessage(Component message)` in `NotQuestsInterceptor`
  - Why it violates the boundary: Mixes message delivery with core conversation-history/configuration policy.
  - Guidance: This is a major boundary violation: mixes message delivery with core conversation-history/configuration policy. The method decides NotQuests behavior or coordinates several domain steps, so Paper and NeoForge can diverge. Move that decision, state, and sequencing into core Conversations/chat-history ownership. Leave behind the smallest native fact or effect needed by core. Do this before cosmetic cleanup because it affects behavior parity and testability.

#### [`src/paper/src/main/java/com/notquests/paper/managers/integrations/citizens/CitizensManager.java`](src/paper/src/main/java/com/notquests/paper/managers/integrations/citizens/CitizensManager.java)

- [x] **V1** [line 97](src/paper/src/main/java/com/notquests/paper/managers/integrations/citizens/CitizensManager.java#L97) — `onDisable()` in `CitizensManager`
  - Why it violates the boundary: Scans NPCs and traits, coordinates cleanup/deregistration, and emits lifecycle messages.
  - Guidance: This is a major boundary violation: scans NPCs and traits, coordinates cleanup/deregistration, and emits lifecycle messages. The method decides NotQuests behavior or coordinates several domain steps, so Paper and NeoForge can diverge. Move that decision, state, and sequencing into core NpcAttachments or the core conversation/quest owner that requested the NPC operation. Leave behind the smallest native fact or effect needed by core. Do this before cosmetic cleanup because it affects behavior parity and testability.

- [x] **V1** [line 137](src/paper/src/main/java/com/notquests/paper/managers/integrations/citizens/CitizensManager.java#L137) — `handleEscortObjective(ActiveObjective progress)` in `CitizensManager`
  - Why it violates the boundary: Coordinates multiple steps, validation, state, or messages; it is an orchestrated flow.
  - Guidance: This is a major boundary violation: coordinates multiple steps, validation, state, or messages; it is an orchestrated flow. The method decides NotQuests behavior or coordinates several domain steps, so Paper and NeoForge can diverge. Move that decision, state, and sequencing into the relevant core text, translation, or feature owner. Leave behind the smallest native fact or effect needed by core. Do this before cosmetic cleanup because it affects behavior parity and testability.

- [x] **V1** [line 158](src/paper/src/main/java/com/notquests/paper/managers/integrations/citizens/CitizensManager.java#L158) — `startEscortObjective(int npcToEscortID, int destinationNPCID, Location spawnLocation, java.util.UUID questPlayerId)` in `CitizensManager`
  - Why it violates the boundary: Coordinates multiple steps, validation, state, or messages; it is an orchestrated flow.
  - Guidance: This is a major boundary violation: coordinates multiple steps, validation, state, or messages; it is an orchestrated flow. The method decides NotQuests behavior or coordinates several domain steps, so Paper and NeoForge can diverge. Move that decision, state, and sequencing into the relevant core text, translation, or feature owner. Leave behind the smallest native fact or effect needed by core. Do this before cosmetic cleanup because it affects behavior parity and testability.

- [x] **V1** [line 235](src/paper/src/main/java/com/notquests/paper/managers/integrations/citizens/CitizensManager.java#L235) — `startEscortObjectiveSynchronous(NPC npcToEscort, NPC destinationNPC, FollowTrait followerTrait, java.util.UUID questPlayerId, Location configuredSpawnLocation)` in `CitizensManager`
  - Why it violates the boundary: Coordinates multiple steps, validation, state, or messages; it is an orchestrated flow.
  - Guidance: This is a major boundary violation: coordinates multiple steps, validation, state, or messages; it is an orchestrated flow. The method decides NotQuests behavior or coordinates several domain steps, so Paper and NeoForge can diverge. Move that decision, state, and sequencing into the relevant core text, translation, or feature owner. Leave behind the smallest native fact or effect needed by core. Do this before cosmetic cleanup because it affects behavior parity and testability.

- [x] **V1** [line 293](src/paper/src/main/java/com/notquests/paper/managers/integrations/citizens/CitizensManager.java#L293) — `cleanupBuggedNPCs()` in `CitizensManager`
  - Why it violates the boundary: Coordinates multiple steps, validation, state, or messages; it is an orchestrated flow.
  - Guidance: This is a major boundary violation: coordinates multiple steps, validation, state, or messages; it is an orchestrated flow. The method decides NotQuests behavior or coordinates several domain steps, so Paper and NeoForge can diverge. Move that decision, state, and sequencing into the relevant core text, translation, or feature owner. Leave behind the smallest native fact or effect needed by core. Do this before cosmetic cleanup because it affects behavior parity and testability.

#### [`src/paper/src/main/java/com/notquests/paper/managers/integrations/citizens/QuestGiverNPCTrait.java`](src/paper/src/main/java/com/notquests/paper/managers/integrations/citizens/QuestGiverNPCTrait.java)

- [x] **V1** [line 81](src/paper/src/main/java/com/notquests/paper/managers/integrations/citizens/QuestGiverNPCTrait.java#L81) — `run()` in `QuestGiverNPCTrait`
  - Why it violates the boundary: Owns timing, visibility, attachment, and indicator gameplay policy plus rendering.
  - Guidance: This is a major boundary violation: owns timing, visibility, attachment, and indicator gameplay policy plus rendering. The method decides NotQuests behavior or coordinates several domain steps, so Paper and NeoForge can diverge. Move that decision, state, and sequencing into the matching core concept owner. Leave behind the smallest native fact or effect needed by core. Do this before cosmetic cleanup because it affects behavior parity and testability.

- [x] **V1** [line 222](src/paper/src/main/java/com/notquests/paper/managers/integrations/citizens/QuestGiverNPCTrait.java#L222) — `onRemove()` in `QuestGiverNPCTrait`
  - Why it violates the boundary: Coordinates core NPC detachment, trait mutation, passenger cleanup, and messages.
  - Guidance: This is a major boundary violation: coordinates core NPC detachment, trait mutation, passenger cleanup, and messages. The method decides NotQuests behavior or coordinates several domain steps, so Paper and NeoForge can diverge. Move that decision, state, and sequencing into core NpcAttachments or the core conversation/quest owner that requested the NPC operation. Leave behind the smallest native fact or effect needed by core. Do this before cosmetic cleanup because it affects behavior parity and testability.

#### [`src/paper/src/main/java/com/notquests/paper/managers/npc/ArmorstandNPC.java`](src/paper/src/main/java/com/notquests/paper/managers/npc/ArmorstandNPC.java)

- [x] **V1** [line 47](src/paper/src/main/java/com/notquests/paper/managers/npc/ArmorstandNPC.java#L47) — `removeQuestGiverNPCTrait(Boolean showQuestInNPC, String questIdentifier)` in `ArmorstandNPC`
  - Why it violates the boundary: Mixes platform mutation with core attachment policy and user-facing error text.
  - Guidance: This is a major boundary violation: mixes platform mutation with core attachment policy and user-facing error text. The method decides NotQuests behavior or coordinates several domain steps, so Paper and NeoForge can diverge. Move that decision, state, and sequencing into core NpcAttachments or the core conversation/quest owner that requested the NPC operation. Leave behind the smallest native fact or effect needed by core. Do this before cosmetic cleanup because it affects behavior parity and testability.

- [x] **V1** [line 58](src/paper/src/main/java/com/notquests/paper/managers/npc/ArmorstandNPC.java#L58) — `addQuestGiverNPCTrait(Boolean showQuestInNPC, String questIdentifier)` in `ArmorstandNPC`
  - Why it violates the boundary: Mixes platform mutation with core attachment policy and user-facing error text.
  - Guidance: This is a major boundary violation: mixes platform mutation with core attachment policy and user-facing error text. The method decides NotQuests behavior or coordinates several domain steps, so Paper and NeoForge can diverge. Move that decision, state, and sequencing into core NpcAttachments or the core conversation/quest owner that requested the NPC operation. Leave behind the smallest native fact or effect needed by core. Do this before cosmetic cleanup because it affects behavior parity and testability.

#### [`src/paper/src/main/java/com/notquests/paper/managers/npc/ConversationFocus.java`](src/paper/src/main/java/com/notquests/paper/managers/npc/ConversationFocus.java)

- [x] **V1** [line 44](src/paper/src/main/java/com/notquests/paper/managers/npc/ConversationFocus.java#L44) — `run()` in `ConversationFocus`
  - Why it violates the boundary: Owns a full conversation lifecycle, distance rule, messages, and camera animation.
  - Guidance: This is a major boundary violation: owns a full conversation lifecycle, distance rule, messages, and camera animation. The method decides NotQuests behavior or coordinates several domain steps, so Paper and NeoForge can diverge. Move that decision, state, and sequencing into core Conversations/chat-history ownership. Leave behind the smallest native fact or effect needed by core. Do this before cosmetic cleanup because it affects behavior parity and testability.

- [x] **V1** [line 107](src/paper/src/main/java/com/notquests/paper/managers/npc/ConversationFocus.java#L107) — `getRotation()` in `ConversationFocus`
  - Why it violates the boundary: Portable focus interpolation/geometry is embedded in Paper orchestration.
  - Guidance: This is a major boundary violation: portable focus interpolation/geometry is embedded in Paper orchestration. The method decides NotQuests behavior or coordinates several domain steps, so Paper and NeoForge can diverge. Move that decision, state, and sequencing into core NpcAttachments or the core conversation/quest owner that requested the NPC operation. Leave behind the smallest native fact or effect needed by core. Do this before cosmetic cleanup because it affects behavior parity and testability.

#### [`src/paper/src/main/java/com/notquests/paper/managers/npc/NQNPC.java`](src/paper/src/main/java/com/notquests/paper/managers/npc/NQNPC.java)

- [x] **V1** [line 44](src/paper/src/main/java/com/notquests/paper/managers/npc/NQNPC.java#L44) — `saveToConfig(YamlConfig configuration, String partialPath)` in `NQNPC`
  - Why it violates the boundary: Configuration persistence/loading and fallback logic belong in core/migrations.
  - Guidance: This is a major boundary violation: configuration persistence/loading and fallback logic belong in core/migrations. The method decides NotQuests behavior or coordinates several domain steps, so Paper and NeoForge can diverge. Move that decision, state, and sequencing into core NpcAttachments or the core conversation/quest owner that requested the NPC operation. Leave behind the smallest native fact or effect needed by core. Do this before cosmetic cleanup because it affects behavior parity and testability.

- [x] **V1** [line 50](src/paper/src/main/java/com/notquests/paper/managers/npc/NQNPC.java#L50) — `fromConfig(NotQuests main, YamlConfig configuration, String partialPath)` in `NQNPC`
  - Why it violates the boundary: Configuration persistence/loading and fallback logic belong in core/migrations.
  - Guidance: This is a major boundary violation: configuration persistence/loading and fallback logic belong in core/migrations. The method decides NotQuests behavior or coordinates several domain steps, so Paper and NeoForge can diverge. Move that decision, state, and sequencing into core NpcAttachments or the core conversation/quest owner that requested the NPC operation. Leave behind the smallest native fact or effect needed by core. Do this before cosmetic cleanup because it affects behavior parity and testability.

#### [`src/paper/src/main/java/com/notquests/paper/managers/packets/ownpacketstuff/modern/PacketInjector.java`](src/paper/src/main/java/com/notquests/paper/managers/packets/ownpacketstuff/modern/PacketInjector.java)

- [x] **V1** [line 46](src/paper/src/main/java/com/notquests/paper/managers/packets/ownpacketstuff/modern/PacketInjector.java#L46) — `setPacketStuffEnabled(boolean packetStuffEnabled)` in `PacketInjector`
  - Why it violates the boundary: Changes shared configuration and conversation-history behavior on capability state change.
  - Guidance: This is a major boundary violation: changes shared configuration and conversation-history behavior on capability state change. The method decides NotQuests behavior or coordinates several domain steps, so Paper and NeoForge can diverge. Move that decision, state, and sequencing into core Conversations/chat-history ownership. Leave behind the smallest native fact or effect needed by core. Do this before cosmetic cleanup because it affects behavior parity and testability.

#### [`src/paper/src/main/java/com/notquests/paper/managers/packets/ownpacketstuff/reflection/ReflectionPacketInjector.java`](src/paper/src/main/java/com/notquests/paper/managers/packets/ownpacketstuff/reflection/ReflectionPacketInjector.java)

- [x] **V1** [line 41](src/paper/src/main/java/com/notquests/paper/managers/packets/ownpacketstuff/reflection/ReflectionPacketInjector.java#L41) — `setPacketStuffEnabled(boolean packetStuffEnabled)` in `ReflectionPacketInjector`
  - Why it violates the boundary: Changes shared configuration and conversation-history behavior on capability state change.
  - Guidance: This is a major boundary violation: changes shared configuration and conversation-history behavior on capability state change. The method decides NotQuests behavior or coordinates several domain steps, so Paper and NeoForge can diverge. Move that decision, state, and sequencing into core Conversations/chat-history ownership. Leave behind the smallest native fact or effect needed by core. Do this before cosmetic cleanup because it affects behavior parity and testability.

### NeoForge (26)

#### [`src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeConversationDisplay.java`](src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeConversationDisplay.java)

- [x] **V1** [line 46](src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeConversationDisplay.java#L46) — `chooseAnswerPrefix(PlatformPlayer questPlayer)` in `NeoForgeConversationDisplay`
  - Why it violates the boundary: Runs conversation prefix/history behavior rather than only rendering supplied text.
  - Guidance: This is a major boundary violation: runs conversation prefix/history behavior rather than only rendering supplied text. The method decides NotQuests behavior or coordinates several domain steps, so Paper and NeoForge can diverge. Move that decision, state, and sequencing into core Conversations/chat-history ownership. Leave behind the smallest native fact or effect needed by core. Do this before cosmetic cleanup because it affects behavior parity and testability.

- [x] **V1** [line 58](src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeConversationDisplay.java#L58) — `option(PlatformPlayer questPlayer, ConversationManager.DisplayOption option)` in `NeoForgeConversationDisplay`
  - Why it violates the boundary: Owns option command/hover construction and shared conversation-history mutation.
  - Guidance: This is a major boundary violation: owns option command/hover construction and shared conversation-history mutation. The method decides NotQuests behavior or coordinates several domain steps, so Paper and NeoForge can diverge. Move that decision, state, and sequencing into the relevant core command definition or the core command framework. Leave behind the smallest native fact or effect needed by core. Do this before cosmetic cleanup because it affects behavior parity and testability.

- [x] **V1** [line 80](src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeConversationDisplay.java#L80) — `optionsFinished(PlatformPlayer questPlayer)` in `NeoForgeConversationDisplay`
  - Why it violates the boundary: Routes conversation completion through shared history behavior, not one native effect.
  - Guidance: This is a major boundary violation: routes conversation completion through shared history behavior, not one native effect. The method decides NotQuests behavior or coordinates several domain steps, so Paper and NeoForge can diverge. Move that decision, state, and sequencing into core Conversations/chat-history ownership. Leave behind the smallest native fact or effect needed by core. Do this before cosmetic cleanup because it affects behavior parity and testability.

- [x] **V1** [line 88](src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeConversationDisplay.java#L88) — `sendConversationMessage(ServerPlayer player, net.kyori.adventure.text.Component component)` in `NeoForgeConversationDisplay`
  - Why it violates the boundary: Combines shared history recording with native message delivery.
  - Guidance: This is a major boundary violation: combines shared history recording with native message delivery. The method decides NotQuests behavior or coordinates several domain steps, so Paper and NeoForge can diverge. Move that decision, state, and sequencing into the relevant core text, translation, or feature owner. Leave behind the smallest native fact or effect needed by core. Do this before cosmetic cleanup because it affects behavior parity and testability.

- [x] **V1** [line 95](src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeConversationDisplay.java#L95) — `rememberConversationMessage(ServerPlayer player, net.kyori.adventure.text.Component component)` in `NeoForgeConversationDisplay`
  - Why it violates the boundary: Reads core configuration and mutates shared conversation display history.
  - Guidance: This is a major boundary violation: reads core configuration and mutates shared conversation display history. The method decides NotQuests behavior or coordinates several domain steps, so Paper and NeoForge can diverge. Move that decision, state, and sequencing into core Conversations/chat-history ownership. Leave behind the smallest native fact or effect needed by core. Do this before cosmetic cleanup because it affects behavior parity and testability.

- [x] **V1** [line 103](src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeConversationDisplay.java#L103) — `removeOldMessages(ServerPlayer player)` in `NeoForgeConversationDisplay`
  - Why it violates the boundary: Owns delete-previous policy and shared history replay orchestration.
  - Guidance: This is a major boundary violation: owns delete-previous policy and shared history replay orchestration. The method decides NotQuests behavior or coordinates several domain steps, so Paper and NeoForge can diverge. Move that decision, state, and sequencing into the matching core concept owner. Leave behind the smallest native fact or effect needed by core. Do this before cosmetic cleanup because it affects behavior parity and testability.

#### [`src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeCoreCommandCompiler.java`](src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeCoreCommandCompiler.java)

- [x] **V1** [line 532](src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeCoreCommandCompiler.java#L532) — `execute(NQCommandTree.Node<NQArgumentType, NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>, NQSuggestionProvider<NQCommandContext>, NQCommandHandler> node, CommandContext<CommandSourceStack> context, String flagString, List<NQCommandTree.Node<NQArgumentType, NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>, NQSuggestionProvider<NQCommandContext>, NQCommandHandler>> path)` in `NeoForgeCoreCommandCompiler`
  - Why it violates the boundary: Owns command permission/sender checks, handler flow, success aggregation, and output sequencing.
  - Guidance: This is a major boundary violation: owns command permission/sender checks, handler flow, success aggregation, and output sequencing. The method decides NotQuests behavior or coordinates several domain steps, so Paper and NeoForge can diverge. Move that decision, state, and sequencing into the relevant core command definition or the core command framework. Leave behind the smallest native fact or effect needed by core. Do this before cosmetic cleanup because it affects behavior parity and testability.

- [x] **V1** [line 590](src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeCoreCommandCompiler.java#L590) — `printBranchHelp(NQCommandTree.Node<NQArgumentType, NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>, NQSuggestionProvider<NQCommandContext>, NQCommandHandler> node, List<NQCommandTree.Node<NQArgumentType, NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>, NQSuggestionProvider<NQCommandContext>, NQCommandHandler>> path, CommandContext<CommandSourceStack> context)` in `NeoForgeCoreCommandCompiler`
  - Why it violates the boundary: Owns command help/output flow; only conversion of core help parts to native components is leaf code.
  - Guidance: This is a major boundary violation: owns command help/output flow; only conversion of core help parts to native components is leaf code. The method decides NotQuests behavior or coordinates several domain steps, so Paper and NeoForge can diverge. Move that decision, state, and sequencing into the relevant core command definition or the core command framework. Leave behind the smallest native fact or effect needed by core. Do this before cosmetic cleanup because it affects behavior parity and testability.

#### [`src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeGuiRenderer.java`](src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeGuiRenderer.java)

- [x] **V1** [line 56](src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeGuiRenderer.java#L56) — `open(ServerPlayer player, String guiName, GuiContext context)` in `NeoForgeGuiRenderer`
  - Why it violates the boundary: Broad flow builds, validates, logs, materializes, and opens a GUI instead of one native effect.
  - Guidance: This is a major boundary violation: broad flow builds, validates, logs, materializes, and opens a GUI instead of one native effect. The method decides NotQuests behavior or coordinates several domain steps, so Paper and NeoForge can diverge. Move that decision, state, and sequencing into the core GUI owner. Leave behind the smallest native fact or effect needed by core. Do this before cosmetic cleanup because it affects behavior parity and testability.

#### [`src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeNpcAttachments.java`](src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeNpcAttachments.java)

- [x] **V1** [line 155](src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeNpcAttachments.java#L155) — `handleEntityInteraction(NeoForgeQuestPlayer questPlayer, Entity target, ItemStack usedItem)` in `NeoForgeNpcAttachments`
  - Why it violates the boundary: Broad interaction flow chooses commands, permissions, attachment behavior, messages, and cancellation.
  - Guidance: This is a major boundary violation: broad interaction flow chooses commands, permissions, attachment behavior, messages, and cancellation. The method decides NotQuests behavior or coordinates several domain steps, so Paper and NeoForge can diverge. Move that decision, state, and sequencing into the relevant core command definition or the core command framework. Leave behind the smallest native fact or effect needed by core. Do this before cosmetic cleanup because it affects behavior parity and testability.

#### [`src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeObjectiveEvents.java`](src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeObjectiveEvents.java)

- [x] **V1** [line 133](src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeObjectiveEvents.java#L133) — `onBreakBlock(BreakBlockEvent event)` in `NeoForgeObjectiveEvents`
  - Why it violates the boundary: Fans one block event into break/harvest state and placed-block bookkeeping decisions.
  - Guidance: This is a major boundary violation: fans one block event into break/harvest state and placed-block bookkeeping decisions. The method decides NotQuests behavior or coordinates several domain steps, so Paper and NeoForge can diverge. Move that decision, state, and sequencing into the matching core concept owner. Leave behind the smallest native fact or effect needed by core. Do this before cosmetic cleanup because it affects behavior parity and testability.

- [x] **V1** [line 153](src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeObjectiveEvents.java#L153) — `onPlaceBlock(BlockEvent.EntityPlaceEvent event)` in `NeoForgeObjectiveEvents`
  - Why it violates the boundary: Combines placement forwarding with shared harvest-placement tracking policy.
  - Guidance: This is a major boundary violation: combines placement forwarding with shared harvest-placement tracking policy. The method decides NotQuests behavior or coordinates several domain steps, so Paper and NeoForge can diverge. Move that decision, state, and sequencing into the matching core concept owner. Leave behind the smallest native fact or effect needed by core. Do this before cosmetic cleanup because it affects behavior parity and testability.

- [x] **V1** [line 190](src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeObjectiveEvents.java#L190) — `onLivingDeath(LivingDeathEvent event)` in `NeoForgeObjectiveEvents`
  - Why it violates the boundary: Fans one death into separate trigger/objective flows instead of one core event entry point.
  - Guidance: This is a major boundary violation: fans one death into separate trigger/objective flows instead of one core event entry point. The method decides NotQuests behavior or coordinates several domain steps, so Paper and NeoForge can diverge. Move that decision, state, and sequencing into core QuestPlayer, ActiveObjectives, or the concrete quest/objective feature owner. Leave behind the smallest native fact or effect needed by core. Do this before cosmetic cleanup because it affects behavior parity and testability.

- [x] **V1** [line 245](src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeObjectiveEvents.java#L245) — `onRightClickBlock(PlayerInteractEvent.RightClickBlock event)` in `NeoForgeObjectiveEvents`
  - Why it violates the boundary: Fans one click into buried-treasure and generic interaction gameplay paths.
  - Guidance: This is a major boundary violation: fans one click into buried-treasure and generic interaction gameplay paths. The method decides NotQuests behavior or coordinates several domain steps, so Paper and NeoForge can diverge. Move that decision, state, and sequencing into the matching core concept owner. Leave behind the smallest native fact or effect needed by core. Do this before cosmetic cleanup because it affects behavior parity and testability.

- [x] **V1** [line 264](src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeObjectiveEvents.java#L264) — `onRightClickEntity(PlayerInteractEvent.EntityInteract event)` in `NeoForgeObjectiveEvents`
  - Why it violates the boundary: Broad entity interaction flow chooses NPC, feeding, milking, and shearing behavior.
  - Guidance: This is a major boundary violation: broad entity interaction flow chooses NPC, feeding, milking, and shearing behavior. The method decides NotQuests behavior or coordinates several domain steps, so Paper and NeoForge can diverge. Move that decision, state, and sequencing into core NpcAttachments or the core conversation/quest owner that requested the NPC operation. Leave behind the smallest native fact or effect needed by core. Do this before cosmetic cleanup because it affects behavior parity and testability.

- [x] **V1** [line 321](src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeObjectiveEvents.java#L321) — `onPlayerTick(PlayerTickEvent.Post event)` in `NeoForgeObjectiveEvents`
  - Why it violates the boundary: Owns per-player movement/sneak state and the global quest runtime tick policy.
  - Guidance: This is a major boundary violation: owns per-player movement/sneak state and the global quest runtime tick policy. The method decides NotQuests behavior or coordinates several domain steps, so Paper and NeoForge can diverge. Move that decision, state, and sequencing into core QuestPlayer, ActiveObjectives, or the concrete quest/objective feature owner. Leave behind the smallest native fact or effect needed by core. Do this before cosmetic cleanup because it affects behavior parity and testability.

- [x] **V1** [line 451](src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeObjectiveEvents.java#L451) — `onCommand(CommandEvent event)` in `NeoForgeObjectiveEvents`
  - Why it violates the boundary: Owns portable slash normalization before forwarding the command event.
  - Guidance: This is a major boundary violation: owns portable slash normalization before forwarding the command event. The method decides NotQuests behavior or coordinates several domain steps, so Paper and NeoForge can diverge. Move that decision, state, and sequencing into the relevant core command definition or the core command framework. Leave behind the smallest native fact or effect needed by core. Do this before cosmetic cleanup because it affects behavior parity and testability.

- [x] **V1** [line 464](src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeObjectiveEvents.java#L464) — `onServerChat(ServerChatEvent event)` in `NeoForgeObjectiveEvents`
  - Why it violates the boundary: Mutates shared conversation/chat history for every viewer instead of only translating the event.
  - Guidance: This is a major boundary violation: mutates shared conversation/chat history for every viewer instead of only translating the event. The method decides NotQuests behavior or coordinates several domain steps, so Paper and NeoForge can diverge. Move that decision, state, and sequencing into core Conversations/chat-history ownership. Leave behind the smallest native fact or effect needed by core. Do this before cosmetic cleanup because it affects behavior parity and testability.

- [x] **V1** [line 690](src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeObjectiveEvents.java#L690) — `cleanupPlayer(ServerPlayer player)` in `NeoForgeObjectiveEvents`
  - Why it violates the boundary: Broad per-player cleanup coordinates NPC, beam, player, and movement state.
  - Guidance: This is a major boundary violation: broad per-player cleanup coordinates NPC, beam, player, and movement state. The method decides NotQuests behavior or coordinates several domain steps, so Paper and NeoForge can diverge. Move that decision, state, and sequencing into core NpcAttachments or the core conversation/quest owner that requested the NPC operation. Leave behind the smallest native fact or effect needed by core. Do this before cosmetic cleanup because it affects behavior parity and testability.

#### [`src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeQuestPlayer.java`](src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeQuestPlayer.java)

- [x] **V1** [line 1031](src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeQuestPlayer.java#L1031) — `spawnMob(String entityType, int amount, boolean usePlayerLocation, NQLocation location, int radiusX, int radiusY, int radiusZ)` in `NeoForgeQuestPlayer`
  - Why it violates the boundary: Owns mob count clamping and random spawn-placement gameplay policy as well as the native spawn effect.
  - Guidance: This is a major boundary violation: owns mob count clamping and random spawn-placement gameplay policy as well as the native spawn effect. The method decides NotQuests behavior or coordinates several domain steps, so Paper and NeoForge can diverge. Move that decision, state, and sequencing into the matching core concept owner. Leave behind the smallest native fact or effect needed by core. Do this before cosmetic cleanup because it affects behavior parity and testability.

#### [`src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeText.java`](src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeText.java)

- [x] **V1** [line 80](src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeText.java#L80) — `sendMessage(ServerPlayer player, String miniMessageText)` in `NeoForgeText`
  - Why it violates the boundary: Combines native delivery with mutation of shared non-conversation history.
  - Guidance: This is a major boundary violation: combines native delivery with mutation of shared non-conversation history. The method decides NotQuests behavior or coordinates several domain steps, so Paper and NeoForge can diverge. Move that decision, state, and sequencing into core Conversations/chat-history ownership. Leave behind the smallest native fact or effect needed by core. Do this before cosmetic cleanup because it affects behavior parity and testability.

#### [`src/neoforge/src/main/java/com/notquests/neoforge/NotQuestsNeoForge.java`](src/neoforge/src/main/java/com/notquests/neoforge/NotQuestsNeoForge.java)

- [x] **V1** [line 49](src/neoforge/src/main/java/com/notquests/neoforge/NotQuestsNeoForge.java#L49) — `NotQuestsNeoForge(IEventBus modBus)` in `NotQuestsNeoForge`
  - Why it violates the boundary: Broad mod bootstrap orchestrates logging, registries, commands, content, events, and services.
  - Guidance: This is a major boundary violation: broad mod bootstrap orchestrates logging, registries, commands, content, events, and services. The method decides NotQuests behavior or coordinates several domain steps, so Paper and NeoForge can diverge. Move that decision, state, and sequencing into the relevant core command definition or the core command framework. Leave behind the smallest native fact or effect needed by core. Do this before cosmetic cleanup because it affects behavior parity and testability.

- [x] **V1** [line 111](src/neoforge/src/main/java/com/notquests/neoforge/NotQuestsNeoForge.java#L111) — `onServerStarting(ServerStartingEvent event)` in `NotQuestsNeoForge`
  - Why it violates the boundary: Broad server-start flow coordinates executor, events, server state, and core startup.
  - Guidance: This is a major boundary violation: broad server-start flow coordinates executor, events, server state, and core startup. The method decides NotQuests behavior or coordinates several domain steps, so Paper and NeoForge can diverge. Move that decision, state, and sequencing into the core plugin lifecycle owner. Leave behind the smallest native fact or effect needed by core. Do this before cosmetic cleanup because it affects behavior parity and testability.

- [x] **V1** [line 135](src/neoforge/src/main/java/com/notquests/neoforge/NotQuestsNeoForge.java#L135) — `loadPlatform()` in `NotQuestsNeoForge`
  - Why it violates the boundary: Orchestrates player runtime, data/configuration setup, and text configuration.
  - Guidance: This is a major boundary violation: orchestrates player runtime, data/configuration setup, and text configuration. The method decides NotQuests behavior or coordinates several domain steps, so Paper and NeoForge can diverge. Move that decision, state, and sequencing into the core GeneralConfig/PlayerDatabase/data-lifecycle owner. Leave behind the smallest native fact or effect needed by core. Do this before cosmetic cleanup because it affects behavior parity and testability.

- [x] **V1** [line 201](src/neoforge/src/main/java/com/notquests/neoforge/NotQuestsNeoForge.java#L201) — `stopPlatform()` in `NotQuestsNeoForge`
  - Why it violates the boundary: Broad shutdown flow cancels loading and coordinates all adapter subsystem cleanup.
  - Guidance: This is a major boundary violation: broad shutdown flow cancels loading and coordinates all adapter subsystem cleanup. The method decides NotQuests behavior or coordinates several domain steps, so Paper and NeoForge can diverge. Move that decision, state, and sequencing into the core plugin lifecycle owner. Leave behind the smallest native fact or effect needed by core. Do this before cosmetic cleanup because it affects behavior parity and testability.

- [x] **V1** [line 239](src/neoforge/src/main/java/com/notquests/neoforge/NotQuestsNeoForge.java#L239) — `onRegisterCommands(RegisterCommandsEvent event)` in `NotQuestsNeoForge`
  - Why it violates the boundary: Broad command flow registers roots and triggers shared metadata export.
  - Guidance: This is a major boundary violation: broad command flow registers roots and triggers shared metadata export. The method decides NotQuests behavior or coordinates several domain steps, so Paper and NeoForge can diverge. Move that decision, state, and sequencing into the relevant core command definition or the core command framework. Leave behind the smallest native fact or effect needed by core. Do this before cosmetic cleanup because it affects behavior parity and testability.

## GAP — Incomplete or unsafe adapter capabilities

### Paper (3)

#### [`src/paper/src/main/java/com/notquests/paper/managers/packets/ownpacketstuff/modern/PacketInjector.java`](src/paper/src/main/java/com/notquests/paper/managers/packets/ownpacketstuff/modern/PacketInjector.java)

- [x] **GAP** [line 55](src/paper/src/main/java/com/notquests/paper/managers/packets/ownpacketstuff/modern/PacketInjector.java#L55) — `initializeNMSStuff()` in `PacketInjector`
  - Why it violates the boundary: Empty/commented/incomplete platform capability stub.
  - Guidance: This override currently promises a platform hook but does not provide a complete or safe implementation. Implement the native operation, or add an explicit capability check and stop registering/calling the feature on this platform. Do not rely on a silent null, false, empty result, or permissive fallback because that hides Paper/NeoForge parity bugs until runtime.

- [x] **GAP** [line 107](src/paper/src/main/java/com/notquests/paper/managers/packets/ownpacketstuff/modern/PacketInjector.java#L107) — `spawnBeaconBeam(Player player, Location location)` in `PacketInjector`
  - Why it violates the boundary: Empty/commented/incomplete platform capability stub.
  - Guidance: This override currently promises a platform hook but does not provide a complete or safe implementation. Implement the native operation, or add an explicit capability check and stop registering/calling the feature on this platform. Do not rely on a silent null, false, empty result, or permissive fallback because that hides Paper/NeoForge parity bugs until runtime.

- [x] **GAP** [line 215](src/paper/src/main/java/com/notquests/paper/managers/packets/ownpacketstuff/modern/PacketInjector.java#L215) — `sendHolo(Player player, ArmorStand armorStand, boolean show)` in `PacketInjector`
  - Why it violates the boundary: Empty/commented/incomplete platform capability stub.
  - Guidance: This override currently promises a platform hook but does not provide a complete or safe implementation. Implement the native operation, or add an explicit capability check and stop registering/calling the feature on this platform. Do not rely on a silent null, false, empty result, or permissive fallback because that hides Paper/NeoForge parity bugs until runtime.

### NeoForge (1)

#### [`src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeCoreCommandCompiler.java`](src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeCoreCommandCompiler.java)

- [x] **GAP** [line 670](src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeCoreCommandCompiler.java#L670) — `senderTypeMatches(CommandContext<CommandSourceStack> context, Class<?> senderType)` in `NeoForgeCoreCommandCompiler`
  - Why it violates the boundary: Contains incomplete sender-type policy and a permissive fallback for unknown types.
  - Guidance: This override currently promises a platform hook but does not provide a complete or safe implementation. Implement the native operation, or add an explicit capability check and stop registering/calling the feature on this platform. Do not rely on a silent null, false, empty result, or permissive fallback because that hides Paper/NeoForge parity bugs until runtime.

## V2 — Portable-code boundary violations

### Paper (209)

#### [`src/paper/src/main/java/com/notquests/paper/NotQuests.java`](src/paper/src/main/java/com/notquests/paper/NotQuests.java)

- [x] **V2** [line 90](src/paper/src/main/java/com/notquests/paper/NotQuests.java#L90) — `registerRegistryPacks()` in `NotQuests`
  - Why it violates the boundary: Portable core lookup/delegation with no atomic Paper capability.
  - Guidance: This is a portable-code violation: portable core lookup/delegation with no atomic Paper capability. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the core plugin lifecycle owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 94](src/paper/src/main/java/com/notquests/paper/NotQuests.java#L94) — `refreshRegistryPacksAfterVariableChange()` in `NotQuests`
  - Why it violates the boundary: Portable core lookup/delegation with no atomic Paper capability.
  - Guidance: This is a portable-code violation: portable core lookup/delegation with no atomic Paper capability. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the core plugin lifecycle owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 98](src/paper/src/main/java/com/notquests/paper/NotQuests.java#L98) — `notifyRegistryPacksAfterVariableValueChange(String variableName, PaperPlayer questPlayer)` in `NotQuests`
  - Why it violates the boundary: Portable core lookup/delegation with no atomic Paper capability.
  - Guidance: This is a portable-code violation: portable core lookup/delegation with no atomic Paper capability. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the core plugin lifecycle owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 143](src/paper/src/main/java/com/notquests/paper/NotQuests.java#L143) — `colorsForTag(String tagName)` in `NotQuests`
  - Why it violates the boundary: Portable core lookup/delegation with no atomic Paper capability.
  - Guidance: This is a portable-code violation: portable core lookup/delegation with no atomic Paper capability. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the core plugin lifecycle owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 416](src/paper/src/main/java/com/notquests/paper/NotQuests.java#L416) — `paperParticle(String key, String value)` in `NotQuests`
  - Why it violates the boundary: Owns config validation, fallback choice, and warning text around a platform conversion.
  - Guidance: This is a portable-code violation: owns config validation, fallback choice, and warning text around a platform conversion. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the core GeneralConfig/PlayerDatabase/data-lifecycle owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 426](src/paper/src/main/java/com/notquests/paper/NotQuests.java#L426) — `loadJournalItem()` in `NotQuests`
  - Why it violates the boundary: Owns journal defaults, config decoding, fallback parsing, validation, and warning policy.
  - Guidance: This is a portable-code violation: owns journal defaults, config decoding, fallback parsing, validation, and warning policy. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the core journal/inventory behavior owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

#### [`src/paper/src/main/java/com/notquests/paper/adapter/PaperNotQuestsAdapter.java`](src/paper/src/main/java/com/notquests/paper/adapter/PaperNotQuestsAdapter.java)

- [x] **V2** [line 308](src/paper/src/main/java/com/notquests/paper/adapter/PaperNotQuestsAdapter.java#L308) — `itemSelectionOptions()` in `PaperNotQuestsAdapter`
  - Why it violates the boundary: Owns portable command-option/keyword suggestion policy, not just a platform observation.
  - Guidance: This is a portable-code violation: owns portable command-option/keyword suggestion policy, not just a platform observation. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core command definition or the core command framework—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 347](src/paper/src/main/java/com/notquests/paper/adapter/PaperNotQuestsAdapter.java#L347) — `entityTypeIds()` in `PaperNotQuestsAdapter`
  - Why it violates the boundary: Owns portable command-option/keyword suggestion policy, not just a platform observation.
  - Guidance: This is a portable-code violation: owns portable command-option/keyword suggestion policy, not just a platform observation. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core command definition or the core command framework—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 424](src/paper/src/main/java/com/notquests/paper/adapter/PaperNotQuestsAdapter.java#L424) — `blockMaterialOptions()` in `PaperNotQuestsAdapter`
  - Why it violates the boundary: Owns portable command-option/keyword suggestion policy, not just a platform observation.
  - Guidance: This is a portable-code violation: owns portable command-option/keyword suggestion policy, not just a platform observation. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core command definition or the core command framework—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 556](src/paper/src/main/java/com/notquests/paper/adapter/PaperNotQuestsAdapter.java#L556) — `setBlockMaterial(PlatformPlayer questPlayer, NQLocation location, String materialOrKeyword)` in `PaperNotQuestsAdapter`
  - Why it violates the boundary: Interprets hand/any/saved-item gameplay keywords before applying the block mutation.
  - Guidance: This is a portable-code violation: interprets hand/any/saved-item gameplay keywords before applying the block mutation. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the core Items/SavedItems or objective owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 706](src/paper/src/main/java/com/notquests/paper/adapter/PaperNotQuestsAdapter.java#L706) — `npcSelectorOptions(boolean allowNone, boolean allowRightClickSelect)` in `PaperNotQuestsAdapter`
  - Why it violates the boundary: Owns portable command-option/keyword suggestion policy, not just a platform observation.
  - Guidance: This is a portable-code violation: owns portable command-option/keyword suggestion policy, not just a platform observation. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core command definition or the core command framework—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 765](src/paper/src/main/java/com/notquests/paper/adapter/PaperNotQuestsAdapter.java#L765) — `giveArmorStandTool(PlatformPlayer actor, ArmorStandToolItem tool)` in `PaperNotQuestsAdapter`
  - Why it violates the boundary: Validates a command-oriented tool definition and authors operation result text.
  - Guidance: This is a portable-code violation: validates a command-oriented tool definition and authors operation result text. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core command definition or the core command framework—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 811](src/paper/src/main/java/com/notquests/paper/adapter/PaperNotQuestsAdapter.java#L811) — `variableNames(VariableDataType type)` in `PaperNotQuestsAdapter`
  - Why it violates the boundary: Portable core registry/log delegation with no Paper capability.
  - Guidance: This is a portable-code violation: portable core registry/log delegation with no Paper capability. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the matching core concept owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 816](src/paper/src/main/java/com/notquests/paper/adapter/PaperNotQuestsAdapter.java#L816) — `variableType(String variableName)` in `PaperNotQuestsAdapter`
  - Why it violates the boundary: Portable core registry/log delegation with no Paper capability.
  - Guidance: This is a portable-code violation: portable core registry/log delegation with no Paper capability. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the matching core concept owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 821](src/paper/src/main/java/com/notquests/paper/adapter/PaperNotQuestsAdapter.java#L821) — `variableSingular(String variableName)` in `PaperNotQuestsAdapter`
  - Why it violates the boundary: Portable core registry/log delegation with no Paper capability.
  - Guidance: This is a portable-code violation: portable core registry/log delegation with no Paper capability. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the matching core concept owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 826](src/paper/src/main/java/com/notquests/paper/adapter/PaperNotQuestsAdapter.java#L826) — `variablePlural(String variableName)` in `PaperNotQuestsAdapter`
  - Why it violates the boundary: Portable core registry/log delegation with no Paper capability.
  - Guidance: This is a portable-code violation: portable core registry/log delegation with no Paper capability. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the matching core concept owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 831](src/paper/src/main/java/com/notquests/paper/adapter/PaperNotQuestsAdapter.java#L831) — `variableFields(String variableName)` in `PaperNotQuestsAdapter`
  - Why it violates the boundary: Portable core registry/log delegation with no Paper capability.
  - Guidance: This is a portable-code violation: portable core registry/log delegation with no Paper capability. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the matching core concept owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 836](src/paper/src/main/java/com/notquests/paper/adapter/PaperNotQuestsAdapter.java#L836) — `variableValue(String variableName, PlatformPlayer questPlayer, Object[] objects)` in `PaperNotQuestsAdapter`
  - Why it violates the boundary: Portable core registry/log delegation with no Paper capability.
  - Guidance: This is a portable-code violation: portable core registry/log delegation with no Paper capability. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the matching core concept owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 844](src/paper/src/main/java/com/notquests/paper/adapter/PaperNotQuestsAdapter.java#L844) — `variableValue(String variableName, PlatformPlayer questPlayer, Map<String, String> stringArguments, Map<String, ?> numberArguments, Map<String, ?> booleanArguments)` in `PaperNotQuestsAdapter`
  - Why it violates the boundary: Portable core registry/log delegation with no Paper capability.
  - Guidance: This is a portable-code violation: portable core registry/log delegation with no Paper capability. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the matching core concept owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 854](src/paper/src/main/java/com/notquests/paper/adapter/PaperNotQuestsAdapter.java#L854) — `warn(String message)` in `PaperNotQuestsAdapter`
  - Why it violates the boundary: Portable core registry/log delegation with no Paper capability.
  - Guidance: This is a portable-code violation: portable core registry/log delegation with no Paper capability. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the matching core concept owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 859](src/paper/src/main/java/com/notquests/paper/adapter/PaperNotQuestsAdapter.java#L859) — `logInfo(String message)` in `PaperNotQuestsAdapter`
  - Why it violates the boundary: Portable core registry/log delegation with no Paper capability.
  - Guidance: This is a portable-code violation: portable core registry/log delegation with no Paper capability. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the matching core concept owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 1131](src/paper/src/main/java/com/notquests/paper/adapter/PaperNotQuestsAdapter.java#L1131) — `untypedStatistic(String statisticId)` in `PaperNotQuestsAdapter`
  - Why it violates the boundary: Combines platform lookup with validation and shared warning-text policy.
  - Guidance: This is a portable-code violation: combines platform lookup with validation and shared warning-text policy. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core text, translation, or feature owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 1147](src/paper/src/main/java/com/notquests/paper/adapter/PaperNotQuestsAdapter.java#L1147) — `advancement(String advancementId)` in `PaperNotQuestsAdapter`
  - Why it violates the boundary: Combines platform lookup with validation and shared warning-text policy.
  - Guidance: This is a portable-code violation: combines platform lookup with validation and shared warning-text policy. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core text, translation, or feature owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 1163](src/paper/src/main/java/com/notquests/paper/adapter/PaperNotQuestsAdapter.java#L1163) — `blockMaterialFromValue(PlatformPlayer questPlayer, String materialOrKeyword)` in `PaperNotQuestsAdapter`
  - Why it violates the boundary: Owns hand/any/saved-item selection and randomness policy.
  - Guidance: This is a portable-code violation: owns hand/any/saved-item selection and randomness policy. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the core Items/SavedItems or objective owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 1218](src/paper/src/main/java/com/notquests/paper/adapter/PaperNotQuestsAdapter.java#L1218) — `resolveActionText(Actions.Data action, PlatformPlayer questPlayer, String text, Object[] objects)` in `PaperNotQuestsAdapter`
  - Why it violates the boundary: Shared action/objective text resolution belongs in core; no Paper operation occurs.
  - Guidance: This is a portable-code violation: shared action/objective text resolution belongs in core; no Paper operation occurs. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into core QuestPlayer, ActiveObjectives, or the concrete quest/objective feature owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 1227](src/paper/src/main/java/com/notquests/paper/adapter/PaperNotQuestsAdapter.java#L1227) — `objectiveTaskText(String translationKey, PlatformPlayer questPlayer, ActiveObjective activeObjective, java.util.Map<String, String> replacements)` in `PaperNotQuestsAdapter`
  - Why it violates the boundary: Shared action/objective text resolution belongs in core; no Paper operation occurs.
  - Guidance: This is a portable-code violation: shared action/objective text resolution belongs in core; no Paper operation occurs. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into core QuestPlayer, ActiveObjectives, or the concrete quest/objective feature owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 1240](src/paper/src/main/java/com/notquests/paper/adapter/PaperNotQuestsAdapter.java#L1240) — `trimSlash(String command)` in `PaperNotQuestsAdapter`
  - Why it violates the boundary: Portable command normalization/validation and error text.
  - Guidance: This is a portable-code violation: portable command normalization/validation and error text. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core command definition or the core command framework—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 1244](src/paper/src/main/java/com/notquests/paper/adapter/PaperNotQuestsAdapter.java#L1244) — `requireText(String value, String label)` in `PaperNotQuestsAdapter`
  - Why it violates the boundary: Portable command normalization/validation and error text.
  - Guidance: This is a portable-code violation: portable command normalization/validation and error text. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core command definition or the core command framework—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

#### [`src/paper/src/main/java/com/notquests/paper/adapter/config/BukkitConfigurationValueCodec.java`](src/paper/src/main/java/com/notquests/paper/adapter/config/BukkitConfigurationValueCodec.java)

- [x] **V2** [line 175](src/paper/src/main/java/com/notquests/paper/adapter/config/BukkitConfigurationValueCodec.java#L175) — `number(Object value, Number fallback)` in `BukkitConfigurationValueCodec`
  - Why it violates the boundary: Portable number parsing/fallback logic; no Paper capability is involved.
  - Guidance: This is a portable-code violation: portable number parsing/fallback logic; no Paper capability is involved. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the matching core concept owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

#### [`src/paper/src/main/java/com/notquests/paper/adapter/objectives/support/ItemObjectiveSupport.java`](src/paper/src/main/java/com/notquests/paper/adapter/objectives/support/ItemObjectiveSupport.java)

- [x] **V2** [line 18](src/paper/src/main/java/com/notquests/paper/adapter/objectives/support/ItemObjectiveSupport.java#L18) — `takenResultAmount(NotQuests main, Player player, ItemStack currentItem, ItemStack cursor, ClickType click, int hotbarButton)` in `ItemObjectiveSupport`
  - Why it violates the boundary: Combines event translation with item-objective progress calculation and policy.
  - Guidance: This is a portable-code violation: combines event translation with item-objective progress calculation and policy. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the core Items/SavedItems or objective owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 34](src/paper/src/main/java/com/notquests/paper/adapter/objectives/support/ItemObjectiveSupport.java#L34) — `craftAmount(NotQuests main, ItemStack result, ItemStack cursor, ClickType click, HumanEntity whoClicked, int hotbarButton, CraftingInventory craftingInventory, InventoryView inventoryView, PaperPlayer questPlayer)` in `ItemObjectiveSupport`
  - Why it violates the boundary: Combines event translation with item-objective progress calculation and policy.
  - Guidance: This is a portable-code violation: combines event translation with item-objective progress calculation and policy. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the core Items/SavedItems or objective owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 58](src/paper/src/main/java/com/notquests/paper/adapter/objectives/support/ItemObjectiveSupport.java#L58) — `inventorySpaceLeftForItem(Inventory inventory, ItemStack item)` in `ItemObjectiveSupport`
  - Why it violates the boundary: Portable inventory/crafting gameplay calculation or state normalization.
  - Guidance: This is a portable-code violation: portable inventory/crafting gameplay calculation or state normalization. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the core Items/SavedItems or objective owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 66](src/paper/src/main/java/com/notquests/paper/adapter/objectives/support/ItemObjectiveSupport.java#L66) — `maxCraftAmount(CraftingInventory inventory)` in `ItemObjectiveSupport`
  - Why it violates the boundary: Portable inventory/crafting gameplay calculation or state normalization.
  - Guidance: This is a portable-code violation: portable inventory/crafting gameplay calculation or state normalization. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the core Items/SavedItems or objective owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 79](src/paper/src/main/java/com/notquests/paper/adapter/objectives/support/ItemObjectiveSupport.java#L79) — `fits(ItemStack stack, Inventory inventory)` in `ItemObjectiveSupport`
  - Why it violates the boundary: Portable inventory/crafting gameplay calculation or state normalization.
  - Guidance: This is a portable-code violation: portable inventory/crafting gameplay calculation or state normalization. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the core Items/SavedItems or objective owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 138](src/paper/src/main/java/com/notquests/paper/adapter/objectives/support/ItemObjectiveSupport.java#L138) — `takenResultAmount(ItemSnapshot result, ItemSnapshot cursor, Click click, boolean hotbarSlotOccupied, boolean offhandOccupied, int inventorySpaceLeftForResult)` in `ItemObjectiveSupport`
  - Why it violates the boundary: Portable inventory/crafting gameplay calculation or state normalization.
  - Guidance: This is a portable-code violation: portable inventory/crafting gameplay calculation or state normalization. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the core Items/SavedItems or objective owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 195](src/paper/src/main/java/com/notquests/paper/adapter/objectives/support/ItemObjectiveSupport.java#L195) — `craftAmount(ItemSnapshot result, ItemSnapshot cursor, Click click, boolean hotbarSlotOccupied, boolean offhandOccupied, int maxCraftable, int resultInventoryCapacity)` in `ItemObjectiveSupport`
  - Why it violates the boundary: Portable inventory/crafting gameplay calculation or state normalization.
  - Guidance: This is a portable-code violation: portable inventory/crafting gameplay calculation or state normalization. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the core Items/SavedItems or objective owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 242](src/paper/src/main/java/com/notquests/paper/adapter/objectives/support/ItemObjectiveSupport.java#L242) — `inventorySpaceLeftForTarget(List<ItemSnapshot> inventory, ItemSnapshot target)` in `ItemObjectiveSupport`
  - Why it violates the boundary: Portable inventory/crafting gameplay calculation or state normalization.
  - Guidance: This is a portable-code violation: portable inventory/crafting gameplay calculation or state normalization. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the core Items/SavedItems or objective owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 254](src/paper/src/main/java/com/notquests/paper/adapter/objectives/support/ItemObjectiveSupport.java#L254) — `maxCraftAmount(int resultAmount, List<Integer> ingredientAmounts)` in `ItemObjectiveSupport`
  - Why it violates the boundary: Portable inventory/crafting gameplay calculation or state normalization.
  - Guidance: This is a portable-code violation: portable inventory/crafting gameplay calculation or state normalization. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the core Items/SavedItems or objective owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 265](src/paper/src/main/java/com/notquests/paper/adapter/objectives/support/ItemObjectiveSupport.java#L265) — `ItemSnapshot(int amount, int maxStackSize, boolean empty, boolean similarToTarget)` in `ItemObjectiveSupport.ItemSnapshot`
  - Why it violates the boundary: Portable inventory/crafting gameplay calculation or state normalization.
  - Guidance: This is a portable-code violation: portable inventory/crafting gameplay calculation or state normalization. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the core Items/SavedItems or objective owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 270](src/paper/src/main/java/com/notquests/paper/adapter/objectives/support/ItemObjectiveSupport.java#L270) — `empty(int targetMaxStackSize)` in `ItemObjectiveSupport.ItemSnapshot`
  - Why it violates the boundary: Portable inventory/crafting gameplay calculation or state normalization.
  - Guidance: This is a portable-code violation: portable inventory/crafting gameplay calculation or state normalization. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the core Items/SavedItems or objective owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 274](src/paper/src/main/java/com/notquests/paper/adapter/objectives/support/ItemObjectiveSupport.java#L274) — `target(int amount, int maxStackSize)` in `ItemObjectiveSupport.ItemSnapshot`
  - Why it violates the boundary: Portable inventory/crafting gameplay calculation or state normalization.
  - Guidance: This is a portable-code violation: portable inventory/crafting gameplay calculation or state normalization. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the core Items/SavedItems or objective owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

#### [`src/paper/src/main/java/com/notquests/paper/adapter/quest/PaperPlayer.java`](src/paper/src/main/java/com/notquests/paper/adapter/quest/PaperPlayer.java)

- [x] **V2** [line 161](src/paper/src/main/java/com/notquests/paper/adapter/quest/PaperPlayer.java#L161) — `updateLocationCompass(Player player)` in `PaperPlayer`
  - Why it violates the boundary: Chooses the tracked marker/label and requests display policy instead of only rendering it.
  - Guidance: This is a portable-code violation: chooses the tracked marker/label and requests display policy instead of only rendering it. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into core QuestPlayer, ActiveObjectives, or the concrete quest/objective feature owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 304](src/paper/src/main/java/com/notquests/paper/adapter/quest/PaperPlayer.java#L304) — `beamRenderLocation(Location markerLocation)` in `PaperPlayer`
  - Why it violates the boundary: Contains reusable beam-placement policy rather than a direct Bukkit conversion.
  - Guidance: This is a portable-code violation: contains reusable beam-placement policy rather than a direct Bukkit conversion. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into core QuestPlayer, ActiveObjectives, or the concrete quest/objective feature owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 382](src/paper/src/main/java/com/notquests/paper/adapter/quest/PaperPlayer.java#L382) — `isBeaconMode()` in `PaperPlayer`
  - Why it violates the boundary: Reads shared display policy or parses shared marker naming/fallback text.
  - Guidance: This is a portable-code violation: reads shared display policy or parses shared marker naming/fallback text. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into core QuestPlayer, ActiveObjectives, or the concrete quest/objective feature owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 386](src/paper/src/main/java/com/notquests/paper/adapter/quest/PaperPlayer.java#L386) — `beamLabel(String beamName)` in `PaperPlayer`
  - Why it violates the boundary: Reads shared display policy or parses shared marker naming/fallback text.
  - Guidance: This is a portable-code violation: reads shared display policy or parses shared marker naming/fallback text. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into core QuestPlayer, ActiveObjectives, or the concrete quest/objective feature owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 410](src/paper/src/main/java/com/notquests/paper/adapter/quest/PaperPlayer.java#L410) — `sendDebugMessage(String message, Object[] interpolatedStrings)` in `PaperPlayer`
  - Why it violates the boundary: Owns debug-player policy and shared debug-message formatting before sending.
  - Guidance: This is a portable-code violation: owns debug-player policy and shared debug-message formatting before sending. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core text, translation, or feature owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 1453](src/paper/src/main/java/com/notquests/paper/adapter/quest/PaperPlayer.java#L1453) — `afterQuestFailed(String questIdentifier)` in `PaperPlayer`
  - Why it violates the boundary: Scans core objective state and decides integration-specific quest-failure cleanup.
  - Guidance: This is a portable-code violation: scans core objective state and decides integration-specific quest-failure cleanup. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into core QuestPlayer, ActiveObjectives, or the concrete quest/objective feature owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 1487](src/paper/src/main/java/com/notquests/paper/adapter/quest/PaperPlayer.java#L1487) — `spawnParticle(String particleId, int count, boolean usePlayerLocation, boolean showToEveryone, NQLocation location, double offsetX, double offsetY, double offsetZ, double speed)` in `PaperPlayer`
  - Why it violates the boundary: Combines platform effects with validation, audience/location/type policy, and shared warnings.
  - Guidance: This is a portable-code violation: combines platform effects with validation, audience/location/type policy, and shared warnings. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the matching core concept owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 1529](src/paper/src/main/java/com/notquests/paper/adapter/quest/PaperPlayer.java#L1529) — `playSound(String soundId, boolean stopOtherSounds, boolean playForEveryoneAtSetLocation, boolean playForEveryoneAtTheirLocation, String worldName, Double x, Double y, Double z, Double volume, Double pitch, String soundCategory)` in `PaperPlayer`
  - Why it violates the boundary: Combines platform effects with validation, audience/location/type policy, and shared warnings.
  - Guidance: This is a portable-code violation: combines platform effects with validation, audience/location/type policy, and shared warnings. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the matching core concept owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 1587](src/paper/src/main/java/com/notquests/paper/adapter/quest/PaperPlayer.java#L1587) — `spawnMob(String entityTypeName, int amount, boolean usePlayerLocation, NQLocation location, int radiusX, int radiusY, int radiusZ)` in `PaperPlayer`
  - Why it violates the boundary: Combines platform effects with validation, audience/location/type policy, and shared warnings.
  - Guidance: This is a portable-code violation: combines platform effects with validation, audience/location/type policy, and shared warnings. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the matching core concept owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 1735](src/paper/src/main/java/com/notquests/paper/adapter/quest/PaperPlayer.java#L1735) — `callOnServerThread(String operation, java.util.concurrent.Callable<Boolean> action)` in `PaperPlayer`
  - Why it violates the boundary: Owns cross-cutting operation error-message policy around the thread bridge.
  - Guidance: This is a portable-code violation: owns cross-cutting operation error-message policy around the thread bridge. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core text, translation, or feature owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 1746](src/paper/src/main/java/com/notquests/paper/adapter/quest/PaperPlayer.java#L1746) — `lowerCase(String value)` in `PaperPlayer`
  - Why it violates the boundary: Portable string normalization helper; no Paper capability is involved.
  - Guidance: This is a portable-code violation: portable string normalization helper; no Paper capability is involved. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the matching core concept owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 1781](src/paper/src/main/java/com/notquests/paper/adapter/quest/PaperPlayer.java#L1781) — `randomLocation(Location baseLocation, int radiusX, int radiusY, int radiusZ)` in `PaperPlayer`
  - Why it violates the boundary: Portable random-spawn placement policy.
  - Guidance: This is a portable-code violation: portable random-spawn placement policy. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the matching core concept owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 1789](src/paper/src/main/java/com/notquests/paper/adapter/quest/PaperPlayer.java#L1789) — `randomOffset(int radius)` in `PaperPlayer`
  - Why it violates the boundary: Portable random-spawn placement policy.
  - Guidance: This is a portable-code violation: portable random-spawn placement policy. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the matching core concept owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 1804](src/paper/src/main/java/com/notquests/paper/adapter/quest/PaperPlayer.java#L1804) — `markerPoint(Point player, Point target, boolean beaconMode, int highestBlockYAtProjectedColumn)` in `PaperPlayer.ObjectiveBeamPlacement`
  - Why it violates the boundary: Portable objective-beam geometry/math embedded in the Paper adapter.
  - Guidance: This is a portable-code violation: portable objective-beam geometry/math embedded in the Paper adapter. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into core QuestPlayer, ActiveObjectives, or the concrete quest/objective feature owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 1827](src/paper/src/main/java/com/notquests/paper/adapter/quest/PaperPlayer.java#L1827) — `blockPoint(Point point)` in `PaperPlayer.ObjectiveBeamPlacement`
  - Why it violates the boundary: Portable objective-beam geometry/math embedded in the Paper adapter.
  - Guidance: This is a portable-code violation: portable objective-beam geometry/math embedded in the Paper adapter. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into core QuestPlayer, ActiveObjectives, or the concrete quest/objective feature owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 1831](src/paper/src/main/java/com/notquests/paper/adapter/quest/PaperPlayer.java#L1831) — `renderBlock(BlockPoint markerBlock, boolean markerBlockIsAir, int minWorldHeight)` in `PaperPlayer.ObjectiveBeamPlacement`
  - Why it violates the boundary: Portable objective-beam geometry/math embedded in the Paper adapter.
  - Guidance: This is a portable-code violation: portable objective-beam geometry/math embedded in the Paper adapter. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into core QuestPlayer, ActiveObjectives, or the concrete quest/objective feature owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 1844](src/paper/src/main/java/com/notquests/paper/adapter/quest/PaperPlayer.java#L1844) — `distanceSquared(Point first, Point second)` in `PaperPlayer.ObjectiveBeamPlacement`
  - Why it violates the boundary: Portable objective-beam geometry/math embedded in the Paper adapter.
  - Guidance: This is a portable-code violation: portable objective-beam geometry/math embedded in the Paper adapter. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into core QuestPlayer, ActiveObjectives, or the concrete quest/objective feature owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 1851](src/paper/src/main/java/com/notquests/paper/adapter/quest/PaperPlayer.java#L1851) — `block(double value)` in `PaperPlayer.ObjectiveBeamPlacement`
  - Why it violates the boundary: Portable objective-beam geometry/math embedded in the Paper adapter.
  - Guidance: This is a portable-code violation: portable objective-beam geometry/math embedded in the Paper adapter. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into core QuestPlayer, ActiveObjectives, or the concrete quest/objective feature owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

#### [`src/paper/src/main/java/com/notquests/paper/commands/arguments/EnchantmentArgument.java`](src/paper/src/main/java/com/notquests/paper/commands/arguments/EnchantmentArgument.java)

- [x] **V2** [line 24](src/paper/src/main/java/com/notquests/paper/commands/arguments/EnchantmentArgument.java#L24) — `valueTypeName()` in `EnchantmentArgument`
  - Why it violates the boundary: Owns command metadata or validation/error text rather than direct translation only.
  - Guidance: This is a portable-code violation: owns command metadata or validation/error text rather than direct translation only. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core command definition or the core command framework—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 29](src/paper/src/main/java/com/notquests/paper/commands/arguments/EnchantmentArgument.java#L29) — `convert(String input)` in `EnchantmentArgument`
  - Why it violates the boundary: Owns command metadata or validation/error text rather than direct translation only.
  - Guidance: This is a portable-code violation: owns command metadata or validation/error text rather than direct translation only. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core command definition or the core command framework—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

#### [`src/paper/src/main/java/com/notquests/paper/commands/arguments/EntityTypeArgument.java`](src/paper/src/main/java/com/notquests/paper/commands/arguments/EntityTypeArgument.java)

- [x] **V2** [line 32](src/paper/src/main/java/com/notquests/paper/commands/arguments/EntityTypeArgument.java#L32) — `valueTypeName()` in `EntityTypeArgument`
  - Why it violates the boundary: Owns command metadata, validation, integration branching, or suggestion policy.
  - Guidance: This is a portable-code violation: owns command metadata, validation, integration branching, or suggestion policy. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core command definition or the core command framework—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 37](src/paper/src/main/java/com/notquests/paper/commands/arguments/EntityTypeArgument.java#L37) — `convert(String input)` in `EntityTypeArgument`
  - Why it violates the boundary: Owns command metadata, validation, integration branching, or suggestion policy.
  - Guidance: This is a portable-code violation: owns command metadata, validation, integration branching, or suggestion policy. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core command definition or the core command framework—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 55](src/paper/src/main/java/com/notquests/paper/commands/arguments/EntityTypeArgument.java#L55) — `suggest(CommandContext<?> context, String remaining)` in `EntityTypeArgument`
  - Why it violates the boundary: Owns command metadata, validation, integration branching, or suggestion policy.
  - Guidance: This is a portable-code violation: owns command metadata, validation, integration branching, or suggestion policy. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core command definition or the core command framework—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

#### [`src/paper/src/main/java/com/notquests/paper/commands/arguments/ItemStackSelectionArgument.java`](src/paper/src/main/java/com/notquests/paper/commands/arguments/ItemStackSelectionArgument.java)

- [x] **V2** [line 37](src/paper/src/main/java/com/notquests/paper/commands/arguments/ItemStackSelectionArgument.java#L37) — `valueTypeName()` in `ItemStackSelectionArgument`
  - Why it violates the boundary: Owns portable token parsing, command validation/messages, keywords, or suggestion policy.
  - Guidance: This is a portable-code violation: owns portable token parsing, command validation/messages, keywords, or suggestion policy. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core command definition or the core command framework—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 48](src/paper/src/main/java/com/notquests/paper/commands/arguments/ItemStackSelectionArgument.java#L48) — `parse(StringReader reader, S source)` in `ItemStackSelectionArgument`
  - Why it violates the boundary: Owns portable token parsing, command validation/messages, keywords, or suggestion policy.
  - Guidance: This is a portable-code violation: owns portable token parsing, command validation/messages, keywords, or suggestion policy. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core command definition or the core command framework—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 52](src/paper/src/main/java/com/notquests/paper/commands/arguments/ItemStackSelectionArgument.java#L52) — `parse(StringReader reader)` in `ItemStackSelectionArgument`
  - Why it violates the boundary: Owns portable token parsing, command validation/messages, keywords, or suggestion policy.
  - Guidance: This is a portable-code violation: owns portable token parsing, command validation/messages, keywords, or suggestion policy. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core command definition or the core command framework—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 57](src/paper/src/main/java/com/notquests/paper/commands/arguments/ItemStackSelectionArgument.java#L57) — `readSelectionToken(StringReader reader)` in `ItemStackSelectionArgument`
  - Why it violates the boundary: Owns portable token parsing, command validation/messages, keywords, or suggestion policy.
  - Guidance: This is a portable-code violation: owns portable token parsing, command validation/messages, keywords, or suggestion policy. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core command definition or the core command framework—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 68](src/paper/src/main/java/com/notquests/paper/commands/arguments/ItemStackSelectionArgument.java#L68) — `convert(String input)` in `ItemStackSelectionArgument`
  - Why it violates the boundary: Owns portable token parsing, command validation/messages, keywords, or suggestion policy.
  - Guidance: This is a portable-code violation: owns portable token parsing, command validation/messages, keywords, or suggestion policy. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core command definition or the core command framework—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 74](src/paper/src/main/java/com/notquests/paper/commands/arguments/ItemStackSelectionArgument.java#L74) — `convert(String input, S source)` in `ItemStackSelectionArgument`
  - Why it violates the boundary: Owns portable token parsing, command validation/messages, keywords, or suggestion policy.
  - Guidance: This is a portable-code violation: owns portable token parsing, command validation/messages, keywords, or suggestion policy. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core command definition or the core command framework—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 119](src/paper/src/main/java/com/notquests/paper/commands/arguments/ItemStackSelectionArgument.java#L119) — `materialSuggestions()` in `ItemStackSelectionArgument`
  - Why it violates the boundary: Owns portable token parsing, command validation/messages, keywords, or suggestion policy.
  - Guidance: This is a portable-code violation: owns portable token parsing, command validation/messages, keywords, or suggestion policy. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core command definition or the core command framework—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 138](src/paper/src/main/java/com/notquests/paper/commands/arguments/ItemStackSelectionArgument.java#L138) — `suggest(CommandContext<?> context, String remaining)` in `ItemStackSelectionArgument`
  - Why it violates the boundary: Owns portable token parsing, command validation/messages, keywords, or suggestion policy.
  - Guidance: This is a portable-code violation: owns portable token parsing, command validation/messages, keywords, or suggestion policy. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core command definition or the core command framework—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

#### [`src/paper/src/main/java/com/notquests/paper/commands/arguments/LocationArgument.java`](src/paper/src/main/java/com/notquests/paper/commands/arguments/LocationArgument.java)

- [x] **V2** [line 25](src/paper/src/main/java/com/notquests/paper/commands/arguments/LocationArgument.java#L25) — `valueTypeName()` in `LocationArgument`
  - Why it violates the boundary: Owns portable location syntax, validation, or user-facing command text.
  - Guidance: This is a portable-code violation: owns portable location syntax, validation, or user-facing command text. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core command definition or the core command framework—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 30](src/paper/src/main/java/com/notquests/paper/commands/arguments/LocationArgument.java#L30) — `convert(String input)` in `LocationArgument`
  - Why it violates the boundary: Owns portable location syntax, validation, or user-facing command text.
  - Guidance: This is a portable-code violation: owns portable location syntax, validation, or user-facing command text. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core command definition or the core command framework—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

#### [`src/paper/src/main/java/com/notquests/paper/commands/arguments/NQNPCArgument.java`](src/paper/src/main/java/com/notquests/paper/commands/arguments/NQNPCArgument.java)

- [x] **V2** [line 41](src/paper/src/main/java/com/notquests/paper/commands/arguments/NQNPCArgument.java#L41) — `valueTypeName()` in `NQNPCArgument`
  - Why it violates the boundary: Owns NPC command metadata, selector validation/lookup, result policy, or suggestions.
  - Guidance: This is a portable-code violation: owns NPC command metadata, selector validation/lookup, result policy, or suggestions. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core command definition or the core command framework—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 51](src/paper/src/main/java/com/notquests/paper/commands/arguments/NQNPCArgument.java#L51) — `convert(String input)` in `NQNPCArgument`
  - Why it violates the boundary: Owns NPC command metadata, selector validation/lookup, result policy, or suggestions.
  - Guidance: This is a portable-code violation: owns NPC command metadata, selector validation/lookup, result policy, or suggestions. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core command definition or the core command framework—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 88](src/paper/src/main/java/com/notquests/paper/commands/arguments/NQNPCArgument.java#L88) — `suggest(CommandContext<?> context, String remaining)` in `NQNPCArgument`
  - Why it violates the boundary: Owns NPC command metadata, selector validation/lookup, result policy, or suggestions.
  - Guidance: This is a portable-code violation: owns NPC command metadata, selector validation/lookup, result policy, or suggestions. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core command definition or the core command framework—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

#### [`src/paper/src/main/java/com/notquests/paper/commands/arguments/variables/BooleanVariableArgument.java`](src/paper/src/main/java/com/notquests/paper/commands/arguments/variables/BooleanVariableArgument.java)

- [x] **V2** [line 48](src/paper/src/main/java/com/notquests/paper/commands/arguments/variables/BooleanVariableArgument.java#L48) — `valueTypeName()` in `BooleanVariableArgument`
  - Why it violates the boundary: Owns shared command metadata or portable suggestion examples.
  - Guidance: This is a portable-code violation: owns shared command metadata or portable suggestion examples. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core command definition or the core command framework—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 66](src/paper/src/main/java/com/notquests/paper/commands/arguments/variables/BooleanVariableArgument.java#L66) — `suggest(CommandContext<?> context, String remaining)` in `BooleanVariableArgument`
  - Why it violates the boundary: Owns shared command metadata or portable suggestion examples.
  - Guidance: This is a portable-code violation: owns shared command metadata or portable suggestion examples. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core command definition or the core command framework—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

#### [`src/paper/src/main/java/com/notquests/paper/commands/arguments/variables/NumberVariableArgument.java`](src/paper/src/main/java/com/notquests/paper/commands/arguments/variables/NumberVariableArgument.java)

- [x] **V2** [line 49](src/paper/src/main/java/com/notquests/paper/commands/arguments/variables/NumberVariableArgument.java#L49) — `valueTypeName()` in `NumberVariableArgument`
  - Why it violates the boundary: Owns shared command metadata or portable suggestion examples.
  - Guidance: This is a portable-code violation: owns shared command metadata or portable suggestion examples. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core command definition or the core command framework—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 68](src/paper/src/main/java/com/notquests/paper/commands/arguments/variables/NumberVariableArgument.java#L68) — `suggest(CommandContext<?> context, String remaining)` in `NumberVariableArgument`
  - Why it violates the boundary: Owns shared command metadata or portable suggestion examples.
  - Guidance: This is a portable-code violation: owns shared command metadata or portable suggestion examples. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core command definition or the core command framework—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

#### [`src/paper/src/main/java/com/notquests/paper/commands/arguments/wrappers/NQNPCResult.java`](src/paper/src/main/java/com/notquests/paper/commands/arguments/wrappers/NQNPCResult.java)

- [x] **V2** [line 11](src/paper/src/main/java/com/notquests/paper/commands/arguments/wrappers/NQNPCResult.java#L11) — `NQNPCResult(NQNPC nqnpc, boolean none, boolean rightClickSelect)` in `NQNPCResult`
  - Why it violates the boundary: Paper command-specific state wrapper; this is not an atomic platform capability.
  - Guidance: This is a portable-code violation: paper command-specific state wrapper; this is not an atomic platform capability. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core command definition or the core command framework—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 17](src/paper/src/main/java/com/notquests/paper/commands/arguments/wrappers/NQNPCResult.java#L17) — `getNQNPC()` in `NQNPCResult`
  - Why it violates the boundary: Paper command-specific state wrapper; this is not an atomic platform capability.
  - Guidance: This is a portable-code violation: paper command-specific state wrapper; this is not an atomic platform capability. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core command definition or the core command framework—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 21](src/paper/src/main/java/com/notquests/paper/commands/arguments/wrappers/NQNPCResult.java#L21) — `isNone()` in `NQNPCResult`
  - Why it violates the boundary: Paper command-specific state wrapper; this is not an atomic platform capability.
  - Guidance: This is a portable-code violation: paper command-specific state wrapper; this is not an atomic platform capability. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core command definition or the core command framework—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 25](src/paper/src/main/java/com/notquests/paper/commands/arguments/wrappers/NQNPCResult.java#L25) — `isRightClickSelect()` in `NQNPCResult`
  - Why it violates the boundary: Paper command-specific state wrapper; this is not an atomic platform capability.
  - Guidance: This is a portable-code violation: paper command-specific state wrapper; this is not an atomic platform capability. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core command definition or the core command framework—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

#### [`src/paper/src/main/java/com/notquests/paper/commands/brigadier/PaperArgumentType.java`](src/paper/src/main/java/com/notquests/paper/commands/brigadier/PaperArgumentType.java)

- [x] **V2** [line 57](src/paper/src/main/java/com/notquests/paper/commands/brigadier/PaperArgumentType.java#L57) — `valueTypeName()` in `PaperArgumentType`
  - Why it violates the boundary: Owns exported command metadata/name policy that belongs in core.
  - Guidance: This is a portable-code violation: owns exported command metadata/name policy that belongs in core. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core command definition or the core command framework—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 61](src/paper/src/main/java/com/notquests/paper/commands/brigadier/PaperArgumentType.java#L61) — `argumentTypeName()` in `PaperArgumentType`
  - Why it violates the boundary: Owns exported command metadata/name policy that belongs in core.
  - Guidance: This is a portable-code violation: owns exported command metadata/name policy that belongs in core. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core command definition or the core command framework—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 93](src/paper/src/main/java/com/notquests/paper/commands/brigadier/PaperArgumentType.java#L93) — `listSuggestions(CommandContext<S> context, SuggestionsBuilder builder)` in `PaperArgumentType`
  - Why it violates the boundary: Owns reusable prefix-filtering suggestion policy rather than only Brigadier translation.
  - Guidance: This is a portable-code violation: owns reusable prefix-filtering suggestion policy rather than only Brigadier translation. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core command definition or the core command framework—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

#### [`src/paper/src/main/java/com/notquests/paper/commands/brigadier/PaperArguments.java`](src/paper/src/main/java/com/notquests/paper/commands/brigadier/PaperArguments.java)

- [x] **V2** [line 23](src/paper/src/main/java/com/notquests/paper/commands/brigadier/PaperArguments.java#L23) — `PaperArguments()` in `PaperArguments`
  - Why it violates the boundary: Owns portable command parsing, validation/text, metadata, or suggestion policy.
  - Guidance: This is a portable-code violation: owns portable command parsing, validation/text, metadata, or suggestion policy. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core command definition or the core command framework—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 25](src/paper/src/main/java/com/notquests/paper/commands/brigadier/PaperArguments.java#L25) — `integerArgument()` in `PaperArguments`
  - Why it violates the boundary: Owns portable command parsing, validation/text, metadata, or suggestion policy.
  - Guidance: This is a portable-code violation: owns portable command parsing, validation/text, metadata, or suggestion policy. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core command definition or the core command framework—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 27](src/paper/src/main/java/com/notquests/paper/commands/brigadier/PaperArguments.java#L27) — `convert(String input)` in `PaperArguments.anonymous@26`
  - Why it violates the boundary: Owns portable command parsing, validation/text, metadata, or suggestion policy.
  - Guidance: This is a portable-code violation: owns portable command parsing, validation/text, metadata, or suggestion policy. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core command definition or the core command framework—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 36](src/paper/src/main/java/com/notquests/paper/commands/brigadier/PaperArguments.java#L36) — `suggest(CommandContext<?> context, String remaining)` in `PaperArguments.anonymous@26`
  - Why it violates the boundary: Owns portable command parsing, validation/text, metadata, or suggestion policy.
  - Guidance: This is a portable-code violation: owns portable command parsing, validation/text, metadata, or suggestion policy. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core command definition or the core command framework—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 41](src/paper/src/main/java/com/notquests/paper/commands/brigadier/PaperArguments.java#L41) — `valueTypeName()` in `PaperArguments.anonymous@26`
  - Why it violates the boundary: Owns portable command parsing, validation/text, metadata, or suggestion policy.
  - Guidance: This is a portable-code violation: owns portable command parsing, validation/text, metadata, or suggestion policy. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core command definition or the core command framework—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 48](src/paper/src/main/java/com/notquests/paper/commands/brigadier/PaperArguments.java#L48) — `doubleArgument()` in `PaperArguments`
  - Why it violates the boundary: Owns portable command parsing, validation/text, metadata, or suggestion policy.
  - Guidance: This is a portable-code violation: owns portable command parsing, validation/text, metadata, or suggestion policy. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core command definition or the core command framework—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 50](src/paper/src/main/java/com/notquests/paper/commands/brigadier/PaperArguments.java#L50) — `convert(String input)` in `PaperArguments.anonymous@49`
  - Why it violates the boundary: Owns portable command parsing, validation/text, metadata, or suggestion policy.
  - Guidance: This is a portable-code violation: owns portable command parsing, validation/text, metadata, or suggestion policy. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core command definition or the core command framework—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 59](src/paper/src/main/java/com/notquests/paper/commands/brigadier/PaperArguments.java#L59) — `suggest(CommandContext<?> context, String remaining)` in `PaperArguments.anonymous@49`
  - Why it violates the boundary: Owns portable command parsing, validation/text, metadata, or suggestion policy.
  - Guidance: This is a portable-code violation: owns portable command parsing, validation/text, metadata, or suggestion policy. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core command definition or the core command framework—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 64](src/paper/src/main/java/com/notquests/paper/commands/brigadier/PaperArguments.java#L64) — `valueTypeName()` in `PaperArguments.anonymous@49`
  - Why it violates the boundary: Owns portable command parsing, validation/text, metadata, or suggestion policy.
  - Guidance: This is a portable-code violation: owns portable command parsing, validation/text, metadata, or suggestion policy. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core command definition or the core command framework—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 71](src/paper/src/main/java/com/notquests/paper/commands/brigadier/PaperArguments.java#L71) — `longArgument()` in `PaperArguments`
  - Why it violates the boundary: Owns portable command parsing, validation/text, metadata, or suggestion policy.
  - Guidance: This is a portable-code violation: owns portable command parsing, validation/text, metadata, or suggestion policy. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core command definition or the core command framework—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 73](src/paper/src/main/java/com/notquests/paper/commands/brigadier/PaperArguments.java#L73) — `convert(String input)` in `PaperArguments.anonymous@72`
  - Why it violates the boundary: Owns portable command parsing, validation/text, metadata, or suggestion policy.
  - Guidance: This is a portable-code violation: owns portable command parsing, validation/text, metadata, or suggestion policy. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core command definition or the core command framework—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 82](src/paper/src/main/java/com/notquests/paper/commands/brigadier/PaperArguments.java#L82) — `valueTypeName()` in `PaperArguments.anonymous@72`
  - Why it violates the boundary: Owns portable command parsing, validation/text, metadata, or suggestion policy.
  - Guidance: This is a portable-code violation: owns portable command parsing, validation/text, metadata, or suggestion policy. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core command definition or the core command framework—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 89](src/paper/src/main/java/com/notquests/paper/commands/brigadier/PaperArguments.java#L89) — `booleanArgument()` in `PaperArguments`
  - Why it violates the boundary: Owns portable command parsing, validation/text, metadata, or suggestion policy.
  - Guidance: This is a portable-code violation: owns portable command parsing, validation/text, metadata, or suggestion policy. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core command definition or the core command framework—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 91](src/paper/src/main/java/com/notquests/paper/commands/brigadier/PaperArguments.java#L91) — `convert(String input)` in `PaperArguments.anonymous@90`
  - Why it violates the boundary: Owns portable command parsing, validation/text, metadata, or suggestion policy.
  - Guidance: This is a portable-code violation: owns portable command parsing, validation/text, metadata, or suggestion policy. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core command definition or the core command framework—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 101](src/paper/src/main/java/com/notquests/paper/commands/brigadier/PaperArguments.java#L101) — `suggest(CommandContext<?> context, String remaining)` in `PaperArguments.anonymous@90`
  - Why it violates the boundary: Owns portable command parsing, validation/text, metadata, or suggestion policy.
  - Guidance: This is a portable-code violation: owns portable command parsing, validation/text, metadata, or suggestion policy. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core command definition or the core command framework—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 106](src/paper/src/main/java/com/notquests/paper/commands/brigadier/PaperArguments.java#L106) — `valueTypeName()` in `PaperArguments.anonymous@90`
  - Why it violates the boundary: Owns portable command parsing, validation/text, metadata, or suggestion policy.
  - Guidance: This is a portable-code violation: owns portable command parsing, validation/text, metadata, or suggestion policy. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core command definition or the core command framework—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 113](src/paper/src/main/java/com/notquests/paper/commands/brigadier/PaperArguments.java#L113) — `stringArgument()` in `PaperArguments`
  - Why it violates the boundary: Owns portable command parsing, validation/text, metadata, or suggestion policy.
  - Guidance: This is a portable-code violation: owns portable command parsing, validation/text, metadata, or suggestion policy. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core command definition or the core command framework—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 115](src/paper/src/main/java/com/notquests/paper/commands/brigadier/PaperArguments.java#L115) — `convert(String input)` in `PaperArguments.anonymous@114`
  - Why it violates the boundary: Owns portable command parsing, validation/text, metadata, or suggestion policy.
  - Guidance: This is a portable-code violation: owns portable command parsing, validation/text, metadata, or suggestion policy. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core command definition or the core command framework—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 120](src/paper/src/main/java/com/notquests/paper/commands/brigadier/PaperArguments.java#L120) — `valueTypeName()` in `PaperArguments.anonymous@114`
  - Why it violates the boundary: Owns portable command parsing, validation/text, metadata, or suggestion policy.
  - Guidance: This is a portable-code violation: owns portable command parsing, validation/text, metadata, or suggestion policy. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core command definition or the core command framework—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 127](src/paper/src/main/java/com/notquests/paper/commands/brigadier/PaperArguments.java#L127) — `namedArgument(String valueTypeName, Supplier<List<String>> names)` in `PaperArguments`
  - Why it violates the boundary: Owns portable command parsing, validation/text, metadata, or suggestion policy.
  - Guidance: This is a portable-code violation: owns portable command parsing, validation/text, metadata, or suggestion policy. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core command definition or the core command framework—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 131](src/paper/src/main/java/com/notquests/paper/commands/brigadier/PaperArguments.java#L131) — `convert(String input)` in `PaperArguments.anonymous@130`
  - Why it violates the boundary: Owns portable command parsing, validation/text, metadata, or suggestion policy.
  - Guidance: This is a portable-code violation: owns portable command parsing, validation/text, metadata, or suggestion policy. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core command definition or the core command framework—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 142](src/paper/src/main/java/com/notquests/paper/commands/brigadier/PaperArguments.java#L142) — `suggest(CommandContext<?> context, String remaining)` in `PaperArguments.anonymous@130`
  - Why it violates the boundary: Owns portable command parsing, validation/text, metadata, or suggestion policy.
  - Guidance: This is a portable-code violation: owns portable command parsing, validation/text, metadata, or suggestion policy. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core command definition or the core command framework—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 147](src/paper/src/main/java/com/notquests/paper/commands/brigadier/PaperArguments.java#L147) — `valueTypeName()` in `PaperArguments.anonymous@130`
  - Why it violates the boundary: Owns portable command parsing, validation/text, metadata, or suggestion policy.
  - Guidance: This is a portable-code violation: owns portable command parsing, validation/text, metadata, or suggestion policy. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core command definition or the core command framework—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 152](src/paper/src/main/java/com/notquests/paper/commands/brigadier/PaperArguments.java#L152) — `safeNames()` in `PaperArguments.anonymous@130`
  - Why it violates the boundary: Owns portable command parsing, validation/text, metadata, or suggestion policy.
  - Guidance: This is a portable-code violation: owns portable command parsing, validation/text, metadata, or suggestion policy. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core command definition or the core command framework—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 160](src/paper/src/main/java/com/notquests/paper/commands/brigadier/PaperArguments.java#L160) — `greedyStringArgument()` in `PaperArguments`
  - Why it violates the boundary: Owns portable command parsing, validation/text, metadata, or suggestion policy.
  - Guidance: This is a portable-code violation: owns portable command parsing, validation/text, metadata, or suggestion policy. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core command definition or the core command framework—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 162](src/paper/src/main/java/com/notquests/paper/commands/brigadier/PaperArguments.java#L162) — `convert(String input)` in `PaperArguments.anonymous@161`
  - Why it violates the boundary: Owns portable command parsing, validation/text, metadata, or suggestion policy.
  - Guidance: This is a portable-code violation: owns portable command parsing, validation/text, metadata, or suggestion policy. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core command definition or the core command framework—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 172](src/paper/src/main/java/com/notquests/paper/commands/brigadier/PaperArguments.java#L172) — `valueTypeName()` in `PaperArguments.anonymous@161`
  - Why it violates the boundary: Owns portable command parsing, validation/text, metadata, or suggestion policy.
  - Guidance: This is a portable-code violation: owns portable command parsing, validation/text, metadata, or suggestion policy. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core command definition or the core command framework—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 179](src/paper/src/main/java/com/notquests/paper/commands/brigadier/PaperArguments.java#L179) — `stringArrayArgument()` in `PaperArguments`
  - Why it violates the boundary: Owns portable command parsing, validation/text, metadata, or suggestion policy.
  - Guidance: This is a portable-code violation: owns portable command parsing, validation/text, metadata, or suggestion policy. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core command definition or the core command framework—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 181](src/paper/src/main/java/com/notquests/paper/commands/brigadier/PaperArguments.java#L181) — `convert(String input)` in `PaperArguments.anonymous@180`
  - Why it violates the boundary: Owns portable command parsing, validation/text, metadata, or suggestion policy.
  - Guidance: This is a portable-code violation: owns portable command parsing, validation/text, metadata, or suggestion policy. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core command definition or the core command framework—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 191](src/paper/src/main/java/com/notquests/paper/commands/brigadier/PaperArguments.java#L191) — `valueTypeName()` in `PaperArguments.anonymous@180`
  - Why it violates the boundary: Owns portable command parsing, validation/text, metadata, or suggestion policy.
  - Guidance: This is a portable-code violation: owns portable command parsing, validation/text, metadata, or suggestion policy. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core command definition or the core command framework—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 199](src/paper/src/main/java/com/notquests/paper/commands/brigadier/PaperArguments.java#L199) — `durationArgument()` in `PaperArguments`
  - Why it violates the boundary: Owns portable command parsing, validation/text, metadata, or suggestion policy.
  - Guidance: This is a portable-code violation: owns portable command parsing, validation/text, metadata, or suggestion policy. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core command definition or the core command framework—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 201](src/paper/src/main/java/com/notquests/paper/commands/brigadier/PaperArguments.java#L201) — `convert(String input)` in `PaperArguments.anonymous@200`
  - Why it violates the boundary: Owns portable command parsing, validation/text, metadata, or suggestion policy.
  - Guidance: This is a portable-code violation: owns portable command parsing, validation/text, metadata, or suggestion policy. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core command definition or the core command framework—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 210](src/paper/src/main/java/com/notquests/paper/commands/brigadier/PaperArguments.java#L210) — `suggest(CommandContext<?> context, String remaining)` in `PaperArguments.anonymous@200`
  - Why it violates the boundary: Owns portable command parsing, validation/text, metadata, or suggestion policy.
  - Guidance: This is a portable-code violation: owns portable command parsing, validation/text, metadata, or suggestion policy. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core command definition or the core command framework—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 215](src/paper/src/main/java/com/notquests/paper/commands/brigadier/PaperArguments.java#L215) — `valueTypeName()` in `PaperArguments.anonymous@200`
  - Why it violates the boundary: Owns portable command parsing, validation/text, metadata, or suggestion policy.
  - Guidance: This is a portable-code violation: owns portable command parsing, validation/text, metadata, or suggestion policy. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core command definition or the core command framework—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 222](src/paper/src/main/java/com/notquests/paper/commands/brigadier/PaperArguments.java#L222) — `worldArgument()` in `PaperArguments`
  - Why it violates the boundary: Owns portable command parsing, validation/text, metadata, or suggestion policy.
  - Guidance: This is a portable-code violation: owns portable command parsing, validation/text, metadata, or suggestion policy. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core command definition or the core command framework—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 224](src/paper/src/main/java/com/notquests/paper/commands/brigadier/PaperArguments.java#L224) — `convert(String input)` in `PaperArguments.anonymous@223`
  - Why it violates the boundary: Owns portable command parsing, validation/text, metadata, or suggestion policy.
  - Guidance: This is a portable-code violation: owns portable command parsing, validation/text, metadata, or suggestion policy. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core command definition or the core command framework—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 233](src/paper/src/main/java/com/notquests/paper/commands/brigadier/PaperArguments.java#L233) — `suggest(CommandContext<?> context, String remaining)` in `PaperArguments.anonymous@223`
  - Why it violates the boundary: Owns portable command parsing, validation/text, metadata, or suggestion policy.
  - Guidance: This is a portable-code violation: owns portable command parsing, validation/text, metadata, or suggestion policy. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core command definition or the core command framework—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 242](src/paper/src/main/java/com/notquests/paper/commands/brigadier/PaperArguments.java#L242) — `valueTypeName()` in `PaperArguments.anonymous@223`
  - Why it violates the boundary: Owns portable command parsing, validation/text, metadata, or suggestion policy.
  - Guidance: This is a portable-code violation: owns portable command parsing, validation/text, metadata, or suggestion policy. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core command definition or the core command framework—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 249](src/paper/src/main/java/com/notquests/paper/commands/brigadier/PaperArguments.java#L249) — `playerArgument()` in `PaperArguments`
  - Why it violates the boundary: Owns portable command parsing, validation/text, metadata, or suggestion policy.
  - Guidance: This is a portable-code violation: owns portable command parsing, validation/text, metadata, or suggestion policy. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core command definition or the core command framework—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 251](src/paper/src/main/java/com/notquests/paper/commands/brigadier/PaperArguments.java#L251) — `convert(String input)` in `PaperArguments.anonymous@250`
  - Why it violates the boundary: Owns portable command parsing, validation/text, metadata, or suggestion policy.
  - Guidance: This is a portable-code violation: owns portable command parsing, validation/text, metadata, or suggestion policy. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core command definition or the core command framework—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 260](src/paper/src/main/java/com/notquests/paper/commands/brigadier/PaperArguments.java#L260) — `suggest(CommandContext<?> context, String remaining)` in `PaperArguments.anonymous@250`
  - Why it violates the boundary: Owns portable command parsing, validation/text, metadata, or suggestion policy.
  - Guidance: This is a portable-code violation: owns portable command parsing, validation/text, metadata, or suggestion policy. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core command definition or the core command framework—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 269](src/paper/src/main/java/com/notquests/paper/commands/brigadier/PaperArguments.java#L269) — `valueTypeName()` in `PaperArguments.anonymous@250`
  - Why it violates the boundary: Owns portable command parsing, validation/text, metadata, or suggestion policy.
  - Guidance: This is a portable-code violation: owns portable command parsing, validation/text, metadata, or suggestion policy. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core command definition or the core command framework—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 276](src/paper/src/main/java/com/notquests/paper/commands/brigadier/PaperArguments.java#L276) — `enumArgument(Class<E> type)` in `PaperArguments`
  - Why it violates the boundary: Owns portable command parsing, validation/text, metadata, or suggestion policy.
  - Guidance: This is a portable-code violation: owns portable command parsing, validation/text, metadata, or suggestion policy. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core command definition or the core command framework—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 278](src/paper/src/main/java/com/notquests/paper/commands/brigadier/PaperArguments.java#L278) — `convert(String input)` in `PaperArguments.anonymous@277`
  - Why it violates the boundary: Owns portable command parsing, validation/text, metadata, or suggestion policy.
  - Guidance: This is a portable-code violation: owns portable command parsing, validation/text, metadata, or suggestion policy. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core command definition or the core command framework—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 288](src/paper/src/main/java/com/notquests/paper/commands/brigadier/PaperArguments.java#L288) — `suggest(CommandContext<?> context, String remaining)` in `PaperArguments.anonymous@277`
  - Why it violates the boundary: Owns portable command parsing, validation/text, metadata, or suggestion policy.
  - Guidance: This is a portable-code violation: owns portable command parsing, validation/text, metadata, or suggestion policy. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core command definition or the core command framework—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 297](src/paper/src/main/java/com/notquests/paper/commands/brigadier/PaperArguments.java#L297) — `valueTypeName()` in `PaperArguments.anonymous@277`
  - Why it violates the boundary: Owns portable command parsing, validation/text, metadata, or suggestion policy.
  - Guidance: This is a portable-code violation: owns portable command parsing, validation/text, metadata, or suggestion policy. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core command definition or the core command framework—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 305](src/paper/src/main/java/com/notquests/paper/commands/brigadier/PaperArguments.java#L305) — `componentArgument(NotQuests main)` in `PaperArguments`
  - Why it violates the boundary: Owns portable command parsing, validation/text, metadata, or suggestion policy.
  - Guidance: This is a portable-code violation: owns portable command parsing, validation/text, metadata, or suggestion policy. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core command definition or the core command framework—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 317](src/paper/src/main/java/com/notquests/paper/commands/brigadier/PaperArguments.java#L317) — `valueTypeName()` in `PaperArguments.anonymous@306`
  - Why it violates the boundary: Owns portable command parsing, validation/text, metadata, or suggestion policy.
  - Guidance: This is a portable-code violation: owns portable command parsing, validation/text, metadata, or suggestion policy. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core command definition or the core command framework—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

#### [`src/paper/src/main/java/com/notquests/paper/commands/brigadier/PaperCoreCommandCompiler.java`](src/paper/src/main/java/com/notquests/paper/commands/brigadier/PaperCoreCommandCompiler.java)

- [x] **V2** [line 360](src/paper/src/main/java/com/notquests/paper/commands/brigadier/PaperCoreCommandCompiler.java#L360) — `flagSuggestions(NQCommandTree.Node<NQArgumentType, NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>, NQSuggestionProvider<NQCommandContext>, NQCommandHandler> node)` in `PaperCoreCommandCompiler`
  - Why it violates the boundary: Owns portable command-token/suggestion policy instead of only Brigadier translation.
  - Guidance: This is a portable-code violation: owns portable command-token/suggestion policy instead of only Brigadier translation. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core command definition or the core command framework—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 403](src/paper/src/main/java/com/notquests/paper/commands/brigadier/PaperCoreCommandCompiler.java#L403) — `awaitingFlagValue(List<NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>> flags, String[] tokens)` in `PaperCoreCommandCompiler`
  - Why it violates the boundary: Owns portable command-token/suggestion policy instead of only Brigadier translation.
  - Guidance: This is a portable-code violation: owns portable command-token/suggestion policy instead of only Brigadier translation. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core command definition or the core command framework—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 422](src/paper/src/main/java/com/notquests/paper/commands/brigadier/PaperCoreCommandCompiler.java#L422) — `currentTokenStart(String input)` in `PaperCoreCommandCompiler`
  - Why it violates the boundary: Owns portable command-token/suggestion policy instead of only Brigadier translation.
  - Guidance: This is a portable-code violation: owns portable command-token/suggestion policy instead of only Brigadier translation. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core command definition or the core command framework—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 437](src/paper/src/main/java/com/notquests/paper/commands/brigadier/PaperCoreCommandCompiler.java#L437) — `suggestArgument(NQArgumentType argument, List<String> candidates, SuggestionsBuilder suggestions)` in `PaperCoreCommandCompiler`
  - Why it violates the boundary: Owns portable command-token/suggestion policy instead of only Brigadier translation.
  - Guidance: This is a portable-code violation: owns portable command-token/suggestion policy instead of only Brigadier translation. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core command definition or the core command framework—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 450](src/paper/src/main/java/com/notquests/paper/commands/brigadier/PaperCoreCommandCompiler.java#L450) — `suggestCommaSeparated(List<String> candidates, SuggestionsBuilder suggestions)` in `PaperCoreCommandCompiler`
  - Why it violates the boundary: Owns portable command-token/suggestion policy instead of only Brigadier translation.
  - Guidance: This is a portable-code violation: owns portable command-token/suggestion policy instead of only Brigadier translation. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core command definition or the core command framework—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 467](src/paper/src/main/java/com/notquests/paper/commands/brigadier/PaperCoreCommandCompiler.java#L467) — `commaCandidates(List<String> candidates)` in `PaperCoreCommandCompiler`
  - Why it violates the boundary: Owns portable command-token/suggestion policy instead of only Brigadier translation.
  - Guidance: This is a portable-code violation: owns portable command-token/suggestion policy instead of only Brigadier translation. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core command definition or the core command framework—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 478](src/paper/src/main/java/com/notquests/paper/commands/brigadier/PaperCoreCommandCompiler.java#L478) — `suggestMatching(List<String> candidates, SuggestionsBuilder suggestions)` in `PaperCoreCommandCompiler`
  - Why it violates the boundary: Owns portable command-token/suggestion policy instead of only Brigadier translation.
  - Guidance: This is a portable-code violation: owns portable command-token/suggestion policy instead of only Brigadier translation. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core command definition or the core command framework—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 615](src/paper/src/main/java/com/notquests/paper/commands/brigadier/PaperCoreCommandCompiler.java#L615) — `append(List<NQCommandTree.Node<NQArgumentType, NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>, NQSuggestionProvider<NQCommandContext>, NQCommandHandler>> path, NQCommandTree.Node<NQArgumentType, NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>, NQSuggestionProvider<NQCommandContext>, NQCommandHandler> child)` in `PaperCoreCommandCompiler`
  - Why it violates the boundary: Portable command-tree path/alias manipulation with no Paper API requirement.
  - Guidance: This is a portable-code violation: portable command-tree path/alias manipulation with no Paper API requirement. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core command definition or the core command framework—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 644](src/paper/src/main/java/com/notquests/paper/commands/brigadier/PaperCoreCommandCompiler.java#L644) — `literalNames(NQCommandTree.Node<NQArgumentType, NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>, NQSuggestionProvider<NQCommandContext>, NQCommandHandler> node)` in `PaperCoreCommandCompiler`
  - Why it violates the boundary: Portable command-tree path/alias manipulation with no Paper API requirement.
  - Guidance: This is a portable-code violation: portable command-tree path/alias manipulation with no Paper API requirement. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core command definition or the core command framework—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 733](src/paper/src/main/java/com/notquests/paper/commands/brigadier/PaperCoreCommandCompiler.java#L733) — `Context(CommandContext<CommandSourceStack> context, String rawFlags, List<NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>> flags)` in `PaperCoreCommandCompiler.Context`
  - Why it violates the boundary: Parses command flags and owns context state rather than only adapting Brigadier values.
  - Guidance: This is a portable-code violation: parses command flags and owns context state rather than only adapting Brigadier values. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core command definition or the core command framework—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

#### [`src/paper/src/main/java/com/notquests/paper/conversation/PaperConversationDisplay.java`](src/paper/src/main/java/com/notquests/paper/conversation/PaperConversationDisplay.java)

- [x] **V2** [line 109](src/paper/src/main/java/com/notquests/paper/conversation/PaperConversationDisplay.java#L109) — `speaker(ConversationManager.DisplayLine line)` in `PaperConversationDisplay`
  - Why it violates the boundary: Reconstructs portable core Speaker data in Paper.
  - Guidance: This is a portable-code violation: reconstructs portable core Speaker data in Paper. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into core Conversations/chat-history ownership—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 120](src/paper/src/main/java/com/notquests/paper/conversation/PaperConversationDisplay.java#L120) — `removeOldMessages(Player player)` in `PaperConversationDisplay`
  - Why it violates the boundary: Owns previous-message deletion/replay policy instead of one render effect.
  - Guidance: This is a portable-code violation: owns previous-message deletion/replay policy instead of one render effect. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into core Conversations/chat-history ownership—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 131](src/paper/src/main/java/com/notquests/paper/conversation/PaperConversationDisplay.java#L131) — `speaker(ConversationManager.DisplayOption option)` in `PaperConversationDisplay`
  - Why it violates the boundary: Reconstructs portable core Speaker data in Paper.
  - Guidance: This is a portable-code violation: reconstructs portable core Speaker data in Paper. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into core Conversations/chat-history ownership—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

#### [`src/paper/src/main/java/com/notquests/paper/events/ArmorStandEvents.java`](src/paper/src/main/java/com/notquests/paper/events/ArmorStandEvents.java)

- [x] **V2** [line 33](src/paper/src/main/java/com/notquests/paper/events/ArmorStandEvents.java#L33) — `onArmorStandClick(PlayerInteractAtEntityEvent event)` in `ArmorStandEvents`
  - Why it violates the boundary: Implements an entire admin-tool command flow with validation, messages, and mutations.
  - Guidance: This is a portable-code violation: implements an entire admin-tool command flow with validation, messages, and mutations. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core command definition or the core command framework—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

#### [`src/paper/src/main/java/com/notquests/paper/events/InventoryEvents.java`](src/paper/src/main/java/com/notquests/paper/events/InventoryEvents.java)

- [x] **V2** [line 24](src/paper/src/main/java/com/notquests/paper/events/InventoryEvents.java#L24) — `isItemSlotWorld(String worldName)` in `InventoryEvents`
  - Why it violates the boundary: Portable enabled-world matching policy belongs in core.
  - Guidance: This is a portable-code violation: portable enabled-world matching policy belongs in core. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the matching core concept owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 41](src/paper/src/main/java/com/notquests/paper/events/InventoryEvents.java#L41) — `onPlayerJoin(PlayerJoinEvent e)` in `InventoryEvents`
  - Why it violates the boundary: Enforces shared journal availability/protection behavior in Paper.
  - Guidance: This is a portable-code violation: enforces shared journal availability/protection behavior in Paper. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the core journal/inventory behavior owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 51](src/paper/src/main/java/com/notquests/paper/events/InventoryEvents.java#L51) — `onPlayerItemInteract(PlayerInteractEvent event)` in `InventoryEvents`
  - Why it violates the boundary: Enforces shared journal availability/protection behavior in Paper.
  - Guidance: This is a portable-code violation: enforces shared journal availability/protection behavior in Paper. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the core journal/inventory behavior owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 62](src/paper/src/main/java/com/notquests/paper/events/InventoryEvents.java#L62) — `onPlayerRespawn(PlayerRespawnEvent e)` in `InventoryEvents`
  - Why it violates the boundary: Enforces shared journal availability/protection behavior in Paper.
  - Guidance: This is a portable-code violation: enforces shared journal availability/protection behavior in Paper. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the core journal/inventory behavior owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 73](src/paper/src/main/java/com/notquests/paper/events/InventoryEvents.java#L73) — `onDeath(EntityDeathEvent e)` in `InventoryEvents`
  - Why it violates the boundary: Enforces shared journal availability/protection behavior in Paper.
  - Guidance: This is a portable-code violation: enforces shared journal availability/protection behavior in Paper. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the core journal/inventory behavior owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 84](src/paper/src/main/java/com/notquests/paper/events/InventoryEvents.java#L84) — `onPickupItem(EntityPickupItemEvent e)` in `InventoryEvents`
  - Why it violates the boundary: Enforces shared journal availability/protection behavior in Paper.
  - Guidance: This is a portable-code violation: enforces shared journal availability/protection behavior in Paper. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the core journal/inventory behavior owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 97](src/paper/src/main/java/com/notquests/paper/events/InventoryEvents.java#L97) — `onDropItem(PlayerDropItemEvent e)` in `InventoryEvents`
  - Why it violates the boundary: Enforces shared journal availability/protection behavior in Paper.
  - Guidance: This is a portable-code violation: enforces shared journal availability/protection behavior in Paper. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the core journal/inventory behavior owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 109](src/paper/src/main/java/com/notquests/paper/events/InventoryEvents.java#L109) — `onInventoryUse(InventoryClickEvent e)` in `InventoryEvents`
  - Why it violates the boundary: Enforces shared journal availability/protection behavior in Paper.
  - Guidance: This is a portable-code violation: enforces shared journal availability/protection behavior in Paper. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the core journal/inventory behavior owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 134](src/paper/src/main/java/com/notquests/paper/events/InventoryEvents.java#L134) — `playerChangeWorldEvent(PlayerChangedWorldEvent e)` in `InventoryEvents`
  - Why it violates the boundary: Enforces shared journal availability/protection behavior in Paper.
  - Guidance: This is a portable-code violation: enforces shared journal availability/protection behavior in Paper. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the core journal/inventory behavior owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 143](src/paper/src/main/java/com/notquests/paper/events/InventoryEvents.java#L143) — `addJournalToInventory(Player player)` in `InventoryEvents`
  - Why it violates the boundary: Knows journal identity and configured-slot replacement policy, not one generic inventory effect.
  - Guidance: This is a portable-code violation: knows journal identity and configured-slot replacement policy, not one generic inventory effect. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the core journal/inventory behavior owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

#### [`src/paper/src/main/java/com/notquests/paper/events/QuestEvents.java`](src/paper/src/main/java/com/notquests/paper/events/QuestEvents.java)

- [x] **V2** [line 99](src/paper/src/main/java/com/notquests/paper/events/QuestEvents.java#L99) — `onChunkLoad(PlayerChunkLoadEvent e)` in `QuestEvents`
  - Why it violates the boundary: Decides marker refresh behavior from adapter-held beacon state.
  - Guidance: This is a portable-code violation: decides marker refresh behavior from adapter-held beacon state. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into core QuestPlayer, ActiveObjectives, or the concrete quest/objective feature owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 121](src/paper/src/main/java/com/notquests/paper/events/QuestEvents.java#L121) — `onPlayerConsumeItem(PlayerItemConsumeEvent e)` in `QuestEvents`
  - Why it violates the boundary: Mixes item-event translation with shared conversation input-blocking policy.
  - Guidance: This is a portable-code violation: mixes item-event translation with shared conversation input-blocking policy. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into core Conversations/chat-history ownership—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 184](src/paper/src/main/java/com/notquests/paper/events/QuestEvents.java#L184) — `onBlockBreak(BlockBreakEvent e)` in `QuestEvents`
  - Why it violates the boundary: Mutates shared harvest tracking and adapter-owned brewed-item state.
  - Guidance: This is a portable-code violation: mutates shared harvest tracking and adapter-owned brewed-item state. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the core Items/SavedItems or objective owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 246](src/paper/src/main/java/com/notquests/paper/events/QuestEvents.java#L246) — `handleBrewedItem(InventoryClickEvent event, PaperPlayer questPlayer)` in `QuestEvents`
  - Why it violates the boundary: Reconstructs fresh-brew gameplay semantics from adapter-owned state.
  - Guidance: This is a portable-code violation: reconstructs fresh-brew gameplay semantics from adapter-owned state. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the matching core concept owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 633](src/paper/src/main/java/com/notquests/paper/events/QuestEvents.java#L633) — `onBrew(BrewEvent e)` in `QuestEvents`
  - Why it violates the boundary: Maintains adapter-owned state to define which items count as freshly brewed.
  - Guidance: This is a portable-code violation: maintains adapter-owned state to define which items count as freshly brewed. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the core Items/SavedItems or objective owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 889](src/paper/src/main/java/com/notquests/paper/events/QuestEvents.java#L889) — `onPlayerJoin(PlayerJoinEvent e)` in `QuestEvents`
  - Why it violates the boundary: Orchestrates async database loading, state cleanup, attachment, and operator behavior.
  - Guidance: This is a portable-code violation: orchestrates async database loading, state cleanup, attachment, and operator behavior. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the core GeneralConfig/PlayerDatabase/data-lifecycle owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

#### [`src/paper/src/main/java/com/notquests/paper/events/hooks/CitizensEvents.java`](src/paper/src/main/java/com/notquests/paper/events/hooks/CitizensEvents.java)

- [x] **V2** [line 43](src/paper/src/main/java/com/notquests/paper/events/hooks/CitizensEvents.java#L43) — `onNPCDeathEvent(NPCDeathEvent event)` in `CitizensEvents`
  - Why it violates the boundary: Iterates players and decides quest/NPC death relevance in the adapter.
  - Guidance: This is a portable-code violation: iterates players and decides quest/NPC death relevance in the adapter. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into core NpcAttachments or the core conversation/quest owner that requested the NPC operation—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 120](src/paper/src/main/java/com/notquests/paper/events/hooks/CitizensEvents.java#L120) — `run()` in `CitizensEvents.anonymous@119`
  - Why it violates the boundary: Schedules and decides NPC navigation lifetime from conversation state.
  - Guidance: This is a portable-code violation: schedules and decides NPC navigation lifetime from conversation state. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into core Conversations/chat-history ownership—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

#### [`src/paper/src/main/java/com/notquests/paper/events/hooks/JobsRebornEvents.java`](src/paper/src/main/java/com/notquests/paper/events/hooks/JobsRebornEvents.java)

- [x] **V2** [line 15](src/paper/src/main/java/com/notquests/paper/events/hooks/JobsRebornEvents.java#L15) — `JobsRebornEvents(NotQuests main)` in `JobsRebornEvents`
  - Why it violates the boundary: Construction starts recurring objective synchronization policy.
  - Guidance: This is a portable-code violation: construction starts recurring objective synchronization policy. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into core QuestPlayer, ActiveObjectives, or the concrete quest/objective feature owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 27](src/paper/src/main/java/com/notquests/paper/events/hooks/JobsRebornEvents.java#L27) — `startLevelSyncTask()` in `JobsRebornEvents`
  - Why it violates the boundary: Schedules polling and selects players/objectives to synchronize.
  - Guidance: This is a portable-code violation: schedules polling and selects players/objectives to synchronize. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into core QuestPlayer, ActiveObjectives, or the concrete quest/objective feature owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

#### [`src/paper/src/main/java/com/notquests/paper/events/hooks/MythicMobsEvents.java`](src/paper/src/main/java/com/notquests/paper/events/hooks/MythicMobsEvents.java)

- [x] **V2** [line 19](src/paper/src/main/java/com/notquests/paper/events/hooks/MythicMobsEvents.java#L19) — `onMythicMobDeath(MythicMobDeathEvent event)` in `MythicMobsEvents`
  - Why it violates the boundary: Selects objective type and kill-credit/matching policy in Paper.
  - Guidance: This is a portable-code violation: selects objective type and kill-credit/matching policy in Paper. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into core QuestPlayer, ActiveObjectives, or the concrete quest/objective feature owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 34](src/paper/src/main/java/com/notquests/paper/events/hooks/MythicMobsEvents.java#L34) — `matches(String target, MythicMob killedMob)` in `MythicMobsEvents`
  - Why it violates the boundary: Portable objective target/faction matching syntax belongs with the objective/core.
  - Guidance: This is a portable-code violation: portable objective target/faction matching syntax belongs with the objective/core. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into core QuestPlayer, ActiveObjectives, or the concrete quest/objective feature owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

#### [`src/paper/src/main/java/com/notquests/paper/events/notquests/ObjectiveCompleteEvent.java`](src/paper/src/main/java/com/notquests/paper/events/notquests/ObjectiveCompleteEvent.java)

- [x] **V2** [line 79](src/paper/src/main/java/com/notquests/paper/events/notquests/ObjectiveCompleteEvent.java#L79) — `getObjectiveId()` in `ObjectiveCompleteEvent`
  - Why it violates the boundary: Derives core objective identity/storage paths inside a Paper event API.
  - Guidance: This is a portable-code violation: derives core objective identity/storage paths inside a Paper event API. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into core QuestPlayer, ActiveObjectives, or the concrete quest/objective feature owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 90](src/paper/src/main/java/com/notquests/paper/events/notquests/ObjectiveCompleteEvent.java#L90) — `getObjectiveHolderPath()` in `ObjectiveCompleteEvent`
  - Why it violates the boundary: Derives core objective identity/storage paths inside a Paper event API.
  - Guidance: This is a portable-code violation: derives core objective identity/storage paths inside a Paper event API. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into core QuestPlayer, ActiveObjectives, or the concrete quest/objective feature owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 97](src/paper/src/main/java/com/notquests/paper/events/notquests/ObjectiveCompleteEvent.java#L97) — `holderPath(String questName, int[] objectivePath)` in `ObjectiveCompleteEvent`
  - Why it violates the boundary: Derives core objective identity/storage paths inside a Paper event API.
  - Guidance: This is a portable-code violation: derives core objective identity/storage paths inside a Paper event API. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into core QuestPlayer, ActiveObjectives, or the concrete quest/objective feature owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

#### [`src/paper/src/main/java/com/notquests/paper/events/notquests/ObjectiveUnlockEvent.java`](src/paper/src/main/java/com/notquests/paper/events/notquests/ObjectiveUnlockEvent.java)

- [x] **V2** [line 83](src/paper/src/main/java/com/notquests/paper/events/notquests/ObjectiveUnlockEvent.java#L83) — `getObjectiveId()` in `ObjectiveUnlockEvent`
  - Why it violates the boundary: Derives core objective identity/storage paths inside a Paper event API.
  - Guidance: This is a portable-code violation: derives core objective identity/storage paths inside a Paper event API. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into core QuestPlayer, ActiveObjectives, or the concrete quest/objective feature owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 94](src/paper/src/main/java/com/notquests/paper/events/notquests/ObjectiveUnlockEvent.java#L94) — `getObjectiveHolderPath()` in `ObjectiveUnlockEvent`
  - Why it violates the boundary: Derives core objective identity/storage paths inside a Paper event API.
  - Guidance: This is a portable-code violation: derives core objective identity/storage paths inside a Paper event API. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into core QuestPlayer, ActiveObjectives, or the concrete quest/objective feature owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 105](src/paper/src/main/java/com/notquests/paper/events/notquests/ObjectiveUnlockEvent.java#L105) — `holderPath(String questName, int[] objectivePath)` in `ObjectiveUnlockEvent`
  - Why it violates the boundary: Derives core objective identity/storage paths inside a Paper event API.
  - Guidance: This is a portable-code violation: derives core objective identity/storage paths inside a Paper event API. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into core QuestPlayer, ActiveObjectives, or the concrete quest/objective feature owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

#### [`src/paper/src/main/java/com/notquests/paper/managers/ArmorStandManager.java`](src/paper/src/main/java/com/notquests/paper/managers/ArmorStandManager.java)

- [x] **V2** [line 17](src/paper/src/main/java/com/notquests/paper/managers/ArmorStandManager.java#L17) — `ArmorStandManager(NotQuests main)` in `ArmorStandManager`
  - Why it violates the boundary: Construction reads shared policy and starts a recurring rendering flow.
  - Guidance: This is a portable-code violation: construction reads shared policy and starts a recurring rendering flow. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the matching core concept owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 43](src/paper/src/main/java/com/notquests/paper/managers/ArmorStandManager.java#L43) — `loadAllArmorStandsFromLoadedChunks()` in `ArmorStandManager`
  - Why it violates the boundary: Broadly scans worlds and synchronizes adapter cache from core state.
  - Guidance: This is a portable-code violation: broadly scans worlds and synchronizes adapter cache from core state. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the matching core concept owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 56](src/paper/src/main/java/com/notquests/paper/managers/ArmorStandManager.java#L56) — `startQuestGiverIndicatorParticleRunnable()` in `ArmorStandManager`
  - Why it violates the boundary: Owns scheduling, TPS/config policy, iteration, and particle placement.
  - Guidance: This is a portable-code violation: owns scheduling, TPS/config policy, iteration, and particle placement. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the core GeneralConfig/PlayerDatabase/data-lifecycle owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

#### [`src/paper/src/main/java/com/notquests/paper/managers/integrations/EcoMobsManager.java`](src/paper/src/main/java/com/notquests/paper/managers/integrations/EcoMobsManager.java)

- [x] **V2** [line 21](src/paper/src/main/java/com/notquests/paper/managers/integrations/EcoMobsManager.java#L21) — `EcoMobsManager(NotQuests main)` in `EcoMobsManager`
  - Why it violates the boundary: Loads/caches a registry and owns success/failure messages during construction.
  - Guidance: This is a portable-code violation: loads/caches a registry and owns success/failure messages during construction. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core text, translation, or feature owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 46](src/paper/src/main/java/com/notquests/paper/managers/integrations/EcoMobsManager.java#L46) — `spawnMob(String mobToSpawnType, Location location, int amount, UnaryOperator<Location> locationRandomizer)` in `EcoMobsManager`
  - Why it violates the boundary: Validates inputs, emits policy messages, randomizes, and loops a multi-spawn flow.
  - Guidance: This is a portable-code violation: validates inputs, emits policy messages, randomizes, and loops a multi-spawn flow. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core text, translation, or feature owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

#### [`src/paper/src/main/java/com/notquests/paper/managers/integrations/LuckpermsManager.java`](src/paper/src/main/java/com/notquests/paper/managers/integrations/LuckpermsManager.java)

- [x] **V2** [line 26](src/paper/src/main/java/com/notquests/paper/managers/integrations/LuckpermsManager.java#L26) — `givePermission(UUID uuid, String permissionNode)` in `LuckpermsManager`
  - Why it violates the boundary: Combines permission effects with adapter-side input validation.
  - Guidance: This is a portable-code violation: combines permission effects with adapter-side input validation. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into core permission policy, leaving only the native permission read/write here—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 39](src/paper/src/main/java/com/notquests/paper/managers/integrations/LuckpermsManager.java#L39) — `denyPermission(UUID uuid, String permissionNode)` in `LuckpermsManager`
  - Why it violates the boundary: Combines permission effects with adapter-side input validation.
  - Guidance: This is a portable-code violation: combines permission effects with adapter-side input validation. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into core permission policy, leaving only the native permission read/write here—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 52](src/paper/src/main/java/com/notquests/paper/managers/integrations/LuckpermsManager.java#L52) — `unsetPermission(UUID uuid, String permissionNode)` in `LuckpermsManager`
  - Why it violates the boundary: Combines permission effects with adapter-side input validation.
  - Guidance: This is a portable-code violation: combines permission effects with adapter-side input validation. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into core permission policy, leaving only the native permission read/write here—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

#### [`src/paper/src/main/java/com/notquests/paper/managers/integrations/MythicMobsManager.java`](src/paper/src/main/java/com/notquests/paper/managers/integrations/MythicMobsManager.java)

- [x] **V2** [line 46](src/paper/src/main/java/com/notquests/paper/managers/integrations/MythicMobsManager.java#L46) — `spawnMob(String mobToSpawnType, Location location, int amount, UnaryOperator<Location> locationRandomizer)` in `MythicMobsManager`
  - Why it violates the boundary: Validates inputs, emits messages, randomizes, and loops a multi-spawn flow.
  - Guidance: This is a portable-code violation: validates inputs, emits messages, randomizes, and loops a multi-spawn flow. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core text, translation, or feature owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

#### [`src/paper/src/main/java/com/notquests/paper/managers/integrations/VaultManager.java`](src/paper/src/main/java/com/notquests/paper/managers/integrations/VaultManager.java)

- [x] **V2** [line 76](src/paper/src/main/java/com/notquests/paper/managers/integrations/VaultManager.java#L76) — `getEconomy()` in `VaultManager`
  - Why it violates the boundary: Adds enablement validation and a user-facing severe message to a simple handle read.
  - Guidance: This is a portable-code violation: adds enablement validation and a user-facing severe message to a simple handle read. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core text, translation, or feature owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

#### [`src/paper/src/main/java/com/notquests/paper/managers/integrations/betonquest/BetonQuestManager.java`](src/paper/src/main/java/com/notquests/paper/managers/integrations/betonquest/BetonQuestManager.java)

- [x] **V2** [line 191](src/paper/src/main/java/com/notquests/paper/managers/integrations/betonquest/BetonQuestManager.java#L191) — `profileFor(PaperPlayer questPlayer)` in `BetonQuestManager`
  - Why it violates the boundary: Validates the player and owns integration-facing error text before conversion.
  - Guidance: This is a portable-code violation: validates the player and owns integration-facing error text before conversion. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core text, translation, or feature owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

#### [`src/paper/src/main/java/com/notquests/paper/managers/integrations/citizens/CitizensManager.java`](src/paper/src/main/java/com/notquests/paper/managers/integrations/citizens/CitizensManager.java)

- [x] **V2** [line 49](src/paper/src/main/java/com/notquests/paper/managers/integrations/citizens/CitizensManager.java#L49) — `registerQuestGiverTrait()` in `CitizensManager`
  - Why it violates the boundary: Combines trait/event registration with core attachment application and conversation binding.
  - Guidance: This is a portable-code violation: combines trait/event registration with core attachment application and conversation binding. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into core Conversations/chat-history ownership—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 75](src/paper/src/main/java/com/notquests/paper/managers/integrations/citizens/CitizensManager.java#L75) — `postRegister()` in `CitizensManager`
  - Why it violates the boundary: Combines trait/event registration with core attachment application and conversation binding.
  - Guidance: This is a portable-code violation: combines trait/event registration with core attachment application and conversation binding. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into core Conversations/chat-history ownership—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

#### [`src/paper/src/main/java/com/notquests/paper/managers/integrations/citizens/QuestGiverNPCTrait.java`](src/paper/src/main/java/com/notquests/paper/managers/integrations/citizens/QuestGiverNPCTrait.java)

- [x] **V2** [line 187](src/paper/src/main/java/com/notquests/paper/managers/integrations/citizens/QuestGiverNPCTrait.java#L187) — `onAttach()` in `QuestGiverNPCTrait`
  - Why it violates the boundary: Owns adapter-side user-facing log text rather than an atomic platform operation.
  - Guidance: This is a portable-code violation: owns adapter-side user-facing log text rather than an atomic platform operation. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core text, translation, or feature owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

#### [`src/paper/src/main/java/com/notquests/paper/managers/integrations/fancynpcs/FancyNPCsManager.java`](src/paper/src/main/java/com/notquests/paper/managers/integrations/fancynpcs/FancyNPCsManager.java)

- [x] **V2** [line 18](src/paper/src/main/java/com/notquests/paper/managers/integrations/fancynpcs/FancyNPCsManager.java#L18) — `FancyNPCsManager(NotQuests main)` in `FancyNPCsManager`
  - Why it violates the boundary: Owns config/timing/filtering policy and a recurring particle-rendering flow.
  - Guidance: This is a portable-code violation: owns config/timing/filtering policy and a recurring particle-rendering flow. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the core GeneralConfig/PlayerDatabase/data-lifecycle owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 35](src/paper/src/main/java/com/notquests/paper/managers/integrations/fancynpcs/FancyNPCsManager.java#L35) — `startQuestGiverIndicatorParticleRunnable()` in `FancyNPCsManager`
  - Why it violates the boundary: Owns config/timing/filtering policy and a recurring particle-rendering flow.
  - Guidance: This is a portable-code violation: owns config/timing/filtering policy and a recurring particle-rendering flow. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the core GeneralConfig/PlayerDatabase/data-lifecycle owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

#### [`src/paper/src/main/java/com/notquests/paper/managers/npc/CitizensNPC.java`](src/paper/src/main/java/com/notquests/paper/managers/npc/CitizensNPC.java)

- [x] **V2** [line 52](src/paper/src/main/java/com/notquests/paper/managers/npc/CitizensNPC.java#L52) — `removeQuestGiverNPCTrait(Boolean showQuestInNPC, String questIdentifier)` in `CitizensNPC`
  - Why it violates the boundary: Includes validation/error text and trait-selection policy, not one raw trait effect.
  - Guidance: This is a portable-code violation: includes validation/error text and trait-selection policy, not one raw trait effect. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into core NpcAttachments or the core conversation/quest owner that requested the NPC operation—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 72](src/paper/src/main/java/com/notquests/paper/managers/npc/CitizensNPC.java#L72) — `addQuestGiverNPCTrait(Boolean showQuestInNPC, String questIdentifier)` in `CitizensNPC`
  - Why it violates the boundary: Includes validation/error text and trait-selection policy, not one raw trait effect.
  - Guidance: This is a portable-code violation: includes validation/error text and trait-selection policy, not one raw trait effect. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into core NpcAttachments or the core conversation/quest owner that requested the NPC operation—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

#### [`src/paper/src/main/java/com/notquests/paper/managers/npc/ConversationFocus.java`](src/paper/src/main/java/com/notquests/paper/managers/npc/ConversationFocus.java)

- [x] **V2** [line 29](src/paper/src/main/java/com/notquests/paper/managers/npc/ConversationFocus.java#L29) — `ConversationFocus(NotQuests main, Player player, Entity entity, String conversationName)` in `ConversationFocus`
  - Why it violates the boundary: Initializes conversation movement/focus policy and timing state.
  - Guidance: This is a portable-code violation: initializes conversation movement/focus policy and timing state. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into core Conversations/chat-history ownership—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

#### [`src/paper/src/main/java/com/notquests/paper/managers/npc/NPCManager.java`](src/paper/src/main/java/com/notquests/paper/managers/npc/NPCManager.java)

- [x] **V2** [line 96](src/paper/src/main/java/com/notquests/paper/managers/npc/NPCManager.java#L96) — `handleRightClickNQNPCSelectionWithAction(Consumer<NQNPC> actionWhenSelected, Player player, String successMessage, String displayName, String[] lore)` in `NPCManager`
  - Why it violates the boundary: Implements selector-tool state, item construction, defaults, and user messages.
  - Guidance: This is a portable-code violation: implements selector-tool state, item construction, defaults, and user messages. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into core NpcAttachments or the core conversation/quest owner that requested the NPC operation—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 134](src/paper/src/main/java/com/notquests/paper/managers/npc/NPCManager.java#L134) — `getNPCSelectionActions()` in `NPCManager`
  - Why it violates the boundary: Exposes command-style callback state owned by the adapter.
  - Guidance: This is a portable-code violation: exposes command-style callback state owned by the adapter. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core command definition or the core command framework—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 138](src/paper/src/main/java/com/notquests/paper/managers/npc/NPCManager.java#L138) — `executeNPCSelectionAction(NQNPC nqnpc, int npcSelectionActionID)` in `NPCManager`
  - Why it violates the boundary: Validates and executes stored command-style selection callbacks.
  - Guidance: This is a portable-code violation: validates and executes stored command-style selection callbacks. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core command definition or the core command framework—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

#### [`src/paper/src/main/java/com/notquests/paper/managers/npc/NQNPC.java`](src/paper/src/main/java/com/notquests/paper/managers/npc/NQNPC.java)

- [x] **V2** [line 21](src/paper/src/main/java/com/notquests/paper/managers/npc/NQNPC.java#L21) — `getIdentifyingString()` in `NQNPC`
  - Why it violates the boundary: Portable NPC identifier formatting belongs in core.
  - Guidance: This is a portable-code violation: portable NPC identifier formatting belongs in core. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into core NpcAttachments or the core conversation/quest owner that requested the NPC operation—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

#### [`src/paper/src/main/java/com/notquests/paper/managers/packets/ownpacketstuff/modern/PacketInjector.java`](src/paper/src/main/java/com/notquests/paper/managers/packets/ownpacketstuff/modern/PacketInjector.java)

- [x] **V2** [line 57](src/paper/src/main/java/com/notquests/paper/managers/packets/ownpacketstuff/modern/PacketInjector.java#L57) — `addPlayer(Player player)` in `PacketInjector`
  - Why it violates the boundary: Atomic channel work is mixed with shared disable policy and messages on failure.
  - Guidance: This is a portable-code violation: atomic channel work is mixed with shared disable policy and messages on failure. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core text, translation, or feature owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 74](src/paper/src/main/java/com/notquests/paper/managers/packets/ownpacketstuff/modern/PacketInjector.java#L74) — `removePlayer(Player player)` in `PacketInjector`
  - Why it violates the boundary: Atomic channel work is mixed with shared disable policy and messages on failure.
  - Guidance: This is a portable-code violation: atomic channel work is mixed with shared disable policy and messages on failure. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core text, translation, or feature owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 97](src/paper/src/main/java/com/notquests/paper/managers/packets/ownpacketstuff/modern/PacketInjector.java#L97) — `getChannel(Connection networkManager)` in `PacketInjector`
  - Why it violates the boundary: Atomic channel work is mixed with shared disable policy and messages on failure.
  - Guidance: This is a portable-code violation: atomic channel work is mixed with shared disable policy and messages on failure. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core text, translation, or feature owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

#### [`src/paper/src/main/java/com/notquests/paper/managers/packets/ownpacketstuff/reflection/ReflectionPacketInjector.java`](src/paper/src/main/java/com/notquests/paper/managers/packets/ownpacketstuff/reflection/ReflectionPacketInjector.java)

- [x] **V2** [line 50](src/paper/src/main/java/com/notquests/paper/managers/packets/ownpacketstuff/reflection/ReflectionPacketInjector.java#L50) — `initializeNMSStuff()` in `ReflectionPacketInjector`
  - Why it violates the boundary: Platform channel work is mixed with shared disable policy and messages on failure.
  - Guidance: This is a portable-code violation: platform channel work is mixed with shared disable policy and messages on failure. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core text, translation, or feature owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 92](src/paper/src/main/java/com/notquests/paper/managers/packets/ownpacketstuff/reflection/ReflectionPacketInjector.java#L92) — `addPlayer(Player player)` in `ReflectionPacketInjector`
  - Why it violates the boundary: Platform channel work is mixed with shared disable policy and messages on failure.
  - Guidance: This is a portable-code violation: platform channel work is mixed with shared disable policy and messages on failure. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core text, translation, or feature owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 108](src/paper/src/main/java/com/notquests/paper/managers/packets/ownpacketstuff/reflection/ReflectionPacketInjector.java#L108) — `removePlayer(Player p)` in `ReflectionPacketInjector`
  - Why it violates the boundary: Platform channel work is mixed with shared disable policy and messages on failure.
  - Guidance: This is a portable-code violation: platform channel work is mixed with shared disable policy and messages on failure. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core text, translation, or feature owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 129](src/paper/src/main/java/com/notquests/paper/managers/packets/ownpacketstuff/reflection/ReflectionPacketInjector.java#L129) — `getChannel(Object networkManager)` in `ReflectionPacketInjector`
  - Why it violates the boundary: Platform channel work is mixed with shared disable policy and messages on failure.
  - Guidance: This is a portable-code violation: platform channel work is mixed with shared disable policy and messages on failure. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core text, translation, or feature owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

#### [`src/paper/src/main/java/com/notquests/paper/managers/packets/packetevents/PacketEventsPacketListener.java`](src/paper/src/main/java/com/notquests/paper/managers/packets/packetevents/PacketEventsPacketListener.java)

- [x] **V2** [line 31](src/paper/src/main/java/com/notquests/paper/managers/packets/packetevents/PacketEventsPacketListener.java#L31) — `onPacketSend(PacketSendEvent event)` in `PacketEventsPacketListener`
  - Why it violates the boundary: Owns conversation replay-marker filtering policy in the packet adapter.
  - Guidance: This is a portable-code violation: owns conversation replay-marker filtering policy in the packet adapter. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into core Conversations/chat-history ownership—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

### NeoForge (71)

#### [`src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeArguments.java`](src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeArguments.java)

- [x] **V2** [line 50](src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeArguments.java#L50) — `listSuggestions(CommandContext<S> context, SuggestionsBuilder builder)` in `NeoForgeArguments.ItemSelectionArgument`
  - Why it violates the boundary: Builds shared item-keyword/comma suggestion policy instead of only exposing native item IDs.
  - Guidance: This is a portable-code violation: builds shared item-keyword/comma suggestion policy instead of only exposing native item IDs. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the core Items/SavedItems or objective owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

#### [`src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeClientBeamRenderer.java`](src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeClientBeamRenderer.java)

- [x] **V2** [line 93](src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeClientBeamRenderer.java#L93) — `marker(BeamLocation beam, Vec3 cameraPosition)` in `NeoForgeClientBeamRenderer`
  - Why it violates the boundary: Owns objective-marker projection/range placement policy rather than only rendering supplied geometry.
  - Guidance: This is a portable-code violation: owns objective-marker projection/range placement policy rather than only rendering supplied geometry. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into core QuestPlayer, ActiveObjectives, or the concrete quest/objective feature owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 166](src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeClientBeamRenderer.java#L166) — `clamp(double value, double min, double max)` in `NeoForgeClientBeamRenderer`
  - Why it violates the boundary: Pure projection-math helper belonging to the non-leaf marker placement policy.
  - Guidance: This is a portable-code violation: pure projection-math helper belonging to the non-leaf marker placement policy. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into core QuestPlayer, ActiveObjectives, or the concrete quest/objective feature owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

#### [`src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeConversationDisplay.java`](src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeConversationDisplay.java)

- [x] **V2** [line 29](src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeConversationDisplay.java#L29) — `line(PlatformPlayer questPlayer, ConversationManager.DisplayLine line)` in `NeoForgeConversationDisplay`
  - Why it violates the boundary: Mixes core speaker formatting and delete-history flow with the native send effect.
  - Guidance: This is a portable-code violation: mixes core speaker formatting and delete-history flow with the native send effect. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the matching core concept owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 126](src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeConversationDisplay.java#L126) — `speaker(ConversationManager.DisplayLine line)` in `NeoForgeConversationDisplay`
  - Why it violates the boundary: Reconstructs a core Speaker domain object from display data in the adapter.
  - Guidance: This is a portable-code violation: reconstructs a core Speaker domain object from display data in the adapter. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the matching core concept owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 137](src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeConversationDisplay.java#L137) — `speaker(ConversationManager.DisplayOption option)` in `NeoForgeConversationDisplay`
  - Why it violates the boundary: Reconstructs a core Speaker domain object from display data in the adapter.
  - Guidance: This is a portable-code violation: reconstructs a core Speaker domain object from display data in the adapter. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the matching core concept owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

#### [`src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeCoreCommandCompiler.java`](src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeCoreCommandCompiler.java)

- [x] **V2** [line 296](src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeCoreCommandCompiler.java#L296) — `pushFlagHint(List<NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>> flags, String input, CommandContext<CommandSourceStack> context)` in `NeoForgeCoreCommandCompiler`
  - Why it violates the boundary: Parses portable flag-token state and chooses command hint behavior.
  - Guidance: This is a portable-code violation: parses portable flag-token state and chooses command hint behavior. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core command definition or the core command framework—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 342](src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeCoreCommandCompiler.java#L342) — `suggestions(NQCommandTree.Node<NQArgumentType, NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>, NQSuggestionProvider<NQCommandContext>, NQCommandHandler> node, CommandContext<CommandSourceStack> context, String input)` in `NeoForgeCoreCommandCompiler`
  - Why it violates the boundary: Owns placeholder fallback policy for core command suggestions.
  - Guidance: This is a portable-code violation: owns placeholder fallback policy for core command suggestions. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core command definition or the core command framework—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 363](src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeCoreCommandCompiler.java#L363) — `flagSuggestions(NQCommandTree.Node<NQArgumentType, NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>, NQSuggestionProvider<NQCommandContext>, NQCommandHandler> node)` in `NeoForgeCoreCommandCompiler`
  - Why it violates the boundary: Owns portable flag parsing, presence filtering, and value-suggestion flow.
  - Guidance: This is a portable-code violation: owns portable flag parsing, presence filtering, and value-suggestion flow. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the matching core concept owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 407](src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeCoreCommandCompiler.java#L407) — `awaitingFlagValue(List<NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>> flags, String[] tokens)` in `NeoForgeCoreCommandCompiler`
  - Why it violates the boundary: Portable flag-token interpretation belongs in the shared command framework.
  - Guidance: This is a portable-code violation: portable flag-token interpretation belongs in the shared command framework. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core command definition or the core command framework—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 423](src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeCoreCommandCompiler.java#L423) — `placeholderSuggestion(String argumentName)` in `NeoForgeCoreCommandCompiler`
  - Why it violates the boundary: Pure portable placeholder suggestion formatting, with no NeoForge operation.
  - Guidance: This is a portable-code violation: pure portable placeholder suggestion formatting, with no NeoForge operation. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core text, translation, or feature owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 427](src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeCoreCommandCompiler.java#L427) — `suggestMatching(List<String> candidates, SuggestionsBuilder suggestions)` in `NeoForgeCoreCommandCompiler`
  - Why it violates the boundary: Portable prefix-matching suggestion policy with no NeoForge API requirement.
  - Guidance: This is a portable-code violation: portable prefix-matching suggestion policy with no NeoForge API requirement. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the matching core concept owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 436](src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeCoreCommandCompiler.java#L436) — `suggestArgument(NQArgumentType argument, List<String> candidates, SuggestionsBuilder suggestions)` in `NeoForgeCoreCommandCompiler`
  - Why it violates the boundary: Selects shared comma-list suggestion policy from core argument kinds.
  - Guidance: This is a portable-code violation: selects shared comma-list suggestion policy from core argument kinds. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the matching core concept owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 449](src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeCoreCommandCompiler.java#L449) — `suggestCommaSeparated(List<String> candidates, SuggestionsBuilder suggestions)` in `NeoForgeCoreCommandCompiler`
  - Why it violates the boundary: Implements portable comma-token suggestion policy inside the adapter.
  - Guidance: This is a portable-code violation: implements portable comma-token suggestion policy inside the adapter. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the matching core concept owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 466](src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeCoreCommandCompiler.java#L466) — `commaCandidates(List<String> candidates)` in `NeoForgeCoreCommandCompiler`
  - Why it violates the boundary: Pure portable comma-candidate expansion, with no NeoForge operation.
  - Guidance: This is a portable-code violation: pure portable comma-candidate expansion, with no NeoForge operation. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the matching core concept owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 470](src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeCoreCommandCompiler.java#L470) — `literalNames(NQCommandTree.Node<NQArgumentType, NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>, NQSuggestionProvider<NQCommandContext>, NQCommandHandler> node)` in `NeoForgeCoreCommandCompiler`
  - Why it violates the boundary: Portable command alias filtering with no NeoForge API requirement.
  - Guidance: This is a portable-code violation: portable command alias filtering with no NeoForge API requirement. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core command definition or the core command framework—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 488](src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeCoreCommandCompiler.java#L488) — `append(List<NQCommandTree.Node<NQArgumentType, NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>, NQSuggestionProvider<NQCommandContext>, NQCommandHandler>> path, NQCommandTree.Node<NQArgumentType, NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>, NQSuggestionProvider<NQCommandContext>, NQCommandHandler> child)` in `NeoForgeCoreCommandCompiler`
  - Why it violates the boundary: Portable command-tree path manipulation with no NeoForge API requirement.
  - Guidance: This is a portable-code violation: portable command-tree path manipulation with no NeoForge API requirement. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core command definition or the core command framework—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 517](src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeCoreCommandCompiler.java#L517) — `currentTokenStart(String input)` in `NeoForgeCoreCommandCompiler`
  - Why it violates the boundary: Pure portable token-boundary parsing, with no NeoForge operation.
  - Guidance: This is a portable-code violation: pure portable token-boundary parsing, with no NeoForge operation. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the matching core concept owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 570](src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeCoreCommandCompiler.java#L570) — `convertFlagValue(NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>> flag, String rawValue)` in `NeoForgeCoreCommandCompiler`
  - Why it violates the boundary: Portable core flag-value coercion belongs in the command framework.
  - Guidance: This is a portable-code violation: portable core flag-value coercion belongs in the command framework. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core command definition or the core command framework—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 716](src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeCoreCommandCompiler.java#L716) — `Context(CommandContext<CommandSourceStack> context, String flagString, List<NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>> flags)` in `NeoForgeCoreCommandCompiler.Context`
  - Why it violates the boundary: Parses portable command flags and owns command-context state rather than only adapting native values.
  - Guidance: This is a portable-code violation: parses portable command flags and owns command-context state rather than only adapting native values. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core command definition or the core command framework—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 784](src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeCoreCommandCompiler.java#L784) — `findFlag(List<NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>> flags, String name)` in `NeoForgeCoreCommandCompiler`
  - Why it violates the boundary: Portable case-insensitive flag lookup belongs in the command framework.
  - Guidance: This is a portable-code violation: portable case-insensitive flag lookup belongs in the command framework. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core command definition or the core command framework—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 796](src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeCoreCommandCompiler.java#L796) — `normalize(String value)` in `NeoForgeCoreCommandCompiler`
  - Why it violates the boundary: Pure portable string normalization, with no NeoForge operation.
  - Guidance: This is a portable-code violation: pure portable string normalization, with no NeoForge operation. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the matching core concept owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

#### [`src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeGuiRenderer.java`](src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeGuiRenderer.java)

- [x] **V2** [line 153](src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeGuiRenderer.java#L153) — `rethrowFatal(Throwable throwable)` in `NeoForgeGuiRenderer`
  - Why it violates the boundary: Generic fatal-error classification is portable process policy, not a platform leaf.
  - Guidance: This is a portable-code violation: generic fatal-error classification is portable process policy, not a platform leaf. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the matching core concept owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

#### [`src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeNotQuestsAdapter.java`](src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeNotQuestsAdapter.java)

- [x] **V2** [line 246](src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeNotQuestsAdapter.java#L246) — `itemSelectionOptions()` in `NeoForgeNotQuestsAdapter`
  - Why it violates the boundary: Combines native IDs with shared saved-item and keyword suggestion policy.
  - Guidance: This is a portable-code violation: combines native IDs with shared saved-item and keyword suggestion policy. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the core Items/SavedItems or objective owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 327](src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeNotQuestsAdapter.java#L327) — `entityTypeIds()` in `NeoForgeNotQuestsAdapter`
  - Why it violates the boundary: Adds the portable 'any' command keyword to native entity IDs; that suggestion policy belongs in core.
  - Guidance: This is a portable-code violation: adds the portable 'any' command keyword to native entity IDs; that suggestion policy belongs in core. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core command definition or the core command framework—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 384](src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeNotQuestsAdapter.java#L384) — `blockMaterialOptions()` in `NeoForgeNotQuestsAdapter`
  - Why it violates the boundary: Combines native block IDs with shared saved-item and keyword suggestion policy.
  - Guidance: This is a portable-code violation: combines native block IDs with shared saved-item and keyword suggestion policy. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the core Items/SavedItems or objective owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 463](src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeNotQuestsAdapter.java#L463) — `setBlockMaterial(PlatformPlayer questPlayer, NQLocation location, String materialOrKeyword)` in `NeoForgeNotQuestsAdapter`
  - Why it violates the boundary: Combines shared hand/any/saved-item resolution policy with the native block mutation.
  - Guidance: This is a portable-code violation: combines shared hand/any/saved-item resolution policy with the native block mutation. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the core Items/SavedItems or objective owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 625](src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeNotQuestsAdapter.java#L625) — `npcSelectorOptions(boolean allowNone, boolean allowRightClickSelect)` in `NeoForgeNotQuestsAdapter`
  - Why it violates the boundary: High-level command suggestion flow delegates to adapter-owned NPC workflow policy.
  - Guidance: This is a portable-code violation: high-level command suggestion flow delegates to adapter-owned NPC workflow policy. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core command definition or the core command framework—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 632](src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeNotQuestsAdapter.java#L632) — `npcSelection(String npcSelector)` in `NeoForgeNotQuestsAdapter`
  - Why it violates the boundary: Passes portable armor-stand selection lookup back into core; no platform leaf exists here.
  - Guidance: This is a portable-code violation: passes portable armor-stand selection lookup back into core; no platform leaf exists here. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into core NpcAttachments or the core conversation/quest owner that requested the NPC operation—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 637](src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeNotQuestsAdapter.java#L637) — `applyConversationNpcAttachment(String conversationName, NpcSelection selection)` in `NeoForgeNotQuestsAdapter`
  - Why it violates the boundary: High-level attachment operation delegates to adapter validation/message behavior.
  - Guidance: This is a portable-code violation: high-level attachment operation delegates to adapter validation/message behavior. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core text, translation, or feature owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 642](src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeNotQuestsAdapter.java#L642) — `applyQuestNpcAttachment(String questName, NpcSelection selection, boolean showQuestInNpc)` in `NeoForgeNotQuestsAdapter`
  - Why it violates the boundary: High-level attachment operation delegates to adapter validation behavior.
  - Guidance: This is a portable-code violation: high-level attachment operation delegates to adapter validation behavior. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the matching core concept owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 656](src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeNotQuestsAdapter.java#L656) — `giveArmorStandTool(PlatformPlayer actor, ArmorStandToolItem tool)` in `NeoForgeNotQuestsAdapter`
  - Why it violates the boundary: High-level tool workflow returns domain statuses and adapter-owned validation text.
  - Guidance: This is a portable-code violation: high-level tool workflow returns domain statuses and adapter-owned validation text. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core text, translation, or feature owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 673](src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeNotQuestsAdapter.java#L673) — `variableNames(VariableDataType type)` in `NeoForgeNotQuestsAdapter`
  - Why it violates the boundary: Portable registry query merely delegates back to core.
  - Guidance: This is a portable-code violation: portable registry query merely delegates back to core. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the matching core concept owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 678](src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeNotQuestsAdapter.java#L678) — `variableType(String variableName)` in `NeoForgeNotQuestsAdapter`
  - Why it violates the boundary: Portable registry query merely delegates back to core.
  - Guidance: This is a portable-code violation: portable registry query merely delegates back to core. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the matching core concept owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 683](src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeNotQuestsAdapter.java#L683) — `variableSingular(String variableName)` in `NeoForgeNotQuestsAdapter`
  - Why it violates the boundary: Portable registry metadata query merely delegates back to core.
  - Guidance: This is a portable-code violation: portable registry metadata query merely delegates back to core. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the matching core concept owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 688](src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeNotQuestsAdapter.java#L688) — `variablePlural(String variableName)` in `NeoForgeNotQuestsAdapter`
  - Why it violates the boundary: Portable registry metadata query merely delegates back to core.
  - Guidance: This is a portable-code violation: portable registry metadata query merely delegates back to core. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the matching core concept owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 693](src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeNotQuestsAdapter.java#L693) — `variableFields(String variableName)` in `NeoForgeNotQuestsAdapter`
  - Why it violates the boundary: Portable registry metadata query merely delegates back to core.
  - Guidance: This is a portable-code violation: portable registry metadata query merely delegates back to core. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the matching core concept owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 698](src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeNotQuestsAdapter.java#L698) — `variableValue(String variableName, PlatformPlayer questPlayer, Object[] objects)` in `NeoForgeNotQuestsAdapter`
  - Why it violates the boundary: Portable variable evaluation merely delegates back to core.
  - Guidance: This is a portable-code violation: portable variable evaluation merely delegates back to core. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the matching core concept owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 706](src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeNotQuestsAdapter.java#L706) — `variableValue(String variableName, PlatformPlayer questPlayer, Map<String, String> stringArguments, Map<String, ?> numberArguments, Map<String, ?> booleanArguments)` in `NeoForgeNotQuestsAdapter`
  - Why it violates the boundary: Portable variable evaluation merely delegates back to core.
  - Guidance: This is a portable-code violation: portable variable evaluation merely delegates back to core. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the matching core concept owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 784](src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeNotQuestsAdapter.java#L784) — `resolveActionText(Actions.Data action, PlatformPlayer questPlayer, String text, Object[] objects)` in `NeoForgeNotQuestsAdapter`
  - Why it violates the boundary: Portable action-text resolution merely delegates back to core.
  - Guidance: This is a portable-code violation: portable action-text resolution merely delegates back to core. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core text, translation, or feature owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 790](src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeNotQuestsAdapter.java#L790) — `objectiveTaskText(String translationKey, PlatformPlayer questPlayer, ActiveObjective activeObjective, Map<String, String> replacements)` in `NeoForgeNotQuestsAdapter`
  - Why it violates the boundary: Portable translation/fallback behavior merely delegates back to core.
  - Guidance: This is a portable-code violation: portable translation/fallback behavior merely delegates back to core. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the matching core concept owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 803](src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeNotQuestsAdapter.java#L803) — `trimSlash(String command)` in `NeoForgeNotQuestsAdapter`
  - Why it violates the boundary: Pure portable command-string normalization, with no NeoForge operation.
  - Guidance: This is a portable-code violation: pure portable command-string normalization, with no NeoForge operation. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core command definition or the core command framework—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 807](src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeNotQuestsAdapter.java#L807) — `prefixSlash(String command)` in `NeoForgeNotQuestsAdapter`
  - Why it violates the boundary: Pure portable command-string normalization, with no NeoForge operation.
  - Guidance: This is a portable-code violation: pure portable command-string normalization, with no NeoForge operation. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core command definition or the core command framework—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 881](src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeNotQuestsAdapter.java#L881) — `customStatistic(String statisticId)` in `NeoForgeNotQuestsAdapter`
  - Why it violates the boundary: Owns validation and shared warning text instead of returning an atomic native lookup.
  - Guidance: This is a portable-code violation: owns validation and shared warning text instead of returning an atomic native lookup. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core text, translation, or feature owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 890](src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeNotQuestsAdapter.java#L890) — `advancement(String advancementId)` in `NeoForgeNotQuestsAdapter`
  - Why it violates the boundary: Owns validation and shared warning text instead of returning an atomic native lookup.
  - Guidance: This is a portable-code violation: owns validation and shared warning text instead of returning an atomic native lookup. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core text, translation, or feature owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 904](src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeNotQuestsAdapter.java#L904) — `blockFromValue(ServerPlayer player, String materialOrKeyword)` in `NeoForgeNotQuestsAdapter`
  - Why it violates the boundary: Owns hand/any/saved-item recursion and random block-selection policy.
  - Guidance: This is a portable-code violation: owns hand/any/saved-item recursion and random block-selection policy. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the core Items/SavedItems or objective owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

#### [`src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeNpcAttachments.java`](src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeNpcAttachments.java)

- [x] **V2** [line 39](src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeNpcAttachments.java#L39) — `npcSelectorOptions(boolean allowNone, boolean allowRightClickSelect)` in `NeoForgeNpcAttachments`
  - Why it violates the boundary: Builds shared command selector keywords from flags; no native NPC observation.
  - Guidance: This is a portable-code violation: builds shared command selector keywords from flags; no native NPC observation. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core command definition or the core command framework—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 50](src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeNpcAttachments.java#L50) — `npcSelection(String selector)` in `NeoForgeNpcAttachments`
  - Why it violates the boundary: Portable selector lookup merely delegates back to core.
  - Guidance: This is a portable-code violation: portable selector lookup merely delegates back to core. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the matching core concept owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 54](src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeNpcAttachments.java#L54) — `applyConversationNpcAttachment(String conversationName, NotQuestsAdapter.NpcSelection selection)` in `NeoForgeNpcAttachments`
  - Why it violates the boundary: Performs attachment validation and emits shared warning text without a native attachment effect.
  - Guidance: This is a portable-code violation: performs attachment validation and emits shared warning text without a native attachment effect. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core text, translation, or feature owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 61](src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeNpcAttachments.java#L61) — `applyQuestNpcAttachment(String questName, NotQuestsAdapter.NpcSelection selection, boolean showQuestInNpc)` in `NeoForgeNpcAttachments`
  - Why it violates the boundary: Validation-only success/failure fallback; it applies no native attachment.
  - Guidance: This is a portable-code violation: validation-only success/failure fallback; it applies no native attachment. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the matching core concept owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 85](src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeNpcAttachments.java#L85) — `giveArmorStandTool(PlatformPlayer actor, NotQuestsAdapter.ArmorStandToolItem tool)` in `NeoForgeNpcAttachments`
  - Why it violates the boundary: Combines domain status flow, validation text, item materialization, and delivery.
  - Guidance: This is a portable-code violation: combines domain status flow, validation text, item materialization, and delivery. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the core Items/SavedItems or objective owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 112](src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeNpcAttachments.java#L112) — `pendingSelection(NotQuestsAdapter.ArmorStandToolItem tool)` in `NeoForgeNpcAttachments`
  - Why it violates the boundary: Maps domain tool operations into an adapter-owned selection state machine.
  - Guidance: This is a portable-code violation: maps domain tool operations into an adapter-owned selection state machine. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the matching core concept owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

#### [`src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeObjectiveEvents.java`](src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeObjectiveEvents.java)

- [x] **V2** [line 203](src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeObjectiveEvents.java#L203) — `onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event)` in `NeoForgeObjectiveEvents`
  - Why it violates the boundary: Orchestrates async player loading, stale-login cleanup, and operator-join behavior.
  - Guidance: This is a portable-code violation: orchestrates async player loading, stale-login cleanup, and operator-join behavior. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the matching core concept owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 227](src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeObjectiveEvents.java#L227) — `onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event)` in `NeoForgeObjectiveEvents`
  - Why it violates the boundary: Orchestrates disconnect behavior, adapter cleanup, and async persistence.
  - Guidance: This is a portable-code violation: orchestrates disconnect behavior, adapter cleanup, and async persistence. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the matching core concept owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 515](src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeObjectiveEvents.java#L515) — `blockKey(Level level, BlockPos pos)` in `NeoForgeObjectiveEvents`
  - Why it violates the boundary: Constructs the shared persisted harvest-block key shape in the adapter.
  - Guidance: This is a portable-code violation: constructs the shared persisted harvest-block key shape in the adapter. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the matching core concept owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 677](src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeObjectiveEvents.java#L677) — `clear()` in `NeoForgeObjectiveEvents`
  - Why it violates the boundary: Broad shutdown orchestration cleans several adapter subsystems and shared tick state.
  - Guidance: This is a portable-code violation: broad shutdown orchestration cleans several adapter subsystems and shared tick state. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the matching core concept owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

#### [`src/neoforge/src/main/java/com/notquests/neoforge/NeoForgePermissions.java`](src/neoforge/src/main/java/com/notquests/neoforge/NeoForgePermissions.java)

- [x] **V2** [line 36](src/neoforge/src/main/java/com/notquests/neoforge/NeoForgePermissions.java#L36) — `nodes()` in `NeoForgePermissions`
  - Why it violates the boundary: Defines shared NotQuests permission names and default-access policy in the adapter.
  - Guidance: This is a portable-code violation: defines shared NotQuests permission names and default-access policy in the adapter. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into core QuestPlayer, ActiveObjectives, or the concrete quest/objective feature owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 45](src/neoforge/src/main/java/com/notquests/neoforge/NeoForgePermissions.java#L45) — `node(String path, boolean availableToEveryone)` in `NeoForgePermissions`
  - Why it violates the boundary: Encodes shared default permission policy while constructing a native node.
  - Guidance: This is a portable-code violation: encodes shared default permission policy while constructing a native node. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into core permission policy, leaving only the native permission read/write here—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

#### [`src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeQuestPlayer.java`](src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeQuestPlayer.java)

- [x] **V2** [line 1155](src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeQuestPlayer.java#L1155) — `randomOffset(int radius)` in `NeoForgeQuestPlayer`
  - Why it violates the boundary: Pure random spawn-placement policy used by the non-leaf mob flow.
  - Guidance: This is a portable-code violation: pure random spawn-placement policy used by the non-leaf mob flow. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the matching core concept owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

#### [`src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeSuggestionText.java`](src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeSuggestionText.java)

- [x] **V2** [line 9](src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeSuggestionText.java#L9) — `placeholderSuggestion(String argumentName)` in `NeoForgeSuggestionText`
  - Why it violates the boundary: Pure portable placeholder formatting, with no NeoForge operation.
  - Guidance: This is a portable-code violation: pure portable placeholder formatting, with no NeoForge operation. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core text, translation, or feature owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 16](src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeSuggestionText.java#L16) — `commaCandidates(List<String> candidates)` in `NeoForgeSuggestionText`
  - Why it violates the boundary: Pure portable comma-candidate expansion, with no NeoForge operation.
  - Guidance: This is a portable-code violation: pure portable comma-candidate expansion, with no NeoForge operation. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the matching core concept owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

#### [`src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeText.java`](src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeText.java)

- [x] **V2** [line 107](src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeText.java#L107) — `broadcast(MinecraftServer server, String miniMessageText)` in `NeoForgeText`
  - Why it violates the boundary: Combines native broadcast with per-player mutation of shared chat history.
  - Guidance: This is a portable-code violation: combines native broadcast with per-player mutation of shared chat history. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into core Conversations/chat-history ownership—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 118](src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeText.java#L118) — `plain(String miniMessageText)` in `NeoForgeText`
  - Why it violates the boundary: Pure portable MiniMessage stripping, with no NeoForge operation.
  - Guidance: This is a portable-code violation: pure portable MiniMessage stripping, with no NeoForge operation. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core text, translation, or feature owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 130](src/neoforge/src/main/java/com/notquests/neoforge/NeoForgeText.java#L130) — `stripMiniMessage(String message)` in `NeoForgeText`
  - Why it violates the boundary: Pure portable MiniMessage stripping delegated to core.
  - Guidance: This is a portable-code violation: pure portable MiniMessage stripping delegated to core. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the relevant core text, translation, or feature owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

#### [`src/neoforge/src/main/java/com/notquests/neoforge/NotQuestsEvents.java`](src/neoforge/src/main/java/com/notquests/neoforge/NotQuestsEvents.java)

- [x] **V2** [line 110](src/neoforge/src/main/java/com/notquests/neoforge/NotQuestsEvents.java#L110) — `objectiveId()` in `NotQuestsEvents.ObjectiveComplete`
  - Why it violates the boundary: Derives portable objective identity from a core path inside the platform event API.
  - Guidance: This is a portable-code violation: derives portable objective identity from a core path inside the platform event API. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into core QuestPlayer, ActiveObjectives, or the concrete quest/objective feature owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 116](src/neoforge/src/main/java/com/notquests/neoforge/NotQuestsEvents.java#L116) — `objectiveHolderPath()` in `NotQuestsEvents.ObjectiveComplete`
  - Why it violates the boundary: Computes a portable objective-holder storage path in the platform event API.
  - Guidance: This is a portable-code violation: computes a portable objective-holder storage path in the platform event API. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into core QuestPlayer, ActiveObjectives, or the concrete quest/objective feature owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 147](src/neoforge/src/main/java/com/notquests/neoforge/NotQuestsEvents.java#L147) — `objectiveId()` in `NotQuestsEvents.ObjectiveUnlock`
  - Why it violates the boundary: Derives portable objective identity from a core path inside the platform event API.
  - Guidance: This is a portable-code violation: derives portable objective identity from a core path inside the platform event API. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into core QuestPlayer, ActiveObjectives, or the concrete quest/objective feature owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 153](src/neoforge/src/main/java/com/notquests/neoforge/NotQuestsEvents.java#L153) — `objectiveHolderPath()` in `NotQuestsEvents.ObjectiveUnlock`
  - Why it violates the boundary: Computes a portable objective-holder storage path in the platform event API.
  - Guidance: This is a portable-code violation: computes a portable objective-holder storage path in the platform event API. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into core QuestPlayer, ActiveObjectives, or the concrete quest/objective feature owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 157](src/neoforge/src/main/java/com/notquests/neoforge/NotQuestsEvents.java#L157) — `holderPath(String questName, int[] objectivePath)` in `NotQuestsEvents`
  - Why it violates the boundary: Portable objective-path formatting belongs on the core objective model.
  - Guidance: This is a portable-code violation: portable objective-path formatting belongs on the core objective model. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into core QuestPlayer, ActiveObjectives, or the concrete quest/objective feature owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

#### [`src/neoforge/src/main/java/com/notquests/neoforge/NotQuestsNeoForge.java`](src/neoforge/src/main/java/com/notquests/neoforge/NotQuestsNeoForge.java)

- [x] **V2** [line 164](src/neoforge/src/main/java/com/notquests/neoforge/NotQuestsNeoForge.java#L164) — `loadPlatformData(BooleanSupplier dataLoad)` in `NotQuestsNeoForge`
  - Why it violates the boundary: Owns async data-load task lifecycle, cancellation state, and completion cleanup.
  - Guidance: This is a portable-code violation: owns async data-load task lifecycle, cancellation state, and completion cleanup. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the core plugin lifecycle owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.

- [x] **V2** [line 186](src/neoforge/src/main/java/com/notquests/neoforge/NotQuestsNeoForge.java#L186) — `finishPlatformStart()` in `NotQuestsNeoForge`
  - Why it violates the boundary: Starts shared update-check behavior rather than only supplying a scheduler leaf.
  - Guidance: This is a portable-code violation: starts shared update-check behavior rather than only supplying a scheduler leaf. The logic does not fundamentally require Bukkit or NeoForge, so keeping it here duplicates policy and makes the adapter harder to scan. Fold it into the core plugin lifecycle owner—preferably as a private helper or nested value in that concept owner, not a new tiny utility class—and keep only the final native conversion/call in the adapter.
