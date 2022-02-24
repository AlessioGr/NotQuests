/*
 * NotQuests - A Questing plugin for Minecraft Servers
 * Copyright (C) 2022 Alessio Gravili
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

package rocks.gravili.notquests.spigot.structs.conditions.hooks.towny;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.paper.PaperCommandManager;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.spigot.NotQuests;
import rocks.gravili.notquests.spigot.structs.QuestPlayer;
import rocks.gravili.notquests.spigot.structs.conditions.Condition;
import rocks.gravili.notquests.spigot.structs.conditions.ConditionFor;

public class TownyTownResidentCountCondition extends Condition {

    private int minTownResidentCount = 1;

    public TownyTownResidentCountCondition(final NotQuests main) {
        super(main);
    }

    public static void handleCommands(NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> builder, ConditionFor conditionFor) {
        if (!main.getIntegrationsManager().isTownyEnabled()) {
            return;
        }

        manager.command(builder.literal("TownyTownResidentCount")
                .argument(IntegerArgument.<CommandSender>newBuilder("min Resident Count").withMin(1), ArgumentDescription.of("Minimum Town resident count"))
                .meta(CommandMeta.DESCRIPTION, "Creates a new TownyTownResidentCount Condition")
                .handler((context) -> {
                    final int minResidentCount = context.get("min Resident Count");

                    TownyTownResidentCountCondition townyTownResidentCountCondition = new TownyTownResidentCountCondition(main);
                    townyTownResidentCountCondition.setMinTownResidentCount(minResidentCount);


                    main.getConditionsManager().addCondition(townyTownResidentCountCondition, context);
                }));
    }

    public final int getMinTownResidentCount() {
        return minTownResidentCount;
    }

    public void setMinTownResidentCount(final int minTownResidentCount) {
        this.minTownResidentCount = minTownResidentCount;
    }

    @Override
    public String check(QuestPlayer questPlayer, boolean enforce) {
        if (!main.getIntegrationsManager().isTownyEnabled()) {
            return "<YELLOW>Error: The server does not have Towny enabled. Please ask the Owner to install Towny for Towny stuff to work.";
        }

        final Player player = questPlayer.getPlayer();
        if (player != null) {
            Resident resident = TownyUniverse.getInstance().getResident(questPlayer.getUUID());
            if (resident != null && resident.getTownOrNull() != null && resident.hasTown()) {
                Town town = resident.getTownOrNull();
                if (town.getNumResidents() >= getMinTownResidentCount()) {
                    return "";
                } else {
                    return "<YELLOW>Your town needs to have at least <AQUA>" + getMinTownResidentCount() + "</AQUA> residents.";
                }
            } else {
                return "<YELLOW>You need to be in a town";
            }


        } else {
            return "<YELLOW>Error reading TownyTownResidentCount requirement...";

        }
    }

    @Override
    public String getConditionDescription() {
        return "<GRAY>-- Minimum town residents: " + getMinTownResidentCount();
    }


    @Override
    public void save(FileConfiguration configuration, String initialPath) {
        configuration.set(initialPath + ".specifics.minTownResidentCount", getMinTownResidentCount());

    }

    @Override
    public void load(FileConfiguration configuration, String initialPath) {
        this.minTownResidentCount = configuration.getInt(initialPath + ".specifics.minTownResidentCount");

    }
}