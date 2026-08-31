package com.notquests.paper;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

class PaperArchitectureTest {
  private static final Path PAPER = Path.of("src/main/java");
  private static final Path CORE = Path.of("../core/src/main/java");
  private static final Path BUILTIN = Path.of("../builtin/src/main/java");

  @Test
  void coreConceptOwnersStayOutOfPaper() throws IOException {
    final List<Path> removed = List.of(
        PAPER.resolve("com/notquests/paper/managers/DataManager.java"),
        PAPER.resolve("com/notquests/paper/managers/LanguageManager.java"),
        PAPER.resolve("com/notquests/paper/managers/LogManager.java"),
        PAPER.resolve("com/notquests/paper/managers/QuestManager.java"),
        PAPER.resolve("com/notquests/paper/managers/PaperQuestPlayerManager.java"),
        PAPER.resolve("com/notquests/paper/managers/UpdateManager.java"),
        PAPER.resolve("com/notquests/paper/managers/UtilManager.java"),
        PAPER.resolve("com/notquests/paper/managers/CommandManager.java"),
        PAPER.resolve("com/notquests/paper/managers/PaperQuestPlayers.java"),
        PAPER.resolve("com/notquests/paper/managers/tags/TagManager.java"),
        PAPER.resolve("com/notquests/paper/adapter/PaperPersistence.java"),
        PAPER.resolve("com/notquests/paper/PaperData.java"),
        PAPER.resolve("com/notquests/paper/PaperTranslations.java"),
        PAPER.resolve("com/notquests/paper/PaperLogger.java"),
        PAPER.resolve("com/notquests/paper/PaperText.java"),
        PAPER.resolve("com/notquests/paper/adapter/quest/Quest.java"),
        PAPER.resolve("com/notquests/paper/adapter/quest/ActiveQuest.java"),
        PAPER.resolve("com/notquests/paper/adapter/quest/ActiveObjective.java"),
        PAPER.resolve("com/notquests/paper/adapter/quest/ActiveObjectiveHolder.java"),
        PAPER.resolve("com/notquests/paper/adapter/quest/PaperQuest.java"),
        PAPER.resolve("com/notquests/paper/adapter/quest/PaperActiveQuest.java"),
        PAPER.resolve("com/notquests/paper/adapter/quest/PaperActiveObjective.java"),
        PAPER.resolve("com/notquests/paper/adapter/quest/PaperQuestPlayer.java"),
        PAPER.resolve("com/notquests/paper/adapter/objectives/Objective.java"),
        PAPER.resolve("com/notquests/paper/adapter/objectives/ObjectiveHolder.java"),
        PAPER.resolve("com/notquests/paper/adapter/config/PaperYamlConfigurations.java"),
        PAPER.resolve("com/notquests/paper/adapter/config/PaperYamlValueCodec.java"));

    assertTrue(
        removed.stream().noneMatch(Files::exists),
        () -> "Core owns persistence, players, quests, objectives, tags, translations, and logging; "
            + "Paper recreated: "
            + removed.stream().filter(Files::exists).toList());
  }

  @Test
  void paperDoesNotOwnPersistenceOrDatabasePolicy() throws IOException {
    assertNoSourceContains(
        PAPER,
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
        "Paper may schedule platform work, but persistence policy, YAML loading, and database access belong in core");
  }

  @Test
  void paperSchedulesCoreDataLoadingOffTheServerThread() throws IOException {
    final String entrypoint = Files.readString(PAPER.resolve("com/notquests/paper/NotQuests.java"));

    assertTrue(
        entrypoint.contains("runTaskAsynchronously(main")
            && entrypoint.contains("dataLoad.getAsBoolean()")
            && entrypoint.contains("runOnServerThread"),
        "Paper should only schedule the core-owned data loader asynchronously and return startup completion to its server thread");
    assertTrue(
        !entrypoint.contains("corePlugin.loadData()"),
        "Paper must not own or synchronously invoke the core data-loading flow");
  }

  @Test
  void paperEntrypointsOnlyComposeNativeLeavesAroundTheCoreLifecycle() throws IOException {
    final String pluginEntrypoint = Files.readString(PAPER.resolve("com/notquests/Main.java"));
    final String paper = Files.readString(PAPER.resolve("com/notquests/paper/NotQuests.java"));

    assertTrue(
        pluginEntrypoint.contains("notQuests.onLoad();")
            && pluginEntrypoint.contains("notQuests.onEnable();")
            && pluginEntrypoint.contains("notQuests.onDisable();")
            && !containsAny(pluginEntrypoint, List.of("PaperLib", "getServer().getVersion()")),
        "The JavaPlugin entrypoint should only translate Bukkit lifecycle callbacks");
    assertTrue(
        !containsAny(paper, List.of(
            "loadPlatform(",
            "preparePlatform(",
            "registerPlatformContent(",
            "finishPlatformStart(",
            "stopPlatform(",
            "exportGeneratedMetadata()")),
        "Shared startup and shutdown sequencing must stay in NotQuestsPlugin");
  }

