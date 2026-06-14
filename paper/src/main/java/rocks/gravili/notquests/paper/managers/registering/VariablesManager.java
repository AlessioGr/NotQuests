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
import rocks.gravili.notquests.paper.commands.framework.NQCommandContext;
import static rocks.gravili.notquests.paper.commands.arguments.variables.BooleanVariableArgument.booleanVariableArgument;
import static rocks.gravili.notquests.paper.commands.arguments.variables.NumberVariableArgument.numberVariableArgument;
import static rocks.gravili.notquests.paper.commands.arguments.variables.StringVariableArgument.stringVariableArgument;
import rocks.gravili.notquests.paper.commands.framework.NQDescription;
import rocks.gravili.notquests.paper.commands.framework.NQCommandManager;
import rocks.gravili.notquests.paper.commands.framework.NQCommandBuilder;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import redempt.crunch.CompiledExpression;
import redempt.crunch.Crunch;
import redempt.crunch.functional.EvaluationEnvironment;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.arguments.variables.BooleanVariableValueParser;
import rocks.gravili.notquests.paper.commands.arguments.variables.NumberVariableValueParser;
import rocks.gravili.notquests.paper.commands.arguments.variables.StringVariableValueParser;
import rocks.gravili.notquests.paper.managers.expressions.NumberExpression;
import rocks.gravili.notquests.paper.structs.variables.*;
import rocks.gravili.notquests.paper.structs.variables.hooks.*;
import rocks.gravili.notquests.paper.structs.variables.reflectionVariables.*;
import rocks.gravili.notquests.paper.structs.variables.tags.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.UUID;


public class VariablesManager {
    private final NotQuests main;

    private final HashMap<String, Class<? extends Variable<?>>> variables;
    public ArrayList<String> alreadyFullRegisteredVariables = new ArrayList<>();

    EvaluationEnvironment env = new EvaluationEnvironment();

    public VariablesManager(final NotQuests main) {
        this.main = main;
        variables = new HashMap<>();

        registerDefaultVariables();

        env.addFunction("test", 0, d -> 4);
        CompiledExpression exp = Crunch.compileExpression("test() + 1", env);
        exp.evaluate(); // will return 5
    }

