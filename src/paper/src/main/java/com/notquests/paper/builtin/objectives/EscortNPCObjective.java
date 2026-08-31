package com.notquests.paper.builtin.objectives;

import com.notquests.core.platform.NotQuestsAdapter;
import com.notquests.core.platform.PlatformPlayer;
import com.notquests.core.registry.NotQuestsRegistry.Objectives;
import com.notquests.core.structs.ActiveObjective;
import com.notquests.paper.NotQuests;

public final class EscortNPCObjective {
    private static final String TYPE = "EscortNPC";
    private static final String NPC_TO_ESCORT_ID = "npcToEscortId";
    private static final String DESTINATION_NPC_ID = "destinationNpcId";
    private static final String SPAWN_LOCATION = "spawnLocation";

    private EscortNPCObjective() {}

    public static void register(final NotQuests main, final NotQuestsAdapter adapter) {
        if (!main.getCorePlugin().integrationEnabled("Citizens")) {
            return;
        }
        adapter.objectives().objective(TYPE)
                .displayName("Escort NPC")
                .description("Counts when the player escorts one Citizens NPC to another Citizens NPC.")
                .field(
                        NPC_TO_ESCORT_ID,
                        adapter.fields().storedInteger(-1).config("specifics.NPCToEscortID"),
                        "Citizens NPC id of the NPC the player must escort.")
                .field(
                        DESTINATION_NPC_ID,
                        adapter.fields().storedInteger(-1).config("specifics.destinationNPCID"),
                        "Citizens NPC id of the destination NPC.")
                .flag(
                        SPAWN_LOCATION,
                        adapter.fields().storedLocation().config("specifics.spawnLocation"),
                        "Optional location where the escorted NPC should spawn before following the player.")
                .taskDescription((objective, questPlayer, activeObjective) -> taskDescription(main, objective, questPlayer, activeObjective))
                .onUnlock((objective, questPlayer, loading) ->
                        main.getCorePlugin().escortObjectiveUnlocked(
                                objective,
                                questPlayer,
                                loading,
                                main.integrations().citizens()::npcName,
                                main.integrations().citizens()::startFollowing))
                .onCompleteOrLock((objective, questPlayer, loading, completed) ->
                        main.getCorePlugin().escortObjectiveCompletedOrLocked(
                                objective,
                                loading,
                                main.integrations().citizens()::stopFollowing))
                .register();
    }

    private static String taskDescription(
            final NotQuests main,
            final Objectives.Data objective,
            final PlatformPlayer questPlayer,
            final ActiveObjective activeObjective) {
        return main.getCorePlugin().escortTaskDescription(
                questPlayer,
                activeObjective,
                objective,
                main.integrations().citizens()::npcName);
    }

}
