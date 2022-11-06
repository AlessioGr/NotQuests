/*
 * NotQuests - A Questing plugin for Minecraft Servers
 * Copyright (C) 2021-2022 Alessio Gravili
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

package rocks.gravili.notquests.paper.conversation;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.Nullable;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.managers.data.Category;
import rocks.gravili.notquests.paper.managers.npc.NQNPC;

public class Conversation {
  final ArrayList<Speaker> speakers;
  private final NotQuests main;
  private final YamlConfiguration config;
  private final File configFile;
  private final String identifier;
  private final ArrayList<ConversationLine> start;
  private final CopyOnWriteArrayList<NQNPC> npcs; // -1: no NPC
  private Category category;

  private int delayInMS;

  public Conversation(
      final NotQuests main,
      final File configFile,
      final YamlConfiguration config,
      final String identifier,
      @Nullable final ArrayList<NQNPC> npcsToAdd,
      final Category category) {
    this.main = main;
    this.configFile = configFile;
    this.config = config;
    this.identifier = identifier;
    npcs = new CopyOnWriteArrayList<>();
    if (npcsToAdd != null) {
      for (final NQNPC nqnpc : npcsToAdd) {
        addNPC(nqnpc);
      }
    }
    start = new ArrayList<>();
    speakers = new ArrayList<>();
    this.category = category;

    delayInMS = category.getConversationDelayInMS();
  }

  public final Category getCategory() {
    return category;
  }

  public void setCategory(final Category category) {
    this.category = category;
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
        main.getLogManager()
            .severe(
                "There was an error saving the configuration of Conversation <highlight>"
                    + identifier
                    + "</highlight>.");
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
        main.getLogManager()
            .severe(
                "There was an error saving the configuration of Conversation <highlight>"
                    + identifier
                    + "</highlight>.");
        return false;
      }
    } else {
      return true;
    }
  }

  public final YamlConfiguration getConfig() {
    return config;
  }

  public void bindToAllCitizensNPCs() {
    for (NQNPC nqnpc : npcs) {
      bindToNQNPC(nqnpc);
    }
  }

  public void bindToNQNPC(final NQNPC nqnpc) {
    if (nqnpc == null) {
      return;
    }
    nqnpc.bindToConversation(this);
  }

  public final String getIdentifier() {
    return identifier;
  }

  public final CopyOnWriteArrayList<NQNPC> getNPCs() {
    return npcs;
  }


  public void addStarterConversationLine(final ConversationLine conversationLine) {
    start.add(conversationLine);
  }

  public final ArrayList<ConversationLine> getStartingLines() {
    return start;
  }

  public void addNPC(final NQNPC nqnpc) {
    if(nqnpc == null){
      main.getLogManager().warn("Tried to add a null NQNPC to the conversation <highlight>%s</highlight>. This NPC has been skipped.", getIdentifier());
      return;
    }
    this.npcs.add(nqnpc);
    bindToNQNPC(nqnpc);

    if (configFile == null || config == null) {
      return;
    }
    nqnpc.saveToConfig(config, "npcs." + nqnpc.getIdentifyingString());

    try {
      config.save(configFile);
    } catch (IOException e) {
      e.printStackTrace();
      main.getLogManager()
          .severe(
              "There was an error saving the configuration of Conversation <highlight>"
                  + identifier
                  + "</highlight>.");
    }
  }

  public void switchCategory(final Category category) {

    getCategory().getConversationsConfigs().remove(config);
    setCategory(category);
    category.getConversationsConfigs().add(config);

    if (!configFile.renameTo(new File(category.getConversationsFolder(), configFile.getName()))) {
      main.getLogManager()
          .severe(
              "There was an error changing the category of conversation <highlight>"
                  + getIdentifier()
                  + "</highlight>. The conversation file could not be moved.");
    }
  }

  public void setDelayInMS(final int delayInMS) {
    this.delayInMS = delayInMS;
  }

  public final int getDelayInMS() {
    return delayInMS;
  }
}
