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

package rocks.gravili.notquests.Conversation;


import java.util.ArrayList;

public class Conversation {
    private final String identifier;
    private final int npcID;
    private final ArrayList<ConversationLine> start;

    public Conversation(final String identifier, final int npcID) {
        this.identifier = identifier;
        this.npcID = npcID;
        start = new ArrayList<>();
    }

    public final String getIdentifier() {
        return identifier;
    }

    public final int getNPCID() {
        return npcID;
    }

    public final boolean isCitizensNPC() {
        return npcID > -1;
    }

    public void addStarterConversationLine(final ConversationLine conversationLine) {
        start.add(conversationLine);
    }


    public final ArrayList<ConversationLine> getStartingLines() {
        return start;
    }


}
