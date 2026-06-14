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
import rocks.gravili.notquests.paper.conversation.Speaker;

import java.util.ArrayList;
import java.util.List;

/**
 * Native-framework port of {@code SpeakerParser}: resolves a {@link Speaker} (by name) within the
 * {@link Conversation} carried by a prior positional argument named {@code conversationContext}.
 *
 * <p>NOTE: the Cloud parser read the conversation from the command context inside {@code parse}.
 * Brigadier does not hand {@code convert} a context, so resolution is delegated to
 * {@link #convert(CommandContext, String)}.
 */
public final class SpeakerArgument extends NQArgumentType<String> {
    private final NotQuests main;
    private final String conversationContext;

    public SpeakerArgument(final NotQuests main, final String conversationContext) {
        this.main = main;
        this.conversationContext = conversationContext;
    }

    public static SpeakerArgument speakerArgument(final NotQuests main, final String conversationContext) {
        return new SpeakerArgument(main, conversationContext);
    }

    @Override
    public String convert(final String input) {
        return input; // raw speaker name; resolved by the handler via resolveSpeaker(conversation, name)
    }

    /** Finds a speaker by name within a conversation (the conversation comes from the handler's context). */
    public static Speaker resolveSpeaker(final Conversation conversation, final String name) {
        if (conversation == null || conversation.getSpeakers() == null) {
            return null;
        }
        for (final Speaker speaker : conversation.getSpeakers()) {
            if (speaker.getSpeakerName().equalsIgnoreCase(name)) {
                return speaker;
            }
        }
        return null;
    }

    @Override
    protected List<String> suggest(final CommandContext<?> context, final String remaining) {
        final List<String> entries = new ArrayList<>();
        final Conversation conversation = (Conversation) context.getArgument(conversationContext, Object.class);
        if (conversation.getSpeakers() != null && conversation.getSpeakers().size() > 0) {
            final int speakerCount = conversation.getSpeakers().size();
            for (int i = 0; i < speakerCount; i++) {
                entries.add(conversation.getSpeakers().get(i).getSpeakerName());
            }
        }
        return entries;
    }
}
