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

package rocks.gravili.notquests.Structs.Rewards;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.NotQuests;
import rocks.gravili.notquests.Structs.Quest;

public abstract class Reward {
    private final NotQuests main;
    private final Quest quest;
    private final int rewardID;
    private String rewardDisplayName = "";


    public Reward(NotQuests main, Quest quest, int rewardID) {
        this.main = main;
        this.quest = quest;
        this.rewardID = rewardID;
    }

    public final String getRewardType() {
        return main.getRewardManager().getRewardType(this.getClass());
    }


    public final int getRewardID() {
        return rewardID;
    }

    public final String getRewardDisplayName() {
        return ChatColor.translateAlternateColorCodes('&', rewardDisplayName);
    }

    public void setRewardDisplayName(final String newRewardDisplayName) {
        this.rewardDisplayName = newRewardDisplayName;

    }

    public void removeRewardDisplayName() {
        this.rewardDisplayName = "";
    }

    public final Quest getQuest() {
        return quest;
    }

    public abstract String getRewardDescription();

    public abstract void giveReward(final Player player, final Quest quest);

    public abstract void save();
}
