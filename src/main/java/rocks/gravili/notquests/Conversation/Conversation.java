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


import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.Trait;
import rocks.gravili.notquests.Hooks.Citizens.QuestGiverNPCTrait;
import rocks.gravili.notquests.NotQuests;

import java.util.ArrayList;
import java.util.logging.Level;

public class Conversation {
    private final NotQuests main;

    private final String identifier;
    private final int npcID; //-1: no NPC
    private final ArrayList<ConversationLine> start;


    public Conversation(final NotQuests main, final String identifier, final int npcID) {
        this.main = main;
        this.identifier = identifier;
        this.npcID = npcID;
        start = new ArrayList<>();


    }

    public void bindToCitizensNPC() {
        if (npcID < 0) {
            return;
        }
        if (!main.isCitizensEnabled()) {
            main.getLogManager().log(Level.WARNING, "The binding to NPC in Conversation " + identifier + " has been cancelled, because the Citizens plugin is not installed on this server. You will need the Citizens plugin to do NPC stuff.");
            return;
        }


        final NPC npc = CitizensAPI.getNPCRegistry().getById(npcID);
        if (npc != null) {
            boolean hasTrait = false;
            for (Trait trait : npc.getTraits()) {
                if (trait.getName().contains("questgiver")) {
                    hasTrait = true;
                    break;
                }
            }
            if (!npc.hasTrait(QuestGiverNPCTrait.class) && !hasTrait) {
                main.getLogManager().log(Level.INFO, "Trying to add Conversation " + identifier + " to NPC with ID " + npc.getId() + "...");

                npc.addTrait(QuestGiverNPCTrait.class);
            }
        }


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
