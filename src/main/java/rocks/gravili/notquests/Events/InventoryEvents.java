/*
 * NotQuests - A Questing plugin for Minecraft Servers
 * Copyright (C) 2021 Alessio Gravili
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

package rocks.gravili.notquests.Events;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import rocks.gravili.notquests.NotQuests;

import java.util.ArrayList;

public class InventoryEvents implements Listener {
    private final NotQuests main;


    private final ItemStack journal;


    public InventoryEvents(NotQuests main) {
        this.main = main;
        journal = new ItemStack(Material.ENCHANTED_BOOK, 1);

        ItemMeta im = journal.getItemMeta();

        ArrayList<String> lore = new ArrayList<>();
        lore.add("§7A book containing all your quest information");

        im.setDisplayName("§9§oJournal");
        im.setLore(lore);

        journal.setItemMeta(im);

    }

    public boolean isItemSlotWorld(final String worldName) {
        if (main.getDataManager().getConfiguration().journalItemEnabledWorlds.contains("*")) {
            return true;
        }
        for (final String itemSlotWorldName : main.getDataManager().getConfiguration().journalItemEnabledWorlds) {
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
            player.getInventory().remove(journal);
            player.getInventory().setItem(8, journal);
        }
    }


    @EventHandler
    public void onPlayerItemInteract(PlayerInteractEvent event) {
        final Player player = event.getPlayer();
        final ItemStack itemInHand = player.getInventory().getItemInMainHand();
        if (event.getAction().equals(Action.RIGHT_CLICK_AIR) || event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
            if (itemInHand.isSimilar(journal)) {
                player.performCommand("notquests activeQuests");
            }
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent e) {
        final Player player = e.getPlayer();
        if (isItemSlotWorld(player.getWorld().getName()) && e.getRespawnLocation().getWorld() != null && isItemSlotWorld(e.getRespawnLocation().getWorld().getName())) {
            final ItemStack journalItem = player.getInventory().getItem(8);
            if (journalItem == null || !journalItem.isSimilar(journal)) {
                player.getInventory().remove(journal);
                player.getInventory().setItem(8, journal);
            }
        }
    }

    @EventHandler
    public void onDeath(EntityDeathEvent e) {
        if (e.getEntity() instanceof Player player) {
            if (isItemSlotWorld(player.getWorld().getName())) {
                e.getDrops().remove(journal);
            }
        }


    }

    @EventHandler
    public void onPickupItem(EntityPickupItemEvent e) {
        if (e.getEntity() instanceof Player player) {

            if (isItemSlotWorld(player.getWorld().getName())) {
                if (e.getItem().getItemStack().isSimilar(journal)) {
                    e.setCancelled(true);
                }
            }
        }

    }

    @EventHandler
    public void onDropItem(PlayerDropItemEvent e) {
        final Player player = e.getPlayer();

        if (isItemSlotWorld(player.getWorld().getName())) {

            if (e.getItemDrop().getItemStack().isSimilar(journal)) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onInventoryUse(InventoryClickEvent e) {
        if (e.getWhoClicked() instanceof Player player && e.getClickedInventory() != null) {
            if (isItemSlotWorld(player.getWorld().getName())) {
                ItemStack item;

                if (e.getClick() == ClickType.NUMBER_KEY) {
                    item = e.getCurrentItem();
                } else {
                    item = e.getCurrentItem();
                }
                if (item != null && item.isSimilar(journal)) {
                    e.setCancelled(true);

                    if (player.getGameMode().equals(GameMode.CREATIVE)) {
                        player.closeInventory();
                        player.updateInventory();
                    }
                }

                /*if(e.getCursor() != null && e.getCursor().isSimilar(journal)){
                    player.sendMessage("b");
                    player.setItemOnCursor(null);
                    e.setCurrentItem(null);
                    player.getInventory().setItem(8, journal);
                    e.setCancelled(true);
                }
                if(e.getCurrentItem() != null && e.getCurrentItem().isSimilar(journal)){
                    player.sendMessage("a");
                    e.setCurrentItem(new ItemStack(Material.AIR));
                    player.setItemOnCursor(new ItemStack(Material.AIR));
                    player.getInventory().setItem(8, journal);
                    //e.setCancelled(true);
                }*/

                final ItemStack journalItem = player.getInventory().getItem(8);
                if (journalItem == null || !journalItem.isSimilar(journal)) {
                    player.getInventory().remove(journal);
                    player.getInventory().setItem(8, journal);
                }
            }
        }

    }


    @EventHandler
    public void playerChangeWorldEvent(PlayerChangedWorldEvent e) {
        final Player player = e.getPlayer();

        if (isItemSlotWorld(player.getWorld().getName())) {
            player.getInventory().remove(journal);
            player.getInventory().setItem(8, journal);
        }

    }
}
