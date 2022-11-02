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
import org.jetbrains.annotations.NotNull;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.managers.data.Category;
import rocks.gravili.notquests.paper.structs.Quest;
import rocks.gravili.notquests.paper.structs.actions.Action;
import rocks.gravili.notquests.paper.structs.conditions.BooleanCondition;
import rocks.gravili.notquests.paper.structs.conditions.CompletedObjectiveCondition;
import rocks.gravili.notquests.paper.structs.conditions.Condition;
import rocks.gravili.notquests.paper.structs.conditions.Condition.ConditionResult;
import rocks.gravili.notquests.paper.structs.conditions.ConditionFor;
import rocks.gravili.notquests.paper.structs.conditions.DateCondition;
import rocks.gravili.notquests.paper.structs.conditions.ItemStackListCondition;
import rocks.gravili.notquests.paper.structs.conditions.ListCondition;
import rocks.gravili.notquests.paper.structs.conditions.NumberCondition;
import rocks.gravili.notquests.paper.structs.conditions.StringCondition;
import rocks.gravili.notquests.paper.structs.conditions.WorldTimeCondition;
import rocks.gravili.notquests.paper.structs.objectives.Objective;

public class ConditionsManager {
    private final NotQuests main;
    private final CommandFlag<SinglePlayerSelector> playerSelectorCommandFlag;

    private final HashMap<String, Class<? extends Condition>> conditions;


    public ConditionsManager(final NotQuests main) {
        this.main = main;
        conditions = new HashMap<>();
        playerSelectorCommandFlag = CommandFlag
            .newBuilder("player")
            .withArgument(SinglePlayerSelectorArgument.of("player"))
            .build();
        registerDefaultConditions();

    }

