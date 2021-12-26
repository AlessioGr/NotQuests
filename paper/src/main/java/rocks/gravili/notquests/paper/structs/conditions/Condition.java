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

package rocks.gravili.notquests.paper.structs.conditions;

import org.bukkit.configuration.file.FileConfiguration;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.Quest;
import rocks.gravili.notquests.paper.structs.QuestPlayer;
import rocks.gravili.notquests.paper.structs.objectives.Objective;

public abstract class Condition {
    protected final NotQuests main;
    private String conditionName = "";
    private long progressNeeded = 1;
    private Quest quest;
    private Objective objective;

    public Condition(NotQuests main) {
        this.main = main;
    }

    public void setProgressNeeded(final long progressNeeded){
        this.progressNeeded = progressNeeded;
    }
    public void setQuest(final Quest quest){
        this.quest = quest;
    }
    public void setObjective(final Objective objective){
        this.objective = objective;
    }

    public final String getConditionType() {
        return main.getConditionsManager().getConditionType(this.getClass());
    }

    public final long getProgressNeeded() {
        return progressNeeded;
    }

    public final Quest getQuest() {
        return quest;
    }

    public final Objective getObjective() {
        return objective;
    }

    public final String getConditionName() {
        return conditionName;
    }

    public void setConditionName(final String conditionName) {
        this.conditionName = conditionName;
    }


    /**
     * @return String if the condition is not fulfilled. Empty string if the condition is fulfilled. The String should say the still-required condition.
     */
    public abstract String check(final QuestPlayer questPlayer, final boolean enforce);


    public abstract String getConditionDescription();

    public abstract void save(final FileConfiguration configuration, final String initialPath);

    public abstract void load(final FileConfiguration configuration, final String initialPath);
}
