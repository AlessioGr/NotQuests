package com.notquests.builtin.objectives;

import com.notquests.core.platform.NotQuestsAdapter;

import java.util.Locale;
import java.util.Map;

public final class KillMobsObjective {
    public static final String TYPE = "KillMobs";
    public static final String ENTITY_TYPE = "entityType";

    private KillMobsObjective() {}

    public static void register(final NotQuestsAdapter adapter) {
        adapter.objectives().objective(TYPE)
                .displayName("Kill Mobs")
                .description("Counts matching mobs killed by the player.")
                .field(
                        ENTITY_TYPE,
                        adapter.fields().entityType().config("specifics.mobToKill"),
                        "Entity type the player must kill, or `any` to count every mob.")
                .field(
                        "amount",
                        adapter.fields().numberExpression().progressNeeded(),
                        "Number of matching mobs the player must kill.")
                .flag(
                        "nametag_equals",
                        adapter.fields().greedyText().config("extras.nameTagEquals"),
                        "Only count mobs whose custom name exactly matches this text.")
                .flag(
                        "nametag_containsany",
                        adapter.fields().greedyText().config("extras.nameTagContainsAny"),
                        "Only count mobs whose custom name contains every word in this text.")
                .taskDescription((objective, questPlayer, activeObjective) -> adapter.objectiveTaskText(
                        "chat.objectives.taskDescription.killMobs.base",
                        questPlayer,
                        activeObjective,
                        Map.of("%MOBTOKILL%", objective.text(ENTITY_TYPE))))
                .onPlayerKillEntity((event, objective) -> {
                    if (event.selfAttributedDeath()) {
                        return;
                    }
                    final String target = objective.text(ENTITY_TYPE);
                    if (!target.equalsIgnoreCase("any")
                            && !target.equalsIgnoreCase(event.entityTypeId())) {
                        return;
                    }
                    if (matchesNameTag(
                            event.plainCustomName(),
                            objective.text("nametag_equals"),
                            objective.text("nametag_containsany"))) {
                        objective.addProgress(1);
                    }
                })
                .register();
    }

    private static boolean matchesNameTag(
            final String plainName,
            final String equals,
            final String containsEveryWord) {
        if (equals.isBlank() && containsEveryWord.isBlank()) {
            return true;
        }
        if (plainName == null || plainName.isBlank()) {
            return false;
        }
        final String lowerName = plainName.toLowerCase(Locale.ROOT);
        if (!containsEveryWord.isBlank()) {
            for (final String namePart : containsEveryWord.toLowerCase(Locale.ROOT).split("\\s+")) {
                if (!lowerName.contains(namePart)) {
                    return false;
                }
            }
        }
        return equals.isBlank() || lowerName.equals(equals.toLowerCase(Locale.ROOT));
    }
}
