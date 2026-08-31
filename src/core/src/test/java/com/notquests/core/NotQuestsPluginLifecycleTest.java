package com.notquests.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.notquests.core.commands.NotQuestsCommands;
import com.notquests.core.conversation.ConversationManager;
import com.notquests.core.managers.LogManager.ConsoleLine;
import com.notquests.core.NotQuestsPlatform.IntegrationPlugin;
import com.notquests.core.NotQuestsPlatform.NativeIntegration;
import com.notquests.core.NotQuestsPlatform.NativeIntegrations;
import com.notquests.core.platform.NotQuestsAdapter;
import com.notquests.core.platform.PlatformPlayer;
import com.notquests.core.registry.NotQuestsRegistry;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BooleanSupplier;

class NotQuestsPluginLifecycleTest {
  @TempDir Path tempDir;
  @Test
  void ownsQuestAndPlayerStateByIdentifier() {
    final NotQuestsPlugin plugin = NotQuestsPlugin.create();

    assertSame(plugin.getOrCreateQuest("QuestA"), plugin.getOrCreateQuest("QuestA"));
    assertSame(plugin.questPlayer("player-1", "default"), plugin.questPlayer("player-1", "default"));
  }

  @Test
  void ownsPlatformLifecycleState() {
    final NotQuestsPlugin plugin = NotQuestsPlugin.create();
    final TestPlatform platform = new TestPlatform(plugin, "Paper", tempDir);

    plugin.load(platform);

    assertEquals("Paper", plugin.platformName());
    assertTrue(plugin.isStarting());
    assertFalse(plugin.isLoaded());
    assertFalse(plugin.isShuttingDown());
    assertTrue(plugin.startupStartedAtMillis() > 0);

    plugin.enable(platform);

    assertFalse(plugin.isStarting());
    assertTrue(plugin.isLoaded());
    assertTrue(plugin.startupFinishedAtMillis() >= plugin.startupStartedAtMillis());

    plugin.stop(platform);

    assertFalse(plugin.isLoaded());
    assertTrue(plugin.isShuttingDown());
    assertTrue(plugin.shutdownStartedAtMillis() >= plugin.startupStartedAtMillis());
    assertEquals(1, platform.registerCommandCalls);
    assertEquals(1, platform.loadDataCalls);
    assertEquals(1, platform.loadedEventCalls);
    assertEquals(1, platform.stopCalls);
  }

  @Test
  void canOwnSinglePhasePlatformStartup() {
    final NotQuestsPlugin plugin = NotQuestsPlugin.create();
    final TestPlatform platform = new TestPlatform(plugin, "NeoForge", tempDir);

    plugin.start(platform);

    assertEquals("NeoForge", plugin.platformName());
    assertFalse(plugin.isStarting());
    assertTrue(plugin.isLoaded());
    assertEquals(1, platform.registerCommandCalls);
    assertEquals(1, platform.loadDataCalls);
    assertEquals(1, platform.loadedEventCalls);
  }

  @Test
  void doesNotFinishStartupWhenDataLoadingFails() {
    final NotQuestsPlugin plugin = NotQuestsPlugin.create();
    final TestPlatform platform = new TestPlatform(plugin, "Paper", tempDir);
    platform.dataLoaded = CompletableFuture.completedFuture(false);

    plugin.load(platform);
    plugin.enable(platform);

    assertFalse(plugin.isStarting());
    assertFalse(plugin.isLoaded());
    assertEquals(1, platform.loadDataCalls);
    assertEquals(0, platform.loadedEventCalls);
  }

  @Test
  void remainsStartingUntilAsynchronousDataLoadingCompletes() {
    final NotQuestsPlugin plugin = NotQuestsPlugin.create();
    final TestPlatform platform = new TestPlatform(plugin, "Paper", tempDir);
    platform.dataLoaded = new CompletableFuture<>();

    plugin.load(platform);
    plugin.enable(platform);

    assertTrue(plugin.isStarting());
    assertFalse(plugin.isLoaded());
    assertEquals(0, platform.loadedEventCalls);

    platform.dataLoaded.complete(true);

    assertFalse(plugin.isStarting());
    assertTrue(plugin.isLoaded());
    assertEquals(1, platform.loadedEventCalls);
  }

  @Test
  void ignoresDataLoadingCompletionAfterShutdown() {
    final NotQuestsPlugin plugin = NotQuestsPlugin.create();
    final TestPlatform platform = new TestPlatform(plugin, "NeoForge", tempDir);
    platform.dataLoaded = new CompletableFuture<>();

    plugin.start(platform);
    plugin.stop(platform);
    platform.dataLoaded.complete(true);

    assertFalse(plugin.isStarting());
    assertFalse(plugin.isLoaded());
    assertTrue(plugin.isShuttingDown());
    assertEquals(0, platform.loadedEventCalls);
    assertEquals(0, platform.platformThreadCalls);
  }

