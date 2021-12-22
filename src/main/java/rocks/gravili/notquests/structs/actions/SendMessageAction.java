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

package rocks.gravili.notquests.structs.actions;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.paper.PaperCommandManager;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.NotQuests;

import java.util.ArrayList;
import java.util.List;

public class SendMessageAction extends Action {

    private String messageToSend = "";


    public SendMessageAction(final NotQuests main) {
        super(main);
    }

    public static void handleCommands(NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> builder, ActionFor actionFor) {
        manager.command(builder.literal("SendMessage")
                .argument(StringArgument.<CommandSender>newBuilder("Message").withSuggestionsProvider(
                        (context, lastString) -> {
                            final List<String> allArgs = context.getRawInput();
                            final Audience audience = main.adventure().sender(context.getSender());
                            main.getUtilManager().sendFancyCommandCompletion(audience, allArgs.toArray(new String[0]), "<Message to send>", "");

                            ArrayList<String> completions = new ArrayList<>();

                            completions.add("<Message to send>");
                            return completions;

                        }
                ).greedy().build(), ArgumentDescription.of("Message to send"))
                .meta(CommandMeta.DESCRIPTION, "Creates a new SendMessage Action")
                .handler((context) -> {
                    final String messageToSend = context.get("Message");

                    SendMessageAction sendMessageAction = new SendMessageAction(main);
                    sendMessageAction.setMessageToSend(messageToSend);

                    main.getActionManager().addAction(sendMessageAction, context);
                }));
    }

    public final String getMessageToSend() {
        return messageToSend;
    }

    public void setMessageToSend(final String messageToSend) {
        this.messageToSend = messageToSend;
    }


    @Override
    public void execute(final Player player, Object... objects) {
        Audience audience = main.adventure().player(player);

        audience.sendMessage(MiniMessage.miniMessage().parse(
                getMessageToSend()
        ));
    }

    @Override
    public void save(FileConfiguration configuration, String initialPath) {
        configuration.set(initialPath + ".specifics.message", getMessageToSend());
    }

    @Override
    public void load(final FileConfiguration configuration, String initialPath) {
        this.messageToSend = configuration.getString(initialPath + ".specifics.message", "");
    }


    @Override
    public String getActionDescription() {
        return "Sends Message: " + getMessageToSend();
    }
}
