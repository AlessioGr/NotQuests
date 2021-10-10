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

package rocks.gravili.Structs.Objectives;

import rocks.gravili.NotQuests;
import rocks.gravili.Structs.Quest;

public class OtherQuestObjective extends Objective {
    private final NotQuests main;
    private final String otherQuestName;
    private final boolean countPreviousCompletions;


    public OtherQuestObjective(NotQuests main, final Quest quest, final int objectiveID, String otherQuestName, int amountOfCompletionsNeeded, boolean countPreviousCompletions) {
        super(main, quest, objectiveID, amountOfCompletionsNeeded);
        this.main = main;
        this.otherQuestName = otherQuestName;
        this.countPreviousCompletions = countPreviousCompletions;

    }

    public OtherQuestObjective(NotQuests main, Quest quest, int objectiveNumber, int progressNeeded) {
        super(main, quest, objectiveNumber, progressNeeded);
        final String questName = quest.getQuestName();

        this.main = main;
        otherQuestName = main.getDataManager().getQuestsData().getString("quests." + questName + ".objectives." + objectiveNumber + ".specifics.otherQuestName");
        countPreviousCompletions = main.getDataManager().getQuestsData().getBoolean("quests." + questName + ".objectives." + objectiveNumber + ".specifics.countPreviousCompletions");
    }

    @Override
    public String getObjectiveTaskDescription(String eventualColor) {
        return "    §7" + eventualColor + "Quest completion: §f" + eventualColor + getOtherQuest().getQuestName();
    }

    @Override
    public void save() {
        main.getDataManager().getQuestsData().set("quests." + getQuest().getQuestName() + ".objectives." + getObjectiveID() + ".specifics.otherQuestName", getOtherQuestName());
        main.getDataManager().getQuestsData().set("quests." + getQuest().getQuestName() + ".objectives." + getObjectiveID() + ".specifics.countPreviousCompletions", isCountPreviousCompletions());
    }

    public final String getOtherQuestName() {
        return otherQuestName;
    }

    public final Quest getOtherQuest() {
        return main.getQuestManager().getQuest(otherQuestName);
    }

    public final long getAmountOfCompletionsNeeded() {
        return super.getProgressNeeded();
    }

    public final boolean isCountPreviousCompletions() {
        return countPreviousCompletions;
    }


}
