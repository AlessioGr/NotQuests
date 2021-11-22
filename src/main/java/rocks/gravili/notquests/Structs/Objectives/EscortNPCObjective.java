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

package rocks.gravili.notquests.Structs.Objectives;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.NotQuests;
import rocks.gravili.notquests.Structs.Quest;

public class EscortNPCObjective extends Objective {

    private final NotQuests main;
    private final int npcToEscortID;
    private final int npcToEscortToID;

    public EscortNPCObjective(NotQuests main, final Quest quest, final int objectiveID, final int npcToEscortID, final int npcToEscortToID) {
        super(main, quest, objectiveID, 1);
        this.main = main;
        this.npcToEscortID = npcToEscortID;
        this.npcToEscortToID = npcToEscortToID;
    }

    public EscortNPCObjective(NotQuests main, Quest quest, int objectiveNumber, int progressNeeded) {
        super(main, quest, objectiveNumber, progressNeeded);
        final String questName = quest.getQuestName();
        this.main = main;

        npcToEscortID = main.getDataManager().getQuestsData().getInt("quests." + questName + ".objectives." + objectiveNumber + ".specifics.NPCToEscortID");
        npcToEscortToID = main.getDataManager().getQuestsData().getInt("quests." + questName + ".objectives." + objectiveNumber + ".specifics.destinationNPCID");

    }

    @Override
    public String getObjectiveTaskDescription(final String eventualColor, final Player player) {
        String toReturn = "";
        if (main.isCitizensEnabled()) {
            final NPC npc = CitizensAPI.getNPCRegistry().getById(getNpcToEscortID());
            final NPC npcDestination = CitizensAPI.getNPCRegistry().getById(getNpcToEscortToID());

            if (npc != null && npcDestination != null) {
                toReturn = main.getLanguageManager().getString("chat.objectives.taskDescription.escortNPC.base", player)
                        .replaceAll("%EVENTUALCOLOR%", eventualColor)
                        .replaceAll("%NPCNAME%", "" + npc.getName())
                        .replaceAll("%DESTINATIONNPCNAME%", "" + npcDestination.getName());
            } else {
                toReturn = "    §7" + eventualColor + "The target or destination NPC is currently not available!";
            }
        } else {
            toReturn += "    §cError: Citizens plugin not installed. Contact an admin.";
        }
        return toReturn;
    }

    @Override
    public void save() {
        main.getDataManager().getQuestsData().set("quests." + getQuest().getQuestName() + ".objectives." + getObjectiveID() + ".specifics.NPCToEscortID", getNpcToEscortID());
        main.getDataManager().getQuestsData().set("quests." + getQuest().getQuestName() + ".objectives." + getObjectiveID() + ".specifics.destinationNPCID", getNpcToEscortToID());
    }

    public final int getNpcToEscortID() {
        return npcToEscortID;
    }

    public final int getNpcToEscortToID() {
        return npcToEscortToID;
    }


}
