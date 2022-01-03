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

package rocks.gravili.notquests.paper.structs.actions;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.paper.PaperCommandManager;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

public class GiveQuestPointsAction extends Action {

    private long rewardedQuestPoints = 0;


    public GiveQuestPointsAction(final NotQuests main) {
        super(main);
    }

    public static void handleCommands(NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> builder, ActionFor rewardFor) {
        manager.command(builder.literal("GiveQuestPoints")
                .argument(IntegerArgument.<CommandSender>newBuilder("amount").withMin(1), ArgumentDescription.of("Amount of QuestPoints the player will receive."))
                .meta(CommandMeta.DESCRIPTION, "Adds a new GiveQuestPoints Reward to a quest")
                .handler((context) -> {
                    final int questPointsAmount = context.get("amount");

                    GiveQuestPointsAction giveQuestPointsAction = new GiveQuestPointsAction(main);
                    giveQuestPointsAction.setRewardedQuestPoints(questPointsAmount);

                    main.getActionManager().addAction(giveQuestPointsAction, context);
                }));
    }

    public void setRewardedQuestPoints(final long rewardedQuestPoints) {
        this.rewardedQuestPoints = rewardedQuestPoints;
    }

    @Override
    public void execute(final Player player, Object... objects) {
        if (rewardedQuestPoints == 0) {
            main.getLogManager().warn("Tried to give questpoints reward, but the amount of rewarded quest points is 0.");
            return;
        }
        QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId());
        if (questPlayer != null) {
            questPlayer.addQuestPoints(rewardedQuestPoints, true);

        } else {
            main.getLogManager().warn("Error giving quest point reward to player <highlight>" + player.getName() + "</highlight>");
            player.sendMessage(main.parse(
                    "<RED>Error giving quest point reward."
            ));
        }

    }

    @Override
    public String getActionDescription() {
        return "Quest points amount: " + getRewardedQuestPoints();
    }

    @Override
    public void save(final FileConfiguration configuration, String initialPath) {
        configuration.set(initialPath + ".specifics.questPoints", getRewardedQuestPoints());
    }


    public final long getRewardedQuestPoints() {
        return rewardedQuestPoints;
    }

    @Override
    public void load(final FileConfiguration configuration, String initialPath) {
        if(configuration.isLong(initialPath + ".specifics.questPoints")){
            this.rewardedQuestPoints = configuration.getLong(initialPath + ".specifics.questPoints");
        }else {
            this.rewardedQuestPoints = configuration.getLong(initialPath + ".specifics.rewardedQuestPoints");
        }
    }
}