    public void registerDefaultVariables() {
        main.getLogManager().info("Registering variables...");

        variables.clear();
        registerVariable("True", TrueVariable.class);
        registerVariable("False", FalseVariable.class);
        registerVariable("Condition", ConditionVariable.class);

        registerVariable("QuestPoints", QuestPointsVariable.class);
        registerVariable("Money", MoneyVariable.class);
        registerVariable("ActiveQuests", ActiveQuestsVariable.class);
        registerVariable("CompletedQuests", CompletedQuestsVariable.class);
        registerVariable("CompletedObjectiveIDsOfQuest", CompletedObjectiveIDsOfQuestVariable.class);
        registerVariable("Permission", PermissionVariable.class);
        registerVariable("Statistic", PlayerStatisticVariable.class);
        registerVariable("Name", PlayerNameVariable.class);
        registerVariable("Experience", PlayerExperienceVariable.class);
        registerVariable("ExperienceLevel", PlayerExperienceLevelVariable.class);
        registerVariable("CurrentWorld", PlayerCurrentWorldVariable.class);
        registerVariable("CurrentPositionX", PlayerCurrentPositionXVariable.class);
        registerVariable("CurrentPositionY", PlayerCurrentPositionYVariable.class);
        registerVariable("CurrentPositionZ", PlayerCurrentPositionZVariable.class);
        registerVariable("RandomNumberBetweenRange", RandomNumberBetweenRangeVariable.class);
        registerVariable("PlaytimeTicks", PlayerPlaytimeTicksVariable.class);
        registerVariable("PlaytimeMinutes", PlayerPlaytimeMinutesVariable.class);
        registerVariable("PlaytimeHours", PlayerPlaytimeHoursVariable.class);

        registerVariable("Glowing", PlayerGlowingVariable.class);
        registerVariable("Op", PlayerOpVariable.class);
        registerVariable("Climbing", PlayerClimbingVariable.class);
        registerVariable("InLava", PlayerInLavaVariable.class);
        registerVariable("InWater", PlayerInWaterVariable.class);
        registerVariable("Ping", PlayerPingVariable.class);
        registerVariable("WalkSpeed", PlayerWalkSpeedVariable.class);
        registerVariable("FlySpeed", PlayerFlySpeedVariable.class);


        registerVariable("Sleeping", PlayerSleepingVariable.class);
        registerVariable("Sneaking", PlayerSneakingVariable.class);
        registerVariable("Sprinting", PlayerSprintingVariable.class);
        registerVariable("Swimming", PlayerSwimmingVariable.class);
        registerVariable("Health", PlayerHealthVariable.class);
        registerVariable("MaxHealth", PlayerMaxHealthVariable.class);
        registerVariable("GameMode", PlayerGameModeVariable.class);
        registerVariable("Flying", PlayerFlyingVariable.class);
        registerVariable("DayOfWeek", DayOfWeekVariable.class);
        registerVariable("CurrentBiome", PlayerCurrentBiomeVariable.class);

        registerVariable("Chance", ChanceVariable.class);
        registerVariable("Advancement", AdvancementVariable.class);
        registerVariable("Inventory", InventoryVariable.class);
        registerVariable("EnderChest", EnderChestVariable.class);

        registerVariable("ContainerInventory", ContainerInventoryVariable.class);
        registerVariable("Block", BlockVariable.class);

        registerVariable("TagBoolean", BooleanTagVariable.class);
        registerVariable("TagInteger", IntegerTagVariable.class);
        registerVariable("TagFloat", FloatTagVariable.class);
        registerVariable("TagDouble", DoubleTagVariable.class);
        registerVariable("TagString", StringTagVariable.class);
        registerVariable("QuestOnCooldown", QuestOnCooldownVariable.class);
        registerVariable("QuestAbleToAccept", QuestAbleToAcceptVariable.class);
        registerVariable("QuestReachedMaxAccepts", QuestReachedMaxAcceptsVariable.class);
        registerVariable("QuestReachedMaxCompletions", QuestReachedMaxCompletionsVariable.class);
        registerVariable("QuestReachedMaxFails", QuestReachedMaxFailsVariable.class);

        registerVariable("ItemInInventoryEnchantments", ItemInInventoryEnchantmentsVariable.class);

        registerVariable("ReflectionStaticDouble", ReflectionStaticDoubleVariable.class);
        registerVariable("ReflectionStaticFloat", ReflectionStaticFloatVariable.class);
        registerVariable("ReflectionStaticInteger", ReflectionStaticIntegerVariable.class);
        registerVariable("ReflectionStaticBoolean", ReflectionStaticBooleanVariable.class);
        registerVariable("ReflectionStaticString", ReflectionStaticStringVariable.class);


        if (main.getIntegrationsManager().isPlaceholderAPIEnabled()) {
            registerVariable("PlaceholderAPINumber", PlaceholderAPINumberVariable.class);
            registerVariable("PlaceholderAPIString", PlaceholderAPIStringVariable.class);
        }
        if (main.getIntegrationsManager().isTownyEnabled()) {
            registerVariable("TownyNationTownCount", TownyNationTownCountVariable.class);
            registerVariable("TownyTownResidentCount", TownyTownResidentCountVariable.class);
            registerVariable("TownyTownPlotCount", TownyTownPlotCountVariable.class);
            registerVariable("TownyNationName", TownyNationNameVariable.class);
        }

        if (main.getIntegrationsManager().isFloodgateEnabled()) {
            registerVariable("FloodgateIsFloodgatePlayer", FloodgateIsFloodgatePlayerVariable.class);
        }
        registerVariableCheckCommands();
    }

