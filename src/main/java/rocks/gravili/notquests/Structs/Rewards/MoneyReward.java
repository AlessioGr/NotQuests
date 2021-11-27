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


public class MoneyReward extends Reward {

    private final NotQuests main;
    private final long rewardedMoney;


    public MoneyReward(final NotQuests main, final Quest quest, final int rewardID) {
        super(main, quest, rewardID);
        this.main = main;

        this.rewardedMoney = main.getDataManager().getQuestsData().getLong("quests." + getQuest().getQuestName() + ".rewards." + rewardID + ".specifics.rewardedMoneyAmount");
    }

    public MoneyReward(final NotQuests main, final Quest quest, final int rewardID, long rewardedMoney) {
        super(main, quest, rewardID);
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

    @Override
    public String getRewardDescription() {
        return "Money: " + getRewardedMoney();
    }


    @Override
    public void save() {
        main.getDataManager().getQuestsData().set("quests." + getQuest().getQuestName() + ".rewards." + getRewardID() + ".specifics.rewardedMoneyAmount", getRewardedMoney());
    }

    public final long getRewardedMoney() {
        return rewardedMoney;
    }

    public static void handleCommands(NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> addRewardBuilder) {
        manager.command(addRewardBuilder.literal("Money")
                .argument(IntegerArgument.<CommandSender>newBuilder("amount").withMin(1), ArgumentDescription.of("Amount of money the player will receive."))
                .meta(CommandMeta.DESCRIPTION, "Adds a new Money Reward to a quest")
                .handler((context) -> {
                    final Audience audience = main.adventure().sender(context.getSender());
                    //Cancel if Vault is not found
                    if (!main.isVaultEnabled()) {
                        audience.sendMessage(MiniMessage.miniMessage().parse(
                                NotQuestColors.errorGradient + "Error: cannot add a money reward because Vault (needed for money stuff to work) is not installed on the server."
                        ));
                        return;
                    }
                    final Quest quest = context.get("quest");


                    final int moneyAmount = context.get("amount");


                    MoneyReward moneyReward = new MoneyReward(main, quest, quest.getRewards().size() + 1, moneyAmount);

                    quest.addReward(moneyReward);

                    audience.sendMessage(MiniMessage.miniMessage().parse(
                            NotQuestColors.successGradient + "Money Reward successfully added to Quest " + NotQuestColors.highlightGradient
                                    + quest.getQuestName() + "</gradient>!</gradient>"
                    ));

                }));
    }
}