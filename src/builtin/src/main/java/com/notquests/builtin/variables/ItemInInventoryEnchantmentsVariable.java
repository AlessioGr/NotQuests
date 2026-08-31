package com.notquests.builtin.variables;

import com.notquests.core.platform.NotQuestsAdapter;
import com.notquests.core.registry.NotQuestsRegistry.Variables.Context;
import com.notquests.core.registry.NotQuestsRegistry.Variables;

public final class ItemInInventoryEnchantmentsVariable {
    private ItemInInventoryEnchantmentsVariable() {}

    public static void register(final NotQuestsAdapter adapter) {
        adapter.variables()
                .listVariable("ItemInInventoryEnchantments")
                .displayName("Item In Inventory Enchantments")
                .description("Lists enchantments on the item in one target-player inventory slot.")
                .singular("Enchantment For Specific Item In Inventory")
                .plural("Enchantments For Specific Item In Inventory")
                .field("ItemSlot", adapter.fields().text(adapter::inventorySlotIds), "Equipment slot name or inventory slot number to inspect.")
                .get(context -> adapter.inventoryItemEnchantments(context.questPlayer(), slotId(context)))
                .possibleValues(context -> adapter.enchantmentIds())
                .register();
    }

    private static String slotId(final Variables.Context context) {
        return context.text("ItemSlot");
    }
}
