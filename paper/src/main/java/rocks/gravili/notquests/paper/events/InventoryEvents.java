/*
 * NotQuests - A Questing plugin for Minecraft Servers
 * Copyright (C) 2021-2022 Alessio Gravili
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package rocks.gravili.notquests.paper.events;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import rocks.gravili.notquests.paper.NotQuests;

public class InventoryEvents implements Listener {
    private final NotQuests main;


    public InventoryEvents(NotQuests main) {
        this.main = main;
    }

    public boolean isItemSlotWorld(final String worldName) {
        if (main.getConfiguration().journalItemEnabledWorlds.contains("*")) {
            return true;
        }
        for (final String itemSlotWorldName : main.getConfiguration().journalItemEnabledWorlds) {
            if (worldName.equalsIgnoreCase(itemSlotWorldName)) {
                return true;
            }
        }
        return false;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        final Player player = e.getPlayer();

        if (isItemSlotWorld(player.getWorld().getName())) {
            addJournalToInventory(player);
        }
    }


    @EventHandler
    public void onPlayerItemInteract(PlayerInteractEvent event) {
        final Player player = event.getPlayer();
        final ItemStack itemInHand = player.getInventory().getItemInMainHand();
        if (event.getAction().equals(Action.RIGHT_CLICK_AIR) || event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
            if (itemInHand.isSimilar(main.getConfiguration().journalItem)) {
                main.getGuiManager().showActiveQuestsGUI(main.getQuestPlayerManager().getActiveQuestPlayer(player.getUniqueId()));
            }
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent e) {
        final Player player = e.getPlayer();
        if (isItemSlotWorld(player.getWorld().getName()) && e.getRespawnLocation().getWorld() != null && isItemSlotWorld(e.getRespawnLocation().getWorld().getName())) {
            final ItemStack journalItem = player.getInventory().getItem(main.getConfiguration().journalInventorySlot);
            if (journalItem == null || !journalItem.isSimilar(main.getConfiguration().journalItem)) {
                addJournalToInventory(player);
            }
        }
    }

    @EventHandler
    public void onDeath(EntityDeathEvent e) {
        if (e.getEntity() instanceof Player player) {
            if (isItemSlotWorld(player.getWorld().getName())) {
                e.getDrops().remove(main.getConfiguration().journalItem);
            }
        }


    }

    @EventHandler
    public void onPickupItem(EntityPickupItemEvent e) {
        if (e.getEntity() instanceof Player player) {

            if (isItemSlotWorld(player.getWorld().getName())) {
                if (e.getItem().getItemStack().isSimilar(main.getConfiguration().journalItem)) {
                    e.setCancelled(true);
                }
            }
        }

    }

    @EventHandler
    public void onDropItem(PlayerDropItemEvent e) {
        final Player player = e.getPlayer();

        if (isItemSlotWorld(player.getWorld().getName())) {

            if (e.getItemDrop().getItemStack().isSimilar(main.getConfiguration().journalItem)) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onInventoryUse(InventoryClickEvent e) {
        if (e.getWhoClicked() instanceof Player player && e.getClickedInventory() != null) {
            if (isItemSlotWorld(player.getWorld().getName())) {
                ItemStack item = e.getCurrentItem();

                if (item != null && item.isSimilar(main.getConfiguration().journalItem)) {
                    e.setCancelled(true);

                    if (player.getGameMode().equals(GameMode.CREATIVE)) {
                        player.closeInventory();
                        player.updateInventory();
                    }
                }

                final ItemStack journalItem = player.getInventory().getItem(main.getConfiguration().journalInventorySlot);
                if (journalItem == null || !journalItem.isSimilar(main.getConfiguration().journalItem)) {
                    addJournalToInventory(player);
                }
            }
        }

    }


    @EventHandler
    public void playerChangeWorldEvent(PlayerChangedWorldEvent e) {
        final Player player = e.getPlayer();

        if (isItemSlotWorld(player.getWorld().getName())) {
            addJournalToInventory(player);
        }
    }

    public void addJournalToInventory(Player player) {
        player.getInventory().remove(main.getConfiguration().journalItem);
        player.getInventory().setItem(main.getConfiguration().journalInventorySlot, main.getConfiguration().journalItem);
    }
}
