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

package rocks.gravili.notquests.Structs.Triggers;


import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.NotQuests;
import rocks.gravili.notquests.Structs.ActiveQuest;

public class Action {

    private final NotQuests main;
    private final String actionName;
    private String consoleCommand;


    public Action(NotQuests main, String name, String consoleCommand) {
        this.main = main;
        this.actionName = name;
        this.consoleCommand = consoleCommand;
    }

    public void execute(final Player player, final ActiveQuest activeQuest) {
        String executeConsoleCommand = consoleCommand.replace("{PLAYER}", player.getName()).replace("{PLAYERUUID}", player.getUniqueId().toString());
        executeConsoleCommand = executeConsoleCommand.replace("{PLAYERX}", "" + player.getLocation().getX());
        executeConsoleCommand = executeConsoleCommand.replace("{PLAYERY}", "" + player.getLocation().getY());
        executeConsoleCommand = executeConsoleCommand.replace("{PLAYERZ}", "" + player.getLocation().getZ());
        executeConsoleCommand = executeConsoleCommand.replace("{WORLD}", "" + player.getWorld().getName());
        executeConsoleCommand = executeConsoleCommand.replace("{QUEST}", "" + activeQuest.getQuest().getQuestName());
        ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
        if (Bukkit.isPrimaryThread()) {
            Bukkit.dispatchCommand(console, executeConsoleCommand);
        } else {
            final String finalExecuteConsoleCommand = executeConsoleCommand;
            Bukkit.getScheduler().runTask(main, () -> {
                Bukkit.dispatchCommand(console, finalExecuteConsoleCommand);
            });
        }
    }

    public void execute(final Player player) {
        String executeConsoleCommand = consoleCommand.replace("{PLAYER}", player.getName()).replace("{PLAYERUUID}", player.getUniqueId().toString());
        executeConsoleCommand = executeConsoleCommand.replace("{PLAYERX}", "" + player.getLocation().getX());
        executeConsoleCommand = executeConsoleCommand.replace("{PLAYERY}", "" + player.getLocation().getY());
        executeConsoleCommand = executeConsoleCommand.replace("{PLAYERZ}", "" + player.getLocation().getZ());
        executeConsoleCommand = executeConsoleCommand.replace("{WORLD}", "" + player.getWorld().getName());
        ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
        if (Bukkit.isPrimaryThread()) {
            Bukkit.dispatchCommand(console, executeConsoleCommand);
        } else {
            final String finalExecuteConsoleCommand = executeConsoleCommand;
            Bukkit.getScheduler().runTask(main, () -> {
                Bukkit.dispatchCommand(console, finalExecuteConsoleCommand);
            });
        }
    }


    public final String getActionName() {
        return actionName;
    }

    public final String getConsoleCommand() {
        return consoleCommand;
    }

    public void setConsoleCommand(String newConsoleCommand) {
        this.consoleCommand = newConsoleCommand;
        main.getDataManager().getQuestsData().set("actions." + actionName + ".consoleCommand", newConsoleCommand);
    }
}
