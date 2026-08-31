package com.notquests.paper.builtin.variables.hooks;

import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;

import com.notquests.core.platform.NotQuestsAdapter;
import com.notquests.core.platform.PlatformPlayer;
import com.notquests.paper.NotQuests;
import com.notquests.paper.PaperPlayer;

import java.util.UUID;

public final class TownyTownResidentCountVariable {
    private TownyTownResidentCountVariable() {}

    public static void register(final NotQuests main, final NotQuestsAdapter adapter) {
        adapter.variables()
                .numberVariable("TownyTownResidentCount")
                .displayName("Towny Town Resident Count")
                .description("Reads how many residents are in the target player's Towny town.")
                .singular("Resident in Town")
                .plural("Residents in Town")
                .get(context -> residentCount(main, context.questPlayer()))
                .register();
    }

    private static int residentCount(final NotQuests main, final PlatformPlayer questPlayer) {
        if (!main.getCorePlugin().integrationEnabled("Towny")) {
            return 0;
        }
        final Resident resident = resident(questPlayer);
        if (resident == null || resident.getTownOrNull() == null || !resident.hasTown()) {
            return 0;
        }
        final Town town = resident.getTownOrNull();
        return town == null ? 0 : town.getNumResidents();
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
