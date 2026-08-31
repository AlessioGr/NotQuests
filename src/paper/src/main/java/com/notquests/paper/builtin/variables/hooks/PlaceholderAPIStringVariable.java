package com.notquests.paper.builtin.variables.hooks;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.notquests.core.platform.NotQuestsAdapter;
import com.notquests.core.platform.PlatformPlayer;
import com.notquests.core.registry.NotQuestsRegistry.Variables.Context;
import com.notquests.core.registry.NotQuestsRegistry.Variables;
import com.notquests.paper.NotQuests;
import com.notquests.paper.PaperPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class PlaceholderAPIStringVariable {
    private PlaceholderAPIStringVariable() {}

    public static void register(final NotQuests main, final NotQuestsAdapter adapter) {
        adapter.variables()
                .stringVariable("PlaceholderAPIString")
                .displayName("PlaceholderAPI String")
                .description("Reads a PlaceholderAPI placeholder as text for the target player.")
                .singular("PlaceholderAPI text")
                .plural("PlaceholderAPI texts")
                .field("Placeholder", adapter.fields().text(PlaceholderAPIStringVariable::placeholderSuggestions), "PlaceholderAPI placeholder text to read, such as %player_name%.")
                .get(PlaceholderAPIStringVariable::placeholderValue)
                .register();
    }

    private static String placeholderValue(final Variables.Context context) {
        if (context == null || context.questPlayer() == null) {
            return "";
        }
        final Player player = player(context.questPlayer());
        return player == null ? "" : PlaceholderAPI.setPlaceholders(player, context.text("Placeholder"));
    }

    private static List<String> placeholderSuggestions() {
        final ArrayList<String> suggestions = new ArrayList<>();
        for (final String identifier : PlaceholderAPI.getRegisteredIdentifiers()) {
            suggestions.add("%" + identifier + "_");
        }
        return suggestions;
    }

    private static Player player(final PlatformPlayer questPlayer) {
        if (questPlayer == null) {
            return null;
        }
        final String identifier = questPlayer.playerIdentifier();
        if (identifier != null && !identifier.isBlank()) {
            try {
                final Player player = Bukkit.getPlayer(UUID.fromString(identifier));
                if (player != null) {
                    return player;
                }
            } catch (final IllegalArgumentException ignored) {
                // Non-UUID platform identifiers can still be resolved by player name.
            }
        }
        final String name = questPlayer.playerName();
        return name == null || name.isBlank() ? null : Bukkit.getPlayerExact(name);
    }
}
