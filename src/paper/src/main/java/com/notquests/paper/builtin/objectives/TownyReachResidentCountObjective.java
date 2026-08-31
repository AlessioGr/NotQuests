package com.notquests.paper.builtin.objectives;

import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;

import com.notquests.core.platform.NotQuestsAdapter;
import com.notquests.core.registry.NotQuestsRegistry.Objectives;
import com.notquests.paper.NotQuests;

import java.util.UUID;

public final class TownyReachResidentCountObjective {
    public static final String TYPE = "TownyReachResidentCount";
    private static final String AMOUNT = "amount";
    private static final String DO_NOT_COUNT_PREVIOUS_RESIDENTS = "doNotCountPreviousResidents";

    private TownyReachResidentCountObjective() {}

    public static void register(final NotQuests main, final NotQuestsAdapter adapter) {
        if (!main.getCorePlugin().integrationEnabled("Towny")) {
            return;
        }
        adapter.objectives().objective(TYPE)
                .displayName("Reach Towny Resident Count")
                .description("Counts when the player's Towny town reaches a target resident count.")
                .field(
                        AMOUNT,
                        adapter.fields().numberExpression(false).progressNeeded(),
                        "Minimum resident count the player's town must reach.")
                .flag(
                        DO_NOT_COUNT_PREVIOUS_RESIDENTS,
                        adapter.fields().presenceFlag().invertedBooleanConfig("specifics.countPreviousResidents"),
                        "Only count residents added after this objective unlocks; existing residents do not count.")
                .taskDescription((objective, questPlayer, activeObjective) ->
                        main.getCorePlugin().townyResidentCountTaskDescription(
                                questPlayer, objective, activeObjective))
                .onUnlock((objective, questPlayer, loading) -> {
                    main.getCorePlugin().townyResidentCountObjectiveUnlocked(
                            objective,
                            loading,
                            currentResidentCount(questPlayer == null ? "" : questPlayer.playerIdentifier()));
                })
                .register();
    }

    private static int currentResidentCount(final String playerIdentifier) {
        final Resident resident = resident(playerIdentifier);
        if (resident == null) {
            return 0;
        }
        final Town town = resident.getTownOrNull();
        return town == null ? 0 : town.getNumResidents();
    }

    private static Resident resident(final String playerIdentifier) {
        try {
            return TownyUniverse.getInstance().getResident(UUID.fromString(playerIdentifier));
        } catch (final RuntimeException ignored) {
            return null;
        }
    }
}
