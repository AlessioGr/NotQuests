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

public class OtherQuestObjective extends Objective {
    private final NotQuests main;
    private final String otherQuestName;
    private final int amountOfCompletionsNeeded;
    private final boolean countPreviousCompletions;


    public OtherQuestObjective(NotQuests main, final Quest quest, final int objectiveID, String otherQuestName, int amountOfCompletionsNeeded, boolean countPreviousCompletions) {
        super(main, quest, objectiveID, ObjectiveType.OtherQuest, amountOfCompletionsNeeded);
        this.main = main;
        this.otherQuestName = otherQuestName;
        this.amountOfCompletionsNeeded = amountOfCompletionsNeeded;
        this.countPreviousCompletions = countPreviousCompletions;

    }


    public final String getOtherQuestName() {
        return otherQuestName;
    }

    public final Quest getOtherQuest() {
        return main.getQuestManager().getQuest(otherQuestName);
    }

    public final int getAmountOfCompletionsNeeded() {
        return amountOfCompletionsNeeded;
    }

    public final boolean isCountPreviousCompletions() {
        return countPreviousCompletions;
    }


}