  @Test
  void integrationLifecycleUsesConfiguredNameWhenPluginReportsAnAlias() {
    final NotQuestsPlugin plugin = NotQuestsPlugin.create();
    final TestPlatform platform = new TestPlatform(plugin, "Paper", tempDir);
    final AtomicInteger registeredEvents = new AtomicInteger();
    final AtomicInteger dataLoaded = new AtomicInteger();
    final AtomicInteger closed = new AtomicInteger();
    platform.nativeIntegrations = Optional.of(new NativeIntegrations() {
      @Override
      public List<NativeIntegration> integrations() {
        return List.of(
            new NativeIntegration(
                "FancyNpcs",
                () -> new IntegrationPlugin("FancyNpcs", "2.8.0", true),
                Optional.of(() -> false),
                Optional.of(registeredEvents::incrementAndGet),
                Optional.of(dataLoaded::incrementAndGet),
                Optional.of(closed::incrementAndGet)),
            new NativeIntegration(
                "WorldEdit",
                () -> new IntegrationPlugin("FastAsyncWorldEdit", "2.15.5", true),
                Optional.empty(),
                Optional.of(registeredEvents::incrementAndGet),
                Optional.of(dataLoaded::incrementAndGet),
                Optional.of(closed::incrementAndGet)));
      }

      @Override
      public boolean spawnMythicMob(final String entityType, final com.notquests.core.platform.NQLocation location) {
        return false;
      }

      @Override
      public boolean spawnEcoMob(final String entityType, final com.notquests.core.platform.NQLocation location) {
        return false;
      }
    });

    plugin.start(platform);

    assertTrue(plugin.integrationEnabled("WorldEdit"));
    assertFalse(plugin.integrationEnabled("FancyNpcs"));
    assertEquals(1, registeredEvents.get());
    assertEquals(1, dataLoaded.get());

    plugin.stop(platform);

    assertEquals(1, closed.get());
  }

  private static final class TestPlatform implements NotQuestsPlatform {
    private final String name;
    private final Path dataFolder;
    private final NotQuestsAdapter adapter;
    private int registerCommandCalls;
    private int loadDataCalls;
    private int loadedEventCalls;
    private int stopCalls;
    private int platformThreadCalls;
    private CompletableFuture<Boolean> dataLoaded = CompletableFuture.completedFuture(true);
    private Optional<NativeIntegrations> nativeIntegrations = Optional.empty();

    private TestPlatform(
        final NotQuestsPlugin plugin,
        final String name,
        final Path dataFolder) {
      this.name = name;
      this.dataFolder = dataFolder;
      adapter = plugin.createRegistryAdapter(
          new NotQuestsRegistry.PlatformHooks(null, null, null));
    }

    @Override
    public String platformName() {
      return name;
    }

    @Override
    public Path dataFolder() {
      return dataFolder;
    }

    @Override
    public String pluginVersion() {
      return "7.0.0";
    }

    @Override
    public String serverVersion() {
      return "26.2";
    }

    @Override
    public NotQuestsAdapter adapter() {
      return adapter;
    }

    @Override
    public void writeConsole(final ConsoleLine line) {}

    @Override
    public void scheduleAction(final Duration delay, final Runnable action) {}

    @Override
    public void scheduleAsync(final Duration delay, final Runnable action) {}

    @Override
    public PlatformPlayer createPlayer(final String playerIdentifier) {
      return null;
    }

    @Override
    public List<String> onlinePlayerIdentifiers() {
      return List.of();
    }

    @Override
    public ConversationManager.Display conversationDisplay() {
      return (player, message) -> {};
    }

    @Override
    public boolean materializeJournalItem(
        final com.notquests.core.managers.ConfigurationManager.JournalItem journalItem) {
      return true;
    }

    @Override
    public java.util.Optional<PacketBridge> packetBridge(final boolean enabled, final boolean usePacketEvents) {
      return java.util.Optional.empty();
    }

    @Override
    public java.util.Optional<NativeIntegrations> nativeIntegrations() {
      return nativeIntegrations;
    }

    @Override
    public void registerPlatformEvents() {}

    @Override
    public double currentTps() { return 20.0; }

    @Override
    public java.util.Optional<NpcIndicatorRenderer> npcIndicatorRenderer(final String npcType) {
      return java.util.Optional.empty();
    }

    @Override
    public void registerCommands(final NotQuestsCommands commands) {
      registerCommandCalls++;
    }

    @Override
    public CompletionStage<Boolean> loadData(final BooleanSupplier dataLoad) {
      loadDataCalls++;
      return dataLoaded;
    }

    @Override
    public void runOnServerThread(final Runnable action) {
      platformThreadCalls++;
      action.run();
    }

    @Override
    public java.util.Optional<MetricsBridge> metricsBridge() {
      return java.util.Optional.empty();
    }

    @Override
    public void publishLoadedEvent() {
      loadedEventCalls++;
    }

    @Override
    public void closePlatform() {
      stopCalls++;
    }
  }
}
