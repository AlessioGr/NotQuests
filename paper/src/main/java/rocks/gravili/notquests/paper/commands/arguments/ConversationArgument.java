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

package rocks.gravili.notquests.paper.commands.arguments;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.framework.NQArgumentType;
import rocks.gravili.notquests.paper.conversation.Conversation;

import java.util.ArrayList;
import java.util.List;

/** Native-framework port of {@code ConversationParser}: resolves a {@link Conversation} by identifier. */
public final class ConversationArgument extends NQArgumentType<Conversation> {
    private final NotQuests main;

    public ConversationArgument(final NotQuests main) {
        this.main = main;
    }

    public static ConversationArgument conversationArgument(final NotQuests main) {
        return new ConversationArgument(main);
    }

    @Override
    public Conversation convert(final String input) throws CommandSyntaxException {
        for (final Conversation conversation : main.getConversationManager().getAllConversations()) {
            if (conversation.getIdentifier().equalsIgnoreCase(input)) {
                return conversation;
            }
        }
        throw fail("No Conversation found: " + input);
    }

    @Override
    protected List<String> suggest(final CommandContext<?> context, final String remaining) {
        final List<String> entries = new ArrayList<>();
        for (final Conversation conversation : main.getConversationManager().getAllConversations()) {
            entries.add(conversation.getIdentifier());
        }
        return entries;
    }
}
