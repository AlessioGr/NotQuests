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

package rocks.gravili.notquests.Structs.Requirements;

import rocks.gravili.notquests.NotQuests;
import rocks.gravili.notquests.Structs.Quest;
import rocks.gravili.notquests.Structs.QuestPlayer;

public abstract class Requirement {
    private final NotQuests main;
    private final long progressNeeded;
    private final int requirementID;
    private final Quest quest;

    public Requirement(NotQuests main, Quest quest, int requirementID, long progressNeeded) {
        this.main = main;
        this.progressNeeded = progressNeeded;
        this.quest = quest;
        this.requirementID = requirementID;
    }

    public final String getRequirementType() {
        return main.getRequirementManager().getRequirementType(this.getClass());
    }

    public final long getProgressNeeded() {
        return progressNeeded;
    }

    public final Quest getQuest() {
        return quest;
    }

    public final int getRequirementID() {
        return requirementID;
    }

    /**
     * @return String if the requirement is not fulfilled. Empty string if the requirement is fulfilled. The String should say the still-required requirements.
     */
    public abstract String check(final QuestPlayer questPlayer, final boolean enforce);


    public abstract String getRequirementDescription();

    public abstract void save();
}
