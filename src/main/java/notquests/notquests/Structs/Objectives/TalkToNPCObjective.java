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

package notquests.notquests.Structs.Objectives;

import notquests.notquests.NotQuests;
import notquests.notquests.Structs.Quest;

import java.util.UUID;

public class TalkToNPCObjective extends Objective {

    private final NotQuests main;
    private final int NPCtoTalkID;

    private final UUID armorStandUUID;

    public TalkToNPCObjective(NotQuests main, final Quest quest, final int objectiveID, final int NPCtoTalkID) {
        super(main, quest, objectiveID, ObjectiveType.TalkToNPC, 1);
        this.main = main;
        this.NPCtoTalkID = NPCtoTalkID;
        this.armorStandUUID = null;
    }

    public TalkToNPCObjective(NotQuests main, final Quest quest, final int objectiveID, final UUID armorStandUUID) {
        super(main, quest, objectiveID, ObjectiveType.TalkToNPC, 1);
        this.main = main;
        this.NPCtoTalkID = -1;
        this.armorStandUUID = armorStandUUID;
    }


    public final int getNPCtoTalkID() {
        return NPCtoTalkID;
    }

    public final UUID getArmorStandUUID() {
        return armorStandUUID;
    }


}