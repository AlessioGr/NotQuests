package com.notquests.builtin.actions;

import com.notquests.core.platform.NotQuestsAdapter;

public final class CloseInventoryAction {
    private CloseInventoryAction() {}

    public static void register(final NotQuestsAdapter adapter) {
        adapter.actions().action("CloseInventory")
                .displayName("Close Inventory")
                .description("Closes the target player's currently open inventory.")
                .singleLine((action, arguments) -> {})
                .execute((action, questPlayer, objects) -> {
                    if (questPlayer != null && questPlayer.hasPlayer()) {
                        questPlayer.closeInventory();
                    }
                })
                .actionDescription((action, questPlayer, objects) -> "Closes the target player's inventory.")
                .register();
    }
}
