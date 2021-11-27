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

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.paper.PaperCommandManager;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.Commands.NotQuestColors;
import rocks.gravili.notquests.NotQuests;
import rocks.gravili.notquests.Structs.Quest;
import rocks.gravili.notquests.Structs.QuestPlayer;

import java.util.logging.Level;

public class QuestPointsReward extends Reward {

    private final NotQuests main;
    private final long rewardedQuestPoints;


    public QuestPointsReward(final NotQuests main, final Quest quest, final int rewardID) {
        super(main, quest, rewardID);
        this.main = main;

        this.rewardedQuestPoints = main.getDataManager().getQuestsConfig().getLong("quests." + getQuest().getQuestName() + ".rewards." + rewardID + ".specifics.rewardedQuestPoints");
    }

    public QuestPointsReward(final NotQuests main, final Quest quest, final int rewardID, long rewardedQuestPoints) {
        super(main, quest, rewardID);
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

    @Override
    public String getRewardDescription() {
        return "Quest points amount: " + getRewardedQuestPoints();
    }


    @Override
    public void save() {
        main.getDataManager().getQuestsConfig().set("quests." + getQuest().getQuestName() + ".rewards." + getRewardID() + ".specifics.rewardedQuestPoints", getRewardedQuestPoints());
    }

    public final long getRewardedQuestPoints() {
        return rewardedQuestPoints;
    }

    public static void handleCommands(NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> addRewardBuilder) {
        manager.command(addRewardBuilder.literal("QuestPoints")
                .argument(IntegerArgument.<CommandSender>newBuilder("amount").withMin(1), ArgumentDescription.of("Amount of QuestPoints the player will receive."))
                .meta(CommandMeta.DESCRIPTION, "Adds a new QuestPoints Reward to a quest")
                .handler((context) -> {
                    final Audience audience = main.adventure().sender(context.getSender());

                    final Quest quest = context.get("quest");


                    final int questPointsAmount = context.get("amount");


                    QuestPointsReward questPointsReward = new QuestPointsReward(main, quest, quest.getRewards().size() + 1, questPointsAmount);

                    quest.addReward(questPointsReward);

                    audience.sendMessage(MiniMessage.miniMessage().parse(
                            NotQuestColors.successGradient + "QuestPoints Reward successfully added to Quest " + NotQuestColors.highlightGradient
                                    + quest.getQuestName() + "</gradient>!</gradient>"
                    ));

                }));
    }
}
