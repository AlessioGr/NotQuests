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

package rocks.gravili.notquestsspigot.conditions.hooks.ultimateclans;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.paper.PaperCommandManager;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import rocks.gravili.notquestsspigot.NotQuests;
import rocks.gravili.notquestsspigot.conditions.Condition;
import rocks.gravili.notquestsspigot.conditions.ConditionFor;
import rocks.gravili.notquestsspigot.structs.QuestPlayer;

public class UltimateClansClanLevelCondition extends Condition {

    private int minClanLevel = 1;


    public UltimateClansClanLevelCondition(final NotQuests main) {
        super(main);
    }

    public void setMinClanLevel(final int minClanLevel){
        this.minClanLevel = minClanLevel;
    }

    public static void handleCommands(NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> builder, ConditionFor conditionFor) {
        if (!main.getIntegrationsManager().isUltimateClansEnabled()) {
            return;
        }

        manager.command(builder.literal("UltimateClansClanLevel")
                .argument(IntegerArgument.<CommandSender>newBuilder("minLevel").withMin(1), ArgumentDescription.of("Minimum clan level"))
                .meta(CommandMeta.DESCRIPTION, "Adds a new UltimateClansClanLevel Requirement to a quest")
                .handler((context) -> {
                    final int minLevel = context.get("minLevel");

                    UltimateClansClanLevelCondition ultimateClansClanLevelCondition = new UltimateClansClanLevelCondition(main);
                    ultimateClansClanLevelCondition.setMinClanLevel(minLevel);

                    main.getConditionsManager().addCondition(ultimateClansClanLevelCondition, context);
                }));
    }

    public final long getMinClanLevel() {
        return minClanLevel;
    }



    @Override
    public String getConditionDescription() {
        return "<GRAY>-- Member of clan with min. level: " + getMinClanLevel();
    }

    @Override
    public void save(FileConfiguration configuration, String initialPath) {
        configuration.set(initialPath + ".specifics.minClanLevel", getMinClanLevel());
    }

    @Override
    public void load(FileConfiguration configuration, String initialPath) {
        minClanLevel = configuration.getInt(initialPath + ".specifics.minClanLevel");
    }

    @Override
    public String check(QuestPlayer questPlayer, boolean enforce) {
        if (!main.getIntegrationsManager().isUltimateClansEnabled()) {
            return "<YELLOW>Error: The server does not have UltimateClans enabled. Please ask the Owner to install UltimateClans for UltimateClans stuff to work.";
        }

        final Player player = questPlayer.getPlayer();
        if (player != null) {
            if (main.getIntegrationsManager().getUltimateClansManager().isInClanWithMinLevel(player, getMinClanLevel())) {
                return "";
            }
            return "<YELLOW>You need to be in a Clan with at least level <AQUA>" + getMinClanLevel() + "</AQUA>.";
        } else {
            return "<YELLOW>Error reading UltimateClans requirement...";

        }
    }
}