    public void registerVariableCheckCommands() {
        //Variable check commands
        for (final String variableString : getVariableIdentifiers()) {

            final Variable<?> variable = getVariableFromString(variableString);

            if (variable == null) {
                continue;
            }
            if (alreadyFullRegisteredVariables.contains(variableString)) {
                continue;
            }


            final rocks.gravili.notquests.paper.commands.framework.NQFlag playerSelectorCommandFlag = rocks.gravili.notquests.paper.commands.framework.NQFlag.builder("player").withArgument(rocks.gravili.notquests.paper.commands.framework.NQArguments.playerArgument()).withDescription(NQDescription.of("Target player")).build();


            final NQCommandBuilder variableCheckCommandBuilder = main.getCommandManager().getAdminCommandBuilder()
                    .literal("variables", "variable")
                    .literal("check");


            main.getCommandManager().getNQCommandManager().command(registerVariableCommands(variableString, variableCheckCommandBuilder)
                    .flag(playerSelectorCommandFlag)
                    .handler((context) -> {

                        final Player playerSelector = context.flags().getValue(playerSelectorCommandFlag, null);

                        final Player player;
                        final UUID uuid;
                        if (playerSelector != null) {
                            uuid = playerSelector.getUniqueId();
                            player = playerSelector;
                        } else if (context.sender() instanceof final Player senderPlayer) {
                            uuid = senderPlayer.getUniqueId();
                            player = senderPlayer;
                        } else {
                            uuid = null;
                            player = null;
                        }


                        final HashMap<String, String> additionalStringArguments = new HashMap<>();
                        for (StringVariableValueParser<CommandSender> stringParser : variable.getRequiredStrings()) {
                            additionalStringArguments.put(stringParser.getIdentifier(), context.get(stringParser.getIdentifier()));
                        }
                        variable.setAdditionalStringArguments(additionalStringArguments);

                        final HashMap<String, NumberExpression> additionalNumberArguments = new HashMap<>();
                        for (NumberVariableValueParser<CommandSender> numberParser : variable.getRequiredNumbers()) {
                            additionalNumberArguments.put(numberParser.getIdentifier(), new NumberExpression(main, context.get(numberParser.getIdentifier())));
                        }
                        variable.setAdditionalNumberArguments(additionalNumberArguments);

                        final HashMap<String, NumberExpression> additionalBooleanArguments = new HashMap<>();
                        for (BooleanVariableValueParser<CommandSender> booleanParser : variable.getRequiredBooleans()) {
                            additionalBooleanArguments.put(booleanParser.getIdentifier(), new NumberExpression(main, context.get(booleanParser.getIdentifier())));
                        }
                        for (final rocks.gravili.notquests.paper.commands.framework.NQFlag commandFlag : variable.getRequiredBooleanFlags()) {
                            additionalBooleanArguments.put(commandFlag.name(), context.flags().isPresent(commandFlag.name()) ? NumberExpression.ofStatic(main, 1) : NumberExpression.ofStatic(main, 0));
                        }
                        variable.setAdditionalBooleanArguments(additionalBooleanArguments);


                        final Object variableValue = variable.getValue(uuid != null ? main.getQuestPlayerManager().getOrCreateQuestPlayer(uuid) : null);
                        String variableValueString = variableValue != null ? variableValue.toString() : "null";

                        if (variableValue != null) {
                            if (variable.getVariableDataType() == VariableDataType.LIST) {
                                variableValueString = String.join(",", (String[]) variableValue);
                            } else if (variable.getVariableDataType() == VariableDataType.ITEMSTACKLIST) {
                                variableValueString = "";
                                int counter = 0;
                                for (final ItemStack itemStack : (ItemStack[]) variableValue) {
                                    if (counter == 0) {
                                        variableValueString += itemStack.toString();
                                    } else {
                                        variableValueString += ", " + itemStack.toString();
                                    }
                                    counter++;
                                }
                            }
                        }
                        main.sendMessage(context.sender(), "<main>" + variableString + " variable (" + variable.getVariableDataType() + ") result for player " + (player != null ? main.getMiniMessage().serialize(player.name()) : "unknown") + ":</main> <highlight>" + variableValueString);
                    })
            );


        }
    }

