package com.notquests.paper.conditions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import com.notquests.builtin.conditions.VariableCondition;
import com.notquests.core.NotQuestsPlugin;
import com.notquests.core.items.ItemSelection;
import com.notquests.core.registry.NotQuestsRegistry.Conditions;
import com.notquests.core.registry.NotQuestsRegistry;
import com.notquests.paper.NotQuests;
import com.notquests.paper.PaperItems.Selection;
import com.notquests.paper.PaperNotQuestsAdapter;

import java.util.LinkedHashMap;
import java.util.List;

class PaperItemStackListConditionTest {
    private NotQuests main;
    private NotQuestsPlugin plugin;
    private PaperNotQuestsAdapter adapter;

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
        main = mock(NotQuests.class, RETURNS_DEEP_STUBS);
        plugin = NotQuestsPlugin.create();
        when(main.getCorePlugin()).thenReturn(plugin);
        adapter = new PaperNotQuestsAdapter(main);
        VariableCondition.register(adapter);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void conditionUsesBukkitSimilarityAndAggregatesOnlyExactMatchingItems() {
        final ItemStack exactTwo = customSword(73, 2);
        final ItemStack exactThree = customSword(73, 3);
        final ItemStack differentMetadata = customSword(74, 20);
        registerItems(List.of(
                selection(exactTwo),
                selection(differentMetadata),
                selection(exactThree)));

        assertEquals("", check(selection(customSword(73, 5)), "contains"));
        assertEquals(
                "<yellow>test items must satisfy contains.",
                check(selection(customSword(73, 6)), "contains"));
        assertEquals(
                "<yellow>test items must satisfy contains.",
                check(selection(new ItemStack(Material.DIAMOND_SWORD)), "contains"),
                "a material-only stack must not match a named, lored or custom-model item");
        assertEquals(
                "<yellow>test items must satisfy equals.",
                check(selection(customSword(73, 5)), "equals"),
                "equals must reject a list containing a different item, even when matching stacks total correctly");
    }

    @Test
    void anySelectionMatchesOrdinaryInventoryEntries() {
        registerItems(List.of(selection(new ItemStack(Material.STONE, 4))));

        assertEquals("", check(com.notquests.core.items.ItemStackSelection.parse("any"), "contains"));
    }

    @Test
    void changingAmountKeepsTheCompleteExactItemPayload() {
        final ItemSelection original = selection(customSword(73, 2));

        final ItemSelection resized = original.withAmount(7);

        assertEquals(7, resized.amount());
        assertEquals(original.materialIds(), resized.materialIds());
        assertEquals(original.exactItems(), resized.exactItems());
    }

    private void registerItems(final List<ItemSelection> items) {
        adapter.variables()
                .itemStackListVariable("TestItems")
                .displayName("Test Items")
                .description("Exact Paper item values.")
                .singular("test item")
                .plural("test items")
                .get((questPlayer, objects) -> items)
                .register();
    }

    private String check(final ItemSelection required, final String operator) {
        final Conditions.Type condition = plugin.registry().conditions().stream()
                .filter(candidate -> candidate.id().equals("ItemStackList"))
                .findFirst()
                .orElseThrow();
        final com.notquests.paper.TestData data = new com.notquests.paper.TestData(new LinkedHashMap<>());
        data.setValue("variableName", "TestItems");
        data.setValue("operator", operator);
        data.setValue("expression", required);
        return condition.checker().check(new TestConditionData(data), null);
    }

    private ItemSelection selection(final ItemStack itemStack) {
        final Selection selection = new Selection(main);
        selection.addItemStack(itemStack);
        return selection;
    }

    @SuppressWarnings("deprecation")
    private static ItemStack customSword(final int modelData, final int amount) {
        final ItemStack itemStack = new ItemStack(Material.DIAMOND_SWORD, amount);
        final ItemMeta meta = itemStack.getItemMeta();
        meta.displayName(Component.text("Quest Blade"));
        meta.lore(List.of(Component.text("Only this blade counts")));
        meta.setCustomModelData(modelData);
        itemStack.setItemMeta(meta);
        return itemStack;
    }

    private record TestConditionData(com.notquests.paper.TestData data) implements Conditions.Data {
        @Override
        public void copyTo(final Conditions.Draft condition) {
            data.copyTo(condition);
        }

        @Override
        public Object value(final String name) {
            return data.value(name);
        }

        @Override
        public String text(final String name) {
            return data.text(name);
        }

        @Override
        public int integer(final String name) {
            return data.integer(name);
        }

        @Override
        public int integer(final String name, final int fallback) {
            return data.integer(name, fallback);
        }
    }
}
