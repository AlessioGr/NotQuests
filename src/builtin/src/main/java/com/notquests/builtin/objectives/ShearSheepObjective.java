package com.notquests.builtin.objectives;

import com.notquests.core.platform.NotQuestsAdapter;

import java.util.Map;

public final class ShearSheepObjective {
    private ShearSheepObjective() {}

    public static void register(final NotQuestsAdapter adapter) {
        adapter.objectives().objective("ShearSheep")
                .displayName("Shear Sheep")
                .description("Counts sheep sheared by the player.")
                .field(
                        "amount",
                        adapter.fields().numberExpression().progressNeeded(),
                        "Number of sheep the player must shear.")
                .flag(
                        "cancelShearing",
                        adapter.fields().presenceFlag().config("specifics.cancelShearing"),
                        "Cancels the vanilla shearing interaction while this objective is active.")
                .taskDescription((objective, questPlayer, activeObjective) -> adapter.objectiveTaskText(
                        "chat.objectives.taskDescription.shearSheep.base",
                        questPlayer,
                        activeObjective,
                        Map.of(
                                "%AMOUNTOFSHEEP%",
                                ""
                                        + (activeObjective != null
                                                ? activeObjective.progressNeeded()
                                                : objective.text("amount")))))
                .onPlayerShearSheep((event, objective) -> {
                    objective.addProgress(1);
                    if (objective.flag("cancelShearing")) {
                        event.cancel();
                    }
                })
                .register();
    }
}
