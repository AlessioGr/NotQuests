package com.notquests.builtin.variables;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.notquests.builtin.TestPlatformPlayer;
import com.notquests.core.NotQuestsPlugin;
import com.notquests.core.actions.Action;
import com.notquests.core.items.ItemSelection;
import com.notquests.core.items.ItemStackSelection;
import com.notquests.core.items.SavedItems;
import com.notquests.core.platform.NotQuestsAdapter;
import com.notquests.core.platform.PlatformPlayer;
import com.notquests.core.registry.NotQuestsRegistry;
import com.notquests.core.variables.VariableDataType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class EnderChestVariableTest {
    @Test
    void itemStackListAddUsesEnderChestLeafWithOldOverflowFlags() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final NotQuestsAdapter adapter = plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
        EnderChestVariable.register(plugin, adapter);
        final FakeQuestPlayer player = new FakeQuestPlayer();

        plugin.executeVariableAction(
                VariableDataType.ITEMSTACKLIST,
                data(
                        "variableName", "EnderChest",
                        "operator", "add",
                        "itemStack", ItemStackSelection.parse("ender_pearl").withAmount(2),
                        "additionalBooleans", Map.of(
                                "add", "1",
                                "addToInventoryIfEnderChestFull", "1",
                                "skipItemIfEnderChestFull", "1")),
                player);

        assertTrue(player.added);
        assertTrue(player.addOverflowToInventory);
        assertFalse(player.dropOverflow);
        assertEquals(1, player.addedItems.size());
        assertTrue(player.addedItems.getFirst().selection().includesMaterial("ender_pearl"));
        assertEquals(2, player.addedItems.getFirst().selection().amount());
    }

    private static Action data(final Object... pairs) {
        final Action values = new Action(0, "VariableAction", null);
        for (int i = 0; i < pairs.length; i += 2) {
            values.setValue(String.valueOf(pairs[i]), pairs[i + 1]);
        }
        return values;
    }

    private static final class FakeQuestPlayer implements TestPlatformPlayer {
        private boolean added;
        private boolean addOverflowToInventory;
        private boolean dropOverflow;
        private List<SavedItems.ItemChoice> addedItems = List.of();

        @Override
        public boolean hasPlayer() {
            return true;
        }

        @Override
        public String playerIdentifier() {
            return "test-player";
        }

        @Override
        public String playerName() {
            return "TestPlayer";
        }

        @Override
        public List<ItemSelection> enderChestItems() {
            return List.of(ItemStackSelection.parse("stone").withAmount(3));
        }

        @Override
        public boolean addEnderChestItems(
                final List<SavedItems.ItemChoice> items,
                final boolean addOverflowToInventory,
                final boolean dropOverflow) {
            this.added = true;
            this.addOverflowToInventory = addOverflowToInventory;
            this.dropOverflow = dropOverflow;
            this.addedItems = List.copyOf(items);
            return true;
        }

        @Override
        public long currentWorldTimeTicks() {
            return 0;
        }

        @Override
        public void sendMessage(final String miniMessage) {}

        @Override
        public void sendActionBar(final String miniMessage) {}

        @Override
        public void showProgressBossBar(final String miniMessage, final double progress) {}

        @Override
        public void hideProgressBossBar() {}

        @Override
        public void showTitle(
                final String title,
                final String subtitle,
                final java.time.Duration fadeIn,
                final java.time.Duration stay,
                final java.time.Duration fadeOut) {}
        @Override
        public void chat(final String message) {}

        @Override
        public void performCommand(final String command) {}

        @Override
        public void closeInventory() {}

        @Override
        public com.notquests.core.platform.NQLocation lookingAtBlock(final double maxDistance) {
            return null;
        }
        @Override
        public boolean showGui(final com.notquests.core.gui.GuiService.ResolvedGui gui) {
        return false;
    }
    }
}
