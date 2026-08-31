package com.notquests.paper.events;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.bukkit.Material;
import org.bukkit.entity.Cow;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.Test;

class QuestEventsLivestockTest {
    @Test
    void milkCowRequiresAnUncancelledCowInteractionWithBucketInTheActualEventHand() {
        final PlayerInteractEntityEvent event = mock(PlayerInteractEntityEvent.class);
        final Player player = mock(Player.class);
        final PlayerInventory inventory = mock(PlayerInventory.class);
        when(event.getPlayer()).thenReturn(player);
        when(event.getRightClicked()).thenReturn(mock(Cow.class));
        when(event.getHand()).thenReturn(EquipmentSlot.HAND);
        when(player.getInventory()).thenReturn(inventory);
        final ItemStack bucket = item(Material.BUCKET);
        when(inventory.getItem(EquipmentSlot.HAND)).thenReturn(bucket);

        assertTrue(QuestEvents.isMilkingCow(event));

        when(event.isCancelled()).thenReturn(true);
        assertFalse(QuestEvents.isMilkingCow(event));

        when(event.isCancelled()).thenReturn(false);
        when(event.getRightClicked()).thenReturn(mock(Pig.class));
        assertFalse(QuestEvents.isMilkingCow(event));
    }

    @Test
    void milkCowReadsTheActualEventHandSoAnOffHandCallbackDoesNotReuseTheMainHandBucket() {
        final PlayerInteractEntityEvent event = mock(PlayerInteractEntityEvent.class);
        final Player player = mock(Player.class);
        final PlayerInventory inventory = mock(PlayerInventory.class);
        when(event.getPlayer()).thenReturn(player);
        when(event.getRightClicked()).thenReturn(mock(Cow.class));
        when(event.getHand()).thenReturn(EquipmentSlot.OFF_HAND);
        when(player.getInventory()).thenReturn(inventory);
        final ItemStack bucket = item(Material.BUCKET);
        final ItemStack air = item(Material.AIR);
        when(inventory.getItem(EquipmentSlot.HAND)).thenReturn(bucket);
        when(inventory.getItem(EquipmentSlot.OFF_HAND)).thenReturn(air);

        assertFalse(QuestEvents.isMilkingCow(event));
    }

    @Test
    void shearSheepRequiresAnUncancelledSheepEvent() {
        final PlayerShearEntityEvent event = mock(PlayerShearEntityEvent.class);
        when(event.getEntity()).thenReturn(mock(Sheep.class));

        assertTrue(QuestEvents.isShearingSheep(event));

        when(event.isCancelled()).thenReturn(true);
        assertFalse(QuestEvents.isShearingSheep(event));
    }

    private static ItemStack item(final Material material) {
        final ItemStack item = mock(ItemStack.class);
        when(item.getType()).thenReturn(material);
        return item;
    }
}
