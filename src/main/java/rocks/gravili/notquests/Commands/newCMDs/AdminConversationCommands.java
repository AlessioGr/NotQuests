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
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.paper.PaperCommandManager;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.Conversation.Conversation;
import rocks.gravili.notquests.Conversation.ConversationManager;
import rocks.gravili.notquests.NotQuests;

import static rocks.gravili.notquests.Commands.NotQuestColors.highlightGradient;
import static rocks.gravili.notquests.Commands.NotQuestColors.mainGradient;

public class AdminConversationCommands {
    protected final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final NotQuests main;
    private final PaperCommandManager<CommandSender> manager;
    private final Command.Builder<CommandSender> conversationBuilder;

    private final ConversationManager conversationManager;


    public AdminConversationCommands(final NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> conversationBuilder, final ConversationManager conversationManager) {
        this.main = main;
        this.manager = manager;
        this.conversationBuilder = conversationBuilder;

        this.conversationManager = conversationManager;


        manager.command(conversationBuilder.literal("test")
                .senderType(Player.class)
                .meta(CommandMeta.DESCRIPTION, "Starts a test conversation.")
                .handler((context) -> {
                    final Audience audience = main.adventure().sender(context.getSender());
                    final Player player = (Player) context.getSender();

                    audience.sendMessage(miniMessage.parse(
                            mainGradient + "Playing test conversation..."
                    ));
                    conversationManager.playConversation(player, conversationManager.createTestConversation());
                }));

        manager.command(conversationBuilder.literal("list")
                .senderType(Player.class)
                .meta(CommandMeta.DESCRIPTION, "Lists all conversations.")
                .handler((context) -> {
                    final Audience audience = main.adventure().sender(context.getSender());
                    final Player player = (Player) context.getSender();

                    audience.sendMessage(miniMessage.parse(
                            highlightGradient + "All conversations:"
                    ));
                    int counter = 1;
                    for (final Conversation conversation : conversationManager.getAllConversations())
                        audience.sendMessage(miniMessage.parse(
                                highlightGradient + counter + ". </gradient>" + mainGradient + conversation.getIdentifier()
                        ));

                }));

    }
}
