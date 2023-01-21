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

package rocks.gravili.notquests.paper.managers.integrations.betonquest;


import org.betonquest.betonquest.BetonQuest;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.managers.integrations.betonquest.conditions.BQConditionsCondition;
import rocks.gravili.notquests.paper.managers.integrations.betonquest.conversationInterceptors.NotQuestsInterceptor;
import rocks.gravili.notquests.paper.managers.integrations.betonquest.events.BQAbortQuestEvent;
import rocks.gravili.notquests.paper.managers.integrations.betonquest.events.BQActionEvent;
import rocks.gravili.notquests.paper.managers.integrations.betonquest.events.BQFailQuestEvent;
import rocks.gravili.notquests.paper.managers.integrations.betonquest.events.BQQuestPointsEvent;
import rocks.gravili.notquests.paper.managers.integrations.betonquest.events.BQStartQuestEvent;
import rocks.gravili.notquests.paper.managers.integrations.betonquest.events.BQTriggerObjectiveEvent;

public class BetonQuestManager {
  private final NotQuests main;
  private final BetonQuest betonQuest;

  public BetonQuestManager(final NotQuests main) {
    this.main = main;
    betonQuest = BetonQuest.getInstance();
    initialize();
  }

  public void initialize() {
    if (main.getIntegrationsManager().isBetonQuestEnabled()) {
      // Register events
      betonQuest.registerEvents(
          "nq_action",
          BQActionEvent
              .class); // notquests_action actionname questname(optional - only used for {QUEST}
                       // placeholder in the action)
      betonQuest.registerEvents(
          "nq_triggerobjective",
          BQTriggerObjectiveEvent.class); // notquests_triggerobjective triggername
      betonQuest.registerEvents(
          "nq_startquest",
          BQStartQuestEvent
              .class); // notquests_startquest questname   (optional: -force -silent -notriggers)
      betonQuest.registerEvents(
          "nq_failquest", BQFailQuestEvent.class); // notquests_failquest questname
      betonQuest.registerEvents(
          "nq_abortquest",
          BQAbortQuestEvent
              .class); // notquests_abortquest questname //Just removes the quest from the player if
                       // it's active. Does not fail the quest
      betonQuest.registerEvents(
          "nq_questpoints",
          BQQuestPointsEvent
              .class); // notquests_questpoints action(set/add/remove) amount   (optional: -silent)

      // Register conditions
      betonQuest.registerConditions(
          "nq_condition",
          BQConditionsCondition.class); // notquests_requirement requirementtype string int

      betonQuest.registerInterceptor("notquests", NotQuestsInterceptor.class);

      main.getLogManager().info("Registered BetonQuest interceptor: notquests");
    }
  }

  public BetonQuest getBetonQuest() {
    return betonQuest;
  }



}
