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

public class QuestPointsRequirement extends Requirement {

    private final NotQuests main;
    private final long questPointRequirement;
    private final boolean deductQuestPoints;


    public QuestPointsRequirement(NotQuests main, long questPointRequirement, boolean deductQuestPoints) {
        super(RequirementType.QuestPoints, questPointRequirement);
        this.main = main;
        this.questPointRequirement = questPointRequirement;
        this.deductQuestPoints = deductQuestPoints;

    }


    public final long getQuestPointRequirement() {
        return questPointRequirement;
    }


    public final boolean isDeductQuestPoints() {
        return deductQuestPoints;
    }

}
