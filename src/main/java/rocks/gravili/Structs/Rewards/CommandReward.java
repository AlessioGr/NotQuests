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

package rocks.gravili.Structs.Rewards;

import rocks.gravili.NotQuests;
import rocks.gravili.Structs.Quest;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

public class CommandReward extends Reward {

    private final NotQuests main;
    private final String consoleCommand;

    public CommandReward(final NotQuests main, String consoleCommand) {
        super(RewardType.ConsoleCommand);
        this.main = main;
        this.consoleCommand = consoleCommand;

    }

    @Override
    public void giveReward(final Player player, final Quest quest) {
        String rewardConsoleCommand = consoleCommand.replace("{PLAYER}", player.getName()).replace("{PLAYERUUID}", player.getUniqueId().toString());
        rewardConsoleCommand = rewardConsoleCommand.replace("{PLAYERX}", "" + player.getLocation().getX());
        rewardConsoleCommand = rewardConsoleCommand.replace("{PLAYERY}", "" + player.getLocation().getY());
        rewardConsoleCommand = rewardConsoleCommand.replace("{PLAYERZ}", "" + player.getLocation().getZ());
        rewardConsoleCommand = rewardConsoleCommand.replace("{WORLD}", "" + player.getWorld().getName());
        rewardConsoleCommand = rewardConsoleCommand.replace("{QUEST}", "" + quest.getQuestName());
        ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();

        //DEBUG: main.getLogManager().info("Giving reward command: §b" + consoleCommand);

        if (Bukkit.isPrimaryThread()) {
            Bukkit.dispatchCommand(console, rewardConsoleCommand);
        } else {
            final String finalRewardConsoleCommand = rewardConsoleCommand;
            Bukkit.getScheduler().runTask(main, () -> Bukkit.dispatchCommand(console, finalRewardConsoleCommand));
        }


    }

    public final String getConsoleCommand() {
        return consoleCommand;
    }
}
