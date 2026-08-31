package com.notquests.neoforge;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.notquests.core.commands.framework.NQCommandSchema;
import com.notquests.core.registry.NotQuestsRegistry.Variables;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

class NeoForgeArchitectureTest {
    private static final Path NEOFORGE = Path.of("src/main/java");
    private static final List<String> FORBIDDEN_CORE_STATE_OWNERSHIP =
            List.of(
                    "new NotQuestsRegistry(",
                    "new NotQuestsPlugin(",
                    "new ActiveObjectives(",
                    "new SavedActions(",
                    "new ConversationManager(",
                    "new LogManager(",
                    "new PluginStatus(",
                    "new CommandManager(",
                    "new ChainRunner",
                    "ChainRunner.<",
                    "class NeoForgeRegistryMessages",
                    "new ConcurrentHashMap<String, QuestPlayer>",
                    "new ConcurrentHashMap<String, Quest>",
                    ".loadOnPlatform(",
                    ".enableOnPlatform(",
                    ".startOnPlatform(",
                    ".stopOnPlatform(",
                    ".beginStartup(",
                    ".finishStartup(",
                    ".beginShutdown(");

    @Test
    void neoforgeDoesNotOwnPersistenceOrDatabasePolicy() throws IOException {
        assertNoSourceContains(
                NEOFORGE,
                List.of(
                        "import java.sql.",
                        "import javax.sql.",
                        "import com.zaxxer.hikari.",
                        "import com.notquests.core.persistence.PlayerDatabase",
                        "import com.notquests.core.persistence.CoreYamlPersistence",
                        "import com.notquests.core.persistence.NotQuestsPersistence",
                        "implements NotQuestsPersistence",
                        "new CoreYamlPersistence(",
                        "import com.notquests.core.managers.DataManager",
                        "import com.notquests.core.managers.PlayerDatabase",
                        "new DataManager(",
                        "new PlayerDatabase("),
                "NeoForge may supply platform paths and scheduling, but persistence policy and database access belong in core");
    }

    @Test
    void neoforgeContainsNoPaperPlayerNamesOrRemovedSyncHook() throws IOException {
        assertNoSourceContains(
                NEOFORGE,
                List.of("PaperQuestPlayer", "PaperQuestPlayers", "syncActiveQuestNames("),
                "NeoForge must use the shared core quest-player model without Paper mirrors or removed sync hooks");
    }

    @Test
    void neoforgeDoesNotInstantiateCorePluginStateDirectly() throws IOException {
        final List<String> offenders;
        try (var files = Files.walk(Path.of("src/main/java"))) {
            offenders = files
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(NeoForgeArchitectureTest::containsForbiddenCoreStateOwnership)
                    .map(Path::toString)
                    .sorted()
                    .toList();
        }

        assertTrue(
                offenders.isEmpty(),
                () -> "NeoForge must receive shared NotQuests state from core, not create it directly: " + offenders);
    }

    @Test
    void neoforgeCommandFilesDoNotOwnRegistryCommandSemantics() throws IOException {
        final List<String> forbiddenCommandInternals =
                List.of(
                        "Actions",
                        "Conditions",
                        "Objectives",
                        "Variables",
                        "Variables.BooleanVariableHandler",
                        "Variables.NumberVariableHandler",
                        "Variables.StringVariableHandler",
                        "Chain",
                        "ActiveObjectives",
                        "NotQuestsRegistry.");
        final List<String> offenders;
        try (var files = Files.walk(Path.of("src/main/java"))) {
            offenders = files
                    .filter(path -> path.getFileName().toString().startsWith("NeoForge"))
                    .filter(path -> path.getFileName().toString().endsWith("Commands.java"))
                    .filter(path -> containsAny(path, forbiddenCommandInternals))
                    .map(Path::toString)
                    .sorted()
                    .toList();
        }

        assertTrue(
                offenders.isEmpty(),
                () -> "NeoForge command files should only build Brigadier nodes and call core command handlers: "
                        + offenders);
    }

    @Test
    void neoforgeDoesNotDefinePerFeatureCommandFiles() throws IOException {
        final List<String> offenders;
        try (var files = Files.walk(Path.of("src/main/java"))) {
            offenders = files
                    .filter(path -> path.getFileName().toString().matches("NeoForge.*Commands\\.java"))
                    .map(Path::toString)
                    .sorted()
                    .toList();
        }

        assertTrue(
                offenders.isEmpty(),
                () -> "NeoForge should compile core command layouts, not own per-feature command files: "
                        + offenders);
    }

