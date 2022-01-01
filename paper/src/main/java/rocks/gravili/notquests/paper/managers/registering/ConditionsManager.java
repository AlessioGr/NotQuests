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

package rocks.gravili.notquests.paper.managers.registering;

import cloud.commandframework.Command;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.paper.PaperCommandManager;
import org.bukkit.command.CommandSender;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.Quest;
import rocks.gravili.notquests.paper.structs.actions.Action;
import rocks.gravili.notquests.paper.structs.conditions.*;
import rocks.gravili.notquests.paper.structs.conditions.hooks.towny.TownyNationNameCondition;
import rocks.gravili.notquests.paper.structs.conditions.hooks.towny.TownyNationTownCountCondition;
import rocks.gravili.notquests.paper.structs.conditions.hooks.towny.TownyTownPlotCountCondition;
import rocks.gravili.notquests.paper.structs.conditions.hooks.towny.TownyTownResidentCountCondition;
import rocks.gravili.notquests.paper.structs.objectives.Objective;

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
        registerCondition("Condition", ConditionCondition.class);

        registerCondition("CompletedQuest", CompletedQuestCondition.class);
        registerCondition("CompletedObjective", CompletedObjectiveCondition.class);
        registerCondition("ActiveQuest", ActiveQuestCondition.class);

        //registerCondition("QuestPoints", QuestPointsCondition.class);
        registerCondition("Permission", PermissionCondition.class);
        //registerCondition("Money", MoneyCondition.class);
        registerCondition("WorldTime", WorldTimeCondition.class);
        //registerCondition("UltimateClansClanLevel", UltimateClansClanLevelCondition.class);

        //Towny
        registerCondition("TownyNationName", TownyNationNameCondition.class);
        registerCondition("TownyNationTownCount", TownyNationTownCountCondition.class);
        registerCondition("TownyTownResidentCount", TownyTownResidentCountCondition.class);
        registerCondition("TownyTownPlotCount", TownyTownPlotCountCondition.class);

        registerCondition("Number", NumberCondition.class);


    }


    public void registerCondition(final String identifier, final Class<? extends Condition> condition) {
        main.getLogManager().info("Registering condition <highlight>" + identifier);
        conditions.put(identifier, condition);

        try {
            Method commandHandler = condition.getMethod("handleCommands", main.getClass(), PaperCommandManager.class, Command.Builder.class, ConditionFor.class);
            commandHandler.invoke(condition, main, main.getCommandManager().getPaperCommandManager(), main.getCommandManager().getAdminEditAddRequirementCommandBuilder(), ConditionFor.QUEST);
            commandHandler.invoke(condition, main, main.getCommandManager().getPaperCommandManager(), main.getCommandManager().getAdminEditObjectiveAddConditionCommandBuilder(), ConditionFor.OBJECTIVE);
            commandHandler.invoke(condition, main, main.getCommandManager().getPaperCommandManager(), main.getCommandManager().getAdminAddConditionCommandBuilder(), ConditionFor.ConditionsYML); //For Actions.yml
            commandHandler.invoke(condition, main, main.getCommandManager().getPaperCommandManager(), main.getCommandManager().getAdminEditActionsAddConditionCommandBuilder(), ConditionFor.Action); //For Actions.yml
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

    public final HashMap<String, Class<? extends Condition>> getConditionsAndIdentifiers() {
        return conditions;
    }

    public final Collection<Class<? extends Condition>> getConditions() {
        return conditions.values();
    }

    public final Collection<String> getConditionIdentifiers() {
        return conditions.keySet();
    }

    public void addCondition(Condition condition, CommandContext<CommandSender> context) {

        Quest quest = context.getOrDefault("quest", null);
        Objective objectiveOfQuest = null;
        if (quest != null && context.contains("Objective ID")) {
            int objectiveID = context.get("Objective ID");
            objectiveOfQuest = quest.getObjectiveFromID(objectiveID);
        }

        String conditionIdentifier = context.getOrDefault("Condition Identifier", "");

        String actionIdentifier = context.getOrDefault("Action Identifier", "");

        if (quest != null) {
            condition.setQuest(quest);
            if (objectiveOfQuest != null) {//Objective Condition
                condition.setObjective(objectiveOfQuest);

                objectiveOfQuest.addCondition(condition, true);

                context.getSender().sendMessage(main.parse(
                        "<success>" + getConditionType(condition.getClass()) + " Condition successfully added to Objective <highlight>"
                                + objectiveOfQuest.getObjectiveFinalName() + "</highlight>!"));
            } else { //Quest Requirement
                quest.addRequirement(condition, true);

                context.getSender().sendMessage(main.parse(
                        "<success>" + getConditionType(condition.getClass()) + " Requirement successfully added to Quest <highlight>"
                                + quest.getQuestName() + "</highlight>!"
                ));
            }
        } else {
            if (conditionIdentifier != null && !conditionIdentifier.isBlank()) { //conditions.yml

                if (main.getConditionsYMLManager().getCondition(conditionIdentifier) == null) {
                    main.getConditionsYMLManager().addCondition(conditionIdentifier, condition);
                    context.getSender().sendMessage(main.parse(
                            "<success>" + getConditionType(condition.getClass()) + " Condition with the name <highlight>"
                                    + conditionIdentifier + "</highlight> has been created successfully!"
                    ));
                } else {
                    context.getSender().sendMessage(main.parse("<error>Error! A condition with the name <highlight>" + conditionIdentifier + "</highlight> already exists!"));
                }
            } else { //Condition for Actions.yml action
                if (actionIdentifier != null && !actionIdentifier.isBlank()) {
                    Action foundAction = main.getActionsYMLManager().getAction(actionIdentifier);
                    if (foundAction != null) {
                        foundAction.addCondition(condition, true, main.getActionsYMLManager().getActionsConfig(), "actions." + actionIdentifier);
                        main.getActionsYMLManager().saveActions();
                        context.getSender().sendMessage(main.parse(
                                "<success>" + getConditionType(condition.getClass()) + " Condition successfully added to Action <highlight>"
                                        + foundAction.getActionName() + "</highlight>!"));
                    }
                }
            }
        }
    }

    public final Condition getConditionFromString(final String conditionString) {
        return null; //TODO
    }
}