package com.notquests.paper.events;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;

import com.notquests.paper.NotQuests;

public class InventoryEvents implements Listener {
    private final NotQuests main;

    public InventoryEvents(NotQuests main) {
        this.main = main;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        final Player player = e.getPlayer();
        main.getCorePlugin().journalPlayerJoined(
                player.getWorld().getName(),
                slot -> replaceJournal(player, slot));
    }

    @EventHandler
    public void onPlayerItemInteract(PlayerInteractEvent event) {
        final Player player = event.getPlayer();
        final ItemStack itemInHand = player.getInventory().getItemInMainHand();
        final var questPlayer = main.getRegistryAdapter().activePaperPlayer(player.getUniqueId());
        main.getCorePlugin().journalItemUsed(
                questPlayer,
                event.getAction().equals(Action.RIGHT_CLICK_AIR)
                        || event.getAction().equals(Action.RIGHT_CLICK_BLOCK),
                isJournal(itemInHand));
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent e) {
        final Player player = e.getPlayer();
        main.getCorePlugin().journalPlayerRespawned(
                player.getWorld().getName(),
                e.getRespawnLocation().getWorld() == null ? "" : e.getRespawnLocation().getWorld().getName(),
                slot -> isJournal(player.getInventory().getItem(slot)),
                slot -> replaceJournal(player, slot));
    }

    @EventHandler
    public void onDeath(EntityDeathEvent e) {
        if (e.getEntity() instanceof Player player) {
            main.getCorePlugin().journalPlayerDied(
                    player.getWorld().getName(),
                    e.getDrops().stream().anyMatch(this::isJournal),
                    () -> e.getDrops().removeIf(this::isJournal));
        }
    }

    @EventHandler
    public void onPickupItem(EntityPickupItemEvent e) {
        if (e.getEntity() instanceof Player player) {
            main.getCorePlugin().journalItemPickedUpOrDropped(
                    player.getWorld().getName(),
                    isJournal(e.getItem().getItemStack()),
                    () -> e.setCancelled(true));
        }
    }

    @EventHandler
    public void onDropItem(PlayerDropItemEvent e) {
        final Player player = e.getPlayer();
        main.getCorePlugin().journalItemPickedUpOrDropped(
                player.getWorld().getName(),
                isJournal(e.getItemDrop().getItemStack()),
                () -> e.setCancelled(true));
    }

    @EventHandler
    public void onInventoryUse(InventoryClickEvent e) {
        if (e.getWhoClicked() instanceof Player player && e.getClickedInventory() != null) {
            main.getCorePlugin().journalInventoryClicked(
                    player.getWorld().getName(),
                    isJournal(e.getCurrentItem()),
                    slot -> isJournal(player.getInventory().getItem(slot)),
                    () -> e.setCancelled(true),
                    player.getGameMode().equals(GameMode.CREATIVE)
                            ? () -> {
                                player.closeInventory();
                                player.updateInventory();
                            }
                            : null,
                    slot -> replaceJournal(player, slot));
        }
    }

    @EventHandler
    public void playerChangeWorldEvent(PlayerChangedWorldEvent e) {
        final Player player = e.getPlayer();
        main.getCorePlugin().journalPlayerJoined(
                player.getWorld().getName(),
                slot -> replaceJournal(player, slot));
    }

    private boolean isJournal(final ItemStack item) {
        final ItemStack journal = main.journalItem();
        return item != null && journal != null && item.isSimilar(journal);
    }

    private void replaceJournal(final Player player, final int slot) {
        final ItemStack journal = main.journalItem();
        if (journal == null) {
            return;
        }
        player.getInventory().remove(journal);
        player.getInventory().setItem(slot, journal);
    }
}