  @Test
  void paperOnlyMaterializesCoreOwnedMetricsAndJournalChoices() throws IOException {
    final String plugin = Files.readString(CORE.resolve("com/notquests/core/NotQuestsPlugin.java"));
    final String configuration = Files.readString(
        CORE.resolve("com/notquests/core/managers/ConfigurationManager.java"));
    final String paper = Files.readString(PAPER.resolve("com/notquests/paper/NotQuests.java"));

    assertTrue(
        plugin.contains("public Metrics metrics()")
            && plugin.contains("new Metrics(12824, counts, types)")
            && !containsAny(paper, List.of(
                "new Metrics(main, 12824)",
                "corePlugin.questCount()",
                "corePlugin.objectiveTypeCounts()")),
        "Core should own the bStats id, chart names, and live metric values; Paper only maps them to bStats");
    assertTrue(
        configuration.contains("record JournalItem(")
            && configuration.contains("JournalItem.read(")
            && plugin.contains("if (!platform.materializeJournalItem(journalItem))")
            && plugin.contains("journalItem = invalidJournalItem(")
            && paper.contains("public boolean materializeJournalItem(")
            && !paper.contains("Material.ENCHANTED_BOOK"),
        "Core should parse the journal and choose its fallback; Paper only creates the Bukkit ItemStack");
    assertTrue(
        paper.contains("line.downsampleColors()")
            && !containsAny(paper, List.of(
                "consoleColorsEnabled()",
                "consoleColorsDownsampled()",
                "consolePrefixPrefix()")),
        "Core LogManager should choose console formatting; Paper only renders the prepared line");
  }

  @Test
  void paperItemConversionHasOneNativeOwnerOutsideCommands() throws IOException {
    final Path removedWrapper = PAPER.resolve(
        "com/notquests/paper/commands/arguments/wrappers/ItemStackSelection.java");
    final String items = Files.readString(PAPER.resolve("com/notquests/paper/PaperItems.java"));
    final String adapter = Files.readString(
        PAPER.resolve("com/notquests/paper/PaperNotQuestsAdapter.java"));

    assertTrue(!Files.exists(removedWrapper), "Native item conversion must not masquerade as a command type");
    assertTrue(
        items.contains("public static final class Selection")
            && adapter.contains("import com.notquests.paper.PaperItems.Selection;"),
        "PaperItems.Selection should be the single Bukkit/core item conversion owner");
  }

  @Test
  void paperYamlCodecOnlyConvertsNativeBukkitValues() throws IOException {
    final String codec = Files.readString(
        PAPER.resolve("com/notquests/paper/adapter/config/BukkitConfigurationValueCodec.java"));

    assertTrue(
        !codec.contains("org.bukkit.Location")
            && !codec.contains("locationFromYamlValue")
            && !codec.contains("locationToYamlValue"),
        "Released Bukkit locations belong exclusively in the 6.3 one-off migration");
  }

  @Test
  void paperBuiltinsOnlyComposePortableAndIntegrationSpecificRegistrations() throws IOException {
    final String builtins = Files.readString(
        PAPER.resolve("com/notquests/paper/builtin/PaperBuiltins.java"));

    assertTrue(
        builtins.contains("BuiltInPack.register(main.getCorePlugin(), main.getRegistryAdapter());")
            && !builtins.contains("registerTypes(")
            && !builtins.contains("Registering actions")
            && !builtins.contains("registerVariableObjectives"),
        "The portable built-in registry belongs to BuiltInPack; Paper may only add capability-gated integration types");
  }

  @Test
  void npcAttachmentIntegrationCallsAreDispatchedThroughThePlatformThreadLeaf() throws IOException {
    final String core = Files.readString(CORE.resolve("com/notquests/core/NotQuestsPlugin.java"));
    final String adapter = Files.readString(
        PAPER.resolve("com/notquests/paper/PaperNotQuestsAdapter.java"));

    assertTrue(
        core.contains("runtimeAdapter().callOnServerThread(() ->")
            && core.contains("applyNpcAttachmentsOnPlatformThread()"),
        "Core must enter the platform thread before applying quest/conversation NPC attachments");
    assertTrue(
        adapter.contains("Bukkit.getScheduler().callSyncMethod(main.getMain(), action).get()"),
        "Paper's atomic platform-thread leaf must synchronously marshal attachment work to Bukkit");
  }

