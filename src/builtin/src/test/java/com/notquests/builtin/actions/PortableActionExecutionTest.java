package com.notquests.builtin.actions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.notquests.builtin.BuiltInPack;
import com.notquests.builtin.TestPlatformPlayer;
import com.notquests.core.NotQuestsPlugin;
import com.notquests.core.actions.Action;
import com.notquests.core.items.ItemStackSelection;
import com.notquests.core.platform.NQLocation;
import com.notquests.core.platform.NotQuestsAdapter;
import com.notquests.core.platform.PlatformPlayer;
import com.notquests.core.registry.NotQuestsRegistry.Actions;
import com.notquests.core.registry.NotQuestsRegistry;
import com.notquests.core.registry.fields.RegistryField;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;

class PortableActionExecutionTest {
  @Test
  void vanillaPlayerActionsCallThePlatformPlayer() {
    final NotQuestsRegistry registry = new NotQuestsRegistry();
    BuiltInPack.register(adapterWithSoundCategories(registry));
    final RecordingPlayer player = new RecordingPlayer();

    execute(registry, "SendMessage", data -> data.setValue("message", "Hello"), player);
    execute(registry, "ShowActionBar", data -> data.setValue("message", "Look here"), player);
    execute(registry, "ShowTitle", data -> data.setValue("title", "Quest started"), player);
    execute(registry, "CloseInventory", data -> {}, player);
    execute(registry, "GiveItem", data -> {
      data.setValue("material", ItemStackSelection.parse("acacia_boat"));
      data.setValue("amount", 2);
    }, player);
    execute(registry, "Teleport", data -> data.setValue("location", NQLocation.at("overworld", 1, 2, 3)), player);
    execute(registry, "SpawnParticle", data -> {
      data.setValue("particle", "happy_villager");
      data.setValue("count", 3);
      data.setValue("usePlayerLocation", true);
    }, player);
    execute(registry, "PlaySound", data -> {
      data.setValue("sound", "entity_player_levelup");
      data.setValue("stopOtherSounds", true);
      data.setValue("playForEveryoneAtTheirLocation", true);
    }, player);
    execute(registry, "SpawnMob", data -> {
      data.setValue("entityType", "zombie");
      data.setValue("amount", 2);
      data.setValue("usePlayerLocation", true);
    }, player);
    execute(registry, "Beam", data -> {
      data.setValue("beamName", "target");
      data.setValue("location", NQLocation.at("overworld", 9, 65, 9));
    }, player);
    execute(registry, "Beam", data -> {
      data.setValue("beamName", "target");
      data.setValue("remove", true);
    }, player);
    execute(registry, "OpenGui", data -> {
      data.setValue("guiName", "quests");
      data.setValue("quest", "TestQuest");
    }, player);

    assertEquals("Hello", player.lastMessage);
    assertEquals("Look here", player.lastActionBar);
    assertEquals("Quest started", player.lastTitle);
    assertEquals("acacia_boat", player.lastGivenItems.listedMaterials(""));
    assertEquals(2, player.lastGivenAmount);
    assertEquals("overworld", player.lastTeleport.worldName());
    assertEquals("happy_villager", player.lastParticle);
    assertEquals(0.0d, player.lastParticleLocation.x());
    assertEquals("entity_player_levelup", player.lastSound);
    assertEquals("master", player.lastSoundCategory);
    assertTrue(player.stoppedSounds);
    assertEquals(PlatformPlayer.SoundAudience.EVERYONE_AT_OWN_LOCATION, player.lastSoundAudience);
    assertEquals("zombie", player.lastSpawnedMob);
    assertEquals(2, player.spawnedMobCount);
    assertEquals("target", player.lastShownBeam);
    assertEquals("target", player.lastRemovedBeam);
    assertEquals("main-base", player.lastOpenedGui);
    assertTrue(player.closedInventory);
  }

