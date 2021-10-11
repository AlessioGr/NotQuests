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
import org.bukkit.entity.Player;


public class MoneyReward extends Reward {

    private final NotQuests main;
    private final long rewardedMoney;

    public MoneyReward(final NotQuests main, long rewardedMoney, final int rewardID) {
        super(RewardType.Money, rewardID);
        this.main = main;
        this.rewardedMoney = rewardedMoney;

    }

    @Override
    public void giveReward(final Player player, final Quest quest) {
        if (!main.isVaultEnabled() || main.getEconomy() == null) {
            player.sendMessage("§cError: cannot give you the money reward because Vault (needed for money stuff to work) is not installed on the server.");
            return;
        }
        if (rewardedMoney > 0) {
            main.getEconomy().depositPlayer(player, rewardedMoney);
        } else if (rewardedMoney < 0) {
            main.getEconomy().withdrawPlayer(player, Math.abs(rewardedMoney));
        }
    }

    public final long getRewardedMoney() {
        return rewardedMoney;
    }
}