  @Test
  void objectiveUnlockEventIsAnAtomicCancellablePlatformGate() throws IOException {
    final String core = Files.readString(CORE.resolve("com/notquests/core/NotQuestsPlugin.java"));
    final String progress = Files.readString(CORE.resolve("com/notquests/core/structs/ActiveObjectives.java"));
    final String adapter = Files.readString(
        PAPER.resolve("com/notquests/paper/PaperNotQuestsAdapter.java"));
    final String event = Files.readString(
        PAPER.resolve("com/notquests/paper/events/notquests/ObjectiveUnlockEvent.java"));

    assertTrue(
        core.contains("runtimeAdapter().allowObjectiveUnlock(")
            && progress.contains("if (!unlockHandler.test(progress))")
            && progress.contains("progress.lock();"),
        "A cancelled Paper event must keep the core objective locked before unlock effects run");
    assertTrue(
        adapter.contains("new ObjectiveUnlockEvent(")
            && adapter.contains("return !event.isCancelled();")
            && adapter.contains("return callOnServerThread(() ->"),
        "Paper must synchronously emit ObjectiveUnlockEvent through one atomic adapter leaf");
    assertTrue(
        event.contains("super(false)")
            && event.contains("objectivePath.clone()"),
        "The event is synchronous and must not expose its mutable objective path array");
  }

  @Test
  void paperAdapterLeavesUseTheSharedPortableSemantics() throws IOException {
    final String adapter = Files.readString(
        PAPER.resolve("com/notquests/paper/PaperNotQuestsAdapter.java"));
    final String player = Files.readString(
        PAPER.resolve("com/notquests/paper/PaperPlayer.java"));

    assertTrue(
        adapter.contains("resolvePaperPlayer(questPlayer)")
            && adapter.contains("Could not dispatch ObjectiveUnlockEvent")
            && adapter.contains("return false;"),
        "Objective unlock events must resolve wrapped players and fail closed when dispatch is impossible");
    assertTrue(
        player.contains("public String displayName()")
            && player.contains("setCustomNameVisible(")
            && adapter.contains("case \"MAINHAND\" -> \"HAND\"")
            && adapter.contains("case \"OFFHAND\" -> \"OFF_HAND\""),
        "Display-name and equipment-slot behavior must share one portable meaning with NeoForge");
  }

  @Test
  void paperGuiOnlyRendersCoreOwnedRegistryActions() throws IOException {
    final String gui = Files.readString(CORE.resolve("com/notquests/core/gui/GuiService.java"));
    final String plugin = Files.readString(CORE.resolve("com/notquests/core/NotQuestsPlugin.java"));
    final String renderer = Files.readString(PAPER.resolve("com/notquests/paper/gui/PaperGuiRenderer.java"));

    assertTrue(
        gui.contains("propertyLines(button, \"conditions\")")
            && gui.contains("GuiAction.registryAction(")
            && plugin.contains("executeRegistryActionLine(")
            && plugin.contains("checkRegistryCondition("),
        "Core must parse and gate arbitrary registered GUI actions and conditions");
    assertTrue(
        renderer.contains("runGuiActions(")
            && renderer.contains("slot.empty()")
            && renderer.contains("Material.matchMaterial(slot.material())")
            && !containsAny(
                renderer,
                List.of(
                    "NotQuestsRegistry.Actions",
                    "NotQuestsRegistry.Conditions",
                    "PlaceholderAPI",
                    "Material.AIR",
                    "isItem() ?",
                    "gray_stained_glass_pane")),
        "Paper GUI rendering may forward clicks and materialize validated core choices, but must not "
            + "parse actions, conditions, placeholders, or own material fallback policy");
    assertTrue(
        gui.contains("withValidMaterials(")
            && gui.contains("Invalid GUI item material '")
            && gui.contains("INVALID_MATERIAL_FALLBACK"),
        "Core GuiService must own GUI material validation, warnings, and fallback selection");
  }

  @Test
  void paperContainsNoRemovedQuestMirrorsOrPlayerSyncHook() throws IOException {
    assertNoSourceContains(
        PAPER,
        List.of(
            "PaperQuestPlayer",
            "PaperQuestPlayers",
            "syncActiveQuestNames("),
        "Paper must use core quest/player/objective state directly");
  }

