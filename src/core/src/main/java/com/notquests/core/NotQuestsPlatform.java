package com.notquests.core;

import com.notquests.core.commands.NotQuestsCommands;
import com.notquests.core.conversation.ConversationManager;
import com.notquests.core.managers.ConfigurationManager;
import com.notquests.core.managers.LogManager.ConsoleLine;
import com.notquests.core.npc.NQNPCID;
import com.notquests.core.npc.NpcAttachments;
import com.notquests.core.platform.NQLocation;
import com.notquests.core.platform.NotQuestsAdapter;
import com.notquests.core.platform.PlatformPlayer;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/** Native facts and effects used by the core-owned NotQuests lifecycle. */
public interface NotQuestsPlatform {
    String platformName();

    Path dataFolder();

    String pluginVersion();

    String serverVersion();

    NotQuestsAdapter adapter();

    void writeConsole(ConsoleLine line);

    void scheduleAction(Duration delay, Runnable action);

    void scheduleAsync(Duration delay, Runnable action);

    PlatformPlayer createPlayer(String playerIdentifier);

    List<String> onlinePlayerIdentifiers();

    ConversationManager.Display conversationDisplay();

    boolean materializeJournalItem(ConfigurationManager.JournalItem journalItem);

    Optional<PacketBridge> packetBridge(boolean enabled, boolean usePacketEvents);

    Optional<NativeIntegrations> nativeIntegrations();

    void registerPlatformEvents();

    double currentTps();

    Optional<NpcIndicatorRenderer> npcIndicatorRenderer(String npcType);

    void registerCommands(NotQuestsCommands commands);

    CompletionStage<Boolean> loadData(BooleanSupplier dataLoad);

    void runOnServerThread(Runnable action);

    Optional<MetricsBridge> metricsBridge();

    void publishLoadedEvent();

    void closePlatform();

    interface PacketBridge {
        void start();

        void close();
    }

    interface NativeIntegrations {
        List<NativeIntegration> integrations();

        boolean spawnMythicMob(String entityType, NQLocation location);

        boolean spawnEcoMob(String entityType, NQLocation location);
    }

    record NativeIntegration(
            String name,
            Supplier<IntegrationPlugin> plugin,
            Optional<BooleanSupplier> enable,
            Optional<Runnable> registerEvents,
            Optional<Runnable> dataLoaded,
            Optional<Runnable> close) {
        public NativeIntegration {
            name = Objects.requireNonNull(name, "name");
            plugin = Objects.requireNonNull(plugin, "plugin");
            enable = Objects.requireNonNull(enable, "enable");
            registerEvents = Objects.requireNonNull(registerEvents, "registerEvents");
            dataLoaded = Objects.requireNonNull(dataLoaded, "dataLoaded");
            close = Objects.requireNonNull(close, "close");
        }
    }

    interface NpcIndicatorRenderer {
        Optional<NpcTextVisibility> textVisibility();

        void render(
                NQNPCID npcId,
                NpcAttachments.Indicator indicator,
                Set<String> visiblePlayerIdentifiers);
    }

    @FunctionalInterface
    interface NpcTextVisibility {
        List<String> nearbyPlayerIdentifiers(NQNPCID npcId, double radius);
    }

    @FunctionalInterface
    interface MetricsBridge {
        void start(NotQuestsPlugin.Metrics metrics);
    }

    record IntegrationPlugin(String name, String version, boolean enabled) {
        public IntegrationPlugin {
            name = name == null ? "" : name;
            version = version == null ? "" : version;
        }
    }
}
