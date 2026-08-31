package com.notquests.builtin.variables;

import com.notquests.core.NotQuestsPlugin;
import com.notquests.core.items.ItemSelection;
import com.notquests.core.platform.NQLocation;
import com.notquests.core.platform.NotQuestsAdapter;
import com.notquests.core.registry.NotQuestsRegistry.Variables.Context;
import com.notquests.core.registry.NotQuestsRegistry.Variables;
import com.notquests.core.variables.NumberExpression;

import java.util.ArrayList;
import java.util.List;

public final class ContainerInventoryVariable {
    private ContainerInventoryVariable() {}

    public static void register(final NotQuestsPlugin plugin, final NotQuestsAdapter adapter) {
        adapter.variables()
                .itemStackListVariable("ContainerInventory")
                .displayName("Container Inventory")
                .description("Reads or changes the inventory of a container block at a configured location.")
                .singular("Container Inventory")
                .plural("Container Inventory")
                .field("world", adapter.fields().text(adapter::worldNames), "World containing the container block.")
                .field("x", adapter.fields().numberExpression(), "Container block X coordinate.")
                .field("y", adapter.fields().numberExpression(), "Container block Y coordinate.")
                .field("z", adapter.fields().numberExpression(), "Container block Z coordinate.")
                .field("skipItemIfInventoryFull", adapter.fields().presenceFlag(), "Keeps overflow items from being dropped when this action adds items to a full container.")
                .get(context -> adapter.containerInventoryItems(location(adapter, context)))
                .set((newValue, context) -> setContainer(plugin, adapter, newValue, context))
                .possibleValues((questPlayer, objects) -> adapter.itemSelectionOptions())
                .register();
    }

    private static boolean setContainer(
            final NotQuestsPlugin plugin,
            final NotQuestsAdapter adapter,
            final List<ItemSelection> newValue,
            final Variables.Context context) {
        final NQLocation location = location(adapter, context);
        final List<ItemSelection> values = newValue == null ? List.of() : newValue;
        if (context.bool("clear", false)) {
            return adapter.setContainerInventoryItems(location, List.of());
        }
        if (context.bool("add", false)) {
            return adapter.addContainerInventoryItems(
                    location,
                    plugin.resolveItems(actionItems(context, values)),
                    !context.bool("skipItemIfInventoryFull", false));
        }
        if (context.bool("remove", false)) {
            return adapter.removeContainerInventoryItems(
                    location, plugin.resolveItems(actionItems(context, values)));
        }
        return adapter.setContainerInventoryItems(location, plugin.resolveItems(values));
    }

    private static NQLocation location(final NotQuestsAdapter adapter, final Variables.Context context) {
        return adapter.location(
                context.text("world"),
                number(adapter, context, "x"),
                number(adapter, context, "y"),
                number(adapter, context, "z"));
    }

    private static double number(
            final NotQuestsAdapter adapter,
            final Variables.Context context,
            final String name) {
        return new NumberExpression(adapter, context.text(name)).calculateValue(context.questPlayer());
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
