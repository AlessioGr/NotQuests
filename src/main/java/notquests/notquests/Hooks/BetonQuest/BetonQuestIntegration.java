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

package notquests.notquests.Hooks.BetonQuest;

import notquests.notquests.Hooks.BetonQuest.Events.BQTriggerObjectiveEvent;
import notquests.notquests.NotQuests;
import org.betonquest.betonquest.BetonQuest;

public class BetonQuestIntegration {
    private final NotQuests main;
    private final BetonQuest betonQuest;


    public BetonQuestIntegration(final NotQuests main) {
        this.main = main;
        betonQuest = BetonQuest.getInstance();
        initialize();
    }

    public void initialize() {
        //Register events
        betonQuest.registerEvents("notquests_triggerobjective", BQTriggerObjectiveEvent.class); //notquests_triggerobjective triggername
        betonQuest.registerEvents("notquests_action", BQTriggerObjectiveEvent.class); //notquests_action actionname questname(optional)

    }

    public BetonQuest getBetonQuest() {
        return betonQuest;
    }
}
