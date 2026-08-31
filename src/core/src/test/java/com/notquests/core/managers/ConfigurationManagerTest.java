package com.notquests.core.managers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.notquests.core.config.YamlConfig;
import com.notquests.core.managers.LogManager.LogCategory;

import java.util.List;
import java.util.Map;

class ConfigurationManagerTest {
    @Test
    void ownsCanonicalJournalItemWithoutLettingAdaptersReadYaml() {
        final Map<String, Object> journal = Map.of(
                "material", "WRITTEN_BOOK",
                "enchantments", Map.of("minecraft:sharpness", 4),
                "stored-enchantments", Map.of("minecraft:mending", 1),
                "hidden-components", List.of("enchantments", "unbreakable"),
                "unbreakable", true,
                "custom", "kept");
        final ConfigurationManager configuration = new ConfigurationManager();

        configuration.loadFrom(YamlConfig.fromMap(Map.of(
                "general", Map.of("journal-item", Map.of("item", journal)))));

        assertTrue(configuration.journalItem().configured());
        assertTrue(configuration.journalItem().valid());
        assertEquals("WRITTEN_BOOK", configuration.journalItem().material());
        assertEquals(Map.of("minecraft:sharpness", 4), configuration.journalItem().enchantments());
        assertEquals(Map.of("minecraft:mending", 1), configuration.journalItem().storedEnchantments());
        assertEquals(List.of("enchantments", "unbreakable"), configuration.journalItem().hiddenComponents());
        assertTrue(configuration.journalItem().unbreakable());
        assertEquals(journal, configuration.journalItem().configuredValue());

        assertEquals("ENCHANTED_BOOK", configuration.useDefaultJournalItem().material());
        assertFalse(configuration.journalItem().configured());
    }

    @Test
    void loadsMiniMessageTagColorsFromSharedGeneralConfig() {
        final ConfigurationManager settings = new ConfigurationManager();

        settings.loadFrom(YamlConfig.fromMap(Map.of(
                "visual",
                Map.of(
                        "colors",
                        Map.of(
                                "tags",
                                Map.of(
                                        "main", List.of("#1985ff", "#2bc7ff"),
                                        "highlight", List.of("#00fffb", "#00ffc3"),
                                        "veryUnimportant", List.of("#5c5c5c", "#454545")))))));

        assertEquals(List.of("#1985ff", "#2bc7ff"), settings.colors("main"));
        assertEquals(List.of("#00fffb", "#00ffc3"), settings.colors("highlight"));
        assertEquals(List.of("#5c5c5c", "#454545"), settings.colors("veryunimportant"));
    }

    @Test
    void loadsSharedLanguageCodeFromGeneralConfig() {
        final ConfigurationManager settings = new ConfigurationManager();

        settings.loadFrom(YamlConfig.fromMap(Map.of(
                "visual",
                Map.of("language", "de-DE"))));

        assertEquals("de-DE", settings.languageCode());
    }

    @Test
    void loadsSharedConsoleColorSettingsFromGeneralConfig() {
        final ConfigurationManager settings = new ConfigurationManager();

        settings.loadFrom(YamlConfig.fromMap(Map.of(
                "visual",
                Map.of(
                        "colors",
                        Map.of(
                                "console",
                                Map.of(
                                        "enabled", false,
                                        "downsampleColors", true,
                                        "prefix", Map.of("prefix", "<p>", "suffix", "<s>"),
                                        "info", Map.of(
                                                "default", Map.of("normal", "<i>", "downsampled", "<id>"),
                                                "data", Map.of("normal", "<data>", "downsampled", "<datad>"),
                                                "language", Map.of("normal", "<lang>", "downsampled", "<langd>")),
                                        "warn", Map.of("default", Map.of("normal", "<warn>", "downsampled", "<warnd>")),
                                        "severe", Map.of("default", Map.of("normal", "<severe>", "downsampled", "<severed>")),
                                        "debug", Map.of("default", Map.of("normal", "<debug>", "downsampled", "<debugd>"))))))));

        assertFalse(settings.consoleColorsEnabled());
        assertEquals(true, settings.consoleColorsDownsampled());
        assertEquals("<p>", settings.consolePrefixPrefix());
        assertEquals("<s>", settings.consolePrefixSuffix());
        assertEquals("<id>", settings.consoleInfoColor(LogCategory.DEFAULT));
        assertEquals("<datad>", settings.consoleInfoColor(LogCategory.DATA));
        assertEquals("<langd>", settings.consoleInfoColor(LogCategory.LANGUAGE));
        assertEquals("<warnd>", settings.consoleWarnColor());
        assertEquals("<severed>", settings.consoleSevereColor());
        assertEquals("<debugd>", settings.consoleDebugColor());
    }

