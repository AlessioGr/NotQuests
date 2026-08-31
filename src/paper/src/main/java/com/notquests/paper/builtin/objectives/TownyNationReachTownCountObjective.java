package com.notquests.paper.builtin.objectives;

import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;

import com.notquests.core.platform.NotQuestsAdapter;
import com.notquests.core.registry.NotQuestsRegistry.Objectives;
import com.notquests.paper.NotQuests;

import java.util.UUID;

public final class TownyNationReachTownCountObjective {
    public static final String TYPE = "TownyNationReachTownCount";
    private static final String AMOUNT = "amount";
    private static final String DO_NOT_COUNT_PREVIOUS_TOWNS = "doNotCountPreviousTowns";

    private TownyNationReachTownCountObjective() {}

    public static void register(final NotQuests main, final NotQuestsAdapter adapter) {
        if (!main.getCorePlugin().integrationEnabled("Towny")) {
            return;
        }
        adapter.objectives().objective(TYPE)
                .displayName("Reach Towny Nation Town Count")
                .description("Counts when the player's Towny nation reaches a target town count.")
                .field(
                        AMOUNT,
                        adapter.fields().numberExpression(false).progressNeeded(),
                        "Minimum town count the player's nation must reach.")
                .flag(
                        DO_NOT_COUNT_PREVIOUS_TOWNS,
                        adapter.fields().presenceFlag().invertedBooleanConfig("specifics.countPreviousTowns"),
                        "Only count towns added after this objective unlocks; existing towns do not count.")
                .taskDescription((objective, questPlayer, activeObjective) ->
                        main.getCorePlugin().townyNationTownCountTaskDescription(
                                questPlayer, objective, activeObjective))
                .onUnlock((objective, questPlayer, loading) -> {
                    main.getCorePlugin().townyNationTownCountObjectiveUnlocked(
                            objective,
                            loading,
                            currentTownCount(questPlayer == null ? "" : questPlayer.playerIdentifier()));
                })
                .register();
    }

    private static int currentTownCount(final String playerIdentifier) {
        final Resident resident = resident(playerIdentifier);
        if (resident == null) {
            return 0;
        }
        final Town town = resident.getTownOrNull();
        if (town == null) {
            return 0;
        }
        final Nation nation = town.getNationOrNull();
        return nation == null ? 0 : nation.getNumTowns();
    }

    private static Resident resident(final String playerIdentifier) {
        try {
            return TownyUniverse.getInstance().getResident(UUID.fromString(playerIdentifier));
        } catch (final RuntimeException ignored) {
            return null;
        }
    }
}
