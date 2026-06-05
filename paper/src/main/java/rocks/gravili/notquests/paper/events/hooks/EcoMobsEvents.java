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

import com.willfp.ecomobs.mob.EcoMob;
import com.willfp.ecomobs.mob.impl.ConfigDrivenEcoMobKt;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.ActiveObjective;
import rocks.gravili.notquests.paper.structs.ActiveQuest;
import rocks.gravili.notquests.paper.structs.QuestPlayer;
import rocks.gravili.notquests.paper.structs.objectives.KillMobsObjective;

public class EcoMobsEvents implements Listener {

  private final NotQuests main;

  public EcoMobsEvents(NotQuests main) {
    this.main = main;
  }

  // We listen to the vanilla EntityDeathEvent (which always fires) and ask EcoMobs whether the dead
  // entity is one of its mobs, rather than relying on EcoMobs' own kill event (which does not fire
  // reliably for plain combat kills).
  @EventHandler
  public void onEcoMobDeath(final EntityDeathEvent event) {
    if (!(event.getEntity() instanceof final Mob mob)) {
      return;
    }
    final Player killer = mob.getKiller();
    if (killer == null) {
      return;
    }
    final EcoMob ecoMob = ConfigDrivenEcoMobKt.getEcoMob(mob);
    if (ecoMob == null) {
      return; // not an EcoMobs mob
    }

    final QuestPlayer questPlayer =
        main.getQuestPlayerManager().getActiveQuestPlayer(killer.getUniqueId());
    if (questPlayer == null || questPlayer.getActiveQuests().isEmpty()) {
      return;
    }

    final String killedMobID = ecoMob.getID();
    for (final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
      for (final ActiveObjective activeObjective : activeQuest.getActiveObjectives()) {
        if (activeObjective.isUnlocked()
            && activeObjective.getObjective() instanceof final KillMobsObjective killMobsObjective) {
          if (killMobsObjective.getMobToKill().equalsIgnoreCase("any")
              || killMobsObjective.getMobToKill().equalsIgnoreCase(killedMobID)) {
            activeObjective.addProgress(1);
          }
        }
      }
      activeQuest.removeCompletedObjectives(true);
    }
    questPlayer.removeCompletedQuests();
  }
}
