package com.notquests.builtin.objectives;

import com.notquests.core.platform.NotQuestsAdapter;
import com.notquests.core.registry.NotQuestsRegistry.Objectives;

import java.util.Map;

public final class BreedObjective {
    private BreedObjective() {}

    public static void register(final NotQuestsAdapter adapter) {
        simpleEntityObjective(
                        adapter,
                        "BreedMobs",
                        "Breed Mobs",
                        "Counts matching mobs bred by the player.",
                        "mobToBreed",
                        "chat.objectives.taskDescription.breed.base",
                        "%ENTITYTOBREED%")
                .onPlayerBreedEntity((event, objective) -> {
                    if (countsEntity(objective.text("entityType"), event.entityTypeId())) {
                        objective.addProgress(1);
                    }
                })
                .register();
    }

    static Objectives.Builder simpleEntityObjective(
            final NotQuestsAdapter adapter,
            final String id,
            final String displayName,
            final String description,
            final String configPath,
            final String taskKey,
            final String taskPlaceholder) {
        return adapter.objectives().objective(id)
                .displayName(displayName)
                .description(description)
                .field(
                        "entityType",
                        adapter.fields().entityType().config("specifics." + configPath),
                        "Entity type that counts, or any to count every matching event.")
                .field(
                        "amount",
                        adapter.fields().numberExpression().progressNeeded(),
                        "Number of matching events required.")
                .taskDescription((objective, questPlayer, activeObjective) -> adapter.objectiveTaskText(
                        taskKey,
                        questPlayer,
                        activeObjective,
                        Map.of(taskPlaceholder, objective.text("entityType"))));
    }

    static boolean countsEntity(final String target, final String actual) {
        return target.equalsIgnoreCase("any") || target.equalsIgnoreCase(actual);
    }
}
