package com.notquests.core.managers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.notquests.core.config.YamlConfig;

import java.util.Map;

class ConfigurationManagerVersionsTest {
    @Test
    void createsMissingVersionKeys() {
        final YamlConfig configuration = YamlConfig.empty();

        final ConfigurationManager.Versions result = ConfigurationManager.ensureVersion(configuration, "7.0.0-beta.1");

        assertTrue(result.changed());
        assertEquals("", result.previousConfigVersion());
        assertEquals("7.0.0-beta.1", result.currentConfigVersion());
        assertEquals("7.0.0-beta.1", configuration.getString(ConfigurationManager.CONFIG_VERSION));
    }

    @Test
    void updatesTheNormalConfigVersionWithoutOwningMigrationState() {
        final YamlConfig configuration = YamlConfig.fromMap(Map.of(
                "config-version-do-not-edit", "6.3.0"));

        final ConfigurationManager.Versions result = ConfigurationManager.ensureVersion(configuration, "7.0.0-beta.1");

        assertTrue(result.changed());
        assertEquals("6.3.0", result.previousConfigVersion());
        assertEquals("7.0.0-beta.1", configuration.getString(ConfigurationManager.CONFIG_VERSION));
    }
}
