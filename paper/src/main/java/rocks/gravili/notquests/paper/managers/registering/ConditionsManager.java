/*
 * NotQuests - A Questing plugin for Minecraft Servers
 * Copyright (C) 2021-2022 Alessio Gravili
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

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.paper.PaperCommandManager;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.managers.data.Category;
import rocks.gravili.notquests.paper.structs.Quest;
import rocks.gravili.notquests.paper.structs.actions.Action;
import rocks.gravili.notquests.paper.structs.conditions.*;
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
        //registerCondition("Condition", ConditionCondition.class); //Old. Replaced with Condition Variable

        //registerCondition("CompletedQuest", CompletedQuestCondition.class);
        registerCondition("CompletedObjective", CompletedObjectiveCondition.class);
        //registerCondition("ActiveQuest", ActiveQuestCondition.class);

        //registerCondition("QuestPoints", QuestPointsCondition.class);
        //registerCondition("Permission", PermissionCondition.class);
        //registerCondition("Money", MoneyCondition.class);
        registerCondition("WorldTime", WorldTimeCondition.class);
        registerCondition("Date", DateCondition.class);

        //registerCondition("UltimateClansClanLevel", UltimateClansClanLevelCondition.class);

        //Towny
        //registerCondition("TownyNationName", TownyNationNameCondition.class);

        registerCondition("Number", NumberCondition.class);
        registerCondition("String", StringCondition.class);
        registerCondition("Boolean", BooleanCondition.class);
        registerCondition("List", ListCondition.class);
        registerCondition("ItemStackList", ItemStackListCondition.class);

        /*if(main.getIntegrationsManager().isBetonQuestEnabled()){
            registerCondition("BetonQuestCheckCondition", BetonQuestCheckConditionCondition.class);
            registerCondition("BetonQuestCheckInlineCondition", BetonQuestCheckInlineConditionCondition.class);
        }*/


    }


    public void registerCondition(final String identifier, final Class<? extends Condition> condition) {
        if (main.getConfiguration().isLoadingMessages()) {
            main.getLogManager().info("Registering condition <highlight>" + identifier);
        }
        conditions.put(identifier, condition);

        try {
            final Method commandHandler = condition.getMethod("handleCommands", main.getClass(), PaperCommandManager.class, Command.Builder.class, ConditionFor.class);

            commandHandler.setAccessible(true);

            if(condition == NumberCondition.class || condition == StringCondition.class || condition == BooleanCondition.class || condition == ListCondition.class || condition == ItemStackListCondition.class){
                commandHandler.invoke(condition, main, main.getCommandManager().getPaperCommandManager(), main.getCommandManager().getAdminEditAddRequirementCommandBuilder().flag(
                                main.getCommandManager().getPaperCommandManager().flagBuilder("negate")
                                        .withDescription(ArgumentDescription.of("Negates this condition"))
                        )
                        .meta(CommandMeta.DESCRIPTION, "Creates a new " + identifier + " condition"), ConditionFor.QUEST);
                commandHandler.invoke(condition, main, main.getCommandManager().getPaperCommandManager(), main.getCommandManager().getAdminEditObjectiveAddConditionCommandBuilder().flag(
                        main.getCommandManager().getPaperCommandManager().flagBuilder("negate")
                                .withDescription(ArgumentDescription.of("Negates this condition"))
                        )
                        .meta(CommandMeta.DESCRIPTION, "Creates a new " + identifier + " condition"), ConditionFor.OBJECTIVE);
                commandHandler.invoke(condition, main, main.getCommandManager().getPaperCommandManager(), main.getCommandManager().getAdminAddConditionCommandBuilder().flag(
                        main.getCommandManager().getPaperCommandManager().flagBuilder("negate")
                                .withDescription(ArgumentDescription.of("Negates this condition"))
                        )
                        .meta(CommandMeta.DESCRIPTION, "Creates a new " + identifier + " condition")
                        .flag(main.getCommandManager().categoryFlag), ConditionFor.ConditionsYML); //For Actions.yml
                commandHandler.invoke(condition, main, main.getCommandManager().getPaperCommandManager(), main.getCommandManager().getAdminActionsAddConditionCommandBuilder().flag(
                        main.getCommandManager().getPaperCommandManager().flagBuilder("negate")
                                .withDescription(ArgumentDescription.of("Negates this condition"))
                        )
                        .meta(CommandMeta.DESCRIPTION, "Creates a new " + identifier + " condition"), ConditionFor.Action); //For Actions.yml

            }else{
                commandHandler.invoke(condition, main, main.getCommandManager().getPaperCommandManager(), main.getCommandManager().getAdminEditAddRequirementCommandBuilder().literal(identifier).flag(
                        main.getCommandManager().getPaperCommandManager().flagBuilder("negate")
                                .withDescription(ArgumentDescription.of("Negates this condition"))
                )
                        .meta(CommandMeta.DESCRIPTION, "Creates a new " + identifier + " condition"), ConditionFor.QUEST);
                commandHandler.invoke(condition, main, main.getCommandManager().getPaperCommandManager(), main.getCommandManager().getAdminEditObjectiveAddConditionCommandBuilder().literal(identifier).flag(
                        main.getCommandManager().getPaperCommandManager().flagBuilder("negate")
                                .withDescription(ArgumentDescription.of("Negates this condition"))
                )
                        .meta(CommandMeta.DESCRIPTION, "Creates a new " + identifier + " condition"), ConditionFor.OBJECTIVE);
                commandHandler.invoke(condition, main, main.getCommandManager().getPaperCommandManager(), main.getCommandManager().getAdminAddConditionCommandBuilder().literal(identifier).flag(
                        main.getCommandManager().getPaperCommandManager().flagBuilder("negate")
                                .withDescription(ArgumentDescription.of("Negates this condition"))
                )
                        .meta(CommandMeta.DESCRIPTION, "Creates a new " + identifier + " condition")
                        .flag(main.getCommandManager().categoryFlag), ConditionFor.ConditionsYML); //For Actions.yml
                commandHandler.invoke(condition, main, main.getCommandManager().getPaperCommandManager(), main.getCommandManager().getAdminActionsAddConditionCommandBuilder().literal(identifier).flag(
                        main.getCommandManager().getPaperCommandManager().flagBuilder("negate")
                                .withDescription(ArgumentDescription.of("Negates this condition"))
                )
                        .meta(CommandMeta.DESCRIPTION, "Creates a new " + identifier + " condition"), ConditionFor.Action); //For Actions.yml
            }

        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }


    public final Class<? extends Condition> getConditionClass(@NotNull final String type) {
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

    public void addCondition(final Condition condition, final CommandContext<CommandSender> context) {
        condition.setNegated(context.flags().isPresent("negate"));


        final Quest quest = context.getOrDefault("quest", null);
        Objective objectiveOfQuest = null;
        if (quest != null && context.contains("Objective ID")) {
            final int objectiveID = context.get("Objective ID");
            objectiveOfQuest = quest.getObjectiveFromID(objectiveID);
        }

        final String conditionIdentifier = context.getOrDefault("Condition Identifier", "");


        String actionIdentifier = context.getOrDefault("Action Identifier", "");
        Action foundAction = context.getOrDefault("action", null);


        if (quest != null) {
            condition.setQuest(quest);
            condition.setCategory(quest.getCategory());
            if (objectiveOfQuest != null) {//Objective Condition
                condition.setObjective(objectiveOfQuest);
                condition.setConditionID(objectiveOfQuest.getFreeConditionID());

                objectiveOfQuest.addCondition(condition, true);


                context.getSender().sendMessage(main.parse(
                        "<success>" + getConditionType(condition.getClass()) + " Condition successfully added to Objective <highlight>"
                                + objectiveOfQuest.getFinalName() + "</highlight>!"));
            } else { //Quest Requirement
                condition.setConditionID(quest.getFreeRequirementID());
                quest.addRequirement(condition, true);

                context.getSender().sendMessage(main.parse(
                        "<success>" + getConditionType(condition.getClass()) + " Requirement successfully added to Quest <highlight>"
                                + quest.getQuestName() + "</highlight>!"
                ));
            }
        } else {
            if (conditionIdentifier != null && !conditionIdentifier.isBlank()) { //conditions.yml

                if (context.flags().contains(main.getCommandManager().categoryFlag)) {
                    final Category category = context.flags().getValue(main.getCommandManager().categoryFlag, main.getDataManager().getDefaultCategory());
                    condition.setCategory(category);
                }

                if (main.getConditionsYMLManager().getCondition(conditionIdentifier) == null) {
                    context.getSender().sendMessage((main.parse(main.getConditionsYMLManager().addCondition(conditionIdentifier, condition))));
                } else {
                    context.getSender().sendMessage(main.parse("<error>Error! A condition with the name <highlight>" + conditionIdentifier + "</highlight> already exists!"));
                }
            } else { //Condition for Actions.yml action

                if ( foundAction != null || (actionIdentifier != null && !actionIdentifier.isBlank()) ) {

                    foundAction = foundAction != null ? foundAction : main.getActionsYMLManager().getAction(actionIdentifier);
                    if (foundAction != null) {
                        actionIdentifier = foundAction.getActionName();

                        condition.setCategory(foundAction.getCategory());


                        foundAction.addCondition(condition, true, foundAction.getCategory().getActionsConfig(), "actions." + actionIdentifier);
                        main.getActionsYMLManager().saveActions(foundAction.getCategory());
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

    public void updateVariableConditions() {
        try {
            for (final Class<? extends Condition> condition : getConditions()) {
                final String identifier = getConditionType(condition);

                final Method commandHandler = condition.getMethod("handleCommands", main.getClass(), PaperCommandManager.class, Command.Builder.class, ConditionFor.class);

                commandHandler.setAccessible(true);
                if (condition == NumberCondition.class || condition == StringCondition.class || condition == BooleanCondition.class || condition == ListCondition.class || condition == ItemStackListCondition.class) {

                    main.getLogManager().info("Re-registering condition " + identifier + " due to variable changes...");

                    commandHandler.invoke(condition, main, main.getCommandManager().getPaperCommandManager(), main.getCommandManager().getAdminEditAddRequirementCommandBuilder().flag(
                                    main.getCommandManager().getPaperCommandManager().flagBuilder("negate")
                                            .withDescription(ArgumentDescription.of("Negates this condition"))
                            )
                            .meta(CommandMeta.DESCRIPTION, "Creates a new " + identifier + " condition"), ConditionFor.QUEST);
                    commandHandler.invoke(condition, main, main.getCommandManager().getPaperCommandManager(), main.getCommandManager().getAdminEditObjectiveAddConditionCommandBuilder().flag(
                                    main.getCommandManager().getPaperCommandManager().flagBuilder("negate")
                                            .withDescription(ArgumentDescription.of("Negates this condition"))
                            )
                            .meta(CommandMeta.DESCRIPTION, "Creates a new " + identifier + " condition"), ConditionFor.OBJECTIVE);
                    commandHandler.invoke(condition, main, main.getCommandManager().getPaperCommandManager(), main.getCommandManager().getAdminAddConditionCommandBuilder().flag(
                                    main.getCommandManager().getPaperCommandManager().flagBuilder("negate")
                                            .withDescription(ArgumentDescription.of("Negates this condition"))
                            )
                            .meta(CommandMeta.DESCRIPTION, "Creates a new " + identifier + " condition")
                            .flag(main.getCommandManager().categoryFlag), ConditionFor.ConditionsYML); //For Actions.yml
                    commandHandler.invoke(condition, main, main.getCommandManager().getPaperCommandManager(), main.getCommandManager().getAdminActionsAddConditionCommandBuilder().flag(
                                    main.getCommandManager().getPaperCommandManager().flagBuilder("negate")
                                            .withDescription(ArgumentDescription.of("Negates this condition"))
                            )
                            .meta(CommandMeta.DESCRIPTION, "Creates a new " + identifier + " condition"), ConditionFor.Action); //For Actions.yml

                }
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }

    }
}