    @Test
    void loadsSharedCommandHintSettingsFromGeneralConfig() {
        final ConfigurationManager settings = new ConfigurationManager();

        settings.loadFrom(YamlConfig.fromMap(Map.of(
                "visual",
                Map.of(
                        "fancy-command-completion",
                        Map.of(
                                "actionbar-enabled", false,
                                "title-enabled", true,
                                "bossbar-enabled", true,
                                "max-previous-arguments-displayed", 5)))));

        assertFalse(settings.commandHintActionbarEnabled());
        assertEquals(true, settings.commandHintTitleEnabled());
        assertEquals(true, settings.commandHintBossbarEnabled());
        assertEquals(5, settings.commandHintMaxPreviousArguments());
    }

    @Test
    void loadsSharedObjectiveTrackingSettingsFromGeneralConfig() {
        final ConfigurationManager settings = new ConfigurationManager();

        settings.loadFrom(YamlConfig.fromMap(Map.of(
                "visual",
                Map.of(
                        "objective-tracking",
                        Map.of(
                                "actionbar", Map.of("enabled", false),
                                "bossbar",
                                        Map.of(
                                                "enabled", false,
                                                "show-time", 17,
                                                "show-if-objective-is-completed", true),
                                "location-compass", Map.of("enabled", true),
                                "beam-mode", "beacon")))));

        assertFalse(settings.objectiveTrackingActionbarEnabled());
        assertFalse(settings.objectiveTrackingBossbarEnabled());
        assertEquals(17, settings.objectiveTrackingBossbarShowTimeSeconds());
        assertEquals(true, settings.objectiveTrackingBossbarShowCompleted());
        assertEquals(true, settings.objectiveTrackingLocationCompassEnabled());
        assertEquals("beacon", settings.objectiveTrackingBeamMode());
    }

    @Test
    void loadsSharedArmorStandVisualSettingsFromGeneralConfig() {
        final ConfigurationManager settings = new ConfigurationManager();

        settings.loadFrom(YamlConfig.fromMap(Map.of(
                "visual",
                Map.of(
                        "armorstands",
                        Map.of(
                                "prevent-editing", false,
                                "quest-giver-indicator-particle",
                                        Map.of(
                                                "enabled", false,
                                                "type", "HAPPY_VILLAGER",
                                                "spawn-interval", 12,
                                                "count", 3,
                                                "disable-if-tps-below", 17.5))))));

        assertFalse(settings.armorStandPreventEditing());
        assertFalse(settings.armorStandQuestGiverIndicatorParticleEnabled());
        assertEquals("HAPPY_VILLAGER", settings.armorStandQuestGiverIndicatorParticleType());
        assertEquals(12, settings.armorStandQuestGiverIndicatorParticleSpawnInterval());
        assertEquals(3, settings.armorStandQuestGiverIndicatorParticleCount());
        assertEquals(17.5, settings.armorStandQuestGiverIndicatorParticleDisableIfTpsBelow());
        assertFalse(settings.armorStandQuestGiverIndicatorCanSpawnAtTps(17.49));
        assertTrue(settings.armorStandQuestGiverIndicatorCanSpawnAtTps(17.5));
    }