  @Test
  void platformPlayerOnlyExposesAtomicPlatformTextCapability() throws IOException {
    final String platformPlayer = Files.readString(
        CORE.resolve("com/notquests/core/platform/PlatformPlayer.java"));
    final String paperPlayer = Files.readString(
        PAPER.resolve("com/notquests/paper/PaperPlayer.java"));

    assertTrue(
        !platformPlayer.contains("String translate(")
            && !platformPlayer.contains("resolvePlaceholders(")
            && !platformPlayer.contains("String profile()"),
        "Core owns translation, placeholders, and active profiles; adapters only expose atomic platform capabilities");
    assertTrue(
        platformPlayer.contains("applyExternalPlaceholders("),
        "PlatformPlayer should retain the atomic external-placeholder platform leaf");
    assertTrue(
        paperPlayer.contains("import com.notquests.core.platform.PlatformPlayer;")
            && paperPlayer.contains("implements PlatformPlayer"),
        "PaperPlayer must remain an adapter implementation, not core QuestPlayer domain state");
  }

  @Test
  void platformCodeDoesNotOwnCoreCommandHandlers() throws IOException {
    final Path compiler = PAPER.resolve(
        "com/notquests/paper/commands/brigadier/PaperCoreCommandCompiler.java");
    final List<String> offenders = new ArrayList<>();
    for (final Path file : javaFiles(PAPER)) {
      if (file.equals(compiler)) {
        continue;
      }
      final String source = Files.readString(file);
      if (containsAny(source, List.of(
          "new AdminEditCommands(",
          "new NotQuestsCommands(",
          "new DebugCommands(",
          "new CoreQuest",
          "new CommandManager(",
          "PaperCommandEffects",
          "NeoForgeCommandEffects"))) {
        offenders.add(file.toString());
      }
    }
    assertTrue(
        offenders.isEmpty(),
        () -> "Paper may compile the core command graph, but must not own handlers/effects: " + offenders);
  }

  @Test
  void paperCommandsOnlyContainPlatformArgumentsAndCompiler() throws IOException {
    final Path commands = PAPER.resolve("com/notquests/paper/commands");
    final List<String> offenders = javaFiles(commands).stream()
        .filter(path -> {
          final String relative = commands.relativize(path).toString().replace('\\', '/');
          return !relative.startsWith("arguments/") && !relative.startsWith("brigadier/");
        })
        .map(Path::toString)
        .toList();

    assertTrue(
        offenders.isEmpty(),
        () -> "Command definitions and handlers belong in core; Paper keeps only arguments/compiler: "
            + offenders);
  }

  @Test
  void paperContainsNoManagerLayer() throws IOException {
    final List<String> offenders = javaFiles(PAPER).stream()
        .filter(path -> path.toString().replace('\\', '/').contains("/managers/")
            || path.getFileName().toString().endsWith("Manager.java"))
        .map(Path::toString)
        .toList();

    assertTrue(
        offenders.isEmpty(),
        () -> "Paper is an adapter library and must not recreate a manager layer: " + offenders);

    final List<String> declarations = new ArrayList<>();
    for (final Path file : javaFiles(PAPER)) {
      final String source = Files.readString(file);
      if (source.matches("(?s).*\\b(class|interface|record)\\s+\\w*Manager\\b.*")) {
        declarations.add(file.toString());
      }
    }
    assertTrue(
        declarations.isEmpty(),
        () -> "Paper adapter types must be named for their native capability, not as managers: "
            + declarations);
  }

  @Test
  void sharedPlatformContractsRequireExplicitParity() throws IOException {
    for (final String relative : List.of(
        "com/notquests/core/platform/PlatformPlayer.java",
        "com/notquests/core/platform/NotQuestsAdapter.java",
        "com/notquests/core/NotQuestsPlatform.java")) {
      final String source = Files.readString(CORE.resolve(relative));
      assertTrue(
          !source.matches("(?s).*\\bdefault\\s+[A-Za-z].*"),
          () -> relative + " must not hide a missing platform capability behind a default method");
    }
  }

