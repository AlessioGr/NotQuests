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

package rocks.gravili.notquests.paper.structs.actions;


import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.Quest;
import rocks.gravili.notquests.paper.structs.conditions.Condition;
import rocks.gravili.notquests.paper.structs.objectives.Objective;

import java.util.ArrayList;

public abstract class Action {


    protected final NotQuests main;
    private String actionName = "";
    private Quest quest;
    private Objective objective;
    private final ArrayList<Condition> conditions;


    public Action(NotQuests main) {
        this.main = main;
        conditions = new ArrayList<>();
    }

    public final String getActionType() {
        return main.getActionManager().getActionType(this.getClass());
    }

    public final String getActionName() {
        return actionName;
    }

    public void setActionName(final String actionName) {
        this.actionName = actionName;
    }

    public void removeActionName() {
        this.actionName = "";
    }

    public final Quest getQuest() {
        return quest;
    }

    public void setQuest(final Quest quest) {
        this.quest = quest;
    }

    public final Objective getObjective() {
        return objective;
    }

    public void setObjective(final Objective objective) {
        this.objective = objective;
    }

    public abstract String getActionDescription();

    public abstract void execute(final Player player, Object... objects);

    public abstract void save(final FileConfiguration configuration, final String initialPath);

    public abstract void load(final FileConfiguration configuration, final String initialPath);

    public final ArrayList<Condition> getConditions() {
        return conditions;
    }

    public void addCondition(final Condition condition, final boolean save, final FileConfiguration configuration, final String initialPath) {
        conditions.add(condition);
        if (save) {
            configuration.set(initialPath + ".conditions." + conditions.size() + ".conditionType", condition.getConditionType());
            configuration.set(initialPath + ".conditions." + conditions.size() + ".progressNeeded", condition.getProgressNeeded());
            configuration.set(initialPath + ".conditions." + conditions.size() + ".negated", condition.isNegated());

            condition.save(configuration, initialPath + ".conditions." + conditions.size());
        }
    }

    public void removeCondition(final Condition condition, final boolean save, final FileConfiguration configuration, final String initialPath) {
        int conditionID = conditions.indexOf(condition);
        conditions.remove(condition);
        if (save) {
            configuration.set(initialPath + ".conditions." + conditionID, null);
        }
    }

    public void clearConditions(final FileConfiguration configuration, final String initialPath) {
        conditions.clear();
        configuration.set(initialPath + ".conditions", null);
    }


}
