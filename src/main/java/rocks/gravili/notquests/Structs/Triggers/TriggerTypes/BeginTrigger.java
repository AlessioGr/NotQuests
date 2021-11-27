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

package rocks.gravili.notquests.Structs.Triggers.TriggerTypes;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.paper.PaperCommandManager;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import rocks.gravili.notquests.Commands.NotQuestColors;
import rocks.gravili.notquests.Commands.newCMDs.arguments.ApplyOnSelector;
import rocks.gravili.notquests.NotQuests;
import rocks.gravili.notquests.Structs.Quest;
import rocks.gravili.notquests.Structs.Rewards.MoneyReward;
import rocks.gravili.notquests.Structs.Triggers.Action;
import rocks.gravili.notquests.Structs.Triggers.Trigger;

public class BeginTrigger extends Trigger {

    private final NotQuests main;

    public BeginTrigger(final NotQuests main, final Quest quest, final int triggerID, Action action, int applyOn, String worldName, long amountNeeded) {
        super(main, quest, triggerID, action, applyOn, worldName, amountNeeded);
        this.main = main;
    }

    @Override
    public void save() {

    }

    @Override
    public String getTriggerDescription() {
        return null;
    }




    /*@Override
    public void isCompleted(){

    }*/

    public static void handleCommands(NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> addTriggerBuilder) {
        manager.command(addTriggerBuilder.literal("BEGIN")
                .argument(ApplyOnSelector.of("applyOn", main, "Quest"), ArgumentDescription.of("Where the Trigger will apply (Examples: 'Quest', 'O1', 'O2. (O1 = Objective 1)."))
                .argument(IntegerArgument.<CommandSender>newBuilder("amount").withMin(1), ArgumentDescription.of("Amount of money the player will receive."))
                .meta(CommandMeta.DESCRIPTION, "Triggers when a Quest begins or an Objective gets unlocked ('begins')")
                .handler((context) -> {
                    final Audience audience = main.adventure().sender(context.getSender());

                    final Quest quest = context.get("quest");
                    final Action action = context.get("action");
                    final String applyOn = context.get("applyOn");


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