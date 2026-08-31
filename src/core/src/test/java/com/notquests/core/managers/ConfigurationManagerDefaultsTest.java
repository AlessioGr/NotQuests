package com.notquests.core.managers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.notquests.core.config.YamlConfig;

import java.util.Map;

class ConfigurationManagerDefaultsTest {
    @Test
    void writesSharedDefaultsForMissingGeneralConfigValues() {
        final YamlConfig configuration = YamlConfig.fromMap(Map.of());

        final ConfigurationManager.Defaults result = ConfigurationManager.ensure(configuration);

        assertTrue(result.changed());
        assertFalse(result.databaseConfigurationMissing());
        assertEquals(false, configuration.getBoolean("debug"));
        assertEquals("en-US", configuration.getString("visual.language"));
        assertEquals(true, configuration.getBoolean("storage.load-playerdata"));
        assertEquals(-1, configuration.getInt("general.max-active-quests-per-player"));
        assertEquals(true, configuration.getBoolean("visual.objective-tracking.actionbar.enabled"));
        assertEquals(true, configuration.getBoolean("visual.armorstands.prevent-editing"));
        assertEquals(true, configuration.getBoolean("visual.armorstands.quest-giver-indicator-particle.enabled"));
        assertEquals("ANGRY_VILLAGER", configuration.getString("visual.armorstands.quest-giver-indicator-particle.type"));
        assertEquals(10, configuration.getInt("visual.armorstands.quest-giver-indicator-particle.spawn-interval"));
        assertEquals(1, configuration.getInt("visual.armorstands.quest-giver-indicator-particle.count"));
        assertEquals(-1d, configuration.getDouble("visual.armorstands.quest-giver-indicator-particle.disable-if-tps-below"));
        assertEquals("main-base", configuration.getString("gui.main-gui-name"));
        assertTrue(configuration.getBoolean("gui.quest-visibility-evaluations.max-accepts.enabled"));
        assertFalse(configuration.contains("gui.quest-visibility-evaluations.limits.enabled"));
        assertEquals(" | ", configuration.getString("placeholders.player_active_quests_list_horizontal.separator"));
        assertFalse(configuration.getBoolean("placeholders.support_placeholderapi_in_translation_strings"));
        assertTrue(configuration.getBoolean("integrations.citizens.enabled"));
        assertTrue(configuration.getBoolean("integrations.vault.enabled"));
        assertTrue(configuration.getBoolean("integrations.placeholderapi.enabled"));
        assertTrue(configuration.getBoolean("integrations.mythicmobs.enabled"));
        assertTrue(configuration.getBoolean("integrations.elitemobs.enabled"));
        assertTrue(configuration.getBoolean("integrations.worldedit.enabled"));
        assertTrue(configuration.getBoolean("integrations.slimefun.enabled"));
        assertTrue(configuration.getBoolean("integrations.luckperms.enabled"));
        assertTrue(configuration.getBoolean("integrations.ultimateclans.enabled"));
        assertTrue(configuration.getBoolean("integrations.towny.enabled"));
        assertTrue(configuration.getBoolean("integrations.jobs-reborn.enabled"));
        assertTrue(configuration.getBoolean("integrations.ecoMobs.enabled"));
        assertTrue(configuration.getBoolean("integrations.betonquest.enabled"));
        assertTrue(configuration.getBoolean("integrations.floodgate.enabled"));
    }

    @Test
    void missingDatabaseEnabledFlagUsesTheCurrentDisabledDefault() {
        final YamlConfig configuration = YamlConfig.fromMap(Map.of(
                "storage",
                Map.of(
                        "database",
                        Map.of(
                                "host", "127.0.0.1",
                                "port", 3306,
                                "database", "notquests",
                                "username", "nq",
                                "password", "secret"))));

        final ConfigurationManager.Defaults result = ConfigurationManager.ensure(configuration);

        assertTrue(result.changed());
        assertFalse(result.databaseConfigurationMissing());
        assertFalse(configuration.getBoolean("storage.database.enabled"));
    }

    @Test
    void reportsMissingDatabaseValuesOnlyWhenDatabaseStorageIsEnabled() {
        final YamlConfig configuration = YamlConfig.fromMap(Map.of(
                "storage",
                Map.of("database", Map.of("enabled", true))));

        final ConfigurationManager.Defaults result = ConfigurationManager.ensure(configuration);

        assertTrue(result.changed());
        assertTrue(result.databaseConfigurationMissing());
    }

    @Test
    void preservesDisabledMaxAcceptsVisibilitySettingWithoutAddingShadowKey() {
        final YamlConfig configuration = YamlConfig.fromMap(Map.of(
                "gui",
                Map.of(
                        "quest-visibility-evaluations",
                        Map.of("max-accepts", Map.of("enabled", false)))));

        ConfigurationManager.ensure(configuration);

        assertFalse(configuration.getBoolean("gui.quest-visibility-evaluations.max-accepts.enabled"));
        assertFalse(configuration.contains("gui.quest-visibility-evaluations.limits.enabled"));
    }
}
