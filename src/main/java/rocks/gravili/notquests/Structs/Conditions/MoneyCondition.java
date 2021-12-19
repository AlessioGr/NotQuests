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
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.NotQuests;
import rocks.gravili.notquests.Structs.QuestPlayer;

public class MoneyCondition extends Condition {

    private boolean deductMoney = false;


    public MoneyCondition(final NotQuests main) {
        super(main);
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



    public static void handleCommands(NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> builder, ConditionFor conditionFor) {
        if (!main.isVaultEnabled()) {
            return;
        }

        manager.command(builder.literal("Money")
                .argument(IntegerArgument.<CommandSender>newBuilder("amount").withMin(1), ArgumentDescription.of("Amount of money needed"))
                .flag(
                        manager.flagBuilder("deductMoney")
                                .withDescription(ArgumentDescription.of("Makes it so the required money is deducted from the players balance if the Quest is accepted."))
                )
                .meta(CommandMeta.DESCRIPTION, "Adds a new Money Requirement to a quest")
                .handler((context) -> {
                    final int amount = context.get("amount");
                    final boolean deductMoney = context.flags().isPresent("deductMoney");

                    MoneyCondition moneyCondition = new MoneyCondition(main);
                    moneyCondition.setProgressNeeded(amount);
                    moneyCondition.setDeductMoney(deductMoney);

                    main.getConditionsManager().addCondition(moneyCondition, context);
                }));
    }

    @Override
    public String getConditionDescription() {
        String description = "<GRAY>-- Money needed: " + getMoneyRequirement();

        if (isDeductMoney()) {
            description += "\n<GRAY>--- <RED>Money WILL BE DEDUCTED!";
        } else {
            description += "\n<GRAY>--- Will money be deducted?: No";
        }
        return description;
    }

    @Override
    public void save(FileConfiguration configuration, String initialPath) {
        configuration.set(initialPath + ".specifics.deductMoney", isDeductMoney());

    }

    @Override
    public void load(FileConfiguration configuration, String initialPath) {
        this.deductMoney = configuration.getBoolean(initialPath + ".specifics.deductMoney");
    }

    private void removeMoney(final Player player, final String worldName, final long moneyToDeduct, final boolean notifyPlayer) {
        if (!main.isVaultEnabled() || main.getEconomy() == null) {
            main.getLogManager().warn("Warning: Could not deduct money, because Vault was not found. Please install Vault for money stuff to work.");
            return;
        }
        main.getEconomy().withdrawPlayer(player, worldName, moneyToDeduct);
        if (notifyPlayer) {
            Audience audience = main.adventure().player(player);
            audience.sendMessage(MiniMessage.miniMessage().parse(
                    "<AQUA>-" + moneyToDeduct + " <RED>$!"
            ));
        }
    }

    @Override
    public String check(QuestPlayer questPlayer, boolean enforce) {

        final long moneyRequirementAmount = getMoneyRequirement();
        final boolean deductMoney = isDeductMoney();
        final Player player = questPlayer.getPlayer();
        if (player != null) {
            if (!main.isVaultEnabled() || main.getEconomy() == null) {
                return "<YELLOW>Error: The server does not have vault enabled. Please ask the Owner to install Vault for money stuff to work.";
            } else if (main.getEconomy().getBalance(player, player.getWorld().getName()) < moneyRequirementAmount) {
                return "<YELLOW>You need <AQUA>" + (moneyRequirementAmount - main.getEconomy().getBalance(player, player.getWorld().getName())) + "</AQUA> more money.";
            } else {
                if (enforce && deductMoney && moneyRequirementAmount > 0) {

                    if (main.isVaultEnabled()) {
                        removeMoney(player, player.getWorld().getName(), moneyRequirementAmount, true);
                    } else {
                        main.getLogManager().warn("Warning: Could not deduct money, because Vault was not found. Please install Vault for money stuff to work.");
                        main.getLogManager().warn("Error: Tried to load Economy when Vault is not enabled. Please report this to the plugin author (and I also recommend you installing Vault for money stuff to work)");
                        return "<RED>Error deducting money, because Vault has not been found. Report this to an Admin.";
                    }


                }
                return "";
            }
        } else {
            return "<YELLOW>Error reading money requirement...";

        }
    }
}
