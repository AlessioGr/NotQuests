package com.notquests.core;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import com.notquests.core.actions.Action;
import com.notquests.core.actions.SavedActions.ActionCondition;
import com.notquests.core.actions.SavedActions.ActionSettings;
import com.notquests.core.actions.SavedActions.Chain;
import com.notquests.core.actions.SavedActions.ChainRunner;
import com.notquests.core.actions.SavedActions;
import com.notquests.core.commands.NotQuestsCommands;
import com.notquests.core.commands.framework.CommandMessage;
import com.notquests.core.conditions.Condition;
import com.notquests.core.conditions.ConditionCheck.Result;
import com.notquests.core.conditions.ConditionCheck;
import com.notquests.core.config.CategoryFiles;
import com.notquests.core.config.YamlConfig;
import com.notquests.core.conversation.ConversationManager;
import com.notquests.core.conversation.Speaker;
import com.notquests.core.gui.GuiContext;
import com.notquests.core.gui.GuiService.GuiAction;
import com.notquests.core.gui.GuiService.ResolvedGui;
import com.notquests.core.gui.GuiService;
import com.notquests.core.items.ItemSelection;
import com.notquests.core.items.ItemStackSelection;
import com.notquests.core.items.SavedItem;
import com.notquests.core.items.SavedItems;
import com.notquests.core.managers.BackupManager;
import com.notquests.core.managers.CommandManager;
import com.notquests.core.managers.ConfigurationManager;
import com.notquests.core.managers.DataManager.ReloadTarget;
import com.notquests.core.managers.DataManager;
import com.notquests.core.managers.LanguageManager;
import com.notquests.core.managers.LogManager.ConsoleLine;
import com.notquests.core.managers.LogManager;
import com.notquests.core.managers.QuestManager;
import com.notquests.core.managers.QuestPlayerManager;
import com.notquests.core.managers.UpdateManager;
import com.notquests.core.managers.UtilManager;
import com.notquests.core.managers.PlayerDatabase;
import com.notquests.core.managers.integrations.IntegrationsManager;
import com.notquests.core.managers.tags.TagManager.Tag;
import com.notquests.core.managers.tags.TagManager;
import com.notquests.core.managers.tags.TagType;
import com.notquests.core.migrations.ConfigurationMigrations;
import com.notquests.core.npc.ArmorStandAttachments;
import com.notquests.core.npc.NQNPCID;
import com.notquests.core.npc.NpcAttachments.Detachment;
import com.notquests.core.npc.NpcAttachments.Detachments;
import com.notquests.core.npc.NpcAttachments.NpcAttachment;
import com.notquests.core.npc.NpcAttachments;
import com.notquests.core.objectives.Objective;
import com.notquests.core.platform.NQLocation;
import com.notquests.core.platform.NotQuestsAdapter;
import com.notquests.core.platform.PlatformPlayer;
import com.notquests.core.registry.NotQuestsRegistry.Actions;
import com.notquests.core.registry.NotQuestsRegistry.Conditions;
import com.notquests.core.registry.NotQuestsRegistry.Objectives.Kinds;
import com.notquests.core.registry.NotQuestsRegistry.Objectives;
import com.notquests.core.registry.NotQuestsRegistry.Pack;
import com.notquests.core.registry.NotQuestsRegistry.Packs;
import com.notquests.core.registry.NotQuestsRegistry.Triggers;
import com.notquests.core.registry.NotQuestsRegistry;
import com.notquests.core.registry.fields.RegistryField;
import com.notquests.core.structs.ActiveObjective;
import com.notquests.core.structs.ActiveObjectives;
import com.notquests.core.structs.Category;
import com.notquests.core.structs.PredefinedProgressOrder;
import com.notquests.core.structs.Quest.AcceptCheck;
import com.notquests.core.structs.Quest.ConditionSettings;
import com.notquests.core.structs.Quest.CooldownDisplay;
import com.notquests.core.structs.Quest.GiveOptions;
import com.notquests.core.structs.Quest.ObjectiveSettings;
import com.notquests.core.structs.Quest.OrderRequirements;
import com.notquests.core.structs.Quest.TriggerSettings;
import com.notquests.core.structs.Quest;
import com.notquests.core.structs.QuestPlayer.CompletedObjective;
import com.notquests.core.structs.QuestPlayer.CompletedQuest;
import com.notquests.core.structs.QuestPlayer.FailedQuest;
import com.notquests.core.structs.QuestPlayer;
import com.notquests.core.text.NotQuestsColors;
import com.notquests.core.text.NotQuestsMiniMessage;
import com.notquests.core.triggers.ActiveTrigger.Event;
import com.notquests.core.triggers.ActiveTrigger;
import com.notquests.core.triggers.Trigger;
import com.notquests.core.variables.NumberExpression;
import com.notquests.core.variables.VariableDataType;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleBiFunction;
import java.util.function.ToDoubleFunction;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

/**
 * Platform-neutral NotQuests plugin state.
 *
 * <p>Paper, NeoForge, and future adapters should provide platform behavior; this object owns the
 * shared registry and runtime state that should not be recreated per platform.
 */
public final class NotQuestsPlugin {
    public record ArmorStandNpcUpdate(String message, boolean applied, String selector) {}

    public record CompletedObjectiveCheck(
            boolean available,
            boolean completed,
            String objectiveName,
            String errorMessage) {}

    public record EliteMobCredit(String playerIdentifier, double damage) {}

    public record QuestPreview(
            List<String> linesBeforeAccept,
            String acceptText,
            String acceptCommand,
            String acceptHover,
            String lineAfterAccept) {}

    /** bStats chart names and live values. Platforms only translate these to their metrics API. */
    public record Metrics(
            int pluginId,
            Map<String, Supplier<Integer>> singleLineCharts,
            Map<String, Supplier<Map<String, Integer>>> advancedPieCharts) {
        public Metrics {
            singleLineCharts = singleLineCharts == null ? Map.of() : Map.copyOf(singleLineCharts);
            advancedPieCharts = advancedPieCharts == null ? Map.of() : Map.copyOf(advancedPieCharts);
        }
    }

    public static final class PluginStatus {
        private final CopyOnWriteArrayList<String> disableReasons = new CopyOnWriteArrayList<>();
        private volatile boolean savingEnabled = true;
        private volatile boolean loadingEnabled = true;
        private volatile boolean disabled;
        private volatile boolean dataLoading = true;
        private volatile boolean configuredDataLoaded;

        public boolean isDisabled() {
            return disabled;
        }

        public void disableSavingAndLoading(final String reason) {
            savingEnabled = false;
            loadingEnabled = false;
            disabled = true;
            if (reason != null && !reason.isBlank()) {
                disableReasons.add(reason);
            }
        }

        public void enableSavingAndLoading() {
            savingEnabled = true;
            loadingEnabled = true;
            disabled = false;
        }

        public boolean isSavingEnabled() {
            return savingEnabled && !disabled;
        }

        public void setSavingEnabled(final boolean savingEnabled) {
            this.savingEnabled = savingEnabled;
        }

        public boolean isLoadingEnabled() {
            return loadingEnabled;
        }

        public void setLoadingEnabled(final boolean loadingEnabled) {
            this.loadingEnabled = loadingEnabled;
        }

        public boolean isDataLoading() {
            return dataLoading;
        }

        public void setDataLoading(final boolean dataLoading) {
            this.dataLoading = dataLoading;
        }

        public boolean isConfiguredDataLoaded() {
            return configuredDataLoaded;
        }

        public void setConfiguredDataLoaded(final boolean configuredDataLoaded) {
            this.configuredDataLoaded = configuredDataLoaded;
        }

        public List<String> disableReasons() {
            return List.copyOf(disableReasons);
        }
    }

    static final class DisableReason {
        private DisableReason() {}

        static Formatted format(final String reason, final List<Detail> details) {
            final ArrayList<String> logLines = new ArrayList<>();
            String formattedReason = reason == null ? "" : reason;
            for (final Detail detail : details == null ? List.<Detail>of() : details) {
                if (detail == null) {
                    continue;
                }
                if (detail.throwable() != null) {
                    logLines.add("Error message:");
                    formattedReason += "\nError message:\n" + stackTrace(detail.throwable());
                    continue;
                }
                if (!detail.line().isBlank()) {
                    logLines.add(detail.line());
                    formattedReason += "\n" + detail.line();
                }
            }
            return new Formatted(formattedReason, List.copyOf(logLines));
        }

        static Detail throwable(final Throwable throwable) {
            return new Detail("", throwable);
        }

        static Detail quest(final String questIdentifier) {
            return line("  <DARK_GRAY>└─</DARK_GRAY> Quest: <highlight>" + safe(questIdentifier));
        }

        static Detail objective(final int objectiveId, final String holderIdentifier) {
            return line("  <DARK_GRAY>└─</DARK_GRAY> Objective ID: <highlight>"
                    + objectiveId
                    + "</highlight> of Quest: <highlight2>"
                    + safe(holderIdentifier));
        }

        static Detail objectiveHolder(final String holderIdentifier) {
            return line("  <DARK_GRAY>└─</DARK_GRAY> Objective Holder: <highlight>" + safe(holderIdentifier));
        }

        static Detail action(final String actionName, final String actionType) {
            return line("  <DARK_GRAY>└─</DARK_GRAY> Action Name: <highlight>"
                    + safe(actionName)
                    + "</highlight> of Type: <highlight2>"
                    + safe(actionType));
        }

        static Detail category(final String categoryName) {
            return line("  <DARK_GRAY>└─</DARK_GRAY> Category name: <highlight>"
                    + safe(categoryName)
                    + "</highlight>.");
        }

        private static Detail line(final String line) {
            return new Detail(line, null);
        }

        private static String stackTrace(final Throwable throwable) {
            final StringWriter stringWriter = new StringWriter();
            throwable.printStackTrace(new PrintWriter(stringWriter));
            return stringWriter.toString();
        }

        private static String safe(final String text) {
            return text == null ? "" : text;
        }

        record Formatted(String reason, List<String> logLines) {
            Formatted {
                reason = reason == null ? "" : reason;
                logLines = logLines == null ? List.of() : List.copyOf(logLines);
            }
        }

        record Detail(String line, Throwable throwable) {
            Detail {
                line = line == null ? "" : line;
            }
        }
    }

    public static final class ObjectiveCompass {
        private ObjectiveCompass() {}

        public static double yawTo(
                final double fromX,
                final double fromZ,
                final double toX,
                final double toZ) {
            final double dx = toX - fromX;
            final double dz = toZ - fromZ;
            final double yaw = Math.toDegrees(Math.atan2(-dx, dz));
            return Math.abs(yaw) < 0.000001 ? 0.0 : yaw;
        }

        public static double wrappedDegrees(final double degrees) {
            double wrapped = degrees % 360.0;
            if (wrapped >= 180.0) {
                wrapped -= 360.0;
            }
            if (wrapped < -180.0) {
                wrapped += 360.0;
            }
            return wrapped;
        }

        public static String direction(final double directionDelta) {
            final double absolute = Math.abs(directionDelta);
            if (absolute <= 12.0) {
                return "^";
            }
            if (absolute >= 165.0) {
                return "behind";
            }
            if (directionDelta < 0.0) {
                return absolute >= 90.0 ? "<<" : "<";
            }
            return absolute >= 90.0 ? ">>" : ">";
        }

        public static float progress(final double directionDelta) {
            return (float) Math.max(0.0, 1.0 - (Math.abs(directionDelta) / 180.0));
        }

        public static Severity severity(final double directionDelta) {
            final double absolute = Math.abs(directionDelta);
            if (absolute <= 15.0) {
                return Severity.GREEN;
            }
            if (absolute <= 75.0) {
                return Severity.YELLOW;
            }
            return Severity.RED;
        }

        public static Display differentWorld(final String worldName) {
            return new Display(
                    "<warn>Objective marker is in world <highlight>" + clean(worldName) + "</highlight>",
                    0.0f,
                    Severity.RED);
        }

        public static Display display(
                final double playerX,
                final double playerZ,
                final float playerYaw,
                final double targetX,
                final double targetZ,
                final double distance,
                final String objectiveLabel) {
            final double directionDelta = wrappedDegrees(yawTo(playerX, playerZ, targetX, targetZ) - playerYaw);
            final String direction = direction(directionDelta);
            return new Display(
                    "<highlight>" + direction + "</highlight> <positive>" + Math.round(distance)
                            + "m</positive> <unimportant>-</unimportant> <main>"
                            + clean(objectiveLabel == null || objectiveLabel.isBlank() ? "Objective Marker" : objectiveLabel)
                            + "</main>",
                    progress(directionDelta),
                    severity(directionDelta));
        }

        private static String clean(final String value) {
            return value == null ? "" : value;
        }

        public enum Severity {
            GREEN,
            YELLOW,
            RED
        }

        public record Display(String title, float progress, Severity severity) {}
    }

    private final NotQuestsRegistry registry = new NotQuestsRegistry();
    private final SavedActions savedActions = new SavedActions();
    private final ConversationManager conversations = new ConversationManager();
    private final ConfigurationManager configuration = new ConfigurationManager();
    private final LogManager logManager = new LogManager(configuration);
    private final LanguageManager languageManager = new LanguageManager();
    private final UpdateManager updateManager = UpdateManager.notQuestsWebsite();
    private final PluginStatus pluginStatus = new PluginStatus();
    private final Packs<Object> registryPacks = new Packs<>();
    private final QuestManager questManager = new QuestManager();
    private final QuestPlayerManager questPlayerManager = new QuestPlayerManager();
    private final NpcAttachments.Selections npcSelections = new NpcAttachments.Selections();
    private final ConcurrentHashMap<String, Tag> tags = new ConcurrentHashMap<>();
    private final TagManager playerTagPersistence = new TagManager();
    private final ConcurrentHashMap<String, StoredCondition> savedConditions = new ConcurrentHashMap<>();
    private final SavedItems savedItems = new SavedItems();
    private final IntegrationsManager integrations = new IntegrationsManager(this);
    private final ConcurrentHashMap<String, List<Component>> chatHistory = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Integer> progressBossBarAges = new ConcurrentHashMap<>();
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final Set<String> debugPlayers = ConcurrentHashMap.newKeySet();
    private final ThreadLocal<Boolean> objectiveUnlockTriggersEnabled = ThreadLocal.withInitial(() -> true);
    private volatile GuiService guiService;
    private volatile String platformName = "";
    private volatile boolean starting;
    private volatile boolean loaded;
    private volatile boolean shuttingDown;
    private volatile long startupSequence;
    private volatile long startupStartedAtMillis;
    private volatile long startupFinishedAtMillis;
    private volatile long shutdownStartedAtMillis;
    private volatile DataManager dataManager = new DataManager();
    private volatile SavedActions.ActionScheduler actionScheduler = SavedActions.ActionScheduler.immediate();
    private volatile ToDoubleBiFunction<String, String> jobsLevelReader;
    private int jobsLevelSyncSeconds;
    private volatile NotQuestsAdapter platformAdapter;
    private volatile PlayerDatabase playerDatabase;
    private volatile Path dataFolder;
    private volatile String pluginVersion = "";
    private volatile String serverVersion = "";
    private volatile boolean databasePlayerRuntime;
    private volatile BackupManager backupManager;
    private volatile Optional<NotQuestsPlatform.PacketBridge> packetBridge = Optional.empty();
    private volatile Function<String, PlatformPlayer> platformPlayer = ignored -> null;
    private volatile Supplier<List<String>> onlinePlayerIdentifiers = List::of;
    private volatile NotQuestsRegistry.PlatformHooks registryHooks =
            new NotQuestsRegistry.PlatformHooks(ignored -> {}, ignored -> {}, ignored -> {});

    private NotQuestsPlugin() {
        guiService = new GuiService(this);
        questPlayerManager.configureObjectives(
                this::objectiveConditionsFulfilled,
                this::sendObjectiveProgressUpdate,
                this::completeObjective,
                this::objectiveUnlocked);
        questPlayerManager.configureProfileDisplay(
                objective -> removeObjectiveMarker(
                        objective.getQuestPlayer(), activeObjectiveBeamName(objective)),
                PlatformPlayer::hideProgressBossBar,
                this::showObjectiveMarker);
        conversations.runtime(new ConversationManager.ConversationRuntime() {
            @Override
            public Result checkCondition(
                    final String rawCondition,
                    final PlatformPlayer questPlayer) {
                return checkConversationCondition(rawCondition, questPlayer);
            }

            @Override
            public void executeAction(final String rawAction, final PlatformPlayer questPlayer) {
                executeConversationAction(rawAction, questPlayer);
            }

            @Override
            public void schedule(final Duration delay, final Runnable action) {
                if (delay == null || delay.isNegative() || delay.isZero()) {
                    action.run();
                } else {
                    actionScheduler.schedule(delay, action);
                }
            }

            @Override
            public String speakerLine(
                    final PlatformPlayer questPlayer,
                    final Speaker speaker,
                    final String message) {
                return conversationSpeakerLine(questPlayer, speaker, message);
            }

            @Override
            public String answerOptionLine(
                    final PlatformPlayer questPlayer,
                    final Speaker speaker,
                    final String message,
                    final int optionNumber) {
                return conversationAnswerOptionLine(questPlayer, speaker, message, optionNumber);
            }

            @Override
            public String chooseAnswerPrefix(final PlatformPlayer questPlayer) {
                return conversationChooseAnswerPrefix(questPlayer);
            }

            @Override
            public String chooseAnswerHover(final PlatformPlayer questPlayer) {
                return conversationChooseAnswerHover(questPlayer);
            }

            @Override
            public Component component(final String miniMessage) {
                return NotQuestsMiniMessage.create(configuration).deserialize(
                        miniMessage == null ? "" : miniMessage);
            }

            @Override
            public boolean deletePreviousMessages() {
                return configuration.deletePreviousConversationMessages();
            }
        });
    }

    public static NotQuestsPlugin create() {
        return new NotQuestsPlugin();
    }

    public void load(final NotQuestsPlatform platform) {
        beginStartup(platform.platformName());
        console(platform::writeConsole);
        info("NotQuests (%s) is starting...", platform.platformName());
        actionScheduler(platform::scheduleAction);
        playerRuntime(platform::createPlayer, platform::onlinePlayerIdentifiers);
        configureData(
                platform.dataFolder(),
                platform.pluginVersion(),
                platform.serverVersion(),
                platform.adapter(),
                true);
        conversationManager().display(platform.conversationDisplay());
        ConfigurationManager.JournalItem journalItem = configuration.journalItem();
        if (!platform.materializeJournalItem(journalItem)) {
            journalItem = invalidJournalItem(journalItem.configuredValue(), null);
            if (!platform.materializeJournalItem(journalItem)) {
                severe("The default journal item could not be created on this platform.");
            }
        }
        packetBridge = platform.packetBridge(
                configuration.packetMagicEnabled(),
                configuration.packetMagicUsePacketEvents());
    }

    public void enable(final NotQuestsPlatform platform) {
        runStartupPhases(platform);
    }

    public void start(final NotQuestsPlatform platform) {
        load(platform);
        runStartupPhases(platform);
    }

    public void stop(final NotQuestsPlatform platform) {
        final boolean dataLoadedBeforeShutdown = loaded && pluginStatus.isConfiguredDataLoaded();
        beginShutdown();
        info("NotQuests is shutting down...");
        activePlatformPlayers().forEach(player -> {
            clearObjectiveMarkers(player);
            player.hideProgressBossBar();
            player.hideLocationCompass();
        });
        if (dataLoadedBeforeShutdown) {
            saveData();
            if (backupManager != null) {
                backupManager.backupQuestConfigsOnShutdown();
            }
        } else {
            info("Skipping data saving because initial data loading did not finish.");
        }
        closePlayerDatabase();
        integrations.close();
        packetBridge.ifPresent(NotQuestsPlatform.PacketBridge::close);
        packetBridge = Optional.empty();
        platform.closePlatform();
    }

    private void beginStartup(final String platformName) {
        this.platformName = platformName == null ? "" : platformName;
        starting = true;
        loaded = false;
        shuttingDown = false;
        startupStartedAtMillis = System.currentTimeMillis();
        startupFinishedAtMillis = 0;
        shutdownStartedAtMillis = 0;
        startupSequence++;
    }

    private void runStartupPhases(final NotQuestsPlatform platform) {
        final long startup = startupSequence;
        integrations.enableConfigured(platform, configuration);
        platform.registerPlatformEvents();
        integrations.registerEvents();
        packetBridge.ifPresent(NotQuestsPlatform.PacketBridge::start);
        startNpcIndicators("armorstand", platform);
        if (integrations.isEnabled("FancyNpcs")) {
            startNpcIndicators("fancynpcs", platform);
        }
        if (integrations.isEnabled("Citizens")) {
            startNpcIndicators("citizens", platform);
        }
        registerRegistryPacks(platform);
        final NotQuestsCommands commands = commandSurface(
                platform.adapter(),
                platform::pluginVersion,
                platform::serverVersion,
                platform::dataFolder);
        for (final CommandMessage message : commands.exportGeneratedMetadata()) {
            if (!message.success()) {
                warn(message.message());
            }
        }
        platform.registerCommands(commands);
        try {
            platform.loadData(this::loadData).whenComplete((dataLoaded, exception) -> {
                if (startup != startupSequence || shuttingDown) {
                    return;
                }
                try {
                    platform.runOnServerThread(
                            () -> completeStartup(
                                    platform,
                                    startup,
                                    Boolean.TRUE.equals(dataLoaded),
                                    exception));
                } catch (final RuntimeException schedulingFailure) {
                    failStartup(startup, schedulingFailure);
                }
            });
        } catch (final RuntimeException exception) {
            failStartup(startup, exception);
        }
    }

    private void completeStartup(
            final NotQuestsPlatform platform,
            final long startup,
            final boolean dataLoaded,
            final Throwable exception) {
        if (startup != startupSequence || shuttingDown) {
            return;
        }
        if (exception != null) {
            failStartup(startup, exception);
            return;
        }
        if (!dataLoaded) {
            failStartup(startup, null);
            return;
        }
        try {
            finishStartup();
            scheduleRuntimeSecond(platform, startup);
            startUpdateChecks(platform.pluginVersion(), platform::scheduleAsync);
            integrations.dataLoaded(configuration);
            platform.metricsBridge().ifPresent(bridge -> bridge.start(metrics()));
            platform.publishLoadedEvent();
        } catch (final RuntimeException startupFailure) {
            failStartup(startup, startupFailure);
        }
    }

    private void finishStartup() {
        starting = false;
        loaded = true;
        shuttingDown = false;
        startupFinishedAtMillis = System.currentTimeMillis();
        info("Initial loading has completed!");
    }

    private void scheduleRuntimeSecond(final NotQuestsPlatform platform, final long startup) {
        platform.scheduleAction(Duration.ofSeconds(1), () -> {
            if (startup != startupSequence || shuttingDown || !loaded) {
                return;
            }
            refreshQuestRuntimeSecond();
            scheduleRuntimeSecond(platform, startup);
        });
    }

    public void startNpcIndicators(
            final String npcType,
            final NotQuestsPlatform platform) {
        if (npcType == null || npcType.isBlank() || platform == null) {
            return;
        }
        platform.npcIndicatorRenderer(npcType).ifPresent(
                renderer -> scheduleNpcIndicators(npcType, platform, renderer, startupSequence, 0, 0));
    }

    private void scheduleNpcIndicators(
            final String npcType,
            final NotQuestsPlatform platform,
            final NotQuestsPlatform.NpcIndicatorRenderer renderer,
            final long startup,
            final int particleTicks,
            final int textTicks) {
        actionScheduler.schedule(Duration.ofMillis(50L), () -> {
            if (startup != startupSequence || shuttingDown) {
                return;
            }
            final int nextParticleTicks = particleTicks + 1;
            final int nextTextTicks = textTicks + 1;
            boolean particleShown = false;
            boolean textShown = false;
            if (loaded) {
                final double tps = platform.currentTps();
                for (final NQNPCID npcId : npcIdsWithAttachment(npcType)) {
                    var indicator = npcIndicator(
                            npcType,
                            npcId,
                            nextParticleTicks,
                            nextTextTicks,
                            tps);
                    final Optional<NotQuestsPlatform.NpcTextVisibility> textVisibility =
                            renderer.textVisibility();
                    if (indicator.showText() && textVisibility.isEmpty()) {
                        indicator = new NpcAttachments.Indicator(
                                indicator.attached(),
                                indicator.showParticle(),
                                indicator.particleCount(),
                                indicator.particleType(),
                                false,
                                "");
                    }
                    if (indicator.showParticle() || indicator.showText()) {
                        final Set<String> visiblePlayers = indicator.showText()
                                ? textVisibility.orElseThrow().nearbyPlayerIdentifiers(npcId, 16.0d).stream()
                                        .filter(playerId -> npcIndicatorVisibleTo(
                                                npcType, npcId, activePlatformPlayer(playerId)))
                                        .collect(Collectors.toUnmodifiableSet())
                                : Set.of();
                        renderer.render(npcId, indicator, visiblePlayers);
                    }
                    particleShown |= indicator.showParticle();
                    textShown |= indicator.showText();
                }
            }
            scheduleNpcIndicators(
                    npcType,
                    platform,
                    renderer,
                    startup,
                    particleShown ? 0 : nextParticleTicks,
                    textShown ? 0 : nextTextTicks);
        });
    }

    private void failStartup(final long startup, final Throwable exception) {
        if (startup != startupSequence || shuttingDown) {
            return;
        }
        starting = false;
        loaded = false;
        if (exception == null) {
            severe("Initial data loading failed. NotQuests startup has not completed.");
        } else {
            severe("Initial data loading failed. NotQuests startup has not completed.", exception);
        }
    }

    private void beginShutdown() {
        starting = false;
        loaded = false;
        shuttingDown = true;
        startupSequence++;
        shutdownStartedAtMillis = System.currentTimeMillis();
    }

    public String platformName() {
        return platformName;
    }

    public void loadGuiLayouts(
            final Path dataFolder,
            final BiConsumer<String, Throwable> warningHandler) {
        try {
            guiService = new GuiService(
                    this,
                    GuiService.loadFromFolder(dataFolder));
        } catch (final IOException | RuntimeException exception) {
            if (warningHandler != null) {
                warningHandler.accept("Failed loading GUI files. Falling back to bundled GUI layouts.", exception);
            }
            guiService = new GuiService(
                    this,
                    GuiService.loadBundledDefaults());
        }
    }

    public ResolvedGui buildGui(
            final String guiName,
            final PlatformPlayer questPlayer,
            final GuiContext context) {
        return guiService.withValidMaterials(
                guiService.build(
                        guiName,
                        questPlayer,
                        context == null ? GuiContext.EMPTY : context),
                runtimeAdapter()::itemMaterialExists,
                this::warn);
    }

    public ResolvedGui buildGui(
            final String guiName,
            final PlatformPlayer questPlayer,
            final String questIdentifier,
            final String categoryIdentifier) {
        return guiService.withValidMaterials(
                guiService.build(guiName, questPlayer, questIdentifier, categoryIdentifier),
                runtimeAdapter()::itemMaterialExists,
                this::warn);
    }

    public ResolvedGui buildGui(
            final String guiName,
            final PlatformPlayer questPlayer,
            final String questIdentifier,
            final String categoryIdentifier,
            final String npcType,
            final NQNPCID npcId) {
        return guiService.withValidMaterials(
                guiService.build(
                        guiName,
                        questPlayer,
                        questIdentifier,
                        categoryIdentifier,
                        npcType,
                        npcId),
                runtimeAdapter()::itemMaterialExists,
                this::warn);
    }

    public boolean openGui(
            final PlatformPlayer questPlayer,
            final String guiName,
            final String playerName,
            final GuiContext context) {
        if (questPlayer == null || guiName == null || guiName.isBlank()) {
            return false;
        }
        final PlatformPlayer target = playerName == null || playerName.isBlank()
                ? questPlayer
                : onlineQuestPlayer(playerName);
        if (target == null || !target.hasPlayer()) {
            return false;
        }
        final ResolvedGui gui = buildGui(guiName, target, context == null ? GuiContext.EMPTY : context);
        if (gui.slots().isEmpty()) {
            warn("Failed showing gui '" + guiName + "' to player " + target.playerName());
            return false;
        }
        return target.showGui(gui);
    }

    private PlatformPlayer onlineQuestPlayer(final String playerName) {
        if (playerName == null || playerName.isBlank()) {
            return null;
        }
        for (final PlatformPlayer player : questPlayerManager.getConnectedPlayers()) {
            if (playerName.equalsIgnoreCase(player.playerName())
                    || playerName.equalsIgnoreCase(player.displayName())
                    || playerName.equalsIgnoreCase(player.playerIdentifier())) {
                return player;
            }
        }
        return runtimeAdapter().onlineQuestPlayer(playerName);
    }

    public boolean isStarting() {
        return starting;
    }

    public boolean isLoaded() {
        return loaded;
    }

    public boolean isShuttingDown() {
        return shuttingDown;
    }

    public long startupStartedAtMillis() {
        return startupStartedAtMillis;
    }

    public long startupFinishedAtMillis() {
        return startupFinishedAtMillis;
    }

    public long shutdownStartedAtMillis() {
        return shutdownStartedAtMillis;
    }

    public NotQuestsRegistry registry() {
        return registry;
    }

    public QuestManager questManager() {
        return questManager;
    }

    public QuestPlayerManager questPlayerManager() {
        return questPlayerManager;
    }

    public NotQuestsAdapter createRegistryAdapter(final NotQuestsRegistry.PlatformHooks hooks) {
        registryHooks = hooks == null
                ? new NotQuestsRegistry.PlatformHooks(ignored -> {}, ignored -> {}, ignored -> {})
                : hooks;
        return registry.createAdapter(registryHooks);
    }

    public void platformAdapter(final NotQuestsAdapter adapter) {
        if (adapter != null) {
            platformAdapter = adapter;
        }
    }

    private NotQuestsAdapter runtimeAdapter() {
        return platformAdapter == null ? registry.createAdapter(registryHooks) : platformAdapter;
    }

    public void parseVariableAction(
            final Actions.Draft action,
            final List<String> arguments,
            final VariableDataType type) {
        registry.parseVariableAction(registryHooks, action, arguments, type);
    }

    public void executeVariableAction(
            final VariableDataType type,
            final Actions.Data action,
            final PlatformPlayer questPlayer,
            final Object... objects) {
        registry.executeVariableAction(registryHooks, type, action, questPlayer, objects);
        if (questPlayer != null
                && questPlayer.playerIdentifier() != null
                && !questPlayer.playerIdentifier().isBlank()) {
            questPlayerManager.getActiveObjectives().refreshObjectives(
                    questPlayer,
                    Objectives.ObjectiveRefresh.variableValueChanged(action.text("variableName")));
        }
    }

    public String variableActionDescription(
            final VariableDataType type,
            final Actions.Data action,
            final PlatformPlayer questPlayer,
            final Object... objects) {
        return registry.variableActionDescription(registryHooks, type, action, questPlayer, objects);
    }

    public CommandManager commandManager(final NotQuestsAdapter adapter) {
        platformAdapter(adapter);
        return new CommandManager(this, adapter);
    }

    public CommandManager commandManager(
            final NotQuestsAdapter adapter,
            final Supplier<Path> dataFolder) {
        platformAdapter(adapter);
        return new CommandManager(this, adapter, dataFolder);
    }

    public NotQuestsCommands commandSurface(
            final NotQuestsAdapter adapter,
            final Supplier<String> version,
            final Supplier<String> minecraftVersion,
            final Supplier<Path> dataFolder) {
        platformAdapter(adapter);
        return NotQuestsCommands.create(this, adapter, version, minecraftVersion, dataFolder);
    }

    public BackupManager backupManager(
            final Path dataFolder,
            final BackupManager.Hooks hooks) {
        return new BackupManager(
                dataFolder,
                hooks,
                configuration,
                pluginStatus::isDataLoading,
                this::categoryNames,
                () -> dataFolder == null ? null : dataFolder.resolve("database_sqlite.db"));
    }

    public GeneralConfigLoad configureData(
            final Path dataFolder,
            final String pluginVersion,
            final String serverVersion,
            final NotQuestsAdapter adapter,
            final boolean databasePlayerRuntime) {
        this.dataFolder = Objects.requireNonNull(dataFolder, "dataFolder");
        this.pluginVersion = pluginVersion == null ? "" : pluginVersion;
        this.serverVersion = serverVersion == null ? "" : serverVersion;
        this.databasePlayerRuntime = databasePlayerRuntime;
        platformAdapter(adapter);
        prepareBackupManager(dataFolder);
        final GeneralConfigLoad loaded = loadGeneralConfig(
                dataFolder,
                this.pluginVersion,
                this.serverVersion,
                platformName);
        if (loaded.loaded()) {
            try {
                ConfigurationManager.copyMissingDefaults(dataFolder);
            } catch (final IOException exception) {
                disablePluginAndSaving(
                        "Failed creating the default NotQuests configuration files.",
                        List.of(DisableReason.throwable(exception)));
            }
        }
        dataManager = new DataManager(this, adapter, dataFolder, !databasePlayerRuntime);
        if (databasePlayerRuntime) {
            playerDatabase(dataFolder, new PlayerDatabase.Logger() {
                @Override public void info(final String message) { NotQuestsPlugin.this.info(message); }
                @Override public void warn(final String message) { NotQuestsPlugin.this.severe(message); }
                @Override public void debug(final Exception exception) {
                    if (configuration.debugEnabled()) {
                        NotQuestsPlugin.this.warn(exception.getMessage(), exception);
                    }
                }
            });
        }
        return loaded;
    }

    public void playerRuntime(
            final Function<String, PlatformPlayer> playerFactory,
            final Supplier<List<String>> onlinePlayers) {
        platformPlayer = playerFactory == null ? ignored -> null : playerFactory;
        onlinePlayerIdentifiers = onlinePlayers == null ? List::of : onlinePlayers;
    }

    public boolean loadData() {
        if (!pluginStatus.isLoadingEnabled() || dataFolder == null) {
            severe("Data loading has been skipped because core data has not been configured or loading is disabled.");
            return false;
        }
        pluginStatus.setDataLoading(true);
        try {
            if (!loadLanguageConfig(dataFolder, false) || !dataManager.reload(ReloadTarget.ALL)) {
                return false;
            }
            if (databasePlayerRuntime && !loadDatabasePlayerData()) {
                return false;
            }
            loadGuiLayouts(dataFolder, this::warn);
            applyNpcAttachments();
            clearNpcConversationSessions();
            return pluginStatus.isLoadingEnabled();
        } finally {
            pluginStatus.setDataLoading(false);
        }
    }

    private boolean loadDatabasePlayerData() {
        openPlayerDatabase();
        if (configuration.backupDatabaseBeforeLoad()) {
            backupManager.backupDatabase();
        }
        if (!preparePlayerDatabase(databaseLogger())) {
            return false;
        }
        if (!configuration.loadPlayerData()) {
            return true;
        }
        if (!configuration.loadPlayerDataOnJoin()) {
            clearAllPlayerRuntime();
            return loadPlayerData(null, this::platformQuestPlayer);
        }
        boolean loadedPlayers = true;
        for (final String playerIdentifier : onlinePlayerIdentifiers.get()) {
            if (playerIdentifier != null && !playerIdentifier.isBlank()) {
                loadedPlayers &= loadPlayerData(playerIdentifier, this::platformQuestPlayer);
            }
        }
        return loadedPlayers;
    }

    private PlatformPlayer platformQuestPlayer(final String playerIdentifier) {
        return playerIdentifier == null ? null : platformPlayer.apply(playerIdentifier);
    }

    private PlayerDatabase.Logger databaseLogger() {
        return new PlayerDatabase.Logger() {
            @Override public void info(final String message) {
                if (configuration.verboseStartupMessages()) {
                    NotQuestsPlugin.this.info(message);
                }
            }
            @Override public void warn(final String message) { NotQuestsPlugin.this.warn(message); }
            @Override public void debug(final Exception exception) {
                if (configuration.debugEnabled()) {
                    NotQuestsPlugin.this.warn(exception.getMessage(), exception);
                }
            }
        };
    }

    public CategoryFiles.PreparedFolders prepareCategoryConfigFiles(
            final Path dataFolder,
            final CategoryFiles.Hooks hooks) {
        ensureDefaultCategory();
        return CategoryFiles.prepare(dataFolder, categories(), hooks);
    }

    public GeneralConfigLoad loadGeneralConfig(
            final Path dataFolder,
            final ConfigurationManager.IO io,
            final ConfigurationManager.Hooks hooks,
            final String pluginVersion,
            final String serverVersion,
            final String platformName) {
        final ConfigurationManager.Loaded loaded = configuration.open(dataFolder, io, hooks);
        if (!loaded.loaded()) {
            return new GeneralConfigLoad(
                    loaded.file(),
                    loaded.configuration(),
                    false,
                    false,
                    List.of(),
                    null,
                    "");
        }
        final boolean migrationVersionChanged = ConfigurationMigrations.prepareDataVersion(
                loaded.configuration(),
                loaded.created()
                        ? pluginVersion
                        : loaded.configuration().getString(ConfigurationManager.CONFIG_VERSION, ""));
        prepareBackupManager(dataFolder);
        if (!new ConfigurationMigrations().runStartupMigrations(new ConfigurationMigrations.MigrationHooks() {
            @Override public Path dataFolder() { return dataFolder; }
            @Override public YamlConfig generalConfig() { return loaded.configuration(); }
            @Override public String currentVersion() { return pluginVersion; }
            @Override public Consumer<String> infoLogger() { return NotQuestsPlugin.this::info; }
            @Override public BiConsumer<String, Exception> warnLogger() {
                return (message, exception) -> NotQuestsPlugin.this.warn(message, exception);
            }
            @Override public void backup(final String targetVersion) {
                backupManager.backupFullNotQuestsFolderForMigration(targetVersion);
                backupManager.backupDatabase();
            }
            @Override public void saveGeneralConfig() { configuration.save(io, hooks); }
        })) {
            return new GeneralConfigLoad(
                    loaded.file(),
                    loaded.configuration(),
                    false,
                    false,
                    List.of(),
                    null,
                    "Plugin disabled because the released configuration data could not be upgraded.");
        }
        final ConfigurationManager.Shared shared = configuration.load(loaded.configuration(), pluginVersion);
        final ConfigurationManager.PacketMagic packetMagic =
                configuration.loadPacketMagic(loaded.configuration(), serverVersion, platformName);
        final GeneralConfigLoad result = new GeneralConfigLoad(
                loaded.file(),
                loaded.configuration(),
                true,
                migrationVersionChanged || shared.changed() || packetMagic.changed(),
                packetMagic.infoMessages(),
                packetMagic,
                shared.disableReason());
        return result;
    }

    private void prepareBackupManager(final Path dataFolder) {
        if (backupManager != null) {
            return;
        }
        backupManager = backupManager(dataFolder, new BackupManager.Hooks() {
            @Override public void info(final String message) { NotQuestsPlugin.this.info(message); }
            @Override public void warn(final String message) { NotQuestsPlugin.this.warn(message); }
            @Override public void disableSaving(final String reason) {
                disablePluginAndSaving(reason, List.of());
            }
        });
    }

    /** Applies the shared fallback when a platform can no longer observe outgoing chat packets. */
    public void packetMagicFailed(final Throwable exception) {
        configuration.disablePacketMagic();
        warn("Packet interception has been disabled because the platform channel failed.", exception);
    }

    /** Chooses the journal fallback and owns its warning when native item materialization fails. */
    public ConfigurationManager.JournalItem invalidJournalItem(
            final Object invalidValue,
            final Throwable exception) {
        final ConfigurationManager.JournalItem fallback = configuration.useDefaultJournalItem();
        final String message = ConfigurationManager.invalidValue(
                "general.yml",
                "item",
                "general.journal-item.item",
                invalidValue,
                fallback.material());
        if (exception == null) {
            warn(message);
        } else {
            warn(message, exception);
        }
        return fallback;
    }

    public String configuredParticle(final String key, final String configuredValue) {
        final String configured = configuredValue == null ? "" : configuredValue;
        for (final String particle : runtimeAdapter().particleTypeIds()) {
            if (particle.equalsIgnoreCase(configured)) {
                return particle;
            }
        }
        final String fallback = "angry_villager";
        warn(ConfigurationManager.invalidValue("general.yml", "particle", key, configured, fallback));
        return fallback;
    }

    public GeneralConfigLoad loadGeneralConfig(
            final Path dataFolder,
            final String pluginVersion,
            final String serverVersion,
            final String platformName) {
        final GeneralConfigLoad loaded = loadGeneralConfig(
                dataFolder,
                new ConfigurationManager.IO() {
                    @Override
                    public YamlConfig load(final Path file) throws IOException {
                        return YamlConfig.load(file);
                    }

                    @Override
                    public void save(final YamlConfig configuration, final Path file) throws IOException {
                        YamlConfig.save(configuration, file);
                    }
                },
                new ConfigurationManager.Hooks() {
                    @Override
                    public void info(final String message) {
                        NotQuestsPlugin.this.info(message);
                    }

                    @Override
                    public void warn(final String message) {
                        NotQuestsPlugin.this.warn(message);
                    }

                    @Override
                    public void disableSaving(final String reason, final Exception exception) {
                        disablePluginAndSaving(
                                reason,
                                exception == null
                                        ? List.of()
                                        : List.of(DisableReason.throwable(exception)));
                    }
                },
                pluginVersion,
                serverVersion,
                platformName);
        loaded.infoMessages().forEach(this::info);
        if (loaded.loaded() && !configuration.journalItem().valid()) {
            warn(ConfigurationManager.invalidValue(
                    "general.yml",
                    "item",
                    "general.journal-item.item",
                    configuration.journalItem().configuredValue(),
                    ConfigurationManager.JournalItem.defaultItem().material()));
        }
        if (loaded.loaded() && loaded.changed()) {
            info("<highlight>General.yml</highlight> ConfigurationManager was updated with new values! Saving it...");
            saveGeneralConfig();
        }
        if (loaded.shouldDisableSaving()) {
            disablePluginAndSaving(loaded.disableReason(), List.of());
        }
        return loaded;
    }

    public YamlConfig generalConfig() {
        return configuration.yaml();
    }

    public boolean generalConfigLoaded() {
        return configuration.loaded();
    }

    public void saveGeneralConfig(final ConfigurationManager.IO io, final ConfigurationManager.Hooks hooks) {
        configuration.save(io, hooks);
    }

    public void saveGeneralConfig() {
        try {
            configuration.save();
        } catch (final IOException exception) {
            disablePluginAndSaving(
                    "Failed to save general.yml.",
                    List.of(DisableReason.throwable(exception)));
        }
    }

    public record GeneralConfigLoad(
            Path file,
            YamlConfig configuration,
            boolean loaded,
            boolean changed,
            List<String> infoMessages,
            ConfigurationManager.PacketMagic packetMagic,
            String disableReason) {
        public boolean shouldDisableSaving() {
            return disableReason != null && !disableReason.isBlank();
        }
    }

    public void playerDatabase(final Path dataFolder, final PlayerDatabase.Logger logger) {
        playerDatabase = new PlayerDatabase(dataFolder, logger);
    }

    public void openPlayerDatabase() {
        requirePlayerDatabase().open(configuration);
    }

    public Connection playerDatabaseConnection() throws SQLException {
        return requirePlayerDatabase().connection();
    }

    public boolean preparePlayerDatabase(final PlayerDatabase.Logger logger) {
        try (Connection connection = playerDatabaseConnection()) {
            PlayerDatabase.prepare(connection, logger);
            return true;
        } catch (final SQLException exception) {
            disablePluginAndSaving(
                    "Plugin disabled, because there was an error while trying to load database tables",
                    List.of(DisableReason.throwable(exception)));
            return false;
        }
    }

    public void closePlayerDatabase() {
        if (playerDatabase != null) {
            playerDatabase.close();
        }
    }

    private PlayerDatabase requirePlayerDatabase() {
        if (playerDatabase == null) {
            throw new IllegalStateException("Player database has not been configured.");
        }
        return playerDatabase;
    }

    public void dataManager(final DataManager dataManager) {
        this.dataManager = dataManager == null ? new DataManager() : dataManager;
    }

    public void actionScheduler(final SavedActions.ActionScheduler actionScheduler) {
        this.actionScheduler = actionScheduler == null ? SavedActions.ActionScheduler.immediate() : actionScheduler;
    }

    public boolean saveData() {
        if (!canSaveLoadedData()) {
            return false;
        }
        final boolean configuredSaved = dataManager.saveConfiguredData();
        final boolean runtimeSaved = databasePlayerRuntime
                ? saveAllPlayerData()
                : dataManager.savePlayerRuntime();
        return configuredSaved && runtimeSaved;
    }

    public boolean saveConfiguredData() {
        if (!canSaveLoadedData()) {
            return false;
        }
        return dataManager.saveConfiguredData();
    }

    private boolean canSaveLoadedData() {
        if (!pluginStatus.isSavingEnabled()) {
            warn("Saving is disabled => no data has been saved.");
            return false;
        }
        if (dataFolder != null && !pluginStatus.isConfiguredDataLoaded()) {
            info("Saving has been skipped because configured data loading has not finished.");
            return false;
        }
        return true;
    }

    public boolean reloadData(final ReloadTarget target) {
        if (!pluginStatus.isLoadingEnabled()) {
            return false;
        }
        final ReloadTarget reloadTarget = target == null ? ReloadTarget.ALL : target;
        if (reloadTarget != ReloadTarget.CONVERSATIONS && dataFolder == null) {
            return false;
        }
        pluginStatus.setDataLoading(true);
        try {
            return switch (reloadTarget) {
                case CONFIG -> loadGeneralConfig(
                        dataFolder,
                        pluginVersion,
                        serverVersion,
                        platformName).loaded();
                case LANGUAGES -> loadLanguageConfig(dataFolder, false);
                case CONVERSATIONS -> {
                    final boolean loadedConversations = dataManager.reload(ReloadTarget.CONVERSATIONS);
                    if (loadedConversations) {
                        clearNpcConversationSessions();
                    }
                    yield loadedConversations;
                }
                case ALL -> {
                    if (!loadGeneralConfig(
                                    dataFolder,
                                    pluginVersion,
                                    serverVersion,
                                    platformName).loaded()
                            || !loadLanguageConfig(dataFolder, false)) {
                        yield false;
                    }
                    final boolean loadedConversations = dataManager.reload(ReloadTarget.CONVERSATIONS);
                    if (loadedConversations) {
                        clearNpcConversationSessions();
                    }
                    yield loadedConversations;
                }
            };
        } finally {
            pluginStatus.setDataLoading(false);
        }
    }

    public boolean reloadAllDataUnsafe() {
        if (!pluginStatus.isLoadingEnabled() || dataFolder == null) {
            return false;
        }
        pluginStatus.setDataLoading(true);
        try {
            if (!loadGeneralConfig(
                            dataFolder,
                            pluginVersion,
                            serverVersion,
                            platformName).loaded()
                    || !loadLanguageConfig(dataFolder, false)
                    || !dataManager.reload(ReloadTarget.ALL)) {
                return false;
            }
            final boolean playersLoaded = !databasePlayerRuntime || loadDatabasePlayerData();
            if (playersLoaded) {
                loadGuiLayouts(dataFolder, this::warn);
                applyNpcAttachments();
            }
            return playersLoaded;
        } finally {
            pluginStatus.setDataLoading(false);
        }
    }

    public SavedActions savedActions() {
        return savedActions;
    }

    public List<ActiveTrigger> activeTriggers(final String playerIdentifier) {
        return questPlayerManager.getActiveTriggers(playerIdentifier);
    }

    private List<ActiveTrigger> activeTriggers(
            final String playerIdentifier,
            final String profile) {
        return questPlayerManager.getActiveTriggers(playerIdentifier, profile);
    }

    public boolean setActiveTriggerProgress(
            final String playerIdentifier,
            final String questName,
            final int triggerId,
            final long progress) {
        return setActiveTriggerProgress(
                playerIdentifier,
                activeProfile(playerIdentifier),
                questName,
                triggerId,
                progress);
    }

    private boolean setActiveTriggerProgress(
            final String playerIdentifier,
            final String profile,
            final String questName,
            final int triggerId,
            final long progress) {
        return questPlayerManager.setActiveTriggerProgress(
                playerIdentifier, profile, questName, triggerId, progress);
    }

    public ConversationManager conversationManager() {
        return conversations;
    }

    public LogManager logManager() {
        return logManager;
    }

    public void console(final Consumer<ConsoleLine> console) {
        logManager.console(console);
    }

    public void info(final String message, final Object... values) {
        logManager.info(message, values);
    }

    public void info(
            final LogManager.LogCategory category,
            final String message,
            final Object... values) {
        logManager.info(category, message, values);
    }

    public void warn(final String message, final Object... values) {
        logManager.warn(message, values);
    }

    public void integrationEnabled(
            final String integrationName,
            final String version,
            final boolean late) {
        final String name = integrationName == null || integrationName.isBlank()
                ? "Integration"
                : integrationName;
        final String versionText = version == null || version.isBlank() ? "" : " " + version;
        info(name + versionText + " found. Enabled " + name + " support"
                + (late ? " (late)!" : "!"));
        if (late && (name.equalsIgnoreCase("Citizens") || name.equalsIgnoreCase("FancyNpcs"))) {
            applyNpcAttachments();
        }
    }

    public void integrationUnavailable(final String integrationName) {
        final String name = integrationName == null || integrationName.isBlank()
                ? "Integration"
                : integrationName;
        info(name + " dependency was not found or could not be initialized; "
                + name + " support is disabled.");
    }

    public void integrationEventsRegistered(final String integrationName) {
        if (integrationName != null && !integrationName.isBlank()) {
            info("Registered events for " + integrationName + ".");
        }
    }

    public void integrationPluginEnabled(final String integrationName) {
        integrations.pluginEnabled(integrationName, configuration);
    }

    public boolean integrationEnabled(final String integrationName) {
        return integrations.isEnabled(integrationName);
    }

    /** Chooses the portable spawn route; adapters only perform one native spawn attempt. */
    public boolean spawnMob(
            final PlatformPlayer player,
            final String entityType,
            final NQLocation location) {
        if (player == null || entityType == null || entityType.isBlank() || location == null) {
            return false;
        }
        if (player.spawnVanillaMob(entityType, location)) {
            return true;
        }
        if (integrations.spawnMythicMob(entityType, location)) {
            return true;
        }
        return integrations.spawnEcoMob(entityType, location);
    }

    public List<NotQuestsAdapter.Integration> enabledIntegrations() {
        return integrations.getEnabledVersions();
    }

    public void warn(
            final LogManager.LogCategory category,
            final String message,
            final Object... values) {
        logManager.warn(category, message, values);
    }

    public void severe(final String message, final Object... values) {
        logManager.severe(message, values);
    }

    public void severe(
            final LogManager.LogCategory category,
            final String message,
            final Object... values) {
        logManager.severe(category, message, values);
    }

    public void debug(final String message, final Object... values) {
        logManager.debug(message, values);
    }

    public void debug(
            final LogManager.LogCategory category,
            final String message,
            final Object... values) {
        logManager.debug(category, message, values);
    }

    public PluginStatus pluginStatus() {
        return pluginStatus;
    }

    public void disablePluginAndSaving(
            final String reason,
            final Collection<DisableReason.Detail> details) {
        final DisableReason.Formatted result = DisableReason.format(
                reason,
                details == null ? List.of() : List.copyOf(details));
        severe("Plugin, saving and loading has been disabled. Reason: " + result.reason());
        for (final String line : result.logLines()) {
            severe(line);
        }
        pluginStatus.disableSavingAndLoading(result.reason());
    }

    public ConfigurationManager configuration() {
        return configuration;
    }

    public LanguageManager languageManager() {
        return languageManager;
    }

    public void loadLanguageConfig(
            final Path dataFolder,
            final boolean skipIfAlreadyLoaded,
            final LanguageManager.Logger logger) throws IOException {
        if (skipIfAlreadyLoaded && languageManager.loaded()) {
            return;
        }
        final String languageCode = configuration.languageCode();
        if (logger != null) {
            logger.info("Loading language config <highlight>" + languageCode + ".yml");
        }
        languageManager.load(dataFolder, languageCode, logger);
    }

    public boolean loadLanguageConfig(
            final Path dataFolder,
            final boolean skipIfAlreadyLoaded) {
        try {
            loadLanguageConfig(
                    dataFolder,
                    skipIfAlreadyLoaded,
                    message -> info(LogManager.LogCategory.LANGUAGE, message));
            return true;
        } catch (final IOException exception) {
            disablePluginAndSaving(
                    "There was an error loading the shared NotQuests language files.",
                    List.of(DisableReason.throwable(exception)));
            return false;
        }
    }

    public String translate(
            final String key,
            final Map<String, String> replacements,
            final String fallbackMiniMessage) {
        return languageManager.translate(key, replacements, fallbackMiniMessage);
    }

    public String translate(
            final PlatformPlayer questPlayer,
            final String key,
            final Map<String, String> replacements,
            final String fallbackMiniMessage) {
        final LanguageManager.Placeholders placeholders = LanguageManager.Placeholders.create();
        if (questPlayer != null) {
            placeholders.questPlayer(
                    () -> questPoints(questPlayer),
                    () -> activeProfile(questPlayer.playerIdentifier()));
        }
        if (replacements != null) {
            placeholders.putAll(replacements);
        }
        final String configured = key == null || key.isBlank() ? null : languageManager.string(key);
        final String text = configured == null ? fallbackMiniMessage : configured;
        final UnaryOperator<String> external = questPlayer != null
                        && externalPlaceholdersEnabled(questPlayer)
                        && configuration.supportPlaceholderApiInTranslationStrings()
                ? questPlayer::applyExternalPlaceholders
                : UnaryOperator.identity();
        return LanguageManager.apply(text, placeholders, external);
    }

    public String resolvePlaceholders(final PlatformPlayer questPlayer, final String text) {
        return resolvePlaceholders(questPlayer, text, null);
    }

    public String resolveActionText(
            final Actions.Data action,
            final PlatformPlayer questPlayer,
            final String text,
            final Object... objects) {
        String questIdentifier = null;
        for (final Object object : objects == null ? new Object[0] : objects) {
            if (object instanceof final ActiveObjective activeObjective) {
                questIdentifier = activeObjective.getQuestIdentifier();
                break;
            }
            if (object instanceof final Quest quest) {
                questIdentifier = quest.getIdentifier();
                break;
            }
        }
        return resolvePlaceholders(questPlayer, text, questIdentifier);
    }

    private String resolvePlaceholders(
            final PlatformPlayer questPlayer,
            final String text,
            final String questIdentifier) {
        final String resolved = UtilManager.applyNotQuestsPlaceholders(
                text,
                questIdentifier,
                questPlayer,
                runtimeAdapter());
        return externalPlaceholdersEnabled(questPlayer)
                ? questPlayer.applyExternalPlaceholders(resolved)
                : resolved;
    }

    private boolean externalPlaceholdersEnabled(final PlatformPlayer questPlayer) {
        return questPlayer != null
                && questPlayer.supportsExternalPlaceholders()
                && configuration.integrationPlaceholderApiEnabled();
    }

    public double placeholderNumber(final String rawValue, final boolean removeText) {
        String value = rawValue == null ? "" : rawValue;
        if (removeText) {
            value = value.replaceAll("[^\\d.]", "");
        }
        try {
            return Double.parseDouble(value);
        } catch (final NumberFormatException exception) {
            warn("Error: Placeholder Variable can not be parsed. Placeholder: <highlight>" + value);
            return 0.0d;
        }
    }

    public boolean toggleDebugPlayer(final PlatformPlayer questPlayer) {
        if (questPlayer == null || !questPlayer.hasPlayer()) {
            return false;
        }
        final String identifier = questPlayer.playerIdentifier();
        if (identifier == null || identifier.isBlank()) {
            return false;
        }
        if (debugPlayers.remove(identifier)) {
            return false;
        }
        debugPlayers.add(identifier);
        return true;
    }

    public boolean isDebugPlayer(final PlatformPlayer questPlayer) {
        return questPlayer != null && debugPlayers.contains(questPlayer.playerIdentifier());
    }

    public void sendDebugMessage(
            final PlatformPlayer questPlayer,
            final String message,
            final Object... interpolatedValues) {
        if (!isDebugPlayer(questPlayer)) {
            return;
        }
        final String text = interpolatedValues == null || interpolatedValues.length == 0
                ? message
                : message.formatted(interpolatedValues);
        questPlayer.sendMessage(NotQuestsColors.formatDebug(text));
    }

    public UpdateManager updateManager() {
        return updateManager;
    }

    public UpdateManager.Status updateStatus() {
        return updateManager.status();
    }

    public UpdateManager.Check checkForUpdates(final String currentVersion) throws Exception {
        return updateManager.check(currentVersion);
    }

    public String checkForUpdatesAndConsoleMessage(final String currentVersion) throws Exception {
        return updateManager.checkAndConsoleMessage(currentVersion);
    }

    public String checkForUpdatesAndChatMessage(final String currentVersion) throws Exception {
        return updateManager.checkAndChatMessage(currentVersion);
    }

    public String updateChatMessage(final String currentVersion) {
        return updateManager.chatMessage(currentVersion);
    }

    public String updateAvailableChatMessage(final String currentVersion) {
        return updateManager.availableChatMessage(currentVersion);
    }

    public boolean shouldNotifyOperatorAboutUpdate() {
        return updateManager.shouldNotifyOperator(configuration.updateCheckerNotifyOpsInChat());
    }

    public void startUpdateChecks(
            final String currentVersion,
            final BiConsumer<Duration, Runnable> asyncScheduler) {
        updateManager.start(currentVersion, asyncScheduler, () -> shuttingDown, this::info, this::warn);
    }

    public void operatorJoined(final PlatformPlayer player, final boolean operator) {
        updateManager.operatorJoined(
                player,
                operator,
                configuration.updateCheckerNotifyOpsInChat(),
                runtimeAdapter()::schedule);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public <Platform> void addRegistryPack(
            final Pack<Platform> registryPack) {
        registryPacks.add((Pack) registryPack);
    }

    @SuppressWarnings("unchecked")
    public <Platform> void registerRegistryPacks(final Platform platform) {
        registryPacks.registerAll(platform);
    }

    @SuppressWarnings("unchecked")
    public <Platform> void refreshRegistryPacksAfterVariableChange(final Platform platform) {
        registryPacks.refreshAfterVariableChange(platform);
    }

    @SuppressWarnings("unchecked")
    public <Platform> void notifyRegistryPacksAfterVariableValueChange(
            final Platform platform,
            final String variableName,
            final PlatformPlayer questPlayer) {
        registryPacks.notifyAfterVariableValueChange(platform, variableName, questPlayer);
    }

    public List<String> questNames() {
        return questManager.getQuestNames();
    }

    public List<String> questNamesInCategory(final String categoryName) {
        return questManager.getQuestNamesInCategory(categoryName);
    }

    public List<Quest> quests() {
        return questManager.getAllQuests();
    }

    public boolean finishConfiguredDataLoad() {
        for (final Quest quest : quests()) {
            for (final Objective objective : quest.getObjectives()) {
                if (!validateObjectiveTree(quest, objective, new int[] {objective.id()})) {
                    return false;
                }
            }
        }
        pluginStatus.setConfiguredDataLoaded(true);
        return true;
    }

    private boolean validateObjectiveTree(
            final Quest quest,
            final Objective objective,
            final int[] path) {
        if (objectiveType(objective.typeId()) == null) {
            disablePluginAndSaving(
                    "Cannot load quest <highlight>" + quest.getIdentifier()
                            + "</highlight>: objective <highlight>" + objectivePathKey(path)
                            + "</highlight> uses unknown type <highlight2>" + objective.typeId()
                            + "</highlight2>.",
                    List.of());
            return false;
        }
        for (final Objective child : objective.getObjectives()) {
            final int[] childPath = Arrays.copyOf(path, path.length + 1);
            childPath[childPath.length - 1] = child.id();
            if (!validateObjectiveTree(quest, child, childPath)) {
                return false;
            }
        }
        return true;
    }

    private static String objectivePathKey(final int[] path) {
        return Arrays.stream(path == null ? new int[0] : path)
                .mapToObj(String::valueOf)
                .collect(Collectors.joining("."));
    }

    public int questCount() {
        return questManager.getQuestCount();
    }

    public int conversationCount() {
        return conversations.conversations().size();
    }

    public Metrics metrics() {
        final Map<String, Supplier<Integer>> counts = new LinkedHashMap<>();
        counts.put("quests", this::questCount);
        counts.put("conversations", this::conversationCount);

        final Map<String, Supplier<Map<String, Integer>>> types = new LinkedHashMap<>();
        types.put("ObjectiveTypes", this::objectiveTypeCounts);
        types.put("ConditionTypes", this::conditionTypeCounts);
        types.put("AllActionTypes", this::actionTypeCounts);
        types.put("TriggerTypes", this::triggerTypeCounts);
        return new Metrics(12824, counts, types);
    }

    public Map<String, Integer> objectiveTypeCounts() {
        final Map<String, Integer> metrics = new HashMap<>();
        for (final Quest quest : quests()) {
            for (final Objective objective : quest.getObjectives()) {
                increment(metrics, objective.typeId());
            }
        }
        return Map.copyOf(metrics);
    }

    public Map<String, Integer> actionTypeCounts() {
        final Map<String, Integer> metrics = new HashMap<>();
        for (final Quest quest : quests()) {
            countActionEntries(metrics, quest.getRewards());
            countObjectiveActionEntries(metrics, quest.getObjectives());
        }
        for (final SavedActions.SavedAction action : savedActions.actions()) {
            increment(metrics, displayEntryType(action.getType().id(), action.getData().values()));
        }
        return Map.copyOf(metrics);
    }

    public Map<String, Integer> conditionTypeCounts() {
        final Map<String, Integer> metrics = new HashMap<>();
        for (final Quest quest : quests()) {
            countConditionEntries(metrics, quest.getRequirements());
            countObjectiveConditionEntries(metrics, quest.getObjectives());
        }
        for (final StoredCondition condition : savedConditions()) {
            increment(metrics, displayEntryType(condition.getType().id(), condition.getData().values()));
        }
        for (final SavedActions.SavedAction action : savedActions.actions()) {
            for (final SavedActions.SavedCondition condition : action.getConditions()) {
                increment(metrics, displayEntryType(condition.getType().id(), condition.getData().values()));
            }
        }
        return Map.copyOf(metrics);
    }

    public Map<String, Integer> triggerTypeCounts() {
        final Map<String, Integer> metrics = new HashMap<>();
        for (final Quest quest : quests()) {
            for (final Trigger trigger : quest.getTriggers()) {
                increment(metrics, trigger.typeId());
            }
        }
        return Map.copyOf(metrics);
    }

    private static void countObjectiveActionEntries(
            final Map<String, Integer> metrics,
            final List<Objective> objectives) {
        for (final Objective objective : objectives) {
            countActionEntries(metrics, objective.getRewards());
            countObjectiveActionEntries(metrics, objective.getObjectives());
        }
    }

    private static void countObjectiveConditionEntries(
            final Map<String, Integer> metrics,
            final List<Objective> objectives) {
        for (final Objective objective : objectives) {
            countConditionEntries(metrics, objective.getConditions("unlock"));
            countConditionEntries(metrics, objective.getConditions("progress"));
            countConditionEntries(metrics, objective.getConditions("complete"));
            countObjectiveConditionEntries(metrics, objective.getObjectives());
        }
    }

    private static void countActionEntries(
            final Map<String, Integer> metrics,
            final List<Action> entries) {
        for (final Action entry : entries) {
            increment(metrics, displayEntryType(entry.typeId(), entry.values()));
        }
    }

    private static void countConditionEntries(
            final Map<String, Integer> metrics,
            final List<Condition> entries) {
        for (final Condition entry : entries) {
            increment(metrics, displayEntryType(entry.typeId(), entry.values()));
        }
    }

    private static void increment(final Map<String, Integer> metrics, final String type) {
        final String key = type == null ? "" : type;
        metrics.put(key, metrics.getOrDefault(key, 0) + 1);
    }

    public static String displayEntryType(
            final String typeId,
            final Map<String, Object> data) {
        if (typeId != null
                && (typeId.equals("Number")
                || typeId.equals("String")
                || typeId.equals("Boolean")
                || typeId.equals("List")
                || typeId.equals("ItemStackList"))) {
            final Object configuredVariableName = data == null ? null : data.get("variableName");
            final String variableName = configuredVariableName == null ? "" : String.valueOf(configuredVariableName);
            if (!variableName.isBlank()) {
                return variableName;
            }
        }
        return typeId == null ? "" : typeId;
    }

    public List<String> categoryNames() {
        return questManager.getCategoryNames();
    }

    public String defaultCategoryName() {
        return questManager.getDefaultCategoryName();
    }

    public List<String> topLevelCategoryNames() {
        return questManager.getTopLevelCategoryNames();
    }

    public List<Category> categories() {
        return questManager.getAllCategories();
    }

    public List<String> tagNames() {
        return tags.keySet().stream().sorted().toList();
    }

    public List<String> tagNames(final TagType type) {
        return tags.values().stream()
                .filter(tag -> type == null || tag.tagType() == type)
                .map(Tag::tagName)
                .sorted()
                .toList();
    }

    public List<Tag> tags() {
        return tags.values().stream()
                .sorted(Comparator.comparing(Tag::tagName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public List<String> savedActionNames() {
        return savedActions.names();
    }

    public List<String> savedConditionNames() {
        return savedConditions.keySet().stream().sorted().toList();
    }

    public List<String> savedItemNames() {
        return savedItems.names();
    }

    public List<SavedItems.ItemChoice> resolveItems(final ItemSelection selection) {
        return savedItems.resolve(selection);
    }

    public List<SavedItems.ItemChoice> resolveItems(final Collection<? extends ItemSelection> selections) {
        final ArrayList<SavedItems.ItemChoice> resolved = new ArrayList<>();
        for (final ItemSelection selection
                : selections == null ? List.<ItemSelection>of() : selections) {
            resolved.addAll(savedItems.resolve(selection));
        }
        return List.copyOf(resolved);
    }

    public boolean itemsAreSimilar(final ItemSelection required, final ItemSelection actual) {
        return runtimeAdapter().itemSelectionsAreSimilar(required, actual);
    }

    public List<SavedItem> savedItems() {
        return savedItems.all();
    }

    public List<StoredCondition> savedConditions() {
        return savedConditions.values().stream()
                .sorted(Comparator.comparing(StoredCondition::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public List<String> conversationNames() {
        return conversations.names();
    }

    public List<Quest> questsAttachedToNpc(
            final String npcType,
            final NQNPCID npcId) {
        return questsAttachedToNpc(npcType, npcId, null);
    }

    public List<Quest> questsAttachedToNpc(
            final String npcType,
            final NQNPCID npcId,
            final Boolean questShowing) {
        if (npcId == null) {
            return List.of();
        }
        return quests().stream()
                .filter(quest -> hasNpcAttachment(quest, npcType, npcId, questShowing))
                .toList();
    }

    public List<NQNPCID> npcIdsWithShowingQuest(final String npcType) {
        if (npcType == null || npcType.isBlank()) {
            return List.of();
        }
        final LinkedHashSet<NQNPCID> ids = new LinkedHashSet<>();
        for (final Quest quest : quests()) {
            for (final NpcAttachment attachment : quest.getNpcAttachments()) {
                if (npcType.equalsIgnoreCase(attachment.npcType()) && attachment.questShowing()) {
                    ids.add(attachment.npcId());
                }
            }
        }
        return List.copyOf(ids);
    }

    public boolean shouldRemovePlatformQuestNpcTraitAfterAttachmentRemoval(
            final String questName,
            final String npcType,
            final NQNPCID npcId) {
        final Quest quest = quest(questName);
        return hasNpcAttachment(quest, npcType, npcId, null)
                && questsAttachedToNpc(npcType, npcId).size() <= 1;
    }

    public Detachments questNpcDetachments(final String questName) {
        return questNpcDetachments(questName, "");
    }

    public Detachments questNpcDetachments(
            final String questName,
            final String npcType) {
        final Quest quest = quest(questName);
        if (quest == null) {
            return new Detachments(List.of());
        }
        final ArrayList<Detachment> attachments =
                new ArrayList<>();
        for (final NpcAttachment attachment : quest.getNpcAttachments()) {
            if (npcType != null
                    && !npcType.isBlank()
                    && !npcType.equalsIgnoreCase(attachment.npcType())) {
                continue;
            }
            attachments.add(new Detachment(
                    quest.getIdentifier(),
                    attachment,
                    shouldRemovePlatformQuestNpcTraitAfterAttachmentRemoval(
                            quest.getIdentifier(),
                            attachment.npcType(),
                            attachment.npcId())));
        }
        return new Detachments(attachments);
    }

    public Detachments detachNpcFromQuests(
            final String npcType,
            final NQNPCID npcId) {
        if (npcType == null || npcType.isBlank() || npcId == null) {
            return new Detachments(List.of());
        }
        final ArrayList<Detachment> removals =
                new ArrayList<>();
        for (final Quest quest : quests()) {
            final List<NpcAttachment> matchingAttachments =
                    quest.getNpcAttachments().stream()
                            .filter(attachment -> attachment.npcType().equalsIgnoreCase(npcType)
                                    && attachment.npcId().equals(npcId))
                            .toList();
            for (final NpcAttachment attachment : matchingAttachments) {
                removals.add(new Detachment(
                        quest.getIdentifier(),
                        attachment,
                        shouldRemovePlatformQuestNpcTraitAfterAttachmentRemoval(
                                quest.getIdentifier(),
                                attachment.npcType(),
                                attachment.npcId())));
                quest.removeNpcAttachment(attachment.npcType(), attachment.npcId());
            }
        }
        if (!removals.isEmpty()) {
            saveConfiguredData();
        }
        return new Detachments(removals);
    }

    public boolean hasNpcAttachment(
            final Quest quest,
            final String npcType,
            final NQNPCID npcId,
            final Boolean questShowing) {
        if (quest == null || npcId == null) {
            return false;
        }
        for (final NpcAttachment attachment : quest.getNpcAttachments()) {
            if (questShowing != null && attachment.questShowing() != questShowing) {
                continue;
            }
            if (attachment.npcId() == null || !attachment.npcId().equals(npcId)) {
                continue;
            }
            if (npcType == null || npcType.isBlank() || attachment.npcType().isBlank()) {
                return true;
            }
            if (attachment.npcType().equalsIgnoreCase(npcType)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasNpcAttachment(final String npcType, final NQNPCID npcId) {
        return NpcAttachments.hasAttachment(
                quests(),
                conversations.conversations(),
                npcType,
                npcId);
    }

    public List<NQNPCID> npcIdsWithAttachment(final String npcType) {
        if (npcType == null || npcType.isBlank()) {
            return List.of();
        }
        final LinkedHashSet<NQNPCID> ids = new LinkedHashSet<>();
        for (final Quest quest : quests()) {
            for (final NpcAttachment attachment : quest.getNpcAttachments()) {
                if (npcType.equalsIgnoreCase(attachment.npcType()) && attachment.npcId() != null) {
                    ids.add(attachment.npcId());
                }
            }
        }
        for (final ConversationManager.Conversation conversation : conversations.conversations()) {
            for (final NpcAttachment attachment : conversation.npcAttachments()) {
                if (npcType.equalsIgnoreCase(attachment.npcType()) && attachment.npcId() != null) {
                    ids.add(attachment.npcId());
                }
            }
        }
        return List.copyOf(ids);
    }

    public List<NQNPCID> staleNpcAttachmentIds(
            final String npcType,
            final Collection<NQNPCID> nativeAttachments) {
        return NpcAttachments.staleNativeAttachments(
                npcIdsWithAttachment(npcType),
                nativeAttachments);
    }

    public int registerNpcSelection(final Consumer<NotQuestsAdapter.NpcSelection> selected) {
        return npcSelections.register(selected);
    }

    public boolean startNpcSelection(
            final NotQuestsAdapter adapter,
            final PlatformPlayer actor,
            final String successMessage,
            final String displayName,
            final List<String> lore,
            final Consumer<NotQuestsAdapter.NpcSelection> selected) {
        if (adapter == null || actor == null || !actor.hasPlayer() || selected == null) {
            return false;
        }
        final int selectionId = npcSelections.register(selected);
        if (selectionId < 0 || !adapter.giveNpcSelectionTool(actor, selectionId, displayName, lore)) {
            npcSelections.remove(selectionId);
            return false;
        }
        if (successMessage != null && !successMessage.isBlank()) {
            actor.sendMessage(successMessage);
        }
        return true;
    }

    public boolean completeNpcSelection(
            final int selectionId,
            final String npcType,
            final NQNPCID npcId,
            final String npcName) {
        return npcSelections.complete(selectionId, npcType, npcId, npcName);
    }

    public void reportNpcAttachmentCleanup(final int removed, final int checked) {
        info(NpcAttachments.cleanupMessage(removed, checked));
    }

    public boolean startEscort(
            final String playerIdentifier,
            final int escortNpcId,
            final int destinationNpcId,
            final NQLocation configuredSpawnLocation,
            final IntFunction<String> npcName,
            final NpcAttachments.EscortStarter nativeStart) {
        final PlatformPlayer player = activePlatformPlayer(playerIdentifier);
        final String escortNpcName = npcName == null ? null : npcName.apply(escortNpcId);
        final String destinationNpcName = npcName == null ? null : npcName.apply(destinationNpcId);
        if (destinationNpcName == null) {
            if (player != null) {
                player.sendMessage(NpcAttachments.missingEscortDestinationMessage());
            }
            warn(NpcAttachments.missingEscortDestinationWarning(destinationNpcId));
        }
        if (escortNpcName == null) {
            if (player != null) {
                player.sendMessage(NpcAttachments.missingEscortNpcMessage());
            }
            warn(NpcAttachments.missingEscortNpcWarning(escortNpcId));
        }
        if (player == null || !player.hasPlayer()) {
            warn(NpcAttachments.missingEscortPlayerWarning(playerIdentifier));
        }
        if (player == null || !player.hasPlayer()
                || escortNpcName == null || destinationNpcName == null || nativeStart == null) {
            return false;
        }
        final NQLocation spawnLocation = configuredSpawnLocation == null
                ? NQLocation.at(
                        player.worldName(),
                        player.positionX(),
                        player.positionY(),
                        player.positionZ(),
                        (float) player.yawDegrees(),
                        (float) player.pitchDegrees())
                : configuredSpawnLocation;
        if (!nativeStart.start(escortNpcId, playerIdentifier, spawnLocation)) {
            return false;
        }
        player.sendMessage(NpcAttachments.escortStartedMessage(escortNpcName, destinationNpcName));
        return true;
    }

    public void escortObjectiveUnlocked(
            final Objectives.Data objective,
            final PlatformPlayer questPlayer,
            final boolean loading,
            final IntFunction<String> npcName,
            final NpcAttachments.EscortStarter nativeStart) {
        if (loading || objective == null) {
            return;
        }
        startEscort(
                questPlayer == null ? "" : questPlayer.playerIdentifier(),
                objective.integer("npcToEscortId", -1),
                objective.integer("destinationNpcId", -1),
                objective.location("spawnLocation"),
                npcName,
                nativeStart);
    }

    public void escortObjectiveCompletedOrLocked(
            final Objectives.Data objective,
            final boolean loading,
            final IntConsumer stopFollowing) {
        if (loading || objective == null || stopFollowing == null) {
            return;
        }
        final int npcId = objective.integer("npcToEscortId", -1);
        if (npcId >= 0) {
            stopFollowing.accept(npcId);
        }
    }

    public String escortTaskDescription(
            final PlatformPlayer questPlayer,
            final ActiveObjective activeObjective,
            final Objectives.Data objective,
            final IntFunction<String> npcName) {
        final int escortNpcId = objective == null ? -1 : objective.integer("npcToEscortId", -1);
        final int destinationNpcId = objective == null ? -1 : objective.integer("destinationNpcId", -1);
        return escortTaskDescription(
                questPlayer,
                activeObjective,
                npcName == null ? null : npcName.apply(escortNpcId),
                npcName == null ? null : npcName.apply(destinationNpcId));
    }

    public String escortTaskDescription(
            final PlatformPlayer questPlayer,
            final ActiveObjective activeObjective,
            final String escortNpcName,
            final String destinationNpcName) {
        if (escortNpcName == null || destinationNpcName == null) {
            return "    <GRAY>The target or destination NPC could not be resolved.";
        }
        return translate(
                questPlayer,
                "chat.objectives.taskDescription.escortNPC.base",
                Map.of(
                        "%NPCNAME%", escortNpcName,
                        "%DESTINATIONNPCNAME%", destinationNpcName,
                        "%AMOUNT%", activeObjective == null
                                ? "1"
                                : String.valueOf(activeObjective.getProgressNeeded())),
                "    <GRAY>Escort <WHITE>%NPCNAME%</WHITE> to <WHITE>%DESTINATIONNPCNAME%</WHITE>.");
    }

    public boolean escortDestinationReached(
            final String playerIdentifier,
            final int destinationNpcId,
            final IntFunction<NpcAttachments.EscortNpc> escortNpc) {
        final PlatformPlayer player = activePlatformPlayer(playerIdentifier);
        if (player == null || escortNpc == null) {
            return false;
        }
        boolean handled = false;
        for (final ActiveObjective objective : matchingActiveObjectives(
                playerIdentifier,
                "EscortNPC",
                active -> active.integer("destinationNpcId", -1) == destinationNpcId)) {
            final int escortNpcId = objective.integer("npcToEscortId", -1);
            if (escortNpcId < 0) {
                continue;
            }
            final NpcAttachments.EscortNpc nativeNpc = escortNpc.apply(escortNpcId);
            if (nativeNpc == null) {
                continue;
            }
            if (!nativeNpc.sameWorld() || nativeNpc.distanceSquared() >= 36.0d) {
                player.sendMessage(NpcAttachments.escortTooFarMessage());
                continue;
            }
            objective.addProgress(1);
            player.sendMessage(NpcAttachments.escortDeliveredMessage(nativeNpc.name()));
            nativeNpc.finish().run();
            handled = true;
        }
        return handled;
    }

    public boolean shouldTrackNpcIndicator(final String npcType, final NQNPCID npcId) {
        final boolean enabled = "armorstand".equalsIgnoreCase(npcType)
                ? configuration.armorStandQuestGiverIndicatorParticleEnabled()
                : configuration.npcQuestGiverIndicatorParticleEnabled()
                        || !configuration.npcQuestGiverIndicatorText().isBlank();
        return enabled && hasNpcAttachment(npcType, npcId);
    }

    public NpcAttachments.Indicator npcIndicator(
            final String npcType,
            final NQNPCID npcId,
            final int particleTicks,
            final int textTicks,
            final double currentTps) {
        final boolean attached = hasNpcAttachment(npcType, npcId);
        final boolean armorStand = "armorstand".equalsIgnoreCase(npcType);
        final boolean healthyTps = armorStand
                ? configuration.armorStandQuestGiverIndicatorCanSpawnAtTps(currentTps)
                : configuration.npcQuestGiverIndicatorCanSpawnAtTps(currentTps);
        if (!attached || !healthyTps) {
            return NpcAttachments.Indicator.hidden(attached);
        }
        final boolean showParticle = armorStand
                ? configuration.armorStandQuestGiverIndicatorParticleEnabled()
                        && particleTicks >= configuration.armorStandQuestGiverIndicatorParticleSpawnInterval()
                : configuration.npcQuestGiverIndicatorParticleEnabled()
                        && particleTicks >= configuration.npcQuestGiverIndicatorParticleSpawnInterval();
        final int particleCount = armorStand
                ? configuration.armorStandQuestGiverIndicatorParticleCount()
                : configuration.npcQuestGiverIndicatorParticleCount();
        final String particleType = configuredParticle(
                armorStand
                        ? "visual.armorstands.quest-giver-indicator-particle.type"
                        : "visual.citizensnpc.quest-giver-indicator-particle.type",
                armorStand
                        ? configuration.armorStandQuestGiverIndicatorParticleType()
                        : configuration.npcQuestGiverIndicatorParticleType());
        final String text = armorStand ? "" : configuration.npcQuestGiverIndicatorText();
        final boolean showText = !text.isBlank()
                && textTicks >= configuration.npcQuestGiverIndicatorTextInterval();
        return new NpcAttachments.Indicator(
                attached, showParticle, particleCount, particleType, showText, text);
    }

    public boolean npcIndicatorVisibleTo(
            final String npcType,
            final NQNPCID npcId,
            final PlatformPlayer questPlayer) {
        return questPlayer != null
                && hasNpcAttachment(npcType, npcId)
                && !visibleQuestIdentifiers(
                        questPlayer,
                        questsAttachedToNpc(npcType, npcId, true),
                        System.currentTimeMillis(),
                        this::warn).isEmpty();
    }

    public void npcTraitAttached(final int npcId, final String npcName) {
        info(NpcAttachments.traitAttachedMessage(npcId, npcName));
    }

    public void nativeNpcTeleported(final Runnable restorePassenger) {
        if (restorePassenger != null) {
            actionScheduler.schedule(Duration.ofMillis(500L), restorePassenger);
        }
    }

    public NpcAttachments.Detachments npcRemoved(
            final String npcType,
            final NQNPCID npcId,
            final String npcName) {
        info(NpcAttachments.npcRemovedMessage(
                npcId == null ? -1 : npcId.getIntegerID(),
                npcName));
        for (final ConversationManager.Conversation conversation : conversations.conversations()) {
            conversations.removeNpcAttachment(conversation.name(), npcType, npcId);
        }
        return detachNpcFromQuests(npcType, npcId);
    }

    public List<QuestPlayer> questPlayers() {
        return questPlayerManager.getAllQuestPlayers();
    }

    public List<PlayerDatabase.PlayerSnapshot> playerRuntimeSnapshots() {
        return PlayerDatabase.snapshots(questPlayers().stream()
                .map(this::storedPlayer)
                .toList());
    }

    public void savePlayerRuntime(
            final Connection connection,
            final Collection<String> playerIdentifiers)
            throws SQLException {
        final Set<String> identifiers = new HashSet<>();
        for (final String playerIdentifier : playerIdentifiers == null ? List.<String>of() : playerIdentifiers) {
            if (playerIdentifier != null && !playerIdentifier.isBlank()) {
                identifiers.add(playerIdentifier);
            }
        }
        PlayerDatabase.save(connection, playerRuntimeSnapshots().stream()
                .filter(snapshot -> identifiers.contains(snapshot.playerIdentifier()))
                .toList());
    }

    public void savePlayerRuntime(final Collection<String> playerIdentifiers)
            throws SQLException {
        try (Connection connection = playerDatabaseConnection()) {
            savePlayerRuntime(connection, playerIdentifiers);
        }
    }

    public List<QuestPlayer> loadPlayerRuntime(
            final Connection connection,
            final String playerIdentifier,
            final Function<String, PlatformPlayer> playerFactory,
            final Consumer<String> warningSink)
            throws SQLException {
        final ArrayList<QuestPlayer> restored = new ArrayList<>();
        for (final PlayerDatabase.LoadedPlayer loadedPlayer :
                PlayerDatabase.load(connection, playerIdentifier)) {
            final PlatformPlayer platformPlayer =
                    playerFactory == null ? null : playerFactory.apply(loadedPlayer.playerIdentifier());
            registerQuestPlayer(
                    platformPlayer,
                    loadedPlayer.profile(),
                    loadedPlayer.profile().equalsIgnoreCase(loadedPlayer.activeProfile()));
            final QuestPlayer questPlayer = restorePlayerRuntime(
                    loadedPlayer,
                    platformPlayer,
                    warningSink);
            if (questPlayer != null) {
                markPlayerLoaded(questPlayer);
                restored.add(questPlayer);
            }
        }
        return List.copyOf(restored);
    }

    public List<QuestPlayer> loadPlayerRuntime(
            final String playerIdentifier,
            final Function<String, PlatformPlayer> questPlayer,
            final Consumer<String> warningSink)
            throws SQLException {
        try (Connection connection = playerDatabaseConnection()) {
            return loadPlayerRuntime(connection, playerIdentifier, questPlayer, warningSink);
        }
    }

    public boolean loadPlayerData(
            final String playerIdentifier,
            final Function<String, PlatformPlayer> playerFactory) {
        if (!configuration.loadPlayerData()) {
            info("Loading of PlayerData has been skipped...");
            return true;
        }
        final boolean onePlayer = playerIdentifier != null && !playerIdentifier.isBlank();
        questPlayerManager.beginLoading(playerIdentifier);
        if (onePlayer) {
            clearPlayerRuntime(playerIdentifier);
            if (configuration.verboseStartupMessages()) {
                info("Loading PlayerData of player %s...", playerIdentifier);
            }
        }
        try {
            loadPlayerRuntime(
                    playerIdentifier,
                    playerFactory,
                    this::warn);
            if (playerIdentifier != null
                    && !playerIdentifier.isBlank()
                    && activePlatformPlayer(playerIdentifier) == null
                    && playerFactory != null) {
                final PlatformPlayer player = playerFactory.apply(playerIdentifier);
                registerQuestPlayer(player, "default", true);
                markPlayerLoaded(player);
            }
            return true;
        } catch (final Exception exception) {
            final String target = playerIdentifier == null || playerIdentifier.isBlank()
                    ? "all players"
                    : "player <highlight>" + playerIdentifier + "</highlight>";
            disablePluginAndSaving(
                    "There was a database error while loading PlayerData for " + target + ".",
                    List.of(DisableReason.throwable(exception)));
            return false;
        } finally {
            questPlayerManager.finishLoading(playerIdentifier);
        }
    }

    public boolean savePlayerData(final String playerIdentifier) {
        if (!canSaveLoadedData()) {
            return false;
        }
        if (!configuration.savePlayerData()) {
            info("Saving of PlayerData has been skipped...");
            return true;
        }
        if (playerIdentifier == null || playerIdentifier.isBlank()) {
            warn("Saving of single PlayerData has been skipped because the player identifier is blank.");
            return false;
        }
        if (questPlayerManager.isLoading(playerIdentifier)) {
            info("Saving of PlayerData for %s has been skipped because loading has not finished.", playerIdentifier);
            return false;
        }
        for (final QuestPlayer player : questPlayers()) {
            if (playerIdentifier.equals(player.getPlayerIdentifier())
                    && !player.isFinishedLoadingGeneralData()) {
                info("Saving of PlayerData for %s has been skipped because loading has not finished.", playerIdentifier);
                return false;
            }
        }
        try {
            savePlayerRuntime(List.of(playerIdentifier));
            if (configuration.verboseStartupMessages()) {
                info("PlayerData of player %s was saved.", playerIdentifier);
            }
            return true;
        } catch (final Exception exception) {
            warn("There was an error saving PlayerData for <highlight>" + playerIdentifier + "</highlight>.", exception);
            return false;
        }
    }

    public boolean saveAllPlayerData() {
        if (!canSaveLoadedData()) {
            return false;
        }
        if (!configuration.savePlayerData()) {
            info("Saving of PlayerData has been skipped...");
            return true;
        }
        if (questPlayerManager.isLoadingAllPlayers()) {
            info("Saving of PlayerData has been skipped because loading has not finished.");
            return false;
        }
        final Set<String> identifiers = questPlayerManager.getPlayersReadyToSave();
        try {
            info("Saving player data...");
            savePlayerRuntime(identifiers);
            info("PlayerData of all players saved");
            return true;
        } catch (final Exception exception) {
            warn("There was an error saving PlayerData.", exception);
            return false;
        }
    }

    public boolean loadPlayerOnJoin(
            final String playerIdentifier,
            final Function<String, PlatformPlayer> playerFactory) {
        if (configuration.loadPlayerDataOnJoin()) {
            return loadPlayerData(playerIdentifier, playerFactory);
        }
        if (activePlatformPlayer(playerIdentifier) == null && playerFactory != null) {
            final String profile = activeProfile(playerIdentifier);
            final PlatformPlayer player = playerFactory.apply(playerIdentifier);
            registerQuestPlayer(player, profile, true);
            markPlayerLoaded(player);
        }
        return true;
    }

    public boolean savePlayerOnQuit(final String playerIdentifier) {
        final boolean saved = !configuration.savePlayerDataOnQuit() || savePlayerData(playerIdentifier);
        removeQuestPlayers(playerIdentifier);
        return saved;
    }

    public void playerJoined(
            final String playerIdentifier,
            final Function<String, PlatformPlayer> playerFactory,
            final BooleanSupplier stillConnected,
            final Consumer<PlatformPlayer> attach,
            final BooleanSupplier operator,
            final Consumer<Runnable> runAsync,
            final Consumer<Runnable> runOnPlatformThread) {
        final Runnable load = () -> {
            final boolean loaded = loadPlayerOnJoin(playerIdentifier, playerFactory);
            final Runnable finish = () -> {
                if (!loaded || stillConnected == null || !stillConnected.getAsBoolean()) {
                    removeQuestPlayers(playerIdentifier);
                    return;
                }
                final PlatformPlayer questPlayer = activePlatformPlayer(playerIdentifier);
                if (questPlayer == null) {
                    removeQuestPlayers(playerIdentifier);
                    return;
                }
                if (attach != null) {
                    attach.accept(questPlayer);
                }
                operatorJoined(questPlayer, operator != null && operator.getAsBoolean());
            };
            if (runOnPlatformThread == null) {
                finish.run();
            } else {
                runOnPlatformThread.accept(finish);
            }
        };
        if (runAsync == null) {
            load.run();
        } else {
            runAsync.accept(load);
        }
    }

    public void playerLeft(
            final String playerIdentifier,
            final PlatformPlayer questPlayer,
            final String worldName,
            final Runnable platformCleanup,
            final Consumer<Runnable> runAsync) {
        if (questPlayer != null) {
            playerDisconnected(questPlayer, worldName);
            questPlayerManager.getActiveObjectives().removePlayerObservations(playerIdentifier);
        }
        if (platformCleanup != null) {
            platformCleanup.run();
        }
        final Runnable save = () -> savePlayerOnQuit(playerIdentifier);
        if (runAsync == null) {
            save.run();
        } else {
            runAsync.accept(save);
        }
    }

    private void clearAllPlayerRuntime() {
        questPlayerManager.clear();
        progressBossBarAges.clear();
    }

    private void clearPlayerRuntime(final String playerIdentifier) {
        questPlayerManager.clear(playerIdentifier);
        progressBossBarAges.remove(playerIdentifier);
    }

    private void markPlayerLoaded(final PlatformPlayer player) {
        questPlayerManager.markLoaded(player);
    }

    private void markPlayerLoaded(final QuestPlayer questPlayer) {
        questPlayerManager.markLoaded(questPlayer);
    }

    public Map<String, String> activeProfiles() {
        return questPlayerManager.getActiveProfiles();
    }

    private PlayerDatabase.StoredPlayer storedPlayer(final QuestPlayer playerData) {
        return new PlayerDatabase.StoredPlayer() {
            @Override
            public String playerIdentifier() {
                return playerData.getPlayerIdentifier();
            }

            @Override
            public String profile() {
                return playerData.getProfile();
            }

            @Override
            public String activeProfile() {
                return NotQuestsPlugin.this.activeProfile(playerData.getPlayerIdentifier());
            }

            @Override
            public long questPoints() {
                return playerData.getQuestPoints();
            }

            @Override
            public List<? extends PlayerDatabase.StoredActiveQuest> activeQuests() {
                return playerData.getActiveQuestIdentifiers().stream()
                        .sorted(String.CASE_INSENSITIVE_ORDER)
                        .map(questIdentifier -> storedActiveQuest(playerData, questIdentifier))
                        .toList();
            }

            @Override
            public List<QuestPlayer.CompletedQuest> completedQuests() {
                return playerData.getCompletedQuests();
            }

            @Override
            public List<QuestPlayer.FailedQuest> failedQuests() {
                return playerData.getFailedQuests();
            }

            @Override
            public Map<String, Object> tags() {
                return playerData.getTags();
            }
        };
    }

    private PlayerDatabase.StoredActiveQuest storedActiveQuest(
            final QuestPlayer playerData,
            final String questIdentifier) {
        return new PlayerDatabase.StoredActiveQuest() {
            @Override
            public String questIdentifier() {
                return questIdentifier;
            }

            @Override
            public List<? extends PlayerDatabase.StoredActiveTrigger> activeTriggers() {
                return NotQuestsPlugin.this.activeTriggers(
                                playerData.getPlayerIdentifier(),
                                playerData.getProfile()).stream()
                        .filter(trigger -> trigger.questName().equalsIgnoreCase(questIdentifier))
                        .map(NotQuestsPlugin::storedActiveTrigger)
                        .toList();
            }

            @Override
            public List<? extends PlayerDatabase.StoredActiveObjective> activeObjectives() {
                return questPlayerManager.getActiveObjectives().activeObjectives(
                                playerData.getPlayerIdentifier(),
                                playerData.getProfile()).stream()
                        .filter(objective -> objective.getQuestIdentifier().equalsIgnoreCase(questIdentifier))
                        .map(NotQuestsPlugin::storedActiveObjective)
                        .toList();
            }

            @Override
            public List<? extends PlayerDatabase.StoredActiveObjective> completedObjectives() {
                final ArrayList<PlayerDatabase.StoredActiveObjective> completed =
                        new ArrayList<>();
                for (final QuestPlayer.CompletedObjective record : playerData.getCompletedObjectives(questIdentifier)) {
                    final PlayerDatabase.StoredActiveObjective objective =
                            NotQuestsPlugin.this.storedCompletedObjective(record);
                    if (objective != null) {
                        completed.add(objective);
                    }
                }
                for (final String objectivePath : playerData.getCompletedObjectiveIDs(questIdentifier)) {
                    if (playerData.getCompletedObjective(questIdentifier, objectivePath) == null) {
                        final PlayerDatabase.StoredActiveObjective objective =
                                NotQuestsPlugin.this.storedCompletedObjective(completedObjectiveFallback(questIdentifier, objectivePath));
                        if (objective != null) {
                            completed.add(objective);
                        }
                    }
                }
                return completed;
            }
        };
    }

    private static PlayerDatabase.StoredActiveTrigger storedActiveTrigger(
            final ActiveTrigger trigger) {
        return new PlayerDatabase.StoredActiveTrigger() {
            @Override
            public String triggerType() {
                return trigger.triggerType();
            }

            @Override
            public long currentProgress() {
                return trigger.currentProgress();
            }

            @Override
            public int triggerId() {
                return trigger.triggerId();
            }
        };
    }

    private static PlayerDatabase.StoredActiveObjective storedActiveObjective(
            final ActiveObjective objective) {
        return new PlayerDatabase.StoredActiveObjective() {
            @Override
            public String objectiveType() {
                return objective.getObjectiveTypeID();
            }

            @Override
            public String holderPath() {
                return objective.getHolderPath();
            }

            @Override
            public double currentProgress() {
                return objective.currentProgress();
            }

            @Override
            public int objectiveId() {
                return objective.getObjectiveID();
            }

            @Override
            public boolean completed() {
                return false;
            }

            @Override
            public double progressNeeded() {
                return objective.getProgressNeeded();
            }

            @Override
            public List<? extends PlayerDatabase.StoredActiveObjective> activeObjectives() {
                return List.of();
            }

            @Override
            public List<? extends PlayerDatabase.StoredActiveObjective> completedObjectives() {
                return List.of();
            }
        };
    }

    private PlayerDatabase.StoredActiveObjective storedCompletedObjective(
            final QuestPlayer.CompletedObjective completedObjective) {
        if (completedObjective == null) {
            return null;
        }
        final int[] objectivePath = parseObjectivePath(completedObjective.objectivePath());
        final Objective objective = objectiveAt(quest(completedObjective.questIdentifier()), objectivePath);
        if (objective == null) {
            return null;
        }
        final double progressNeeded = completedObjective.progressNeeded() > 0
                ? completedObjective.progressNeeded()
                : progressNeeded(objective);
        return new PlayerDatabase.StoredActiveObjective() {
            @Override
            public String objectiveType() {
                return completedObjective.objectiveType().isBlank()
                        ? objective.typeId()
                        : completedObjective.objectiveType();
            }

            @Override
            public String holderPath() {
                return completedObjective.holderPath().isBlank()
                        ? NotQuestsPlugin.this.holderPath(completedObjective.questIdentifier(), objectivePath)
                        : completedObjective.holderPath();
            }

            @Override
            public double currentProgress() {
                return completedObjective.currentProgress();
            }

            @Override
            public int objectiveId() {
                return objective.id();
            }

            @Override
            public boolean completed() {
                return true;
            }

            @Override
            public double progressNeeded() {
                return progressNeeded;
            }

            @Override
            public List<? extends PlayerDatabase.StoredActiveObjective> activeObjectives() {
                return List.of();
            }

            @Override
            public List<? extends PlayerDatabase.StoredActiveObjective> completedObjectives() {
                return List.of();
            }
        };
    }

    private QuestPlayer.CompletedObjective completedObjectiveFallback(
            final String questIdentifier,
            final String objectivePathString) {
        final int[] objectivePath = parseObjectivePath(objectivePathString);
        final Objective objective = objectiveAt(quest(questIdentifier), objectivePath);
        final double progressNeeded = progressNeeded(objective);
        return new QuestPlayer.CompletedObjective(
                questIdentifier,
                objectivePathString,
                holderPath(questIdentifier, objectivePath),
                objective == null ? "" : objective.typeId(),
                progressNeeded,
                progressNeeded);
    }

    private static int[] parseObjectivePath(final String objectivePathString) {
        if (objectivePathString == null || objectivePathString.isBlank()) {
            return new int[0];
        }
        final String[] tokens = objectivePathString.split("\\.");
        final int[] objectivePath = new int[tokens.length];
        for (int i = 0; i < tokens.length; i++) {
            try {
                objectivePath[i] = Integer.parseInt(tokens[i]);
            } catch (final NumberFormatException ignored) {
                return new int[0];
            }
        }
        return objectivePath;
    }

    private static String holderPath(final String questIdentifier, final int[] objectivePath) {
        if (questIdentifier == null || questIdentifier.isBlank() || objectivePath == null || objectivePath.length <= 1) {
            return questIdentifier == null ? "" : questIdentifier;
        }
        final StringBuilder builder = new StringBuilder(questIdentifier);
        for (int i = 0; i < objectivePath.length - 1; i++) {
            builder.append('.').append(objectivePath[i]);
        }
        return builder.toString();
    }

    private double progressNeeded(final Objective objective) {
        if (objective == null) {
            return 1;
        }
        final Objectives.Type type = registry.objectives().stream()
                .filter(objectiveType -> objectiveType.id().equalsIgnoreCase(objective.typeId()))
                .findFirst()
                .orElse(null);
        if (type != null) {
            for (final RegistryField.Definition field : type.fields()) {
                if (field.progressNeeded()) {
                    final double fieldValue = progressNeededValue(objective.data().value(field.name()));
                    if (fieldValue > 0) {
                        return fieldValue;
                    }
                }
            }
        }
        return 1;
    }

    private static double progressNeededValue(final Object amount) {
        if (amount instanceof Number number) {
            return number.doubleValue();
        }
        if (amount != null) {
            try {
                return Double.parseDouble(amount.toString());
            } catch (final NumberFormatException ignored) {
                return 1;
            }
        }
        return 1;
    }

    public void clearStoredData() {
        clearStoredData(true);
    }

    public void clearStoredData(final boolean clearRuntimeData) {
        pluginStatus.setConfiguredDataLoaded(false);
        savedActions.clear();
        conversations.clear();
        if (clearRuntimeData) {
            questPlayerManager.clear();
        } else {
            questPlayerManager.clearProgress();
        }
        questManager.clear();
        tags.clear();
        savedConditions.clear();
        savedItems.clear();
        npcSelections.clear();
        chatHistory.clear();
        progressBossBarAges.clear();
    }

    public void rememberNonConversationDisplayMessage(final String playerIdentifier, final Component component) {
        conversations.rememberNonConversationMessage(playerIdentifier, component, configuration().previousConversationHistorySize());
        rememberChatHistoryMessage(playerIdentifier, component);
    }

    public Component nonConversationDisplayMessage(
            final String playerIdentifier,
            final String miniMessageText) {
        final Component component = NotQuestsMiniMessage.create(configuration).deserialize(
                miniMessageText == null ? "" : miniMessageText);
        rememberNonConversationDisplayMessage(playerIdentifier, component);
        return component;
    }

    public void rememberConversationDisplayMessage(final String playerIdentifier, final Component component) {
        conversations.rememberConversationMessage(playerIdentifier, component);
    }

    public void conversationDisplayMessage(final String playerIdentifier, final Component component) {
        if (configuration.deletePreviousConversationMessages()) {
            conversations.rememberConversationMessage(playerIdentifier, component);
        }
    }

    public Component conversationOptionReplay(final String playerIdentifier) {
        if (!configuration.deletePreviousConversationMessages()) {
            return null;
        }
        return conversations.removeConversationMessages(playerIdentifier);
    }

    public void rememberChatHistoryMessage(final String playerIdentifier, final Component component) {
        if (playerIdentifier == null || playerIdentifier.isBlank() || component == null) {
            return;
        }
        chatHistory.compute(playerIdentifier, (ignored, history) -> {
            final List<Component> mutable = history == null
                    ? new CopyOnWriteArrayList<>()
                    : history;
            mutable.add(component);
            return mutable;
        });
    }

    public boolean playerChatted(
            final PlatformPlayer questPlayer,
            final String plainMessage,
            final Map<String, Component> renderedMessageByRecipient) {
        if (questPlayer != null
                && configuration.conversationAnswerNumberInChatEnabled()
                && hasActiveConversation(questPlayer)
                && runtimeAdapter().hasPermission(questPlayer, "notquests.use")) {
            try {
                final int option = Integer.parseInt((plainMessage == null ? "" : plainMessage).replace(".", ""));
                if (chooseConversationOption(questPlayer, option)) {
                    return true;
                }
            } catch (final NumberFormatException ignored) {
            }
        }
        if (renderedMessageByRecipient != null) {
            renderedMessageByRecipient.forEach(this::rememberNonConversationDisplayMessage);
        }
        return false;
    }

    public List<String> conversationChatHistory(final PlatformPlayer questPlayer, final boolean indexed) {
        if (questPlayer == null) {
            return List.of();
        }
        return conversationChatHistory(questPlayer.playerIdentifier(), indexed);
    }

    public List<String> conversationChatHistory(final String playerIdentifier, final boolean indexed) {
        if (playerIdentifier == null || playerIdentifier.isBlank()) {
            return List.of();
        }
        final List<Component> history = chatHistory.get(playerIdentifier);
        if (history == null || history.isEmpty()) {
            return List.of();
        }
        final ArrayList<String> lines = new ArrayList<>();
        for (int i = 0; i < history.size(); i++) {
            final Component component = history.get(i);
            if (component == null) {
                continue;
            }
            final String serialized = miniMessage.serialize(component);
            lines.add(indexed ? "<red>" + i + ".</red>" + serialized : serialized);
        }
        return List.copyOf(lines);
    }

    public boolean putSavedItem(
            final String itemName,
            final ItemSelection itemSelection,
            final String categoryName,
            final String displayName) {
        if (itemName == null || itemName.isBlank() || itemSelection == null) {
            return false;
        }
        if (categoryName != null && !categoryName.isBlank()) {
            getOrCreateCategory(categoryName);
        }
        return savedItems.add(itemName, itemSelection, categoryName, displayName);
    }

    public boolean deleteSavedItem(final String itemName) {
        return savedItems.remove(itemName);
    }

    public SavedItem savedItem(final String itemName) {
        return savedItems.get(itemName);
    }

    public boolean setSavedItemDisplayName(final String itemName, final String displayName) {
        return savedItems.setDisplayName(itemName, displayName);
    }

    public void journalPlayerJoined(
            final String worldName,
            final IntConsumer replaceJournal) {
        savedItems.journalPlayerJoined(
                configuration.journalEnabledWorlds(),
                configuration.journalInventorySlot(),
                worldName,
                replaceJournal);
    }

    public void journalItemUsed(
            final PlatformPlayer questPlayer,
            final boolean rightClick,
            final boolean journalItem) {
        if (questPlayer != null && savedItems.journalItemUsed(rightClick, journalItem)) {
            openGui(questPlayer, "main-active", "", GuiContext.EMPTY);
        }
    }

    public void journalPlayerRespawned(
            final String deathWorld,
            final String respawnWorld,
            final IntPredicate journalInSlot,
            final IntConsumer replaceJournal) {
        savedItems.journalPlayerRespawned(
                configuration.journalEnabledWorlds(),
                configuration.journalInventorySlot(),
                deathWorld,
                respawnWorld,
                journalInSlot,
                replaceJournal);
    }

    public void journalPlayerDied(
            final String worldName,
            final boolean journalInDrops,
            final Runnable removeJournalDrop) {
        savedItems.journalPlayerDied(
                configuration.journalEnabledWorlds(),
                worldName,
                journalInDrops,
                removeJournalDrop);
    }

    public void journalItemPickedUpOrDropped(
            final String worldName,
            final boolean journalItem,
            final Runnable cancel) {
        savedItems.journalItemPickedUpOrDropped(
                configuration.journalEnabledWorlds(),
                worldName,
                journalItem,
                cancel);
    }

    public void journalInventoryClicked(
            final String worldName,
            final boolean clickedJournal,
            final IntPredicate journalInSlot,
            final Runnable cancel,
            final Runnable refreshCreativeInventory,
            final IntConsumer replaceJournal) {
        savedItems.journalInventoryClicked(
                configuration.journalEnabledWorlds(),
                configuration.journalInventorySlot(),
                worldName,
                clickedJournal,
                journalInSlot,
                cancel,
                refreshCreativeInventory,
                replaceJournal);
    }

    public void putSavedCondition(final StoredCondition condition) {
          if (condition == null || condition.getName() == null || condition.getName().isBlank()) {
            return;
        }
          savedConditions.put(condition.getName(), condition);
    }

    public void setActiveProfile(final String playerIdentifier, final String profile) {
        questPlayerManager.setActiveProfile(playerIdentifier, profile);
    }

    public List<String> triggerCommandNames() {
        final ArrayList<String> names = new ArrayList<>();
        for (final Quest quest : quests()) {
            for (final Objective objective : quest.getObjectives()) {
                if (!"TriggerCommand".equalsIgnoreCase(objective.typeId())) {
                    continue;
                }
                final String triggerName = objective.data().text("triggerName");
                if (!triggerName.isBlank()) {
                    names.add(triggerName);
                }
            }
        }
        questPlayerManager.getActiveObjectives().activeTriggerCommandNames().stream()
                .filter(triggerName -> names.stream().noneMatch(triggerName::equalsIgnoreCase))
                .forEach(names::add);
        return names.stream()
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    public Quest getOrCreateQuest(final String questName) {
        return questManager.getOrCreateQuest(questName);
    }

    public Quest quest(final String questName) {
        return questManager.getQuest(questName);
    }

    public String questCategory(final String questName) {
        final Quest quest = quest(questName);
        return quest == null ? Category.DEFAULT_NAME : quest.getCategory();
    }

    public int questMaxCompletions(final String questName) {
        final Quest quest = quest(questName);
        return quest == null ? -1 : quest.getMaxCompletions();
    }

    public int questMaxAccepts(final String questName) {
        final Quest quest = quest(questName);
        return quest == null ? -1 : quest.getMaxAccepts();
    }

    public int questMaxFails(final String questName) {
        final Quest quest = quest(questName);
        return quest == null ? -1 : quest.getMaxFails();
    }

    public boolean questTakeEnabled(final String questName) {
        final Quest quest = quest(questName);
        return quest == null || quest.isTakeEnabled();
    }

    public boolean questAbortEnabled(final String questName) {
        final Quest quest = quest(questName);
        return quest == null || quest.isAbortEnabled();
    }

    public long questAcceptCooldownComplete(final String questName) {
        final Quest quest = quest(questName);
        return quest == null ? -1 : quest.getAcceptCooldownComplete();
    }

    public String questDescription(final String questName) {
        final Quest quest = quest(questName);
        return quest == null ? "" : quest.getDescription();
    }

    public String questDescriptionWrapped(final String questName, final int maxLengthPerLine) {
        return UtilManager.wrap(questDescription(questName), maxLengthPerLine, configuration.wrapLongWords());
    }

    public List<String> questDescriptionLines(final String questName, final int maxLengthPerLine) {
        return UtilManager.wrapToList(questDescription(questName), maxLengthPerLine, configuration.wrapLongWords());
    }

    public String questDisplayName(final String questName) {
        final Quest quest = quest(questName);
        return quest == null ? "" : quest.getDisplayName();
    }

    public String questDisplayNameOrIdentifier(final String questName) {
        final Quest quest = quest(questName);
        return quest == null ? (questName == null ? "" : questName) : quest.getDisplayNameOrIdentifier();
    }

    public String questObjectiveProgressOrder(final String questName) {
        final Quest quest = quest(questName);
        return quest == null ? "" : quest.getObjectiveProgressOrder();
    }

    public ItemSelection questGuiItemSelection(final String questName) {
        final Quest quest = quest(questName);
        return quest == null ? null : quest.getGuiItemSelection();
    }

    public List<NpcAttachment> questNpcAttachments(final String questName, final boolean questShowing) {
        final Quest quest = quest(questName);
        return quest == null ? List.of() : NpcAttachments.questAttachments(quest.getNpcAttachments(), questShowing);
    }

    public void applyNpcAttachments() {
        try {
            runtimeAdapter().callOnServerThread(() -> {
                applyNpcAttachmentsOnPlatformThread();
                return null;
            });
        } catch (final InterruptedException exception) {
            Thread.currentThread().interrupt();
            warn("NPC attachment application was interrupted.");
        } catch (final Exception exception) {
            warn("Could not apply NPC attachments on the platform thread: " + exception.getMessage());
        }
    }

    private void applyNpcAttachmentsOnPlatformThread() {
        final NpcAttachments.QuestAttachments questAttachments = NpcAttachments.collect(quests());
        questAttachments.warnings().forEach(this::warn);
        for (final NpcAttachments.Attachment attachment : questAttachments.attachments()) {
            final NpcAttachment npc = attachment.attachment();
            final String selector = npc.npcType() + ":" + npc.npcId().getEitherAsString();
            if (!setNativeNpcQuestGiver(npcSelection(npc), true)) {
                warn("Could not attach quest <highlight>" + attachment.questIdentifier()
                        + "</highlight> to NPC <highlight2>" + selector + "</highlight2>.");
            }
        }
        final NpcAttachments.ConversationAttachments conversationAttachments =
                NpcAttachments.collectConversations(conversations.conversations());
        conversationAttachments.warnings().forEach(this::warn);
        for (final NpcAttachments.ConversationAttachment attachment : conversationAttachments.attachments()) {
            final NpcAttachment npc = attachment.attachment();
            final String selector = npc.npcType() + ":" + npc.npcId().getEitherAsString();
            setNativeNpcQuestGiver(npcSelection(npc), true);
        }
    }

    public boolean attachQuestNpc(
            final String questName,
            final NotQuestsAdapter.NpcSelection selection,
            final boolean showQuestInNpc) {
        if (selection == null || selection.npcId() == null
                || !setNativeNpcQuestGiver(selection, true)) {
            return false;
        }
        syncQuestNpcAttachment(
                questName,
                selection.npcType(),
                selection.npcId(),
                selection.npcName(),
                showQuestInNpc);
        saveConfiguredData();
        return true;
    }

    public void attachConversationNpc(
            final String conversationName,
            final NotQuestsAdapter.NpcSelection selection) {
        if (selection == null || selection.npcId() == null
                || !setNativeNpcQuestGiver(selection, true)) {
            return;
        }
        syncConversationNpcAttachment(
                conversationName,
                selection.npcType(),
                selection.npcId(),
                selection.npcName());
        saveConfiguredData();
    }

    public void applyNpcDetachments(final NpcAttachments.Detachments detachments) {
        if (detachments == null) {
            return;
        }
        for (final NpcAttachments.Detachment detachment : detachments.attachments()) {
            if (detachment == null || !detachment.removePlatformTrait()) {
                continue;
            }
            setNativeNpcQuestGiver(npcSelection(detachment.attachment()), false);
        }
    }

    private boolean setNativeNpcQuestGiver(
            final NotQuestsAdapter.NpcSelection selection,
            final boolean enabled) {
        if (selection == null || selection.npcId() == null || selection.npcType().isBlank()) {
            return false;
        }
        final boolean requiresNativeEffect = runtimeAdapter().nativeNpcQuestGiverTypes().stream()
                .anyMatch(selection.npcType()::equalsIgnoreCase);
        return !requiresNativeEffect || runtimeAdapter().setNpcQuestGiver(selection, enabled);
    }

    private static NotQuestsAdapter.NpcSelection npcSelection(final NpcAttachment npc) {
        final String selector = NpcAttachments.selector(npc.npcType(), npc.npcId());
        return new NotQuestsAdapter.NpcSelection(
                selector,
                NpcAttachments.formatAttachedNPC(npc.npcType(), npc.npcId(), npc.npcName()),
                npc.npcType(),
                npc.npcId(),
                npc.npcName());
    }

    public CommandMessage createQuest(final String questName) {
        return createQuest(questName, "");
    }

    public CommandMessage createQuest(final String questName, final String categoryName) {
        if (questName == null || questName.isBlank()) {
            return CommandMessage.error("<error>Quest name cannot be blank.");
        }
        if (questName.contains("°")) {
            return CommandMessage.error("<error>The symbol <highlight>°</highlight>"
                    + " cannot be used, because it's used for some important, plugin-internal stuff.");
        }
        if (quest(questName) != null) {
            return CommandMessage.error("<error>Quest <highlight>" + questName + "</highlight> already exists!");
        }
        if (categoryName != null && !categoryName.isBlank() && category(categoryName) == null) {
            return CommandMessage.error("<error>No Category found: " + categoryName);
        }
        final Quest quest = questManager.createQuest(questName, categoryName);
        if (quest == null) {
            return CommandMessage.error("<error>Quest <highlight>" + questName + "</highlight> already exists!");
        }
        saveConfiguredData();
        return CommandMessage.success("<success>Quest <highlight>" + questName + "</highlight> successfully created!");
    }

    public CommandMessage deleteQuest(final String questName) {
        if (!removeQuest(questName)) {
            return CommandMessage.error("<error>Quest <highlight>" + questName + "</highlight> doesn't exist!");
        }
        saveConfiguredData();
        return CommandMessage.success("<success>Quest <highlight>" + questName + "</highlight> successfully deleted!");
    }

    public boolean syncQuestRequirement(
            final String questName,
            final int id,
            final String typeId,
            final Conditions.Data data) {
        return syncQuestRequirement(questName, id, typeId, data, null);
    }

    public boolean syncQuestRequirement(
            final String questName,
            final int id,
            final String typeId,
            final Conditions.Data data,
            final Quest.ConditionSettings settings) {
        final Quest quest = quest(questName);
        if (quest == null || id < 1 || typeId == null || typeId.isBlank()) {
            return false;
        }
        Condition requirement = quest.getRequirementFromID(id);
        if (requirement == null) {
            requirement = quest.addRequirement(id, typeId, data);
        }
        requirement.apply(settings);
        return true;
    }

    public Condition questRequirement(final String questName, final int id) {
        final Quest quest = quest(questName);
        return quest == null ? null : quest.getRequirementFromID(id);
    }

    public boolean removeQuestRequirement(final String questName, final int id) {
        final Quest quest = quest(questName);
        return quest != null && quest.removeRequirement(id);
    }

    public boolean clearQuestRequirements(final String questName) {
        final Quest quest = quest(questName);
        if (quest == null) {
            return false;
        }
        quest.clearRequirements();
        return true;
    }

    public boolean syncQuestNpcAttachment(
            final String questName,
            final String npcType,
            final NQNPCID npcId,
            final String npcName,
            final boolean questShowing) {
        final Quest quest = quest(questName);
        if (quest == null) {
            return false;
        }
        quest.addNpcAttachment(npcType, npcId, npcName, questShowing);
        return true;
    }

    public String addArmorStandQuestAttachment(
            final String questName,
            final NQNPCID npcId,
            final String npcName,
            final boolean questShowing) {
        final Quest quest = quest(questName);
        if (quest == null) {
            return "<error>Error: Quest <highlight>" + (questName == null ? "" : questName) + "</highlight> does not exist.";
        }
        quest.addNpcAttachment("armorstand", npcId, npcName, questShowing);
        saveConfiguredData();
        return ArmorStandAttachments.questAttachedToNpc(
                quest.getIdentifier(),
                "armorstand:" + (npcId == null ? "" : npcId.getEitherAsString()));
    }

    public boolean removeQuestNpcAttachment(
            final String questName,
            final String npcType,
            final NQNPCID npcId) {
        final Quest quest = quest(questName);
        return quest != null && quest.removeNpcAttachment(npcType, npcId);
    }

    public String removeArmorStandQuestAttachment(
            final String questName,
            final NQNPCID npcId) {
        final Quest quest = quest(questName);
        if (quest == null) {
            return "<error>Error: Quest <highlight>" + (questName == null ? "" : questName) + "</highlight> does not exist.";
        }
        if (!quest.removeNpcAttachment("armorstand", npcId)) {
            return "<error>Quest <highlight>" + (questName == null ? "" : questName)
                    + "</highlight> is not attached to <highlight2>armorstand:"
                    + (npcId == null ? "" : npcId.getEitherAsString())
                    + "</highlight2>.";
        }
        saveConfiguredData();
        return ArmorStandAttachments.questRemovedFromNpc(
                quest.getIdentifier(),
                "armorstand:" + (npcId == null ? "" : npcId.getEitherAsString()));
    }

    public boolean clearQuestNpcAttachments(final String questName) {
        final Quest quest = quest(questName);
        if (quest == null) {
            return false;
        }
        quest.clearNpcAttachments();
        return true;
    }

    public int clearQuestNpcAttachments(final String questName, final String npcType) {
        final Quest quest = quest(questName);
        if (quest == null) {
            return 0;
        }
        if (npcType == null || npcType.isBlank()) {
            return quest.clearNpcAttachments();
        }
        int removed = 0;
        for (final NpcAttachment attachment : quest.getNpcAttachments()) {
            if (npcType.equalsIgnoreCase(attachment.npcType())
                    && quest.removeNpcAttachment(attachment.npcType(), attachment.npcId())) {
                removed++;
            }
        }
        return removed;
    }

    public boolean syncConversationNpcAttachment(
            final String conversationName,
            final String npcType,
            final NQNPCID npcId,
            final String npcName) {
        return conversations.addNpcAttachment(conversationName, npcType, npcId, npcName);
    }

    public ArmorStandAttachments.ConversationAddition addArmorStandConversationAttachment(
            final String conversationName,
            final String existingConversation,
            final NQNPCID npcId,
            final String npcName) {
        if (conversationName == null || conversationName.isBlank()) {
            return new ArmorStandAttachments.ConversationAddition(
                    ArmorStandAttachments.missingItemConversation(),
                    false,
                    "");
        }
        if (!conversations.exists(conversationName)) {
            return new ArmorStandAttachments.ConversationAddition(
                    ArmorStandAttachments.conversationDoesNotExist(conversationName),
                    false,
                    conversationName);
        }
        if (existingConversation != null && !existingConversation.isBlank()) {
            return new ArmorStandAttachments.ConversationAddition(
                    ArmorStandAttachments.alreadyAttached(existingConversation),
                    false,
                    existingConversation);
        }
        conversations.addNpcAttachment(conversationName, "armorstand", npcId, npcName);
        saveConfiguredData();
        return new ArmorStandAttachments.ConversationAddition(
                ArmorStandAttachments.added(conversationName),
                true,
                conversationName);
    }

    public boolean removeConversationNpcAttachment(
            final String conversationName,
            final String npcType,
            final NQNPCID npcId) {
        return conversations.removeNpcAttachment(conversationName, npcType, npcId);
    }

    public ArmorStandAttachments.ConversationRemoval removeArmorStandConversationAttachment(
            final String existingConversation,
            final NQNPCID npcId) {
        if (existingConversation == null || existingConversation.isBlank()) {
            return new ArmorStandAttachments.ConversationRemoval(
                    ArmorStandAttachments.noneAttached(),
                    false,
                    "");
        }
        conversations.removeNpcAttachment(existingConversation, "armorstand", npcId);
        saveConfiguredData();
        return new ArmorStandAttachments.ConversationRemoval(
                ArmorStandAttachments.removedAll(),
                true,
                existingConversation);
    }

    public List<String> removeConversationNpcAttachments(
            final String npcType,
            final NQNPCID npcId) {
        if (npcId == null) {
            return List.of();
        }
        final ArrayList<String> removed = new ArrayList<>();
        for (final String conversationName : conversations.names()) {
            if (conversations.removeNpcAttachment(conversationName, npcType, npcId)) {
                removed.add(conversationName);
            }
        }
        return List.copyOf(removed);
    }

    public boolean clearConversationNpcAttachments(final String conversationName) {
        return conversations.clearNpcAttachments(conversationName);
    }

    public List<NpcAttachment> conversationNpcAttachments(final String conversationName) {
        return conversations.npcAttachments(conversationName);
    }

    public String conversationAttachedToNpc(final String npcType, final NQNPCID npcId) {
        return conversations.conversationAttachedToNpc(npcType, npcId);
    }

    public NotQuestsAdapter.NpcSelection armorStandNpcSelection(final String selector) {
        final UUID armorStandId = armorStandId(selector);
        if (armorStandId == null) {
            return null;
        }
        final NQNPCID npcId = NQNPCID.fromUUID(armorStandId);
        return new NotQuestsAdapter.NpcSelection(
                armorStandSelector(npcId),
                armorStandNpcLabel(npcId),
                "armorstand",
                npcId,
                "");
    }

    public String armorStandNpcLabel(final String selector) {
        final UUID armorStandId = armorStandId(selector);
        return armorStandId == null ? "armor stand" : armorStandNpcLabel(NQNPCID.fromUUID(armorStandId));
    }

    public String armorStandAttachedQuestsMessage(final String selector) {
        final UUID armorStandId = armorStandId(selector);
        final List<String> questNames = armorStandId == null
                ? List.of()
                : questsAttachedToNpc("armorstand", NQNPCID.fromUUID(armorStandId)).stream()
                        .map(Quest::getIdentifier)
                        .toList();
        return ArmorStandAttachments.attachedQuestsMessage(
                armorStandNpcLabel(selector),
                questNames);
    }

    public List<String> armorStandQuestCheckMessages(final String selector) {
        final UUID armorStandId = armorStandId(selector);
        if (armorStandId == null) {
            return List.of(invalidArmorStandSelector(selector));
        }
        final NQNPCID npcId = NQNPCID.fromUUID(armorStandId);
        final List<String> showing = questsAttachedToNpc("armorstand", npcId, true).stream()
                .map(Quest::getIdentifier)
                .toList();
        final List<String> nonShowing = questsAttachedToNpc("armorstand", npcId, false).stream()
                .map(Quest::getIdentifier)
                .toList();
        return ArmorStandAttachments.checkMessages(
                armorStandId.toString(),
                !showing.isEmpty(),
                armorStandQuestStorageText(showing),
                !nonShowing.isEmpty(),
                armorStandQuestStorageText(nonShowing));
    }

    public String addArmorStandQuestAttachment(
            final String questName,
            final String selector,
            final boolean questShowing) {
        final UUID armorStandId = armorStandId(selector);
        if (armorStandId == null) {
            return invalidArmorStandSelector(selector);
        }
        return addArmorStandQuestAttachment(questName, NQNPCID.fromUUID(armorStandId), "", questShowing);
    }

    public String removeArmorStandQuestAttachment(
            final String questName,
            final String selector) {
        final UUID armorStandId = armorStandId(selector);
        if (armorStandId == null) {
            return invalidArmorStandSelector(selector);
        }
        return removeArmorStandQuestAttachment(questName, NQNPCID.fromUUID(armorStandId));
    }

    public String addArmorStandConversationAttachment(final String conversationName, final String selector) {
        final UUID armorStandId = armorStandId(selector);
        if (armorStandId == null) {
            return invalidArmorStandSelector(selector);
        }
        final NQNPCID npcId = NQNPCID.fromUUID(armorStandId);
        return addArmorStandConversationAttachment(
                        conversationName,
                        conversationAttachedToNpc("armorstand", npcId),
                        npcId,
                        "")
                .message();
    }

    public String removeArmorStandConversationAttachment(final String selector) {
        final UUID armorStandId = armorStandId(selector);
        if (armorStandId == null) {
            return invalidArmorStandSelector(selector);
        }
        final NQNPCID npcId = NQNPCID.fromUUID(armorStandId);
        return removeArmorStandConversationAttachment(
                        conversationAttachedToNpc("armorstand", npcId),
                        npcId)
                .message();
    }

    public ArmorStandAttachments.ToolUse useArmorStandTool(
            final int itemId,
            final String questName,
            final int objectiveId,
            final String conversationName,
            final String armorStandSelector,
            final String armorStandName) {
        final ArmorStandAttachments.Operation operation = ArmorStandAttachments.operation(itemId);
        if (operation == ArmorStandAttachments.Operation.UNKNOWN) {
            return ArmorStandAttachments.ToolUse.ignored();
        }
        if (ArmorStandAttachments.requiresQuestName(itemId)
                && (questName == null || questName.isBlank())) {
            return new ArmorStandAttachments.ToolUse(
                    true,
                    hasArmorStandAttachment(armorStandSelector),
                    List.of(ArmorStandAttachments.missingQuestName()));
        }
        final ArrayList<String> messages = new ArrayList<>();
        switch (operation) {
            case ADD_QUEST -> messages.add(addArmorStandQuestAttachment(
                    questName,
                    armorStandSelector,
                    ArmorStandAttachments.isShowingQuestTool(itemId)));
            case REMOVE_QUEST -> messages.add(removeArmorStandQuestAttachment(questName, armorStandSelector));
            case CHECK_QUESTS -> messages.addAll(armorStandQuestCheckMessages(armorStandSelector));
            case SET_OBJECTIVE_COMPLETION_NPC -> {
                final UUID armorStandId = armorStandId(armorStandSelector);
                final ArmorStandNpcUpdate update = setObjectiveCompletionNpcFromArmorStand(
                        questName,
                        objectiveId,
                        armorStandId == null ? "" : armorStandId.toString(),
                        armorStandName);
                messages.add(update.message());
            }
            case ADD_CONVERSATION -> messages.add(addArmorStandConversationAttachment(
                    conversationName,
                    armorStandSelector));
            case REMOVE_CONVERSATION -> messages.add(removeArmorStandConversationAttachment(armorStandSelector));
            case UNKNOWN -> {
                return ArmorStandAttachments.ToolUse.ignored();
            }
        }
        return new ArmorStandAttachments.ToolUse(
                true,
                hasArmorStandAttachment(armorStandSelector),
                messages);
    }

    public ArmorStandAttachments.Interaction playerInteractedWithArmorStand(
            final PlatformPlayer questPlayer,
            final int selectionId,
            final int itemId,
            final String questName,
            final int objectiveId,
            final String conversationName,
            final String selector,
            final String platformName) {
        final boolean mayUseEditingTool = questPlayer != null
                && runtimeAdapter().hasPermission(
                        questPlayer,
                        CommandManager.ARMOR_STAND_EDIT_PERMISSION);
        if (mayUseEditingTool && selectionId >= 0) {
            final NotQuestsAdapter.NpcSelection selection = armorStandNpcSelection(selector);
            final boolean selected = selection != null && completeNpcSelection(
                    selectionId,
                    selection.npcType(),
                    selection.npcId(),
                    platformName == null || platformName.isBlank() ? selection.label() : platformName);
            return new ArmorStandAttachments.Interaction(
                    selected,
                    selected && configuration.armorStandPreventEditing(),
                    hasArmorStandAttachment(selector),
                    List.of());
        }
        if (mayUseEditingTool && itemId >= 0) {
            final ArmorStandAttachments.ToolUse toolUse = useArmorStandTool(
                    itemId,
                    questName,
                    objectiveId,
                    conversationName,
                    selector,
                    platformName);
            if (toolUse.handled()) {
                return new ArmorStandAttachments.Interaction(
                        true,
                        configuration.armorStandPreventEditing(),
                        toolUse.trackIndicator(),
                        toolUse.messages());
            }
        }
        final boolean handled = playerInteractedWithArmorStandNpc(questPlayer, selector, platformName);
        return new ArmorStandAttachments.Interaction(
                handled,
                handled && configuration.armorStandPreventEditing(),
                hasArmorStandAttachment(selector),
                List.of());
    }

    public boolean hasArmorStandAttachment(final String selector) {
        final UUID armorStandId = armorStandId(selector);
        return armorStandId != null && hasNpcAttachment("armorstand", NQNPCID.fromUUID(armorStandId));
    }

    public NpcAttachments.Interaction playerInteractedWithAttachedNpc(
            final PlatformPlayer questPlayer,
            final String npcType,
            final NQNPCID npcId,
            final String npcName,
            final boolean objectiveAlreadyHandled) {
        return playerInteractedWithAttachedNpc(
                questPlayer,
                npcType,
                npcId,
                npcName,
                objectiveAlreadyHandled,
                null);
    }

    private NpcAttachments.Interaction playerInteractedWithAttachedNpc(
            final PlatformPlayer questPlayer,
            final String npcType,
            final NQNPCID npcId,
            final String npcName,
            final boolean objectiveAlreadyHandled,
            final Runnable npcConversationEnded) {
        if (questPlayer == null || npcType == null || npcType.isBlank() || npcId == null) {
            return NpcAttachments.Interaction.none();
        }
        if (objectiveAlreadyHandled || playerInteractedWithNpc(
                questPlayer,
                new AttachedNpcInteractionEvent(
                        questPlayer,
                        NpcAttachments.selector(npcType, npcId),
                        npcName == null ? "" : npcName))) {
            return NpcAttachments.Interaction.objective();
        }
        final boolean previewShown = showAttachedNpcQuestPreview(questPlayer, npcType, npcId, false);
        final String conversationName = conversationAttachedToNpc(npcType, npcId);
        boolean conversationStarted = false;
        if (conversationName != null && !conversationName.isBlank()) {
            final String endedMessage = playConversation(
                    questPlayer,
                    conversationName,
                    "citizens".equalsIgnoreCase(npcType) ? npcId.getIntegerID() : -1,
                    this::warn,
                    npcConversationEnded);
            if (endedMessage != null && !endedMessage.isBlank()) {
                questPlayer.sendMessage(endedMessage);
            }
            conversationStarted = conversationName.equalsIgnoreCase(activeConversationName(questPlayer));
        }
        return new NpcAttachments.Interaction(
                previewShown || conversationStarted,
                false,
                previewShown,
                conversationStarted,
                conversationStarted
                        && "citizens".equalsIgnoreCase(npcType)
                        && configuration.citizensFocusingEnabled(),
                conversationName);
    }

    public NpcAttachments.ClickEffects nativeNpcClicked(
            final String playerIdentifier,
            final String npcType,
            final NQNPCID npcId,
            final String npcName,
            final int selectionId,
            final IntFunction<NpcAttachments.EscortNpc> escortNpc,
            final Runnable npcConversationEnded,
            final ConversationManager.Focus.Native nativeFocus) {
        if (npcType == null || npcType.isBlank() || npcId == null) {
            return NpcAttachments.ClickEffects.none();
        }
        final PlatformPlayer questPlayer = activePlatformPlayer(playerIdentifier);
        final boolean mayCompleteSelection = questPlayer != null
                && runtimeAdapter().hasPermission(
                        questPlayer,
                        CommandManager.ARMOR_STAND_EDIT_PERMISSION);
        if (mayCompleteSelection
                && selectionId >= 0
                && completeNpcSelection(selectionId, npcType, npcId, npcName)) {
            return NpcAttachments.ClickEffects.selection();
        }
        if (questPlayer == null) {
            return NpcAttachments.ClickEffects.none();
        }

        final boolean escortHandled = escortNpc != null
                && npcId.getIntegerID() >= 0
                && escortDestinationReached(playerIdentifier, npcId.getIntegerID(), escortNpc);
        final NpcAttachments.Interaction interaction = playerInteractedWithAttachedNpc(
                questPlayer,
                npcType,
                npcId,
                npcName,
                escortHandled,
                npcConversationEnded);
        boolean focusStarted = false;
        if (interaction.focusConversation() && nativeFocus != null) {
            focusStarted = conversations.startFocus(
                    questPlayer,
                    interaction.conversationName(),
                    configuration.citizensFocusingRotateTime() / 2f,
                    configuration.citizensFocusingCancelConversationWhenTooFar(),
                    nativeFocus,
                    () -> conversationFocusEnded(questPlayer));
        }
        return new NpcAttachments.ClickEffects(
                interaction.handled(),
                false,
                interaction.objectiveHandled(),
                interaction.previewShown(),
                interaction.conversationStarted(),
                interaction.conversationStarted(),
                interaction.conversationStarted(),
                focusStarted);
    }

    public boolean playerInteractedWithArmorStandNpc(
            final PlatformPlayer questPlayer,
            final String selector,
            final String platformName) {
        final NotQuestsAdapter.NpcSelection selection = armorStandNpcSelection(selector);
        if (selection == null) {
            return false;
        }
        final boolean handledObjective = playerInteractedWithNpc(
                questPlayer,
                new ArmorStandNpcInteractionEvent(
                        questPlayer,
                        selection.selector(),
                        platformName == null || platformName.isBlank() ? selection.label() : platformName));
        if (handledObjective) {
            return true;
        }
        boolean handledInteraction = showAttachedNpcQuestPreview(questPlayer, "armorstand", selection.npcId(), true);
        final String conversationName = conversationAttachedToNpc("armorstand", selection.npcId());
        if (conversationName != null) {
            startConversation(questPlayer, conversationName, true, ignored -> {});
            handledInteraction = true;
        }
        return handledInteraction;
    }

    private static String armorStandSelector(final NQNPCID npcId) {
        return "armorstand:" + (npcId == null ? "" : npcId.getEitherAsString());
    }

    private static String armorStandNpcLabel(final NQNPCID npcId) {
        return NpcAttachments.formatAttachedNPC("armorstand", npcId, null);
    }

    private static UUID armorStandId(final String selector) {
        if (selector == null || !selector.toLowerCase(Locale.ROOT).startsWith("armorstand:")) {
            return null;
        }
        try {
            return UUID.fromString(selector.substring("armorstand:".length()));
        } catch (final IllegalArgumentException exception) {
            return null;
        }
    }

    private static String invalidArmorStandSelector(final String selector) {
        return "<error>Invalid armor stand selector: <highlight>" + (selector == null ? "" : selector) + "</highlight>.";
    }

    private static String armorStandQuestStorageText(final List<String> questNames) {
        if (questNames == null || questNames.isEmpty()) {
            return "";
        }
        return "°" + String.join("°", questNames) + "°";
    }

    private record ArmorStandNpcInteractionEvent(
            PlatformPlayer questPlayer,
            String npcSelector,
            String npcName)
            implements Objectives.NpcInteractionEvent {}

    private record AttachedNpcInteractionEvent(
            PlatformPlayer questPlayer,
            String npcSelector,
            String npcName)
            implements Objectives.NpcInteractionEvent {}

    public boolean syncQuestReward(
            final String questName,
            final int id,
            final String typeId,
            final Actions.Data data,
            final String displayName) {
        final Quest quest = quest(questName);
        if (quest == null || id < 1 || typeId == null || typeId.isBlank()) {
            return false;
        }
        Action reward = quest.getRewardFromID(id);
        if (reward == null) {
            reward = quest.addReward(id, typeId, data);
        }
        reward.setDisplayName(displayName);
        return true;
    }

    public ActionSettings questRewardSettings(
            final String questName,
            final int id,
            final String fallbackName) {
        final Quest quest = quest(questName);
        return ActionSettings.from(quest == null ? null : quest.getRewardFromID(id), fallbackName);
    }

    public boolean setQuestRewardDisplayName(
            final String questName,
            final int id,
            final String displayName) {
        final Quest quest = quest(questName);
        final Action reward = quest == null ? null : quest.getRewardFromID(id);
        if (reward == null) {
            return false;
        }
        reward.setDisplayName(displayName);
        return true;
    }

    public boolean removeQuestReward(final String questName, final int id) {
        final Quest quest = quest(questName);
        return quest != null && quest.removeReward(id);
    }

    public boolean clearQuestRewards(final String questName) {
        final Quest quest = quest(questName);
        if (quest == null) {
            return false;
        }
        quest.clearRewards();
        return true;
    }

    public boolean clearQuestTriggers(final String questName) {
        final Quest quest = quest(questName);
        if (quest == null) {
            return false;
        }
        quest.clearTriggers();
        return true;
    }

    public boolean removeQuestTrigger(final String questName, final int id) {
        final Quest quest = quest(questName);
        return quest != null && quest.removeTrigger(id);
    }

    public boolean syncQuestTrigger(
            final String questName,
            final int id,
            final String typeId,
            final Triggers.Data data) {
        return syncQuestTrigger(questName, id, typeId, data, null);
    }

    public boolean syncQuestTrigger(
            final String questName,
            final int id,
            final String typeId,
            final Triggers.Data data,
            final Quest.TriggerSettings settings) {
        final Quest quest = quest(questName);
        if (quest == null || id < 1 || typeId == null || typeId.isBlank()) {
            return false;
        }
        final Trigger trigger = quest.setTrigger(id, typeId, data);
        if (settings != null) {
            settings.applyTo(trigger);
        }
        return true;
    }

    public boolean setQuestGuiItem(final String questName, final String materialName) {
        return setQuestGuiItem(
                questName,
                materialName == null || materialName.isBlank() ? null : ItemStackSelection.parse(materialName));
    }

    public boolean setQuestGuiItem(final String questName, final ItemSelection itemSelection) {
        final Quest quest = quest(questName);
        if (quest == null) {
            return false;
        }
        quest.setGuiItem(itemSelection);
        return true;
    }

    public boolean setQuestGuiItem(
            final String questName,
            final ItemSelection itemSelection,
            final boolean glow) {
        final Quest quest = quest(questName);
        if (quest == null) {
            return false;
        }
        quest.setGuiItem(itemSelection);
        quest.setGuiItemGlow(glow);
        return true;
    }

    public boolean setQuestCategory(final String questName, final String categoryName) {
        final Quest quest = quest(questName);
        if (quest == null) {
            return false;
        }
        quest.setCategory(categoryName);
        getOrCreateCategory(quest.getCategory());
        return true;
    }

    public boolean setQuestDisplayName(final String questName, final String displayName) {
        final Quest quest = quest(questName);
        if (quest == null) {
            return false;
        }
        quest.setDisplayName(displayName);
        return true;
    }

    public boolean clearQuestDisplayName(final String questName) {
        final Quest quest = quest(questName);
        if (quest == null) {
            return false;
        }
        quest.clearDisplayName();
        return true;
    }

    public boolean setQuestDescription(final String questName, final String description) {
        final Quest quest = quest(questName);
        if (quest == null) {
            return false;
        }
        quest.setDescription(description);
        return true;
    }

    public boolean clearQuestDescription(final String questName) {
        final Quest quest = quest(questName);
        if (quest == null) {
            return false;
        }
        quest.clearDescription();
        return true;
    }

    public boolean setQuestMaxCompletions(final String questName, final int maxCompletions) {
        final Quest quest = quest(questName);
        if (quest == null) {
            return false;
        }
        quest.setMaxCompletions(maxCompletions);
        return true;
    }

    public boolean setQuestMaxAccepts(final String questName, final int maxAccepts) {
        final Quest quest = quest(questName);
        if (quest == null) {
            return false;
        }
        quest.setMaxAccepts(maxAccepts);
        return true;
    }

    public boolean setQuestMaxFails(final String questName, final int maxFails) {
        final Quest quest = quest(questName);
        if (quest == null) {
            return false;
        }
        quest.setMaxFails(maxFails);
        return true;
    }

    public boolean setQuestTakeEnabled(final String questName, final boolean takeEnabled) {
        final Quest quest = quest(questName);
        if (quest == null) {
            return false;
        }
        quest.setTakeEnabled(takeEnabled);
        return true;
    }

    public boolean setQuestAbortEnabled(final String questName, final boolean abortEnabled) {
        final Quest quest = quest(questName);
        if (quest == null) {
            return false;
        }
        quest.setAbortEnabled(abortEnabled);
        return true;
    }

    public boolean setQuestAcceptCooldownComplete(final String questName, final long cooldownMillis) {
        final Quest quest = quest(questName);
        if (quest == null) {
            return false;
        }
        quest.setAcceptCooldownComplete(cooldownMillis);
        return true;
    }

    public boolean setQuestObjectiveProgressOrder(final String questName, final String progressOrder) {
        final Quest quest = quest(questName);
        if (quest == null) {
            return false;
        }
        quest.setObjectiveProgressOrder(progressOrder);
        return true;
    }

    public boolean syncConfiguredObjective(
            final String questName,
            final int[] objectivePath,
            final String typeId,
            final Objectives.Data data,
            final Quest.ObjectiveSettings settings) {
        final Quest quest = quest(questName);
        if (quest == null || objectivePath == null || objectivePath.length == 0 || typeId == null || typeId.isBlank()) {
            return false;
        }
        final Objective entry = upsertObjective(quest, objectivePath, typeId, data);
        if (entry == null) {
            return false;
        }
        applyObjectiveSettings(entry, settings);
        return true;
    }

    public boolean removeConfiguredObjective(final String questName, final int[] objectivePath) {
        final Quest quest = quest(questName);
        if (quest == null || objectivePath == null || objectivePath.length == 0) {
            return false;
        }
        if (objectivePath.length == 1) {
            return quest.removeObjective(objectivePath[0]);
        }
        final Objective parent = objectiveAt(quest, Arrays.copyOf(objectivePath, objectivePath.length - 1));
        return parent != null && parent.removeChildObjective(objectivePath[objectivePath.length - 1]);
    }

    public boolean clearConfiguredObjectives(final String questName) {
        final Quest quest = quest(questName);
        if (quest == null) {
            return false;
        }
        quest.clearObjectives();
        return true;
    }

    public boolean clearConfiguredChildObjectives(final String questName, final int[] objectivePath) {
        final Objective objective = objectiveAt(quest(questName), objectivePath);
        if (objective == null) {
            return false;
        }
        objective.clearChildObjectives();
        return true;
    }

    public Objective configuredObjective(final String questName, final int[] objectivePath) {
        return objectiveAt(quest(questName), objectivePath);
    }

    public String configuredObjectiveTypeId(final String questName, final int[] objectivePath) {
        final Objective objective = configuredObjective(questName, objectivePath);
        return objective == null ? "" : objective.typeId();
    }

    public String configuredObjectiveDisplayName(final String questName, final int[] objectivePath) {
        final Objective objective = configuredObjective(questName, objectivePath);
        return objective == null ? "" : objective.getDisplayName();
    }

    public String configuredObjectiveTaskDescription(final String questName, final int[] objectivePath) {
        final Objective objective = configuredObjective(questName, objectivePath);
        return objective == null ? "" : objective.getTaskDescription();
    }

    public String configuredObjectiveProgressNeededExpression(final String questName, final int[] objectivePath) {
        final Objective objective = configuredObjective(questName, objectivePath);
        return objective == null ? "1" : objective.data().text("progressNeededExpression");
    }

    public double configuredObjectiveProgressNeededValue(
            final String questName,
            final int[] objectivePath,
            final PlatformPlayer questPlayer) {
        final Objective objective = configuredObjective(questName, objectivePath);
        if (objective == null) {
            return 1;
        }
        final String expression = objective.data().text("progressNeededExpression");
        if (expression == null || expression.isBlank()) {
            return progressNeeded(objective);
        }
        try {
            return Math.max(1, new NumberExpression(runtimeAdapter(), expression).calculateValue(questPlayer));
        } catch (final RuntimeException exception) {
            return progressNeeded(objective);
        }
    }

    public boolean setConfiguredObjectiveProgressNeededExpression(
            final String questName,
            final int[] objectivePath,
            final String progressNeededExpression) {
        final Objective objective = configuredObjective(questName, objectivePath);
        if (objective == null) {
            return false;
        }
        objective.data().setValue(
                "progressNeededExpression",
                progressNeededExpression == null || progressNeededExpression.isBlank() ? "1" : progressNeededExpression);
        return true;
    }

    public String configuredObjectiveCompletionNpc(final String questName, final int[] objectivePath) {
        final Objective objective = configuredObjective(questName, objectivePath);
        return objective == null ? "" : objective.getCompletionNPC();
    }

    public NQLocation configuredObjectiveLocation(final String questName, final int[] objectivePath) {
        final Objective objective = configuredObjective(questName, objectivePath);
        return objective == null ? null : objective.getLocation();
    }

    public boolean configuredObjectiveLocationEnabled(final String questName, final int[] objectivePath) {
        final Objective objective = configuredObjective(questName, objectivePath);
        return objective != null && objective.isLocationEnabled();
    }

    public int firstFreeConfiguredObjectiveRewardId(
            final String questName,
            final int[] objectivePath,
            final int fallback) {
        final Objective objective = objectiveAt(quest(questName), objectivePath);
        return objective == null ? fallback : objective.getFreeRewardID();
    }

    public int firstFreeConfiguredObjectiveConditionId(
            final String questName,
            final int[] objectivePath,
            final String group,
            final int fallback) {
        final Objective objective = objectiveAt(quest(questName), objectivePath);
        return objective == null ? fallback : objective.getFreeConditionID(group);
    }

    public boolean syncConfiguredObjectiveReward(
            final String questName,
            final int[] objectivePath,
            final int id,
            final String typeId,
            final Actions.Data data,
            final String displayName) {
        final Objective objective = objectiveAt(quest(questName), objectivePath);
        if (objective == null || id < 1 || typeId == null || typeId.isBlank()) {
            return false;
        }
        Action reward = objective.getRewardFromID(id);
        if (reward == null) {
            reward = objective.addReward(id, typeId, data);
        }
        reward.setDisplayName(displayName);
        return true;
    }

    public ActionSettings configuredObjectiveRewardSettings(
            final String questName,
            final int[] objectivePath,
            final int id,
            final String fallbackName) {
        final Objective objective = objectiveAt(quest(questName), objectivePath);
        return ActionSettings.from(objective == null ? null : objective.getRewardFromID(id), fallbackName);
    }

    public boolean setConfiguredObjectiveRewardDisplayName(
            final String questName,
            final int[] objectivePath,
            final int id,
            final String displayName) {
        final Objective objective = objectiveAt(quest(questName), objectivePath);
        final Action reward = objective == null ? null : objective.getRewardFromID(id);
        if (reward == null) {
            return false;
        }
        reward.setDisplayName(displayName);
        return true;
    }

    public boolean removeConfiguredObjectiveReward(final String questName, final int[] objectivePath, final int id) {
        final Objective objective = objectiveAt(quest(questName), objectivePath);
        return objective != null && objective.removeReward(id);
    }

    public boolean clearConfiguredObjectiveRewards(final String questName, final int[] objectivePath) {
        final Objective objective = objectiveAt(quest(questName), objectivePath);
        if (objective == null) {
            return false;
        }
        objective.clearRewards();
        return true;
    }

    public boolean syncConfiguredObjectiveCondition(
            final String questName,
            final int[] objectivePath,
            final String group,
            final int id,
            final String typeId,
            final Conditions.Data data) {
        return syncConfiguredObjectiveCondition(questName, objectivePath, group, id, typeId, data, null);
    }

    public boolean syncConfiguredObjectiveCondition(
            final String questName,
            final int[] objectivePath,
            final String group,
            final int id,
            final String typeId,
            final Conditions.Data data,
            final Quest.ConditionSettings settings) {
        final Objective objective = objectiveAt(quest(questName), objectivePath);
        if (objective == null || id < 1 || typeId == null || typeId.isBlank()) {
            return false;
        }
        Condition condition = objective.getConditionFromID(group, id);
        if (condition == null) {
            condition = objective.addCondition(group, id, typeId, data);
        }
        condition.apply(settings);
        return true;
    }

    public boolean removeConfiguredObjectiveCondition(
            final String questName,
            final int[] objectivePath,
            final String group,
            final int id) {
        final Objective objective = objectiveAt(quest(questName), objectivePath);
        return objective != null && objective.removeCondition(group, id);
    }

    public boolean clearConfiguredObjectiveConditions(
            final String questName,
            final int[] objectivePath,
            final String group) {
        final Objective objective = objectiveAt(quest(questName), objectivePath);
        if (objective == null) {
            return false;
        }
        objective.clearConditions(group);
        return true;
    }

    public boolean setConfiguredObjectiveCompletionNpc(
            final String questName,
            final int objectiveId,
            final String completionNpc) {
        return setConfiguredObjectiveCompletionNpc(questName, new int[] {objectiveId}, completionNpc);
    }

    public boolean setConfiguredObjectiveCompletionNpc(
            final String questName,
            final int[] objectivePath,
            final String completionNpc) {
        final Objective objective = objectiveAt(quest(questName), objectivePath);
        if (objective == null) {
            return false;
        }
        objective.setCompletionNpc(completionNpc);
        return true;
    }

    public ArmorStandNpcUpdate setObjectiveCompletionNpcFromArmorStand(
            final String questName,
            final int objectiveId,
            final String armorStandId,
            final String armorStandName) {
        final Quest quest = quest(questName);
        if (quest == null) {
            return new ArmorStandNpcUpdate(
                    "<error>Error: Quest <highlight>" + questName + "</highlight> does not exist.",
                    false, "");
        }
        final Objective objective = quest.getObjectiveFromID(objectiveId);
        if (objective == null) {
            return new ArmorStandNpcUpdate(
                    "<error>Error: Objective with the ID <highlight>" + objectiveId
                            + "</highlight> was not found for quest <highlight2>"
                            + quest.getIdentifier() + "</highlight2>!",
                    false, "");
        }
        final String selector = "armorstand:" + (armorStandId == null ? "" : armorStandId);
        objective.setCompletionNpc(selector);
        saveConfiguredData();
        return new ArmorStandNpcUpdate(
                "<success>The completionArmorStandUUID of the objective with the ID <highlight>"
                        + objectiveId + "</highlight> has been set to the Armor Stand with the UUID <highlight2>"
                        + armorStandId + "</highlight2> and name <highlight2>" + armorStandName + "</highlight2>!",
                true, selector);
    }

    private static Objective upsertObjective(
            final Quest quest,
            final int[] objectivePath,
            final String typeId,
            final Objectives.Data data) {
        if (objectivePath.length == 1) {
            final Objective existing = quest.getObjectiveFromID(objectivePath[0]);
            return existing == null ? quest.addObjective(objectivePath[0], typeId, data, "") : existing;
        }
        final Objective parent = objectiveAt(quest, Arrays.copyOf(objectivePath, objectivePath.length - 1));
        if (parent == null) {
            return null;
        }
        final int id = objectivePath[objectivePath.length - 1];
        final Objective existing = parent.getObjectiveFromID(id);
        return existing == null ? parent.addChildObjective(id, typeId, data, "") : existing;
    }

    private static Objective objectiveAt(final Quest quest, final int[] objectivePath) {
        if (quest == null || objectivePath == null || objectivePath.length == 0) {
            return null;
        }
        Objective current = quest.getObjectiveFromID(objectivePath[0]);
        for (int i = 1; i < objectivePath.length && current != null; i++) {
            current = current.getObjectiveFromID(objectivePath[i]);
        }
        return current;
    }

    private static void applyObjectiveSettings(
            final Objective entry,
            final Quest.ObjectiveSettings settings) {
        if (settings == null) {
            return;
        }
        entry.setDisplayName(settings.displayName());
        entry.setDescription(settings.description());
        entry.setTaskDescription(settings.taskDescription());
        entry.setChildObjectiveProgressOrder(settings.childObjectiveProgressOrder());
        entry.setLocationEnabled(settings.locationEnabled());
        entry.setCompletionNpc(settings.completionNpc());
        entry.setLocation(settings.location());
    }

    public Category getOrCreateCategory(final String categoryName) {
        return questManager.getOrCreateCategory(categoryName);
    }

    public Category category(final String categoryName) {
        return questManager.getCategory(categoryName);
    }

    public String categoryDisplayNameOrIdentifier(final String categoryName) {
        return questManager.getCategoryDisplayNameOrIdentifier(categoryName);
    }

    public boolean createCategory(final String categoryName) {
        if (!questManager.createCategory(categoryName)) {
            return false;
        }
        final Category category = questManager.getCategory(categoryName);
        dataManager.saveCategory(category);
        return true;
    }

    public boolean createCategory(final String categoryName, final String parentCategoryName) {
        return createCategory(questManager.getCategoryIdentifier(categoryName, parentCategoryName));
    }

    public String categoryIdentifier(final String categoryName, final String parentCategoryName) {
        return questManager.getCategoryIdentifier(categoryName, parentCategoryName);
    }

    public Category loadCategory(
            final String categoryName,
            final String displayName,
            final String progressOrder,
            final String guiItem,
            final boolean guiItemGlow) {
        return loadCategory(
                categoryName,
                displayName,
                progressOrder,
                guiItem == null || guiItem.isBlank() ? null : ItemStackSelection.parse(guiItem),
                guiItemGlow,
                0);
    }

    public Category loadCategory(
            final String categoryName,
            final String displayName,
            final String progressOrder,
            final ItemSelection guiItem,
            final boolean guiItemGlow) {
        return loadCategory(categoryName, displayName, progressOrder, guiItem, guiItemGlow, 0);
    }

    public Category loadCategory(
            final String categoryName,
            final String displayName,
            final String progressOrder,
            final ItemSelection guiItem,
            final boolean guiItemGlow,
            final int conversationDelayMillis) {
        final Category category = getOrCreateCategory(categoryName);
        category.setDisplayName(displayName);
        category.setProgressOrder(progressOrder);
        category.setGuiItem(guiItem);
        category.setGuiItemGlow(guiItemGlow);
        category.setConversationDelayInMS(conversationDelayMillis);
        return category;
    }

    public boolean setCategoryConversationDelayMillis(final String categoryName, final int conversationDelayMillis) {
        final Category category = category(categoryName);
        if (category == null) {
            return false;
        }
        category.setConversationDelayInMS(conversationDelayMillis);
        dataManager.saveCategory(category);
        return true;
    }

    public boolean setCategoryDisplayName(final String categoryName, final String displayName) {
        final Category category = category(categoryName);
        if (category == null) {
            return false;
        }
        category.setDisplayName(displayName);
        dataManager.saveCategory(category);
        return true;
    }

    public boolean clearCategoryDisplayName(final String categoryName) {
        return setCategoryDisplayName(categoryName, "");
    }

    public boolean setCategoryProgressOrder(final String categoryName, final String progressOrder) {
        final Category category = category(categoryName);
        if (category == null) {
            return false;
        }
        category.setProgressOrder(progressOrder);
        dataManager.saveCategory(category);
        return true;
    }

    public boolean clearCategoryProgressOrder(final String categoryName) {
        return setCategoryProgressOrder(categoryName, "");
    }

    public boolean setCategoryGuiItem(
            final String categoryName,
            final String guiItem,
            final boolean guiItemGlow) {
        return setCategoryGuiItem(
                categoryName,
                guiItem == null || guiItem.isBlank() ? null : ItemStackSelection.parse(guiItem),
                guiItemGlow);
    }

    public boolean setCategoryGuiItem(
            final String categoryName,
            final ItemSelection guiItem,
            final boolean guiItemGlow) {
        final Category category = category(categoryName);
        if (category == null) {
            return false;
        }
        category.setGuiItem(guiItem);
        category.setGuiItemGlow(guiItemGlow);
        dataManager.saveCategory(category);
        return true;
    }

    public void ensureDefaultCategory() {
        questManager.ensureDefaultCategory();
    }

    public static String canonicalCategoryName(final String categoryName) {
        return Category.canonical(categoryName);
    }

    public boolean createTag(final TagType type, final String tagName) {
        return createTag(type, tagName, "");
    }

    public boolean createTag(final TagType type, final String tagName, final String categoryName) {
        if (type == null || tagName == null || tagName.isBlank()) {
            return false;
        }
        if (categoryName != null && !categoryName.isBlank()) {
            getOrCreateCategory(categoryName);
        }
        return tags.putIfAbsent(
                tagName.toLowerCase(Locale.ROOT),
                new Tag(tagName, type, categoryName)) == null;
    }

    public boolean deleteTag(final String tagName) {
        if (tagName == null || tagName.isBlank()) {
            return false;
        }
        return tags.remove(tagName.toLowerCase(Locale.ROOT)) != null;
    }

    public Tag tag(final String tagName) {
        if (tagName == null || tagName.isBlank()) {
            return null;
        }
        return tags.get(tagName.toLowerCase(Locale.ROOT));
    }

    public TagManager.LoadSummary loadPlayerTags(
            final Connection connection,
            final String playerIdentifier,
            final String profile,
            final String playerName,
            final TagManager.Logger logger,
            final boolean verbose) throws SQLException {
        return playerTagPersistence.loadOnJoin(
                connection,
                questPlayer(playerIdentifier, profile),
                playerName,
                logger,
                verbose);
    }

    public TagManager.SaveSummary savePlayerTags(
            final Connection connection,
            final String playerIdentifier,
            final String profile,
            final String playerName,
            final TagManager.Logger logger,
            final boolean verbose) throws SQLException {
        return playerTagPersistence.saveOnQuit(
                connection,
                questPlayer(playerIdentifier, profile),
                playerName,
                logger,
                verbose);
    }

    public void loadOnlinePlayerTags(
            final TagManager.ConnectionSource connections,
            final Iterable<TagManager.OnlinePlayerTags> players,
            final TagManager.BatchLogger logger,
            final boolean verbose) {
        playerTagPersistence.loadAllOnlinePlayers(connections, players, logger, verbose);
    }

    public void loadOnlinePlayerTags(
            final Iterable<TagManager.OnlinePlayerTags> players,
            final TagManager.BatchLogger logger,
            final boolean verbose) {
        loadOnlinePlayerTags(this::playerDatabaseConnection, players, logger, verbose);
    }

    public void saveOnlinePlayerTags(
            final TagManager.ConnectionSource connections,
            final Iterable<TagManager.OnlinePlayerTags> players,
            final TagManager.BatchLogger logger,
            final boolean verbose) {
        playerTagPersistence.saveAllOnlinePlayers(connections, players, logger, verbose);
    }

    public void saveOnlinePlayerTags(
            final Iterable<TagManager.OnlinePlayerTags> players,
            final TagManager.BatchLogger logger,
            final boolean verbose) {
        saveOnlinePlayerTags(this::playerDatabaseConnection, players, logger, verbose);
    }

    public TagManager.LoadSummary loadOnlinePlayerTags(
            final TagManager.ConnectionSource connections,
            final TagManager.OnlinePlayerTags player,
            final TagManager.BatchLogger logger,
            final boolean verbose) {
        return playerTagPersistence.loadOnlinePlayer(connections, player, logger, verbose);
    }

    public TagManager.LoadSummary loadOnlinePlayerTags(
            final TagManager.OnlinePlayerTags player,
            final TagManager.BatchLogger logger,
            final boolean verbose) {
        return loadOnlinePlayerTags(this::playerDatabaseConnection, player, logger, verbose);
    }

    public TagManager.SaveSummary saveOnlinePlayerTags(
            final TagManager.ConnectionSource connections,
            final TagManager.OnlinePlayerTags player,
            final TagManager.BatchLogger logger,
            final boolean verbose) {
        return playerTagPersistence.saveOnlinePlayer(connections, player, logger, verbose);
    }

    public TagManager.SaveSummary saveOnlinePlayerTags(
            final TagManager.OnlinePlayerTags player,
            final TagManager.BatchLogger logger,
            final boolean verbose) {
        return saveOnlinePlayerTags(this::playerDatabaseConnection, player, logger, verbose);
    }

    public boolean removeQuest(final String questName) {
        if (questName == null || questName.isBlank()) {
            return false;
        }
        final Quest removed = questManager.removeQuest(questName);
        if (removed == null) {
            return false;
        }
        for (final QuestPlayer playerData : questPlayerManager.getAllQuestPlayers()) {
            playerData.removeActiveQuest(questName);
            for (final CompletedQuest completedQuest : playerData.getCompletedQuests()) {
                if (completedQuest.questIdentifier().equalsIgnoreCase(questName)) {
                    playerData.removeCompletedQuest(completedQuest);
                }
            }
            for (final FailedQuest failedQuest : playerData.getFailedQuests()) {
                if (failedQuest.questIdentifier().equalsIgnoreCase(questName)) {
                    playerData.removeFailedQuest(failedQuest);
                }
            }
        }
        return true;
    }

    public QuestPlayer questPlayer(final String playerIdentifier, final String profile) {
        return questPlayerManager.getQuestPlayer(playerIdentifier, profile);
    }

    public PlatformPlayer platformPlayer(final String playerIdentifier) {
        return questPlayerManager.getPlatformPlayer(playerIdentifier);
    }

    public PlatformPlayer activePlatformPlayer(final String playerIdentifier) {
        return questPlayerManager.getPlatformPlayer(playerIdentifier);
    }

    /** Returns the connected platform player, creating and registering its handle when needed. */
    public PlatformPlayer getOrCreatePlatformPlayer(final String playerIdentifier) {
        if (playerIdentifier == null || playerIdentifier.isBlank()) {
            return null;
        }
        final PlatformPlayer existing = activePlatformPlayer(playerIdentifier);
        if (existing != null) {
            return existing;
        }
        final String profile = activeProfile(playerIdentifier);
        final PlatformPlayer created = platformQuestPlayer(playerIdentifier);
        return registerQuestPlayer(created, profile, true);
    }

    /** Loads the player's persisted state when necessary, then returns its connected handle. */
    public PlatformPlayer getOrLoadPlatformPlayer(final String playerIdentifier) {
        PlatformPlayer player = activePlatformPlayer(playerIdentifier);
        if (player != null) {
            return player;
        }
        loadPlayerData(playerIdentifier, this::platformQuestPlayer);
        player = activePlatformPlayer(playerIdentifier);
        return player == null ? getOrCreatePlatformPlayer(playerIdentifier) : player;
    }

    public PlatformPlayer registerQuestPlayer(
            final PlatformPlayer questPlayer,
            final String profile,
            final boolean active) {
        return questPlayerManager.register(questPlayer, profile, active);
    }

    public List<PlatformPlayer> platformPlayers() {
        return questPlayerManager.getConnectedPlayers();
    }

    public List<PlatformPlayer> activePlatformPlayers() {
        return questPlayerManager.getActivePlatformPlayers();
    }

    public void removeQuestPlayers(final String playerIdentifier) {
        questPlayerManager.disconnect(playerIdentifier);
        progressBossBarAges.remove(playerIdentifier);
    }

    public QuestPlayer questPlayerIfLoaded(final String playerIdentifier) {
        return questPlayerManager.getQuestPlayerIfLoaded(playerIdentifier);
    }

    public TagManager.OnlinePlayerTags onlinePlayerTags(
            final String playerIdentifier,
            final String profile,
            final String playerName) {
        final QuestPlayer playerData =
                playerIdentifier == null || playerIdentifier.isBlank()
                        ? null
                        : questPlayer(playerIdentifier, profile);
        return new TagManager.OnlinePlayerTags(playerData, playerIdentifier, playerName);
    }

    public QuestPlayer activeQuestPlayer(final String playerIdentifier) {
        return questPlayerManager.getActiveQuestPlayer(playerIdentifier);
    }

    public boolean removeActiveQuest(
            final String playerIdentifier,
            final String profile,
            final String questName) {
        if (questName == null || questName.isBlank()) {
            return false;
        }
        final QuestPlayer questPlayer = questPlayer(playerIdentifier, profile);
        questPlayerManager.getActiveObjectives().removeActiveObjectives(playerIdentifier, profile, questName);
        final boolean removed = questPlayer.removeActiveQuest(questName);
        removeActiveTriggers(playerIdentifier, profile, questName);
        return removed;
    }

    public void addCompletedQuest(
            final String playerIdentifier,
            final String profile,
            final QuestPlayer.CompletedQuest completedQuest) {
        if (completedQuest != null) {
            questPlayer(playerIdentifier, profile).addCompletedQuest(completedQuest);
        }
    }

    public void recordCompletedQuest(
            final String playerIdentifier,
            final String profile,
            final String questName,
            final long completionTimeMillis) {
        if (questName != null && !questName.isBlank()) {
            questPlayer(playerIdentifier, profile).recordCompletedQuest(questName, completionTimeMillis);
        }
    }

    public void removeCompletedQuest(
            final String playerIdentifier,
            final String profile,
            final QuestPlayer.CompletedQuest completedQuest) {
        if (completedQuest != null) {
            questPlayer(playerIdentifier, profile).removeCompletedQuest(completedQuest);
        }
    }

    public void addFailedQuest(
            final String playerIdentifier,
            final String profile,
            final QuestPlayer.FailedQuest failedQuest) {
        if (failedQuest != null) {
            questPlayer(playerIdentifier, profile).addFailedQuest(failedQuest);
        }
    }

    public void recordFailedQuest(
            final String playerIdentifier,
            final String profile,
            final String questName,
            final long failureTimeMillis) {
        if (questName != null && !questName.isBlank()) {
            questPlayer(playerIdentifier, profile).recordFailedQuest(questName, failureTimeMillis);
        }
    }

    public void removeFailedQuest(
            final String playerIdentifier,
            final String profile,
            final QuestPlayer.FailedQuest failedQuest) {
        if (failedQuest != null) {
            questPlayer(playerIdentifier, profile).removeFailedQuest(failedQuest);
        }
    }

    public QuestPlayer restorePlayerRuntime(
            final PlayerDatabase.LoadedPlayer loadedPlayer,
            final PlatformPlayer questPlayer,
            final Consumer<String> warningSink) {
        if (loadedPlayer == null) {
            return null;
        }
        final String playerIdentifier = loadedPlayer.playerIdentifier();
        final String profile = QuestPlayerManager.cleanProfile(loadedPlayer.profile());
        final QuestPlayer playerData = questPlayer(playerIdentifier, profile);
        playerData.setQuestPoints(loadedPlayer.questPoints());
        playerData.replaceTags(loadedPlayer.tags());
        playerData.setFinishedLoadingTags(true);
        questPlayerManager.setActiveProfile(playerIdentifier, loadedPlayer.activeProfile());

        for (final PlayerDatabase.QuestHistoryReadRow completedQuest : loadedPlayer.completedQuests()) {
            final String questName = completedQuest.questName();
            if (questName == null || questName.isBlank()) {
                warn(warningSink, "ERROR: A completed quest with a blank name could not be loaded from database");
                continue;
            }
            if (completedQuest.timestamp() <= 0) {
                warn(warningSink, "ERROR: TimeCompleted from Quest with name <highlight>" + questName
                        + "</highlight> could not be loaded from database (requested for loading completed Quests)");
                continue;
            }
            playerData.addCompletedQuest(
                    new QuestPlayer.CompletedQuest(questName, playerIdentifier, completedQuest.timestamp()));
        }

        for (final PlayerDatabase.QuestHistoryReadRow failedQuest : loadedPlayer.failedQuests()) {
            final String questName = failedQuest.questName();
            if (questName == null || questName.isBlank()) {
                warn(warningSink, "ERROR: A failed quest with a blank name could not be loaded from database");
                continue;
            }
            if (failedQuest.timestamp() <= 0) {
                warn(warningSink, "ERROR: TimeFailed from Quest with name <highlight>" + questName
                        + "</highlight> could not be loaded from database (requested for loading failed Quests)");
                continue;
            }
            playerData.addFailedQuest(
                    new QuestPlayer.FailedQuest(questName, playerIdentifier, failedQuest.timestamp()));
        }

        if (questPlayer == null) {
            return playerData;
        }
        questPlayerManager.withProfile(playerIdentifier, profile, () -> {
            for (final String questName : loadedPlayer.activeQuestNames()) {
                final Quest quest = quest(questName);
                if (quest == null) {
                    warn(warningSink, "ERROR: Quest with name <highlight>" + questName
                            + "</highlight> could not be loaded from database");
                    continue;
                }
                playerData.addActiveQuest(quest);
                activateQuestProgress(questPlayer, profile, quest.getIdentifier(), warningSink, true);
                restoreActiveTriggers(playerIdentifier, profile, quest, loadedPlayer);
                restoreActiveObjectives(playerIdentifier, profile, playerData, quest, loadedPlayer, warningSink);
            }
        });
        return playerData;
    }

    private void restoreActiveTriggers(
            final String playerIdentifier,
            final String profile,
            final Quest quest,
            final PlayerDatabase.LoadedPlayer loadedPlayer) {
        for (final PlayerDatabase.ActiveTriggerReadRow row : loadedPlayer.activeTriggers(quest.getIdentifier())) {
            setActiveTriggerProgress(
                    playerIdentifier,
                    profile,
                    quest.getIdentifier(),
                    row.triggerId(),
                    row.currentProgress());
        }
    }

    private void restoreActiveObjectives(
            final String playerIdentifier,
            final String profile,
            final QuestPlayer playerData,
            final Quest quest,
            final PlayerDatabase.LoadedPlayer loadedPlayer,
            final Consumer<String> warningSink) {
        for (final PlayerDatabase.ActiveObjectiveReadRow row : loadedPlayer.activeObjectivesByHolderPath()
                .values()
                .stream()
                .flatMap(List::stream)
                .filter(row -> rowBelongsToQuest(row, quest.getIdentifier()))
                .toList()) {
            final int[] objectivePath = objectivePathFromStoredHolderPath(quest.getIdentifier(), row.holderPath(), row.objectiveId());
            if (objectivePath.length == 0 || configuredObjective(quest, objectivePath) == null) {
                warn(warningSink, "ERROR: ObjectiveType for the Quest <highlight>"
                        + quest.getIdentifier()
                        + "</highlight> could not be loaded from database");
                continue;
            }
            final boolean restored = questPlayerManager.getActiveObjectives().restoreObjectiveProgress(
                    playerIdentifier,
                    profile,
                    quest.getIdentifier(),
                    objectivePath,
                    row.currentProgress(),
                    row.completed(),
                    true);
            if (row.completed()) {
                playerData.addCompletedObjective(new QuestPlayer.CompletedObjective(
                        quest.getIdentifier(),
                        objectivePath(objectivePath),
                        row.holderPath(),
                        row.objectiveType(),
                        row.currentProgress(),
                        row.progressNeededNull() ? progressNeeded(configuredObjective(quest, objectivePath)) : row.progressNeeded()));
            }
            if (!restored) {
                warn(warningSink, "ERROR: ObjectiveType for the Quest <highlight>"
                        + quest.getIdentifier()
                        + "</highlight> could not be loaded from database");
            }
        }
    }

    private static boolean rowBelongsToQuest(
            final PlayerDatabase.ActiveObjectiveReadRow row,
            final String questIdentifier) {
        if (row == null || questIdentifier == null || questIdentifier.isBlank()) {
            return false;
        }
        final String holderPath = row.holderPath();
        return holderPath != null
                && (holderPath.equalsIgnoreCase(questIdentifier)
                        || holderPath.regionMatches(true, 0, questIdentifier + ".", 0, questIdentifier.length() + 1));
    }

    private static int[] objectivePathFromStoredHolderPath(
            final String questIdentifier,
            final String holderPath,
            final int objectiveId) {
        if (objectiveId <= 0 || questIdentifier == null || questIdentifier.isBlank()) {
            return new int[0];
        }
        final String prefix = questIdentifier + ".";
        if (holderPath == null || holderPath.isBlank() || holderPath.equalsIgnoreCase(questIdentifier)) {
            return new int[] {objectiveId};
        }
        if (!holderPath.regionMatches(true, 0, prefix, 0, prefix.length())) {
            return new int[0];
        }
        final String[] parentIds = holderPath.substring(prefix.length()).split("\\.");
        final int[] path = new int[parentIds.length + 1];
        for (int i = 0; i < parentIds.length; i++) {
            try {
                path[i] = Integer.parseInt(parentIds[i]);
            } catch (final NumberFormatException exception) {
                return new int[0];
            }
        }
        path[path.length - 1] = objectiveId;
        return path;
    }

    private static Objective configuredObjective(
            final Quest quest,
            final int[] objectivePath) {
        if (quest == null || objectivePath == null || objectivePath.length == 0) {
            return null;
        }
        Objective current = quest.getObjectiveFromID(objectivePath[0]);
        for (int i = 1; current != null && i < objectivePath.length; i++) {
            current = current.getObjectiveFromID(objectivePath[i]);
        }
        return current;
    }

    public boolean activateQuest(
            final PlatformPlayer questPlayer,
            final String questName,
            final Consumer<String> warningSink) {
        final Quest quest = quest(questName);
        final QuestPlayer playerData = questPlayer(questPlayer);
        if (quest == null || playerData == null) {
            warn(warningSink, "Cannot activate quest: missing player or quest " + questName + ".");
            return false;
        }
        playerData.addActiveQuest(quest);
        activateQuestProgress(questPlayer, quest.getIdentifier(), warningSink);
        return true;
    }

    public List<String> playerIdentifiers() {
        return questPlayerManager.getPlayerIdentifiers();
    }

    public int removeActiveQuestForAllPlayers(final String questName, final boolean markFailed) {
        int changed = 0;
        for (final QuestPlayer questPlayer : questPlayerManager.getAllQuestPlayers()) {
            if (markFailed) {
                if (questPlayer.failQuest(questName, System.currentTimeMillis())) {
                    changed++;
                }
            } else if (questPlayer.removeActiveQuest(questName)) {
                changed++;
            }
        }
        return changed;
    }

    public QuestReset resetAndRemoveQuest(final String playerIdentifier, final String questName) {
        final QuestPlayer questPlayer = questPlayerIfLoaded(playerIdentifier);
        if (questPlayer == null) {
            return QuestReset.playerNotFound(playerIdentifier);
        }
        return resetAndRemoveQuest(questPlayer, questName);
    }

    public List<QuestReset> resetAndRemoveQuestForAllPlayers(final String questName) {
        return questPlayerManager.getAllQuestPlayers().stream()
                .map(questPlayer -> resetAndRemoveQuest(questPlayer, questName))
                .toList();
    }

    public List<QuestReset> resetAndFailQuestForAllPlayers(final String questName) {
        return questPlayerManager.getAllQuestPlayers().stream()
                .map(questPlayer -> resetAndFailQuest(questPlayer, questName))
                .toList();
    }

    private QuestReset resetAndRemoveQuest(final QuestPlayer questPlayer, final String questName) {
        final boolean activeRemoved = questPlayer.removeActiveQuest(questName);
        int completedRemoved = 0;
        for (final QuestPlayer.CompletedQuest completedQuest : questPlayer.getCompletedQuests()) {
            if (completedQuest.questIdentifier().equalsIgnoreCase(questName)) {
                questPlayer.removeCompletedQuest(completedQuest);
                completedRemoved++;
            }
        }
        return QuestReset.changed(
                questPlayer.getPlayerIdentifier(),
                questPlayer.getPlayerIdentifier(),
                activeRemoved,
                false,
                completedRemoved);
    }

    private QuestReset resetAndFailQuest(final QuestPlayer questPlayer, final String questName) {
        final boolean activeFailed = questPlayer.failQuest(questName, System.currentTimeMillis());
        int completedRemoved = 0;
        for (final QuestPlayer.CompletedQuest completedQuest : questPlayer.getCompletedQuests()) {
            if (completedQuest.questIdentifier().equalsIgnoreCase(questName)) {
                questPlayer.removeCompletedQuest(completedQuest);
                completedRemoved++;
            }
        }
        return QuestReset.changed(
                questPlayer.getPlayerIdentifier(),
                questPlayer.getPlayerIdentifier(),
                false,
                activeFailed,
                completedRemoved);
    }

    public List<String> playerProfileNames(final String playerIdentifier) {
        return questPlayerManager.getProfileNames(playerIdentifier);
    }

    public String activeProfile(final String playerIdentifier) {
        return questPlayerManager.getActiveProfile(playerIdentifier);
    }

    public boolean createPlayerProfile(final String playerIdentifier, final String profile) {
        return questPlayerManager.createProfile(playerIdentifier, profile);
    }

    public boolean changePlayerProfile(final String playerIdentifier, final String profile) {
        return questPlayerManager.changeProfile(playerIdentifier, profile);
    }

    public QuestPlayer activatePlayerProfile(final String playerIdentifier, final String profile) {
        return questPlayerManager.activateProfile(playerIdentifier, profile);
    }

    public boolean hasAnyActiveQuests(final PlatformPlayer questPlayer) {
        final QuestPlayer playerData = questPlayer(questPlayer);
        return playerData != null && !playerData.getActiveQuestIdentifiers().isEmpty();
    }

    public List<String> activeQuestNames(final PlatformPlayer questPlayer) {
        final QuestPlayer playerData = questPlayer(questPlayer);
        return playerData == null
                ? List.of()
                : playerData.getActiveQuestIdentifiers().stream()
                        .sorted(String.CASE_INSENSITIVE_ORDER)
                        .toList();
    }

    public List<String> activeQuestIdentifiers(final String playerIdentifier, final String profile) {
        return List.copyOf(questPlayer(playerIdentifier, profile).getActiveQuestIdentifiers());
    }

    public List<String> activeQuestIdentifiersIfLoaded(final String playerIdentifier, final String profile) {
        if (playerIdentifier == null || playerIdentifier.isBlank()) {
            return List.of();
        }
        final QuestPlayer playerData = questPlayerManager.getQuestPlayerIfLoaded(playerIdentifier, profile);
        return playerData == null ? List.of() : List.copyOf(playerData.getActiveQuestIdentifiers());
    }

    public boolean setActiveQuestNames(final PlatformPlayer questPlayer, final List<String> questNames) {
        final QuestPlayer playerData = questPlayer(questPlayer);
        if (playerData == null) {
            return false;
        }
        final ArrayList<Quest> requestedQuests = new ArrayList<>();
        for (final String questName : questNames == null ? List.<String>of() : questNames) {
            if (questName == null || questName.isBlank()) {
                return false;
            }
            final Quest quest = quest(questName);
            if (quest == null) {
                return false;
            }
            if (requestedQuests.stream()
                    .noneMatch(requested -> requested.getIdentifier().equalsIgnoreCase(quest.getIdentifier()))) {
                requestedQuests.add(quest);
            }
        }
        for (final String activeQuestName : List.copyOf(playerData.getActiveQuestIdentifiers())) {
            final boolean keep = requestedQuests.stream()
                    .anyMatch(quest -> quest.getIdentifier().equalsIgnoreCase(activeQuestName));
            if (!keep && !failQuest(questPlayer, activeQuestName, warning -> {})) {
                return false;
            }
        }
        for (final Quest quest : requestedQuests) {
            if (!playerData.hasActiveQuest(quest.getIdentifier())
                    && !giveQuest(
                            questPlayer,
                            quest.getIdentifier(),
                            Quest.GiveOptions.forcedSilent(),
                            warning -> {})) {
                return false;
            }
        }
        return true;
    }

    public List<String> completedQuestNames(final PlatformPlayer questPlayer) {
        final QuestPlayer playerData = questPlayer(questPlayer);
        return playerData == null
                ? List.of()
                : playerData.getCompletedQuests().stream()
                        .map(QuestPlayer.CompletedQuest::questIdentifier)
                        .toList();
    }

    public List<QuestPlayer.CompletedQuest> completedQuests(final String playerIdentifier, final String profile) {
        return questPlayer(playerIdentifier, profile).getCompletedQuests();
    }

    public List<QuestPlayer.FailedQuest> failedQuests(final String playerIdentifier, final String profile) {
        return questPlayer(playerIdentifier, profile).getFailedQuests();
    }

    public long completedQuestCount(final PlatformPlayer questPlayer, final String questName) {
        if (questName == null || questName.isBlank()) {
            return 0;
        }
        return completedQuestNames(questPlayer).stream()
                .filter(questName::equalsIgnoreCase)
                .count();
    }

    public boolean setCompletedQuestNames(final PlatformPlayer questPlayer, final List<String> questNames) {
        final QuestPlayer playerData = questPlayer(questPlayer);
        if (playerData == null) {
            return false;
        }
        final List<String> requestedNames = questNames == null ? List.of() : questNames;
        for (final QuestPlayer.CompletedQuest completedQuest : playerData.getCompletedQuests()) {
            final boolean keep = requestedNames.stream()
                    .anyMatch(name -> name != null && name.equalsIgnoreCase(completedQuest.questIdentifier()));
            if (!keep) {
                playerData.removeCompletedQuest(completedQuest);
            }
        }
        for (final String questName : requestedNames) {
            if (questName != null
                    && !questName.isBlank()
                    && quest(questName) != null
                    && !playerData.hasCompletedQuest(questName)) {
                playerData.addCompletedQuest(
                        new QuestPlayer.CompletedQuest(questName, playerData.getPlayerIdentifier(), System.currentTimeMillis()));
            }
        }
        return true;
    }

    public long questPoints(final PlatformPlayer questPlayer) {
        final QuestPlayer playerData = questPlayer(questPlayer);
        return playerData == null ? 0 : playerData.getQuestPoints();
    }

    public boolean hasActiveQuest(final PlatformPlayer questPlayer, final String questName) {
        final QuestPlayer playerData = questPlayer(questPlayer);
        return playerData != null && playerData.hasActiveQuest(questName);
    }

    public boolean hasCompletedQuest(final PlatformPlayer questPlayer, final String questName) {
        final QuestPlayer playerData = questPlayer(questPlayer);
        return playerData != null && playerData.hasCompletedQuest(questName);
    }

    public boolean hasFailedQuest(final PlatformPlayer questPlayer, final String questName) {
        final QuestPlayer playerData = questPlayer(questPlayer);
        return playerData != null && playerData.hasFailedQuest(questName);
    }

    public boolean setQuestPoints(final PlatformPlayer questPlayer, final long questPoints) {
        final QuestPlayer playerData = questPlayer(questPlayer);
        return setQuestPoints(playerData, questPlayer, questPoints);
    }

    public boolean setQuestPoints(
            final QuestPlayer playerData,
            final PlatformPlayer platformPlayer,
            final long questPoints) {
        if (playerData == null) {
            return false;
        }
        final long storedQuestPoints = Math.max(0, questPoints);
        if (platformPlayer != null && !platformPlayer.beforeQuestPointsChanged(storedQuestPoints)) {
            return false;
        }
        playerData.setQuestPoints(storedQuestPoints);
        return true;
    }

    public boolean addQuestPoints(final PlatformPlayer questPlayer, final long questPoints) {
        return setQuestPoints(questPlayer, questPoints(questPlayer) + questPoints);
    }

    public boolean removeQuestPoints(final PlatformPlayer questPlayer, final long questPoints) {
        return setQuestPoints(questPlayer, questPoints(questPlayer) - questPoints);
    }

    public boolean setQuestPoints(
            final PlatformPlayer questPlayer,
            final long questPoints,
            final boolean notifyPlayer) {
        if (!setQuestPoints(questPlayer, questPoints)) {
            return false;
        }
        if (notifyPlayer) {
            questPlayer.sendMessage(translate(questPlayer,
                    "chat.questpoints.notify-when-changed.set",
                    Map.of("%NEWQUESTPOINTSAMOUNT%", String.valueOf(questPoints(questPlayer))),
                    "<success>Your quest points have been set to <highlight>%NEWQUESTPOINTSAMOUNT%</highlight>."));
        }
        return true;
    }

    public boolean addQuestPoints(
            final PlatformPlayer questPlayer,
            final long questPoints,
            final boolean notifyPlayer) {
        if (!addQuestPoints(questPlayer, questPoints)) {
            return false;
        }
        if (notifyPlayer) {
            questPlayer.sendMessage(translate(questPlayer,
                    "chat.questpoints.notify-when-changed.add",
                    Map.of("%QUESTPOINTSTOADD%", String.valueOf(questPoints)),
                    "<success>You received <highlight>%QUESTPOINTSTOADD%</highlight> quest points."));
        }
        return true;
    }

    public boolean removeQuestPoints(
            final PlatformPlayer questPlayer,
            final long questPoints,
            final boolean notifyPlayer) {
        if (!removeQuestPoints(questPlayer, questPoints)) {
            return false;
        }
        if (notifyPlayer) {
            questPlayer.sendMessage(translate(questPlayer,
                    "chat.questpoints.notify-when-changed.remove",
                    Map.of("%QUESTPOINTSTOREMOVE%", String.valueOf(questPoints)),
                    "<warn>You lost <highlight>%QUESTPOINTSTOREMOVE%</highlight> quest points."));
        }
        return true;
    }

    public boolean setPlayerTagValue(
            final String playerIdentifier,
            final String profile,
            final String tagIdentifier,
            final Object value) {
        if (tagIdentifier == null || tagIdentifier.isBlank()) {
            return false;
        }
        questPlayer(playerIdentifier, profile).setTagValue(tagIdentifier, value);
        return true;
    }

    public Object playerTagValue(
            final String playerIdentifier,
            final String profile,
            final String tagIdentifier) {
        if (tagIdentifier == null || tagIdentifier.isBlank()) {
            return null;
        }
        return questPlayer(playerIdentifier, profile).getTagValue(tagIdentifier);
    }

    public Map<String, Object> playerTags(final String playerIdentifier, final String profile) {
        return questPlayer(playerIdentifier, profile).getTags();
    }

    public Object playerTagValue(
            final PlatformPlayer questPlayer,
            final String tagIdentifier,
            final TagType type) {
        if (questPlayer == null || !questPlayer.hasPlayer() || tagIdentifier == null || tagIdentifier.isBlank()) {
            return null;
        }
        final Tag tag = tag(tagIdentifier);
        if (tag == null || (type != null && tag.tagType() != type)) {
            return null;
        }
        final Object value = playerTagValue(
                questPlayer.playerIdentifier(),
                activeProfile(questPlayer.playerIdentifier()),
                tagIdentifier);
        return type == null || TagManager.matchesType(value, type) ? value : null;
    }

    public boolean setPlayerTagValue(
            final PlatformPlayer questPlayer,
            final String tagIdentifier,
            final TagType type,
            final Object value) {
        if (questPlayer == null || !questPlayer.hasPlayer() || tagIdentifier == null || tagIdentifier.isBlank()) {
            return false;
        }
        final Tag tag = tag(tagIdentifier);
        if (tag == null || (type != null && tag.tagType() != type)) {
            return false;
        }
        if (type != null && !TagManager.matchesType(value, type)) {
            return false;
        }
        return setPlayerTagValue(
                questPlayer.playerIdentifier(),
                activeProfile(questPlayer.playerIdentifier()),
                tagIdentifier,
                value);
    }

    public boolean setPlayerFinishedLoadingTags(
            final String playerIdentifier,
            final String profile,
            final boolean finishedLoadingTags) {
        questPlayer(playerIdentifier, profile).setFinishedLoadingTags(finishedLoadingTags);
        return true;
    }

    public boolean isPlayerCurrentlyLoading(final String playerIdentifier, final String profile) {
        return questPlayer(playerIdentifier, profile).isCurrentlyLoading();
    }

    public boolean setPlayerCurrentlyLoading(
            final String playerIdentifier,
            final String profile,
            final boolean currentlyLoading) {
        questPlayer(playerIdentifier, profile).setCurrentlyLoading(currentlyLoading);
        return true;
    }

    public boolean isPlayerFinishedLoadingGeneralData(final String playerIdentifier, final String profile) {
        return questPlayer(playerIdentifier, profile).isFinishedLoadingGeneralData();
    }

    public boolean setPlayerFinishedLoadingGeneralData(
            final String playerIdentifier,
            final String profile,
            final boolean finishedLoadingGeneralData) {
        questPlayer(playerIdentifier, profile).setFinishedLoadingGeneralData(finishedLoadingGeneralData);
        return true;
    }

    public boolean isPlayerFinishedLoadingTags(final String playerIdentifier, final String profile) {
        return questPlayer(playerIdentifier, profile).isFinishedLoadingTags();
    }

    public List<String> completedObjectiveIds(
            final PlatformPlayer questPlayer,
            final String questName) {
        final QuestPlayer playerData = questPlayer(questPlayer);
        return playerData == null ? List.of() : playerData.getCompletedObjectiveIDs(questName);
    }

    public boolean setCompletedObjectiveIds(
            final PlatformPlayer questPlayer,
            final String questName,
            final List<String> objectiveIds) {
        final QuestPlayer playerData = questPlayer(questPlayer);
        final Quest quest = quest(questName);
        if (playerData == null
                || quest == null
                || !playerData.hasActiveQuest(quest.getIdentifier())) {
            return false;
        }
        final ArrayList<String> requestedPaths = new ArrayList<>();
        final ArrayList<ActiveObjective> requestedObjectives = new ArrayList<>();
        for (final String objectiveId : objectiveIds == null ? List.<String>of() : objectiveIds) {
            final int[] objectivePath = parseObjectivePath(objectiveId);
            if (objectivePath.length == 0 || configuredObjective(quest, objectivePath) == null) {
                return false;
            }
            final String path = objectivePath(objectivePath);
            if (requestedPaths.stream().anyMatch(path::equals)) {
                continue;
            }
            requestedPaths.add(path);
            if (playerData.getCompletedObjectiveIDs(quest.getIdentifier()).stream().anyMatch(path::equalsIgnoreCase)) {
                continue;
            }
            final ActiveObjective objective = activeObjectiveProgress(questPlayer, quest.getIdentifier(), objectivePath);
            if (objective == null) {
                return false;
            }
            requestedObjectives.add(objective);
        }
        for (final ActiveObjective objective : requestedObjectives) {
            final double progressToAdd = Math.max(0, objective.getProgressNeeded() - objective.currentProgress());
            if (progressToAdd > 0) {
                addActiveObjectiveProgress(
                        questPlayer,
                        quest.getIdentifier(),
                        objective.getObjectivePath(),
                        progressToAdd,
                        activeObjectiveName(objective),
                        activeObjectiveHolderName(objective));
            } else {
                objective.setProgress(objective.getProgressNeeded(), false);
            }
        }
        return requestedObjectives.stream().allMatch(ActiveObjective::hasBeenCompleted);
    }

    public boolean completedObjective(
            final PlatformPlayer questPlayer,
            final String questName,
            final int objectiveId) {
        return completedObjectiveIds(questPlayer, questName).stream()
                .anyMatch(String.valueOf(objectiveId)::equalsIgnoreCase);
    }

    public CompletedObjectiveCheck completedObjectiveRequirement(
            final Conditions.Data condition,
            final PlatformPlayer questPlayer,
            final int objectiveId) {
        final String questName = condition == null ? "" : condition.text("__questName");
        if (questName == null || questName.isBlank()) {
            return new CompletedObjectiveCheck(
                    false,
                    false,
                    String.valueOf(objectiveId),
                    "Cannot find current quest objective holder.");
        }
        final Quest quest = quest(questName);
        final Objective objective = quest == null ? null : quest.getObjectiveFromID(objectiveId);
        if (objective == null) {
            return new CompletedObjectiveCheck(
                    false,
                    false,
                    String.valueOf(objectiveId),
                    "Cannot find objective you have to complete first.");
        }
        return new CompletedObjectiveCheck(
                true,
                completedObjective(questPlayer, questName, objectiveId),
                objectiveDisplayNameOrType(objective),
                "");
    }

    private static String objectiveDisplayNameOrType(final Objective objective) {
        return objective.getDisplayName() == null || objective.getDisplayName().isBlank()
                ? objective.typeId()
                : objective.getDisplayName();
    }

    public boolean addCompletedObjectiveId(
            final PlatformPlayer questPlayer,
            final String questName,
            final int objectiveId) {
        return addCompletedObjectiveId(questPlayer, questName, String.valueOf(objectiveId));
    }

    public boolean addCompletedObjectiveId(
            final PlatformPlayer questPlayer,
            final String questName,
            final int[] objectivePath) {
        return addCompletedObjectiveId(questPlayer, questName, objectivePath(objectivePath));
    }

    public boolean addCompletedObjectiveId(
            final PlatformPlayer questPlayer,
            final String questName,
            final String objectiveId) {
        final QuestPlayer playerData = questPlayer(questPlayer);
        if (playerData == null || questName == null || questName.isBlank() || objectiveId == null || objectiveId.isBlank()) {
            return false;
        }
        playerData.addCompletedObjectiveId(questName, objectiveId);
        return true;
    }

    public Quest.AcceptCheck questAcceptCheck(final PlatformPlayer questPlayer, final String questName) {
        if (questName == null || questName.isBlank()) {
            return new Quest.AcceptCheck(Quest.AcceptCheck.Status.ACCEPTABLE, 0, 0, 0, 0);
        }
        final Quest quest = quest(questName);
        if (quest == null) {
            return new Quest.AcceptCheck(Quest.AcceptCheck.Status.ACCEPTABLE, 0, 0, 0, 0);
        }
        final QuestPlayer playerData = questPlayer(questPlayer);
        if (playerData == null) {
            return new Quest.AcceptCheck(Quest.AcceptCheck.Status.ACCEPTABLE, 0, 0, 0, 0);
        }
        return Quest.acceptCheck(
                quest,
                configuration.maxActiveQuestsPerPlayer(),
                playerData.getActiveQuestIdentifiers(),
                playerData.getCompletedQuests(),
                playerData.getFailedQuests(),
                System.currentTimeMillis());
    }

    public boolean questOnCooldown(final PlatformPlayer questPlayer, final String questName) {
        return questAcceptCheck(questPlayer, questName).timeToWaitInMinutes() > 0;
    }

    public boolean questReachedMaxAccepts(final PlatformPlayer questPlayer, final String questName) {
        final Quest quest = quest(questName);
        if (quest == null || quest.getMaxAccepts() < 0) {
            return false;
        }
        return questAcceptCheck(questPlayer, questName).acceptedAmount() >= quest.getMaxAccepts();
    }

    public boolean questReachedMaxCompletions(final PlatformPlayer questPlayer, final String questName) {
        final Quest quest = quest(questName);
        if (quest == null || quest.getMaxCompletions() < 0) {
            return false;
        }
        return questAcceptCheck(questPlayer, questName).completedAmount() >= quest.getMaxCompletions();
    }

    public boolean questReachedMaxFails(final PlatformPlayer questPlayer, final String questName) {
        final Quest quest = quest(questName);
        if (quest == null || quest.getMaxFails() < 0) {
            return false;
        }
        return questAcceptCheck(questPlayer, questName).failedAmount() >= quest.getMaxFails();
    }

    public Quest.AcceptCheck questCooldownCheckForDisplay(
            final PlatformPlayer questPlayer,
            final String questName,
            final long nowMillis) {
        if (questName == null || questName.isBlank()) {
            return new Quest.AcceptCheck(Quest.AcceptCheck.Status.ACCEPTABLE, 0, 0, 0, 0);
        }
        final Quest quest = quest(questName);
        if (quest == null) {
            return new Quest.AcceptCheck(Quest.AcceptCheck.Status.ACCEPTABLE, 0, 0, 0, 0);
        }
        final QuestPlayer playerData = questPlayer(questPlayer);
        if (playerData == null) {
            return new Quest.AcceptCheck(Quest.AcceptCheck.Status.ACCEPTABLE, 0, 0, 0, 0);
        }
        final Quest.AcceptCheck check = Quest.acceptCheck(
                quest,
                -1,
                playerData.getActiveQuestIdentifiers(),
                playerData.getCompletedQuests(),
                playerData.getFailedQuests(),
                nowMillis);
        return check.timeToWaitInMinutes() > 0
                ? new Quest.AcceptCheck(
                        Quest.AcceptCheck.Status.COOLDOWN,
                        check.completedAmount(),
                        check.failedAmount(),
                        check.acceptedAmount(),
                        check.timeToWaitInMinutes())
                : check;
    }

    public String questCooldownLeftFormatted(
            final PlatformPlayer questPlayer,
            final String questName,
            final long nowMillis) {
        return Quest.CooldownDisplay
                .from(questCooldownCheckForDisplay(questPlayer, questName, nowMillis))
                .format(questCooldownText(questPlayer));
    }

    public String noQuestCooldownLeftFormatted(final PlatformPlayer questPlayer) {
        return Quest.CooldownDisplay
                .from(new Quest.AcceptCheck(Quest.AcceptCheck.Status.ACCEPTABLE, 0, 0, 0, 0))
                .format(questCooldownText(questPlayer));
    }

    public String questCooldownLeftFormatted(
            final PlatformPlayer questPlayer,
            final String questName,
            final long nowMillis,
            final Quest.CooldownDisplay.Text text) {
        return Quest.CooldownDisplay
                .from(questCooldownCheckForDisplay(questPlayer, questName, nowMillis))
                .format(text);
    }

    public String resolvePlaceholderApiValue(
            final PlatformPlayer questPlayer,
            final String identifier,
            final long nowMillis) {
        if (questPlayer == null || !questPlayer.hasPlayer()) {
            return "";
        }
        final String rawIdentifier = identifier == null ? "" : identifier;
        final QuestPlayer playerData = questPlayer(questPlayer);

        if (rawIdentifier.startsWith("player_questpoints")) {
            return String.valueOf(questPoints(questPlayer));
        }
        if (rawIdentifier.startsWith("player_completed_quests_amount")) {
            return String.valueOf(playerData == null ? 0 : playerData.getCompletedQuests().size());
        }
        if (rawIdentifier.startsWith("player_active_quests_amount")) {
            return String.valueOf(playerData == null ? 0 : playerData.getActiveQuestIdentifiers().size());
        }
        if (rawIdentifier.startsWith("player_active_quests_list_horizontal")) {
            return formatActiveQuestPlaceholder(
                    activeQuestPlaceholderNames(playerData),
                    configuration.activeQuestListHorizontalLimit(),
                    configuration.activeQuestListHorizontalUseDisplayName(),
                    configuration.activeQuestListHorizontalSeparator());
        }
        if (rawIdentifier.startsWith("player_active_quests_list_vertical")) {
            return formatActiveQuestPlaceholder(
                    activeQuestPlaceholderNames(playerData),
                    configuration.activeQuestListVerticalLimit(),
                    configuration.activeQuestListVerticalUseDisplayName(),
                    "\n");
        }
        if (rawIdentifier.startsWith("player_has_completed_quest_")) {
            final String questName = rawIdentifier.replace("player_has_completed_quest_", "");
            return quest(questName) != null && hasCompletedQuest(questPlayer, questName) ? "Yes" : "No";
        }
        if (rawIdentifier.startsWith("player_has_current_active_quest_")) {
            final String questName = rawIdentifier.replace("player_has_current_active_quest_", "");
            return quest(questName) != null && hasActiveQuest(questPlayer, questName) ? "Yes" : "No";
        }
        if (rawIdentifier.startsWith("player_is_objective_unlocked_and_active")
                && rawIdentifier.contains("_from_active_quest_")) {
            final ObjectivePlaceholderRequest request = objectivePlaceholderRequest(
                    rawIdentifier,
                    "player_is_objective_unlocked_and_active_");
            if (request == null) {
                return "No";
            }
            final ActiveObjective objective = activeObjectiveForPlaceholder(questPlayer, request);
            return objective != null && objective.isUnlocked() ? "Yes" : "No";
        }
        if (rawIdentifier.startsWith("player_is_objective_unlocked_")
                && rawIdentifier.contains("_from_active_quest_")) {
            final ObjectivePlaceholderRequest request =
                    objectivePlaceholderRequest(rawIdentifier, "player_is_objective_unlocked_");
            if (request == null) {
                return "No";
            }
            final ActiveObjective objective = activeObjectiveForPlaceholder(questPlayer, request);
            return (objective != null && objective.isUnlocked()) || completedObjective(questPlayer, request.questName(), request.objectiveId())
                    ? "Yes"
                    : "No";
        }
        if (rawIdentifier.startsWith("player_is_objective_completed_")
                && rawIdentifier.contains("_from_active_quest_")) {
            final ObjectivePlaceholderRequest request =
                    objectivePlaceholderRequest(rawIdentifier, "player_is_objective_completed_");
            return request != null && completedObjective(questPlayer, request.questName(), request.objectiveId())
                    ? "Yes"
                    : "No";
        }
        if (rawIdentifier.startsWith("player_expression_")) {
            final String expression = rawIdentifier.replace("player_expression_", "");
            return String.valueOf(new NumberExpression(runtimeAdapter(), expression).calculateValue(questPlayer));
        }
        if (rawIdentifier.startsWith("player_rounded_expression_")) {
            final String expression = rawIdentifier.replace("player_rounded_expression_", "");
            return String.valueOf((int) Math.round(new NumberExpression(runtimeAdapter(), expression).calculateValue(questPlayer)));
        }
        if (rawIdentifier.startsWith("player_variable_")) {
            final String variableName = rawIdentifier.replace("player_variable_", "");
            final Object value = runtimeAdapter().variableValue(variableName, questPlayer);
            return value == null ? "" : String.valueOf(value);
        }
        if (rawIdentifier.startsWith("player_tag_")) {
            final String tagName = rawIdentifier.replace("player_tag_", "");
            if (tag(tagName) == null || playerData == null) {
                return "";
            }
            final Object tagValue = playerTagValue(
                    playerData.getPlayerIdentifier(),
                    playerData.getProfile(),
                    tagName);
            return tagValue == null ? "" : String.valueOf(tagValue);
        }
        if (rawIdentifier.startsWith("player_quest_cooldown_left_formatted_")) {
            final String questName = rawIdentifier.replace("player_quest_cooldown_left_formatted_", "");
            return quest(questName) == null
                    ? noQuestCooldownLeftFormatted(questPlayer)
                    : questCooldownLeftFormatted(questPlayer, questName, nowMillis);
        }
        if (rawIdentifier.startsWith("player_objective_progress_percentage_")
                && rawIdentifier.contains("_from_active_quest_")) {
            final ObjectivePlaceholderRequest request =
                    objectivePlaceholderRequest(rawIdentifier, "player_objective_progress_percentage_");
            if (request == null) {
                return "0";
            }
            final ActiveObjective objective = activeObjectiveForPlaceholder(questPlayer, request);
            if (objective != null && objective.isUnlocked()) {
                final double needed = objective.getProgressNeeded();
                return String.valueOf(needed <= 0 ? 0 : (int) ((objective.currentProgress() / needed) * 100));
            }
            return completedObjective(questPlayer, request.questName(), request.objectiveId()) ? "100" : "0";
        }
        if (rawIdentifier.startsWith("player_objective_progress_")
                && rawIdentifier.contains("_from_active_quest_")) {
            final ObjectivePlaceholderRequest request =
                    objectivePlaceholderRequest(rawIdentifier, "player_objective_progress_");
            if (request == null) {
                return "0";
            }
            final ActiveObjective objective = activeObjectiveForPlaceholder(questPlayer, request);
            if (objective != null && objective.isUnlocked()) {
                return String.valueOf(objective.currentProgress());
            }
            final QuestPlayer.CompletedObjective completed = completedObjectiveForPlaceholder(questPlayer, request);
            return completed == null ? "0" : String.valueOf(completed.progressNeeded());
        }

        return null;
    }

    private List<String[]> activeQuestPlaceholderNames(final QuestPlayer playerData) {
        if (playerData == null) {
            return List.of();
        }
        final ArrayList<String[]> names = new ArrayList<>();
        for (final String questIdentifier : playerData.getActiveQuestIdentifiers().stream()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList()) {
            names.add(new String[] {questIdentifier, questDisplayName(questIdentifier)});
        }
        return List.copyOf(names);
    }

    private static String formatActiveQuestPlaceholder(
            final List<String[]> quests,
            final int limit,
            final boolean useDisplayName,
            final String separator) {
        if (quests == null || quests.isEmpty()) {
            return "";
        }
        final StringBuilder list = new StringBuilder();
        int amount = 0;
        for (final String[] quest : quests) {
            amount++;
            if (limit >= 0 && amount > limit) {
                return list.toString();
            }
            final String nameToAdd = useDisplayName && quest[1] != null && !quest[1].isBlank()
                    ? quest[1] : quest[0];
            if (list.isEmpty()) {
                list.append(nameToAdd);
            } else {
                list.append(separator == null ? "" : separator).append(nameToAdd);
            }
        }
        return list.toString();
    }

    private ObjectivePlaceholderRequest objectivePlaceholderRequest(
            final String identifier,
            final String prefix) {
        if (identifier == null || prefix == null || !identifier.startsWith(prefix)) {
            return null;
        }
        final String value = identifier.replace(prefix, "");
        final int separator = value.indexOf("_from_active_quest_");
        if (separator < 0) {
            return null;
        }
        final String objectiveIdText = value.substring(0, separator);
        final String questName = value.substring(separator + "_from_active_quest_".length());
        try {
            return new ObjectivePlaceholderRequest(Integer.parseInt(objectiveIdText), questName);
        } catch (final NumberFormatException ignored) {
            return null;
        }
    }

    private ActiveObjective activeObjectiveForPlaceholder(
            final PlatformPlayer questPlayer,
            final ObjectivePlaceholderRequest request) {
        if (questPlayer == null || request == null || quest(request.questName()) == null) {
            return null;
        }
        return questPlayerManager.getActiveObjectives().activeObjective(
                questPlayer.playerIdentifier(),
                request.questName(),
                request.objectiveId());
    }

    private QuestPlayer.CompletedObjective completedObjectiveForPlaceholder(
            final PlatformPlayer questPlayer,
            final ObjectivePlaceholderRequest request) {
        final QuestPlayer playerData = questPlayer(questPlayer);
        if (playerData == null || request == null || quest(request.questName()) == null) {
            return null;
        }
        final String objectiveId = String.valueOf(request.objectiveId());
        final QuestPlayer.CompletedObjective recorded = playerData.getCompletedObjective(request.questName(), objectiveId);
        if (recorded != null) {
            return recorded;
        }
        return playerData.getCompletedObjectiveIDs(request.questName()).stream()
                .anyMatch(objectiveId::equalsIgnoreCase)
                ? completedObjectiveFallback(request.questName(), objectiveId)
                : null;
    }

    private record ObjectivePlaceholderRequest(int objectiveId, String questName) {
        private ObjectivePlaceholderRequest {
            questName = questName == null ? "" : questName;
        }
    }

    private Quest.CooldownDisplay.Text questCooldownText(final PlatformPlayer questPlayer) {
        return new Quest.CooldownDisplay.Text() {
            @Override
            public String prefix() {
                return cooldownText(questPlayer, "prefix", Map.of(), "");
            }

            @Override
            public String noCooldown() {
                return cooldownText(questPlayer, "no-cooldown", Map.of(), "");
            }

            @Override
            public String minute() {
                return cooldownText(questPlayer, "minute", Map.of(), "1 minute");
            }

            @Override
            public String minutes(final String minutes) {
                return cooldownText(questPlayer, "minutes", Map.of("%MINUTES%", minutes), minutes + " minutes");
            }

            @Override
            public String hour() {
                return cooldownText(questPlayer, "hour", Map.of(), "1 hour");
            }

            @Override
            public String hours(final String hours) {
                return cooldownText(questPlayer, "hours", Map.of("%HOURS%", hours), hours + " hours");
            }

            @Override
            public String day() {
                return cooldownText(questPlayer, "day", Map.of(), "1 day");
            }

            @Override
            public String days(final String days) {
                return cooldownText(questPlayer, "days", Map.of("%DAYS%", days), days + " days");
            }
        };
    }

    private String cooldownText(
            final PlatformPlayer questPlayer,
            final String key,
            final Map<String, String> replacements,
            final String fallback) {
        final String translationKey = "placeholders.questcooldownleftformatted." + key;
        return questPlayer == null
                ? translate(translationKey, replacements, fallback)
                : translate(questPlayer, translationKey, replacements, fallback);
    }

    public List<String> visibleQuestIdentifiers(
            final PlatformPlayer questPlayer,
            final Collection<Quest> quests,
            final long nowMillis,
            final Consumer<String> warningSink) {
        return Quest.visibleQuestIdentifiers(
                quests,
                questPlayer(questPlayer),
                configuration,
                nowMillis,
                quest -> questRequirementsFulfilled(questPlayer, quest, warningSink));
    }

    public boolean showAttachedNpcQuestPreview(
            final PlatformPlayer questPlayer,
            final String npcType,
            final NQNPCID npcId,
            final boolean armorStand) {
        if (questPlayer == null || npcId == null) {
            return false;
        }
        final List<Quest> attached = questsAttachedToNpc(npcType, npcId, true);
        if (attached.isEmpty()) {
            return false;
        }
        if (configuration.questPreviewGuiEnabled()) {
            return openGui(
                    questPlayer,
                    configuration.npcGuiName(),
                    "",
                    new GuiContext("", "", npcType, npcId));
        }
        questPlayer.sendMessage("");
        final String availableLabel = armorStand ? "Available" : "Availahle";
        questPlayer.sendMessage("<BLUE>" + attached.size() + " " + availableLabel + " Quests:");
        int counter = 1;
        for (final Quest quest : attached) {
            final String displayName = questDisplayNameOrIdentifier(quest.getIdentifier());
            questPlayer.sendCommandChoice(
                    "<YELLOW>" + counter + ". <highlight>" + displayName + " ",
                    "<GREEN>[CHOOSE]",
                    "nquests preview " + quest.getIdentifier(),
                    "<GREEN>Click to preview/choose the quest <highlight>" + displayName);
            counter++;
        }
        return true;
    }

    public boolean canAcceptQuest(final PlatformPlayer questPlayer, final String questName) {
        if (questName == null || questName.isBlank()) {
            return true;
        }
        final Quest quest = quest(questName);
        if (quest == null) {
            return true;
        }
        final QuestPlayer playerData = questPlayer(questPlayer);
        if (playerData != null && questAcceptCheck(questPlayer, questName).status() != Quest.AcceptCheck.Status.ACCEPTABLE) {
            return false;
        }
        if (playerData == null && quest.getMaxAccepts() == 0) {
            return false;
        }
        return unmetRequirements(questPlayer, quest, null).isEmpty();
    }

    public void saveAction(
            final String actionName,
            final Actions.Type actionType,
            final Actions.Data data) {
        saveAction(actionName, actionType, data, null);
    }

    public void saveAction(
            final String actionName,
            final Actions.Type actionType,
            final Actions.Data data,
            final Duration executionDelay) {
        savedActions.save(actionName, actionType, data, executionDelay);
    }

    public boolean syncSavedAction(
            final String actionName,
            final String actionTypeId,
            final Actions.Data data,
            final Duration executionDelay,
            final String category,
            final List<ActionCondition> conditions) {
        final Actions.Type actionType = actionType(actionTypeId);
        if (actionName == null || actionName.isBlank() || actionType == null) {
            return false;
        }
        final SavedActions.SavedAction action =
                savedActions.saveAndReturn(actionName, actionType, data, executionDelay);
        action.setCategory(category);
        action.clearConditions();
        if (conditions == null) {
            return true;
        }
        for (final ActionCondition settings : conditions) {
            if (settings == null) {
                continue;
            }
            final Conditions.Type conditionType = conditionType(settings.typeId());
            if (conditionType == null) {
                continue;
            }
            action.addCondition(
                    settings.id(),
                    conditionType,
                    settings.data(),
                    settings.settings());
        }
        return true;
    }

    public void saveCondition(
            final String conditionName,
            final Conditions.Type conditionType,
            final Conditions.Data data) {
        saveConditionAndReturn(conditionName, conditionType, data);
    }

    public StoredCondition saveConditionAndReturn(
            final String conditionName,
            final Conditions.Type conditionType,
            final Conditions.Data data) {
        if (conditionName == null || conditionName.isBlank()) {
            throw new IllegalArgumentException("Condition name cannot be blank.");
        }
        final StoredCondition condition = new StoredCondition(conditionName, conditionType, data);
        savedConditions.put(conditionName, condition);
        return condition;
    }

    public boolean syncSavedCondition(
            final String conditionName,
            final String conditionTypeId,
            final Conditions.Data data,
            final String category,
            final Quest.ConditionSettings settings) {
        final Conditions.Type conditionType = conditionType(conditionTypeId);
        if (conditionName == null || conditionName.isBlank() || conditionType == null) {
            return false;
        }
        final StoredCondition condition = saveConditionAndReturn(conditionName, conditionType, data);
        if (settings == null) {
            condition.setCategory(category);
        } else {
            condition.applyMetadata(
                    category,
                    settings.progressNeeded(),
                    settings.negated(),
                    settings.description(),
                    settings.hiddenExpression());
        }
        return true;
    }

    public StoredCondition savedCondition(final String conditionName) {
        if (conditionName == null || conditionName.isBlank()) {
            return null;
        }
        return savedConditions.get(conditionName);
    }

    public boolean deleteSavedCondition(final String conditionName) {
        if (conditionName == null || conditionName.isBlank()) {
            return false;
        }
        return savedConditions.remove(conditionName) != null;
    }

    public boolean deleteSavedAction(final String actionName) {
        return savedActions.delete(actionName);
    }

    public boolean savedConditionFulfilled(
            final PlatformPlayer questPlayer,
            final String conditionName) {
        final StoredCondition condition = savedCondition(conditionName);
        if (condition == null || condition.getType() == null || condition.getType().checker() == null) {
            return false;
        }
        return ConditionCheck.check(condition.getType(), condition.getData(), questPlayer).isBlank();
    }

    public boolean evaluateSavedConditionExpression(
            final PlatformPlayer questPlayer,
            final String expression) {
        return evaluateBooleanExpression(questPlayer, expression == null ? "" : expression.trim());
    }

    public String savedConditionDescription(
            final PlatformPlayer questPlayer,
            final String conditionName,
            final Object... objects) {
        final StoredCondition condition = savedCondition(conditionName);
        if (condition == null) {
            return conditionName == null ? "" : conditionName;
        }
        if (!condition.getHiddenExpression().isBlank()
                && evaluateBooleanExpression(questPlayer, condition.getHiddenExpression())) {
            return "Hidden";
        }
        if (!condition.getDescription().isBlank()) {
            return "<GRAY>" + condition.getDescription();
        }
        if (condition.getType() != null && condition.getType().descriptionRenderer() != null) {
            return condition.getType().descriptionRenderer().render(condition.getData(), questPlayer, objects);
        }
        return conditionName == null ? "" : conditionName;
    }

    public boolean conditionHidden(
            final PlatformPlayer questPlayer,
            final Condition condition) {
        return condition != null
                && !condition.getHiddenExpression().isBlank()
                && evaluateBooleanExpression(questPlayer, condition.getHiddenExpression());
    }

    private boolean evaluateBooleanExpression(
            final PlatformPlayer questPlayer,
            final String expression) {
        final String clean = stripOuterParentheses(expression == null ? "" : expression.trim());
        if (clean.isBlank()) {
            return false;
        }
        final List<String> orParts = splitTopLevel(clean, '|');
        if (orParts.size() > 1) {
            return orParts.stream().anyMatch(part -> evaluateBooleanExpression(questPlayer, part));
        }
        final List<String> andParts = splitTopLevel(clean, '&');
        if (andParts.size() > 1) {
            return andParts.stream().allMatch(part -> evaluateBooleanExpression(questPlayer, part));
        }
        if (clean.startsWith("!")) {
            return !evaluateBooleanExpression(questPlayer, clean.substring(1));
        }
        if (clean.equalsIgnoreCase("true")) {
            return true;
        }
        if (clean.equalsIgnoreCase("false")) {
            return false;
        }
        try {
            return Double.parseDouble(clean) != 0;
        } catch (final NumberFormatException ignored) {
            return savedConditionFulfilled(questPlayer, clean);
        }
    }

    private static List<String> splitTopLevel(final String expression, final char operator) {
        final ArrayList<String> parts = new ArrayList<>();
        int depth = 0;
        int start = 0;
        for (int index = 0; index < expression.length(); index++) {
            final char current = expression.charAt(index);
            if (current == '(') {
                depth++;
            } else if (current == ')') {
                depth = Math.max(0, depth - 1);
            } else if (current == operator && depth == 0) {
                parts.add(expression.substring(start, index).trim());
                if (index + 1 < expression.length() && expression.charAt(index + 1) == operator) {
                    index++;
                }
                start = index + 1;
            }
        }
        parts.add(expression.substring(start).trim());
        return parts.stream().filter(part -> !part.isBlank()).toList();
    }

    private static String stripOuterParentheses(final String expression) {
        String clean = expression;
        while (clean.startsWith("(") && clean.endsWith(")") && enclosesWholeExpression(clean)) {
            clean = clean.substring(1, clean.length() - 1).trim();
        }
        return clean;
    }

    private static boolean enclosesWholeExpression(final String expression) {
        int depth = 0;
        for (int index = 0; index < expression.length(); index++) {
            final char current = expression.charAt(index);
            if (current == '(') {
                depth++;
            } else if (current == ')') {
                depth--;
                if (depth == 0 && index < expression.length() - 1) {
                    return false;
                }
            }
            if (depth < 0) {
                return false;
            }
        }
        return depth == 0;
    }

    public void saveConversation(final String conversationName, final List<String> lines) {
        conversations.save(conversationName, lines);
    }

    public String conversationSpeakerLine(
            final PlatformPlayer questPlayer,
            final Speaker speaker,
            final String message) {
        final Speaker checkedSpeaker = speaker == null ? new Speaker("", 0) : speaker;
        return translateFor(
                questPlayer,
                "chat.conversations.speaker-line-format",
                Map.of(
                        "%SPEAKERCOLOR%", checkedSpeaker.getColor(),
                        "%SPEAKER%", checkedSpeaker.getSpeakerDisplayName(),
                        "%MESSAGE%", resolvePlayerPlaceholders(questPlayer, message)),
                "%SPEAKERCOLOR%[%SPEAKER%] <GRAY>%MESSAGE%");
    }

    public String conversationAnswerOptionLine(
            final PlatformPlayer questPlayer,
            final Speaker speaker,
            final String message,
            final int optionNumber) {
        final Speaker checkedSpeaker = speaker == null ? new Speaker("", 0) : speaker;
        return translateFor(
                questPlayer,
                "chat.conversations.answer-option-line-format",
                Map.of(
                        "%SPEAKERCOLOR%", checkedSpeaker.getColor(),
                        "%SPEAKER%", checkedSpeaker.getSpeakerDisplayName(),
                        "%MESSAGE%", resolvePlayerPlaceholders(questPlayer, message),
                        "%OPTIONNUMBER%", String.valueOf(optionNumber)),
                " <main>%OPTIONNUMBER%. <gray>%MESSAGE%");
    }

    public String conversationChooseAnswerPrefix(final PlatformPlayer questPlayer) {
        return translateFor(
                questPlayer,
                "chat.conversations.choose-answer-prefix",
                Map.of(),
                "<EMPTY>\n<main>Choose your answer:</main>");
    }

    public String conversationChooseAnswerHover(final PlatformPlayer questPlayer) {
        return translateFor(
                questPlayer,
                "chat.conversations.choose-answer-answer-hover-text",
                Map.of(),
                "<highlight>Click to answer");
    }

    public String conversationEndedPreviousMessage(final PlatformPlayer questPlayer) {
        return translateFor(
                questPlayer,
                "chat.conversations.ended-previous-conversation",
                Map.of(),
                "<main>You have ended your previous conversation!");
    }

    private void conversationFocusEnded(final PlatformPlayer questPlayer) {
        if (questPlayer == null) {
            return;
        }
        questPlayer.sendMessage(conversationEndedPreviousMessage(questPlayer));
        stopConversation(questPlayer);
    }

    public boolean executeConversationActionLine(
            final String rawAction,
            final PlatformPlayer questPlayer,
            final Consumer<String> warningSink) {
        return executeRegistryActionLine(rawAction, questPlayer, warningSink, "conversation line");
    }

    public boolean conversationConditionLineFulfilled(
            final String rawCondition,
            final PlatformPlayer questPlayer,
            final Consumer<String> warningSink) {
        final Result result =
                checkRegistryCondition(rawCondition, questPlayer, "conversation line");
        if (!result.fulfilled()) {
            warn(warningSink, result.message());
        }
        return result.fulfilled();
    }

    private void executeConversationAction(
            final String rawAction,
            final PlatformPlayer questPlayer) {
        executeConversationAction(rawAction, questPlayer, registryHooks.warn());
    }

    private boolean executeConversationAction(
            final String rawAction,
            final PlatformPlayer questPlayer,
            final Consumer<String> warningSink) {
        return executeRegistryActionLine(rawAction, questPlayer, warningSink, "conversation line");
    }

    private boolean executeRegistryActionLine(
            final String rawAction,
            final PlatformPlayer questPlayer,
            final Consumer<String> warningSink,
            final String source) {
        final String actionLine = rawAction == null ? "" : rawAction.trim();
        if (actionLine.isBlank()) {
            return false;
        }
        try {
            if (actionLine.toLowerCase(Locale.ROOT).startsWith("action ")) {
                final String actionNames = actionLine.substring("action ".length()).replace(" ", "");
                executeSavedActions(
                        Chain.builder().actionNames(actionNames).build(),
                        actionScheduler,
                        questPlayer,
                        warningSink);
                return true;
            }
            final ParsedRegistryLine<Actions.Type> parsed = conversationAction(actionLine);
            if (parsed.type() == null) {
                warn(warningSink, "Unable to find " + source + " action: " + firstToken(actionLine));
                return false;
            }
            final Action data =
                    Actions.parse(runtimeAdapter(), parsed.type(), parsed.arguments(), questPlayer);
            return executeConfiguredAction(
                    parsed.type().id(),
                    data,
                    null,
                    List.of(),
                    questPlayer,
                    warningSink,
                    source + " action failed");
        } catch (final RuntimeException exception) {
            warn(warningSink,
                    "Unable to execute " + source + " action '" + actionLine + "': " + exception.getMessage());
            return false;
        }
    }

    private Result checkConversationCondition(
            final String rawCondition,
            final PlatformPlayer questPlayer) {
        return checkRegistryCondition(rawCondition, questPlayer, "conversation line");
    }

    private Result checkRegistryCondition(
            final String rawCondition,
            final PlatformPlayer questPlayer,
            final String source) {
        String conditionLine = rawCondition == null ? "" : rawCondition.trim();
        if (conditionLine.isBlank()) {
            return new Result(true, "");
        }
        boolean negated = false;
        if (conditionLine.startsWith("!")) {
            negated = true;
            conditionLine = conditionLine.substring(1).trim();
        }
        try {
            final String result;
            if (conditionLine.toLowerCase(Locale.ROOT).startsWith("condition ")) {
                final String conditionName = conditionLine.substring("condition ".length()).replace(" ", "");
                final StoredCondition condition = savedCondition(conditionName);
                if (condition == null || condition.getType() == null || condition.getType().checker() == null) {
                    return rejected("Unable to find " + source + " condition: " + conditionName);
                }
                final Condition data = new Condition(0, condition.getType().id(), condition.getData());
                data.setNegated(condition.isNegated() ^ negated);
                result = ConditionCheck.check(condition.getType(), data, questPlayer);
            } else {
                final ParsedRegistryLine<Conditions.Type> parsed = conversationCondition(conditionLine);
                if (parsed.type() == null) {
                    return rejected("Unable to find " + source + " condition type: " + firstToken(conditionLine));
                }
                final Condition data =
                        Conditions.parse(runtimeAdapter(), parsed.type(), parsed.arguments(), questPlayer);
                data.setNegated(data.flag("negated") ^ negated);
                result = ConditionCheck.check(parsed.type(), data, questPlayer);
            }
            return new Result(
                    result == null || result.isBlank(),
                    result == null ? "" : result);
        } catch (final RuntimeException exception) {
            return rejected("Unable to check " + source + " condition '"
                    + conditionLine + "': " + exception.getMessage());
        }
    }

    private static Result rejected(final String message) {
        return new Result(false, message);
    }

    private ParsedRegistryLine<Actions.Type> conversationAction(final String actionLine) {
        final String firstToken = firstToken(actionLine);
        final Actions.Type type = actionType(firstToken);
        if (type != null) {
            return new ParsedRegistryLine<>(type, remainderAfterFirstToken(actionLine));
        }
        final Actions.Type variableType = actionType(variableRegistryType(firstToken));
        return new ParsedRegistryLine<>(variableType, actionLine);
    }

    private ParsedRegistryLine<Conditions.Type> conversationCondition(final String conditionLine) {
        final String firstToken = firstToken(conditionLine);
        final Conditions.Type type = conditionType(firstToken);
        if (type != null) {
            return new ParsedRegistryLine<>(type, remainderAfterFirstToken(conditionLine));
        }
        final Conditions.Type variableType = conditionType(variableRegistryType(firstToken));
        return new ParsedRegistryLine<>(variableType, conditionLine);
    }

    private String variableRegistryType(final String variableName) {
        final VariableDataType variableType = runtimeAdapter().variableType(variableName);
        if (variableType == null) {
            return "";
        }
        return switch (variableType) {
            case NUMBER -> "Number";
            case STRING -> "String";
            case BOOLEAN -> "Boolean";
            case LIST -> "List";
            case ITEMSTACKLIST -> "ItemStackList";
            default -> "";
        };
    }

    private static String firstToken(final String line) {
        final List<String> tokens = Actions.tokenize(line);
        return tokens.isEmpty() ? "" : tokens.getFirst();
    }

    private static String remainderAfterFirstToken(final String line) {
        final List<String> tokens = Actions.tokenize(line);
        if (tokens.size() <= 1) {
            return "";
        }
        return String.join(" ", tokens.subList(1, tokens.size()));
    }

    private record ParsedRegistryLine<T>(T type, String arguments) {}

    public SavedActions.Execution executeSavedActions(
            final Chain request,
            final SavedActions.ActionScheduler scheduler,
            final PlatformPlayer questPlayer,
            final Consumer<String> warningSink) {
        return savedActions.execute(
                request,
                scheduler,
                questPlayer,
                warningSink);
    }

    public <T> void executeActionChain(
            final Chain request,
            final Function<String, T> resolveAction,
            final Predicate<T> conditionsFulfilled,
            final ChainRunner.ActionExecutor<T> executeAction,
            final Consumer<String> warningSink) {
        ChainRunner.<T>builder()
                .resolveWith(resolveAction)
                .conditionsFulfilledBy(conditionsFulfilled)
                .executeWith(executeAction)
                .warnWith(warningSink)
                .build()
                .run(request);
    }

    public boolean giveQuest(
            final PlatformPlayer questPlayer,
            final String questName,
            final boolean forceGive,
            final Consumer<String> warningSink) {
        return giveQuest(
                questPlayer,
                questName,
                Quest.GiveOptions.normal().forceGive(forceGive),
                warningSink);
    }

    public boolean giveQuest(
            final PlatformPlayer questPlayer,
            final String questName,
            final Quest.GiveOptions options,
            final Consumer<String> warningSink) {
        final Quest.GiveOptions giveOptions = options == null ? Quest.GiveOptions.normal() : options;
        final QuestPlayer playerData = questPlayer(questPlayer);
        if (playerData == null || questName == null || questName.isBlank()) {
            warn(warningSink, "Cannot give quest: missing target player or quest name.");
            return false;
        }
        final Quest quest = quest(questName);
        if (quest == null) {
            warn(warningSink, "Cannot give quest: unknown quest " + questName + ".");
            return false;
        }
        if (!giveOptions.forceGive() && !quest.isTakeEnabled()) {
            final String message = translate(questPlayer,
                    "chat.take-disabled",
                    questReplacements(quest),
                    "<error>Accepting or previewing the quest <highlight>"
                            + quest.getIdentifier() + "</highlight> is disabled.");
            questPlayer.sendMessage(message);
            warn(warningSink, message);
            return false;
        }
        if (!giveOptions.forceGive()) {
            final Quest.AcceptCheck check = Quest.acceptCheck(
                    quest,
                    configuration.maxActiveQuestsPerPlayer(),
                    playerData.getActiveQuestIdentifiers(),
                    playerData.getCompletedQuests(),
                    playerData.getFailedQuests(),
                    System.currentTimeMillis());
            if (check.status() != Quest.AcceptCheck.Status.ACCEPTABLE) {
                final String message = questAcceptFailureMessage(questPlayer, quest, check);
                questPlayer.sendMessage(message);
                warn(warningSink, message);
                return false;
            }
            final List<String> unmetRequirements = unmetRequirements(questPlayer, quest, warningSink);
            if (!unmetRequirements.isEmpty()) {
                final String message = translate(questPlayer,
                        "chat.quest-not-all-requirements-fulfilled",
                        Map.of(),
                        "<negative>You do not fulfill all the requirements this quest needs! Requirement still needed:")
                        + "\n" + String.join("\n", unmetRequirements);
                questPlayer.sendMessage(message);
                warn(warningSink, message);
                return false;
            }
        }
        if (giveOptions.callPlatformAcceptEvent()
                && !questPlayer.beforeQuestAccepted(quest, giveOptions.triggerAcceptQuestTrigger())) {
            return false;
        }
        if (playerData.acceptQuest(quest)) {
            activateQuestTriggers(questPlayer, quest);
            final List<ObjectiveActivation> activatedObjectives = activateQuestObjectives(
                    questPlayer,
                    quest,
                    warningSink,
                    giveOptions.triggerAcceptQuestTrigger());
            if (giveOptions.triggerAcceptQuestTrigger()) {
                triggerQuestEvent(questPlayer, "BEGIN", quest.getIdentifier());
            }
            if (giveOptions.sendQuestInfo()) {
                sendQuestAcceptedDisplay(questPlayer, quest, activatedObjectives);
            }
            return true;
        }
        return false;
    }

    private List<String> unmetRequirements(
            final PlatformPlayer questPlayer,
            final Quest quest,
            final Consumer<String> warningSink) {
        final ArrayList<String> messages = new ArrayList<>();
        for (final Condition requirement : quest.getRequirements()) {
            final Conditions.Type conditionType = conditionType(requirement.typeId());
            if (conditionType == null || conditionType.checker() == null) {
                warn(warningSink, "Cannot check unknown quest requirement type: " + requirement.typeId());
                messages.add("<warn>Error: Requirement " + requirement.id() + " could not be checked.");
                continue;
            }
            try {
                final String result = ConditionCheck.check(conditionType, requirement.data(), questPlayer);
                if (result != null && !result.isBlank()) {
                    messages.add(result);
                }
            } catch (final RuntimeException exception) {
                warn(warningSink, "Quest requirement check failed: " + exception.getMessage());
                messages.add("<warn>Error: Requirement " + requirement.id() + " could not be checked.");
            }
        }
        final String questOrderMessage = questOrderRequirementMessage(questPlayer, quest);
        if (!questOrderMessage.isBlank()) {
            messages.add(questOrderMessage);
        }
        return List.copyOf(messages);
    }

    public String questOrderRequirementMessage(
            final PlatformPlayer questPlayer,
            final Quest quest) {
        final QuestPlayer playerData = questPlayer(questPlayer);
        if (playerData == null || quest == null) {
            return "";
        }
        final Category category = category(quest.getCategory());
        final PredefinedProgressOrder progressOrder =
                PredefinedProgressOrder.fromString(category == null ? "" : category.getProgressOrder());
        if (progressOrder == null) {
            return "";
        }
        final List<String> questNames = questNamesInCategory(quest.getCategory());
        final ArrayList<Quest.OrderEntry> orderedQuests =
                new ArrayList<>();
        int questIndex = -1;
        for (int index = 0; index < questNames.size(); index++) {
            final String orderedQuestName = questNames.get(index);
            final Quest orderedQuest = quest(orderedQuestName);
            orderedQuests.add(new Quest.OrderEntry(
                    orderedQuestName,
                    questDisplayName(orderedQuestName, orderedQuest)));
            if (orderedQuestName.equalsIgnoreCase(quest.getIdentifier())) {
                questIndex = index;
            }
        }
        if (questIndex < 0) {
            return "";
        }
        return Quest.OrderRequirements.requirementMessage(
                progressOrder,
                quest.getIdentifier(),
                questIndex,
                orderedQuests,
                playerData::hasCompletedQuest);
    }

    public String questOrderRequirementMessage(
            final PlatformPlayer questPlayer,
            final String questName) {
        return questOrderRequirementMessage(questPlayer, quest(questName));
    }

    public boolean questRequirementsFulfilled(
            final PlatformPlayer questPlayer,
            final Quest quest,
            final Consumer<String> warningSink) {
        return unmetRequirements(questPlayer, quest, warningSink).isEmpty();
    }

    public String questRequirementsText(
            final PlatformPlayer questPlayer,
            final String questName) {
        final Quest quest = quest(questName);
        final String orderReq = questOrderRequirementMessage(questPlayer, quest);
        final StringBuilder text = new StringBuilder();
        int counter = 1;
        for (final var req : questRequirementEntries(quest)) {
            final boolean hidden = conditionHidden(questPlayer, req);
            if (hidden) continue;
            if (counter != 1) text.append("\n");
            text.append("<GREEN>").append(counter).append(". <YELLOW>")
                    .append(displayEntryType(req.typeId(), req.values())).append("\n")
                    .append(conditionDescription(req, conditionType(req.typeId()), questPlayer)).append("\n");
            counter++;
        }
        if (orderReq != null && !orderReq.isBlank()) {
            if (counter != 1) text.append("\n");
            text.append("<GREEN>").append(counter).append(". <YELLOW>Quest Order\n").append(orderReq).append("\n");
        }
        return text.toString();
    }

    public List<String> questRequirementsList(
            final PlatformPlayer questPlayer,
            final String questName) {
        final Quest quest = quest(questName);
        final String orderReq = questOrderRequirementMessage(questPlayer, quest);
        final ArrayList<String> lines = new ArrayList<>();
        int counter = 1;
        for (final var req : questRequirementEntries(quest)) {
            final boolean hidden = conditionHidden(questPlayer, req);
            if (hidden) continue;
            lines.add("<GREEN>" + counter + ". <YELLOW>" + displayEntryType(req.typeId(), req.values()));
            lines.add(conditionDescription(req, conditionType(req.typeId()), questPlayer));
            counter++;
        }
        if (orderReq != null && !orderReq.isBlank()) {
            lines.add("<GREEN>" + counter + ". <YELLOW>Quest Order");
            lines.add(orderReq);
        }
        return List.copyOf(lines);
    }

    public String questRewardsText(
            final PlatformPlayer questPlayer,
            final String questName) {
        final Quest quest = quest(questName);
        final boolean hideUnnamed = configuration().hideRewardsWithoutName();
        final StringBuilder text = new StringBuilder();
        int counter = 1;
        for (final var reward : questRewardEntries(quest)) {
            if (counter != 1) text.append("\n");
            text.append(formatRewardLine(counter, reward, hideUnnamed, questPlayer, quest));
            counter++;
        }
        return text.toString();
    }

    public List<String> questRewardsList(
            final PlatformPlayer questPlayer,
            final String questName) {
        final Quest quest = quest(questName);
        final boolean hideUnnamed = configuration().hideRewardsWithoutName();
        final ArrayList<String> lines = new ArrayList<>();
        int counter = 1;
        for (final var reward : questRewardEntries(quest)) {
            lines.add(formatRewardLine(counter, reward, hideUnnamed, questPlayer, quest));
            counter++;
        }
        return List.copyOf(lines);
    }

    private static final String QUEST_PREVIEW_SEPARATOR = "<GRAY>-----------------------------------";

    public QuestPreview singleQuestPreview(
            final PlatformPlayer questPlayer,
            final String questName) {
        final Quest quest = quest(questName);
        final String identifier = quest == null ? questName : quest.getIdentifier();
        final String displayName = quest == null ? questName : questDisplayName(quest);
        final String description = quest == null ? "" : quest.getDescription();
        final String missingDescText = quest == null ? "" : translateFor(
                questPlayer, "chat.missing-quest-description",
                questReplacements(quest), "<unimportant>This quest has no quest description.");
        final String requirements = questRequirementsText(questPlayer, identifier);
        final String rewards = questRewardsText(questPlayer, identifier);
        return new QuestPreview(
                List.of("", QUEST_PREVIEW_SEPARATOR,
                        "<BLUE>Quest Preview for Quest <highlight>" + displayName + "</highlight>:",
                        description == null || description.isBlank() ? missingDescText
                                : "<YELLOW>Quest description: <GRAY>" + description,
                        "<BLUE>Quest Requirements:", requirements,
                        "<BLUE>Quest Rewards:", rewards, ""),
                "<GREEN>**[ACCEPT THIS QUEST]",
                "nquests take " + identifier,
                "<GREEN>Click to accept the Quest <highlight>" + displayName,
                QUEST_PREVIEW_SEPARATOR);
    }

    private List<Condition> questRequirementEntries(final Quest quest) {
        return quest == null ? List.of() : quest.getRequirements();
    }

    private List<Action> questRewardEntries(final Quest quest) {
        return quest == null ? List.of() : quest.getRewards();
    }

    private String formatRewardLine(
            final int counter,
            final Action reward,
            final boolean hideUnnamed,
            final PlatformPlayer questPlayer,
            final Quest quest) {
        final String name = reward.getDisplayName();
        if (name != null && !name.isBlank()) {
            return "<GREEN>" + counter + ". <BLUE>" + name + "</GREEN>";
        }
        if (hideUnnamed) {
            final String hiddenText = translateFor(questPlayer, "gui.reward-hidden-text",
                    quest == null ? Map.of() : questReplacements(quest), ". <blue>Reward hidden</blue>");
            return "<GREEN>" + counter + hiddenText + "</GREEN>";
        }
        return "<GREEN>" + counter + ". <BLUE>"
                + actionDescription(reward, actionType(reward.typeId()), questPlayer) + "</GREEN>";
    }

    private String conditionDescription(
            final Condition condition,
            final Conditions.Type type,
            final PlatformPlayer questPlayer) {
        final String fallback;
        if (type == null) {
            fallback = condition == null ? "" : condition.typeId();
        } else if (type.descriptionRenderer() == null) {
            fallback = type.description();
        } else {
            String rendered;
            try {
                rendered = type.descriptionRenderer().render(condition.data(), questPlayer);
            } catch (final RuntimeException exception) {
                rendered = type.description();
            }
            fallback = rendered;
        }
        return ConditionCheck.description(
                condition == null ? "" : condition.getDescription(),
                fallback);
    }

    private String actionDescription(
            final Action action,
            final Actions.Type type,
            final PlatformPlayer questPlayer) {
        if (action != null && action.getDescription() != null && !action.getDescription().isBlank()) {
            return action.getDescription();
        }
        if (type == null) {
            return action == null ? "" : action.typeId();
        }
        if (type.descriptionRenderer() == null) {
            return type.displayName() == null || type.displayName().isBlank()
                    ? type.description()
                    : type.displayName();
        }
        try {
            return type.descriptionRenderer().render(action.data(), questPlayer);
        } catch (final RuntimeException exception) {
            return type.displayName() == null || type.displayName().isBlank()
                    ? type.description()
                    : type.displayName();
        }
    }

    private List<ObjectiveActivation> activateQuestObjectives(
            final PlatformPlayer questPlayer,
            final Quest quest,
            final Consumer<String> warningSink) {
        return activateQuestObjectives(
                questPlayer,
                activeProfile(questPlayer == null ? "" : questPlayer.playerIdentifier()),
                quest,
                warningSink,
                true);
    }

    private List<ObjectiveActivation> activateQuestObjectives(
            final PlatformPlayer questPlayer,
            final Quest quest,
            final Consumer<String> warningSink,
            final boolean triggerObjectiveBeginTriggers) {
        return activateQuestObjectives(
                questPlayer,
                activeProfile(questPlayer == null ? "" : questPlayer.playerIdentifier()),
                quest,
                warningSink,
                triggerObjectiveBeginTriggers);
    }

    private List<ObjectiveActivation> activateQuestObjectives(
            final PlatformPlayer questPlayer,
            final String profile,
            final Quest quest,
            final Consumer<String> warningSink,
            final boolean triggerObjectiveBeginTriggers) {
        return activateQuestObjectives(
                questPlayer,
                profile,
                quest,
                warningSink,
                triggerObjectiveBeginTriggers,
                false);
    }

    private List<ObjectiveActivation> activateQuestObjectives(
            final PlatformPlayer questPlayer,
            final String profile,
            final Quest quest,
            final Consumer<String> warningSink,
            final boolean triggerObjectiveBeginTriggers,
            final boolean loading) {
        final List<ObjectiveActivation> activated = new ArrayList<>();
        for (final Objective objective : quest.getObjectives()) {
            activateObjectiveTree(
                    questPlayer,
                    profile,
                    quest,
                    objective,
                    new int[] {objective.id()},
                    quest.getObjectiveProgressOrder(),
                    warningSink,
                    activated,
                    true);
        }
        final boolean previousObjectiveUnlockTriggersEnabled = objectiveUnlockTriggersEnabled();
        setObjectiveUnlockTriggersEnabled(triggerObjectiveBeginTriggers);
        try {
            questPlayerManager.getActiveObjectives().refreshObjectiveUnlocks(questPlayer, profile, loading);
        } finally {
            setObjectiveUnlockTriggersEnabled(previousObjectiveUnlockTriggersEnabled);
        }
        return List.copyOf(activated);
    }

    private void activateObjectiveTree(
            final PlatformPlayer questPlayer,
            final String profile,
            final Quest quest,
            final Objective objective,
            final int[] objectivePath,
            final String ownerProgressOrder,
            final Consumer<String> warningSink,
            final List<ObjectiveActivation> topLevelActivations,
            final boolean topLevel) {
        final Objectives.Type objectiveType = objectiveType(objective.typeId());
        if (objectiveType == null) {
            warn(warningSink, "Cannot activate unknown objective type: " + objective.typeId());
            return;
        }
        annotateObjectiveConditionContext(quest, objective);
        final ActiveObjective progress = questPlayerManager.getActiveObjectives().activateObjective(
                questPlayer,
                profile,
                quest.getIdentifier(),
                objective,
                objectivePath,
                ownerProgressOrder,
                objectiveType,
                false);
        if (topLevel) {
            topLevelActivations.add(new ObjectiveActivation(objective, objectiveType, progress));
        }
        for (final Objective childObjective : objective.getObjectives()) {
            final int[] childPath = Arrays.copyOf(objectivePath, objectivePath.length + 1);
            childPath[childPath.length - 1] = childObjective.id();
            activateObjectiveTree(
                    questPlayer,
                    profile,
                    quest,
                    childObjective,
                    childPath,
                    objective.getChildObjectiveProgressOrder(),
                    warningSink,
                    topLevelActivations,
                    false);
        }
    }

    public void activateQuestProgress(
            final PlatformPlayer questPlayer,
            final String questName,
            final Consumer<String> warningSink) {
        activateQuestProgress(
                questPlayer,
                activeProfile(questPlayer == null ? "" : questPlayer.playerIdentifier()),
                questName,
                warningSink);
    }

    private void activateQuestProgress(
            final PlatformPlayer questPlayer,
            final String profile,
            final String questName,
            final Consumer<String> warningSink) {
        activateQuestProgress(questPlayer, profile, questName, warningSink, false);
    }

    private void activateQuestProgress(
            final PlatformPlayer questPlayer,
            final String profile,
            final String questName,
            final Consumer<String> warningSink,
            final boolean loading) {
        questPlayerManager.withProfile(
                questPlayer == null ? "" : questPlayer.playerIdentifier(),
                profile,
                () -> {
            final Quest quest = quest(questName);
            if (quest == null || questPlayer == null) {
                warn(warningSink, "Cannot activate quest progress: missing player or quest " + questName + ".");
                return;
            }
            questPlayerManager.getActiveObjectives().removeActiveObjectives(questPlayer.playerIdentifier(), profile, quest.getIdentifier());
            removeActiveTriggers(questPlayer.playerIdentifier(), profile, quest.getIdentifier());
            activateQuestTriggers(questPlayer, profile, quest);
            activateQuestObjectives(questPlayer, profile, quest, warningSink, false, loading);
        });
    }

    private void activateQuestTriggers(final PlatformPlayer questPlayer, final Quest quest) {
        activateQuestTriggers(
                questPlayer,
                activeProfile(questPlayer == null ? "" : questPlayer.playerIdentifier()),
                quest);
    }

    private void activateQuestTriggers(
            final PlatformPlayer questPlayer,
            final String profile,
            final Quest quest) {
        questPlayerManager.activateTriggers(questPlayer, profile, quest);
    }

    private void removeActiveTriggers(final String playerIdentifier, final String questName) {
        questPlayerManager.removeActiveTriggers(playerIdentifier, questName);
    }

    private void removeActiveTriggers(
            final String playerIdentifier,
            final String profile,
            final String questName) {
        questPlayerManager.removeActiveTriggers(playerIdentifier, profile, questName);
    }

    public void triggerQuestEvent(
            final PlatformPlayer questPlayer,
            final String triggerType,
            final String questName) {
        triggerEvent(questPlayer, Event.quest(triggerType, questName, worldName(questPlayer)));
    }

    public void triggerObjectiveEvent(
            final PlatformPlayer questPlayer,
            final String triggerType,
            final String questName,
            final int objectiveId) {
        triggerEvent(questPlayer, Event.objective(triggerType, questName, objectiveId, worldName(questPlayer)));
    }

    public void playerDied(final PlatformPlayer questPlayer) {
        if (questPlayer == null || !questPlayer.hasPlayer()) {
            return;
        }
        final QuestPlayer playerData = questPlayer(questPlayer);
        if (playerData.getActiveQuestIdentifiers().isEmpty()) {
            return;
        }
        triggerEvent(questPlayer, Event.player("DEATH", worldName(questPlayer)));
    }

    public void npcDied(
            final PlatformPlayer questPlayer,
            final String npcIdentifier,
            final String worldName) {
        if (questPlayer == null || !questPlayer.hasPlayer()) {
            return;
        }
        final QuestPlayer playerData = questPlayer(questPlayer);
        if (playerData.getActiveQuestIdentifiers().isEmpty()) {
            return;
        }
        triggerEvent(
                questPlayer,
                Event.player("NPCDEATH", worldName == null ? "" : worldName)
                        .withAttribute("npc", npcIdentifier == null ? "" : npcIdentifier));
    }

    public void npcDied(final String npcIdentifier, final String worldName) {
        for (final PlatformPlayer questPlayer : questPlayerManager.getActivePlatformPlayers()) {
            npcDied(
                    questPlayer,
                    npcIdentifier,
                    worldName == null || worldName.isBlank() ? worldName(questPlayer) : worldName);
        }
    }

    public void playerDisconnected(final PlatformPlayer questPlayer) {
        playerDisconnected(questPlayer, worldName(questPlayer));
    }

    public void playerDisconnected(
            final PlatformPlayer questPlayer,
            final String worldName) {
        if (questPlayer == null || !questPlayer.hasPlayer()) {
            return;
        }
        final QuestPlayer playerData = questPlayer(questPlayer);
        if (playerData.getActiveQuestIdentifiers().isEmpty()) {
            return;
        }
        triggerEvent(questPlayer, Event.player("DISCONNECT", worldName));
    }

    public void playerChangedWorld(
            final PlatformPlayer questPlayer,
            final String fromWorldName,
            final String toWorldName) {
        if (questPlayer == null || !questPlayer.hasPlayer()) {
            return;
        }
        final QuestPlayer playerData = questPlayer(questPlayer);
        if (playerData.getActiveQuestIdentifiers().isEmpty()) {
            return;
        }
        triggerEvent(questPlayer, Event.player("WORLDENTER", toWorldName));
        triggerEvent(questPlayer, Event.player("WORLDLEAVE", fromWorldName));
    }

    public void triggerEvent(final PlatformPlayer questPlayer, final Event event) {
        if (questPlayer == null || !questPlayer.hasPlayer() || event == null || event.triggerType().isBlank()) {
            return;
        }
        final List<ActiveTrigger> activeTriggers =
                questPlayerManager.getActiveTriggers(questPlayer.playerIdentifier());
        if (activeTriggers.isEmpty()) {
            return;
        }
        for (final ActiveTrigger trigger : List.copyOf(activeTriggers)) {
            if (!trigger.matches(
                    event,
                    objectiveId -> activeObjectiveUnlocked(questPlayer.playerIdentifier(), trigger.questName(), objectiveId))) {
                continue;
            }
            if (trigger.addProgress(1)) {
                executeTriggerAction(trigger, questPlayer);
            }
        }
    }

    private boolean objectiveUnlocked(final ActiveObjective objective) {
        if (objective == null) {
            return false;
        }
        final boolean triggerAcceptQuestTrigger = objectiveUnlockTriggersEnabled();
        try {
            if (!runtimeAdapter().allowObjectiveUnlock(
                    objective.getQuestPlayer(),
                    quest(objective.getQuestIdentifier()),
                    objective,
                    triggerAcceptQuestTrigger)) {
                return false;
            }
        } catch (final RuntimeException exception) {
            warn("Could not dispatch the objective unlock event.", exception);
            return false;
        }
        showObjectiveMarker(objective);
        if (triggerAcceptQuestTrigger) {
            triggerObjectiveEvent(
                    objective.getQuestPlayer(),
                    "BEGIN",
                    objective.getQuestIdentifier(),
                    objective.getObjectiveID());
        }
        return true;
    }

    private void showObjectiveMarker(final ActiveObjective objective) {
        final Objective entry = objective == null ? null : objective.getObjective();
        if (entry != null && entry.isLocationEnabled() && entry.getLocation() != null) {
            showObjectiveMarker(
                    objective.getQuestPlayer(), activeObjectiveBeamName(objective), entry.getLocation());
        }
    }

    private boolean activeObjectiveUnlocked(
            final String playerIdentifier,
            final String questName,
            final int objectiveId) {
        final ActiveObjective activeObjective =
                questPlayerManager.getActiveObjectives().activeObjective(playerIdentifier, questName, objectiveId);
        return activeObjective != null && activeObjective.isUnlocked();
    }

    private void executeTriggerAction(
            final ActiveTrigger trigger,
            final PlatformPlayer questPlayer) {
        if (trigger == null || trigger.actionName().isBlank()) {
            return;
        }
        executeSavedActions(
                Chain.builder()
                        .actionNames(trigger.actionName())
                        .amount(1)
                        .randomRange(-1, -1)
                        .ignoreConditions(false)
                        .build(),
                actionScheduler,
                questPlayer,
                warning -> registryHooks.warn().accept(warning));
    }

    private static String worldName(final PlatformPlayer questPlayer) {
        return questPlayer == null ? "" : questPlayer.worldName();
    }

    private boolean objectiveUnlockTriggersEnabled() {
        return Boolean.TRUE.equals(objectiveUnlockTriggersEnabled.get());
    }

    private void setObjectiveUnlockTriggersEnabled(final boolean enabled) {
        objectiveUnlockTriggersEnabled.set(enabled);
    }

    public void refreshObjectiveUnlocks(final PlatformPlayer questPlayer) {
        questPlayerManager.getActiveObjectives().refreshObjectiveUnlocks(questPlayer);
    }

    public void questRuntimeSecondPassed() {
        final List<PlatformPlayer> activePlayers = questPlayerManager.getActivePlatformPlayers();
        questPlayerManager.getActiveObjectives().secondPassed(
                activePlayers,
                configuration.objectiveUnlockConditionsCheckRegularIntervalSeconds());
        synchronizeJobsLevels();
        final int showTime = configuration.objectiveTrackingBossbarShowTimeSeconds();
        if (showTime <= 0) {
            return;
        }
        for (final PlatformPlayer player : activePlayers) {
            if (player == null) {
                continue;
            }
            final String playerId = player.playerIdentifier();
            final Integer age = progressBossBarAges.computeIfPresent(playerId, (ignored, seconds) -> seconds + 1);
            if (age != null && age >= showTime) {
                player.hideProgressBossBar();
                progressBossBarAges.remove(playerId);
            }
        }
    }

    private void refreshQuestRuntimeSecond() {
        questRuntimeSecondPassed();
        final List<PlatformPlayer> activePlayers = questPlayerManager.getActivePlatformPlayers();
        final boolean refreshMarkers = questPlayerManager.getActiveObjectives().objectiveMarkersDue();
        for (final PlatformPlayer player : activePlayers) {
            if (player == null) {
                continue;
            }
            if (refreshMarkers) {
                refreshObjectiveMarkers(player, false);
            }
            final ObjectiveCompass.Display display = objectiveLocationCompassDisplay(
                    player,
                    questPlayerManager.getActiveObjectives().objectiveMarkers(player));
            if (display == null) {
                player.hideLocationCompass();
            } else {
                player.showLocationCompass(display);
            }
        }
    }

    public void playerChunkLoaded(final PlatformPlayer questPlayer) {
        if (!pluginStatus.isDisabled() && questPlayer != null) {
            refreshObjectiveMarkers(questPlayer, true);
        }
    }

    public void refreshObjectives(
            final PlatformPlayer questPlayer,
            final Objectives.ObjectiveRefresh refresh) {
        questPlayerManager.getActiveObjectives().refreshObjectives(
                questPlayer,
                refresh == null ? Objectives.ObjectiveRefresh.periodic() : refresh);
    }

    public List<ActiveObjective> activeObjectives(final String playerIdentifier) {
        return questPlayerManager.getActiveObjectives().activeObjectives(playerIdentifier);
    }

    public List<ActiveObjective.Update> addProgressToActiveObjectives(
            final String playerIdentifier,
            final String objectiveTypeId,
            final Predicate<ActiveObjective> matcher,
            final double amount) {
        final ArrayList<ActiveObjective.Update> updates = new ArrayList<>();
        for (final ActiveObjective progress : matchingActiveObjectives(playerIdentifier, objectiveTypeId, matcher)) {
            updates.add(addActiveObjectiveProgress(
                    progress.getQuestPlayer(),
                    progress.getQuestIdentifier(),
                    progress.getObjectivePath(),
                    amount,
                    activeObjectiveName(progress),
                    activeObjectiveHolderName(progress)));
        }
        return List.copyOf(updates);
    }

    public List<ActiveObjective.Update> removeProgressFromActiveObjectives(
            final String playerIdentifier,
            final String objectiveTypeId,
            final Predicate<ActiveObjective> matcher,
            final double amount,
            final boolean capAtZero) {
        final ArrayList<ActiveObjective.Update> updates = new ArrayList<>();
        for (final ActiveObjective progress : matchingActiveObjectives(playerIdentifier, objectiveTypeId, matcher)) {
            updates.add(removeActiveObjectiveProgress(
                    progress.getQuestPlayer(),
                    progress.getQuestIdentifier(),
                    progress.getObjectivePath(),
                    amount,
                    capAtZero,
                    activeObjectiveName(progress),
                    activeObjectiveHolderName(progress)));
        }
        return List.copyOf(updates);
    }

    public List<ActiveObjective.Update> setProgressOfActiveObjectives(
            final String playerIdentifier,
            final String objectiveTypeId,
            final Predicate<ActiveObjective> matcher,
            final double newProgress,
            final boolean capAtZero) {
        return setProgressOfActiveObjectives(playerIdentifier, objectiveTypeId, matcher, ignored -> newProgress, capAtZero);
    }

    public List<ActiveObjective.Update> setProgressOfActiveObjectives(
            final String playerIdentifier,
            final String objectiveTypeId,
            final Predicate<ActiveObjective> matcher,
            final ToDoubleFunction<ActiveObjective> newProgress,
            final boolean capAtZero) {
        final ArrayList<ActiveObjective.Update> updates = new ArrayList<>();
        for (final ActiveObjective progress : matchingActiveObjectives(playerIdentifier, objectiveTypeId, matcher)) {
            final double currentProgress = progress.currentProgress();
            final double nextProgress = newProgress.applyAsDouble(progress);
            if (nextProgress > currentProgress) {
                updates.add(addActiveObjectiveProgress(
                        progress.getQuestPlayer(),
                        progress.getQuestIdentifier(),
                        progress.getObjectivePath(),
                        nextProgress - currentProgress,
                        activeObjectiveName(progress),
                        activeObjectiveHolderName(progress)));
            } else if (nextProgress < currentProgress) {
                updates.add(removeActiveObjectiveProgress(
                        progress.getQuestPlayer(),
                        progress.getQuestIdentifier(),
                        progress.getObjectivePath(),
                        currentProgress - nextProgress,
                        capAtZero,
                        activeObjectiveName(progress),
                        activeObjectiveHolderName(progress)));
            }
        }
        return List.copyOf(updates);
    }

    private List<ActiveObjective> matchingActiveObjectives(
            final String playerIdentifier,
            final String objectiveTypeId,
            final Predicate<ActiveObjective> matcher) {
        final Predicate<ActiveObjective> effectiveMatcher = matcher == null ? ignored -> true : matcher;
        return questPlayerManager.getActiveObjectives().dispatchableActiveObjectives(playerIdentifier).stream()
                .filter(progress -> objectiveTypeId == null
                        || objectiveTypeId.isBlank()
                        || progress.getObjectiveTypeID().equalsIgnoreCase(objectiveTypeId))
                .filter(effectiveMatcher)
                .toList();
    }

    public void betonQuestIntegrationActivationFailed(final Throwable exception) {
        final String detail = betonQuestFailureDetail(exception);
        warn(detail.isBlank()
                ? "Could not enable BetonQuest support."
                : "Could not enable BetonQuest support: " + detail);
    }

    public void betonQuestInterceptorRegistered() {
        info("Registered BetonQuest interceptor: notquests");
    }

    public String executeBetonQuestActionLine(
            final String playerIdentifier,
            final String actionLine) {
        if (playerIdentifier == null || playerIdentifier.isBlank()) {
            return "Cannot execute NotQuests action from BetonQuest: missing target player.";
        }
        final PlatformPlayer questPlayer;
        try {
            questPlayer = getOrCreatePlatformPlayer(playerIdentifier);
        } catch (final RuntimeException exception) {
            final String failure = "Cannot execute NotQuests action from BetonQuest for player '"
                    + playerIdentifier + "': " + betonQuestFailureDetail(exception);
            warn(failure);
            return failure;
        }
        if (questPlayer == null) {
            return "Cannot execute NotQuests action from BetonQuest: target player '"
                    + playerIdentifier + "' is unavailable.";
        }
        final ArrayList<String> failures = new ArrayList<>();
        final boolean executed = executeConversationActionLine(
                actionLine,
                questPlayer,
                failure -> {
                    failures.add(failure);
                    warn(failure);
                });
        if (executed) {
            return "";
        }
        if (!failures.isEmpty()) {
            return String.join("\n", failures);
        }
        final String failure = "NotQuests action line could not be executed: "
                + (actionLine == null ? "" : actionLine);
        warn(failure);
        return failure;
    }

    public boolean checkBetonQuestConditionLine(
            final String playerIdentifier,
            final String conditionLine) {
        if (playerIdentifier == null || playerIdentifier.isBlank()) {
            warn("Cannot check NotQuests condition from BetonQuest: missing target player.");
            return false;
        }
        try {
            final PlatformPlayer questPlayer = getOrCreatePlatformPlayer(playerIdentifier);
            if (questPlayer == null) {
                warn("Cannot check NotQuests condition from BetonQuest: target player '%s' is unavailable.",
                        playerIdentifier);
                return false;
            }
            return conversationConditionLineFulfilled(conditionLine, questPlayer, this::warn);
        } catch (final RuntimeException exception) {
            warn("Invalid NotQuests condition line '%s': %s",
                    conditionLine == null ? "" : conditionLine,
                    betonQuestFailureDetail(exception));
            return false;
        }
    }

    public String betonQuestAbortQuest(
            final String playerIdentifier,
            final String questName) {
        if (questName == null || questName.isBlank() || quest(questName) == null) {
            return "NotQuests quest '" + (questName == null ? "" : questName) + "' does not exist.";
        }
        if (playerIdentifier == null || playerIdentifier.isBlank()) {
            return "Cannot abort NotQuests quest '" + questName + "': missing target player.";
        }
        try {
            if (activePlatformPlayer(playerIdentifier) == null) {
                return "";
            }
            if (removeActiveQuest(playerIdentifier, activeProfile(playerIdentifier), questName)) {
                return "";
            }
            return "Cannot abort NotQuests quest '" + questName + "' because it is not active.";
        } catch (final RuntimeException exception) {
            return "Cannot abort NotQuests quest '" + questName + "': "
                    + betonQuestFailureDetail(exception);
        }
    }

    public String betonQuestFailQuest(
            final String playerIdentifier,
            final String questName) {
        if (questName == null || questName.isBlank() || quest(questName) == null) {
            return "NotQuests quest '" + (questName == null ? "" : questName) + "' does not exist.";
        }
        if (playerIdentifier == null || playerIdentifier.isBlank()) {
            return "Cannot fail NotQuests quest '" + questName + "': missing target player.";
        }
        try {
            final PlatformPlayer questPlayer = activePlatformPlayer(playerIdentifier);
            if (questPlayer == null) {
                return "";
            }
            final ArrayList<String> failures = new ArrayList<>();
            if (failQuest(questPlayer, questName, failures::add)) {
                return "";
            }
            return failures.isEmpty()
                    ? "Cannot fail NotQuests quest '" + questName + "'."
                    : String.join("\n", failures);
        } catch (final RuntimeException exception) {
            return "Cannot fail NotQuests quest '" + questName + "': "
                    + betonQuestFailureDetail(exception);
        }
    }

    public String betonQuestStartQuest(
            final String playerIdentifier,
            final String questName,
            final boolean forced,
            final boolean silent,
            final boolean triggers) {
        if (questName == null || questName.isBlank() || quest(questName) == null) {
            return "NotQuests quest '" + (questName == null ? "" : questName) + "' does not exist.";
        }
        if (playerIdentifier == null || playerIdentifier.isBlank()) {
            return "Cannot start NotQuests quest '" + questName + "': missing target player.";
        }
        try {
            final PlatformPlayer questPlayer = getOrCreatePlatformPlayer(playerIdentifier);
            if (questPlayer == null) {
                return "Cannot start NotQuests quest '" + questName + "': target player '"
                        + playerIdentifier + "' is unavailable.";
            }
            final ArrayList<String> failures = new ArrayList<>();
            if (giveQuest(
                    questPlayer,
                    questName,
                    GiveOptions.normal()
                            .forceGive(forced)
                            .triggerAcceptQuestTrigger(triggers)
                            .sendQuestInfo(!silent && !forced),
                    failure -> {
                        failures.add(failure);
                        warn(failure);
                    })) {
                return "";
            }
            return failures.isEmpty()
                    ? "Cannot start NotQuests quest '" + questName + "'."
                    : String.join("\n", failures);
        } catch (final RuntimeException exception) {
            return "Cannot start NotQuests quest '" + questName + "': "
                    + betonQuestFailureDetail(exception);
        }
    }

    public String betonQuestChangeQuestPoints(
            final String playerIdentifier,
            final String action,
            final String amount,
            final boolean silent) {
        if (playerIdentifier == null || playerIdentifier.isBlank()) {
            return "Cannot change NotQuests quest points: missing target player.";
        }
        final String normalizedAction = action == null ? "" : action.trim().toLowerCase(Locale.ROOT);
        if (!normalizedAction.equals("set")
                && !normalizedAction.equals("add")
                && !normalizedAction.equals("remove")) {
            return "NotQuests quest-points action must be set, add, or remove.";
        }
        final long parsedAmount;
        try {
            parsedAmount = Long.parseLong(amount == null ? "" : amount.trim());
        } catch (final NumberFormatException exception) {
            return "Invalid NotQuests quest-points amount.";
        }
        try {
            final PlatformPlayer questPlayer = activePlatformPlayer(playerIdentifier);
            if (questPlayer == null) {
                return "";
            }
            final boolean changed = switch (normalizedAction) {
                case "set" -> setQuestPoints(questPlayer, parsedAmount, !silent);
                case "add" -> addQuestPoints(questPlayer, parsedAmount, !silent);
                case "remove" -> removeQuestPoints(questPlayer, parsedAmount, !silent);
                default -> false;
            };
            return changed ? "" : "Could not change NotQuests quest points for the target player.";
        } catch (final RuntimeException exception) {
            return "Could not change NotQuests quest points for the target player: "
                    + betonQuestFailureDetail(exception);
        }
    }

    public String betonQuestTriggerObjective(
            final String playerIdentifier,
            final String triggerName) {
        if (playerIdentifier == null || playerIdentifier.isBlank()) {
            return "Cannot trigger NotQuests objective: missing target player.";
        }
        if (triggerName == null || triggerName.isBlank()) {
            return "Cannot trigger NotQuests objective: missing trigger name.";
        }
        try {
            final PlatformPlayer questPlayer = activePlatformPlayer(playerIdentifier);
            if (questPlayer != null) {
                triggerCommandObjectiveProgress(questPlayer, triggerName);
            }
            return "";
        } catch (final RuntimeException exception) {
            return "Cannot trigger NotQuests objective '" + triggerName + "': "
                    + betonQuestFailureDetail(exception);
        }
    }

    public void executeBetonQuestNamedAction(
            final PlatformPlayer questPlayer,
            final String packageName,
            final String actionName,
            final Consumer<PlatformPlayer> betonQuestCall) {
        if (questPlayer == null || questPlayer.playerIdentifier().isBlank()) {
            warn("Tried to execute BetonQuestFireEvent action for a missing target player.");
            return;
        }
        if (packageName == null || packageName.isBlank() || actionName == null || actionName.isBlank()) {
            warn("Tried to execute BetonQuestFireEvent action with a missing package or action name.");
            return;
        }
        if (betonQuestCall == null) {
            warn("Tried to execute BetonQuestFireEvent action, but BetonQuest is unavailable.");
            return;
        }
        try {
            betonQuestCall.accept(questPlayer);
        } catch (final RuntimeException exception) {
            warn("Tried to execute BetonQuestFireEvent action, but BetonQuest could not run %s.%s: %s",
                    packageName,
                    actionName,
                    betonQuestFailureDetail(exception));
        }
    }

    public void executeBetonQuestInlineAction(
            final PlatformPlayer questPlayer,
            final String actionInstruction,
            final Consumer<PlatformPlayer> betonQuestCall) {
        if (questPlayer == null || questPlayer.playerIdentifier().isBlank()) {
            warn("Tried to execute BetonQuestFireInlineEvent action for a missing target player.");
            return;
        }
        if (actionInstruction == null || actionInstruction.isBlank()) {
            warn("Tried to execute BetonQuestFireInlineEvent action with an empty instruction.");
            return;
        }
        if (betonQuestCall == null) {
            warn("Tried to execute BetonQuestFireInlineEvent action, but BetonQuest is unavailable.");
            return;
        }
        try {
            betonQuestCall.accept(questPlayer);
        } catch (final RuntimeException exception) {
            warn("Tried to execute BetonQuestFireInlineEvent action, but BetonQuest could not run '%s': %s",
                    actionInstruction,
                    betonQuestFailureDetail(exception));
        }
    }

    public boolean checkBetonQuestConditionVariable(
            final PlatformPlayer questPlayer,
            final String packageName,
            final String conditionName,
            final Predicate<PlatformPlayer> betonQuestCall) {
        if (questPlayer == null || questPlayer.playerIdentifier().isBlank()) {
            warn("Tried to check BetonQuestCondition variable for a missing target player.");
            return false;
        }
        if (packageName == null || packageName.isBlank()
                || conditionName == null || conditionName.isBlank()) {
            warn("Tried to check BetonQuestCondition variable with a missing package or condition name.");
            return false;
        }
        if (betonQuestCall == null) {
            warn("Tried to check BetonQuestCondition variable, but BetonQuest is unavailable.");
            return false;
        }
        try {
            return betonQuestCall.test(questPlayer);
        } catch (final RuntimeException exception) {
            warn("Tried to check BetonQuestCondition variable, but BetonQuest could not test %s.%s: %s",
                    packageName,
                    conditionName,
                    betonQuestFailureDetail(exception));
            return false;
        }
    }

    private static String betonQuestFailureDetail(final Throwable exception) {
        if (exception == null) {
            return "";
        }
        Throwable cause = exception;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        final String message = cause.getMessage();
        return message == null || message.isBlank()
                ? cause.getClass().getSimpleName()
                : message;
    }

    public void betonQuestObjectiveStateChanged(
            final String playerIdentifier,
            final String state,
            final String objectiveIdentifier) {
        addProgressToActiveObjectives(
                playerIdentifier,
                "BetonQuestObjectiveStateChange",
                objective -> objective.text("objectiveState").equalsIgnoreCase(state)
                        && (objective.text("packageName") + "." + objective.text("objectiveName"))
                                .equalsIgnoreCase(objectiveIdentifier),
                1);
    }

    public void eliteMobDied(
            final Collection<EliteMobCredit> credits,
            final String mobName,
            final int mobLevel,
            final double maxHealth,
            final String spawnReason) {
        for (final EliteMobCredit credit : credits == null ? List.<EliteMobCredit>of() : credits) {
            if (credit == null || credit.playerIdentifier() == null || credit.playerIdentifier().isBlank()) {
                continue;
            }
            addProgressToActiveObjectives(
                    credit.playerIdentifier(),
                    "KillEliteMobs",
                    objective -> eliteMobMatches(
                            objective,
                            mobName,
                            mobLevel,
                            credit.damage(),
                            maxHealth,
                            spawnReason),
                    1);
        }
    }

    private static boolean eliteMobMatches(
            final ActiveObjective objective,
            final String mobName,
            final int mobLevel,
            final double playerDamage,
            final double maxHealth,
            final String spawnReason) {
        String configuredName = objective.text("mobname");
        configuredName = configuredName == null || configuredName.equalsIgnoreCase("any")
                ? ""
                : configuredName.replace('_', ' ');
        final String comparedName = mobName == null ? "" : mobName.toLowerCase(Locale.ROOT);
        if (!configuredName.isBlank()) {
            for (final String part : configuredName.toLowerCase(Locale.ROOT).split("\\s+")) {
                if (!comparedName.contains(part)) {
                    return false;
                }
            }
        }
        final int minimumLevel = objective.integer("minimumLevel", -1);
        final int maximumLevel = objective.integer("maximumLevel", -1);
        final int minimumDamagePercentage = objective.integer("minimumDamagePercentage", -1);
        if (minimumLevel >= 0 && mobLevel < minimumLevel
                || maximumLevel >= 0 && mobLevel > maximumLevel
                || minimumDamagePercentage >= 0
                        && maxHealth > 0
                        && playerDamage / maxHealth * 100 < minimumDamagePercentage) {
            return false;
        }
        final String configuredSpawnReason = objective.text("spawnReason");
        return configuredSpawnReason == null
                || configuredSpawnReason.isBlank()
                || configuredSpawnReason.equalsIgnoreCase("any")
                || configuredSpawnReason.equalsIgnoreCase(spawnReason);
    }

    public void mythicMobDied(
            final String playerIdentifier,
            final String internalName,
            final String faction,
            final boolean selfAttributedDeath) {
        if (selfAttributedDeath) {
            return;
        }
        addProgressToActiveObjectives(
                playerIdentifier,
                "KillMobs",
                objective -> mythicMobMatches(objective.text("entityType"), internalName, faction),
                1);
    }

    private static boolean mythicMobMatches(
            final String target,
            final String internalName,
            final String faction) {
        if (target == null) {
            return false;
        }
        if (target.equalsIgnoreCase("any") || target.equals(internalName)) {
            return true;
        }
        if (!target.toLowerCase(Locale.ROOT).startsWith("mmfaction:")) {
            return false;
        }
        final String targetFaction = target.substring("mmfaction:".length());
        return faction == null
                ? targetFaction.equalsIgnoreCase("none")
                : targetFaction.equals(faction);
    }

    public void townAddedToNation(final Collection<String> residentIdentifiers) {
        townyCountChanged(residentIdentifiers, "TownyNationReachTownCount", 1);
    }

    public void townRemovedFromNation(final Collection<String> residentIdentifiers) {
        townyCountChanged(residentIdentifiers, "TownyNationReachTownCount", -1);
    }

    public void townResidentAdded(final Collection<String> residentIdentifiers) {
        townyCountChanged(residentIdentifiers, "TownyReachResidentCount", 1);
    }

    public void townResidentRemoved(final Collection<String> residentIdentifiers) {
        townyCountChanged(residentIdentifiers, "TownyReachResidentCount", -1);
    }

    private void townyCountChanged(
            final Collection<String> residentIdentifiers,
            final String objectiveTypeId,
            final int amount) {
        for (final String playerIdentifier
                : residentIdentifiers == null ? List.<String>of() : residentIdentifiers) {
            if (playerIdentifier == null || playerIdentifier.isBlank()) {
                continue;
            }
            if (amount >= 0) {
                addProgressToActiveObjectives(playerIdentifier, objectiveTypeId, ignored -> true, amount);
            } else {
                removeProgressFromActiveObjectives(
                        playerIdentifier,
                        objectiveTypeId,
                        ignored -> true,
                        Math.abs(amount),
                        true);
            }
        }
    }

    public void nativeNpcIntegrationReloaded(final Runnable registerNativeNpcType) {
        if (registerNativeNpcType != null) {
            registerNativeNpcType.run();
        }
        applyNpcAttachments();
    }

    public String slimefunResearchTaskDescription(final PlatformPlayer questPlayer) {
        return translate(
                questPlayer,
                "chat.objectives.taskDescription.SlimefunResearch.base",
                Map.of(),
                "Research Slimefun items.");
    }

    public void slimefunResearchCompleted(
            final String playerIdentifier,
            final double researchCost) {
        addProgressToActiveObjectives(
                playerIdentifier,
                "SlimefunResearch",
                objective -> true,
                researchCost);
    }

    public String jobsTaskDescription(
            final PlatformPlayer questPlayer,
            final Objectives.Data objective,
            final ActiveObjective activeObjective) {
        return translate(
                questPlayer,
                "chat.objectives.taskDescription.jobsRebornReachJobLevel.base",
                Map.of(
                        "%AMOUNT%",
                        String.valueOf(activeObjective == null
                                ? objective.text("level")
                                : activeObjective.getProgressNeeded()),
                        "%JOB%",
                        objective.text("jobName")),
                "    <GRAY>Reach level <WHITE>%AMOUNT%</WHITE> in <WHITE>%JOB%</WHITE>.");
    }

    public String townyNationTownCountTaskDescription(
            final PlatformPlayer questPlayer,
            final Objectives.Data objective,
            final ActiveObjective activeObjective) {
        return translate(
                questPlayer,
                "chat.objectives.taskDescription.townyNationReachTownCount.base",
                Map.of("%AMOUNT%", String.valueOf(activeObjective == null
                        ? objective.text("amount")
                        : activeObjective.getProgressNeeded())),
                "    <GRAY>Reach <WHITE>%AMOUNT%</WHITE> towns in your nation.");
    }

    public String townyResidentCountTaskDescription(
            final PlatformPlayer questPlayer,
            final Objectives.Data objective,
            final ActiveObjective activeObjective) {
        return translate(
                questPlayer,
                "chat.objectives.taskDescription.townyReachResidentCount.base",
                Map.of("%AMOUNT%", String.valueOf(activeObjective == null
                        ? objective.text("amount")
                        : activeObjective.getProgressNeeded())),
                "    <GRAY>Reach <WHITE>%AMOUNT%</WHITE> residents in your town.");
    }

    public String killEliteMobsTaskDescription(
            final PlatformPlayer questPlayer,
            final Objectives.Data objective) {
        final String mobName = objective.text("mobname");
        String description = mobName.isBlank()
                ? translate(
                        questPlayer,
                        "chat.objectives.taskDescription.killEliteMobs.any",
                        Map.of(),
                        "    <GRAY>Kill any EliteMob.")
                : translate(
                        questPlayer,
                        "chat.objectives.taskDescription.killEliteMobs.base",
                        Map.of("%ELITEMOBNAME%", mobName),
                        "    <GRAY>Kill EliteMob <WHITE>%ELITEMOBNAME%</WHITE>.");
        final Integer minimumLevel = objective.value("minimumLevel", Integer.class);
        final Integer maximumLevel = objective.value("maximumLevel", Integer.class);
        if (minimumLevel != null && minimumLevel != -1) {
            description += maximumLevel != null && maximumLevel != -1
                    ? "\n        <GRAY>Level: <WHITE>" + minimumLevel + "-" + maximumLevel
                    : "\n        <GRAY>Minimum Level: <WHITE>" + minimumLevel;
        } else if (maximumLevel != null && maximumLevel != -1) {
            description += "\n        <GRAY>Maximum Level: <WHITE>" + maximumLevel;
        }
        final String spawnReason = objective.text("spawnReason");
        if (!spawnReason.isBlank()) {
            description += "\n        <GRAY>Spawned from: <WHITE>" + spawnReason;
        }
        final Integer minimumDamage = objective.value("minimumDamagePercentage", Integer.class);
        if (minimumDamage != null && minimumDamage != -1) {
            description += "\n        <GRAY>Inflict minimum damage: <WHITE>" + minimumDamage + "%";
        }
        return description;
    }

    public void townyNationTownCountObjectiveUnlocked(
            final Objectives.Progress objective,
            final boolean loading,
            final int currentTownCount) {
        applyCurrentCountOnUnlock(
                objective,
                loading,
                "doNotCountPreviousTowns",
                currentTownCount);
    }

    public void townyResidentCountObjectiveUnlocked(
            final Objectives.Progress objective,
            final boolean loading,
            final int currentResidentCount) {
        applyCurrentCountOnUnlock(
                objective,
                loading,
                "doNotCountPreviousResidents",
                currentResidentCount);
    }

    private static void applyCurrentCountOnUnlock(
            final Objectives.Progress objective,
            final boolean loading,
            final String doNotCountPreviousFlag,
            final int currentCount) {
        if (!loading
                && objective != null
                && objective.currentProgress() == 0
                && !objective.flag(doNotCountPreviousFlag)
                && currentCount > 0) {
            objective.addProgress(currentCount);
        }
    }

    public boolean spawnMobs(
            final String mobName,
            final int amount,
            final Runnable spawnOneMob) {
        if (spawnOneMob == null) {
            warn("Tried to spawn <highlight>" + (mobName == null ? "" : mobName)
                    + "</highlight>, but that mob type is unavailable.");
            return false;
        }
        try {
            for (int index = 0; index < amount; index++) {
                spawnOneMob.run();
            }
            return true;
        } catch (final RuntimeException exception) {
            warn("Could not spawn <highlight>" + (mobName == null ? "" : mobName)
                    + "</highlight>: " + exception.getMessage());
            return false;
        }
    }

    public void enableJobsLevelSync(
            final ToDoubleBiFunction<String, String> currentLevel) {
        jobsLevelReader = currentLevel;
    }

    public void jobsObjectiveUnlocked(
            final Objectives.Progress objective,
            final boolean loading,
            final double currentLevel) {
        if (loading || objective == null) {
            return;
        }
        if (!objective.flag("doNotCountPreviousLevels")) {
            if (Double.isFinite(currentLevel)) {
                objective.setProgress(currentLevel, true);
            }
        } else if (objective.currentProgress() == 0) {
            objective.addProgress(1);
        }
    }

    public void jobsObjectiveLoaded(
            final Objectives.Data objective,
            final boolean jobExists) {
        if (!jobExists && objective != null) {
            warn("The job <highlight>" + objective.text("jobName") + "</highlight> does not exist.");
        }
    }

    private void synchronizeJobsLevels() {
        final ToDoubleBiFunction<String, String> currentLevel = jobsLevelReader;
        if (currentLevel == null || ++jobsLevelSyncSeconds < 3) {
            return;
        }
        jobsLevelSyncSeconds = 0;
        for (final PlatformPlayer player : activePlatformPlayers()) {
            if (player == null) {
                continue;
            }
            final String playerIdentifier = player.playerIdentifier();
            setProgressOfActiveObjectives(
                    playerIdentifier,
                    "JobsRebornReachJobLevel",
                    objective -> !objective.flag("doNotCountPreviousLevels"),
                    objective -> currentLevel.applyAsDouble(
                            playerIdentifier,
                            objective.text("jobName")),
                    true);
        }
    }

    public void jobsLevelChanged(
            final String playerIdentifier,
            final String jobName,
            final double currentLevel) {
        setProgressOfActiveObjectives(
                playerIdentifier,
                "JobsRebornReachJobLevel",
                objective -> !objective.flag("doNotCountPreviousLevels")
                        && objective.text("jobName").equalsIgnoreCase(jobName),
                currentLevel,
                true);
        addProgressToActiveObjectives(
                playerIdentifier,
                "JobsRebornReachJobLevel",
                objective -> objective.flag("doNotCountPreviousLevels")
                        && objective.text("jobName").equalsIgnoreCase(jobName),
                1);
    }

    public ActiveObjective activateManualObjective(
            final PlatformPlayer questPlayer,
            final Objectives.Type objectiveType,
            final Objectives.Data data) {
        if (questPlayer == null || objectiveType == null) {
            return null;
        }
        final Objective entry = getOrCreateQuest("__manual_objectives__")
                .addObjective(objectiveType.id(), data, "");
        return questPlayerManager.getActiveObjectives().activateObjective(
                questPlayer,
                "__manual_objectives__",
                entry,
                "",
                objectiveType);
    }

    public void playerHarvestedBlock(
            final PlatformPlayer questPlayer,
            final Objectives.HarvestBlockEvent event) {
        questPlayerManager.getActiveObjectives().onPlayerHarvestBlock(questPlayer, event);
    }

    public void trackPlayerPlacedHarvestBlock(
            final String blockKey,
            final String materialId,
            final boolean fullyGrown) {
        questPlayerManager.getActiveObjectives().onBlockPlaced(null, blockKey, materialId, fullyGrown);
    }

    public boolean isPlayerPlacedHarvestBlock(final String blockKey) {
        return questPlayerManager.getActiveObjectives().isPlayerPlacedHarvestBlock(blockKey);
    }

    public void clearPlayerPlacedHarvestBlock(final String blockKey) {
        questPlayerManager.getActiveObjectives().blockBreakFinished(blockKey, false);
    }

    public void playerBrokeBlock(
            final PlatformPlayer questPlayer,
            final String blockKey,
            final String materialId,
            final boolean samePlantBelow,
            final boolean ageableAtMaxAge,
            final boolean finalEvent) {
        questPlayerManager.getActiveObjectives().onBlockBroken(
                questPlayer,
                blockKey,
                materialId,
                ActiveObjectives.isFullyGrownHarvestable(
                        materialId, samePlantBelow, ageableAtMaxAge),
                finalEvent);
    }

    public void blockBreakFinished(final String blockKey, final boolean brewingStand) {
        questPlayerManager.getActiveObjectives().blockBreakFinished(blockKey, brewingStand);
    }

    public void playerBrokeBlock(
            final PlatformPlayer questPlayer,
            final Objectives.BlockEvent event) {
        questPlayerManager.getActiveObjectives().onPlayerBreakBlock(questPlayer, event);
    }

    public void playerPlacedBlock(
            final PlatformPlayer questPlayer,
            final Objectives.BlockEvent event) {
        questPlayerManager.getActiveObjectives().onPlayerPlaceBlock(questPlayer, event);
    }

    public void playerPlacedBlock(
            final PlatformPlayer questPlayer,
            final String blockKey,
            final String materialId,
            final boolean ageableAtMaxAge) {
        questPlayerManager.getActiveObjectives().onBlockPlaced(
                questPlayer,
                blockKey,
                materialId,
                ageableAtMaxAge);
    }

    public void playerPickedUpItem(
            final PlatformPlayer questPlayer,
            final Objectives.ItemEvent event) {
        questPlayerManager.getActiveObjectives().onPlayerPickupItem(questPlayer, event);
    }

    public void playerDroppedItem(
            final PlatformPlayer questPlayer,
            final Objectives.ItemEvent event) {
        questPlayerManager.getActiveObjectives().onPlayerDropItem(questPlayer, event);
    }

    public void playerTookBrewedItem(
            final PlatformPlayer questPlayer,
            final Objectives.ItemEvent event) {
        questPlayerManager.getActiveObjectives().onPlayerTakeBrewedItem(questPlayer, event);
    }

    public void brewingFinished(
            final String brewingStandKey,
            final List<ActiveObjectives.BrewedItem> brewedItems) {
        questPlayerManager.getActiveObjectives().brewingFinished(brewingStandKey, brewedItems);
    }

    public void playerTookInventoryItem(
            final PlatformPlayer questPlayer,
            final ActiveObjectives.TakenItem takenItem,
            final String brewingStandKey,
            final String itemKey,
            final Objectives.ItemEvent event) {
        questPlayerManager.getActiveObjectives().onPlayerTakeInventoryItem(
                questPlayer,
                takenItem,
                brewingStandKey,
                itemKey,
                event);
    }

    public void playerConsumedItem(
            final PlatformPlayer questPlayer,
            final Objectives.ItemEvent event) {
        questPlayerManager.getActiveObjectives().onPlayerConsumeItem(questPlayer, event);
    }

    public void playerConsumedItem(
            final PlatformPlayer questPlayer,
            final Objectives.ItemEvent event,
            final Runnable cancel) {
        if (hasActiveConversation(questPlayer)) {
            if (cancel != null) {
                cancel.run();
            }
            return;
        }
        playerConsumedItem(questPlayer, event);
    }

    public void playerStartedConsumingItem(
            final PlatformPlayer questPlayer,
            final Runnable cancel) {
        if (hasActiveConversation(questPlayer) && cancel != null) {
            cancel.run();
        }
    }

    public void playerFishedItem(
            final PlatformPlayer questPlayer,
            final Objectives.ItemEvent event) {
        questPlayerManager.getActiveObjectives().onPlayerFishItem(questPlayer, event);
    }

    public void playerCraftedItem(
            final PlatformPlayer questPlayer,
            final Objectives.ItemEvent event) {
        playerCraftedItem(questPlayer, event, true);
    }

    public void playerCraftedItem(
            final PlatformPlayer questPlayer,
            final Objectives.ItemEvent event,
            final boolean cursorEmpty) {
        if (!cursorEmpty) {
            sendDebugMessage(questPlayer, "Inventory craft event: Cursor is not empty");
        }
        questPlayerManager.getActiveObjectives().onPlayerCraftItem(questPlayer, event);
    }

    public void playerTookSmeltedItem(
            final PlatformPlayer questPlayer,
            final Objectives.ItemEvent event) {
        questPlayerManager.getActiveObjectives().onPlayerTakeSmeltedItem(questPlayer, event);
    }

    public void playerTookSmithingResult(
            final PlatformPlayer questPlayer,
            final Objectives.ItemEvent event) {
        questPlayerManager.getActiveObjectives().onPlayerTakeSmithingResult(questPlayer, event);
    }

    public void playerTradedItem(
            final PlatformPlayer questPlayer,
            final Objectives.ItemEvent event) {
        questPlayerManager.getActiveObjectives().onPlayerTradeItem(questPlayer, event);
    }

    public void playerShearedSheep(
            final PlatformPlayer questPlayer,
            final Objectives.ShearSheepEvent event) {
        questPlayerManager.getActiveObjectives().onPlayerShearSheep(questPlayer, event);
    }

    public void playerMilkedCow(
            final PlatformPlayer questPlayer,
            final Objectives.MilkCowEvent event) {
        questPlayerManager.getActiveObjectives().onPlayerMilkCow(questPlayer, event);
    }

    public void playerOpenedBuriedTreasure(final PlatformPlayer questPlayer) {
        questPlayerManager.getActiveObjectives().onPlayerOpenBuriedTreasure(questPlayer);
    }

    public void playerKilledEntity(
            final PlatformPlayer questPlayer,
            final Objectives.EntityEvent event) {
        questPlayerManager.getActiveObjectives().onPlayerKillEntity(questPlayer, event);
    }

    public void playerBredEntity(
            final PlatformPlayer questPlayer,
            final Objectives.EntityEvent event) {
        questPlayerManager.getActiveObjectives().onPlayerBreedEntity(questPlayer, event);
    }

    public void playerFedEntity(
            final PlatformPlayer questPlayer,
            final Objectives.EntityEvent event) {
        questPlayerManager.getActiveObjectives().onPlayerFeedEntity(questPlayer, event);
    }

    public void playerTamedEntity(
            final PlatformPlayer questPlayer,
            final Objectives.EntityEvent event) {
        questPlayerManager.getActiveObjectives().onPlayerTameEntity(questPlayer, event);
    }

    public void playerInteractedWithBlock(
            final PlatformPlayer questPlayer,
            final Objectives.InteractionEvent event) {
        questPlayerManager.getActiveObjectives().onPlayerInteractBlock(questPlayer, event);
    }

    public void playerInteractedWithBlock(
            final PlatformPlayer questPlayer,
            final Objectives.InteractionEvent event,
            final boolean unopenedBuriedTreasure) {
        if (questPlayer == null || !hasAnyActiveQuests(questPlayer)) {
            return;
        }
        if (hasActiveConversation(questPlayer)) {
            if (event != null) {
                event.cancel();
            }
            return;
        }
        if (event != null) {
            playerInteractedWithBlock(questPlayer, event);
        }
        if (unopenedBuriedTreasure) {
            playerOpenedBuriedTreasure(questPlayer);
        }
    }

    public void playerInteractedWithEntity(
            final PlatformPlayer questPlayer,
            final boolean npcHandled,
            final Objectives.EntityEvent fedEntity,
            final Objectives.MilkCowEvent milkCow,
            final Objectives.ShearSheepEvent shearSheep) {
        if (questPlayer == null || npcHandled) {
            return;
        }
        if (fedEntity != null) {
            playerFedEntity(questPlayer, fedEntity);
        }
        if (milkCow != null) {
            playerMilkedCow(questPlayer, milkCow);
        } else if (shearSheep != null) {
            playerShearedSheep(questPlayer, shearSheep);
        }
    }

    public void entityDied(
            final PlatformPlayer deadPlayer,
            final Objectives.DeathEvent death,
            final PlatformPlayer killer,
            final Objectives.EntityEvent killedEntity) {
        if (deadPlayer != null) {
            playerDied(deadPlayer);
            if (death != null) {
                playerDied(deadPlayer, death);
            }
        }
        if (killer != null && killedEntity != null) {
            playerKilledEntity(killer, killedEntity);
        }
    }

    public void playerMoved(
            final PlatformPlayer questPlayer,
            final Objectives.MoveEvent event) {
        questPlayerManager.getActiveObjectives().onPlayerMove(questPlayer, event);
    }

    public void playerTick(
            final PlatformPlayer questPlayer,
            final NQLocation location,
            final boolean sneaking) {
        questPlayerManager.getActiveObjectives().playerTick(questPlayer, location, sneaking);
    }

    public void playerProjectileHit(
            final PlatformPlayer questPlayer,
            final Objectives.ProjectileHitEvent event) {
        questPlayerManager.getActiveObjectives().onPlayerShootProjectileHit(questPlayer, event);
    }

    public void playerJumped(final PlatformPlayer questPlayer) {
        questPlayerManager.getActiveObjectives().onPlayerJump(questPlayer);
    }

    public void playerStartedSneaking(final PlatformPlayer questPlayer) {
        questPlayerManager.getActiveObjectives().onPlayerStartSneak(questPlayer);
    }

    public void playerDied(
            final PlatformPlayer questPlayer,
            final Objectives.DeathEvent event) {
        questPlayerManager.getActiveObjectives().onPlayerDeath(questPlayer, event);
    }

    public void playerEnchantedItem(
            final PlatformPlayer questPlayer,
            final Objectives.EnchantEvent event) {
        questPlayerManager.getActiveObjectives().onPlayerEnchantItem(questPlayer, event);
    }

    public void playerRanCommand(
            final PlatformPlayer questPlayer,
            final Objectives.CommandEvent event) {
        questPlayerManager.getActiveObjectives().onPlayerRunCommand(questPlayer, event);
    }

    public void playerRanCommand(
            final PlatformPlayer questPlayer,
            final String command,
            final Runnable cancel) {
        final String normalized = command == null || command.isBlank()
                ? ""
                : command.startsWith("/") ? command : "/" + command;
        playerRanCommand(questPlayer, new RunCommandEvent(normalized, cancel));
    }

    public boolean playerInteractedWithNpc(
            final PlatformPlayer questPlayer,
            final Objectives.NpcInteractionEvent event) {
        return questPlayerManager.getActiveObjectives().onPlayerInteractNpc(questPlayer, event);
    }

    public boolean restoreObjectiveProgress(
            final String playerIdentifier,
            final String questName,
            final int objectiveId,
            final double progress,
            final boolean completed) {
        return questPlayerManager.getActiveObjectives().restoreObjectiveProgress(
                playerIdentifier,
                questName,
                objectiveId,
                progress,
                completed);
    }

    public boolean restoreObjectiveProgress(
            final String playerIdentifier,
            final String questName,
            final int[] objectivePath,
            final double progress,
            final boolean completed) {
        return questPlayerManager.getActiveObjectives().restoreObjectiveProgress(
                playerIdentifier,
                questName,
                objectivePath,
                progress,
                completed);
    }

    public ActiveObjective activeObjectiveProgress(
            final PlatformPlayer questPlayer,
            final String questName,
            final int objectiveId) {
        if (questPlayer == null) {
            return null;
        }
        return questPlayerManager.getActiveObjectives().activeObjective(questPlayer.playerIdentifier(), questName, objectiveId);
    }

    public ActiveObjective activeObjectiveProgress(
            final PlatformPlayer questPlayer,
            final String questName,
            final int[] objectivePath) {
        if (questPlayer == null) {
            return null;
        }
        return questPlayerManager.getActiveObjectives().activeObjective(questPlayer.playerIdentifier(), questName, objectivePath);
    }

    public boolean activeObjectiveReadyToComplete(
            final PlatformPlayer questPlayer,
            final String questName,
            final int[] objectivePath,
            final String completionNpcSelector) {
        final ActiveObjective progress = activeObjectiveProgress(questPlayer, questName, objectivePath);
        return progress != null && progress.readyToComplete(completionNpcSelector);
    }

    public boolean showActiveObjectiveProgress(
            final PlatformPlayer questPlayer,
            final String questName,
            final int[] objectivePath) {
        final ActiveObjective progress = activeObjectiveProgress(questPlayer, questName, objectivePath);
        if (progress == null) {
            return false;
        }
        sendObjectiveProgressUpdate(progress, progress.currentProgress(), progress.currentProgress());
        return true;
    }

    public ObjectiveCompass.Display objectiveLocationCompassDisplay(
            final PlatformPlayer questPlayer,
            final NQLocation target,
            final String objectiveLabel) {
        if (!configuration.objectiveTrackingLocationCompassEnabled()
                || questPlayer == null
                || !questPlayer.hasPlayer()
                || target == null
                || target.worldName() == null
                || target.worldName().isBlank()) {
            return null;
        }
        final String targetWorld = target.worldName();
        if (!sameWorld(targetWorld, questPlayer.worldName())
                && !sameWorld(targetWorld, questPlayer.worldIdentifier())) {
            return ObjectiveCompass.differentWorld(targetWorld);
        }
        final double distance = questPlayer.distanceTo(target);
        if (!Double.isFinite(distance)) {
            return ObjectiveCompass.differentWorld(targetWorld);
        }
        return ObjectiveCompass.display(
                questPlayer.positionX(),
                questPlayer.positionZ(),
                (float) questPlayer.yawDegrees(),
                target.x(),
                target.z(),
                distance,
                objectiveLabel);
    }

    public ObjectiveCompass.Display objectiveLocationCompassDisplay(
            final PlatformPlayer questPlayer,
            final Map<String, NQLocation> markers) {
        if (markers == null || markers.isEmpty()) {
            return null;
        }
        for (final Map.Entry<String, NQLocation> marker : markers.entrySet()) {
            if (marker.getValue() != null) {
                return objectiveLocationCompassDisplay(
                        questPlayer,
                        marker.getValue(),
                        objectiveBeamLabel(marker.getKey()));
            }
        }
        return null;
    }

    public boolean objectiveBeamUsesBeaconBlocks() {
        return "beacon".equalsIgnoreCase(configuration.objectiveTrackingBeamMode());
    }

    public boolean showObjectiveMarker(
            final PlatformPlayer questPlayer,
            final String name,
            final NQLocation location) {
        return questPlayerManager.getActiveObjectives().showObjectiveMarker(
                questPlayer, name, location, objectiveBeamUsesBeaconBlocks());
    }

    public boolean showTemporaryObjectiveMarker(
            final PlatformPlayer questPlayer,
            final String name,
            final NQLocation location,
            final Duration duration) {
        return questPlayerManager.getActiveObjectives().showTemporaryObjectiveMarker(
                questPlayer,
                name,
                location,
                duration,
                actionScheduler,
                objectiveBeamUsesBeaconBlocks());
    }

    public boolean removeObjectiveMarker(
            final PlatformPlayer questPlayer,
            final String name) {
        return questPlayerManager.getActiveObjectives().removeObjectiveMarker(
                questPlayer, name, objectiveBeamUsesBeaconBlocks());
    }

    private void clearObjectiveMarkers(final PlatformPlayer questPlayer) {
        questPlayerManager.getActiveObjectives().clearObjectiveMarkers(
                questPlayer, objectiveBeamUsesBeaconBlocks());
    }

    private void refreshObjectiveMarkers(
            final PlatformPlayer questPlayer,
            final boolean force) {
        questPlayerManager.getActiveObjectives().refreshObjectiveMarkers(
                questPlayer, objectiveBeamUsesBeaconBlocks(), force);
    }

    public static String objectiveBeamLabel(final String beamName) {
        if (beamName == null || !beamName.startsWith("objective:")) {
            return beamName == null || beamName.isBlank() ? "Objective Marker" : beamName;
        }
        final String[] parts = beamName.split(":", 4);
        return parts.length == 4 && !parts[3].isBlank() ? parts[3] : "Objective Marker";
    }

    public boolean trackActiveObjective(
            final PlatformPlayer questPlayer,
            final String questName,
            final int[] objectivePath) {
        final ActiveObjective progress = activeObjectiveProgress(questPlayer, questName, objectivePath);
        if (progress == null || questPlayer == null) {
            return false;
        }
        sendObjectiveProgressUpdate(progress, progress.currentProgress(), progress.currentProgress());
        final Objective entry = progress.getObjective();
        if (entry != null && entry.isLocationEnabled() && entry.getLocation() != null) {
            showObjectiveMarker(questPlayer, activeObjectiveBeamName(progress), entry.getLocation());
        }
        return true;
    }

    public boolean untrackActiveObjective(
            final PlatformPlayer questPlayer,
            final String questName,
            final int[] objectivePath) {
        final ActiveObjective progress = activeObjectiveProgress(questPlayer, questName, objectivePath);
        if (progress == null || questPlayer == null) {
            return false;
        }
        removeObjectiveMarker(questPlayer, activeObjectiveBeamName(progress));
        questPlayer.hideProgressBossBar();
        return true;
    }

    private String activeObjectiveBeamName(final ActiveObjective progress) {
        if (progress == null) {
            return "";
        }
        final String name = activeObjectiveName(progress);
        return "objective:"
                + progress.getQuestIdentifier()
                + ":"
                + progress.getObjectivePathKey()
                + ":"
                + (name.isBlank() ? String.valueOf(progress.getObjectiveID()) : name);
    }

    private static boolean sameWorld(final String first, final String second) {
        return first != null && second != null && first.equalsIgnoreCase(second);
    }

    public List<String> activeObjectiveDisplayLines(
            final PlatformPlayer questPlayer,
            final String questName,
            final int[] objectivePath,
            final int level) {
        final ActiveObjective progress = activeObjectiveProgress(questPlayer, questName, objectivePath);
        if (progress == null) {
            return List.of();
        }
        return activeObjectiveDisplayLines(questPlayer, progress, level);
    }

    public String configuredObjectiveTaskDescriptionLine(
            final PlatformPlayer questPlayer,
            final String questName,
            final int[] objectivePath,
            final boolean completed) {
        final Objective entry = configuredObjective(questName, objectivePath);
        final Objectives.Type type = entry == null ? null : objectiveType(entry.typeId());
        if (entry == null || type == null) {
            return "";
        }
        return objectiveTaskDescription(
                questPlayer,
                entry,
                type,
                ActiveObjective.configured(progressNeeded(entry)),
                completed);
    }

    public List<String> objectiveAdminLines(
            final PlatformPlayer questPlayer,
            final String questName,
            final int[] holderObjectivePath) {
        final List<Objective> objectives = childObjectivesForAdmin(questName, holderObjectivePath);
        final ArrayList<String> result = new ArrayList<>();
        for (final Objective objective : objectives) {
            final Objectives.Type type = objectiveType(objective.typeId());
            final String displayName = type == null ? objective.typeId() : objectiveDisplayName(objective, type);
            final String description = objective.getDescription() == null ? "" : objective.getDescription();
            result.add("<highlight>" + objective.id() + ".</highlight> <main>" + displayName);
            if (!description.isBlank()) {
                result.add("   <highlight>Description:</highlight> <main>" + description);
            }
            addAdminConditionGroup(result, "Unlock", "unlock",
                    objective.getConditions("unlock"), questPlayer);
            addAdminConditionGroup(result, "Progress", "progress",
                    objective.getConditions("progress"), questPlayer);
            addAdminConditionGroup(result, "Complete", "complete",
                    objective.getConditions("complete"), questPlayer);
            result.add(configuredObjectiveTaskDescriptionLine(
                    questPlayer, questName,
                    appendObjectivePath(holderObjectivePath, objective.id()), false));
        }
        return List.copyOf(result);
    }

    private List<Objective> childObjectivesForAdmin(
            final String questName,
            final int[] holderObjectivePath) {
        final Quest quest = quest(questName);
        if (quest == null) {
            return List.of();
        }
        if (holderObjectivePath == null || holderObjectivePath.length == 0) {
            return quest.getObjectives();
        }
        final Objective holder = configuredObjective(questName, holderObjectivePath);
        return holder == null ? List.of() : holder.getObjectives();
    }

    private void addAdminConditionGroup(
            final List<String> lines,
            final String title,
            final String missingName,
            final List<Condition> conditions,
            final PlatformPlayer questPlayer) {
        lines.add("   <highlight>" + title + " Conditions:");
        if (conditions == null || conditions.isEmpty()) {
            lines.add("      <unimportant>No " + missingName + " conditions found!");
            return;
        }
        for (final Condition condition : conditions) {
            lines.add("         <highlight>" + condition.id()
                    + ".</highlight> <main>Condition:</main> <highlight2>"
                    + conditionDescription(condition, conditionType(condition.typeId()), questPlayer));
        }
    }

    private static int[] appendObjectivePath(final int[] parentPath, final int objectiveId) {
        if (parentPath == null || parentPath.length == 0) {
            return new int[] {objectiveId};
        }
        final int[] path = Arrays.copyOf(parentPath, parentPath.length + 1);
        path[path.length - 1] = objectiveId;
        return path;
    }

    public List<String> completedObjectiveDisplayLines(
            final PlatformPlayer questPlayer,
            final String questName,
            final int[] objectivePath,
            final double currentProgress,
            final double recordedProgressNeeded) {
        final Objective entry = configuredObjective(questName, objectivePath);
        if (entry == null) {
            return List.of();
        }
        final Objectives.Type type = objectiveType(entry.typeId());
        final double progressNeeded = recordedProgressNeeded > 0 ? recordedProgressNeeded : progressNeeded(entry);
        final double displayedProgress = currentProgress > 0 ? currentProgress : progressNeeded;
        final int id = objectiveId(objectivePath);
        final String displayName = type == null ? entry.typeId() : objectiveDisplayName(entry, type);
        final String description = entry.getDescription();
        final String taskText = objectiveTaskDescription(
                questPlayer, entry, type, ActiveObjective.configured(progressNeeded), true);
        return List.of(
                "<strikethrough><GRAY>" + id + ". " + displayName + ":</strikethrough>",
                "    <strikethrough><GRAY>Description: <WHITE>" + (description == null ? "" : description) + "</strikethrough>",
                taskText == null ? "" : taskText,
                "   <strikethrough><GRAY>Progress: <WHITE>"
                        + displayedProgress + " / " + progressNeeded + "</strikethrough>");
    }

    private static int objectiveId(final int[] objectivePath) {
        return objectivePath == null || objectivePath.length == 0 ? 0 : objectivePath[objectivePath.length - 1];
    }

    private List<String> activeObjectiveDisplayLines(
            final PlatformPlayer questPlayer,
            final ActiveObjective progress,
            final int level) {
        final Objective entry = progress.getObjective();
        final Objectives.Type type = objectiveType(progress.getObjectiveTypeID());
        if (entry == null || type == null) {
            return List.of();
        }
        final String prefixAppend = translateFor(
                questPlayer,
                "chat.objectives.subObjectivePrefixAppend",
                Map.of(),
                "    ");
        final boolean objectiveGroup = Kinds.isObjectiveGroup(type.id());
        final String counterWithSubId = objectivePath(progress.getObjectivePath());
        final String objectiveName = objectiveDisplayName(entry, type);
        final String counterText = objectiveGroup
                ? translateFor(
                        questPlayer,
                        "chat.objectives.counterForObjectiveObjective",
                        Map.of(
                                "%OBJECTIVEIDWITHSUBID%", counterWithSubId,
                                "%OBJECTIVEHOLDERNAME%", Kinds.objectiveGroupHolderName(type.id(), progress::text)),
                        "<highlight>" + counterWithSubId + ".</highlight> <main>"
                                + Kinds.objectiveGroupHolderName(type.id(), progress::text) + ":")
                : translateFor(
                        questPlayer,
                        "chat.objectives.counter",
                        Map.of(
                                "%OBJECTIVEIDWITHSUBID%", counterWithSubId,
                                "%OBJECTIVENAME%", objectiveName),
                        "<highlight>" + counterWithSubId + ".</highlight> <main>" + objectiveName + ":");
        final String prefix = (prefixAppend == null ? "" : prefixAppend).repeat(Math.max(0, level));
        final String descriptionSource = entry.getDescription();
        if (!progress.isUnlocked()) {
            final String hiddenText = translateFor(
                    questPlayer,
                    "chat.objectives.hidden",
                    Map.of("%OBJECTIVEID%", counterWithSubId),
                    "<highlight>" + counterWithSubId + ".</highlight> <unimportant><BOLD>[HIDDEN]");
            return List.of(prefix + hiddenText);
        }
        final ArrayList<String> lines = new ArrayList<>();
        lines.add(prefix + counterText);
        if (descriptionSource != null && !descriptionSource.isBlank()) {
            lines.add(prefix + translateFor(
                    questPlayer,
                    "chat.objectives.description",
                    Map.of("%OBJECTIVEDESCRIPTION%", descriptionSource),
                    "    <veryUnimportant>└─ <unimportant>Description: <main>" + descriptionSource));
        }
        if (!objectiveGroup) {
            lines.add(prefix + activeObjectiveTaskDescription(questPlayer, progress, type));
            lines.add(prefix + translateFor(
                    questPlayer,
                    "chat.objectives.progress",
                    Map.of(
                            "%ACTIVEOBJECTIVEPROGRESS%", formatProgress(progress.getCurrentProgress()),
                            "%OBJECTIVEPROGRESSNEEDED%", formatProgress(progress.getProgressNeeded())),
                    "    <veryUnimportant>└─ <unimportant>Progress: <main>"
                            + formatProgress(progress.getCurrentProgress()) + " / "
                            + formatProgress(progress.getProgressNeeded())));
        }
        return List.copyOf(lines);
    }

    public ActiveObjective.Update addActiveObjectiveProgress(
            final PlatformPlayer questPlayer,
            final String questName,
            final int[] objectivePath,
            final double progressToAdd,
            final String objectiveName,
            final String holderName) {
        return addActiveObjectiveProgress(
                questPlayer,
                questName,
                objectivePath,
                progressToAdd,
                objectiveName,
                holderName,
                false,
                "",
                false);
    }

    public ActiveObjective.Update addActiveObjectiveProgress(
            final PlatformPlayer questPlayer,
            final String questName,
            final int[] objectivePath,
            final double progressToAdd,
            final String objectiveName,
            final String holderName,
            final boolean silent,
            final String completionNpcSelector,
            final boolean suppressCompletionEffects) {
        final ActiveObjective progress = activeObjectiveProgress(questPlayer, questName, objectivePath);
        if (progress == null) {
            return ActiveObjective.Update.missing();
        }
        final boolean wasComplete = progress.hasBeenCompleted();
        progress.setCompletionOptions(silent, suppressCompletionEffects);
        progress.addProgress(progressToAdd);
        if (completionNpcSelector != null && !completionNpcSelector.isBlank()) {
            progress.completeWithNpc(completionNpcSelector);
        }
        progress.setCompletionOptions(false, false);
        final boolean callerHandlesCompletionEffects =
                silent || suppressCompletionEffects || (completionNpcSelector != null && !completionNpcSelector.isBlank());
        final List<String> debugMessages = new ArrayList<>();
        if (!wasComplete && progress.hasBeenCompleted()) {
            debugMessages.add(callerHandlesCompletionEffects
                    ? "Objective completed: " + debugHL(objectiveName) + " of quest " + debugHL(holderName)
                            + ". Silent: " + silent
                    : "Objective completed through core progress: "
                            + debugHL(objectiveName) + " of quest " + debugHL(holderName) + ".");
        }
        final String progressMessage =
                callerHandlesCompletionEffects
                        ? "+" + progressToAdd + " progress for objective " + debugHL(objectiveName)
                                + " of quest " + debugHL(holderName) + ". Silent: " + silent
                        : "+" + progressToAdd + " core progress for objective " + debugHL(objectiveName)
                                + " of quest " + debugHL(holderName) + ".";
        debugMessages.add(progressMessage);
        return ActiveObjective.Update.applied(
                progress.getCurrentProgress(),
                progress.isUnlocked(),
                progress.hasBeenCompleted(),
                debugMessages,
                configuration().debugEnabled() ? List.of(progressMessage) : List.of());
    }

    public ActiveObjective.Update removeActiveObjectiveProgress(
            final PlatformPlayer questPlayer,
            final String questName,
            final int[] objectivePath,
            final double progressToRemove,
            final boolean capAtZero,
            final String objectiveName,
            final String holderName) {
        if (progressToRemove < 0) {
            return ActiveObjective.Update.severe(
                    "Tried to remove negative progress (=> add progress) from objective "
                            + objectiveName + " of quest " + holderName + "!");
        }
        final ActiveObjective progress = activeObjectiveProgress(questPlayer, questName, objectivePath);
        if (progress == null) {
            return ActiveObjective.Update.missing();
        }
        progress.removeProgress(progressToRemove, capAtZero);
        final String progressMessage =
                "-" + progressToRemove + " core progress for objective " + debugHL(objectiveName)
                        + " of quest " + debugHL(holderName) + ".";
        return ActiveObjective.Update.applied(
                progress.getCurrentProgress(),
                progress.isUnlocked(),
                progress.hasBeenCompleted(),
                List.of(progressMessage),
                List.of());
    }

    private static void annotateObjectiveConditionContext(
            final Quest quest,
            final Objective objective) {
        for (final String group : List.of("unlock", "progress", "complete")) {
            for (final Condition condition : objective.getConditions(group)) {
                condition.data().setValue("__questName", quest.getIdentifier());
                condition.data().setValue("__objectiveId", objective.id());
            }
        }
    }

    private boolean objectiveConditionsFulfilled(
            final List<Condition> conditions,
            final PlatformPlayer questPlayer,
            final boolean progressDecrease) {
        for (final Condition condition : conditions) {
            if (progressDecrease
                    && Boolean.TRUE.equals(condition.data().value("allowProgressDecreaseIfNotFulfilled"))) {
                continue;
            }
            final Conditions.Type conditionType = conditionType(condition.typeId());
            if (conditionType == null || conditionType.checker() == null) {
                return false;
            }
            final String result = ConditionCheck.check(conditionType, condition.data(), questPlayer);
            if (result != null && !result.isBlank()) {
                return false;
            }
        }
        return true;
    }

    private void sendObjectiveProgressUpdate(
            final ActiveObjective objective,
            final double oldProgress,
            final double newProgress) {
        final Objectives.Type type = objectiveType(objective.getObjectiveTypeID());
        final Quest quest = quest(objective.getQuestIdentifier());
        final String objectiveName = type == null
                ? objective.getObjectiveTypeID()
                : objectiveDisplayName(objective.getObjective(), type);
        final double percentage = objective.getProgressNeeded() <= 0
                ? 100
                : Math.min(100, Math.max(0, (newProgress / objective.getProgressNeeded()) * 100));
        final String text = objective.getProgressNeeded() == 1
                ? "<positive>" + questDisplayName(objective.getQuestIdentifier(), quest)
                        + "</positive> <unimportant>- <main>" + objectiveName
                        + "</main> <unimportant>(<highlight>" + formatProgress(percentage) + "%<unimportant>)"
                : "<positive>" + questDisplayName(objective.getQuestIdentifier(), quest)
                        + "</positive> <unimportant>- <main>" + objectiveName
                        + "</main> <unimportant>(<positive>" + formatProgress(newProgress)
                        + " <unimportant>/ <main>" + formatProgress(objective.getProgressNeeded())
                        + " <unimportant>- <highlight>" + formatProgress(percentage) + "%<unimportant>)";
        final Map<String, String> replacements = Map.of(
                "%QUESTNAME%", questDisplayName(objective.getQuestIdentifier(), quest),
                "%OBJECTIVENAME%", objectiveName,
                "%ACTIVEOBJECTIVEPROGRESS%", formatProgress(newProgress),
                "%OBJECTIVEPROGRESSNEEDED%", formatProgress(objective.getProgressNeeded()),
                "%OBJECTIVEPROGRESSPERCENTAGE%", formatProgress(percentage));
        if (configuration.objectiveTrackingActionbarEnabled()) {
            objective.getQuestPlayer().sendActionBar(translate(
                    objective.getQuestPlayer(),
                    ActiveObjective.actionBarTranslationKey(objective.getProgressNeeded()),
                    replacements,
                    text));
        }
        if (configuration.objectiveTrackingBossbarEnabled()) {
            final float progress = ActiveObjective.clampedProgress(newProgress, objective.getProgressNeeded());
            if (ActiveObjective.shouldHideCompletedBossBar(
                    progress,
                    configuration.objectiveTrackingBossbarShowCompleted())) {
                objective.getQuestPlayer().hideProgressBossBar();
                progressBossBarAges.remove(objective.getQuestPlayer().playerIdentifier());
            } else {
                objective.getQuestPlayer().showProgressBossBar(translate(
                        objective.getQuestPlayer(),
                        ActiveObjective.bossBarTranslationKey(objective.getProgressNeeded()),
                        replacements,
                        text), progress);
                progressBossBarAges.put(objective.getQuestPlayer().playerIdentifier(), 0);
            }
        }
    }

    private String activeObjectiveName(final ActiveObjective objective) {
        if (objective == null) {
            return "";
        }
        final Objectives.Type type = objectiveType(objective.getObjectiveTypeID());
        return type == null ? objective.getObjectiveTypeID() : objectiveDisplayName(objective.getObjective(), type);
    }

    private String activeObjectiveHolderName(final ActiveObjective objective) {
        if (objective == null) {
            return "";
        }
        final int[] objectivePath = objective.getObjectivePath();
        if (objectivePath.length <= 1) {
            return questDisplayName(objective.getQuestIdentifier(), quest(objective.getQuestIdentifier()));
        }
        final int[] parentPath = Arrays.copyOf(objectivePath, objectivePath.length - 1);
        final Objective parent = configuredObjective(objective.getQuestIdentifier(), parentPath);
        final Objectives.Type parentType = parent == null ? null : objectiveType(parent.typeId());
        return parent == null
                ? questDisplayName(objective.getQuestIdentifier(), quest(objective.getQuestIdentifier()))
                : parentType == null ? parent.typeId() : objectiveDisplayName(parent, parentType);
    }

    private void completeObjective(final ActiveObjective objective) {
        final PlatformPlayer questPlayer = objective.getQuestPlayer();
        final boolean suppressEffects = objective.isSuppressCompletionEffects();
        final boolean silent = objective.isSilentCompletion();
        if (!suppressEffects && !questPlayer.beforeObjectiveCompleted(
                quest(objective.getQuestIdentifier()), objective)) {
            return;
        }
        final Quest quest = quest(objective.getQuestIdentifier());
        final Objectives.Type type = objectiveType(objective.getObjectiveTypeID());
        final QuestPlayer playerData = questPlayer(questPlayer);
        if (playerData != null) {
            playerData.addCompletedObjective(new QuestPlayer.CompletedObjective(
                    objective.getQuestIdentifier(),
                    objective.getObjectivePathKey(),
                    objective.getHolderPath(),
                    objective.getObjectiveTypeID(),
                    objective.currentProgress(),
                    objective.getProgressNeeded()));
        }
        if (!suppressEffects) {
            triggerObjectiveEvent(questPlayer, "COMPLETE", objective.getQuestIdentifier(), objective.getObjectiveID());
            final String rewardBlock = giveObjectiveRewards(questPlayer, objective, warning -> {});
            if (!silent) {
                questPlayer.playSound(
                        "minecraft:block.anvil.land",
                        PlatformPlayer.SoundAudience.PLAYER,
                        playerLocation(questPlayer),
                        75.0,
                        1.4,
                        "master");
                String completionMessage = translate(questPlayer,
                        "chat.objectives.successfully-completed",
                        Map.of(
                                "%OBJECTIVENAME%",
                                type == null ? objective.getObjectiveTypeID() : objectiveDisplayName(objective.getObjective(), type),
                                "%QUESTNAME%",
                                questDisplayName(objective.getQuestIdentifier(), quest)),
                        "\n<CENTER><success>[Objective Completed]\n<CENTER><highlight><bold>"
                                + (type == null ? objective.getObjectiveTypeID() : objectiveDisplayName(objective.getObjective(), type))
                                + "</bold> <!i><main>(" + questDisplayName(objective.getQuestIdentifier(), quest) + ")\n<EMPTY>");
                if (!rewardBlock.isBlank()) {
                    completionMessage += "<RESET>" + rewardBlock;
                }
                questPlayer.sendMessage(completionMessage);
            }
        }
        removeObjectiveMarker(questPlayer, activeObjectiveBeamName(objective));
        questPlayerManager.getActiveObjectives().removeActiveObjective(objective);
        if (!suppressEffects && !questPlayerManager.getActiveObjectives().hasActiveObjectives(questPlayer.playerIdentifier(), objective.getQuestIdentifier())) {
            completeQuest(questPlayer, objective.getQuestIdentifier(), false, warning -> {});
        }
    }

    private String giveObjectiveRewards(
            final PlatformPlayer questPlayer,
            final ActiveObjective objective,
            final Consumer<String> warningSink) {
        final ArrayList<String> displayedRewards = new ArrayList<>();
        for (final Action reward : objective.getObjective().getRewards()) {
            if (executeConfiguredAction(reward, questPlayer, warningSink, "Objective reward failed")
                    && configuration.showRewardsAfterObjectiveCompletion()
                    && reward.getDisplayName() != null
                    && !reward.getDisplayName().isBlank()) {
                displayedRewards.add(reward.getDisplayName());
            }
        }
        if (displayedRewards.isEmpty()) {
            return "";
        }
        final StringBuilder rewardBlock = new StringBuilder();
        rewardBlock.append("\n").append(translate(questPlayer,
                    "chat.objectives.successfully-completed-rewards-prefix",
                    Map.of(),
                    "    <highlight>Rewards:"));
        for (final String rewardName : displayedRewards) {
            rewardBlock.append("\n").append(translate(questPlayer,
                        "chat.objectives.successfully-completed-rewards-rewardformat",
                        Map.of("%reward%", rewardName),
                        "    <highlight> - <unimportant>" + rewardName));
        }
        rewardBlock.append("\n").append(translate(questPlayer,
                    "chat.objectives.successfully-completed-rewards-suffix",
                    Map.of(),
                    "<EMPTY>"));
        return rewardBlock.toString();
    }

    private void sendQuestAcceptedDisplay(
            final PlatformPlayer questPlayer,
            final Quest quest,
            final List<ObjectiveActivation> activatedObjectives) {
        final List<ObjectiveActivation> unlockedObjectives = activatedObjectives.stream()
                .filter(objective -> objective.progress() != null && objective.progress().isUnlocked())
                .toList();
        if (!unlockedObjectives.isEmpty()) {
            questPlayer.sendMessage(translate(questPlayer,
                    "chat.objectives-label-after-quest-accepting",
                    Map.of(),
                    "<highlight>Objectives:"));
            for (final ObjectiveActivation objective : unlockedObjectives) {
                questPlayer.sendMessage(translate(questPlayer,
                        "chat.objectives.counter",
                        Map.of(
                                "%OBJECTIVEIDWITHSUBID%", String.valueOf(objective.objective().id()),
                                "%OBJECTIVENAME%", objectiveDisplayName(objective.objective(), objective.type())),
                        "<highlight>" + objective.objective().id() + ".</highlight> <main>"
                                + objectiveDisplayName(objective.objective(), objective.type()) + ":"));
                final String description = objective.objective().getDescription();
                if (description != null && !description.isBlank()) {
                    questPlayer.sendMessage(translate(questPlayer,
                            "chat.objectives.description",
                            Map.of("%OBJECTIVEDESCRIPTION%", description),
                            "    <veryUnimportant>└─ <unimportant>Description: <main>" + description));
                }
                final String taskDescription = objectiveTaskDescription(objective, questPlayer);
                if (taskDescription != null && !taskDescription.isBlank()) {
                    questPlayer.sendMessage(taskDescription);
                }
                questPlayer.sendMessage(translate(questPlayer,
                        "chat.objectives.progress",
                        Map.of(
                                "%ACTIVEOBJECTIVEPROGRESS%", formatProgress(objective.progress().getCurrentProgress()),
                                "%OBJECTIVEPROGRESSNEEDED%", formatProgress(objective.progress().progressNeeded())),
                        "    <veryUnimportant>└─ <unimportant>Progress: <main>"
                                + formatProgress(objective.progress().getCurrentProgress())
                                + " / "
                                + formatProgress(objective.progress().progressNeeded())));
            }
        }
        if (configuration.questAcceptedTitleEnabled()) {
            questPlayer.showTitle(
                    translate(questPlayer,
                            "titles.quest-accepted.title",
                            questReplacements(quest),
                            "<main>Quest accepted"),
                    translate(questPlayer,
                            "titles.quest-accepted.subtitle",
                            questReplacements(quest),
                            "<highlight>" + questDisplayName(quest)),
                    Duration.ofMillis(2),
                    Duration.ofSeconds(3),
                    Duration.ofMillis(8));
        }
        questPlayer.playSound(
                "minecraft:entity.evoker.prepare_summon",
                PlatformPlayer.SoundAudience.PLAYER,
                playerLocation(questPlayer),
                100.0,
                2.0,
                "master");
        if (quest.getDescription() == null || quest.getDescription().isBlank()) {
            questPlayer.sendMessage(translate(questPlayer,
                    "chat.missing-quest-description",
                    questReplacements(quest),
                    "<unimportant>This quest has no quest description."));
        } else {
            questPlayer.sendMessage(translate(questPlayer,
                    "chat.quest-description",
                    questReplacements(quest),
                    "<main>Quest description: <unimportant>" + quest.getDescription()));
        }
        questPlayer.sendMessage(translate(questPlayer,
                "chat.quest-successfully-accepted",
                questReplacements(quest),
                "\n<CENTER><main>[Quest Accepted]\n<CENTER><highlight><BOLD>" + questDisplayName(quest) + "\n<EMPTY>"));
    }

    private Objectives.Type objectiveType(final String objectiveTypeId) {
        return registry.objectives().stream()
                .filter(objective -> objective.id().equalsIgnoreCase(objectiveTypeId))
                .findFirst()
                .orElse(null);
    }

    private Conditions.Type conditionType(final String conditionTypeId) {
        return registry.conditions().stream()
                .filter(condition -> condition.id().equalsIgnoreCase(conditionTypeId))
                .findFirst()
                .orElse(null);
    }

    private Actions.Type actionType(final String actionTypeId) {
        return registry.actions().stream()
                .filter(action -> action.id().equalsIgnoreCase(actionTypeId))
                .findFirst()
                .orElse(null);
    }

    private static String debugHL(final String text) {
        return NotQuestsColors.debugHighlightGradient + text + "</gradient>";
    }

    private static String formatProgress(final double value) {
        if (value == Math.rint(value)) {
            return String.valueOf((long) value);
        }
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static NQLocation playerLocation(final PlatformPlayer player) {
        return NQLocation.at(
                player.worldName(),
                player.positionX(),
                player.positionY(),
                player.positionZ());
    }

    private String objectiveTaskDescription(
            final ObjectiveActivation objective,
            final PlatformPlayer questPlayer) {
        if (objective.objective().getTaskDescription() != null && !objective.objective().getTaskDescription().isBlank()) {
            return wrapObjectiveTaskDescription(objective.objective().getTaskDescription());
        }
        if (objective.type().taskDescriptionRenderer() == null) {
            return "";
        }
        final String rendered = objective.type().taskDescriptionRenderer().render(
                objective.progress(),
                questPlayer,
                objective.progress());
        return wrapObjectiveTaskDescription(rendered);
    }

    private String activeObjectiveTaskDescription(
            final PlatformPlayer questPlayer,
            final ActiveObjective progress,
            final Objectives.Type type) {
        return objectiveTaskDescription(questPlayer, progress.getObjective(), type, progress, false);
    }

    private String objectiveTaskDescription(
            final PlatformPlayer questPlayer,
            final Objective entry,
            final Objectives.Type type,
            final ActiveObjective activeObjective,
            final boolean completed) {
        final String taskDescription = entry.getTaskDescription() == null
                || entry.getTaskDescription().isBlank()
                ? renderObjectiveTaskDescription(questPlayer, entry.data(), type, activeObjective)
                : entry.getTaskDescription();
        final String[] completionNpc = completionNpc(entry.getCompletionNPC());
        final String prefix = translateFor(
                questPlayer,
                "chat.objectives.taskDescription.global.prefix",
                Map.of(),
                "    <veryUnimportant>└─ <unimportant>");
        final String suffix = translateFor(
                questPlayer,
                "chat.objectives.taskDescription.global.suffix",
                Map.of(),
                "");
        final String formattedTask = formatObjectiveTask(taskDescription, completionNpc, completed);
        return (prefix == null ? "" : prefix)
                + (formattedTask == null ? "" : formattedTask).replace("    <veryUnimportant>└─ <unimportant>", "")
                + (suffix == null ? "" : suffix);
    }

    private String renderObjectiveTaskDescription(
            final PlatformPlayer questPlayer,
            final Objectives.Data objectiveData,
            final Objectives.Type type,
            final ActiveObjective activeObjective) {
        if (type == null || type.taskDescriptionRenderer() == null) {
            return "";
        }
        return type.taskDescriptionRenderer().render(objectiveData, questPlayer, activeObjective);
    }

    private String[] completionNpc(final String selector) {
        if (selector == null || selector.isBlank()) {
            return null;
        }
        final NotQuestsAdapter.NpcSelection npc = runtimeAdapter().npcSelection(selector);
        if (npc != null) {
            return new String[] {
                    npc.npcId() == null ? selector : String.valueOf(npc.npcId()),
                    npc.npcName()};
        }
        return new String[] {selector, null};
    }

    private static String formatObjectiveTask(
            final String taskDescription, final String[] completionNpc, final boolean completed) {
        String text = taskDescription == null ? "" : taskDescription;
        if (completionNpc != null) {
            if (completionNpc[1] != null) {
                text += "\n    <GRAY>To complete: Talk to <highlight>" + completionNpc[1];
            } else {
                text += "\n    <GRAY>To complete: Talk to NPC with ID <highlight>"
                        + completionNpc[0] + " <RED>[currently unreachable]";
            }
        }
        return completed ? "<strikethrough>" + text + "</strikethrough>" : text;
    }

    private static String wrapObjectiveTaskDescription(final String taskDescription) {
        if (taskDescription == null || taskDescription.isBlank()) {
            return "";
        }
        final String prefix = "    <veryUnimportant>└─ <unimportant>";
        if (taskDescription.startsWith(prefix)) {
            return taskDescription;
        }
        return prefix + taskDescription;
    }

    private String translateFor(
            final PlatformPlayer questPlayer,
            final String translationKey,
            final Map<String, String> replacements,
            final String fallback) {
        return questPlayer == null
                ? translate(translationKey, replacements, fallback)
                : translate(questPlayer, translationKey, replacements, fallback);
    }

    private String resolvePlayerPlaceholders(
            final PlatformPlayer questPlayer,
            final String message) {
        return resolvePlaceholders(questPlayer, message);
    }

    private static String objectiveDisplayName(
            final Objective objective,
            final Objectives.Type type) {
        return objective.getDisplayName() == null || objective.getDisplayName().isBlank()
                ? type.displayName()
                : objective.getDisplayName();
    }

    private static String questDisplayName(final Quest quest) {
        return quest.getDisplayName() == null || quest.getDisplayName().isBlank()
                ? quest.getIdentifier()
                : quest.getDisplayName();
    }

    private static String questDisplayName(final String questName, final Quest quest) {
        return quest == null ? questName : questDisplayName(quest);
    }

    private static Map<String, String> questReplacements(final Quest quest) {
        if (quest == null) {
            return Map.of();
        }
        return Map.of(
                "%QUESTNAME%", questDisplayName(quest),
                "%QUESTID%", quest.getIdentifier(),
                "%QUESTDESCRIPTION%", quest.getDescription() == null ? "" : quest.getDescription());
    }

    private static Map<String, String> questReplacements(final String questName, final Quest quest) {
        if (quest == null) {
            return Map.of(
                    "%QUESTNAME%", questName == null ? "" : questName,
                    "%QUESTID%", questName == null ? "" : questName,
                    "%QUESTDESCRIPTION%", "");
        }
        return questReplacements(quest);
    }

    private String questAcceptFailureMessage(
            final PlatformPlayer questPlayer,
            final Quest quest,
            final Quest.AcceptCheck check) {
        return switch (check.status()) {
            case ALREADY_ACCEPTED -> translate(questPlayer,
                    "chat.quest-already-accepted",
                    Map.of(),
                    "<error>Quest already accepted.");
            case MAX_COMPLETIONS -> translate(questPlayer,
                    "chat.reached-max-completions-limit",
                    Map.of(
                            "%MAXCOMPLETIONS%", String.valueOf(quest.getMaxCompletions()),
                            "%COMPLETEDAMOUNT%", String.valueOf(check.completedAmount())),
                    "<red>You have completed this quest too many times already. You can only complete it "
                            + "<highlight>" + quest.getMaxCompletions() + "</highlight> times, but you have already completed it "
                            + "<highlight>" + check.completedAmount() + "</highlight> times.");
            case MAX_ACCEPTS -> translate(questPlayer,
                    "chat.reached-max-accepts-limit",
                    Map.of(
                            "%MAXACCEPTS%", String.valueOf(quest.getMaxAccepts()),
                            "%ACCEPTEDAMOUNT%", String.valueOf(check.acceptedAmount())),
                    "<red>You have accepted this quest too many times already. You can only accept it "
                            + "<highlight>" + quest.getMaxAccepts() + "</highlight> times, but you have already accepted it "
                            + "<highlight>" + check.acceptedAmount() + "</highlight> times.");
            case MAX_FAILS -> translate(questPlayer,
                    "chat.reached-max-fails-limit",
                    Map.of(
                            "%MAXFAILS%", String.valueOf(quest.getMaxFails()),
                            "%FAILEDAMOUNT%", String.valueOf(check.failedAmount())),
                    "<red>You have failed this quest too many times already. You can only fail it "
                            + "<highlight>" + quest.getMaxFails() + "</highlight> times, but you have already failed it "
                            + "<highlight>" + check.failedAmount() + "</highlight> times.");
            case COOLDOWN -> questCooldownMessage(questPlayer, check);
            case MAX_ACTIVE_QUESTS_PER_PLAYER -> translate(questPlayer,
                    "chat.reached-max-active-quests-per-player-limit",
                    Map.of("%MAXACTIVEQUESTSPERPLAYER%", String.valueOf(configuration.maxActiveQuestsPerPlayer())),
                    "<red>You can not accept more quests right now.");
            case ACCEPTABLE -> "";
        };
    }

    private String questCooldownMessage(final PlatformPlayer questPlayer, final Quest.AcceptCheck check) {
        if (check.timeToWaitInMinutes() < 60) {
            return check.timeToWaitInMinutes() == 1
                    ? translate(questPlayer,
                            "chat.quest-on-cooldown.minute",
                            Map.of(),
                            "<red>This quest is on a cooldown! You have to wait another <highlight>1 minute</highlight> until you can take it again.")
                    : translate(questPlayer,
                            "chat.quest-on-cooldown.minutes",
                            Map.of("%MINUTES%", String.valueOf(check.timeToWaitInMinutes())),
                            "<red>This quest is on a cooldown! You have to wait another <highlight>" + check.timeToWaitInMinutes()
                                    + " minutes</highlight> until you can take it again.");
        }
        if (check.timeToWaitInHours() < 24) {
            return check.timeToWaitInHours() == 1.0
                    ? translate(questPlayer,
                            "chat.quest-on-cooldown.hour",
                            Map.of(),
                            "<red>This quest is on a cooldown! You have to wait another <highlight>1 hour</highlight> until you can take it again.")
                    : translate(questPlayer,
                            "chat.quest-on-cooldown.hours",
                            Map.of("%HOURS%", String.valueOf(check.timeToWaitInHours())),
                            "<red>This quest is on a cooldown! You have to wait another <highlight>" + check.timeToWaitInHours()
                                    + " hours</highlight> until you can take it again.");
        }
        return check.timeToWaitInDays() == 1.0
                ? translate(questPlayer,
                        "chat.quest-on-cooldown.day",
                        Map.of(),
                        "<red>This quest is on a cooldown! You have to wait another <highlight>1 day</highlight> until you can take it again.")
                : translate(questPlayer,
                        "chat.quest-on-cooldown.days",
                        Map.of("%DAYS%", String.valueOf(check.timeToWaitInDays())),
                        "<red>This quest is on a cooldown! You have to wait another <highlight>" + check.timeToWaitInDays()
                                + " days</highlight> until you can take it again.");
    }

    private void sendQuestCompletedDisplay(
            final PlatformPlayer questPlayer,
            final String questName,
            final Quest quest,
            final ConfigurationManager configuration,
            final String rewardBlock) {
        if (configuration.questCompletedTitleEnabled()) {
            questPlayer.showTitle(
                    translate(questPlayer,
                            "titles.quest-completed.title",
                            questReplacements(questName, quest),
                            "<positive>Quest Completed"),
                    translate(questPlayer,
                            "titles.quest-completed.subtitle",
                            questReplacements(questName, quest),
                            "<highlight>" + questDisplayName(questName, quest)),
                    Duration.ofMillis(2),
                    Duration.ofSeconds(3),
                    Duration.ofMillis(8));
        }
        questPlayer.playSound(
                "minecraft:ui.toast.challenge_complete",
                PlatformPlayer.SoundAudience.PLAYER,
                playerLocation(questPlayer),
                100.0,
                40.0,
                "master");
        String message = translate(questPlayer,
                "chat.quest-completed-and-rewards-given",
                questReplacements(questName, quest),
                "\n<CENTER><positive>[Quest Completed]\n<CENTER><highlight><BOLD>"
                        + questDisplayName(questName, quest) + "\n<EMPTY>");
        if (rewardBlock != null && !rewardBlock.isBlank()) {
            message += "<RESET>" + rewardBlock;
        }
        questPlayer.sendMessage(message);
    }

    private void sendQuestFailedDisplay(
            final PlatformPlayer questPlayer,
            final String questName,
            final Quest quest,
            final ConfigurationManager configuration) {
        if (configuration.questFailedTitleEnabled()) {
            questPlayer.showTitle(
                    translate(questPlayer,
                            "titles.quest-failed.title",
                            questReplacements(questName, quest),
                            "<negative>Quest Failed"),
                    translate(questPlayer,
                            "titles.quest-failed.subtitle",
                            questReplacements(questName, quest),
                            "<highlight>" + questDisplayName(questName, quest)),
                    Duration.ofMillis(2),
                    Duration.ofSeconds(3),
                    Duration.ofMillis(8));
        }
        questPlayer.playSound(
                "minecraft:entity.ravager.death",
                PlatformPlayer.SoundAudience.PLAYER,
                playerLocation(questPlayer),
                100.0,
                1.0,
                "master");
        questPlayer.sendMessage(translate(questPlayer,
                "chat.quest-failed",
                questReplacements(questName, quest),
                "\n<CENTER><negative>[Quest Failed]\n<CENTER><highlight><BOLD>"
                        + questDisplayName(questName, quest) + "\n<EMPTY>"));
    }

    public boolean completeQuest(
            final PlatformPlayer questPlayer,
            final String questName,
            final Consumer<String> warningSink) {
        return completeQuest(questPlayer, questName, true, warningSink);
    }

    public boolean forceCompleteQuest(
            final PlatformPlayer questPlayer,
            final String questName,
            final Consumer<String> warningSink) {
        return completeQuest(questPlayer, questName, true, warningSink);
    }

    public boolean completeQuest(
            final PlatformPlayer questPlayer,
            final String questName,
            final boolean forced,
            final Consumer<String> warningSink) {
        final QuestPlayer playerData = questPlayer(questPlayer);
        if (playerData == null || questName == null || questName.isBlank()) {
            warn(warningSink, "Cannot complete quest: missing target player or quest name.");
            return false;
        }
        if (!playerData.hasActiveQuest(questName)) {
            questPlayer.sendMessage("<error>Cannot complete quest <highlight>" + questName
                    + "</highlight> because it is not active.");
            return false;
        }
        if (!questPlayer.beforeQuestCompleted(quest(questName), forced)) {
            return false;
        }
        if (forced) {
            removeQuestObjectiveMarkers(questPlayer, questName);
            questPlayerManager.getActiveObjectives().forceCompleteActiveObjectives(questPlayer.playerIdentifier(), questName);
        }
        triggerQuestEvent(questPlayer, "COMPLETE", questName);
        if (!forced) {
            removeQuestObjectiveMarkers(questPlayer, questName);
        }
        questPlayerManager.getActiveObjectives().refreshObjectives(
                questPlayer,
                Objectives.ObjectiveRefresh.questCompleted(questName));
        questPlayerManager.getActiveObjectives().removeActiveObjectives(questPlayer.playerIdentifier(), questName);
        if (playerData.completeQuest(questName, System.currentTimeMillis())) {
            removeActiveTriggers(questPlayer.playerIdentifier(), questName);
            final Quest quest = quest(questName);
            final String rewardBlock = giveQuestRewards(questPlayer, quest, warningSink);
            sendQuestCompletedDisplay(questPlayer, questName, quest, configuration, rewardBlock);
            return true;
        } else {
            questPlayer.sendMessage("<error>Cannot complete quest <highlight>" + questName
                    + "</highlight> because it is not active.");
            return false;
        }
    }

    private String giveQuestRewards(
            final PlatformPlayer questPlayer,
            final Quest quest,
            final Consumer<String> warningSink) {
        if (quest == null) {
            return "";
        }
        final ArrayList<String> displayedRewards = new ArrayList<>();
        for (final Action reward : quest.getRewards()) {
            final String displayName = reward.getDisplayName();
            if (executeConfiguredAction(reward, questPlayer, warningSink, "Quest reward failed")
                    && configuration.showRewardsAfterQuestCompletion()
                    && displayName != null
                    && !displayName.isBlank()) {
                displayedRewards.add(displayName);
            }
        }
        if (displayedRewards.isEmpty()) {
            return "";
        }
        final StringBuilder rewardBlock = new StringBuilder();
        rewardBlock.append("\n").append(translate(questPlayer,
                    "chat.quest-completed-rewards-prefix",
                    Map.of(),
                    "    <highlight>Rewards:"));
        for (final String rewardName : displayedRewards) {
            rewardBlock.append("\n").append(translate(questPlayer,
                        "chat.quest-completed-rewards-rewardformat",
                        Map.of("%reward%", rewardName),
                        "    <highlight> - <unimportant>" + rewardName));
        }
        rewardBlock.append("\n").append(translate(questPlayer,
                    "chat.quest-completed-rewards-suffix",
                    Map.of(),
                    "<EMPTY>"));
        return rewardBlock.toString();
    }

    private boolean executeConfiguredAction(
            final Action action,
            final PlatformPlayer questPlayer,
            final Consumer<String> warningSink,
            final String failurePrefix) {
        return executeConfiguredAction(
                action.typeId(),
                action.data(),
                configuredActionDelay(action),
                configuredActionConditions(action),
                questPlayer,
                warningSink,
                failurePrefix);
    }

    public boolean executeConfiguredAction(
            final String actionTypeId,
            final Actions.Data actionData,
            final Duration delay,
            final List<ActionCondition> conditions,
            final PlatformPlayer questPlayer,
            final Consumer<String> warningSink,
            final String failurePrefix,
            final Object... objects) {
        final Actions.Type actionType = actionType(actionTypeId);
        if (actionType == null || actionType.executor() == null) {
            warn(warningSink, "Cannot execute unknown action type: " + actionTypeId);
            return false;
        }
        if (!configuredActionConditionsFulfilled(conditions, questPlayer, warningSink)) {
            return false;
        }
        try {
            final Runnable run = () -> {
                actionType.executor().execute(
                        actionData == null ? new Action(0, actionTypeId, null) : actionData,
                        questPlayer,
                        objects);
                if (configuration.objectiveUnlockConditionsCheckOnAnyAction()) {
                    questPlayerManager.getActiveObjectives().refreshObjectives(
                            questPlayer,
                            Objectives.ObjectiveRefresh.periodic());
                    questPlayerManager.getActiveObjectives().refreshObjectiveUnlocks(questPlayer);
                }
            };
            if (delay == null || delay.isNegative() || delay.isZero()) {
                run.run();
            } else {
                actionScheduler.schedule(delay, run);
            }
            return true;
        } catch (final RuntimeException exception) {
            warn(warningSink, failurePrefix + ": " + exception.getMessage());
            return false;
        }
    }

    private boolean configuredActionConditionsFulfilled(
            final Action action,
            final PlatformPlayer questPlayer,
            final Consumer<String> warningSink) {
        return configuredActionConditionsFulfilled(configuredActionConditions(action), questPlayer, warningSink);
    }

    private boolean configuredActionConditionsFulfilled(
            final List<ActionCondition> conditions,
            final PlatformPlayer questPlayer,
            final Consumer<String> warningSink) {
        for (final ActionCondition condition : conditions == null
                ? List.<ActionCondition>of()
                : conditions) {
            final Conditions.Type conditionType = conditionType(condition.typeId());
            if (conditionType == null || conditionType.checker() == null) {
                warn(warningSink, "Cannot check unknown action condition type: " + condition.typeId());
                return false;
            }
            try {
                final String result = ConditionCheck.check(
                        conditionType,
                        actionConditionData(condition),
                        questPlayer);
                if (result != null && !result.isBlank()) {
                    warn(warningSink, result);
                    return false;
                }
            } catch (final RuntimeException exception) {
                warn(warningSink, "Action condition failed: " + exception.getMessage());
                return false;
            }
        }
        return true;
    }

    private static List<ActionCondition> configuredActionConditions(
            final Action action) {
        if (action == null) {
            return List.of();
        }
        return action.getConditions().stream()
                .map(condition -> new ActionCondition(
                        condition.id(),
                        condition.typeId(),
                        condition,
                        new Quest.ConditionSettings(
                                condition.getProgressNeeded(),
                                condition.isNegated(),
                                condition.getDescription(),
                                condition.getHiddenExpression(),
                                condition.isAllowProgressDecreaseIfNotFulfilled())))
                .toList();
    }

    private static Condition actionConditionData(final ActionCondition condition) {
        final Condition data = new Condition(condition.id(), condition.typeId(), condition.data());
        final Quest.ConditionSettings settings = condition.settings();
        if (settings != null) {
            data.apply(settings);
        }
        return data;
    }

    private static Duration configuredActionDelay(final Action action) {
        if (action == null || action.data() == null) {
            return null;
        }
        final Object value = action.data().value("executionDelay");
        if (value instanceof Number number) {
            final long millis = number.longValue();
            return millis <= 0 ? null : Duration.ofMillis(millis);
        }
        if (value != null) {
            try {
                final long millis = Long.parseLong(value.toString());
                return millis <= 0 ? null : Duration.ofMillis(millis);
            } catch (final NumberFormatException ignored) {
            }
        }
        return null;
    }

    private record ObjectiveActivation(
            Objective objective,
            Objectives.Type type,
            ActiveObjective progress) {}

    private static String objectivePath(final int[] objectivePath) {
        if (objectivePath == null || objectivePath.length == 0) {
            return "";
        }
        final StringBuilder builder = new StringBuilder();
        for (final int id : objectivePath) {
            if (!builder.isEmpty()) {
                builder.append('.');
            }
            builder.append(id);
        }
        return builder.toString();
    }

    public boolean failQuest(
            final PlatformPlayer questPlayer,
            final String questName,
            final Consumer<String> warningSink) {
        final QuestPlayer playerData = questPlayer(questPlayer);
        if (playerData == null || questName == null || questName.isBlank()) {
            warn(warningSink, "Cannot fail quest: missing target player or quest name.");
            return false;
        }
        if (!questPlayer.beforeQuestFailed(quest(questName))) {
            return false;
        }
        triggerQuestEvent(questPlayer, "FAIL", questName);
        removeQuestObjectiveMarkers(questPlayer, questName);
        questPlayerManager.getActiveObjectives().removeActiveObjectives(questPlayer.playerIdentifier(), questName);
        if (playerData.failQuest(questName, System.currentTimeMillis())) {
            removeActiveTriggers(questPlayer.playerIdentifier(), questName);
            final Quest quest = quest(questName);
            sendQuestFailedDisplay(questPlayer, questName, quest, configuration);
            return true;
        } else {
            questPlayer.sendMessage("<error>Cannot fail quest <highlight>" + questName
                    + "</highlight> because it is not active.");
            return false;
        }
    }

    private void removeQuestObjectiveMarkers(
            final PlatformPlayer questPlayer,
            final String questName) {
        if (questPlayer == null || questName == null || questName.isBlank()) {
            return;
        }
        for (final ActiveObjective objective : questPlayerManager.getActiveObjectives().activeObjectives(questPlayer.playerIdentifier())) {
            if (objective.getQuestIdentifier().equalsIgnoreCase(questName)) {
                removeObjectiveMarker(questPlayer, activeObjectiveBeamName(objective));
            }
        }
        questPlayer.hideProgressBossBar();
    }

    public boolean runGuiActions(
            final PlatformPlayer questPlayer,
            final List<GuiAction> actions,
            final Consumer<String> warningSink) {
        if (questPlayer == null || actions == null || actions.isEmpty()) {
            return false;
        }
        boolean ranAction = false;
        for (final GuiAction action : actions) {
            if (action == null || action.type() == null) {
                continue;
            }
            boolean conditionsFulfilled = true;
            for (final String condition : action.conditions()) {
                final Result result =
                        checkRegistryCondition(condition, questPlayer, "GUI");
                if (!result.fulfilled()) {
                    warn(warningSink, result.message());
                    conditionsFulfilled = false;
                    break;
                }
            }
            if (!conditionsFulfilled) {
                continue;
            }
            switch (action.type()) {
                case OPEN_GUI -> ranAction = openGui(
                        questPlayer,
                        action.target(),
                        action.playerName(),
                        action.context()) || ranAction;
                case TAKE_QUEST -> ranAction = giveQuest(
                        questPlayer,
                        action.questIdentifier(),
                        false,
                        warningSink) || ranAction;
                case FAIL_QUEST -> ranAction = failQuest(
                        questPlayer,
                        action.questIdentifier(),
                        warningSink) || ranAction;
                case CLOSE -> {
                    questPlayer.closeInventory();
                    ranAction = true;
                }
                case REGISTRY_ACTION -> ranAction = executeRegistryActionLine(
                        action.target(),
                        questPlayer,
                        warningSink,
                        "GUI") || ranAction;
            }
        }
        return ranAction;
    }

    public String playConversation(
            final PlatformPlayer questPlayer,
            final String conversationName,
            final int npcId,
            final Consumer<String> warningSink) {
        return playConversation(questPlayer, conversationName, npcId, warningSink, null);
    }

    public String playConversation(
            final PlatformPlayer questPlayer,
            final String conversationName,
            final int npcId,
            final Consumer<String> warningSink,
            final Runnable npcConversationEnded) {
        String endedMessage = null;
        if (hasActiveConversation(questPlayer)) {
            endedMessage = conversationEndedPreviousMessage(questPlayer);
        }
        final boolean started = startConversation(questPlayer, conversationName, true, warningSink);
        if (started
                && npcId >= 0
                && questPlayer != null
                && conversationName.equalsIgnoreCase(activeConversationName(questPlayer))) {
            conversations.startNpcSession(
                    npcId,
                    UUID.fromString(questPlayer.playerIdentifier()),
                    npcConversationEnded);
        }
        return endedMessage;
    }

    public boolean startConversation(
            final PlatformPlayer questPlayer,
            final String conversationName,
            final boolean endPrevious,
            final Consumer<String> warningSink) {
        if (!conversations.start(questPlayer, conversationName, endPrevious)) {
            warn(warningSink, "Cannot start conversation: unknown conversation or target player is already in a conversation.");
            return false;
        }
        return true;
    }

    public void markNpcConversationStarted(final int npcId, final UUID playerId) {
        if (playerId != null) {
            conversations.startNpcSession(npcId, playerId);
        }
    }

    public void stopNpcConversation(final int npcId, final UUID playerId) {
        if (playerId != null) {
            conversations.stopNpcSession(npcId, playerId);
        }
    }

    public void clearNpcConversationSessions() {
        conversations.clearNpcSessions();
    }

    public boolean hasActiveConversationForNpc(final int npcId) {
        return conversations.hasNpcSession(npcId);
    }

    public boolean chooseConversationOption(final PlatformPlayer questPlayer, final int optionId) {
        return conversations.chooseOption(questPlayer, optionId);
    }

    public boolean stopConversation(final PlatformPlayer questPlayer) {
        return conversations.stop(questPlayer);
    }

    public String activeConversationName(final PlatformPlayer questPlayer) {
        return questPlayer == null ? null : conversations.activeConversation(questPlayer.playerIdentifier());
    }

    public String activeConversationName(final String playerIdentifier) {
        return conversations.activeConversation(playerIdentifier);
    }

    public boolean hasActiveConversation(final PlatformPlayer questPlayer) {
        return questPlayer != null
                && questPlayer.hasPlayer()
                && conversations.activeConversation(questPlayer.playerIdentifier()) != null;
    }

    public boolean hasActiveConversation(final String playerIdentifier) {
        return conversations.activeConversation(playerIdentifier) != null;
    }

    public void triggerCommandObjectiveProgress(
            final PlatformPlayer questPlayer,
            final String triggerName) {
        if (questPlayer == null || !questPlayer.hasPlayer()) {
            return;
        }
        if (questPlayer.playerIdentifier().isBlank()) {
            return;
        }
        questPlayerManager.getActiveObjectives().onPlayerRunCommand(questPlayer, new RunCommandEvent(triggerName));
    }

    private QuestPlayer questPlayer(final PlatformPlayer questPlayer) {
        return questPlayerManager.getQuestPlayer(questPlayer);
    }

    private static void warn(final Consumer<String> warningSink, final String message) {
        if (warningSink != null) {
            warningSink.accept(message);
        }
    }

    private record RunCommandEvent(String command, Runnable cancelCallback)
            implements Objectives.CommandEvent {
        private RunCommandEvent(final String command) {
            this(command, () -> {});
        }

        @Override
        public void cancel() {
            if (cancelCallback != null) {
                cancelCallback.run();
            }
        }
    }

    public record QuestReset(
            String playerIdentifier,
            String playerName,
            boolean playerFound,
            boolean activeRemoved,
            boolean activeFailed,
            int completedRemoved) {
        public static QuestReset playerNotFound(final String playerIdentifier) {
            return new QuestReset(playerIdentifier, playerIdentifier, false, false, false, 0);
        }

        public static QuestReset changed(
                final String playerIdentifier,
                final String playerName,
                final boolean activeRemoved,
                final boolean activeFailed,
                final int completedRemoved) {
            return new QuestReset(playerIdentifier, playerName, true, activeRemoved, activeFailed, completedRemoved);
        }

        public boolean changed() {
            return activeRemoved || activeFailed || completedRemoved > 0;
        }
    }

    public static final class StoredCondition {
        private final String name;
        private final Conditions.Type type;
        private final Condition condition;
        private String category = "";

        private StoredCondition(
                final String name,
                final Conditions.Type type,
                final Conditions.Data data) {
            this.name = name;
            this.type = type;
            this.condition = new Condition(0, type.id(), data);
        }

        public static StoredCondition restore(
                final String name,
                final Conditions.Type type,
                final Conditions.Data data) {
            return new StoredCondition(name, type, data);
        }

        public String getName() {
            return name;
        }

        public Conditions.Type getType() {
            return type;
        }

        public Condition getData() {
            return condition;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(final String category) {
            this.category = category == null ? "" : category;
        }

        public void applyMetadata(
                final String category,
                final long progressNeeded,
                final boolean negated,
                final String description,
                final String hiddenExpression) {
            setCategory(category);
            setProgressNeeded(progressNeeded);
            setNegated(negated);
            setDescription(description);
            setHiddenExpression(hiddenExpression);
        }

        public String getDescription() {
            return condition.getDescription();
        }

        public void setDescription(final String description) {
            condition.setDescription(description);
        }

        public String getHiddenExpression() {
            return condition.getHiddenExpression();
        }

        public void setHiddenExpression(final String hiddenExpression) {
            condition.setHiddenExpression(hiddenExpression);
        }

        public long getProgressNeeded() {
            return condition.getProgressNeeded();
        }

        public void setProgressNeeded(final long progressNeeded) {
            condition.setProgressNeeded(progressNeeded);
        }

        public boolean isNegated() {
            return condition.isNegated();
        }

        public void setNegated(final boolean negated) {
            condition.setNegated(negated);
        }
    }
}