    public NQCommandBuilder registerVariableCommands(
            String variableString, NQCommandBuilder builder) {
        NQCommandBuilder newBuilder =
                builder.literal(variableString, NQDescription.of("Variable Name"));

        Variable<?> variable = getVariableFromString(variableString);
        if (variable != null) {
            // Descriptions left empty so the fancy completion bar falls back to each argument's own
            // identifier (e.g. "TagName", "Block", "Field") — far more useful than the old generic,
            // and incorrect ("Optional"), labels. These are all required() arguments.
            if (variable.getRequiredStrings() != null) {
                for (StringVariableValueParser<CommandSender> stringParser : variable.getRequiredStrings()) {
                    newBuilder = newBuilder.required(stringParser.getIdentifier(), stringVariableArgument(stringParser.getIdentifier(), variable), NQDescription.EMPTY);
                }
            }
            if (variable.getRequiredNumbers() != null) {
                for (NumberVariableValueParser<CommandSender> numberParser : variable.getRequiredNumbers()) {
                    // Positional (non-greedy): these required numbers (e.g. a Block variable's x/y/z)
                    // are followed by further arguments, so they must not greedily swallow the rest.
                    newBuilder = newBuilder.required(numberParser.getIdentifier(), numberVariableArgument(numberParser.getIdentifier(), variable, false), NQDescription.EMPTY);
                }
            }
            if (variable.getRequiredBooleans() != null) {
                for (BooleanVariableValueParser<CommandSender> booleanParser : variable.getRequiredBooleans()) {
                    newBuilder = newBuilder.required(booleanParser.getIdentifier(), booleanVariableArgument(booleanParser.getIdentifier(), variable, false), NQDescription.EMPTY);
                }
            }
            if (variable.getRequiredBooleanFlags() != null) {
                for (rocks.gravili.notquests.paper.commands.framework.NQFlag commandFlag : variable.getRequiredBooleanFlags()) {
                    newBuilder = newBuilder.flag(rocks.gravili.notquests.paper.commands.framework.NQFlag.presence(commandFlag.name(), NQDescription.EMPTY));
                }
            }
        }
        return newBuilder;
    }

    public void registerVariable(
            final String identifier, final Class<? extends Variable<?>> variable) {
        if (main.getConfiguration().isVerboseStartupMessages()) {
            main.getLogManager().info("Registering variable <highlight>" + identifier);
        }
        variables.put(identifier, variable);

    /*if(main.getActionManager() != null){
        main.getActionManager().updateVariableActions();
    }*/
        if (!main.getDataManager().isCurrentlyLoading()) {
            if (main.getConditionsManager() != null) {
                main.getConditionsManager().updateVariableConditions();
            }
            if (main.getActionManager() != null) {
                main.getActionManager().updateVariableActions();
            }
            if (main.getObjectiveManager() != null) {
                main.getObjectiveManager().updateVariableObjectives();
            }
            alreadyFullRegisteredVariables.add(identifier);
        }

    /*try {
        Method commandHandler = Variable.getMethod("handleCommands", main.getClass(), NQCommandManager.class, NQCommandBuilder.class, VariableFor.class);
        commandHandler.invoke(Variable, main, main.getCommandManager().getNQCommandManager(), main.getCommandManager().getAdminEditAddRequirementCommandBuilder(), VariableFor.QUEST);
        commandHandler.invoke(Variable, main, main.getCommandManager().getNQCommandManager(), main.getCommandManager().getAdminEditObjectiveAddVariableCommandBuilder(), VariableFor.OBJECTIVE);
        commandHandler.invoke(Variable, main, main.getCommandManager().getNQCommandManager(), main.getCommandManager().getAdminAddVariableCommandBuilder(), VariableFor.variablesYML); //For Actions.yml
        commandHandler.invoke(Variable, main, main.getCommandManager().getNQCommandManager(), main.getCommandManager().getAdminEditActionsAddVariableCommandBuilder(), VariableFor.Action); //For Actions.yml
    } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
        e.printStackTrace();
    }*/
    }

    public final Class<? extends Variable<?>> getVariableClass(final String type) {
        return variables.get(type);
    }

    public final String getVariableType(final Class<? extends Variable> variable) {
        for (final String VariableType : variables.keySet()) {
            if (variables.get(VariableType).equals(variable)) {
                return VariableType;
            }
        }
        return null;
    }

    public final HashMap<String, Class<? extends Variable<?>>> getVariablesAndIdentifiers() {
        return variables;
    }

    public final Collection<Class<? extends Variable<?>>> getVariables() {
        return variables.values();
    }

    public final Collection<String> getVariableIdentifiers() {
        return variables.keySet();
    }

    public void addVariable(Variable<?> Variable, NQCommandContext context) {
    }

    public final Variable<?> getVariableFromString(final String variableString) {
        Class<? extends Variable<?>> variableClass = getVariableClass(variableString);
        try {
            return variableClass.getDeclaredConstructor(NotQuests.class).newInstance(main);
        } catch (Exception e) {
            return null;
        }
    }
}
