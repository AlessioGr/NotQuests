package com.notquests.core.managers;

import com.notquests.core.config.CategoryFiles;
import com.notquests.core.config.YamlConfig;
import com.notquests.core.gui.GuiService;
import com.notquests.core.managers.LogManager.LogCategory;
import com.notquests.core.text.NotQuestsColors.Palette;
import com.notquests.core.text.NotQuestsMiniMessage;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public final class ConfigurationManager implements Palette {
    public static final String CONFIG_VERSION = "config-version-do-not-edit";
    private volatile int maxActiveQuestsPerPlayer = -1;
    private volatile boolean debugEnabled = false;
    private volatile boolean verboseStartupMessages = true;
    private volatile boolean consoleColorsEnabled = true;
    private volatile boolean consoleColorsDownsampled = false;
    private volatile String consolePrefixPrefix = "<#393e46>[<gradient:#E0EAFC:#CFDEF3>";
    private volatile String consolePrefixSuffix = "<#393e46>]<#636c73>: ";
    private volatile String consoleInfoDefault = "<main>";
    private volatile String consoleInfoDefaultDownsampled = "<gray>";
    private volatile String consoleInfoData = "<gradient:#1FA2FF:#12D8FA:#A6FFCB>";
    private volatile String consoleInfoDataDownsampled = "<blue>";
    private volatile String consoleInfoLanguage = "<gradient:#AA076B:#61045F>";
    private volatile String consoleInfoLanguageDownsampled = "<dark_purple>";
    private volatile String consoleWarnDefault = "<warn>";
    private volatile String consoleWarnDefaultDownsampled = "<yellow>";
    private volatile String consoleSevereDefault = "<error>";
    private volatile String consoleSevereDefaultDownsampled = "<red>";
    private volatile String consoleDebugDefault = "<unimportant>";
    private volatile String consoleDebugDefaultDownsampled = "<dark_gray>";
    private volatile boolean loadPlayerData = true;
    private volatile boolean savePlayerData = true;
    private volatile boolean loadPlayerDataOnJoin = true;
    private volatile boolean savePlayerDataOnQuit = true;
    private volatile boolean backupQuestsOnShutdown = true;
    private volatile boolean backupDatabaseBeforeLoad = true;
    private volatile boolean databaseEnabled = false;
    private volatile String databaseHost = "";
    private volatile int databasePort = 3306;
    private volatile String databaseName = "";
    private volatile String databaseUsername = "";
    private volatile String databasePassword = "";
    private volatile boolean integrationCitizensEnabled = true;
    private volatile boolean integrationVaultEnabled = true;
    private volatile boolean integrationPlaceholderApiEnabled = true;
    private volatile boolean integrationMythicMobsEnabled = true;
    private volatile boolean integrationEliteMobsEnabled = true;
    private volatile boolean integrationWorldEditEnabled = true;
    private volatile boolean integrationSlimefunEnabled = true;
    private volatile boolean integrationLuckPermsEnabled = true;
    private volatile boolean integrationUltimateClansEnabled = true;
    private volatile boolean integrationTownyEnabled = true;
    private volatile boolean integrationJobsRebornEnabled = true;
    private volatile boolean integrationEcoMobsEnabled = true;
    private volatile boolean integrationBetonQuestEnabled = true;
    private volatile boolean integrationFloodgateEnabled = true;
    private volatile boolean updateCheckerNotifyOpsInChat = true;
    private volatile String languageCode = "en-US";
    private volatile boolean deletePreviousConversationMessages = true;
    private volatile int previousConversationHistorySize = 20;
    private volatile boolean packetMagicEnabled = true;
    private volatile boolean packetMagicUsePacketEvents = false;
    private volatile boolean packetMagicUnsafeDisregardVersion = false;
    private volatile boolean conversationAnswerNumberInChatEnabled = true;
    private volatile boolean supportPlaceholderApiInTranslationStrings = false;
    private volatile boolean userCommandGuiEnabled = true;
    private volatile boolean questPreviewGuiEnabled = true;
    private volatile String mainGuiName = "main-base";
    private volatile String npcGuiName = "npc-available-quests";
    private volatile boolean questAcceptedTitleEnabled = true;
    private volatile boolean questCompletedTitleEnabled = true;
    private volatile boolean questFailedTitleEnabled = true;
    private volatile boolean hideRewardsWithoutName = true;
    private volatile boolean showRewardsAfterQuestCompletion = true;
    private volatile boolean showRewardsAfterObjectiveCompletion = true;
    private volatile boolean showQuestItemAmount = false;
    private volatile boolean showObjectiveItemAmount = true;
    private volatile int questDescriptionMaxLineLength = 50;
    private volatile int objectiveDescriptionMaxLineLength = 50;
    private volatile boolean wrapLongWords = false;
    private volatile boolean moveEventEnabled = true;
    private volatile boolean objectiveUnlockConditionsCheckOnAnyAction = true;
    private volatile int objectiveUnlockConditionsCheckRegularIntervalSeconds = -1;
    private volatile boolean objectiveTrackingActionbarEnabled = true;
    private volatile boolean objectiveTrackingBossbarEnabled = true;
    private volatile int objectiveTrackingBossbarShowTimeSeconds = 10;
    private volatile boolean objectiveTrackingBossbarShowCompleted = false;
    private volatile boolean objectiveTrackingLocationCompassEnabled = false;
    private volatile String objectiveTrackingBeamMode = "end_gateway";
    private volatile boolean citizensFocusingEnabled = true;
    private volatile int citizensFocusingRotateTime = 14;
    private volatile boolean citizensFocusingCancelConversationWhenTooFar = true;
    private volatile boolean npcQuestGiverIndicatorParticleEnabled = true;
    private volatile String npcQuestGiverIndicatorParticleType = "ANGRY_VILLAGER";
    private volatile String npcQuestGiverIndicatorText = "";
    private volatile int npcQuestGiverIndicatorTextInterval = 100;
    private volatile int npcQuestGiverIndicatorParticleSpawnInterval = 10;
    private volatile int npcQuestGiverIndicatorParticleCount = 1;
    private volatile double npcQuestGiverIndicatorParticleDisableIfTpsBelow = -1;
    private volatile boolean armorStandPreventEditing = true;
    private volatile boolean armorStandQuestGiverIndicatorParticleEnabled = true;
    private volatile String armorStandQuestGiverIndicatorParticleType = "ANGRY_VILLAGER";
    private volatile int armorStandQuestGiverIndicatorParticleSpawnInterval = 10;
    private volatile int armorStandQuestGiverIndicatorParticleCount = 1;
    private volatile double armorStandQuestGiverIndicatorParticleDisableIfTpsBelow = -1;
    private volatile boolean commandHintActionbarEnabled = true;
    private volatile boolean commandHintTitleEnabled = false;
    private volatile boolean commandHintBossbarEnabled = false;
    private volatile int commandHintMaxPreviousArguments = 2;
    private volatile boolean questVisibilityEvaluationAlreadyAccepted = true;
    private volatile boolean questVisibilityEvaluationLimits = true;
    private volatile boolean questVisibilityEvaluationAcceptCooldown = false;
    private volatile boolean questVisibilityEvaluationConditions = false;
    private volatile String activeQuestListHorizontalSeparator = " | ";
    private volatile int activeQuestListHorizontalLimit = -1;
    private volatile int activeQuestListVerticalLimit = -1;
    private volatile boolean activeQuestListHorizontalUseDisplayName = true;
    private volatile boolean activeQuestListVerticalUseDisplayName = true;
    private volatile List<String> journalEnabledWorlds = List.of("");
    private volatile int journalInventorySlot = 8;
    private volatile JournalItem journalItem = JournalItem.defaultItem();
    private volatile Path file;
    private volatile YamlConfig yaml;
    private final Map<String, List<String>> tagColors = new ConcurrentHashMap<>();

    /** Opens general.yml and remembers the loaded file as this configuration's backing document. */
    public Loaded open(final Path dataFolder, final IO io, final Hooks hooks) {
        final Loaded loaded = loadOrCreate(dataFolder, io, hooks);
        if (loaded.loaded()) {
            file = loaded.file();
            yaml = loaded.configuration();
        }
        return loaded;
    }

    public YamlConfig yaml() {
        if (yaml == null) {
            throw new IllegalStateException("General config has not been loaded.");
        }
        return yaml;
    }

    public boolean loaded() {
        return yaml != null && file != null;
    }

    public boolean save(final IO io, final Hooks hooks) {
        return save(yaml(), file, io, hooks);
    }

    public void save() throws IOException {
        YamlConfig.save(yaml(), file);
    }

    /** Applies defaults and version bookkeeping, then loads every runtime setting from general.yml. */
    public Shared load(final YamlConfig yaml, final String pluginVersion) {
        final Defaults defaults = ensure(yaml);
        final Versions versions = ensureVersion(yaml, pluginVersion);
        loadFrom(yaml);
        return new Shared(
                defaults.changed() || versions.changed(),
                defaults.databaseConfigurationMissing() ? "Please specify your database information" : "");
    }

    /** Loads and applies the packet implementation settings after server compatibility is known. */
    public PacketMagic loadPacketMagic(
            final YamlConfig yaml,
            final String serverVersion,
            final String platformName) {
        final PacketMagic settings = packetMagic(yaml, serverVersion, platformName);
        applyPacketMagic(
                settings.packetMagic(),
                settings.usePacketEvents(),
                settings.unsafeDisregardVersion());
        return settings;
    }

    public void loadFrom(final YamlConfig configuration) {
        if (configuration == null) {
            return;
        }
        maxActiveQuestsPerPlayer = configuration.getInt("general.max-active-quests-per-player", -1);
        debugEnabled = configuration.getBoolean("debug", false);
        verboseStartupMessages =
                debugEnabled || configuration.getBoolean("logging.verbose-startup-messages", true);
        consoleColorsEnabled = configuration.getBoolean("visual.colors.console.enabled", true);
        consoleColorsDownsampled = configuration.getBoolean("visual.colors.console.downsampleColors", false);
        consolePrefixPrefix = configuration.getString(
                "visual.colors.console.prefix.prefix",
                "<#393e46>[<gradient:#E0EAFC:#CFDEF3>");
        consolePrefixSuffix = configuration.getString(
                "visual.colors.console.prefix.suffix",
                "<#393e46>]<#636c73>: ");
        consoleInfoDefault = configuration.getString(
                "visual.colors.console.info.default.normal", "<main>");
        consoleInfoDefaultDownsampled =
                configuration.getString("visual.colors.console.info.default.downsampled", "<gray>");
        consoleInfoData = configuration.getString(
                "visual.colors.console.info.data.normal", "<gradient:#1FA2FF:#12D8FA:#A6FFCB>");
        consoleInfoDataDownsampled =
                configuration.getString("visual.colors.console.info.data.downsampled", "<blue>");
        consoleInfoLanguage = configuration.getString(
                "visual.colors.console.info.language.normal", "<gradient:#AA076B:#61045F>");
        consoleInfoLanguageDownsampled =
                configuration.getString("visual.colors.console.info.language.downsampled", "<dark_purple>");
        consoleWarnDefault = configuration.getString(
                "visual.colors.console.warn.default.normal", "<warn>");
        consoleWarnDefaultDownsampled =
                configuration.getString("visual.colors.console.warn.default.downsampled", "<yellow>");
        consoleSevereDefault = configuration.getString(
                "visual.colors.console.severe.default.normal", "<error>");
        consoleSevereDefaultDownsampled =
                configuration.getString("visual.colors.console.severe.default.downsampled", "<red>");
        consoleDebugDefault = configuration.getString(
                "visual.colors.console.debug.default.normal", "<unimportant>");
        consoleDebugDefaultDownsampled =
                configuration.getString("visual.colors.console.debug.default.downsampled", "<dark_gray>");
        loadPlayerData = configuration.getBoolean("storage.load-playerdata", true);
        savePlayerData = configuration.getBoolean("storage.save-playerdata", true);
        loadPlayerDataOnJoin = configuration.getBoolean("storage.load-playerdata-on-join", true);
        savePlayerDataOnQuit = configuration.getBoolean("storage.save-playerdata-on-quit", true);
        backupQuestsOnShutdown =
                configuration.getBoolean("storage.backups.create-when-server-shuts-down", true);
        backupDatabaseBeforeLoad =
                configuration.getBoolean("storage.backups.create-for-database-before-database-loads", true);
        databaseEnabled = configuration.getBoolean("storage.database.enabled", false);
        databaseHost = configuration.getString("storage.database.host", "");
        databasePort = configuration.getInt("storage.database.port", 3306);
        databaseName = configuration.getString("storage.database.database", "");
        databaseUsername = configuration.getString("storage.database.username", "");
        databasePassword = configuration.getString("storage.database.password", "");
        integrationCitizensEnabled = configuration.getBoolean("integrations.citizens.enabled", true);
        integrationVaultEnabled = configuration.getBoolean("integrations.vault.enabled", true);
        integrationPlaceholderApiEnabled = configuration.getBoolean("integrations.placeholderapi.enabled", true);
        integrationMythicMobsEnabled = configuration.getBoolean("integrations.mythicmobs.enabled", true);
        integrationEliteMobsEnabled = configuration.getBoolean("integrations.elitemobs.enabled", true);
        integrationWorldEditEnabled = configuration.getBoolean("integrations.worldedit.enabled", true);
        integrationSlimefunEnabled = configuration.getBoolean("integrations.slimefun.enabled", true);
        integrationLuckPermsEnabled = configuration.getBoolean("integrations.luckperms.enabled", true);
        integrationUltimateClansEnabled = configuration.getBoolean("integrations.ultimateclans.enabled", true);
        integrationTownyEnabled = configuration.getBoolean("integrations.towny.enabled", true);
        integrationJobsRebornEnabled = configuration.getBoolean("integrations.jobs-reborn.enabled", true);
        integrationEcoMobsEnabled = configuration.getBoolean("integrations.ecoMobs.enabled", true);
        integrationBetonQuestEnabled = configuration.getBoolean("integrations.betonquest.enabled", true);
        integrationFloodgateEnabled = configuration.getBoolean("integrations.floodgate.enabled", true);
        updateCheckerNotifyOpsInChat =
                configuration.getBoolean("general.update-checker.notify-ops-in-chat", true);
        languageCode = configuration.getString("visual.language", "en-US");
        deletePreviousConversationMessages =
                configuration.getBoolean("general.packet-magic.conversations.delete-previous", true);
        previousConversationHistorySize =
                configuration.getInt("general.packet-magic.conversations.history-size", 20);
        packetMagicEnabled = configuration.getBoolean("general.packet-magic.enabled", true);
        packetMagicUsePacketEvents = "packetevents".equalsIgnoreCase(
                configuration.getString("general.packet-magic.mode", "internal"));
        packetMagicUnsafeDisregardVersion =
                configuration.getBoolean("general.packet-magic.unsafe-disregard-version", false);
        conversationAnswerNumberInChatEnabled = configuration.getBoolean(
                "conversations.interaction-handlers.clickable-text.allow-selecting-option-by-typing-number-in-chat",
                true);
        supportPlaceholderApiInTranslationStrings =
                configuration.getBoolean("placeholders.support_placeholderapi_in_translation_strings", false);
        moveEventEnabled = configuration.getBoolean("general.enable-move-event", true);
        objectiveUnlockConditionsCheckOnAnyAction =
                configuration.getBoolean("general.objectives.unlock-conditions-checks.any-action", true);
        objectiveUnlockConditionsCheckRegularIntervalSeconds =
                configuration.getInt("general.objectives.unlock-conditions-checks.regular-interval", -1);
        userCommandGuiEnabled = configuration.getBoolean("gui.usercommands.enabled", true);
        questPreviewGuiEnabled = configuration.getBoolean("gui.questpreview.enabled", true);
        mainGuiName = configuration.getString("gui.main-gui-name", "main-base");
        npcGuiName = configuration.getString("gui.npc-gui-name", "npc-available-quests");
        showQuestItemAmount = configuration.getBoolean("gui.show-quest-item-amount", false);
        showObjectiveItemAmount = configuration.getBoolean("gui.show-objective-item-amount", true);
        questDescriptionMaxLineLength = configuration.getInt("gui.quest-description-max-line-length", 50);
        objectiveDescriptionMaxLineLength =
                configuration.getInt("gui.objective-description-max-line-length", 50);
        wrapLongWords = configuration.getBoolean("gui.wrap-long-words", false);
        questAcceptedTitleEnabled =
                configuration.getBoolean("visual.titles.quest-successfully-accepted.enabled", true);
        questCompletedTitleEnabled = configuration.getBoolean("visual.titles.quest-completed.enabled", true);
        questFailedTitleEnabled = configuration.getBoolean("visual.titles.quest-failed.enabled", true);
        hideRewardsWithoutName = configuration.getBoolean("visual.hide-rewards-without-name", true);
        showRewardsAfterQuestCompletion =
                configuration.getBoolean("visual.show-rewards-after-quest-completion", true);
        showRewardsAfterObjectiveCompletion =
                configuration.getBoolean("visual.show-rewards-after-objective-completion", true);
        objectiveTrackingActionbarEnabled =
                configuration.getBoolean("visual.objective-tracking.actionbar.enabled", true);
        objectiveTrackingBossbarEnabled =
                configuration.getBoolean("visual.objective-tracking.bossbar.enabled", true);
        objectiveTrackingBossbarShowTimeSeconds =
                configuration.getInt("visual.objective-tracking.bossbar.show-time", 10);
        objectiveTrackingBossbarShowCompleted =
                configuration.getBoolean("visual.objective-tracking.bossbar.show-if-objective-is-completed", false);
        objectiveTrackingLocationCompassEnabled =
                configuration.getBoolean("visual.objective-tracking.location-compass.enabled", false);
        objectiveTrackingBeamMode =
                configuration.getString("visual.objective-tracking.beam-mode", "end_gateway");
        citizensFocusingEnabled =
                configuration.getBoolean("visual.citizensnpc.focusing.enabled", true);
        citizensFocusingRotateTime =
                configuration.getInt("visual.citizensnpc.focusing.rotate-time", 14);
        citizensFocusingCancelConversationWhenTooFar =
                configuration.getBoolean("visual.citizensnpc.focusing.cancel-conversation-when-leaving", true);
        npcQuestGiverIndicatorParticleEnabled =
                configuration.getBoolean("visual.citizensnpc.quest-giver-indicator-particle.enabled", true);
        npcQuestGiverIndicatorParticleType =
                configuration.getString("visual.citizensnpc.quest-giver-indicator-particle.type", "ANGRY_VILLAGER");
        npcQuestGiverIndicatorText =
                configuration.getString("visual.citizensnpc.quest-giver-indicator-above-name.text", "");
        npcQuestGiverIndicatorTextInterval =
                configuration.getInt("visual.citizensnpc.quest-giver-indicator-above-name.text-interval", 100);
        npcQuestGiverIndicatorParticleSpawnInterval =
                configuration.getInt("visual.citizensnpc.quest-giver-indicator-particle.spawn-interval", 10);
        npcQuestGiverIndicatorParticleCount =
                configuration.getInt("visual.citizensnpc.quest-giver-indicator-particle.count", 1);
        npcQuestGiverIndicatorParticleDisableIfTpsBelow =
                configuration.getDouble("visual.citizensnpc.quest-giver-indicator-particle.disable-if-tps-below", -1);
        armorStandPreventEditing = configuration.getBoolean("visual.armorstands.prevent-editing", true);
        armorStandQuestGiverIndicatorParticleEnabled =
                configuration.getBoolean("visual.armorstands.quest-giver-indicator-particle.enabled", true);
        armorStandQuestGiverIndicatorParticleType =
                configuration.getString("visual.armorstands.quest-giver-indicator-particle.type", "ANGRY_VILLAGER");
        armorStandQuestGiverIndicatorParticleSpawnInterval =
                configuration.getInt("visual.armorstands.quest-giver-indicator-particle.spawn-interval", 10);
        armorStandQuestGiverIndicatorParticleCount =
                configuration.getInt("visual.armorstands.quest-giver-indicator-particle.count", 1);
        armorStandQuestGiverIndicatorParticleDisableIfTpsBelow =
                configuration.getDouble("visual.armorstands.quest-giver-indicator-particle.disable-if-tps-below", -1);
        commandHintActionbarEnabled =
                configuration.getBoolean("visual.fancy-command-completion.actionbar-enabled", true);
        commandHintTitleEnabled =
                configuration.getBoolean("visual.fancy-command-completion.title-enabled", false);
        commandHintBossbarEnabled =
                configuration.getBoolean("visual.fancy-command-completion.bossbar-enabled", false);
        commandHintMaxPreviousArguments =
                configuration.getInt("visual.fancy-command-completion.max-previous-arguments-displayed", 2);
        questVisibilityEvaluationAlreadyAccepted =
                configuration.getBoolean("gui.quest-visibility-evaluations.already-accepted.enabled", true);
        questVisibilityEvaluationLimits =
                configuration.getBoolean("gui.quest-visibility-evaluations.max-accepts.enabled", true);
        questVisibilityEvaluationAcceptCooldown =
                configuration.getBoolean("gui.quest-visibility-evaluations.accept-cooldown.enabled", false);
        questVisibilityEvaluationConditions =
                configuration.getBoolean("gui.quest-visibility-evaluations.conditions.enabled", false);
        activeQuestListHorizontalSeparator =
                configuration.getString("placeholders.player_active_quests_list_horizontal.separator", " | ");
        activeQuestListHorizontalLimit =
                configuration.getInt("placeholders.player_active_quests_list_horizontal.limit", -1);
        activeQuestListVerticalLimit =
                configuration.getInt("placeholders.player_active_quests_list_vertical.limit", -1);
        activeQuestListHorizontalUseDisplayName = configuration.getBoolean(
                "placeholders.player_active_quests_list_horizontal.use-displayname-if-available",
                true);
        activeQuestListVerticalUseDisplayName = configuration.getBoolean(
                "placeholders.player_active_quests_list_vertical.use-displayname-if-available",
                true);
        journalEnabledWorlds = List.copyOf(configuration.getStringList("general.journal-item.enabled-worlds"));
        if (journalEnabledWorlds.isEmpty()) {
            journalEnabledWorlds = List.of("");
        }
        journalInventorySlot = configuration.getInt("general.journal-item.inventory-slot", 8);
        journalItem = JournalItem.read(
                configuration.get("general.journal-item.item"),
                configuration.contains("general.journal-item.item"));
        loadTagColors(configuration);
    }

    public int maxActiveQuestsPerPlayer() {
        return maxActiveQuestsPerPlayer;
    }

    public boolean debugEnabled() {
        return debugEnabled;
    }

    public boolean verboseStartupMessages() {
        return verboseStartupMessages;
    }

    public boolean consoleColorsEnabled() {
        return consoleColorsEnabled;
    }

    public boolean consoleColorsDownsampled() {
        return consoleColorsDownsampled;
    }

    public String consolePrefixPrefix() {
        return consolePrefixPrefix;
    }

    public String consolePrefixSuffix() {
        return consolePrefixSuffix;
    }

    public String consoleInfoColor(final LogCategory category) {
        final LogCategory finalCategory = category == null ? LogCategory.DEFAULT : category;
        return switch (finalCategory) {
            case DATA -> consoleColorsDownsampled ? consoleInfoDataDownsampled : consoleInfoData;
            case LANGUAGE -> consoleColorsDownsampled ? consoleInfoLanguageDownsampled : consoleInfoLanguage;
            case DEFAULT -> consoleColorsDownsampled ? consoleInfoDefaultDownsampled : consoleInfoDefault;
        };
    }

    public String consoleWarnColor() {
        return consoleColorsDownsampled ? consoleWarnDefaultDownsampled : consoleWarnDefault;
    }

    public String consoleSevereColor() {
        return consoleColorsDownsampled ? consoleSevereDefaultDownsampled : consoleSevereDefault;
    }

    public String consoleDebugColor() {
        return consoleColorsDownsampled ? consoleDebugDefaultDownsampled : consoleDebugDefault;
    }

    public boolean loadPlayerData() {
        return loadPlayerData;
    }

    public boolean savePlayerData() {
        return savePlayerData;
    }

    public boolean loadPlayerDataOnJoin() {
        return loadPlayerDataOnJoin;
    }

    public boolean savePlayerDataOnQuit() {
        return savePlayerDataOnQuit;
    }

    public boolean backupQuestsOnShutdown() {
        return backupQuestsOnShutdown;
    }

    public boolean backupDatabaseBeforeLoad() {
        return backupDatabaseBeforeLoad;
    }

    public boolean databaseEnabled() {
        return databaseEnabled;
    }

    public String databaseHost() {
        return databaseHost;
    }

    public int databasePort() {
        return databasePort;
    }

    public String databaseName() {
        return databaseName;
    }

    public String databaseUsername() {
        return databaseUsername;
    }

    public String databasePassword() {
        return databasePassword;
    }

    public boolean integrationCitizensEnabled() {
        return integrationCitizensEnabled;
    }

    public boolean integrationVaultEnabled() {
        return integrationVaultEnabled;
    }

    public boolean integrationPlaceholderApiEnabled() {
        return integrationPlaceholderApiEnabled;
    }

    public boolean integrationMythicMobsEnabled() {
        return integrationMythicMobsEnabled;
    }

    public boolean integrationEliteMobsEnabled() {
        return integrationEliteMobsEnabled;
    }

    public boolean integrationWorldEditEnabled() {
        return integrationWorldEditEnabled;
    }

    public boolean integrationSlimefunEnabled() {
        return integrationSlimefunEnabled;
    }

    public boolean integrationLuckPermsEnabled() {
        return integrationLuckPermsEnabled;
    }

    public boolean integrationUltimateClansEnabled() {
        return integrationUltimateClansEnabled;
    }

    public boolean integrationTownyEnabled() {
        return integrationTownyEnabled;
    }

    public boolean integrationJobsRebornEnabled() {
        return integrationJobsRebornEnabled;
    }

    public boolean integrationEcoMobsEnabled() {
        return integrationEcoMobsEnabled;
    }

    public boolean integrationBetonQuestEnabled() {
        return integrationBetonQuestEnabled;
    }

    public boolean integrationFloodgateEnabled() {
        return integrationFloodgateEnabled;
    }

    public boolean updateCheckerNotifyOpsInChat() {
        return updateCheckerNotifyOpsInChat;
    }

    public String languageCode() {
        return languageCode;
    }

    public boolean deletePreviousConversationMessages() {
        return deletePreviousConversationMessages;
    }

    public void disablePreviousConversationMessageDeletion() {
        deletePreviousConversationMessages = false;
    }

    public int previousConversationHistorySize() {
        return previousConversationHistorySize;
    }

    public boolean packetMagicEnabled() {
        return packetMagicEnabled;
    }

    public boolean packetMagicUsePacketEvents() {
        return packetMagicUsePacketEvents;
    }

    public boolean packetMagicUnsafeDisregardVersion() {
        return packetMagicUnsafeDisregardVersion;
    }

    public void applyPacketMagic(
            final boolean enabled,
            final boolean usePacketEvents,
            final boolean unsafeDisregardVersion) {
        packetMagicEnabled = enabled;
        packetMagicUsePacketEvents = usePacketEvents;
        packetMagicUnsafeDisregardVersion = unsafeDisregardVersion;
    }

    public void disablePacketMagic() {
        packetMagicEnabled = false;
        deletePreviousConversationMessages = false;
    }

    public boolean conversationAnswerNumberInChatEnabled() {
        return conversationAnswerNumberInChatEnabled;
    }

    public boolean supportPlaceholderApiInTranslationStrings() {
        return supportPlaceholderApiInTranslationStrings;
    }

    public boolean userCommandGuiEnabled() {
        return userCommandGuiEnabled;
    }

    public boolean questPreviewGuiEnabled() {
        return questPreviewGuiEnabled;
    }

    public String mainGuiName() {
        return mainGuiName;
    }

    public String npcGuiName() {
        return npcGuiName;
    }

    public boolean questAcceptedTitleEnabled() {
        return questAcceptedTitleEnabled;
    }

    public boolean questCompletedTitleEnabled() {
        return questCompletedTitleEnabled;
    }

    public boolean questFailedTitleEnabled() {
        return questFailedTitleEnabled;
    }

    public boolean hideRewardsWithoutName() {
        return hideRewardsWithoutName;
    }

    public boolean showRewardsAfterQuestCompletion() {
        return showRewardsAfterQuestCompletion;
    }

    public boolean showRewardsAfterObjectiveCompletion() {
        return showRewardsAfterObjectiveCompletion;
    }

    public boolean showQuestItemAmount() {
        return showQuestItemAmount;
    }

    public boolean showObjectiveItemAmount() {
        return showObjectiveItemAmount;
    }

    public int questDescriptionMaxLineLength() {
        return questDescriptionMaxLineLength;
    }

    public int objectiveDescriptionMaxLineLength() {
        return objectiveDescriptionMaxLineLength;
    }

    public boolean wrapLongWords() {
        return wrapLongWords;
    }

    public boolean moveEventEnabled() {
        return moveEventEnabled;
    }

    public boolean objectiveUnlockConditionsCheckOnAnyAction() {
        return objectiveUnlockConditionsCheckOnAnyAction;
    }

    public int objectiveUnlockConditionsCheckRegularIntervalSeconds() {
        return objectiveUnlockConditionsCheckRegularIntervalSeconds;
    }

    public boolean objectiveTrackingActionbarEnabled() {
        return objectiveTrackingActionbarEnabled;
    }

    public boolean objectiveTrackingBossbarEnabled() {
        return objectiveTrackingBossbarEnabled;
    }

    public int objectiveTrackingBossbarShowTimeSeconds() {
        return objectiveTrackingBossbarShowTimeSeconds;
    }

    public boolean objectiveTrackingBossbarShowCompleted() {
        return objectiveTrackingBossbarShowCompleted;
    }

    public boolean objectiveTrackingLocationCompassEnabled() {
        return objectiveTrackingLocationCompassEnabled;
    }

    public String objectiveTrackingBeamMode() {
        return objectiveTrackingBeamMode;
    }

    public boolean citizensFocusingEnabled() {
        return citizensFocusingEnabled;
    }

    public int citizensFocusingRotateTime() {
        return citizensFocusingRotateTime;
    }

    public boolean citizensFocusingCancelConversationWhenTooFar() {
        return citizensFocusingCancelConversationWhenTooFar;
    }

    public boolean npcQuestGiverIndicatorParticleEnabled() {
        return npcQuestGiverIndicatorParticleEnabled;
    }

    public String npcQuestGiverIndicatorParticleType() {
        return npcQuestGiverIndicatorParticleType;
    }

    public String npcQuestGiverIndicatorText() {
        return npcQuestGiverIndicatorText;
    }

    public int npcQuestGiverIndicatorTextInterval() {
        return npcQuestGiverIndicatorTextInterval;
    }

    public int npcQuestGiverIndicatorParticleSpawnInterval() {
        return npcQuestGiverIndicatorParticleSpawnInterval;
    }

    public int npcQuestGiverIndicatorParticleCount() {
        return npcQuestGiverIndicatorParticleCount;
    }

    public double npcQuestGiverIndicatorParticleDisableIfTpsBelow() {
        return npcQuestGiverIndicatorParticleDisableIfTpsBelow;
    }

    public boolean npcQuestGiverIndicatorCanSpawnAtTps(final double currentTps) {
        return npcQuestGiverIndicatorParticleDisableIfTpsBelow < 0
                || currentTps >= npcQuestGiverIndicatorParticleDisableIfTpsBelow;
    }

    public boolean armorStandPreventEditing() {
        return armorStandPreventEditing;
    }

    public boolean armorStandQuestGiverIndicatorParticleEnabled() {
        return armorStandQuestGiverIndicatorParticleEnabled;
    }

    public String armorStandQuestGiverIndicatorParticleType() {
        return armorStandQuestGiverIndicatorParticleType;
    }

    public int armorStandQuestGiverIndicatorParticleSpawnInterval() {
        return armorStandQuestGiverIndicatorParticleSpawnInterval;
    }

    public int armorStandQuestGiverIndicatorParticleCount() {
        return armorStandQuestGiverIndicatorParticleCount;
    }

    public double armorStandQuestGiverIndicatorParticleDisableIfTpsBelow() {
        return armorStandQuestGiverIndicatorParticleDisableIfTpsBelow;
    }

    public boolean armorStandQuestGiverIndicatorCanSpawnAtTps(final double currentTps) {
        return armorStandQuestGiverIndicatorParticleDisableIfTpsBelow < 0
                || currentTps >= armorStandQuestGiverIndicatorParticleDisableIfTpsBelow;
    }

    public boolean commandHintActionbarEnabled() {
        return commandHintActionbarEnabled;
    }

    public boolean commandHintTitleEnabled() {
        return commandHintTitleEnabled;
    }

    public boolean commandHintBossbarEnabled() {
        return commandHintBossbarEnabled;
    }

    public int commandHintMaxPreviousArguments() {
        return commandHintMaxPreviousArguments;
    }

    public boolean questVisibilityEvaluationAlreadyAccepted() {
        return questVisibilityEvaluationAlreadyAccepted;
    }

    public boolean questVisibilityEvaluationLimits() {
        return questVisibilityEvaluationLimits;
    }

    public boolean questVisibilityEvaluationAcceptCooldown() {
        return questVisibilityEvaluationAcceptCooldown;
    }

    public boolean questVisibilityEvaluationConditions() {
        return questVisibilityEvaluationConditions;
    }

    public String activeQuestListHorizontalSeparator() {
        return activeQuestListHorizontalSeparator;
    }

    public int activeQuestListHorizontalLimit() {
        return activeQuestListHorizontalLimit;
    }

    public int activeQuestListVerticalLimit() {
        return activeQuestListVerticalLimit;
    }

    public boolean activeQuestListHorizontalUseDisplayName() {
        return activeQuestListHorizontalUseDisplayName;
    }

    public boolean activeQuestListVerticalUseDisplayName() {
        return activeQuestListVerticalUseDisplayName;
    }

    public List<String> journalEnabledWorlds() {
        return journalEnabledWorlds;
    }

    public int journalInventorySlot() {
        return journalInventorySlot;
    }

    public JournalItem journalItem() {
        return journalItem;
    }

    public JournalItem useDefaultJournalItem() {
        journalItem = JournalItem.defaultItem();
        return journalItem;
    }

    /** The complete, platform-neutral journal description used by every adapter. */
    public record JournalItem(
            String material,
            int amount,
            int damage,
            String displayName,
            List<String> lore,
            Integer customModelData,
            Map<String, Integer> enchantments,
            Map<String, Integer> storedEnchantments,
            List<String> hiddenComponents,
            boolean unbreakable,
            boolean glint,
            Object configuredValue,
            boolean configured,
            boolean valid) {
        private static final String DEFAULT_MATERIAL = "ENCHANTED_BOOK";
        private static final String DEFAULT_NAME = "<blue><italic>Journal";
        private static final List<String> DEFAULT_LORE =
                List.of("<gray>A book containing all your quest information");

        public JournalItem {
            material = material == null ? "" : material;
            amount = Math.max(1, amount);
            damage = Math.max(0, damage);
            displayName = displayName == null ? "" : displayName;
            lore = lore == null ? List.of() : List.copyOf(lore);
            enchantments = enchantments == null ? Map.of() : Map.copyOf(enchantments);
            storedEnchantments = storedEnchantments == null ? Map.of() : Map.copyOf(storedEnchantments);
            hiddenComponents = hiddenComponents == null ? List.of() : List.copyOf(hiddenComponents);
        }

        public static JournalItem defaultItem() {
            return new JournalItem(
                    DEFAULT_MATERIAL, 1, 0, DEFAULT_NAME, DEFAULT_LORE, null,
                    Map.of(), Map.of(), List.of(), false, false,
                    null, false, true);
        }

        private static JournalItem read(final Object value, final boolean configured) {
            if (!configured) {
                return defaultItem();
            }
            if (!(value instanceof Map<?, ?> map)) {
                return invalid(value);
            }
            final Object rawMaterial = map.get("material");
            final String material = rawMaterial == null ? "" : rawMaterial.toString().trim();
            if (material.isEmpty()) {
                return invalid(value);
            }
            final int amount = map.get("amount") instanceof Number number ? number.intValue() : 1;
            final int damage = map.get("damage") instanceof Number number ? number.intValue() : 0;
            if (amount < 1 || damage < 0) {
                return invalid(value);
            }
            final String name = map.get("display-name") == null
                    ? ""
                    : map.get("display-name").toString();
            final List<String> lore = map.get("lore") instanceof List<?> lines
                    ? lines.stream().map(String::valueOf).toList()
                    : List.of();
            final Integer customModelData = map.get("custom-model-data") instanceof Number number
                    ? number.intValue()
                    : null;
            final Map<String, Integer> enchantments = enchantments(map.get("enchantments"));
            final Map<String, Integer> storedEnchantments = enchantments(map.get("stored-enchantments"));
            final List<String> hiddenComponents = map.get("hidden-components") instanceof List<?> values
                    ? values.stream().map(String::valueOf).toList()
                    : List.of();
            final boolean unbreakable = map.get("unbreakable") instanceof Boolean enabled && enabled;
            final boolean glint = map.get("glint") instanceof Boolean enabled && enabled;
            return new JournalItem(
                    material,
                    amount,
                    damage,
                    name,
                    lore,
                    customModelData,
                    enchantments,
                    storedEnchantments,
                    hiddenComponents,
                    unbreakable,
                    glint,
                    value,
                    true,
                    true);
        }

        private static JournalItem invalid(final Object value) {
            final JournalItem fallback = defaultItem();
            return new JournalItem(
                    fallback.material,
                    fallback.amount,
                    fallback.damage,
                    fallback.displayName,
                    fallback.lore,
                    fallback.customModelData,
                    fallback.enchantments,
                    fallback.storedEnchantments,
                    fallback.hiddenComponents,
                    fallback.unbreakable,
                    fallback.glint,
                    value,
                    true,
                    false);
        }

        private static Map<String, Integer> enchantments(final Object value) {
            if (!(value instanceof Map<?, ?> map)) {
                return Map.of();
            }
            final Map<String, Integer> result = new LinkedHashMap<>();
            for (final Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getValue() instanceof Number level && level.intValue() > 0) {
                    result.put(String.valueOf(entry.getKey()), level.intValue());
                }
            }
            return Map.copyOf(result);
        }
    }

    @Override
    public List<String> colors(final String tagName) {
        if (tagName == null || tagName.isBlank()) {
            return List.of();
        }
        return tagColors.getOrDefault(normalizeTag(tagName), List.of());
    }

    private void loadTagColors(final YamlConfig configuration) {
        tagColors.clear();
        for (final String tag : NotQuestsMiniMessage.CUSTOM_TAGS) {
            final List<String> colors = configuration.getStringList("visual.colors.tags." + configTagName(tag));
            if (!colors.isEmpty()) {
                tagColors.put(normalizeTag(tag), List.copyOf(colors));
            }
        }
    }

    private static String normalizeTag(final String tagName) {
        return tagName.replace("_", "").replace("-", "").toLowerCase(Locale.ROOT);
    }

    private static String configTagName(final String tagName) {
        return "veryunimportant".equalsIgnoreCase(tagName) ? "veryUnimportant" : tagName;
    }

    public static void copyMissingDefaults(final Path dataFolder) throws IOException {
        copyMissingGeneral(dataFolder);
        LanguageManager.copyMissingLanguages(dataFolder);
        CategoryFiles.copyMissingDefaultCategory(dataFolder);
        GuiService.copyMissingDefaults(dataFolder);
    }

    public static void copyMissingGeneral(final Path dataFolder) throws IOException {
        Files.createDirectories(dataFolder);
        copyMissingResource("general.yml", dataFolder.resolve("general.yml"));
    }

    public static String bundledResourceText(final String path) {
        return resourceText(path);
    }

    private static void copyMissingResource(final String resourcePath, final Path target) throws IOException {
        if (Files.exists(target)) {
            return;
        }
        Files.createDirectories(target.getParent());
        Files.writeString(target, resourceText(resourcePath), StandardCharsets.UTF_8);
    }

    private static String resourceText(final String path) {
        try (InputStream input = ConfigurationManager.class.getClassLoader().getResourceAsStream(path)) {
            if (input == null) {
                throw new IllegalStateException("Missing bundled NotQuests resource: " + path);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (final IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    public static Defaults ensure(final YamlConfig configuration) {
        if (configuration == null) {
            return new Defaults(false, false);
        }
        final DefaultsBuilder defaults = new DefaultsBuilder();

        bool(configuration, defaults, "debug", false);
        database(configuration, defaults);

        bool(configuration, defaults, "storage.load-playerdata", true);
        bool(configuration, defaults, "storage.save-playerdata", true);
        bool(configuration, defaults, "storage.load-playerdata-on-join", true);
        bool(configuration, defaults, "storage.save-playerdata-on-quit", true);
        bool(configuration, defaults, "storage.backups.create-when-server-shuts-down", true);
        bool(configuration, defaults, "storage.backups.create-for-database-before-database-loads", true);

        integer(configuration, defaults, "general.max-active-quests-per-player", -1);
        string(configuration, defaults, "visual.language", "en-US");
        bool(configuration, defaults, "visual.hide-rewards-without-name", true);
        bool(configuration, defaults, "visual.show-rewards-after-quest-completion", true);
        bool(configuration, defaults, "visual.show-rewards-after-objective-completion", true);
        bool(configuration, defaults, "visual.titles.quest-successfully-accepted.enabled", true);
        bool(configuration, defaults, "visual.titles.quest-failed.enabled", true);
        bool(configuration, defaults, "visual.titles.quest-completed.enabled", true);

        bool(configuration, defaults, "visual.objective-tracking.actionbar.enabled", true);
        bool(configuration, defaults, "visual.objective-tracking.bossbar.enabled", true);
        integer(configuration, defaults, "visual.objective-tracking.bossbar.show-time", 10);
        bool(configuration, defaults, "visual.objective-tracking.bossbar.show-if-objective-is-completed", false);
        bool(configuration, defaults, "visual.objective-tracking.location-compass.enabled", false);
        string(configuration, defaults, "visual.objective-tracking.beam-mode", "end_gateway");
        bool(configuration, defaults, "visual.citizensnpc.focusing.enabled", true);
        integer(configuration, defaults, "visual.citizensnpc.focusing.rotate-time", 14);
        bool(configuration, defaults, "visual.citizensnpc.focusing.cancel-conversation-when-leaving", true);
        bool(configuration, defaults, "visual.citizensnpc.quest-giver-indicator-particle.enabled", true);
        string(configuration, defaults, "visual.citizensnpc.quest-giver-indicator-particle.type", "ANGRY_VILLAGER");
        string(configuration, defaults, "visual.citizensnpc.quest-giver-indicator-above-name.text", "");
        integer(configuration, defaults, "visual.citizensnpc.quest-giver-indicator-above-name.text-interval", 100);
        integer(configuration, defaults, "visual.citizensnpc.quest-giver-indicator-particle.spawn-interval", 10);
        integer(configuration, defaults, "visual.citizensnpc.quest-giver-indicator-particle.count", 1);
        decimal(configuration, defaults, "visual.citizensnpc.quest-giver-indicator-particle.disable-if-tps-below", -1d);
        bool(configuration, defaults, "visual.armorstands.prevent-editing", true);
        bool(configuration, defaults, "visual.armorstands.quest-giver-indicator-particle.enabled", true);
        string(configuration, defaults, "visual.armorstands.quest-giver-indicator-particle.type", "ANGRY_VILLAGER");
        integer(configuration, defaults, "visual.armorstands.quest-giver-indicator-particle.spawn-interval", 10);
        integer(configuration, defaults, "visual.armorstands.quest-giver-indicator-particle.count", 1);
        decimal(configuration, defaults, "visual.armorstands.quest-giver-indicator-particle.disable-if-tps-below", -1d);

        bool(configuration, defaults, "gui.quest-visibility-evaluations.already-accepted.enabled", true);
        bool(configuration, defaults, "gui.quest-visibility-evaluations.max-accepts.enabled", true);
        bool(configuration, defaults, "gui.quest-visibility-evaluations.accept-cooldown.enabled", false);
        bool(configuration, defaults, "gui.quest-visibility-evaluations.conditions.enabled", false);
        string(configuration, defaults, "gui.npc-gui-name", "npc-available-quests");
        string(configuration, defaults, "gui.main-gui-name", "main-base");
        bool(configuration, defaults, "gui.questpreview.enabled", true);
        bool(configuration, defaults, "gui.questpreview.description.enabled", true);
        bool(configuration, defaults, "gui.questpreview.rewards.enabled", true);
        bool(configuration, defaults, "gui.questpreview.requirements.enabled", true);
        bool(configuration, defaults, "gui.show-quest-item-amount", false);
        bool(configuration, defaults, "gui.show-objective-item-amount", true);
        integer(configuration, defaults, "gui.quest-description-max-line-length", 50);
        integer(configuration, defaults, "gui.objective-description-max-line-length", 50);
        bool(configuration, defaults, "gui.wrap-long-words", false);
        bool(configuration, defaults, "gui.usercommands.enabled", true);

        string(configuration, defaults, "placeholders.player_active_quests_list_horizontal.separator", " | ");
        integer(configuration, defaults, "placeholders.player_active_quests_list_horizontal.limit", -1);
        integer(configuration, defaults, "placeholders.player_active_quests_list_vertical.limit", -1);
        bool(configuration, defaults, "placeholders.player_active_quests_list_horizontal.use-displayname-if-available", true);
        bool(configuration, defaults, "placeholders.player_active_quests_list_vertical.use-displayname-if-available", true);

        bool(configuration, defaults, "visual.fancy-command-completion.actionbar-enabled", true);
        bool(configuration, defaults, "visual.fancy-command-completion.title-enabled", false);
        bool(configuration, defaults, "visual.fancy-command-completion.bossbar-enabled", false);
        integer(configuration, defaults, "visual.fancy-command-completion.max-previous-arguments-displayed", 2);

        bool(configuration, defaults, "general.enable-move-event", true);
        stringList(configuration, defaults, "general.journal-item.enabled-worlds", List.of(""));
        integer(configuration, defaults, "general.journal-item.inventory-slot", 8);
        bool(configuration, defaults, "general.objectives.unlock-conditions-checks.any-action", true);
        integer(configuration, defaults, "general.objectives.unlock-conditions-checks.regular-interval", -1);
        bool(configuration, defaults, "logging.verbose-startup-messages", true);
        bool(configuration, defaults, "general.packet-magic.enabled", true);
        string(configuration, defaults, "general.packet-magic.mode", "internal");
        bool(configuration, defaults, "general.packet-magic.unsafe-disregard-version", false);
        bool(configuration, defaults, "general.packet-magic.conversations.delete-previous", true);
        integer(configuration, defaults, "general.packet-magic.conversations.history-size", 20);
        bool(configuration, defaults, "general.update-checker.notify-ops-in-chat", true);
        bool(configuration, defaults,
                "conversations.interaction-handlers.clickable-text.allow-selecting-option-by-typing-number-in-chat",
                true);
        bool(configuration, defaults, "placeholders.support_placeholderapi_in_translation_strings", false);

        consoleColors(configuration, defaults);
        colorTags(configuration, defaults);
        integrations(configuration, defaults);

        return defaults.build();
    }

    private static void database(final YamlConfig configuration, final DefaultsBuilder defaults) {
        boolean missingDatabaseConfiguration = false;
        if (!configuration.isBoolean("storage.database.enabled")) {
            configuration.set("storage.database.enabled", false);
            defaults.changed = true;
        }
        if (!configuration.isString("storage.database.host")) {
            configuration.set("storage.database.host", "");
            missingDatabaseConfiguration = true;
            defaults.changed = true;
        }
        if (!configuration.isInt("storage.database.port")) {
            configuration.set("storage.database.port", 3306);
            defaults.changed = true;
        }
        if (!configuration.isString("storage.database.database")) {
            configuration.set("storage.database.database", "");
            missingDatabaseConfiguration = true;
            defaults.changed = true;
        }
        if (!configuration.isString("storage.database.username")) {
            configuration.set("storage.database.username", "");
            missingDatabaseConfiguration = true;
            defaults.changed = true;
        }
        if (!configuration.isString("storage.database.password")) {
            configuration.set("storage.database.password", "");
            missingDatabaseConfiguration = true;
            defaults.changed = true;
        }
        defaults.databaseConfigurationMissing =
                configuration.getBoolean("storage.database.enabled", false) && missingDatabaseConfiguration;
    }

    private static void integrations(final YamlConfig configuration, final DefaultsBuilder defaults) {
        bool(configuration, defaults, "integrations.citizens.enabled", true);
        bool(configuration, defaults, "integrations.vault.enabled", true);
        bool(configuration, defaults, "integrations.placeholderapi.enabled", true);
        bool(configuration, defaults, "integrations.mythicmobs.enabled", true);
        bool(configuration, defaults, "integrations.elitemobs.enabled", true);
        bool(configuration, defaults, "integrations.worldedit.enabled", true);
        bool(configuration, defaults, "integrations.slimefun.enabled", true);
        bool(configuration, defaults, "integrations.luckperms.enabled", true);
        bool(configuration, defaults, "integrations.ultimateclans.enabled", true);
        bool(configuration, defaults, "integrations.towny.enabled", true);
        bool(configuration, defaults, "integrations.jobs-reborn.enabled", true);
        bool(configuration, defaults, "integrations.ecoMobs.enabled", true);
        bool(configuration, defaults, "integrations.betonquest.enabled", true);
        bool(configuration, defaults, "integrations.floodgate.enabled", true);
    }

    private static void consoleColors(final YamlConfig configuration, final DefaultsBuilder defaults) {
        bool(configuration, defaults, "visual.colors.console.enabled", true);
        bool(configuration, defaults, "visual.colors.console.downsampleColors", false);
        string(configuration, defaults, "visual.colors.console.prefix.prefix", "<#393e46>[<gradient:#E0EAFC:#CFDEF3>");
        string(configuration, defaults, "visual.colors.console.prefix.suffix", "<#393e46>]<#636c73>: ");
        string(configuration, defaults, "visual.colors.console.info.default.normal", "<main>");
        string(configuration, defaults, "visual.colors.console.info.default.downsampled", "<gray>");
        string(configuration, defaults, "visual.colors.console.info.data.normal", "<gradient:#1FA2FF:#12D8FA:#A6FFCB>");
        string(configuration, defaults, "visual.colors.console.info.data.downsampled", "<blue>");
        string(configuration, defaults, "visual.colors.console.info.language.normal", "<gradient:#AA076B:#61045F>");
        string(configuration, defaults, "visual.colors.console.info.language.downsampled", "<dark_purple>");
        string(configuration, defaults, "visual.colors.console.warn.default.normal", "<warn>");
        string(configuration, defaults, "visual.colors.console.warn.default.downsampled", "<yellow>");
        string(configuration, defaults, "visual.colors.console.severe.default.normal", "<error>");
        string(configuration, defaults, "visual.colors.console.severe.default.downsampled", "<red>");
        string(configuration, defaults, "visual.colors.console.debug.default.normal", "<unimportant>");
        string(configuration, defaults, "visual.colors.console.debug.default.downsampled", "<dark_gray>");
    }

    private static void colorTags(final YamlConfig configuration, final DefaultsBuilder defaults) {
        stringList(configuration, defaults, "visual.colors.tags.main", List.of("#1985ff", "#2bc7ff"));
        stringList(configuration, defaults, "visual.colors.tags.highlight", List.of("#00fffb", "#00ffc3"));
        stringList(configuration, defaults, "visual.colors.tags.highlight2", List.of("#ff2465", "#ff24a0"));
        stringList(configuration, defaults, "visual.colors.tags.error", List.of("#ff004c", "#a80000"));
        stringList(configuration, defaults, "visual.colors.tags.success", List.of("#54b2ff", "#ff5ecc"));
        stringList(configuration, defaults, "visual.colors.tags.unimportant", List.of("#9c9c9c", "#858383"));
        stringList(configuration, defaults, "visual.colors.tags.veryUnimportant", List.of("#5c5c5c", "#454545"));
        stringList(configuration, defaults, "visual.colors.tags.warn", List.of("#fff700", "#ffa629"));
        stringList(configuration, defaults, "visual.colors.tags.positive", List.of("#73ff00", "#00ffd0"));
        stringList(configuration, defaults, "visual.colors.tags.negative", List.of("#ff006f", "#ff002f"));
    }

    private static void string(
            final YamlConfig configuration,
            final DefaultsBuilder defaults,
            final String key,
            final String defaultValue) {
        if (!configuration.isString(key)) {
            configuration.set(key, defaultValue);
            defaults.changed = true;
        }
        configuration.setComments(key, List.of("Default: " + defaultValue));
    }

    private static void bool(
            final YamlConfig configuration,
            final DefaultsBuilder defaults,
            final String key,
            final boolean defaultValue) {
        if (!configuration.isBoolean(key)) {
            configuration.set(key, defaultValue);
            defaults.changed = true;
        }
        configuration.setComments(key, List.of("Default: " + defaultValue));
    }

    private static void integer(
            final YamlConfig configuration,
            final DefaultsBuilder defaults,
            final String key,
            final int defaultValue) {
        if (!configuration.isInt(key)) {
            configuration.set(key, defaultValue);
            defaults.changed = true;
        }
        configuration.setComments(key, List.of("Default: " + defaultValue));
    }

    private static void decimal(
            final YamlConfig configuration,
            final DefaultsBuilder defaults,
            final String key,
            final double defaultValue) {
        if (!configuration.isDouble(key)) {
            configuration.set(key, defaultValue);
            defaults.changed = true;
        }
        configuration.setComments(key, List.of("Default: " + defaultValue));
    }

    private static void stringList(
            final YamlConfig configuration,
            final DefaultsBuilder defaults,
            final String key,
            final List<String> defaultValue) {
        if (!configuration.isList(key)) {
            configuration.set(key, defaultValue);
            defaults.changed = true;
        }
        configuration.setComments(key, List.of("Default: " + defaultValue));
    }

    private static Loaded loadOrCreate(
            final Path dataFolder,
            final IO io,
            final Hooks hooks) {
        final Path folder = Objects.requireNonNull(dataFolder, "dataFolder");
        final IO fileIo = Objects.requireNonNull(io, "io");
        final Hooks callbacks = hooks == null ? Hooks.NO_OP : hooks;
        final Path file = folder.resolve("general.yml");

        if (!ensureDataFolder(folder, callbacks)) {
            return Loaded.failed(file);
        }

        final boolean created = Files.notExists(file);
        if (created) {
            callbacks.info("General ConfigurationManager (general.yml) does not exist. Creating a new one...");
            try {
                callbacks.info("Loading default <highlight>general.yml</highlight>...");
                copyMissingGeneral(folder);
            } catch (final IOException exception) {
                callbacks.disableSaving("There was an error creating the general.yml config file. (2)", exception);
                return Loaded.failed(file);
            }
        }

        try {
            return new Loaded(file, fileIo.load(file), true, created);
        } catch (final IOException exception) {
            callbacks.disableSaving(
                    "There was an error loading the general configuration file. It either doesn't exist or is invalid.",
                    exception);
            return Loaded.failed(file);
        }
    }

    public static boolean ensureDataFolder(final Path dataFolder, final DataFolderHooks hooks) {
        final Path folder = Objects.requireNonNull(dataFolder, "dataFolder");
        final DataFolderHooks callbacks = hooks == null ? DataFolderHooks.NO_OP : hooks;
        if (Files.exists(folder)) {
            return true;
        }
        callbacks.info("Data Folder not found. Creating a new one...");
        try {
            Files.createDirectories(folder);
            return true;
        } catch (final IOException exception) {
            callbacks.disableSaving("There was an error creating the NotQuests data folder.", exception);
            return false;
        }
    }

    private static boolean save(
            final YamlConfig configuration,
            final Path file,
            final IO io,
            final Hooks hooks) {
        final IO fileIo = Objects.requireNonNull(io, "io");
        final Hooks callbacks = hooks == null ? Hooks.NO_OP : hooks;
        if (configuration == null || file == null) {
            callbacks.warn("General Config file could not be saved.");
            return false;
        }
        try {
            fileIo.save(configuration, file);
            return true;
        } catch (final IOException exception) {
            callbacks.warn("General Config file could not be saved.");
            return false;
        }
    }

    public static Value<String> string(
            final YamlConfig configuration,
            final String key,
            final String defaultValue,
            final String... commentLines) {
        if (!configuration.isString(key)) {
            configuration.set(key, defaultValue);
            setDefaultComment(configuration, key, defaultValue, commentLines);
            return new Value<>(configuration.getString(key), true);
        }
        setDefaultComment(configuration, key, defaultValue, commentLines);
        return new Value<>(configuration.getString(key), false);
    }

    public static Value<Boolean> bool(
            final YamlConfig configuration,
            final String key,
            final boolean defaultValue,
            final String... commentLines) {
        if (!configuration.isBoolean(key)) {
            configuration.set(key, defaultValue);
            setDefaultComment(configuration, key, defaultValue, commentLines);
            return new Value<>(configuration.getBoolean(key), true);
        }
        setDefaultComment(configuration, key, defaultValue, commentLines);
        return new Value<>(configuration.getBoolean(key), false);
    }

    public static Value<Integer> integer(
            final YamlConfig configuration,
            final String key,
            final int defaultValue,
            final String... commentLines) {
        if (!configuration.isInt(key)) {
            configuration.set(key, defaultValue);
            setDefaultComment(configuration, key, defaultValue, commentLines);
            return new Value<>(configuration.getInt(key), true);
        }
        setDefaultComment(configuration, key, defaultValue, commentLines);
        return new Value<>(configuration.getInt(key), false);
    }

    public static Value<Double> decimal(
            final YamlConfig configuration,
            final String key,
            final double defaultValue,
            final String... commentLines) {
        if (!configuration.isDouble(key)) {
            configuration.set(key, defaultValue);
            setDefaultComment(configuration, key, defaultValue, commentLines);
            return new Value<>(configuration.getDouble(key), true);
        }
        setDefaultComment(configuration, key, defaultValue, commentLines);
        return new Value<>(configuration.getDouble(key), false);
    }

    public static Value<List<String>> stringList(
            final YamlConfig configuration,
            final String key,
            final List<String> defaultValue,
            final String... commentLines) {
        if (!configuration.isList(key)) {
            configuration.set(key, defaultValue);
            setDefaultComment(configuration, key, defaultValue, commentLines);
            return new Value<>(configuration.getStringList(key), true);
        }
        setDefaultComment(configuration, key, defaultValue, commentLines);
        return new Value<>(configuration.getStringList(key), false);
    }

    private static void setDefaultComment(
            final YamlConfig configuration,
            final String key,
            final Object defaultValue,
            final String... commentLines) {
        final List<String> comments = new ArrayList<>(Arrays.asList(commentLines));
        comments.add("Default: " + defaultValue);
        configuration.setComments(key, comments);
    }

    public static Versions ensureVersion(final YamlConfig configuration, final String currentVersion) {
        if (configuration == null) {
            return new Versions("", cleanVersion(currentVersion), false);
        }
        boolean changed = false;
        final String previousConfigVersion = configuration.getString(CONFIG_VERSION, "");

        if (!configuration.isString(CONFIG_VERSION)) {
            configuration.set(CONFIG_VERSION, cleanVersion(currentVersion));
            configuration.setComments(
                    CONFIG_VERSION,
                    List.of(
                            "Do not modify this line. If you modify it, there is a chance of completely breaking automatic configuration updates.",
                            "Default: " + cleanVersion(currentVersion)));
            changed = true;
        }

        if (!configuration.getString(CONFIG_VERSION, "").equalsIgnoreCase(cleanVersion(currentVersion))) {
            configuration.set(CONFIG_VERSION, cleanVersion(currentVersion));
            changed = true;
        }

        return new Versions(
                previousConfigVersion,
                configuration.getString(CONFIG_VERSION, cleanVersion(currentVersion)),
                changed);
    }

    public static PacketMagic packetMagic(
            final YamlConfig configuration,
            final String serverVersion,
            final String platformName) {
        boolean changed = false;

        final Value<Boolean> enabledValue = bool(configuration, "general.packet-magic.enabled", true);
        changed |= enabledValue.changed();

        final Value<String> modeValue = string(
                configuration,
                "general.packet-magic.mode",
                "internal",
                "Possible modes: 'internal' and 'packetevents'");
        changed |= modeValue.changed();

        final Value<Boolean> unsafeValue = bool(
                configuration,
                "general.packet-magic.unsafe-disregard-version",
                false,
                "Usually, the version of the server is checked to see if it is compatible with the packets feature. If it's not, packet-magic is disabled no matter if you enabled it or not. Setting this to true will disable this safety mechanism.");
        changed |= unsafeValue.changed();

        boolean packetMagic = enabledValue.value();
        final boolean usePacketEvents = "packetevents".equalsIgnoreCase(modeValue.value());
        final boolean unsafeDisregardVersion = unsafeValue.value();
        final List<String> info = new ArrayList<>();
        info.add("Detected version: " + safe(serverVersion) + " <highlight>(" + safe(platformName) + ")");

        if (!safe(serverVersion).contains("26.")) {
            if (!unsafeDisregardVersion) {
                packetMagic = false;
                info.add("Packet magic has been disabled, because you are using an unsupported bukkit version...");
            } else {
                info.add("You are using an unsupported version for packet magic. However, because the unsafe-disregard-version flag which is set to true in your config, we will try to do some packet magic anyways. Let's hope it works - good luck!");
            }
        }

        return new PacketMagic(packetMagic, usePacketEvents, unsafeDisregardVersion, changed, List.copyOf(info));
    }

    public static String invalidValue(
            final String fileName,
            final String valueType,
            final String key,
            final Object invalidValue,
            final Object defaultValue) {
        return "Invalid " + fileName + " " + valueType + " at '" + key + "': '" + invalidValue
                + "'. Falling back to '" + defaultValue + "' for this server version.";
    }

    private static String safe(final String value) {
        return value == null ? "" : value;
    }

    private static String cleanVersion(final String version) {
        return version == null || version.isBlank() ? "0.0.0" : version;
    }

    public record Defaults(boolean changed, boolean databaseConfigurationMissing) {}

    public record Loaded(Path file, YamlConfig configuration, boolean loaded, boolean created) {
        private static Loaded failed(final Path file) {
            return new Loaded(file, null, false, false);
        }
    }

    public interface IO {
        YamlConfig load(Path file) throws IOException;

        void save(YamlConfig configuration, Path file) throws IOException;
    }

    public interface DataFolderHooks {
        DataFolderHooks NO_OP = new DataFolderHooks() {
            @Override
            public void info(final String message) {}

            @Override
            public void disableSaving(final String reason, final Exception exception) {}
        };

        void info(String message);

        void disableSaving(String reason, Exception exception);
    }

    public interface Hooks extends DataFolderHooks {
        Hooks NO_OP = new Hooks() {
            @Override
            public void info(final String message) {}

            @Override
            public void warn(final String message) {}

            @Override
            public void disableSaving(final String reason, final Exception exception) {}
        };

        void info(String message);

        void warn(String message);

        void disableSaving(String reason, Exception exception);
    }

    public record Value<T>(T value, boolean changed) {}

    public record Versions(
            String previousConfigVersion,
            String currentConfigVersion,
            boolean changed) {}

    public record PacketMagic(
            boolean packetMagic,
            boolean usePacketEvents,
            boolean unsafeDisregardVersion,
            boolean changed,
            List<String> infoMessages) {}

    public record Shared(
            boolean changed,
            String disableReason) {
        public boolean shouldDisableSaving() {
            return disableReason != null && !disableReason.isBlank();
        }
    }

    private static final class DefaultsBuilder {
        private boolean changed;
        private boolean databaseConfigurationMissing;

        private Defaults build() {
            return new Defaults(changed, databaseConfigurationMissing);
        }
    }
}
