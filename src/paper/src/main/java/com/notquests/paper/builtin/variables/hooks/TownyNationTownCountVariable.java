package com.notquests.paper.builtin.variables.hooks;

import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;

import com.notquests.core.platform.NotQuestsAdapter;
import com.notquests.core.platform.PlatformPlayer;
import com.notquests.paper.NotQuests;
import com.notquests.paper.PaperPlayer;

import java.util.UUID;

public final class TownyNationTownCountVariable {
    private TownyNationTownCountVariable() {}

    public static void register(final NotQuests main, final NotQuestsAdapter adapter) {
        adapter.variables()
                .numberVariable("TownyNationTownCount")
                .displayName("Towny Nation Town Count")
                .description("Reads how many towns are in the target player's Towny nation.")
                .singular("Town in Nation")
                .plural("Towns in Nation")
                .get(context -> townCount(main, context.questPlayer()))
                .register();
    }

    private static int townCount(final NotQuests main, final PlatformPlayer questPlayer) {
        if (!main.getCorePlugin().integrationEnabled("Towny")) {
            return 0;
        }
        final Resident resident = resident(questPlayer);
        if (resident == null
                || resident.getTownOrNull() == null
                || !resident.hasNation()
                || resident.getNationOrNull() == null) {
            return 0;
        }
        final Nation nation = resident.getNationOrNull();
        return nation == null ? 0 : nation.getNumTowns();
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
