package com.notquests.builtin.actions;

import com.notquests.core.NotQuestsPlugin;
import com.notquests.core.items.ItemSelection;
import com.notquests.core.platform.NotQuestsAdapter;

public final class GiveItemAction {
    private static final String MATERIAL = "material";
    private static final String AMOUNT = "amount";

    private GiveItemAction() {}

    public static void register(final NotQuestsPlugin plugin, final NotQuestsAdapter adapter) {
        adapter.actions()
                .action("GiveItem")
                .displayName("Give Item")
                .description("Gives one or more materials or NotQuests custom items to the target player.")
                .field(
                        MATERIAL,
                        adapter.fields().itemSelection().config("specifics.itemStackSelection"),
                        "Material, NotQuests custom item, hand item, any item, or comma-separated item selection to give.")
                .field(
                        AMOUNT,
                        adapter.fields().integer(1).config("specifics.nqitemamount"),
                        "Amount of each selected item stack to give.")
                .singleLine((action, arguments) -> {
                    action.setValue(MATERIAL, adapter.parseItemSelection(arguments.get(0)));
                    action.setValue(AMOUNT, arguments.size() >= 2 ? Integer.parseInt(arguments.get(1)) : 1);
                })
                .execute((action, questPlayer, objects) -> {
                    final ItemSelection selection = action.itemSelection(MATERIAL);
                    if (questPlayer == null
                            || !questPlayer.hasPlayer()
                            || selection == null
                            || !questPlayer.giveItems(plugin.resolveItems(
                                    selection.withAmount(Math.max(1, action.integer(AMOUNT, 1)))))) {
                        adapter.warn("Tried to execute GiveItem action with invalid target player or item selection.");
                    }
                })
                .actionDescription((action, questPlayer, objects) -> {
                    final ItemSelection selection = action.itemSelection(MATERIAL);
                    return "Gives item: " + (selection == null ? "" : selection.listedMaterials("main"));
                })
                .register();
    }
}
