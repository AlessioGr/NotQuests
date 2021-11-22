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

import java.util.UUID;

public class TalkToNPCObjective extends Objective {

    private final NotQuests main;
    private final int NPCtoTalkID;

    private final UUID armorStandUUID;

    public TalkToNPCObjective(NotQuests main, final Quest quest, final int objectiveID, final int NPCtoTalkID, final UUID armorStandUUID) {
        super(main, quest, objectiveID, 1);
        this.main = main;
        this.NPCtoTalkID = NPCtoTalkID;
        this.armorStandUUID = armorStandUUID;
    }




    public TalkToNPCObjective(NotQuests main, Quest quest, int objectiveNumber, int progressNeeded) {
        super(main, quest, objectiveNumber, progressNeeded);
        final String questName = quest.getQuestName();
        this.main = main;

        NPCtoTalkID = main.getDataManager().getQuestsData().getInt("quests." + questName + ".objectives." + objectiveNumber + ".specifics.NPCtoTalkID", -1);
        if (NPCtoTalkID != -1) {
            armorStandUUID = null;
        } else {
            final String armorStandUUIDString = main.getDataManager().getQuestsData().getString("quests." + questName + ".objectives." + objectiveNumber + ".specifics.ArmorStandToTalkUUID");
            if (armorStandUUIDString != null) {
                armorStandUUID = UUID.fromString(armorStandUUIDString);
            } else {
                armorStandUUID = null;
            }

        }
    }

    @Override
    public String getObjectiveTaskDescription(final String eventualColor, final Player player) {
        String toReturn = "";
        if (main.isCitizensEnabled() && getNPCtoTalkID() != -1) {
            final NPC npc = CitizensAPI.getNPCRegistry().getById(getNPCtoTalkID());
            if (npc != null) {
                toReturn = main.getLanguageManager().getString("chat.objectives.taskDescription.talkToNPC.base", player)
                        .replaceAll("%EVENTUALCOLOR%", eventualColor)
                        .replaceAll("%NAME%", npc.getName());
            } else {
                toReturn = "    §7" + eventualColor + "The target NPC is currently not available!";
            }
        } else {
            if (getNPCtoTalkID() != -1) {
                toReturn += "    §cError: Citizens plugin not installed. Contact an admin.";
            } else { //Armor Stands
                final UUID armorStandUUID = getArmorStandUUID();
                if (armorStandUUID != null) {
                    toReturn = main.getLanguageManager().getString("chat.objectives.taskDescription.talkToNPC.base", player)
                            .replaceAll("%EVENTUALCOLOR%", eventualColor)
                            .replaceAll("%NAME%", main.getArmorStandManager().getArmorStandName(armorStandUUID));
                } else {
                    toReturn += "    §7" + eventualColor + "The target Armor Stand is currently not available!";
                }
            }
        }
        return toReturn;
    }

    @Override
    public void save() {
        main.getDataManager().getQuestsData().set("quests." + getQuest().getQuestName() + ".objectives." + getObjectiveID() + ".specifics.NPCtoTalkID", getNPCtoTalkID());
        if (getArmorStandUUID() != null) {
            main.getDataManager().getQuestsData().set("quests." + getQuest().getQuestName() + ".objectives." + getObjectiveID() + ".specifics.ArmorStandToTalkUUID", getArmorStandUUID().toString());
        } else {
            main.getDataManager().getQuestsData().set("quests." + getQuest().getQuestName() + ".objectives." + getObjectiveID() + ".specifics.ArmorStandToTalkUUID", null);
        }
    }


    public final int getNPCtoTalkID() {
        return NPCtoTalkID;
    }

    public final UUID getArmorStandUUID() {
        return armorStandUUID;
    }


}