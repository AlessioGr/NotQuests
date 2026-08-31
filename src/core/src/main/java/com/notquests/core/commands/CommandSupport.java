package com.notquests.core.commands;

import com.notquests.core.NotQuestsPlugin;
import com.notquests.core.commands.framework.*;
import com.notquests.core.platform.NotQuestsAdapter;
import com.notquests.core.platform.PlatformPlayer;

final class CommandSupport {
    private CommandSupport() {}

    static String highlight(final Object value) {
        return "<highlight>" + value + "</highlight>";
    }

    static String highlight2(final Object value) {
        return "<highlight2>" + value + "</highlight2>";
    }

    static String unimportant(final Object value) {
        return "<unimportant>" + value + "</unimportant>";
    }

    static String blankDefault(final String value, final String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    static String playerDisplayName(final PlatformPlayer questPlayer) {
        if (questPlayer == null) {
            return "";
        }
        return questPlayer.playerName() == null || questPlayer.playerName().isBlank()
                ? questPlayer.playerIdentifier()
                : questPlayer.playerName();
    }

    static PlatformPlayer targetPlatformPlayer(
            final NotQuestsAdapter adapter,
            final String playerName) {
        return onlinePlayer(adapter, playerName);
    }

    static PlatformPlayer targetPlatformPlayer(
            final NotQuestsAdapter adapter,
            final String playerName,
            final PlatformPlayer fallback) {
        if (playerName == null || playerName.isBlank()) {
            return fallback;
        }
        return onlinePlayer(adapter, playerName);
    }

    static PlayerTarget playerTarget(
            final NotQuestsPlugin plugin,
            final NotQuestsAdapter adapter,
            final String playerName) {
        final String lookup = playerName == null || playerName.isBlank() ? "unknown" : playerName;
        final PlatformPlayer online = onlinePlayer(adapter, lookup);
        final String identifier = online == null || online.playerIdentifier() == null || online.playerIdentifier().isBlank()
                ? lookup
                : online.playerIdentifier();
        final String onlineDisplayName = playerDisplayName(online);
        final String displayName = onlineDisplayName.isBlank() ? lookup : onlineDisplayName;
        return new PlayerTarget(
                lookup,
                plugin.activeQuestPlayer(identifier),
                online,
                identifier,
                displayName);
    }

    static String playerOnlineStatus(
            final NotQuestsPlugin plugin,
            final NotQuestsAdapter adapter,
            final String playerName) {
        return playerTarget(plugin, adapter, playerName).onlineStatus();
    }

    private static PlatformPlayer onlinePlayer(
            final NotQuestsAdapter adapter,
            final String playerName) {
        if (adapter == null || playerName == null || playerName.isBlank()
                || playerName.equalsIgnoreCase("unknown")) {
            return null;
        }
        final PlatformPlayer player = adapter.onlineQuestPlayer(playerName);
        return player != null && player.hasPlayer() ? player : null;
    }
}
