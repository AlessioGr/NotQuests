package com.notquests.builtin.variables;

import com.notquests.core.NotQuestsPlugin;
import com.notquests.core.items.ItemSelection;
import com.notquests.core.platform.NotQuestsAdapter;
import com.notquests.core.platform.PlatformPlayer;
import com.notquests.core.registry.NotQuestsRegistry.Variables.Context;
import com.notquests.core.registry.NotQuestsRegistry.Variables;

import java.util.ArrayList;
import java.util.List;

public final class InventoryVariable {
    private InventoryVariable() {}

    public static void register(final NotQuestsPlugin plugin, final NotQuestsAdapter adapter) {
        adapter.variables()
                .itemStackListVariable("Inventory")
                .displayName("Inventory")
                .description("Reads or changes the target player's inventory contents.")
                .singular("Inventory")
                .plural("Inventory")
                .get((questPlayer, objects) -> questPlayer == null ? List.of() : questPlayer.inventoryItems())
                .field("skipItemIfInventoryFull", adapter.fields().presenceFlag(), "Keeps overflow items from being dropped when this action adds items to a full inventory.")
                .set((newValue, context) -> setInventory(plugin, newValue, context))
                .possibleValues((questPlayer, objects) -> adapter.itemSelectionOptions())
                .register();
    }

    private static boolean setInventory(
            final NotQuestsPlugin plugin,
            final List<ItemSelection> newValue,
            final Variables.Context context) {
        final PlatformPlayer questPlayer = context.questPlayer();
        if (questPlayer == null) {
            return false;
        }
        final List<ItemSelection> values = newValue == null ? List.of() : newValue;
        if (context.bool("clear", false)) {
            return questPlayer.setInventoryItems(List.of());
        }
        if (context.bool("add", false)) {
            return questPlayer.addInventoryItems(
                    plugin.resolveItems(actionItems(context, values)),
                    !context.bool("skipItemIfInventoryFull", false));
        }
        if (context.bool("remove", false)) {
            return questPlayer.removeInventoryItems(plugin.resolveItems(actionItems(context, values)));
        }
        return questPlayer.setInventoryItems(plugin.resolveItems(values));
    }

    private static List<ItemSelection> actionItems(
            final Variables.Context context,
            final List<ItemSelection> fallback) {
        final Object value = context.value("itemStackActionItems");
        if (!(value instanceof final List<?> list)) {
            return fallback;
        }
        final ArrayList<ItemSelection> items = new ArrayList<>();
        for (final Object item : list) {
            if (item instanceof final ItemSelection selection) {
                items.add(selection);
            }
        }
        return List.copyOf(items);
    }
}
