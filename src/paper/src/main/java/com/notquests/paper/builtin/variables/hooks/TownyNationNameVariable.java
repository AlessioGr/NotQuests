package com.notquests.paper.builtin.variables.hooks;

import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;

import com.notquests.core.platform.NotQuestsAdapter;
import com.notquests.core.platform.PlatformPlayer;
import com.notquests.paper.NotQuests;
import com.notquests.paper.PaperPlayer;

import java.util.UUID;

public final class TownyNationNameVariable {
    private TownyNationNameVariable() {}

    public static void register(final NotQuests main, final NotQuestsAdapter adapter) {
        adapter.variables()
                .stringVariable("TownyNationName")
                .displayName("Towny Nation Name")
                .description("Reads or changes the target player's Towny nation name.")
                .singular("Nation name")
                .plural("Nation names")
                .get(context -> nationName(main, context.questPlayer()))
                .set((newValue, context) -> setNationName(main, context.questPlayer(), newValue))
                .register();
    }

    private static String nationName(final NotQuests main, final PlatformPlayer questPlayer) {
        if (!main.getCorePlugin().integrationEnabled("Towny")) {
            return "";
        }
        final Nation nation = nation(questPlayer);
        return nation == null ? "" : nation.getName().replace("_", " ");
    }

    private static boolean setNationName(
            final NotQuests main,
            final PlatformPlayer questPlayer,
            final String nationName) {
        if (!main.getCorePlugin().integrationEnabled("Towny")) {
            return false;
        }
        final Nation nation = nation(questPlayer);
        if (nation == null) {
            return false;
        }
        nation.setName(nationName);
        return true;
    }

    private static Nation nation(final PlatformPlayer questPlayer) {
        if (questPlayer == null || questPlayer.playerIdentifier() == null || questPlayer.playerIdentifier().isBlank()) {
            return null;
        }
        try {
            final Resident resident = TownyUniverse.getInstance().getResident(UUID.fromString(questPlayer.playerIdentifier()));
            if (resident == null
                    || resident.getTownOrNull() == null
                    || !resident.hasNation()
                    || resident.getNationOrNull() == null) {
                return null;
            }
            return resident.getNationOrNull();
        } catch (final RuntimeException ignored) {
            return null;
        }
    }
}
