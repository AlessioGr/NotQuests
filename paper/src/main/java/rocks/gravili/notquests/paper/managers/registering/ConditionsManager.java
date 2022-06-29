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
import cloud.commandframework.arguments.flags.CommandFlag;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.bukkit.arguments.selector.SinglePlayerSelector;
import cloud.commandframework.bukkit.parsers.selector.SinglePlayerSelectorArgument;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.paper.PaperCommandManager;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.UUID;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.arguments.VariableSelector;
import rocks.gravili.notquests.paper.commands.arguments.variables.BooleanVariableValueArgument;
import rocks.gravili.notquests.paper.commands.arguments.variables.NumberVariableValueArgument;
import rocks.gravili.notquests.paper.managers.data.Category;
import rocks.gravili.notquests.paper.managers.expressions.NumberExpression;
import rocks.gravili.notquests.paper.structs.Quest;
import rocks.gravili.notquests.paper.structs.actions.Action;
import rocks.gravili.notquests.paper.structs.conditions.BooleanCondition;
import rocks.gravili.notquests.paper.structs.conditions.CompletedObjectiveCondition;
import rocks.gravili.notquests.paper.structs.conditions.Condition;
import rocks.gravili.notquests.paper.structs.conditions.ConditionFor;
import rocks.gravili.notquests.paper.structs.conditions.DateCondition;
import rocks.gravili.notquests.paper.structs.conditions.ItemStackListCondition;
import rocks.gravili.notquests.paper.structs.conditions.ListCondition;
import rocks.gravili.notquests.paper.structs.conditions.NumberCondition;
import rocks.gravili.notquests.paper.structs.conditions.StringCondition;
import rocks.gravili.notquests.paper.structs.conditions.WorldTimeCondition;
import rocks.gravili.notquests.paper.structs.objectives.Objective;
import rocks.gravili.notquests.paper.structs.variables.Variable;
import rocks.gravili.notquests.paper.structs.variables.VariableDataType;

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


        //Variable check commands
        for (String variableString : main.getVariablesManager().getVariableIdentifiers()) {

            Variable<?> variable = main.getVariablesManager().getVariableFromString(variableString);

            if (variable == null) {
                continue;
            }
            if (main.getVariablesManager().alreadyFullRegisteredVariables.contains(variableString)) {
                continue;
            }


            final CommandFlag<SinglePlayerSelector> playerSelectorCommandFlag = CommandFlag
                    .newBuilder("player")
                    .withArgument(SinglePlayerSelectorArgument.of("player"))
                    .build();

            final Command.Builder<CommandSender> variableCheckCommandBuilder = main.getCommandManager().getAdminCommandBuilder()
                    .literal("variables", "variable")
                    .literal("check");


            main.getCommandManager().getPaperCommandManager().command(
                    main.getVariablesManager().registerVariableCommands(variableString, variableCheckCommandBuilder)
                    .flag(playerSelectorCommandFlag)
                    .handler((context) -> {

                        final SinglePlayerSelector singlePlayerSelector = context.flags().getValue(playerSelectorCommandFlag, null);

                        Player player = null;
                        UUID uuid = null;
                        if(singlePlayerSelector != null && singlePlayerSelector.hasAny() && singlePlayerSelector.getPlayer() != null){
                            uuid = singlePlayerSelector.getPlayer().getUniqueId();
                            player = singlePlayerSelector.getPlayer();
                        }else{
                            if(context.getSender() instanceof final Player senderPlayer){
                                uuid = senderPlayer.getUniqueId();
                                player = senderPlayer;
                            }
                        }


                        final HashMap<String, String> additionalStringArguments = new HashMap<>();
                        for (final StringArgument<CommandSender> stringArgument : variable.getRequiredStrings()) {
                            additionalStringArguments.put(stringArgument.getName(), context.get(stringArgument.getName()));
                        }
                        variable.setAdditionalStringArguments(additionalStringArguments);

                        final HashMap<String, NumberExpression> additionalNumberArguments = new HashMap<>();
                        for (final NumberVariableValueArgument<CommandSender> numberVariableValueArgument : variable.getRequiredNumbers()) {
                            additionalNumberArguments.put(numberVariableValueArgument.getName(), new NumberExpression(main, context.get(numberVariableValueArgument.getName())));
                        }
                        variable.setAdditionalNumberArguments(additionalNumberArguments);

                        final HashMap<String, NumberExpression> additionalBooleanArguments = new HashMap<>();
                        for (final BooleanVariableValueArgument<CommandSender> booleanArgument : variable.getRequiredBooleans()) {
                            additionalBooleanArguments.put(booleanArgument.getName(), new NumberExpression(main, context.get(booleanArgument.getName())));
                        }
                        for (final CommandFlag<?> commandFlag : variable.getRequiredBooleanFlags()) {
                            additionalBooleanArguments.put(commandFlag.getName(), context.flags().isPresent(commandFlag.getName()) ? NumberExpression.ofStatic(main, 1) : NumberExpression.ofStatic(main, 0));
                        }
                        variable.setAdditionalBooleanArguments(additionalBooleanArguments);


                        final Object variableValue = variable.getValue(uuid != null ? main.getQuestPlayerManager().getOrCreateQuestPlayer(uuid) : null);
                        String variableValueString = variableValue != null ? variableValue.toString() : "null";

                        if(variableValue != null){
                            if(variable.getVariableDataType() == VariableDataType.LIST){
                                variableValueString = String.join(",", (String[])variableValue);
                            }else if(variable.getVariableDataType() == VariableDataType.ITEMSTACKLIST){
                                variableValueString = "";
                                int counter = 0;
                                for(final ItemStack itemStack : (ItemStack[])variableValue){
                                    if(counter == 0){
                                        variableValueString += itemStack.toString();
                                    }else{
                                        variableValueString += ", " + itemStack.toString();
                                    }
                                    counter++;
                                }
                            }
                        }



                        main.sendMessage(context.getSender(), "<main>" + variableString + " variable (" + variable.getVariableDataType() + ") result for player " + (player != null ? main.getMiniMessage().serialize(player.name()) : "unknown") + ":</main> <highlight>" +  variableValueString);
                    })
            );


        }
        main.getCommandManager().getPaperCommandManager().command(main.getCommandManager().getAdminCommandBuilder().literal("variables")
                .literal("check")
                .argument(VariableSelector.of("variable", main), ArgumentDescription.of("Variable Name"))
                .flag(main.getCommandManager().categoryFlag)
                .meta(CommandMeta.DESCRIPTION, "Create a new quest.")
                .handler((context) -> {
                    if (context.flags().contains(main.getCommandManager().categoryFlag)) {
                        final Category category = context.flags().getValue(main.getCommandManager().categoryFlag, main.getDataManager().getDefaultCategory());
                        context.getSender().sendMessage(main.parse(main.getQuestManager().createQuest(context.get("Quest Name"), category)));
                    }else{
                        context.getSender().sendMessage(main.parse(main.getQuestManager().createQuest(context.get("Quest Name"))));
                    }
                }));

    }


    public void registerCondition(final String identifier, final Class<? extends Condition> condition) {
        if (main.getConfiguration().isVerboseStartupMessages()) {
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



                commandHandler.invoke(condition, main, main.getCommandManager().getPaperCommandManager(), main.getCommandManager().getAdminEditObjectiveAddUnlockConditionCommandBuilder().flag(
                        main.getCommandManager().getPaperCommandManager().flagBuilder("negate")
                                .withDescription(ArgumentDescription.of("Negates this condition"))
                        )
                        .meta(CommandMeta.DESCRIPTION, "Creates a new " + identifier + " unlock condition"), ConditionFor.OBJECTIVEUNLOCK);
                commandHandler.invoke(condition, main, main.getCommandManager().getPaperCommandManager(), main.getCommandManager().getAdminEditObjectiveAddProgressConditionCommandBuilder().flag(
                        main.getCommandManager().getPaperCommandManager().flagBuilder("negate")
                            .withDescription(ArgumentDescription.of("Negates this condition"))
                    )
                    .meta(CommandMeta.DESCRIPTION, "Creates a new " + identifier + " progress condition"), ConditionFor.OBJECTIVEPROGRESS);
                commandHandler.invoke(condition, main, main.getCommandManager().getPaperCommandManager(), main.getCommandManager().getAdminEditObjectiveAddCompleteConditionCommandBuilder().flag(
                        main.getCommandManager().getPaperCommandManager().flagBuilder("negate")
                            .withDescription(ArgumentDescription.of("Negates this condition"))
                    )
                    .meta(CommandMeta.DESCRIPTION, "Creates a new " + identifier + " complete condition"), ConditionFor.OBJECTIVECOMPLETE);



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



                commandHandler.invoke(condition, main, main.getCommandManager().getPaperCommandManager(), main.getCommandManager().getAdminEditObjectiveAddUnlockConditionCommandBuilder().literal(identifier).flag(
                        main.getCommandManager().getPaperCommandManager().flagBuilder("negate")
                                .withDescription(ArgumentDescription.of("Negates this condition"))
                )
                        .meta(CommandMeta.DESCRIPTION, "Creates a new " + identifier + " unlock condition"), ConditionFor.OBJECTIVEUNLOCK);
                commandHandler.invoke(condition, main, main.getCommandManager().getPaperCommandManager(), main.getCommandManager().getAdminEditObjectiveAddProgressConditionCommandBuilder().literal(identifier).flag(
                        main.getCommandManager().getPaperCommandManager().flagBuilder("negate")
                            .withDescription(ArgumentDescription.of("Negates this condition"))
                    )
                    .meta(CommandMeta.DESCRIPTION, "Creates a new " + identifier + " progress condition"), ConditionFor.OBJECTIVEPROGRESS);
                commandHandler.invoke(condition, main, main.getCommandManager().getPaperCommandManager(), main.getCommandManager().getAdminEditObjectiveAddCompleteConditionCommandBuilder().literal(identifier).flag(
                        main.getCommandManager().getPaperCommandManager().flagBuilder("negate")
                            .withDescription(ArgumentDescription.of("Negates this condition"))
                    )
                    .meta(CommandMeta.DESCRIPTION, "Creates a new " + identifier + " complete condition"), ConditionFor.OBJECTIVECOMPLETE);



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

    public void addCondition(final Condition condition, final CommandContext<CommandSender> context, final ConditionFor conditionFor) {
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

                if(conditionFor == ConditionFor.OBJECTIVEPROGRESS){
                    condition.setConditionID(objectiveOfQuest.getFreeProgressConditionID());

                    objectiveOfQuest.addProgressCondition(condition, true);

                    context.getSender().sendMessage(main.parse(
                        "<success>" + getConditionType(condition.getClass()) + " Condition successfully added to Objective <highlight>"
                            + objectiveOfQuest.getFinalName() + "</highlight>!"));

                } else if(conditionFor == ConditionFor.OBJECTIVECOMPLETE){
                    condition.setConditionID(objectiveOfQuest.getFreeCompleteConditionID());

                    objectiveOfQuest.addCompleteCondition(condition, true);

                    context.getSender().sendMessage(main.parse(
                        "<success>" + getConditionType(condition.getClass()) + " Complete Condition successfully added to Objective <highlight>"
                            + objectiveOfQuest.getFinalName() + "</highlight>!"));

                } else {
                    condition.setConditionID(objectiveOfQuest.getFreeUnlockConditionID());

                    objectiveOfQuest.addUnlockCondition(condition, true);

                    context.getSender().sendMessage(main.parse(
                        "<success>" + getConditionType(condition.getClass()) + " Unlock Condition successfully added to Objective <highlight>"
                            + objectiveOfQuest.getFinalName() + "</highlight>!"));
                }
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



                    commandHandler.invoke(condition, main, main.getCommandManager().getPaperCommandManager(), main.getCommandManager().getAdminEditObjectiveAddUnlockConditionCommandBuilder().flag(
                                    main.getCommandManager().getPaperCommandManager().flagBuilder("negate")
                                            .withDescription(ArgumentDescription.of("Negates this condition"))
                            )
                            .meta(CommandMeta.DESCRIPTION, "Creates a new " + identifier + " unlock condition"), ConditionFor.OBJECTIVEUNLOCK);
                    commandHandler.invoke(condition, main, main.getCommandManager().getPaperCommandManager(), main.getCommandManager().getAdminEditObjectiveAddProgressConditionCommandBuilder().flag(
                            main.getCommandManager().getPaperCommandManager().flagBuilder("negate")
                                .withDescription(ArgumentDescription.of("Negates this condition"))
                        )
                        .meta(CommandMeta.DESCRIPTION, "Creates a new " + identifier + " progress condition"), ConditionFor.OBJECTIVEPROGRESS);
                    commandHandler.invoke(condition, main, main.getCommandManager().getPaperCommandManager(), main.getCommandManager().getAdminEditObjectiveAddCompleteConditionCommandBuilder().flag(
                            main.getCommandManager().getPaperCommandManager().flagBuilder("negate")
                                .withDescription(ArgumentDescription.of("Negates this condition"))
                        )
                        .meta(CommandMeta.DESCRIPTION, "Creates a new " + identifier + " complete condition"), ConditionFor.OBJECTIVECOMPLETE);



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