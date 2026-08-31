package com.notquests.builtin.objectives;

import com.notquests.core.platform.NotQuestsAdapter;

public final class FeedMobsObjective {
    private FeedMobsObjective() {}

    public static void register(final NotQuestsAdapter adapter) {
        BreedObjective.simpleEntityObjective(
                        adapter,
                        "FeedMobs",
                        "Feed Mobs",
                        "Counts matching mobs fed by the player.",
                        "mobToFeed",
                        "chat.objectives.taskDescription.feedMobs.base",
                        "%ENTITYTOFEED%")
                .onPlayerFeedEntity((event, objective) -> {
                    if (BreedObjective.countsEntity(objective.text("entityType"), event.entityTypeId())) {
                        objective.addProgress(1);
                    }
                })
                .register();
    }
}