  @Test
  void coreOwnsRecurringRuntimeAndNpcIndicatorPolicy() throws IOException {
    final String plugin = Files.readString(CORE.resolve("com/notquests/core/NotQuestsPlugin.java"));
    final String questEvents = Files.readString(PAPER.resolve("com/notquests/paper/events/QuestEvents.java"));
    final String armorStands = Files.readString(PAPER.resolve("com/notquests/paper/npc/PaperArmorStands.java"));
    final String fancyNpcs = Files.readString(
        PAPER.resolve("com/notquests/paper/integrations/fancynpcs/FancyNPCsIntegration.java"));
    final String citizensTrait = Files.readString(
        PAPER.resolve("com/notquests/paper/integrations/citizens/QuestGiverNPCTrait.java"));

    assertTrue(
        plugin.contains("scheduleRuntimeSecond(")
            && plugin.contains("startNpcIndicators(")
            && plugin.contains("synchronizeJobsLevels()"),
        "Core must own recurring objective, NPC-indicator, and integration synchronization policy");
    assertTrue(
        !containsAny(questEvents, List.of("scheduleSyncRepeatingTask", "questRuntimeSecondPassed(")),
        "QuestEvents should only translate Bukkit events, not own the shared runtime clock");
    assertTrue(
        !containsAny(armorStands, List.of("runTaskTimer", "scheduleSyncRepeatingTask", "npcIndicator("))
            && !containsAny(fancyNpcs, List.of("runTaskTimer", "scheduleSyncRepeatingTask", "npcIndicator(")),
        "Paper NPC integrations may render one prepared indicator but must not own its cadence or policy");
    assertTrue(
        !containsAny(citizensTrait, List.of("void run()", "particleTimer", "nameTagTimer", "npcIndicator(")),
        "The Citizens trait is only a native marker/lifecycle hook; core owns indicator cadence and visibility policy");
  }

  @Test
  void paperCommandCompilerOnlyTranslatesTheCoreGraph() throws IOException {
    final String compiler = Files.readString(
        PAPER.resolve("com/notquests/paper/commands/brigadier/PaperCoreCommandCompiler.java"));
    final String commands = Files.readString(
        CORE.resolve("com/notquests/core/commands/NotQuestsCommands.java"));

    assertTrue(
        !containsAny(compiler, List.of("CommandHintRenderer", "showCommandHints", "pushHint(")),
        "Paper's compiler must not decide command hint policy or text");
    assertTrue(
        commands.contains("showHint("),
        "The core command surface must own command hint decisions");
  }

  @Test
  void npcAttachmentPolicyUsesOneAtomicNativeTraitLeaf() throws IOException {
    final String contract = Files.readString(
        CORE.resolve("com/notquests/core/platform/NotQuestsAdapter.java"));
    final String plugin = Files.readString(CORE.resolve("com/notquests/core/NotQuestsPlugin.java"));

    assertTrue(
        contract.contains("boolean setNpcQuestGiver(NpcSelection selection, boolean enabled);")
            && !containsAny(contract, List.of(
                "applyConversationNpcAttachment",
                "applyQuestNpcAttachment",
                "applyQuestNpcDetachments")),
        "Adapters should expose one native NPC trait effect, not quest/conversation attachment flows");
    assertTrue(
        plugin.contains("attachQuestNpc(")
            && plugin.contains("attachConversationNpc(")
            && plugin.contains("applyNpcDetachments("),
        "Core must own attachment validation, state synchronization, saving, and detachment flow");
  }

  @Test
  void coreAndBuiltinDoNotImportPlatformApis() throws IOException {
    final List<String> forbidden = List.of(
        "import org.bukkit.",
        "import io.papermc.",
        "import net.neoforged.",
        "import net.minecraft.",
        "import net.citizensnpcs.",
        "import me.clip.placeholderapi.");
    assertNoSourceContains(CORE, forbidden, "Core must remain platform-neutral");
    assertNoSourceContains(BUILTIN, forbidden, "Builtins must remain platform-neutral");
  }

  private static void assertNoSourceContains(
      final Path root,
      final List<String> forbidden,
      final String message) throws IOException {
    final List<String> offenders = new ArrayList<>();
    for (final Path file : javaFiles(root)) {
      final String source = Files.readString(file);
      if (containsAny(source, forbidden)) {
        offenders.add(file.toString());
      }
    }
    assertTrue(offenders.isEmpty(), () -> message + ": " + offenders);
  }

  private static List<Path> javaFiles(final Path root) throws IOException {
    if (!Files.exists(root)) {
      return List.of();
    }
    try (var files = Files.walk(root)) {
      return files.filter(path -> path.toString().endsWith(".java")).sorted().toList();
    }
  }

  private static boolean containsAny(final String source, final List<String> snippets) {
    return snippets.stream().anyMatch(source::contains);
  }
}
