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

package rocks.gravili.notquests.paper.events.hooks;

import com.palmergames.bukkit.towny.event.NationAddTownEvent;
import com.palmergames.bukkit.towny.event.TownAddResidentEvent;
import com.palmergames.bukkit.towny.event.TownRemoveResidentEvent;
import com.palmergames.bukkit.towny.object.Resident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.ActiveObjective;
import rocks.gravili.notquests.paper.structs.ActiveQuest;
import rocks.gravili.notquests.paper.structs.QuestPlayer;
import rocks.gravili.notquests.paper.structs.objectives.hooks.towny.TownyNationReachTownCountObjective;
import rocks.gravili.notquests.paper.structs.objectives.hooks.towny.TownyReachResidentCountObjective;

public class TownyEvents implements Listener {
  private final NotQuests main;

  public TownyEvents(NotQuests main) {
    this.main = main;
  }

  @EventHandler
  public void onTownAddToNation(NationAddTownEvent e) {
    for (final Resident resident : e.getNation().getResidents()) {
      final QuestPlayer questPlayer =
          main.getQuestPlayerManager().getActiveQuestPlayer(resident.getUUID());
      if (questPlayer != null) {
        if (questPlayer.getActiveQuests().size() > 0) {
          for (final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
            for (final ActiveObjective activeObjective : activeQuest.getActiveObjectives()) {
              if (activeObjective.isUnlocked()) {
                if (activeObjective.getObjective() instanceof TownyNationReachTownCountObjective) {
                  activeObjective.addProgress(1);
                }
              }
            }
            activeQuest.removeCompletedObjectives(true);
          }
          questPlayer.removeCompletedQuests();
        }
      }
    }
  }

  @EventHandler
  public void onTownRemoveFromNation(NationAddTownEvent e) {
    for (final Resident resident : e.getNation().getResidents()) {
      final QuestPlayer questPlayer =
          main.getQuestPlayerManager().getActiveQuestPlayer(resident.getUUID());
      if (questPlayer != null) {
        if (questPlayer.getActiveQuests().size() > 0) {
          for (final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
            for (final ActiveObjective activeObjective : activeQuest.getActiveObjectives()) {
              if (activeObjective.isUnlocked()) {
                if (activeObjective.getObjective() instanceof TownyNationReachTownCountObjective) {
                  activeObjective.removeProgress(1, true);
                }
              }
            }
            activeQuest.removeCompletedObjectives(true);
          }
          questPlayer.removeCompletedQuests();
        }
      }
    }
  }

  @EventHandler
  public void onResidentAdd(TownAddResidentEvent e) {
    for (final Resident resident : e.getTown().getResidents()) {
      final QuestPlayer questPlayer =
          main.getQuestPlayerManager().getActiveQuestPlayer(resident.getUUID());
      if (questPlayer != null) {
        if (questPlayer.getActiveQuests().size() > 0) {
          for (final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
            for (final ActiveObjective activeObjective : activeQuest.getActiveObjectives()) {
              if (activeObjective.isUnlocked()) {
                if (activeObjective.getObjective() instanceof TownyReachResidentCountObjective) {
                  activeObjective.addProgress(1);
                }
              }
            }
            activeQuest.removeCompletedObjectives(true);
          }
          questPlayer.removeCompletedQuests();
        }
      }
    }
  }

  @EventHandler
  public void onResidentRemove(TownRemoveResidentEvent e) {
    for (final Resident resident : e.getTown().getResidents()) {
      final QuestPlayer questPlayer =
          main.getQuestPlayerManager().getActiveQuestPlayer(resident.getUUID());
      if (questPlayer != null) {
        if (!questPlayer.getActiveQuests().isEmpty()) {
          for (final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
            for (final ActiveObjective activeObjective : activeQuest.getActiveObjectives()) {
              if (activeObjective.isUnlocked()) {
                if (activeObjective.getObjective() instanceof TownyReachResidentCountObjective) {
                  activeObjective.removeProgress(1, true);
                }
              }
            }
            activeQuest.removeCompletedObjectives(true);
          }
          questPlayer.removeCompletedQuests();
        }
      }
    }
  }
}
