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
import cloud.commandframework.arguments.standard.StringArrayArgument;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.paper.PaperCommandManager;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.Commands.NotQuestColors;
import rocks.gravili.notquests.NotQuests;
import rocks.gravili.notquests.Structs.Quest;

import java.util.ArrayList;
import java.util.List;

public class CommandReward extends Reward {

    private final NotQuests main;
    private final String consoleCommand;


    public CommandReward(final NotQuests main, final Quest quest, final int rewardID) {
        super(main, quest, rewardID);
        this.main = main;

        this.consoleCommand = main.getDataManager().getQuestsData().getString("quests." + getQuest().getQuestName() + ".rewards." + rewardID + ".specifics.consoleCommand");
    }

    public CommandReward(final NotQuests main, final Quest quest, final int rewardID, String consoleCommand) {
        super(main, quest, rewardID);
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

    @Override
    public String getRewardDescription() {
        return "Reward Command: " + getConsoleCommand();
    }


    @Override
    public void save() {
        main.getDataManager().getQuestsData().set("quests." + getQuest().getQuestName() + ".rewards." + getRewardID() + ".specifics.consoleCommand", getConsoleCommand());
    }

    public final String getConsoleCommand() {
        return consoleCommand;
    }

    public static void handleCommands(NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> addRewardBuilder) {
        manager.command(addRewardBuilder.literal("ConsoleCommand")
                .argument(StringArrayArgument.of("ConsoleCommand",
                        (context, lastString) -> {
                            final List<String> allArgs = context.getRawInput();
                            final Audience audience = main.adventure().sender(context.getSender());
                            main.getUtilManager().sendFancyCommandCompletion(audience, allArgs.toArray(new String[0]), "<Enter Console Command>", "");
                            ArrayList<String> completions = new ArrayList<>();
                            completions.add("<Enter Console Command>");
                            return completions;
                        }
                ), ArgumentDescription.of("Command which will be executed from the console as a reward. A '/' at the beginning is not required."))
                .meta(CommandMeta.DESCRIPTION, "Adds a new ConsoleCommand Requirement to a quest")
                .handler((context) -> {
                    final Audience audience = main.adventure().sender(context.getSender());

                    final Quest quest = context.get("quest");

                    final String consoleCommand = String.join(" ", (String[]) context.get("ConsoleCommand"));

                    CommandReward commandReward = new CommandReward(main, quest, quest.getRewards().size() + 1, consoleCommand);
                    quest.addReward(commandReward);

                    audience.sendMessage(MiniMessage.miniMessage().parse(
                            NotQuestColors.successGradient + "ConsoleCommand Reward successfully added to Quest " + NotQuestColors.highlightGradient
                                    + quest.getQuestName() + "</gradient>!</gradient>"
                    ));

                }));
    }
}
