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

package rocks.gravili.notquests.Structs.Requirements;

import cloud.commandframework.Command;
import cloud.commandframework.paper.PaperCommandManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.NotQuests;
import rocks.gravili.notquests.Structs.Quest;
import rocks.gravili.notquests.Structs.QuestPlayer;

import java.util.logging.Level;

public class MoneyRequirement extends Requirement {

    private final NotQuests main;
    private final long moneyRequirement;
    private final boolean deductMoney;


    public MoneyRequirement(final NotQuests main, final Quest quest, final int requirementID, final long moneyRequirement) {
        super(main, quest, requirementID, moneyRequirement);
        this.main = main;
        this.moneyRequirement = moneyRequirement;

        this.deductMoney = main.getDataManager().getQuestsData().getBoolean("quests." + quest.getQuestName() + ".requirements." + requirementID + ".specifics.deductMoney");
    }


    public MoneyRequirement(final NotQuests main, final Quest quest, final int requirementID, final long moneyRequirement, final boolean deductMoney) {
        super(main, quest, requirementID, moneyRequirement);
        this.main = main;
        this.moneyRequirement = moneyRequirement;
        this.deductMoney = deductMoney;
    }


    public final long getMoneyRequirement() {
        return moneyRequirement;
    }


    public final boolean isDeductMoney() {
        return deductMoney;
    }

    public static void handleCommands(NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> builder) {

    }

    @Override
    public void save() {
        main.getDataManager().getQuestsData().set("quests." + getQuest().getQuestName() + ".requirements." + getRequirementID() + ".specifics.moneyRequirement", getMoneyRequirement());
        main.getDataManager().getQuestsData().set("quests." + getQuest().getQuestName() + ".requirements." + getRequirementID() + ".specifics.deductMoney", isDeductMoney());

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
                        main.getLogManager().log(Level.WARNING, "§eWarning: Could not deduct money, because Vault was not found. Please install Vault for money stuff to work.");
                        main.getLogManager().log(Level.WARNING, "§cError: Tried to load Economy when Vault is not enabled. Please report this to the plugin author (and I also recommend you installing Vault for money stuff to work)");
                        return "§cError deducting money, because Vault has not been found. Report this to an Admin.";
                    }


                }
                return "";
            }
        } else {
            return "\n§eError reading money requirement...";

        }
    }

    @Override
    public String getRequirementDescription() {
        String description = "§7-- Money needed: " + getMoneyRequirement() + "\n";

        if (isDeductMoney()) {
            description += "§7--- §cMoney WILL BE DEDUCTED!";
        } else {
            description += "§7--- Will money be deducted?: No";
        }
        return description;
    }

    private void removeMoney(final Player player, final String worldName, final long moneyToDeduct, final boolean notifyPlayer) {
        if (!main.isVaultEnabled() || main.getEconomy() == null) {
            main.getLogManager().log(Level.WARNING, "§eWarning: Could not deduct money, because Vault was not found. Please install Vault for money stuff to work.");
            return;
        }
        main.getEconomy().withdrawPlayer(player, worldName, moneyToDeduct);
        if (notifyPlayer) {
            player.sendMessage("§b-" + moneyToDeduct + " §c$!");

        }
    }
}
