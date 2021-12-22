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
import org.bukkit.configuration.file.YamlConfiguration;
import rocks.gravili.notquests.Managers.Integrations.Citizens.QuestGiverNPCTrait;
import rocks.gravili.notquests.NotQuests;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class Conversation {
    private final NotQuests main;
    private final YamlConfiguration config;
    private final File configFile;

    private final String identifier;
    private int npcID; //-1: no NPC
    private final ArrayList<ConversationLine> start;

    final ArrayList<Speaker> speakers;


    public Conversation(final NotQuests main, File configFile, YamlConfiguration config, final String identifier, final int npcID) {
        this.main = main;
        this.configFile = configFile;
        this.config = config;
        this.identifier = identifier;
        setNPC(npcID);
        start = new ArrayList<>();
        speakers = new ArrayList<>();
    }

    public final ArrayList<Speaker> getSpeakers() {
        return speakers;
    }

    public final boolean hasSpeaker(final Speaker speaker) {
        if (speakers.contains(speaker)) {
            return true;
        }
        for (final Speaker speakerToCheck : speakers) {
            if (speakerToCheck.getSpeakerName().equalsIgnoreCase(speaker.getSpeakerName())) {
                return true;
            }
        }
        return false;
    }

    public final boolean removeSpeaker(final Speaker speaker, final boolean save) {
        if (!hasSpeaker(speaker)) {
            return false;
        }

        speakers.remove(speaker);
        if (save) {
            if (configFile == null || config == null) {
                return false;
            }
            if (config.get(speaker.getSpeakerName()) != null) {
                return false;
            }
            config.set("Lines." + speaker.getSpeakerName(), null);
            try {
                config.save(configFile);
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                main.getLogManager().severe("There was an error saving the configuration of Conversation <AQUA>" + identifier + "</AQUA>.");
                return false;
            }
        } else {
            return true;
        }
    }


    public boolean addSpeaker(final Speaker speaker, final boolean save) {
        if (hasSpeaker(speaker)) {
            return false;
        }

        speakers.add(speaker);
        if (save) {
            if (configFile == null || config == null) {
                return false;
            }
            if (config.get(speaker.getSpeakerName()) != null) {
                return false;
            }
            config.set("Lines." + speaker.getSpeakerName() + ".color", speaker.getColor());
            try {
                config.save(configFile);
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                main.getLogManager().severe("There was an error saving the configuration of Conversation <AQUA>" + identifier + "</AQUA>.");
                return false;
            }
        } else {
            return true;
        }

    }

    public final YamlConfiguration getConfig() {
        return config;
    }

    public void bindToCitizensNPC() {
        if (npcID < 0) {
            return;
        }
        if (!main.getIntegrationsManager().isCitizensEnabled()) {
            main.getLogManager().warn("The binding to NPC in Conversation <AQUA>" + identifier + "</AQUA> has been cancelled, because the Citizens plugin is not installed on this server. You will need the Citizens plugin to do NPC stuff.");
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
                main.getLogManager().info("Trying to add Conversation <AQUA>" + identifier + "</AQUA> to NPC with ID <AQUA>" + npc.getId() + "</AQUA>...");

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


    public void setNPC(int npcID) {
        this.npcID = npcID;
        bindToCitizensNPC();

        if (configFile == null || config == null) {
            return;
        }
        config.set("npcID", npcID);
        try {
            config.save(configFile);
        } catch (IOException e) {
            e.printStackTrace();
            main.getLogManager().severe("There was an error saving the configuration of Conversation <AQUA>" + identifier + "</AQUA>.");
        }

    }


}
