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

package rocks.gravili.notquests.Structs.Actions;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.arguments.standard.StringArrayArgument;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.paper.PaperCommandManager;
import net.kyori.adventure.audience.Audience;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.NotQuests;
import rocks.gravili.notquests.Structs.Quest;

import java.util.ArrayList;
import java.util.List;

public class ConsoleCommandAction extends Action {

    private String consoleCommand = "";


    public ConsoleCommandAction(final NotQuests main) {
        super(main);
    }

    public static void handleCommands(NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> builder, ActionFor rewardFor) {
        manager.command(builder.literal("ConsoleCommand")
                .argument(StringArrayArgument.of("ConsoleCommand",
                        (context, lastString) -> {
                            final List<String> allArgs = context.getRawInput();
                            final Audience audience = main.adventure().sender(context.getSender());
                            main.getUtilManager().sendFancyCommandCompletion(audience, allArgs.toArray(new String[0]), "<Enter Console Command>", "");
                            ArrayList<String> completions = new ArrayList<>();

                            if (lastString.startsWith("{")) {
                                completions.addAll(main.getCommandManager().getAdminCommands().placeholders);
                            } else {
                                completions.add("<Enter Console Command>");
                            }

                            return completions;

                        }
                ), ArgumentDescription.of("Command which will be executed from the console as a reward. A '/' at the beginning is not required."))
                .meta(CommandMeta.DESCRIPTION, "Adds a new ConsoleCommand Reward to a quest")
                .handler((context) -> {
                    final String consoleCommand = String.join(" ", (String[]) context.get("ConsoleCommand"));

                    ConsoleCommandAction consoleCommandAction = new ConsoleCommandAction(main);
                    consoleCommandAction.setConsoleCommand(consoleCommand);

                    main.getActionManager().addAction(consoleCommandAction, context);

                }));
    }

    public void setConsoleCommand(final String consoleCommand) {
        this.consoleCommand = consoleCommand;
    }

    @Override
    public void execute(final Player player, Object... objects) {
        if (consoleCommand.isBlank()) {
            main.getLogManager().warn("Tried to give ConsoleCommand action with invalid console command.");
            return;
        }
        Quest quest = getQuest();
        if (quest == null) {
            for (Object object : objects) {
                if (object instanceof Quest quest1) {
                    quest = quest1;
                }
            }
        }

        String rewardConsoleCommand = consoleCommand.replace("{PLAYER}", player.getName()).replace("{PLAYERUUID}", player.getUniqueId().toString());
        rewardConsoleCommand = rewardConsoleCommand.replace("{PLAYERX}", "" + player.getLocation().getX());
        rewardConsoleCommand = rewardConsoleCommand.replace("{PLAYERY}", "" + player.getLocation().getY());
        rewardConsoleCommand = rewardConsoleCommand.replace("{PLAYERZ}", "" + player.getLocation().getZ());
        rewardConsoleCommand = rewardConsoleCommand.replace("{WORLD}", "" + player.getWorld().getName());
        if (quest != null) {
            rewardConsoleCommand = rewardConsoleCommand.replace("{QUEST}", "" + quest.getQuestName());
        }
        ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();

        if (Bukkit.isPrimaryThread()) {
            Bukkit.dispatchCommand(console, rewardConsoleCommand);
        } else {
            final String finalRewardConsoleCommand = rewardConsoleCommand;
            Bukkit.getScheduler().runTask(main, () -> Bukkit.dispatchCommand(console, finalRewardConsoleCommand));
        }


    }

    @Override
    public void save(FileConfiguration configuration, String initialPath) {
        configuration.set(initialPath + ".specifics.consoleCommand", getConsoleCommand());
    }

    @Override
    public void load(final FileConfiguration configuration, String initialPath) {
        this.consoleCommand = configuration.getString(initialPath + ".specifics.consoleCommand");

    }


    public final String getConsoleCommand() {
        return consoleCommand;
    }

    @Override
    public String getActionDescription() {
        return "Reward Command: " + getConsoleCommand();
    }
}
