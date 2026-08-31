package com.notquests.paper.builtin.variables.hooks;

import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;

import com.notquests.core.platform.NotQuestsAdapter;
import com.notquests.core.platform.PlatformPlayer;
import com.notquests.paper.NotQuests;
import com.notquests.paper.PaperPlayer;

import java.util.UUID;

public final class TownyTownPlotCountVariable {
    private TownyTownPlotCountVariable() {}

    public static void register(final NotQuests main, final NotQuestsAdapter adapter) {
        adapter.variables()
                .numberVariable("TownyTownPlotCount")
                .displayName("Towny Town Plot Count")
                .description("Reads how many plot groups are in the target player's Towny town.")
                .singular("Plot in Town")
                .plural("Plots in Town")
                .get(context -> plotCount(main, context.questPlayer()))
                .register();
    }

    private static int plotCount(final NotQuests main, final PlatformPlayer questPlayer) {
        if (!main.getCorePlugin().integrationEnabled("Towny")) {
            return 0;
        }
        final Resident resident = resident(questPlayer);
        if (resident == null || resident.getTownOrNull() == null || !resident.hasTown()) {
            return 0;
        }
        final Town town = resident.getTownOrNull();
        return town == null ? 0 : town.getPlotGroups().size();
    }

    private static Resident resident(final PlatformPlayer questPlayer) {
        if (questPlayer == null || questPlayer.playerIdentifier() == null || questPlayer.playerIdentifier().isBlank()) {
            return null;
        }
        try {
            return TownyUniverse.getInstance().getResident(UUID.fromString(questPlayer.playerIdentifier()));
        } catch (final RuntimeException ignored) {
            return null;
        }
    }
}
