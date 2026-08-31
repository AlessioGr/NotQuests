package com.notquests.builtin.variables;

import com.notquests.core.NotQuestsPlugin;
import com.notquests.core.items.ItemSelection;
import com.notquests.core.platform.NotQuestsAdapter;
import com.notquests.core.platform.PlatformPlayer;
import com.notquests.core.registry.NotQuestsRegistry.Variables.Context;
import com.notquests.core.registry.NotQuestsRegistry.Variables;

import java.util.ArrayList;
import java.util.List;

public final class EnderChestVariable {
    private EnderChestVariable() {}

    public static void register(final NotQuestsPlugin plugin, final NotQuestsAdapter adapter) {
        adapter.variables()
                .itemStackListVariable("EnderChest")
                .displayName("EnderChest")
                .description("Reads or changes the target player's ender chest contents.")
                .singular("EnderChest Inventory")
                .plural("EnderChest Inventory")
                .get((questPlayer, objects) -> questPlayer == null ? List.of() : questPlayer.enderChestItems())
                .field("addToInventoryIfEnderChestFull", adapter.fields().presenceFlag(), "Puts overflow items in the player's normal inventory when their ender chest is full.")
                .field("skipItemIfEnderChestFull", adapter.fields().presenceFlag(), "Keeps overflow items from being dropped when the ender chest and fallback inventory are full.")
                .set((newValue, context) -> setEnderChest(plugin, newValue, context))
                .possibleValues((questPlayer, objects) -> adapter.itemSelectionOptions())
                .register();
    }

    private static boolean setEnderChest(
            final NotQuestsPlugin plugin,
            final List<ItemSelection> newValue,
            final Variables.Context context) {
        final PlatformPlayer questPlayer = context.questPlayer();
        if (questPlayer == null) {
            return false;
        }
        final List<ItemSelection> values = newValue == null ? List.of() : newValue;
        if (context.bool("clear", false)) {
            return questPlayer.setEnderChestItems(List.of());
        }
        if (context.bool("add", false)) {
            return questPlayer.addEnderChestItems(
                    plugin.resolveItems(actionItems(context, values)),
                    context.bool("addToInventoryIfEnderChestFull", false),
                    !context.bool("skipItemIfEnderChestFull", false));
        }
        if (context.bool("remove", false)) {
            return questPlayer.removeEnderChestItems(plugin.resolveItems(actionItems(context, values)));
        }
        return questPlayer.setEnderChestItems(plugin.resolveItems(values));
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