    @Test
    void loadsQuestPreviewRewardVisibilityFromSharedGeneralConfig() {
        final ConfigurationManager settings = new ConfigurationManager();

        settings.loadFrom(YamlConfig.fromMap(Map.of(
                "visual",
                Map.of(
                        "hide-rewards-without-name", false,
                        "show-rewards-after-quest-completion", false,
                        "show-rewards-after-objective-completion", false),
                "gui",
                Map.of(
                        "show-quest-item-amount", true,
                        "show-objective-item-amount", false,
                        "quest-description-max-line-length", 33,
                        "objective-description-max-line-length", 44,
                        "wrap-long-words", true))));

        assertFalse(settings.hideRewardsWithoutName());
        assertFalse(settings.showRewardsAfterQuestCompletion());
        assertFalse(settings.showRewardsAfterObjectiveCompletion());
        assertEquals(true, settings.showQuestItemAmount());
        assertFalse(settings.showObjectiveItemAmount());
        assertEquals(33, settings.questDescriptionMaxLineLength());
        assertEquals(44, settings.objectiveDescriptionMaxLineLength());
        assertEquals(true, settings.wrapLongWords());
    }

    @Test
    void loadsSharedPlayerStorageSettingsFromGeneralConfig() {
        final ConfigurationManager settings = new ConfigurationManager();

        settings.loadFrom(YamlConfig.fromMap(Map.of(
                "storage",
                Map.of(
                        "load-playerdata", false,
                        "save-playerdata", false,
                        "load-playerdata-on-join", false,
                        "save-playerdata-on-quit", false,
                        "backups",
                        Map.of(
                                "create-when-server-shuts-down", false,
                                "create-for-database-before-database-loads", false)))));

        assertFalse(settings.loadPlayerData());
        assertFalse(settings.savePlayerData());
        assertFalse(settings.loadPlayerDataOnJoin());
        assertFalse(settings.savePlayerDataOnQuit());
        assertFalse(settings.backupQuestsOnShutdown());
        assertFalse(settings.backupDatabaseBeforeLoad());
    }

    @Test
    void loadsSharedDatabaseSettingsFromGeneralConfig() {
        final ConfigurationManager settings = new ConfigurationManager();

        settings.loadFrom(YamlConfig.fromMap(Map.of(
                "storage",
                Map.of(
                        "database",
                        Map.of(
                                "enabled", true,
                                "host", "db.example.test",
                                "port", 3307,
                                "database", "notquests",
                                "username", "quests",
                                "password", "secret")))));

        assertTrue(settings.databaseEnabled());
        assertEquals("db.example.test", settings.databaseHost());
        assertEquals(3307, settings.databasePort());
        assertEquals("notquests", settings.databaseName());
        assertEquals("quests", settings.databaseUsername());
        assertEquals("secret", settings.databasePassword());
    }

    @Test
    void loadsSharedIntegrationEnableSettingsFromGeneralConfig() {
        final ConfigurationManager settings = new ConfigurationManager();

        settings.loadFrom(YamlConfig.fromMap(Map.of(
                "integrations",
                Map.ofEntries(
                        Map.entry("citizens", Map.of("enabled", false)),
                        Map.entry("vault", Map.of("enabled", false)),
                        Map.entry("placeholderapi", Map.of("enabled", false)),
                        Map.entry("mythicmobs", Map.of("enabled", false)),
                        Map.entry("elitemobs", Map.of("enabled", false)),
                        Map.entry("worldedit", Map.of("enabled", false)),
                        Map.entry("slimefun", Map.of("enabled", false)),
                        Map.entry("luckperms", Map.of("enabled", false)),
                        Map.entry("ultimateclans", Map.of("enabled", false)),
                        Map.entry("towny", Map.of("enabled", false)),
                        Map.entry("jobs-reborn", Map.of("enabled", false)),
                        Map.entry("ecoMobs", Map.of("enabled", false)),
                        Map.entry("betonquest", Map.of("enabled", false)),
                        Map.entry("floodgate", Map.of("enabled", false))))));

        assertFalse(settings.integrationCitizensEnabled());
        assertFalse(settings.integrationVaultEnabled());
        assertFalse(settings.integrationPlaceholderApiEnabled());
        assertFalse(settings.integrationMythicMobsEnabled());
        assertFalse(settings.integrationEliteMobsEnabled());
        assertFalse(settings.integrationWorldEditEnabled());
        assertFalse(settings.integrationSlimefunEnabled());
        assertFalse(settings.integrationLuckPermsEnabled());
        assertFalse(settings.integrationUltimateClansEnabled());
        assertFalse(settings.integrationTownyEnabled());
        assertFalse(settings.integrationJobsRebornEnabled());
        assertFalse(settings.integrationEcoMobsEnabled());
        assertFalse(settings.integrationBetonQuestEnabled());
        assertFalse(settings.integrationFloodgateEnabled());
    }

