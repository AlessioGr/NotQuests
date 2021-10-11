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

package rocks.gravili.Structs.Rewards;

import rocks.gravili.NotQuests;
import rocks.gravili.Structs.Quest;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ItemReward extends Reward {

    private final NotQuests main;
    private final ItemStack item;

    public ItemReward(final NotQuests main, ItemStack item, final int rewardID) {
        super(RewardType.Item, rewardID);
        this.main = main;
        this.item = item;

    }

    @Override
    public void giveReward(final Player player, final Quest quest) {

        if (Bukkit.isPrimaryThread()) {
            player.getInventory().addItem(item);
        } else {
            Bukkit.getScheduler().runTask(main, () -> player.getInventory().addItem(item)); //TODO: Check if I can't just run it async if it already is async`?
        }


    }

    public final ItemStack getItemReward() {
        return item;
    }
}