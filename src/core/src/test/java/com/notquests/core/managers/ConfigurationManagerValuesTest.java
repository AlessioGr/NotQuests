package com.notquests.core.managers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.notquests.core.config.YamlConfig;

import java.util.List;
import java.util.Map;

class ConfigurationManagerValuesTest {
    @Test
    void writesMissingPrimitiveDefaultsAndReportsChanges() {
        final YamlConfig configuration = YamlConfig.empty();

        assertTrue(ConfigurationManager.string(configuration, "text", "fallback").changed());
        assertTrue(ConfigurationManager.bool(configuration, "flag", true).changed());
        assertTrue(ConfigurationManager.integer(configuration, "count", 5).changed());
        assertTrue(ConfigurationManager.decimal(configuration, "amount", 2.5d).changed());
        assertTrue(ConfigurationManager.stringList(configuration, "list", List.of("a", "b")).changed());

        assertEquals("fallback", configuration.getString("text"));
        assertTrue(configuration.getBoolean("flag"));
        assertEquals(5, configuration.getInt("count"));
        assertEquals(2.5d, configuration.getDouble("amount"));
        assertEquals(List.of("a", "b"), configuration.getStringList("list"));
    }

    @Test
    void readsExistingValuesWithoutReportingChanges() {
        final YamlConfig configuration = YamlConfig.fromMap(Map.of(
                "text", "custom",
                "flag", false,
                "count", 7,
                "amount", 3.25d,
                "list", List.of("x")));

        assertFalse(ConfigurationManager.string(configuration, "text", "fallback").changed());
        assertFalse(ConfigurationManager.bool(configuration, "flag", true).changed());
        assertFalse(ConfigurationManager.integer(configuration, "count", 5).changed());
        assertFalse(ConfigurationManager.decimal(configuration, "amount", 2.5d).changed());
        assertFalse(ConfigurationManager.stringList(configuration, "list", List.of("a", "b")).changed());

        assertEquals("custom", configuration.getString("text"));
        assertFalse(configuration.getBoolean("flag"));
        assertEquals(7, configuration.getInt("count"));
        assertEquals(3.25d, configuration.getDouble("amount"));
        assertEquals(List.of("x"), configuration.getStringList("list"));
    }
}