    @Test
    void loadsSharedPlaceholderApiTranslationSwitchFromGeneralConfig() {
        final ConfigurationManager settings = new ConfigurationManager();

        settings.loadFrom(YamlConfig.fromMap(Map.of(
                "placeholders",
                Map.of("support_placeholderapi_in_translation_strings", true))));

        assertTrue(settings.supportPlaceholderApiInTranslationStrings());
    }

    @Test
    void loadsSharedUpdateNotificationSettingFromGeneralConfig() {
        final ConfigurationManager settings = new ConfigurationManager();

        settings.loadFrom(YamlConfig.fromMap(Map.of(
                "general",
                Map.of("update-checker", Map.of("notify-ops-in-chat", false)))));

        assertFalse(settings.updateCheckerNotifyOpsInChat());
    }

    @Test
    void loadsSharedConversationPolicyFromGeneralConfig() {
        final ConfigurationManager settings = new ConfigurationManager();

        settings.loadFrom(YamlConfig.fromMap(Map.of(
                "general",
                Map.of("packet-magic", Map.of(
                        "conversations", Map.of(
                                "delete-previous", false,
                                "history-size", 7))),
                "conversations",
                Map.of(
                        "interaction-handlers",
                        Map.of(
                                "clickable-text",
                                Map.of("allow-selecting-option-by-typing-number-in-chat", false))))));

        assertFalse(settings.deletePreviousConversationMessages());
        assertEquals(7, settings.previousConversationHistorySize());
        assertFalse(settings.conversationAnswerNumberInChatEnabled());
    }

    @Test
    void loadsSharedPlaceholderListSettingsFromGeneralConfig() {
        final ConfigurationManager settings = new ConfigurationManager();

        settings.loadFrom(YamlConfig.fromMap(Map.of(
                "placeholders",
                Map.of(
                        "player_active_quests_list_horizontal",
                        Map.of(
                                "separator", " / ",
                                "limit", 3,
                                "use-displayname-if-available", false),
                        "player_active_quests_list_vertical",
                        Map.of(
                                "limit", 4,
                                "use-displayname-if-available", false)))));

        assertEquals(" / ", settings.activeQuestListHorizontalSeparator());
        assertEquals(3, settings.activeQuestListHorizontalLimit());
        assertEquals(4, settings.activeQuestListVerticalLimit());
        assertFalse(settings.activeQuestListHorizontalUseDisplayName());
        assertFalse(settings.activeQuestListVerticalUseDisplayName());
    }

    @Test
    void loadsSharedJournalPlacementSettingsFromGeneralConfig() {
        final ConfigurationManager settings = new ConfigurationManager();

        settings.loadFrom(YamlConfig.fromMap(Map.of(
                "general",
                Map.of(
                        "journal-item",
                        Map.of(
                                "enabled-worlds", List.of("world", "world_nether"),
                                "inventory-slot", 7)))));

        assertEquals(List.of("world", "world_nether"), settings.journalEnabledWorlds());
        assertEquals(7, settings.journalInventorySlot());
    }

