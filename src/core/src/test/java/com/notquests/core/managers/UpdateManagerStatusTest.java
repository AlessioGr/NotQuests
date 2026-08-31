package com.notquests.core.managers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.notquests.core.NotQuestsPlugin;
import com.notquests.core.config.YamlConfig;
import com.notquests.core.managers.UpdateManager.Check;
import com.notquests.core.managers.UpdateManager.Status;

import java.util.Map;

class UpdateManagerStatusTest {
    @Test
    void defaultsLatestVersionToCurrentVersion() {
        final Status status = new Status();

        status.currentVersion("6.3.0");

        assertEquals("6.3.0", status.currentVersion());
        assertEquals("6.3.0", status.latestVersion());
        assertFalse(status.updateAvailable());
    }

    @Test
    void storesUpdateCheckResult() {
        final Status status = new Status();

        status.update(new Check("6.3.0", "6.4.0", true));

        assertEquals("6.3.0", status.currentVersion());
        assertEquals("6.4.0", status.latestVersion());
        assertTrue(status.updateAvailable());
    }

    @Test
    void managerOwnsUpdateStatusMutation() throws Exception {
        final UpdateManager manager = new UpdateManager(() -> "6.4.0");

        final Check check = manager.check("6.3.0");

        assertTrue(check.updateAvailable());
        assertEquals("6.3.0", manager.status().currentVersion());
        assertEquals("6.4.0", manager.status().latestVersion());
        assertTrue(manager.status().updateAvailable());
    }

    @Test
    void managerOwnsUpdateCommandAndNotificationMessages() throws Exception {
        final UpdateManager manager = new UpdateManager(() -> "6.4.0");

        final String commandMessage = manager.checkAndChatMessage("6.3.0");
        final String notificationMessage = manager.availableChatMessage("6.3.0");
        final String consoleMessage = manager.checkAndConsoleMessage("6.3.0");

        assertTrue(commandMessage.contains("not the latest version"));
        assertEquals(commandMessage, notificationMessage);
        assertTrue(consoleMessage.contains("A new version of NotQuests is available"));

        final UpdateManager current = new UpdateManager(() -> "6.3.0");
        final String upToDateMessage = current.checkAndChatMessage("6.3.0");

        assertEquals("<success>NotQuests is up to date! (version: <green>6.3.0</green>)", upToDateMessage);
        assertEquals("", current.availableChatMessage("6.3.0"));
    }

    @Test
    void pluginOwnsOperatorUpdateNotificationDecision() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();

        assertFalse(plugin.shouldNotifyOperatorAboutUpdate());

        plugin.updateStatus().update(new Check("6.3.0", "6.4.0", true));
        assertTrue(plugin.shouldNotifyOperatorAboutUpdate());

        plugin.configuration().loadFrom(YamlConfig.fromMap(Map.of(
                "general", Map.of("update-checker", Map.of("notify-ops-in-chat", false)))));
        assertFalse(plugin.shouldNotifyOperatorAboutUpdate());
    }
}