  @Test
  void openGuiSingleLineParserUsesSharedFlagParsing() {
    final NotQuestsRegistry registry = new NotQuestsRegistry();
    BuiltInPack.register(registry.createAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null)));
    final Actions.Type openGui = action(registry, "OpenGui");
    final Action data = new Action(0, "OpenGui", null);

    openGui.singleLineParser().parse(
        data::setValue,
        List.of("quests", "--quest", "DailyQuest", "--npc", "42", "--category", "daily"));

    assertEquals("quests", data.text("guiName"));
    assertEquals("DailyQuest", data.text("quest"));
    assertEquals(42, data.integer("npc", -1));
    assertEquals("daily", data.text("category"));
  }

  @Test
  void openGuiSingleLineParserReportsMissingGuiNameCleanly() {
    final NotQuestsRegistry registry = new NotQuestsRegistry();
    BuiltInPack.register(registry.createAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null)));
    final Actions.Type openGui = action(registry, "OpenGui");
    final Actions.Draft draft = (name, value) -> {};

    assertThrows(IllegalArgumentException.class, () -> openGui.singleLineParser().parse(draft, List.of()));
  }

  @Test
  void commandActionsNormalizeLeadingSlashInSingleLineParsers() {
    final NotQuestsRegistry registry = new NotQuestsRegistry();
    BuiltInPack.register(registry.createAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null)));
    final Action console = new Action(0, "ConsoleCommand", null);
    final Action player = new Action(0, "PlayerCommand", null);

    action(registry, "ConsoleCommand").singleLineParser().parse(console::setValue, List.of("say", "hello"));
    action(registry, "PlayerCommand").singleLineParser().parse(player::setValue, List.of("spawn"));

    assertEquals("/say hello", console.text("command"));
    assertEquals("/spawn", player.text("command"));
  }

  @Test
  void variableActionsKeepVariableFieldsAndFlagsInCoreParser() {
    final NotQuestsPlugin plugin = NotQuestsPlugin.create();
    final NotQuestsRegistry registry = plugin.registry();
    final NotQuestsAdapter adapter = plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
    final String[] capturedZone = new String[1];
    final String[] capturedFlag = new String[1];
    final Number[] capturedValue = new Number[1];
    adapter.variables()
        .numberVariable("ScopedNumber")
        .displayName("Scoped Number")
        .description("Test number variable with extra arguments.")
        .singular("Scoped Number")
        .plural("Scoped Numbers")
        .field("zone", adapter.fields().text(), "Zone key.")
        .field("inverted", adapter.fields().presenceFlag(), "Whether the zone is inverted.")
        .get(context -> 2)
        .set((newValue, context) -> {
          capturedValue[0] = newValue;
          capturedZone[0] = context.text("zone");
          capturedFlag[0] = context.text("inverted");
          return true;
        })
        .register();
    NumberAction.register(plugin, adapter);
    final Actions.Type actionType = action(registry, "Number");

    final Action data =
        Actions.parse(adapter, actionType, "ScopedNumber spawn add 5 --inverted");
    assertEquals("add", data.text("operator"));
    assertEquals("5", data.text("expression"));
    actionType.executor().execute(data, new RecordingPlayer());

    assertEquals(7.0, capturedValue[0].doubleValue());
    assertEquals("spawn", capturedZone[0]);
    assertEquals("true", capturedFlag[0]);
  }

  @Test
  void numberAndBooleanVariableActionsEvaluateExpressionsAndTypedArguments() {
    final NotQuestsPlugin plugin = NotQuestsPlugin.create();
    final NotQuestsRegistry registry = plugin.registry();
    final NotQuestsAdapter adapter = plugin.createRegistryAdapter(
        new NotQuestsRegistry.PlatformHooks(null, null, null));
    final double[] numberValue = new double[1];
    final boolean[] booleanValue = new boolean[1];
    final double[] argumentValue = new double[1];

    adapter.variables()
        .numberVariable("BaseNumber")
        .displayName("Base Number")
        .description("Expression input.")
        .singular("Base Number")
        .plural("Base Numbers")
        .get((questPlayer, objects) -> 5)
        .register();
    adapter.variables()
        .numberVariable("ExpressionArgument")
        .displayName("Expression Argument")
        .description("Returns its expression argument.")
        .singular("Expression Argument")
        .plural("Expression Arguments")
        .field("factor", adapter.fields().numberExpression(), "Expression argument.")
        .get(context -> context.number("factor", 0))
        .register();
    adapter.variables()
        .numberVariable("NumberTarget")
        .displayName("Number Target")
        .description("Settable number target.")
        .singular("Number Target")
        .plural("Number Targets")
        .get((questPlayer, objects) -> numberValue[0])
        .set((newValue, questPlayer, objects) -> {
          numberValue[0] = newValue.doubleValue();
          return true;
        })
        .register();
    adapter.variables()
        .numberVariable("ArgumentTarget")
        .displayName("Argument Target")
        .description("Settable target with an expression argument.")
        .singular("Argument Target")
        .plural("Argument Targets")
        .field("factor", adapter.fields().numberExpression(), "Expression argument.")
        .get(context -> 0)
        .set((newValue, context) -> {
          argumentValue[0] = context.number("factor", 0);
          return true;
        })
        .register();
    adapter.variables()
        .booleanVariable("BooleanTarget")
        .displayName("Boolean Target")
        .description("Settable boolean target.")
        .singular("Boolean Target")
        .plural("Boolean Targets")
        .get((questPlayer, objects) -> booleanValue[0])
        .set((newValue, questPlayer, objects) -> {
          booleanValue[0] = newValue;
          return true;
        })
        .register();
    NumberAction.register(plugin, adapter);
    BooleanAction.register(plugin, adapter);
    final RecordingPlayer player = new RecordingPlayer();

    execute(registry, "Number", data -> {
      data.setValue("variableName", "NumberTarget");
      data.setValue("operator", "set");
      data.setValue("expression", "5 + 3");
    }, player);
    assertEquals(8.0d, numberValue[0]);

    execute(registry, "Number", data -> {
      data.setValue("variableName", "NumberTarget");
      data.setValue("operator", "set");
      data.setValue("expression", "BaseNumber + ExpressionArgument(factor:2+1)");
    }, player);
    assertEquals(8.0d, numberValue[0]);

    execute(registry, "Number", data -> {
      data.setValue("variableName", "ArgumentTarget");
      data.setValue("operator", "set");
      data.setValue("expression", "1");
      data.setValue("additionalNumbers", java.util.Map.of("factor", "BaseNumber + 1"));
    }, player);
    assertEquals(6.0d, argumentValue[0]);

    execute(registry, "Boolean", data -> {
      data.setValue("variableName", "BooleanTarget");
      data.setValue("operator", "set");
      data.setValue("expression", "0.98");
    }, player);
    assertTrue(booleanValue[0]);

    execute(registry, "Boolean", data -> {
      data.setValue("variableName", "BooleanTarget");
      data.setValue("operator", "set");
      data.setValue("expression", "0.97");
    }, player);
    assertEquals(false, booleanValue[0]);
  }

  @Test
  void playSoundKeepsOldWorldFlagNameWithWorldNameConfigPath() {
    final NotQuestsRegistry registry = new NotQuestsRegistry();
    BuiltInPack.register(registry.createAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null)));
    final Actions.Type playSound = action(registry, "PlaySound");
    final RegistryField.Definition worldFlag = playSound.flags().stream()
        .filter(flag -> flag.configPath().equals("specifics.worldName"))
        .findFirst()
        .orElseThrow();

    assertEquals("world", worldFlag.name());
  }

  @Test
  void playSoundRejectsUnknownCategoriesBeforeCallingThePlatform() {
    final NotQuestsRegistry registry = new NotQuestsRegistry();
    BuiltInPack.register(adapterWithSoundCategories(registry));
    final RecordingPlayer player = new RecordingPlayer();

    execute(registry, "PlaySound", data -> {
      data.setValue("sound", "entity_player_levelup");
      data.setValue("SoundCategory", "not-a-category");
      data.setValue("stopOtherSounds", true);
    }, player);

    assertNull(player.lastSound);
    assertEquals(false, player.stoppedSounds);
  }

  @Test
  void questActionsMutateCoreQuestState() {
    final NotQuestsPlugin plugin = NotQuestsPlugin.create();
    final NotQuestsAdapter adapter = plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
    BuiltInPack.register(plugin, adapter);
    plugin.getOrCreateQuest("DailyQuest");
    final RecordingPlayer player = new RecordingPlayer();

    execute(plugin.registry(), "GiveQuest", data -> data.setValue("quest", "DailyQuest"), player);
    assertEquals(List.of("DailyQuest"), plugin.questPlayer("player-1", "default").getActiveQuestIdentifiers().stream().toList());
    assertTrue(player.messages.stream().anyMatch(message -> message.contains("[Quest Accepted]")));

    execute(plugin.registry(), "CompleteQuest", data -> data.setValue("quest", "DailyQuest"), player);
    assertEquals(List.of(), plugin.questPlayer("player-1", "default").getActiveQuestIdentifiers().stream().toList());
    assertEquals(List.of("DailyQuest"), plugin.questPlayer("player-1", "default").getCompletedQuests().stream()
        .map(com.notquests.core.structs.QuestPlayer.CompletedQuest::questIdentifier)
        .toList());
    assertEquals(List.of(true), player.forcedQuestCompletions);
  }

  @Test
  void forcedGiveQuestActionIsSilentButKeepsTheAcceptEvent() {
    final NotQuestsPlugin plugin = NotQuestsPlugin.create();
    final NotQuestsAdapter adapter = plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
    BuiltInPack.register(plugin, adapter);
    plugin.getOrCreateQuest("HiddenQuestInfo");
    final RecordingPlayer player = new RecordingPlayer();

    execute(plugin.registry(), "GiveQuest", data -> {
      data.setValue("quest", "HiddenQuestInfo");
      data.setValue("forceGive", true);
    }, player);

    assertEquals(List.of("HiddenQuestInfo"),
        plugin.questPlayer("player-1", "default").getActiveQuestIdentifiers().stream().toList());
    assertTrue(player.messages.isEmpty());
    assertEquals(List.of(true), player.acceptQuestTriggerFlags);
  }

  @Test
  void actionChainExecutesSavedActionsFromCoreRegistry() {
    final NotQuestsPlugin plugin = NotQuestsPlugin.create();
    final NotQuestsAdapter adapter = plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
    BuiltInPack.register(plugin, adapter);
    final Action message = new Action(0, "SendMessage", null);
    message.setValue("message", "Core-owned action chain");
    plugin.saveAction("Notify", action(plugin.registry(), "SendMessage"), message);
    final RecordingPlayer player = new RecordingPlayer();

    execute(plugin.registry(), "Action", data -> {
      data.setValue("actions", "Notify");
      data.setValue("amount", 1);
      data.setValue("executedActionDelay", Duration.ofMillis(-1));
    }, player);

    assertEquals("Core-owned action chain", player.lastMessage);
  }

  private static void execute(
      final NotQuestsRegistry registry,
      final String id,
      final java.util.function.Consumer<Action> setup,
      final RecordingPlayer player) {
    final Actions.Type action = action(registry, id);
    final Action data = new Action(0, id, null);
    setup.accept(data);
    action.executor().execute(data, player);
  }

  private static Actions.Type action(final NotQuestsRegistry registry, final String id) {
    return registry.actions().stream()
        .filter(candidate -> candidate.id().equals(id))
        .findFirst()
        .orElseThrow();
  }

  private static NotQuestsAdapter adapterWithSoundCategories(final NotQuestsRegistry registry) {
    final NotQuestsAdapter delegate =
        registry.createAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
    return (NotQuestsAdapter) java.lang.reflect.Proxy.newProxyInstance(
        NotQuestsAdapter.class.getClassLoader(),
        new Class<?>[] {NotQuestsAdapter.class},
        (proxy, method, arguments) -> method.getName().equals("soundCategoryIds")
            ? List.of("master", "music")
            : method.invoke(delegate, arguments));
  }

  private static final class RecordingPlayer implements TestPlatformPlayer {
    private String lastMessage;
    private final java.util.ArrayList<String> messages = new java.util.ArrayList<>();
    private String lastActionBar;
    private String lastTitle;
    private ItemStackSelection lastGivenItems;
    private int lastGivenAmount;
    private NQLocation lastTeleport;
    private String lastParticle;
    private String lastSound;
    private String lastSoundCategory;
    private String lastSpawnedMob;
    private NQLocation lastParticleLocation;
    private PlatformPlayer.SoundAudience lastSoundAudience;
    private boolean stoppedSounds;
    private int spawnedMobCount;
    private String lastShownBeam;
    private String lastRemovedBeam;
    private String lastOpenedGui;
    private boolean closedInventory;
    private final java.util.ArrayList<Boolean> acceptQuestTriggerFlags = new java.util.ArrayList<>();
    private final java.util.ArrayList<Boolean> forcedQuestCompletions = new java.util.ArrayList<>();

    @Override
    public boolean hasPlayer() {
      return true;
    }

    @Override
    public String playerName() {
      return "player-1";
    }

    @Override
    public String playerIdentifier() {
      return "player-1";
    }

    @Override
    public long currentWorldTimeTicks() {
      return 0;
    }

    @Override
    public void sendMessage(final String miniMessage) {
      messages.add(miniMessage);
      lastMessage = miniMessage;
    }

    @Override
    public void sendActionBar(final String miniMessage) {
      lastActionBar = miniMessage;
    }

    @Override
    public void showProgressBossBar(final String miniMessage, final double progress) {}

    @Override
    public void hideProgressBossBar() {}

    @Override
    public void showTitle(
        final String title,
        final String subtitle,
        final java.time.Duration fadeIn,
        final java.time.Duration stay,
        final java.time.Duration fadeOut) {
      lastTitle = title;
    }

    @Override
    public void chat(final String message) {}

    @Override
    public void performCommand(final String command) {}

    @Override
    public void closeInventory() {
      closedInventory = true;
    }
    @Override
    public boolean beforeQuestAccepted(
        final com.notquests.core.structs.Quest quest,
        final boolean triggerAcceptQuestTrigger) {
      acceptQuestTriggerFlags.add(triggerAcceptQuestTrigger);
      return true;
    }

    @Override
    public boolean beforeQuestCompleted(
        final com.notquests.core.structs.Quest quest,
        final boolean forced) {
      forcedQuestCompletions.add(forced);
      return true;
    }

    @Override
    public boolean giveItems(
        final java.util.List<com.notquests.core.items.SavedItems.ItemChoice> items) {
      final com.notquests.core.items.ItemSelection selection = items.getFirst().selection();
      lastGivenItems = (ItemStackSelection) selection;
      lastGivenAmount = selection.amount();
      return true;
    }

    @Override
    public boolean teleport(final NQLocation location, final Double yaw, final Double pitch) {
      lastTeleport = location;
      return true;
    }

    @Override
    public boolean spawnParticle(
        final String particleId,
        final int count,
        final boolean showToEveryone,
        final NQLocation location,
        final double offsetX,
        final double offsetY,
        final double offsetZ,
        final double speed) {
      lastParticle = particleId;
      lastParticleLocation = location;
      return true;
    }

    @Override
    public void stopSounds() {
      stoppedSounds = true;
    }

    @Override
    public boolean playSound(
        final String soundId,
        final PlatformPlayer.SoundAudience audience,
        final NQLocation location,
        final double volume,
        final double pitch,
        final String soundCategory) {
      lastSound = soundId;
      lastSoundAudience = audience;
      lastSoundCategory = soundCategory;
      return true;
    }

    @Override
    public boolean spawnVanillaMob(
        final String entityType,
        final NQLocation location) {
      lastSpawnedMob = entityType;
      spawnedMobCount++;
      return true;
    }

    @Override
    public boolean renderObjectiveMarkers(
        final java.util.Map<String, NQLocation> markers,
        final boolean useBeaconBlocks,
        final boolean force) {
      if (markers.isEmpty()) {
        lastRemovedBeam = lastShownBeam;
      } else {
        lastShownBeam = markers.keySet().iterator().next();
      }
      return true;
    }

    @Override
    public NQLocation lookingAtBlock(final double maxDistance) {
      return null;
    }

    @Override
    public boolean showGui(final com.notquests.core.gui.GuiService.ResolvedGui gui) {
      lastOpenedGui = gui.id();
      return true;
    }
  }
}