    @Test
    void canDisableConversationMessageDeletionAtRuntime() {
        final ConfigurationManager settings = new ConfigurationManager();

        settings.disablePreviousConversationMessageDeletion();

        assertFalse(settings.deletePreviousConversationMessages());
    }

    @Test
    void loadsSharedVerboseStartupSettingFromGeneralConfig() {
        final ConfigurationManager settings = new ConfigurationManager();

        settings.loadFrom(YamlConfig.fromMap(Map.of(
                "debug", false,
                "logging", Map.of("verbose-startup-messages", false))));

        assertFalse(settings.verboseStartupMessages());
    }

    @Test
    void loadsQuestVisibilitySettingsFromSharedGeneralConfig() {
        final ConfigurationManager settings = new ConfigurationManager();

        settings.loadFrom(YamlConfig.fromMap(Map.of(
                "gui",
                Map.of(
                        "quest-visibility-evaluations",
                        Map.of(
                                "already-accepted", Map.of("enabled", false),
                                "max-accepts", Map.of("enabled", true),
                                "accept-cooldown", Map.of("enabled", true),
                                "conditions", Map.of("enabled", true))))));

        assertFalse(settings.questVisibilityEvaluationAlreadyAccepted());
        assertEquals(true, settings.questVisibilityEvaluationLimits());
        assertEquals(true, settings.questVisibilityEvaluationAcceptCooldown());
        assertEquals(true, settings.questVisibilityEvaluationConditions());
    }

    @Test
    void loadsMaxAcceptsVisibilitySettingFromCanonicalKey() {
        final ConfigurationManager settings = new ConfigurationManager();

        settings.loadFrom(YamlConfig.fromMap(Map.of(
                "gui",
                Map.of(
                        "quest-visibility-evaluations",
                        Map.of("max-accepts", Map.of("enabled", false))))));

        assertFalse(settings.questVisibilityEvaluationLimits());
    }

    @Test
    void defaultInsertionDoesNotShadowDisabledMaxAcceptsVisibilitySetting() {
        final YamlConfig configuration = YamlConfig.fromMap(Map.of(
                "gui",
                Map.of(
                        "quest-visibility-evaluations",
                        Map.of("max-accepts", Map.of("enabled", false)))));

        ConfigurationManager.ensure(configuration);
        final ConfigurationManager settings = new ConfigurationManager();
        settings.loadFrom(configuration);

        assertFalse(settings.questVisibilityEvaluationLimits());
        assertFalse(configuration.contains("gui.quest-visibility-evaluations.limits.enabled"));
    }

    @Test
    void loadsSharedGuiNamesFromGeneralConfig() {
        final ConfigurationManager settings = new ConfigurationManager();

        settings.loadFrom(YamlConfig.fromMap(Map.of(
                "gui",
                Map.of(
                        "usercommands", Map.of("enabled", false),
                        "questpreview", Map.of("enabled", false),
                        "main-gui-name", "main-custom",
                        "npc-gui-name", "npc-custom"))));

        assertFalse(settings.userCommandGuiEnabled());
        assertFalse(settings.questPreviewGuiEnabled());
        assertEquals("main-custom", settings.mainGuiName());
        assertEquals("npc-custom", settings.npcGuiName());
    }

    @Test
    void loadsSharedObjectiveRuntimeSettingsFromGeneralConfig() {
        final ConfigurationManager settings = new ConfigurationManager();

        settings.loadFrom(YamlConfig.fromMap(Map.of(
                "general",
                Map.of(
                        "enable-move-event", false,
                        "objectives",
                                Map.of(
                                        "unlock-conditions-checks",
                                        Map.of(
                                                "any-action", false,
                                                "regular-interval", 9))))));

        assertFalse(settings.moveEventEnabled());
        assertFalse(settings.objectiveUnlockConditionsCheckOnAnyAction());
        assertEquals(9, settings.objectiveUnlockConditionsCheckRegularIntervalSeconds());
    }
}
