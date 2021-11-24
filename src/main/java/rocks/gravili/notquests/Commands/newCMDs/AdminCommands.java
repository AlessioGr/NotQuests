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

package rocks.gravili.notquests.Commands.newCMDs;


import cloud.commandframework.Command;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.paper.PaperCommandManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.NotQuests;

import java.util.ArrayList;

public class AdminCommands {
    private final NotQuests main;

    public AdminCommands(final NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> builder) {
        this.main = main;
        ArrayList<String> ee = new ArrayList<>();
        ee.add("e");
       /* manager.command(builder.literal("create")
                .argument( StringArgument.of("Quest Name") )
                .meta(CommandMeta.DESCRIPTION, "Create a new quest.").senderType(Player.class).handler((commandContext) -> {
                    commandContext.getSender().sendMessage(main.getQuestManager().createQuest( commandContext.get("Quest Name") ));
        }));*/


        manager.command(builder.literal("create")
                .argument(StringArgument.of("Quest Name"))

                .meta(CommandMeta.DESCRIPTION, "Create a new quest.").senderType(Player.class).handler((commandContext) -> {
                    commandContext.getSender().sendMessage(main.getQuestManager().createQuest(commandContext.get("Quest Name")));
                }));


        manager.command(builder.literal("delete")
                .argument(StringArgument.of("Quest Name"))
                .meta(CommandMeta.DESCRIPTION, "Delete an existing Quest.").handler((commandContext) -> {
                    commandContext.getSender().sendMessage(main.getQuestManager().deleteQuest(commandContext.get("Quest Name")));
                }));


    }
}