    public void registerDefaultConditions() {
        main.getLogManager().info("Registering conditions...");
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
                commandHandler.invoke(condition, main, main.getCommandManager().getPaperCommandManager(), main.getCommandManager().getAdminEditObjectiveAddProgressConditionCommandBuilder()
                    .flag(
                        main.getCommandManager().getPaperCommandManager().flagBuilder("negate")
                            .withDescription(ArgumentDescription.of("Negates this condition"))
                    )
                    .flag(
                        main.getCommandManager().getPaperCommandManager().flagBuilder("allowProgressDecreaseIfNotFulfilled")
                            .withDescription(ArgumentDescription.of("By default, if this condition is not fulfilled, the objective progress also wont be allowed to decrease. Setting this flag would allow it to decrease in any case, while only not allowing progress to be increased if the condition is not fulfilled"))
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
                        .flag(main.getCommandManager().categoryFlag), ConditionFor.ConditionsYML); //For conditions.yml
                commandHandler.invoke(condition, main, main.getCommandManager().getPaperCommandManager(), main.getCommandManager().getAdminActionsAddConditionCommandBuilder().flag(
                        main.getCommandManager().getPaperCommandManager().flagBuilder("negate")
                                .withDescription(ArgumentDescription.of("Negates this condition"))
                        )
                        .meta(CommandMeta.DESCRIPTION, "Creates a new " + identifier + " condition"), ConditionFor.Action); //For conditions.yml

                commandHandler.invoke(condition, main, main.getCommandManager().getPaperCommandManager(), main.getCommandManager().getAdminConditionCheckCommandBuilder().flag(
                        main.getCommandManager().getPaperCommandManager().flagBuilder("negate")
                            .withDescription(ArgumentDescription.of("Negates this condition"))
                    )
                    .meta(CommandMeta.DESCRIPTION, "Checks a " + identifier + " condition inline")
                    .flag(playerSelectorCommandFlag), ConditionFor.INLINE); //For inline /qa conditions check
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
                    .flag(
                        main.getCommandManager().getPaperCommandManager().flagBuilder("allowProgressDecreaseIfNotFulfilled")
                            .withDescription(ArgumentDescription.of("By default, if this condition is not fulfilled, the objective progress also wont be allowed to decrease. Setting this flag would allow it to decrease in any case, while only not allowing progress to be increased if the condition is not fulfilled"))
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
                        .flag(main.getCommandManager().categoryFlag), ConditionFor.ConditionsYML); //For conditions.yml
                commandHandler.invoke(condition, main, main.getCommandManager().getPaperCommandManager(), main.getCommandManager().getAdminActionsAddConditionCommandBuilder().literal(identifier).flag(
                        main.getCommandManager().getPaperCommandManager().flagBuilder("negate")
                                .withDescription(ArgumentDescription.of("Negates this condition"))
                )
                        .meta(CommandMeta.DESCRIPTION, "Creates a new " + identifier + " condition"), ConditionFor.Action); //For conditions.yml


                commandHandler.invoke(condition, main, main.getCommandManager().getPaperCommandManager(), main.getCommandManager().getAdminConditionCheckCommandBuilder().literal(identifier).flag(
                        main.getCommandManager().getPaperCommandManager().flagBuilder("negate")
                            .withDescription(ArgumentDescription.of("Negates this condition"))
                    )
                    .meta(CommandMeta.DESCRIPTION, "Checks a " + identifier + " condition inline")
                    .flag(playerSelectorCommandFlag), ConditionFor.INLINE); //For inline /qa conditions check

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
            objectiveOfQuest = context.get("Objective ID"); //TODO: Support nested objectives
        }

        final String conditionIdentifier = context.getOrDefault("Condition Identifier", "");


        String actionIdentifier = context.getOrDefault("Action Identifier", "");
        Action foundAction = context.getOrDefault("action", null);


        if (quest != null) {
            condition.setObjectiveHolder(quest);
            condition.setCategory(quest.getCategory());
            if (objectiveOfQuest != null) {//Objective Condition
                condition.setObjective(objectiveOfQuest);

                if(conditionFor == ConditionFor.OBJECTIVEPROGRESS){
                    final boolean allowProgressDecreaseIfNotFulfilled = context.flags().isPresent("allowProgressDecreaseIfNotFulfilled");
                    condition.setConditionID(objectiveOfQuest.getFreeProgressConditionID());
                    condition.setObjectiveConditionSpecific_allowProgressDecreaseIfNotFulfilled(allowProgressDecreaseIfNotFulfilled);

                    objectiveOfQuest.addProgressCondition(condition, true);

                    context.getSender().sendMessage(main.parse(
                        "<success>" + getConditionType(condition.getClass()) + " Condition successfully added to Objective <highlight>"
                            + objectiveOfQuest.getDisplayNameOrIdentifier() + "</highlight>!"));

                } else if(conditionFor == ConditionFor.OBJECTIVECOMPLETE){
                    condition.setConditionID(objectiveOfQuest.getFreeCompleteConditionID());

                    objectiveOfQuest.addCompleteCondition(condition, true);

                    context.getSender().sendMessage(main.parse(
                        "<success>" + getConditionType(condition.getClass()) + " Complete Condition successfully added to Objective <highlight>"
                            + objectiveOfQuest.getDisplayNameOrIdentifier() + "</highlight>!"));

                } else {
                    condition.setConditionID(objectiveOfQuest.getFreeUnlockConditionID());

                    objectiveOfQuest.addUnlockCondition(condition, true);

                    context.getSender().sendMessage(main.parse(
                        "<success>" + getConditionType(condition.getClass()) + " Unlock Condition successfully added to Objective <highlight>"
                            + objectiveOfQuest.getDisplayNameOrIdentifier() + "</highlight>!"));
                }
            } else { //Quest Requirement
                condition.setConditionID(quest.getFreeRequirementID());
                quest.addRequirement(condition, true);

                context.getSender().sendMessage(main.parse(
                        "<success>" + getConditionType(condition.getClass()) + " Requirement successfully added to Quest <highlight>"
                                + quest.getIdentifier() + "</highlight>!"
                ));
            }
        } else {
            if(conditionFor == ConditionFor.INLINE){
                //Execute action here
                final SinglePlayerSelector singlePlayerSelector = context.flags().getValue(playerSelectorCommandFlag, null);

                final UUID uuid;
                final Player player;
                if(singlePlayerSelector != null && singlePlayerSelector.hasAny() && singlePlayerSelector.getPlayer() != null){
                    uuid = singlePlayerSelector.getPlayer().getUniqueId();
                    player = singlePlayerSelector.getPlayer();
                }else if(context.getSender() instanceof final Player senderPlayer){
                    uuid = senderPlayer.getUniqueId();
                    player = senderPlayer;
                } else {
                    uuid = null;
                    player = null;
                }
                if(uuid != null){
                    final ConditionResult conditionResult = condition.check(main.getQuestPlayerManager().getOrCreateQuestPlayer(uuid));
                    main.sendMessage(context.getSender(),"<main>" + condition.getConditionType() + " condition result for player " + (player != null ? main.getMiniMessage().serialize(player.name()) : "unknown") + ":</main> <highlight>" +  conditionResult.message() + (conditionResult.fulfilled() ? "<positive>fulfilled" : " <negative>(not fulfilled)") );
                }
            } else if (conditionIdentifier != null && !conditionIdentifier.isBlank()) { //conditions.yml

                if (context.flags().contains(main.getCommandManager().categoryFlag)) {
                    final Category category = context.flags().getValue(main.getCommandManager().categoryFlag, main.getDataManager().getDefaultCategory());
                    condition.setCategory(category);
                }

                if (main.getConditionsYMLManager().getCondition(conditionIdentifier) == null) {
                    context.getSender().sendMessage((main.parse(main.getConditionsYMLManager().addCondition(conditionIdentifier, condition))));
                } else {
                    context.getSender().sendMessage(main.parse("<error>Error! A condition with the name <highlight>" + conditionIdentifier + "</highlight> already exists!"));
                }
            } else { //Condition For conditions.yml action

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
                        .flag(
                            main.getCommandManager().getPaperCommandManager().flagBuilder("allowProgressDecreaseIfNotFulfilled")
                                .withDescription(ArgumentDescription.of("By default, if this condition is not fulfilled, the objective progress also wont be allowed to decrease. Setting this flag would allow it to decrease in any case, while only not allowing progress to be increased if the condition is not fulfilled"))
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
                            .flag(main.getCommandManager().categoryFlag), ConditionFor.ConditionsYML); //For conditions.yml
                    commandHandler.invoke(condition, main, main.getCommandManager().getPaperCommandManager(), main.getCommandManager().getAdminActionsAddConditionCommandBuilder().flag(
                                    main.getCommandManager().getPaperCommandManager().flagBuilder("negate")
                                            .withDescription(ArgumentDescription.of("Negates this condition"))
                            )
                            .meta(CommandMeta.DESCRIPTION, "Creates a new " + identifier + " condition"), ConditionFor.Action); //For conditions.yml

                    commandHandler.invoke(condition, main, main.getCommandManager().getPaperCommandManager(), main.getCommandManager().getAdminConditionCheckCommandBuilder().flag(
                            main.getCommandManager().getPaperCommandManager().flagBuilder("negate")
                                .withDescription(ArgumentDescription.of("Negates this condition"))
                        )
                        .meta(CommandMeta.DESCRIPTION, "Checks a " + identifier + " condition inline")
                        .flag(playerSelectorCommandFlag), ConditionFor.INLINE); //For inline /qa conditions check

                }
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }

    }
}