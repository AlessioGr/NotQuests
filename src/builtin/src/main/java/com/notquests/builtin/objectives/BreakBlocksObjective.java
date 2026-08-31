package com.notquests.builtin.objectives;

import com.notquests.core.items.ItemSelection;
import com.notquests.core.platform.NotQuestsAdapter;

import java.util.Map;

public final class BreakBlocksObjective {
    private static final String MATERIALS = "materials";
    private static final String AMOUNT = "amount";
    private static final String DO_NOT_DEDUCT_IF_BLOCK_IS_PLACED = "doNotDeductIfBlockIsPlaced";

    private BreakBlocksObjective() {}

    public static void register(final NotQuestsAdapter adapter) {
        adapter.objectives().objective("BreakBlocks")
                .displayName("Break Blocks")
                .description("Counts matching blocks broken by the player.")
                .field(
                        MATERIALS,
                        adapter.fields().itemSelection().config("specifics.itemStackSelection"),
                        "Blocks or NotQuests custom items that count when broken. Supports one value or a comma-separated list.")
                .field(
                        AMOUNT,
                        adapter.fields().numberExpression().progressNeeded(),
                        "Number of matching blocks the player must break. Supports math and NotQuests number variables.")
                .flag(
                        DO_NOT_DEDUCT_IF_BLOCK_IS_PLACED,
                        adapter.fields().presenceFlag().invertedBooleanConfig("specifics.deductIfBlockPlaced"),
                        "Stops NotQuests from removing progress when the player places a matching block again.")
                .taskDescription((objective, questPlayer, activeObjective) -> adapter.objectiveTaskText(
                                "chat.objectives.taskDescription.breakBlocks.base",
                                questPlayer,
                                activeObjective,
                                Map.of(
                                        "%BLOCKTOBREAK%",
                                        listedMaterials(objective.itemSelection(MATERIALS)))))
                .onPlayerBreakBlock((event, objective) -> {
                    final ItemSelection selection = objective.itemSelection(MATERIALS);
                    if (selection != null && selection.includesMaterial(event.materialId())) {
                        objective.addProgress(1);
                    }
                })
                .onPlayerPlaceBlock((event, objective) -> {
                    final ItemSelection selection = objective.itemSelection(MATERIALS);
                    if (selection != null
                            && selection.includesMaterial(event.materialId())
                            && !objective.flag(DO_NOT_DEDUCT_IF_BLOCK_IS_PLACED)) {
                        objective.removeProgress(1, false);
                    }
                })
                .register();
    }

    private static String listedMaterials(final ItemSelection selection) {
        return selection == null ? "" : selection.listedMaterials("main");
    }
}
