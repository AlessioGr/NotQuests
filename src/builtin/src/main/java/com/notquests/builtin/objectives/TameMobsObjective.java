package com.notquests.builtin.objectives;

import com.notquests.core.platform.NotQuestsAdapter;

public final class TameMobsObjective {
    private TameMobsObjective() {}

    public static void register(final NotQuestsAdapter adapter) {
        BreedObjective.simpleEntityObjective(
                        adapter,
                        "TameMobs",
                        "Tame Mobs",
                        "Counts matching mobs tamed by the player.",
                        "mobToTame",
                        "chat.objectives.taskDescription.tameMobs.base",
                        "%ENTITYTOTAME%")
                .onPlayerTameEntity((event, objective) -> {
                    if (BreedObjective.countsEntity(objective.text("entityType"), event.entityTypeId())) {
                        objective.addProgress(1);
                    }
                })
                .register();
    }
}
