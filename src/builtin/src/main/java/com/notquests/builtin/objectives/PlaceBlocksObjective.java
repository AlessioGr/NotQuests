package com.notquests.builtin.objectives;

import com.notquests.core.items.ItemSelection;
import com.notquests.core.platform.NotQuestsAdapter;

import java.util.Map;

public final class PlaceBlocksObjective {
    private static final String MATERIALS = "materials";
    private static final String AMOUNT = "amount";
    private static final String DO_NOT_DEDUCT_IF_BLOCK_IS_BROKEN = "doNotDeductIfBlockIsBroken";

    private PlaceBlocksObjective() {}

    public static void register(final NotQuestsAdapter adapter) {
        adapter.objectives().objective("PlaceBlocks")
                .displayName("Place Blocks")
                .description("Counts matching blocks placed by the player.")
                .field(
                        MATERIALS,
                        adapter.fields().itemSelection().config("specifics.itemStackSelection"),
                        "Blocks or NotQuests custom items that count when placed. Supports one value or a comma-separated list.")
                .field(
                        AMOUNT,
                        adapter.fields().numberExpression().progressNeeded(),
                        "Number of matching blocks the player must place. Supports math and NotQuests number variables.")
                .flag(
                        DO_NOT_DEDUCT_IF_BLOCK_IS_BROKEN,
                        adapter.fields().presenceFlag().invertedBooleanConfig("specifics.deductIfBlockBroken"),
                        "Stops NotQuests from removing progress when the player breaks a matching placed block.")
                .taskDescription((objective, questPlayer, activeObjective) -> adapter.objectiveTaskText(
                                "chat.objectives.taskDescription.placeBlocks.base",
                                questPlayer,
                                activeObjective,
                                Map.of(
                                        "%BLOCKTOPLACE%",
                                        listedMaterials(objective.itemSelection(MATERIALS)))))
                .onPlayerPlaceBlock((event, objective) -> {
                    final ItemSelection selection = objective.itemSelection(MATERIALS);
                    if (selection != null && selection.includesMaterial(event.materialId())) {
                        objective.addProgress(1);
                    }
                })
                .onPlayerBreakBlock((event, objective) -> {
                    final ItemSelection selection = objective.itemSelection(MATERIALS);
                    if (selection != null
                            && selection.includesMaterial(event.materialId())
                            && !objective.flag(DO_NOT_DEDUCT_IF_BLOCK_IS_BROKEN)) {
                        objective.removeProgress(1, false);
                    }
                })
                .register();
    }

    private static String listedMaterials(final ItemSelection selection) {
        return selection == null ? "" : selection.listedMaterials("main");
    }
}