    @Test
    void neoforgeDoesNotOwnPortableCommandShape() throws IOException {
        final List<String> forbiddenLiteralBranches =
                List.of(
                        "literal(\"version\")",
                        "literal(\"registry\")",
                        "literal(\"objectives\")",
                        "literal(\"actions\")",
                        "literal(\"conditions\")",
                        "literal(\"conversations\")",
                        "literal(\"variables\")",
                        "literal(\"triggers\")",
                        "literal(\"activate\")",
                        "literal(\"execute\")",
                        "literal(\"save\")",
                        "literal(\"executeSaved\")",
                        "literal(\"check\")",
                        "literal(\"set\")");
        final List<String> offenders;
        try (var files = Files.walk(Path.of("src/main/java"))) {
            offenders = files
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> containsAny(path, forbiddenLiteralBranches))
                    .map(Path::toString)
                    .sorted()
                    .toList();
        }

        assertTrue(
                offenders.isEmpty(),
                () -> "NeoForge should compile command branch names from the core command registration: " + offenders);
    }

    @Test
    void neoforgeRegistersUserAndAdminCommandsFromCoreSurface() throws IOException {
        final String source = Files.readString(Path.of("src/main/java/com/notquests/neoforge/NotQuestsNeoForge.java"));

        assertTrue(
                source.contains("private NotQuestsCommands commands;"),
                "NeoForge should keep only the public core NotQuestsCommands surface.");
        assertTrue(
                source.contains("registerCommands(final NotQuestsCommands commands)"),
                "Core should deliver the public NotQuestsCommands surface through the platform lifecycle leaf.");
        assertTrue(
                source.contains("commands.userCommands("),
                "NeoForge should map the core-owned user command graph through NotQuestsCommands.");
        assertTrue(
                source.contains("commands.adminCommands("),
                "NeoForge should map the core-owned admin command graph through NotQuestsCommands.");
        assertTrue(
                source.contains("if (commands == null || commandCompiler == null)"),
                "The initial NeoForge command event runs before core startup and must not read an uninitialized command surface.");
        assertTrue(
                source.contains("registerCommands(currentServer.getCommands().getDispatcher());"),
                "Core commands must be added to the live dispatcher after the NotQuests command surface is ready.");
    }

    @Test
    void neoforgeRegistersCustomArgumentTypesBeforeCommandsCanSyncToClients() throws IOException {
        final String source = Files.readString(Path.of("src/main/java/com/notquests/neoforge/NotQuestsNeoForge.java"));
        final String argumentTypes =
                Files.readString(Path.of("src/main/java/com/notquests/neoforge/NeoForgeArgumentTypes.java"));

        assertTrue(
                source.contains("NeoForgeArgumentTypes.register(modBus);"),
                "NeoForge custom argument types must be registered on the mod bus before clients receive commands.");
        assertTrue(
                argumentTypes.contains("Registries.COMMAND_ARGUMENT_TYPE"),
                "NeoForge custom argument types must be registered in Minecraft's command argument type registry.");
        assertTrue(
                argumentTypes.contains("ArgumentTypeInfos.registerByClass("),
                "NeoForge custom argument type classes must be mapped for ClientboundCommandsPacket serialization.");
    }

    @Test
    void neoforgeRegistersPermissionNodesOnTheRuntimeEventBus() throws IOException {
        final String entrypoint =
                Files.readString(Path.of("src/main/java/com/notquests/neoforge/NotQuestsNeoForge.java"));
        final String permissions =
                Files.readString(Path.of("src/main/java/com/notquests/neoforge/NeoForgePermissions.java"));

        assertTrue(
                entrypoint.contains("NeoForgePermissions.register(CommandManager.permissionDefaults());"),
                "Permission registration should not receive the mod-construction event bus.");
        assertTrue(
                permissions.contains("NeoForge.EVENT_BUS.addListener(NeoForgePermissions::registerNodes);"),
                "PermissionGatherEvent.Nodes is posted on NeoForge's runtime event bus.");
    }

    @Test
    void neoforgeGeneratedCommandJsonComesFromCoreCommandSurface() throws IOException {
        final String source = Files.readString(Path.of("src/main/java/com/notquests/neoforge/NotQuestsNeoForge.java"));
        final String commandSurface =
                Files.readString(Path.of("../core/src/main/java/com/notquests/core/commands/NotQuestsCommands.java"));
        final String commandManager =
                Files.readString(Path.of("../core/src/main/java/com/notquests/core/managers/CommandManager.java"));
        final String plugin =
                Files.readString(Path.of("../core/src/main/java/com/notquests/core/NotQuestsPlugin.java"));

        assertTrue(
                !source.contains("commands.exportGeneratedMetadata()")
                        && plugin.contains("commands.exportGeneratedMetadata()"),
                "Core startup should export command metadata; NeoForge should only register the compiled graph.");
        assertTrue(
                !commandSurface.contains("CommandHandlers"),
                "NotQuestsCommands should not construct a generic command handler dump.");
        assertTrue(
                commandSurface.contains("return exportGeneratedMetadata(commandManager,"),
                "NotQuestsCommands should own generated metadata export directly.");
        assertTrue(
                commandSurface.contains("commandManager.commandIndex(pluginVersion)"),
                "NeoForge commands.json should be generated from the core command index.");
        assertTrue(
                commandManager.contains("new NQCommandSchema.CommandIndex(pluginVersion"),
                "NeoForge commands.json should be generated from registrations owned by core.");
        assertTrue(
                commandSurface.contains("commandManager.metadataIndex(pluginVersion, minecraftVersion)"),
                "NeoForge metadata.json should be generated from the core metadata index.");
    }

    @Test
    void neoforgeCompilesTypedCoreArgumentsToNativeBrigadierTypes() throws IOException {
        final String source =
                Files.readString(Path.of("src/main/java/com/notquests/neoforge/NeoForgeCoreCommandCompiler.java"));

        assertTrue(
                source.contains("case INTEGER -> IntegerArgumentType.integer()"),
                "Core integer arguments should become native Brigadier integer arguments on NeoForge.");
        assertTrue(
                source.contains("case DOUBLE -> DoubleArgumentType.doubleArg()"),
                "Core double arguments should become native Brigadier double arguments on NeoForge.");
        assertTrue(
                source.contains("case BOOLEAN -> StringArgumentType.word()"),
                "Portable boolean aliases should reach core instead of being rejected by NeoForge Brigadier first.");
        assertTrue(
                source.contains("context.getArgument(name, Object.class)"),
                "Core command handlers should receive values from Brigadier-validated arguments, not string-only parsing.");
        assertTrue(
                source.contains("case ITEM_SELECTION, ACTION_LIST -> NeoForgeArguments.commaToken()")
                        && source.contains("commands.suggestions("),
                "NeoForge should only parse comma tokens natively and obtain item-selection suggestions from core.");
        assertTrue(
                source.contains("builder.getRemaining()") && !source.contains("final String raw = argument(context, FLAG_ARG)"),
                "NeoForge flag suggestions should use the current suggestion token, not read an unparsed __flags argument.");
        assertTrue(
                !source.contains("child.aliases()")
                        && !source.contains("hiddenLiteralAlias")
                        && !source.contains("attachEmptyLiteralAlias"),
                "NeoForge must compile only canonical nested literals; aliases are allowed only at command roots.");
    }

    @Test
    void neoforgeValidatesItemSelectionAgainstTheMinecraftItemRegistry() throws IOException {
        final String source =
                Files.readString(Path.of("src/main/java/com/notquests/neoforge/NeoForgeNotQuestsAdapter.java"));
        final String adapter =
                Files.readString(Path.of("../core/src/main/java/com/notquests/core/platform/NotQuestsAdapter.java"));
        final String savedItems =
                Files.readString(Path.of("../core/src/main/java/com/notquests/core/items/SavedItems.java"));

        assertTrue(
                adapter.contains("public final ItemSelection parseItemSelection(final String input)")
                        && source.contains("protected String nativeItemMaterialId(final String input)"),
                "Core must own item-selection parsing while NeoForge supplies only native material validation.");
        assertTrue(
                source.contains("!BuiltInRegistries.ITEM.containsKey(itemId)"),
                "NeoForge item parsing must validate materials against the Minecraft item registry.");
        assertTrue(
                adapter.contains("return SavedItems.parse(input, plugin::savedItem")
                        && savedItems.contains("throw new ParseException(ParseProblem.UNKNOWN_MATERIAL, part);"),
                "Core item parsing should reject a material after NeoForge reports that it is absent from the native registry.");
        assertTrue(
                savedItems.contains("savedItemNames.add(savedItem.getName())")
                        && savedItems.contains("ItemStackSelection.of(materialIds, savedItemNames, exactItems, any, 1)"),
                "Core item parsing must preserve saved custom item names instead of re-parsing them as vanilla materials.");
        assertTrue(
                adapter.contains("itemSelectionBlockMaterial(plugin.resolveItems(savedItem.getItemSelection()))")
                        && source.contains("protected String itemSelectionBlockMaterial(final List<SavedItems.ItemChoice> items)"),
                "Core must resolve saved custom items before NeoForge materializes the resulting native stacks.");
    }

    @Test
    void neoforgeBlockMaterialOptionsMatchSharedItemKeywordsAndSavedItems() throws IOException {
        final String source =
                Files.readString(Path.of("src/main/java/com/notquests/neoforge/NeoForgeNotQuestsAdapter.java"));
        final String shared = Files.readString(
                Path.of("../core/src/main/java/com/notquests/core/platform/NotQuestsAdapter.java"));

        assertTrue(
                shared.contains("options.addAll(plugin.savedItemNames())"),
                "NeoForge block-material suggestions should include saved NotQuests item names like Paper.");
        assertTrue(
                shared.contains("options.add(\"hand\")") && shared.contains("options.add(\"any\")"),
                "NeoForge block-material suggestions should include the shared hand/any keywords.");
        assertTrue(
                shared.contains("plugin.savedItem(materialOrKeyword)")
                        && source.contains("itemSelectionBlockMaterial"),
                "NeoForge block-material resolution should turn saved item selections into a usable block material.");
    }

    @Test
    void neoforgeSeparatesSuccessfulFeedingFromBreeding() throws IOException {
        final String source =
                Files.readString(Path.of("src/main/java/com/notquests/neoforge/NeoForgeObjectiveEvents.java"));
        final int rightClickStart = source.indexOf("private void onRightClickEntity");
        final int leftClickStart = source.indexOf("private void onLeftClickBlock");
        final String rightClickEntity = source.substring(rightClickStart, leftClickStart);
        final String breedEntity = source.substring(
                source.indexOf("private void onBreedEntity"),
                source.indexOf("private void onTameEntity"));

        assertTrue(
                rightClickEntity.contains("final boolean wasInLove = animal.isInLove()")
                        && rightClickEntity.contains("!wasInLove && animal.isAlive() && animal.isInLove()")
                        && rightClickEntity.contains("plugin.playerInteractedWithEntity"),
                "FeedMobs should advance only after the interaction actually puts an animal into love mode.");
        assertTrue(
                !breedEntity.contains("plugin.playerFedEntity") && breedEntity.contains("plugin.playerBredEntity"),
                "The later breeding event must not double-count feeding progress.");
    }

    @Test
    void neoforgeRuntimeDataFolderIsWorldScoped() throws IOException {
        final String source = Files.readString(Path.of("src/main/java/com/notquests/neoforge/NotQuestsNeoForge.java"));

        assertTrue(
                source.contains("currentServer.getWorldPath(LevelResource.ROOT)"),
                "NeoForge must store NotQuests data under the loaded world/save, not an instance-global folder.");
        assertTrue(
                source.contains("notQuestsFolder(currentServer.getWorldPath(LevelResource.ROOT))"),
                "NeoForge data folder resolution should stay with the platform lifecycle owner.");
    }

    @Test
    void neoforgeDelegatesGeneralConfigurationToCore() throws IOException {
        final String source = Files.readString(Path.of("src/main/java/com/notquests/neoforge/NotQuestsNeoForge.java"));

        assertTrue(
                !source.contains("configuration().loadFrom("),
                "General configuration loading belongs in core ConfigurationManager, not the NeoForge adapter.");
        assertTrue(
                source.contains("plugin.load(this)")
                        && source.contains("plugin.enable(this)")
                        && !source.contains("loadGeneralConfig(")
                        && !source.contains("readSharedGeneralConfig(")
                        && !source.contains("loadSharedSettings("),
                "NeoForge should delegate the complete general.yml path to core.");
    }

    @Test
    void neoforgeProductionDoesNotUseCoreCommandHandlersOrEffects() throws IOException {
        final List<String> offenders;
        try (var files = Files.walk(Path.of("src/main/java"))) {
            offenders = files
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> containsAny(
                            path,
                            List.of(
                                    "CommandHandlers",
                                    "CommandEffects",
                                    "PaperCommandEffects",
                                    "NeoForgeCommandEffects")))
                    .map(Path::toString)
                    .sorted()
                    .toList();
        }

        assertTrue(
                offenders.isEmpty(),
                () -> "Command execution handlers and platform hooks live behind the core NotQuestsCommands surface; "
                        + "NeoForge should only compile/adapt commands: "
                        + offenders);
    }

    @Test
    void neoforgeQuestPlayerOverridesLifecycleAndInventoryLeaves() throws IOException {
        final String source = Files.readString(Path.of("src/main/java/com/notquests/neoforge/NeoForgePlayer.java"));
        final List<String> requiredOverrides = List.of(
                "public String displayName()",
                "public boolean setDisplayName(",
                "public List<String> availableGameModes()",
                "public List<String> availableWorldNames()",
                "public List<String> availableBiomeNames()",
                "public String applyExternalPlaceholders(",
                "public List<ItemSelection> inventoryItems()",
                "public List<ItemSelection> enderChestItems()",
                "public boolean beforeQuestAccepted(",
                "public boolean beforeQuestPointsChanged(",
                "public boolean beforeQuestCompleted(",
                "public boolean beforeQuestFailed(",
                "public boolean beforeObjectiveCompleted(",
                "public boolean spawnParticle(",
                "public boolean playSound(",
                "public boolean spawnVanillaMob(");
        final List<String> missing = requiredOverrides.stream()
                .filter(required -> !source.contains(required))
                .toList();

        assertTrue(
                missing.isEmpty(),
                () -> "NeoForgePlayer must explicitly implement shared player leaves; "
                        + "PlatformPlayer defaults must not hide platform parity gaps: "
                        + missing);
        assertTrue(
                source.contains("implements PlatformPlayer"),
                "NeoForgePlayer must remain an adapter implementation, not core QuestPlayer domain state");
        assertTrue(
                source.contains("getDefaultClockTime() % 24_000L"),
                "World-time variables should use the visible day clock, not total game time.");
        assertTrue(
                source.contains("new ClientboundStopSoundPacket(null, null)"),
                "The shared play-sound action's stop-other-sounds flag must be implemented on NeoForge.");
        assertTrue(
                source.contains("yaw == null ? location.yaw() : yaw.floatValue()")
                        && source.contains("pitch == null ? location.pitch() : pitch.floatValue()"),
                "Portable location orientation must survive NeoForge teleports unless the action overrides it.");
    }

    @Test
    void neoforgeAdapterLeavesMatchSharedPaperSemantics() throws IOException {
        final String adapter = Files.readString(
                Path.of("src/main/java/com/notquests/neoforge/NeoForgeNotQuestsAdapter.java"));
        final String player = Files.readString(
                Path.of("src/main/java/com/notquests/neoforge/NeoForgePlayer.java"));
        final String events = Files.readString(
                Path.of("src/main/java/com/notquests/neoforge/NotQuestsEvents.java"));
        final String spawnMobAction = Files.readString(
                Path.of("../builtin/src/main/java/com/notquests/builtin/actions/SpawnMobAction.java"));
        final String playSoundAction = Files.readString(
                Path.of("../builtin/src/main/java/com/notquests/builtin/actions/PlaySoundAction.java"));

        assertTrue(
                adapter.contains("block == null || block == Blocks.AIR")
                        && adapter.contains("block == Blocks.AIR ? null"),
                "A non-block held item must never turn a target block into air.");
        assertTrue(
                adapter.contains("reference.value() != Items.AIR")
                        && adapter.contains("reference.value() == Items.AIR"),
                "AIR must be excluded from item suggestions and rejected by item parsing.");
        assertTrue(
                player.contains("new ClickEvent.RunCommand(")
                        && player.contains("command.startsWith(\"/\") ? command : \"/\" + command"),
                "Client run-command click events require a leading slash.");
        assertTrue(
                player.contains("CommonHooks.onServerChatSubmittedEvent(")
                        && !player.contains("new ServerboundChatPacket("),
                "Programmatic chat must use NeoForge's server-chat event instead of fabricated signed-chat state.");
        assertTrue(
                spawnMobAction.contains("questPlayer.positionY() + 1.0d")
                        && player.contains("public boolean spawnVanillaMob(")
                        && !player.contains("randomOffset(")
                        && adapter.contains("case MAINHAND -> \"HAND\"")
                        && adapter.contains("case OFFHAND -> \"OFF_HAND\""),
                "Core actions should resolve spawn coordinates while NeoForge performs one exact native spawn.");
        assertTrue(
                playSoundAction.contains("final PlatformPlayer.SoundAudience audience")
                        && player.contains("final SoundAudience audience")
                        && !player.contains("playForEveryoneAtSetLocation"),
                "Core actions should resolve sound audience and location while NeoForge only plays the resolved sound.");
        assertTrue(
                player.contains("allowLifecycleEvent(")
                        && adapter.contains("return callOnServerThread(() -> !NeoForge.EVENT_BUS.post(")
                        && adapter.contains("Could not dispatch NeoForge ObjectiveUnlock event")
                        && adapter.contains("return false;"),
                "Cancellable lifecycle events must run synchronously on the server thread and fail closed.");
        assertTrue(
                events.contains("public String questName()")
                        && events.contains("public int objectiveId()")
                        && events.contains("public String objectiveHolderPath()"),
                "NeoForge lifecycle payloads must expose the same nested quest/objective identity as Paper.");
    }

    @Test
    void neoforgeGuiReceivesNpcContextFromCoreActionsAndPreviews() throws IOException {
        final String playerSource = Files.readString(Path.of("src/main/java/com/notquests/neoforge/NeoForgePlayer.java"));
        final String renderer = Files.readString(Path.of("src/main/java/com/notquests/neoforge/NeoForgeGuiRenderer.java"));
        final String entrypoint = Files.readString(Path.of("src/main/java/com/notquests/neoforge/NotQuestsNeoForge.java"));

        assertTrue(
                playerSource.contains("public boolean showGui(")
                        && playerSource.contains("return guiRenderer.open(player, gui, this)"),
                "Core should resolve GUI context and pagination before invoking the NeoForge renderer.");
        assertTrue(
                renderer.contains("final ResolvedGui gui")
                        && !renderer.contains("GuiContext"),
                "NeoForge GUI rendering should consume the complete core-built GUI without owning GUI decisions.");
        assertTrue(
                !containsAny(Path.of("src/main/java/com/notquests/neoforge/NeoForgeGuiRenderer.java"), List.of(
                        "new GuiService(",
                        "GuiService.loadFromFolder(",
                        "GuiDefaults.copyMissing",
                        "GuiLayoutLoader.load(")),
                "NeoForge GUI renderer should render core-built ResolvedGui values, not own GUI configuration loading.");
        assertTrue(
                entrypoint.contains("dataLoad::getAsBoolean"),
                "NeoForge should schedule the core-owned data loader instead of owning data loading.");
        assertTrue(
                !entrypoint.contains("CompletableFuture.delayedExecutor")
                        && entrypoint.contains("executor.shutdownNow()"),
                "NeoForge update checks must use a lifecycle-owned executor that is cancelled on stop.");
    }

    @Test
    void neoforgeGuiUsesResolvedSkullTexturesWithoutNativeItemTooltipNoise() throws IOException {
        final String renderer = Files.readString(
                Path.of("src/main/java/com/notquests/neoforge/NeoForgeGuiRenderer.java"));

        assertTrue(
                renderer.contains("new PropertyMap(ImmutableMultimap.of(\"textures\", texture))")
                        && renderer.contains("new GameProfile(")
                        && renderer.contains("ResolvableProfile.createResolved(profile)")
                        && !renderer.contains("profile.properties().put("),
                "NeoForge GUI skulls must construct a populated profile instead of mutating Authlib's immutable empty property map.");
        assertTrue(
                renderer.contains("TooltipDisplay.DEFAULT.withHidden(DataComponents.ATTRIBUTE_MODIFIERS, true)"),
                "GUI icons must hide native equipment attributes so only the core-configured name and lore are shown.");
    }

    @Test
    void onlyNeoForgeCoreCompilerTranslatesCoreCommandsToNativeBrigadier() throws IOException {
        final List<String> forbiddenNativeCommandConstruction =
                List.of("Commands.literal(", "Commands.argument(", "builder.then(");
        final List<String> offenders;
        try (var files = Files.walk(Path.of("src/main/java"))) {
            offenders = files
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> !path.endsWith(Path.of("com/notquests/neoforge/NeoForgeCoreCommandCompiler.java")))
                    .filter(path -> containsAny(path, forbiddenNativeCommandConstruction))
                    .map(Path::toString)
                    .sorted()
                    .toList();
        }

        assertTrue(
                offenders.isEmpty(),
                () -> "NeoForge command branches must be translated from core registrations only by "
                        + "NeoForgeCoreCommandCompiler: "
                        + offenders);
    }

    @Test
    void neoforgeDoesNotOwnDisconnectTriggerFlow() throws IOException {
        final Path objectiveEvents = Path.of("src/main/java/com/notquests/neoforge/NeoForgeObjectiveEvents.java");

        assertTrue(
                !containsAny(objectiveEvents, List.of("\"DISCONNECT\"")),
                "Player disconnect lifecycle flow belongs in core NotQuestsPlugin.playerDisconnected. "
                        + "NeoForge should only report the platform logout event.");
    }

    @Test
    void neoforgeDoesNotOwnWorldChangeTriggerFlow() throws IOException {
        final Path objectiveEvents = Path.of("src/main/java/com/notquests/neoforge/NeoForgeObjectiveEvents.java");

        assertTrue(
                !containsAny(objectiveEvents, List.of("\"WORLDENTER\"", "\"WORLDLEAVE\"")),
                "World-change lifecycle flow belongs in core NotQuestsPlugin.playerChangedWorld. "
                        + "NeoForge should only report the platform dimension-change event.");
    }

    @Test
    void neoforgeDoesNotCallStringBasedPlayerTriggerEvents() throws IOException {
        final List<String> offenders;
        try (var files = Files.walk(Path.of("src/main/java/com/notquests/neoforge"))) {
            offenders = files
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> containsAny(path, List.of("triggerPlayerEvent(")))
                    .map(Path::toString)
                    .sorted()
                    .toList();
        }

        assertTrue(
                offenders.isEmpty(),
                () -> "Platform events should call typed core lifecycle methods like playerDied, "
                        + "playerDisconnected, or playerChangedWorld; string trigger dispatch stays inside core: "
                        + offenders);
    }

    @Test
    void neoforgeDoesNotOwnObjectiveProgress() throws IOException {
        final List<String> offenders;
        try (var files = Files.walk(Path.of("src/main/java/com/notquests/neoforge"))) {
            offenders = files
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> containsAny(path, List.of("progressEngine()")))
                    .map(Path::toString)
                    .sorted()
                    .toList();
        }

        assertTrue(
                offenders.isEmpty(),
                () -> "NeoForge should report platform events through NotQuestsPlugin, not call core engine internals: "
                        + offenders);
        final String beamRenderer = Files.readString(
                Path.of("src/main/java/com/notquests/neoforge/NeoForgeClientBeamRenderer.java"));
        assertTrue(
                beamRenderer.contains("ActiveObjectives.clientBeam("),
                "NeoForge should render the beam geometry selected by the core ActiveObjectives owner.");
    }

    @Test
    void neoforgeRuntimeEventIdsMatchSuggestedRegistryIds() throws IOException {
        final String adapter = Files.readString(Path.of("src/main/java/com/notquests/neoforge/NeoForgeNotQuestsAdapter.java"));
        final String events = Files.readString(Path.of("src/main/java/com/notquests/neoforge/NeoForgeObjectiveEvents.java"));

        assertTrue(
                adapter.contains(".map(NeoForgeNotQuestsAdapter::suggestionId)"),
                "Damage, entity, and enchantment suggestions should use the shared vanilla-path/modded-namespaced ID form.");
        assertTrue(
                events.contains("damageTypeId(event.getSource())") && !events.contains("event.getSource().getMsgId()"),
                "Death objective runtime events should report the damage type registry ID, not only the death-message id.");
        assertTrue(
                events.contains("suggestionId(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()))"),
                "Entity objective runtime events should keep namespaces for modded entity types.");
        assertTrue(
                events.contains(".map(resourceKey -> suggestionId(resourceKey.identifier()))"),
                "Enchant objective runtime events should keep namespaces for modded enchantments.");
    }

    @Test
    void neoforgeNpcAttachmentsDoNotOwnUserFacingAttachmentText() throws IOException {
        final Path source = Path.of("src/main/java/com/notquests/neoforge/NeoForgeNpcAttachments.java");

        assertTrue(
                !containsAny(source, List.of(
                        "attached to <highlight2>",
                        "removed from <highlight2>",
                        "No NotQuests quests are attached",
                        "Quests attached to <highlight>",
                        "Removed attached conversations")),
                "NPC attachment feedback text belongs in core NPC helpers. "
                        + "NeoForge should only detect armor-stand clicks and send returned core text.");
    }

    @Test
    void neoforgeNpcAttachmentsDoNotMirrorPersistentQuestAttachmentState() throws IOException {
        final Path source = Path.of("src/main/java/com/notquests/neoforge/NeoForgeNpcAttachments.java");

        assertTrue(
                !containsAny(source, List.of(
                        "questAttachments",
                        "Map<String, List<Attachment>>",
                        "private record Attachment",
                        "plugin.syncQuestNpcAttachment(",
                        "plugin.removeQuestNpcAttachment(",
                        "plugin.syncConversationNpcAttachment(",
                        "plugin.removeConversationNpcAttachments(")),
                "Persistent NPC attachment state belongs in core. NeoForge may keep pending click selections, "
                        + "but click tools and check/list behavior must go through core attachment operations.");
    }

    @Test
    void neoforgeNpcAttachmentsUseCoreConversationLookup() throws IOException {
        final String source = Files.readString(Path.of("src/main/java/com/notquests/neoforge/NeoForgeNpcAttachments.java"));

        assertTrue(
                source.contains("plugin.playerInteractedWithArmorStand("),
                "NeoForge armor-stand conversation activation should be delegated to core, not reimplemented in the adapter.");
        assertTrue(
                !source.contains("plugin.conversationNames()") && !source.contains("plugin.conversationNpcAttachments("),
                "NeoForge should not iterate raw conversation attachment state to decide NPC conversation activation.");
    }

    @Test
    void neoforgeContainsNoManagerLayer() throws IOException {
        final List<String> offenders;
        try (var files = Files.walk(NEOFORGE)) {
            offenders = files
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> path.toString().replace('\\', '/').contains("/managers/")
                            || path.getFileName().toString().endsWith("Manager.java"))
                    .map(Path::toString)
                    .sorted()
                    .toList();
        }
        assertTrue(
                offenders.isEmpty(),
                () -> "NeoForge is an adapter library and must not recreate a manager layer: " + offenders);
    }

    @Test
    void neoforgeCommandCompilerDoesNotOwnHintPolicy() throws IOException {
        final String compiler = Files.readString(
                NEOFORGE.resolve("com/notquests/neoforge/NeoForgeCoreCommandCompiler.java"));

        assertTrue(
                List.of("CommandHintRenderer", "showCommandHints", "pushHint(")
                        .stream().noneMatch(compiler::contains),
                "NeoForge's compiler must only translate the core command graph and suggestions");
    }

    @Test
    void neoforgeBeamRenderingOwnsNoMarkerLifecycleOrCadence() throws IOException {
        final String tracker = Files.readString(
                NEOFORGE.resolve("com/notquests/neoforge/NeoForgeBeamTracker.java"));
        final String events = Files.readString(
                NEOFORGE.resolve("com/notquests/neoforge/NeoForgeObjectiveEvents.java"));
        assertTrue(
                tracker.contains("boolean render(")
                        && List.of("temporary", "expire", "ticks %", "refreshInterval", "resend")
                                .stream().noneMatch(value -> tracker.toLowerCase(java.util.Locale.ROOT)
                                        .contains(value.toLowerCase(java.util.Locale.ROOT)))
                        && !events.contains("ServerTickEvent"),
                "Core owns permanent/temporary marker state and refresh cadence; NeoForge only reconciles native rendering");
    }

    @Test
    void neoforgeRendersCoreOwnedArmorStandIndicators() throws IOException {
        final String entrypoint = Files.readString(
                NEOFORGE.resolve("com/notquests/neoforge/NotQuestsNeoForge.java"));
        final String attachments = Files.readString(
                NEOFORGE.resolve("com/notquests/neoforge/NeoForgeNpcAttachments.java"));

        assertTrue(
                entrypoint.contains("public Optional<NpcIndicatorRenderer> npcIndicatorRenderer(")
                        && entrypoint.contains("public Optional<NpcTextVisibility> textVisibility()")
                        && entrypoint.contains("return Optional.empty();")
                        && entrypoint.contains("npcAttachments.renderArmorStandIndicator(npcId, indicator)")
                        && attachments.contains("renderArmorStandIndicator("),
                "Armor stands are a shared Minecraft capability; NeoForge must render the same core-owned indicator plan as Paper");
        assertTrue(
                !attachments.contains("npcIndicator("),
                "NeoForge's indicator leaf may materialize particles but must not decide attachment visibility or cadence");
    }

    @Test
    void neoforgeAdvertisesOnlyImplementedOptionalCapabilities() throws IOException {
        final String entrypoint = Files.readString(
                NEOFORGE.resolve("com/notquests/neoforge/NotQuestsNeoForge.java"));

        assertTrue(
                entrypoint.contains("public Optional<PacketBridge> packetBridge(")
                        && entrypoint.contains("public Optional<NativeIntegrations> nativeIntegrations()")
                        && entrypoint.contains("public Optional<MetricsBridge> metricsBridge()"),
                "NeoForge must implement the optional capability API explicitly instead of returning nullable bridges");
        assertTrue(
                entrypoint.contains("public Optional<NpcIndicatorRenderer> npcIndicatorRenderer(final String npcType)")
                        && entrypoint.contains("if (!\"armorstand\".equalsIgnoreCase(npcType))"),
                "NeoForge should advertise only its implemented armor-stand indicator renderer");
    }

    @Test
    void neoforgeJournalUsesCorePolicyAndNativeEventLeaves() throws IOException {
        final String entrypoint = Files.readString(
                NEOFORGE.resolve("com/notquests/neoforge/NotQuestsNeoForge.java"));
        final String events = Files.readString(
                NEOFORGE.resolve("com/notquests/neoforge/NeoForgeObjectiveEvents.java"));
        final String menuMixin = Files.readString(
                NEOFORGE.resolve("com/notquests/neoforge/mixin/ContainerMenuMixin.java"));

        assertTrue(
                entrypoint.contains("private volatile ItemStack journalItem = ItemStack.EMPTY")
                        && entrypoint.contains("DataComponents.CUSTOM_NAME")
                        && entrypoint.contains("DataComponents.LORE")
                        && !entrypoint.contains("createMigratedPaperJournal(")
                        && !entrypoint.contains("__notquestsType"),
                "NeoForge must materialize the canonical journal and leave all released Paper decoding in the 6.3 migration");
        assertTrue(
                events.contains("plugin.journalPlayerJoined(")
                        && events.contains("plugin.journalItemUsed(")
                        && events.contains("plugin.journalPlayerRespawned(")
                        && events.contains("plugin.journalPlayerDied(")
                        && events.contains("plugin.journalItemPickedUpOrDropped(")
                        && events.contains("plugin.journalInventoryClicked("),
                "NeoForge journal events must report native facts through the existing core-owned journal decisions");
        assertTrue(
                events.contains("ItemEntityPickupEvent.Pre")
                        && events.contains("event.setCanPickup(TriState.FALSE)")
                        && events.contains("player.getInventory().add(restored)")
                        && events.contains("player.containerMenu.setCarried(restored)")
                        && events.contains("LivingDropsEvent")
                        && menuMixin.contains("NeoForgeObjectiveEvents.onContainerClicked("),
                "NeoForge must use the cancellable event phase and restore a toss that NeoForge removes before cancellation");
    }

    private static boolean containsForbiddenCoreStateOwnership(final Path path) {
        return containsAny(path, FORBIDDEN_CORE_STATE_OWNERSHIP);
    }

    private static void assertNoSourceContains(
            final Path root,
            final List<String> forbidden,
            final String message) throws IOException {
        final List<String> offenders;
        try (var files = Files.walk(root)) {
            offenders = files
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> containsAny(path, forbidden))
                    .map(Path::toString)
                    .sorted()
                    .toList();
        }
        assertTrue(offenders.isEmpty(), () -> message + ": " + offenders);
    }

    private static boolean containsAny(final Path path, final List<String> snippets) {
        try {
            final String source = Files.readString(path);
            return snippets.stream().anyMatch(source::contains);
        } catch (final IOException exception) {
            throw new IllegalStateException("Could not read " + path, exception);
        }
    }
}
