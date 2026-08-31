package com.notquests.core.managers;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.notquests.core.config.YamlConfig;

import java.util.Map;

class ConfigurationManagerPacketMagicTest {
    @Test
    void readsDefaultsAndKeepsPacketMagicEnabledOnSupportedVersion() {
        final YamlConfig configuration = YamlConfig.empty();

        final ConfigurationManager.PacketMagic result = ConfigurationManager.packetMagic(configuration, "26.2", "Paper");

        assertTrue(result.packetMagic());
        assertFalse(result.usePacketEvents());
        assertFalse(result.unsafeDisregardVersion());
        assertTrue(result.changed());
        assertTrue(result.infoMessages().contains("Detected version: 26.2 <highlight>(Paper)"));
    }

    @Test
    void disablesPacketMagicOnUnsupportedVersionUnlessUnsafeOverrideIsEnabled() {
        final ConfigurationManager.PacketMagic result = ConfigurationManager.packetMagic(
                YamlConfig.fromMap(Map.of(
                        "general",
                        Map.of("packet-magic", Map.of("enabled", true)))),
                "25.0.0",
                "Paper");

        assertFalse(result.packetMagic());
        assertTrue(result.infoMessages().stream().anyMatch(message -> message.contains("unsupported bukkit version")));
    }

    @Test
    void unsafeOverrideKeepsConfiguredPacketMagicMode() {
        final ConfigurationManager.PacketMagic result = ConfigurationManager.packetMagic(
                YamlConfig.fromMap(Map.of(
                        "general",
                        Map.of("packet-magic", Map.of(
                                "enabled", true,
                                "mode", "packetevents",
                                "unsafe-disregard-version", true)))),
                "25.0.0",
                "Paper");

        assertTrue(result.packetMagic());
        assertTrue(result.usePacketEvents());
        assertTrue(result.unsafeDisregardVersion());
        assertTrue(result.infoMessages().stream().anyMatch(message -> message.contains("unsafe-disregard-version")));
    }
}
