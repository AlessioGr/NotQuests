package com.notquests.paper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import com.notquests.core.NotQuestsPlugin;
import com.notquests.core.items.ItemSelection;
import com.notquests.core.items.ItemStackSelection;
import com.notquests.core.items.SavedItems;
import com.notquests.paper.PaperNotQuestsAdapter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class PaperItemsServerTest {
  @BeforeEach
  void setUp() {
    MockBukkit.mock();
  }

  @AfterEach
  void tearDown() {
    MockBukkit.unmock();
  }

  @Test
  @DisplayName("isEmpty treats null and AIR as empty, real items as non-empty")
  void isEmptyContract() {
    assertTrue(PaperItems.isEmpty(null), "null is empty");
    assertTrue(PaperItems.isEmpty(new ItemStack(Material.AIR)), "AIR is empty");
    assertFalse(PaperItems.isEmpty(new ItemStack(Material.STONE)), "a real item is not empty");
  }

  @Test
  void nativeSelectionKeepsAnyAndSavedItemAmounts() {
    final PaperItems.Selection any = new PaperItems.Selection(8);
    any.setAny(true);
    final ItemSelection anySelection = any;
    assertTrue(anySelection.any());
    assertEquals(8, anySelection.amount());

    final PaperItems.Selection savedItem = new PaperItems.Selection(13);
    savedItem.addNqItemName("QuestKey");
    final ItemSelection savedItemSelection = savedItem;
    assertEquals(List.of("QuestKey"), savedItemSelection.savedItemNames());
    assertEquals(13, savedItemSelection.amount());

    final PaperItems.Selection capturedStack = new PaperItems.Selection((NotQuests) null);
    capturedStack.addItemStack(new ItemStack(Material.DIAMOND, 6));
    assertEquals(6, capturedStack.amount());
  }

  @Test
  void nativeSelectionRequiresAnExplicitPaperPayload() {
    final PaperItems.Selection captured = new PaperItems.Selection(1);
    captured.addItemStack(new ItemStack(Material.DIAMOND, 3));
    final Map<String, Object> explicitPaperItem = captured.exactItems().getFirst();

    final PaperItems.Selection tagged = new PaperItems.Selection(1);
    tagged.addExactItem(explicitPaperItem);
    assertEquals(1, tagged.getItemStacks().size());

    final Map<String, Object> untaggedItem = new LinkedHashMap<>(explicitPaperItem);
    untaggedItem.remove("platform");
    final PaperItems.Selection untagged = new PaperItems.Selection(1);
    untagged.addExactItem(untaggedItem);
    assertTrue(untagged.getItemStacks().isEmpty());
  }

  @Test
  void adapterMaterializationRequiresAnExplicitPaperPayload() {
    final NotQuests main = mock(NotQuests.class, RETURNS_DEEP_STUBS);
    when(main.getCorePlugin()).thenReturn(NotQuestsPlugin.create());
    final PaperNotQuestsAdapter adapter = new PaperNotQuestsAdapter(main);
    final PaperItems.Selection captured = new PaperItems.Selection(1);
    captured.addItemStack(new ItemStack(Material.DIAMOND, 3));
    final Map<String, Object> explicitPaperItem = captured.exactItems().getFirst();

    final PaperItems.Selection tagged = adapter.materializeItems(List.of(new SavedItems.ItemChoice(
        exactSelection(explicitPaperItem), "")));
    assertEquals(1, tagged.getItemStacks().size());

    final Map<String, Object> untaggedItem = new LinkedHashMap<>(explicitPaperItem);
    untaggedItem.remove("platform");
    final PaperItems.Selection untagged = adapter.materializeItems(List.of(new SavedItems.ItemChoice(
        exactSelection(untaggedItem), "")));
    assertTrue(untagged.getItemStacks().isEmpty());
  }

  private static ItemSelection exactSelection(final Map<String, Object> exactItem) {
    return ItemStackSelection.of(List.of(), List.of(), List.of(exactItem), false, 3);
  }
}
