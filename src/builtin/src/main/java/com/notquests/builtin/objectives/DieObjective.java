package com.notquests.builtin.objectives;

import com.notquests.core.platform.NotQuestsAdapter;

import java.util.Map;

public final class DieObjective {
    private static final String AMOUNT = "amount";
    private static final String CAUSE = "cause";

    private DieObjective() {}

    public static boolean countsDamageType(final String configuredDamageType, final String actualDamageType) {
        return configuredDamageType == null
                || configuredDamageType.isBlank()
                || (actualDamageType != null && configuredDamageType.equalsIgnoreCase(actualDamageType));
    }

    public static void register(final NotQuestsAdapter adapter) {
        adapter.objectives().objective("Die")
                .displayName("Die")
                .description("Counts times the player dies, optionally requiring a specific death cause.")
                .field(
                        AMOUNT,
                        adapter.fields().numberExpression().progressNeeded(),
                        "Number of times the player must die.")
                .flag(
                        CAUSE,
                        adapter.fields().text(adapter::damageTypeIds).config("specifics.damageType"),
                        "Only count deaths caused by this damage type, such as fall, lava, drown, or player_attack.")
                .taskDescription((objective, questPlayer, activeObjective) -> adapter.objectiveTaskText(
                        "chat.objectives.taskDescription.die.base",
                        questPlayer,
                        activeObjective,
                        Map.of("%DAMAGETYPE%", objective.text(CAUSE).isBlank() ? "any" : objective.text(CAUSE))))
                .onPlayerDeath((event, objective) -> {
                    if (countsDamageType(objective.text(CAUSE), event.damageTypeId())) {
                        objective.addProgress(1);
                    }
                })
                .register();
    }
}
