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

package rocks.gravili.notquests.Managers.Registering;

import cloud.commandframework.Command;
import cloud.commandframework.paper.PaperCommandManager;
import rocks.gravili.notquests.NotQuests;
import rocks.gravili.notquests.Structs.Conditions.*;
import rocks.gravili.notquests.Structs.Conditions.hooks.TownyNationNameCondition;
import rocks.gravili.notquests.Structs.Conditions.hooks.UltimateClansClanLevelCondition;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;

public class ConditionsManager {
    private final NotQuests main;

    private final HashMap<String, Class<? extends Condition>> conditions;


    public ConditionsManager(final NotQuests main) {
        this.main = main;
        conditions = new HashMap<>();

        registerDefaultConditions();

    }

    public void registerDefaultConditions() {
        conditions.clear();
        registerCondition("OtherQuest", OtherQuestCondition.class);
        registerCondition("QuestPoints", QuestPointsCondition.class);
        registerCondition("Permission", PermissionCondition.class);
        registerCondition("Money", MoneyCondition.class);
        registerCondition("WorldTime", WorldTimeCondition.class);
        registerCondition("UltimateClansClanLevel", UltimateClansClanLevelCondition.class);
        registerCondition("ObjectiveCompleted", ObjectiveCompletedCondition.class);
        registerCondition("TownyNationName", TownyNationNameCondition.class);

    }


    public void registerCondition(final String identifier, final Class<? extends Condition> condition) {
        main.getLogManager().info("Registering condition <AQUA>" + identifier);
        conditions.put(identifier, condition);

        try {
            Method commandHandler = condition.getMethod("handleCommands", main.getClass(), PaperCommandManager.class, Command.Builder.class, Command.Builder.class);
            commandHandler.invoke(condition, main, main.getCommandManager().getPaperCommandManager(), main.getCommandManager().getAdminEditAddRequirementCommandBuilder(), main.getCommandManager().getAdminEditObjectiveAddConditionCommandBuilder());
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }


    public final Class<? extends Condition> getConditionClass(final String type) {
        return conditions.get(type);
    }

    public final String getConditionType(final Class<? extends Condition> condition) {
        for (final String conditionType : conditions.keySet()) {
            if (conditions.get(conditionType).equals(condition)) {
                return conditionType;
            }
        }
        return null;
    }

    public final HashMap<String, Class<? extends Condition>> getConditionsAndIdentfiers() {
        return conditions;
    }

    public final Collection<Class<? extends Condition>> getConditions() {
        return conditions.values();
    }

    public final Collection<String> getConditionIdentifiers() {
        return conditions.keySet();
    }
}
