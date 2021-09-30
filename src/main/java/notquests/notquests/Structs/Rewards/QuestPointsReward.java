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

package notquests.notquests.Structs.Rewards;

import notquests.notquests.NotQuests;
import notquests.notquests.Structs.Quest;
import notquests.notquests.Structs.QuestPlayer;
import org.bukkit.entity.Player;

import java.util.logging.Level;

public class QuestPointsReward extends Reward {

    private final NotQuests main;
    private final long rewardedQuestPoints;

    public QuestPointsReward(final NotQuests main, long rewardedQuestPoints) {
        super(RewardType.QuestPoints);
        this.main = main;
        this.rewardedQuestPoints = rewardedQuestPoints;

    }

    @Override
    public void giveReward(final Player player, final Quest quest) {
        QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId());
        if (questPlayer != null) {
            questPlayer.addQuestPoints(rewardedQuestPoints, true);

        } else {
            main.getLogManager().log(Level.WARNING, "§cError giving quest point reward to player §b" + player.getName());

            player.sendMessage("§cError giving quest point reward.");
        }

    }

    public final long getRewardedQuestPoints() {
        return rewardedQuestPoints;
    }
}
