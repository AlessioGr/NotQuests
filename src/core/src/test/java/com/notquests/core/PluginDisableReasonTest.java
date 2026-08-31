package com.notquests.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.util.List;

class PluginDisableReasonTest {
    @Test
    void formatsContextDetails() {
        final NotQuestsPlugin.DisableReason.Formatted result = NotQuestsPlugin.DisableReason.format(
                "Cannot load",
                List.of(
                        NotQuestsPlugin.DisableReason.quest("TheVirus"),
                        NotQuestsPlugin.DisableReason.objective(3, "TheVirus"),
                        NotQuestsPlugin.DisableReason.action("reward", "GiveItem"),
                        NotQuestsPlugin.DisableReason.category("default")));

        assertEquals(List.of(
                "  <DARK_GRAY>└─</DARK_GRAY> Quest: <highlight>TheVirus",
                "  <DARK_GRAY>└─</DARK_GRAY> Objective ID: <highlight>3</highlight> of Quest: <highlight2>TheVirus",
                "  <DARK_GRAY>└─</DARK_GRAY> Action Name: <highlight>reward</highlight> of Type: <highlight2>GiveItem",
                "  <DARK_GRAY>└─</DARK_GRAY> Category name: <highlight>default</highlight>."),
                result.logLines());
        assertEquals("""
                Cannot load
                  <DARK_GRAY>└─</DARK_GRAY> Quest: <highlight>TheVirus
                  <DARK_GRAY>└─</DARK_GRAY> Objective ID: <highlight>3</highlight> of Quest: <highlight2>TheVirus
                  <DARK_GRAY>└─</DARK_GRAY> Action Name: <highlight>reward</highlight> of Type: <highlight2>GiveItem
                  <DARK_GRAY>└─</DARK_GRAY> Category name: <highlight>default</highlight>.""",
                result.reason());
    }

    @Test
    void includesThrowableStackTrace() {
        final NotQuestsPlugin.DisableReason.Formatted result = NotQuestsPlugin.DisableReason.format(
                "Cannot load",
                List.of(NotQuestsPlugin.DisableReason.throwable(new IllegalStateException("broken"))));

        assertEquals(List.of("Error message:"), result.logLines());
        assertTrue(result.reason().contains("Error message:\njava.lang.IllegalStateException: broken"));
    }
}
