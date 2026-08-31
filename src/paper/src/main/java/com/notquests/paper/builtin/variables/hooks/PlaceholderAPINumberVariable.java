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

public final class PlaceholderAPINumberVariable {
    private PlaceholderAPINumberVariable() {}

    public static void register(final NotQuests main, final NotQuestsAdapter adapter) {
        adapter.variables()
                .numberVariable("PlaceholderAPINumber")
                .displayName("PlaceholderAPI Number")
                .description("Reads a PlaceholderAPI placeholder as a number for the target player.")
                .singular("PlaceholderAPI number")
                .plural("PlaceholderAPI numbers")
                .field("Placeholder", adapter.fields().text(PlaceholderAPINumberVariable::placeholderSuggestions), "PlaceholderAPI placeholder text to read, such as %player_level%.")
                .field("removeTextFromPlaceholderValue", adapter.fields().presenceFlag(), "Removes non-number characters before parsing the placeholder value.")
                .get(context -> placeholderValue(main, context))
                .register();
    }

    private static Number placeholderValue(final NotQuests main, final Variables.Context context) {
        if (context == null || context.questPlayer() == null || context.text("Placeholder").isBlank()) {
            return 0D;
        }
        final Player player = player(context.questPlayer());
        if (player == null) {
            return 0D;
        }
        final String placeholder = PlaceholderAPI.setPlaceholders(player, context.text("Placeholder"));
        return main.getCorePlugin().placeholderNumber(
                placeholder,
                Boolean.TRUE.equals(context.value("removeTextFromPlaceholderValue")));
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
