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

package rocks.gravili.notquests.Structs.Conditions;

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
import rocks.gravili.notquests.Structs.Objectives.Objective;
import rocks.gravili.notquests.Structs.Quest;
import rocks.gravili.notquests.Structs.QuestPlayer;

public class MoneyCondition extends Condition {

    private final NotQuests main;
    private boolean deductMoney = false;


    public MoneyCondition(final NotQuests main, Object... objects) {
        super(main, objects);
        this.main = main;
    }


   public void setDeductMoney(final boolean deductMoney){
        this.deductMoney = deductMoney;
   }


    public final long getMoneyRequirement() {
        return getProgressNeeded();
    }


    public final boolean isDeductMoney() {
        return deductMoney;
    }



    public static void handleCommands(NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> addRequirementBuilder, Command.Builder<CommandSender> objectiveAddConditionBuilder) {
        if (!main.isVaultEnabled()) {
            return;
        }

        manager.command(addRequirementBuilder.literal("Money")
                .argument(IntegerArgument.<CommandSender>newBuilder("amount").withMin(1), ArgumentDescription.of("Amount of money needed"))
                .flag(
                        manager.flagBuilder("deductMoney")
                                .withDescription(ArgumentDescription.of("Makes it so the required money is deducted from the players balance if the Quest is accepted."))
                )
                .meta(CommandMeta.DESCRIPTION, "Adds a new Money Requirement to a quest")
                .handler((context) -> {
                    final Audience audience = main.adventure().sender(context.getSender());

                    final Quest quest = context.get("quest");

                    final int amount = context.get("amount");
                    final boolean deductMoney = context.flags().isPresent("deductMoney");

                    MoneyCondition moneyRequirement = new MoneyCondition(main, amount, deductMoney, quest);
                    moneyRequirement.setDeductMoney(deductMoney);
                    quest.addRequirement(moneyRequirement);

                    audience.sendMessage(MiniMessage.miniMessage().parse(
                            NotQuestColors.successGradient + "Money Requirement successfully added to Quest " + NotQuestColors.highlightGradient
                                    + quest.getQuestName() + "</gradient>!</gradient>"
                    ));
                }));

        manager.command(objectiveAddConditionBuilder.literal("Money")
                .argument(IntegerArgument.<CommandSender>newBuilder("amount").withMin(1), ArgumentDescription.of("Amount of money needed"))
                .flag(
                        manager.flagBuilder("deductMoney")
                                .withDescription(ArgumentDescription.of("Makes it so the required money is deducted from the players balance if the Quest is accepted."))
                )
                .meta(CommandMeta.DESCRIPTION, "Adds a new Money Requirement to a quest")
                .handler((context) -> {
                    final Audience audience = main.adventure().sender(context.getSender());

                    final Quest quest = context.get("quest");

                    final int amount = context.get("amount");
                    final boolean deductMoney = context.flags().isPresent("deductMoney");

                    final int objectiveID = context.get("Objective ID");
                    final Objective objective = quest.getObjectiveFromID(objectiveID);
                    assert objective != null; //Shouldn't be null

                    MoneyCondition moneyCondition = new MoneyCondition(main, amount, deductMoney, quest, objective);
                    moneyCondition.setDeductMoney(deductMoney);
                    objective.addCondition(moneyCondition, true);

                    audience.sendMessage(MiniMessage.miniMessage().parse(
                            NotQuestColors.successGradient + "Money Condition successfully added to Objective " + NotQuestColors.highlightGradient
                                    + objective.getObjectiveFinalName() + "</gradient>!</gradient>"));


                }));
    }

    @Override
    public String getConditionDescription() {
        String description = "§7-- Money needed: " + getMoneyRequirement() + "\n";

        if (isDeductMoney()) {
            description += "§7--- §cMoney WILL BE DEDUCTED!";
        } else {
            description += "§7--- Will money be deducted?: No";
        }
        return description;
    }

    @Override
    public void save(String initialPath) {
        main.getDataManager().getQuestsConfig().set(initialPath + ".specifics.deductMoney", isDeductMoney());

    }

    @Override
    public void load(String initialPath) {
        this.deductMoney = main.getDataManager().getQuestsConfig().getBoolean(initialPath + ".specifics.deductMoney");
    }

    private void removeMoney(final Player player, final String worldName, final long moneyToDeduct, final boolean notifyPlayer) {
        if (!main.isVaultEnabled() || main.getEconomy() == null) {
            main.getLogManager().warn("Warning: Could not deduct money, because Vault was not found. Please install Vault for money stuff to work.");
            return;
        }
        main.getEconomy().withdrawPlayer(player, worldName, moneyToDeduct);
        if (notifyPlayer) {
            player.sendMessage("§b-" + moneyToDeduct + " §c$!");

        }
    }

    @Override
    public String check(QuestPlayer questPlayer, boolean enforce) {

        final long moneyRequirementAmount = getMoneyRequirement();
        final boolean deductMoney = isDeductMoney();
        final Player player = questPlayer.getPlayer();
        if (player != null) {
            if (!main.isVaultEnabled() || main.getEconomy() == null) {
                return "\n§eError: The server does not have vault enabled. Please ask the Owner to install Vault for money stuff to work.";
            } else if (main.getEconomy().getBalance(player, player.getWorld().getName()) < moneyRequirementAmount) {
                return "\n§eYou need §b" + (moneyRequirementAmount - main.getEconomy().getBalance(player, player.getWorld().getName())) + " §emore money.";
            } else {
                if (enforce && deductMoney && moneyRequirementAmount > 0) {

                    if (main.isVaultEnabled()) {
                        removeMoney(player, player.getWorld().getName(), moneyRequirementAmount, true);
                    } else {
                        main.getLogManager().warn("Warning: Could not deduct money, because Vault was not found. Please install Vault for money stuff to work.");
                        main.getLogManager().warn("Error: Tried to load Economy when Vault is not enabled. Please report this to the plugin author (and I also recommend you installing Vault for money stuff to work)");
                        return "§cError deducting money, because Vault has not been found. Report this to an Admin.";
                    }


                }
                return "";
            }
        } else {
            return "\n§eError reading money requirement...";

        }
    }
}
