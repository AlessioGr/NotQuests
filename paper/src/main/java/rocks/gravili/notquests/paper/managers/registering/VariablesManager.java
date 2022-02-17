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

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.arguments.flags.CommandFlag;
import cloud.commandframework.arguments.standard.BooleanArgument;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.context.CommandContext;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import redempt.crunch.CompiledExpression;
import redempt.crunch.Crunch;
import redempt.crunch.functional.EvaluationEnvironment;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.arguments.variables.NumberVariableValueArgument;
import rocks.gravili.notquests.paper.structs.variables.*;
import rocks.gravili.notquests.paper.structs.variables.hooks.*;
import rocks.gravili.notquests.paper.structs.variables.tags.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;


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
        variables.clear();
        registerVariable("True", TrueVariable.class);
        registerVariable("False", FalseVariable.class);
        registerVariable("Condition", ConditionVariable.class);

        registerVariable("QuestPoints", QuestPointsVariable.class);
        registerVariable("Money", MoneyVariable.class);
        registerVariable("UltimateClansClanLevel", UltimateClansClanLevelVariable.class);
        registerVariable("ActiveQuests", ActiveQuestsVariable.class);
        registerVariable("CompletedQuests", CompletedQuestsVariable.class);
        registerVariable("Permission", PermissionVariable.class);
        registerVariable("Name", PlayerNameVariable.class);
        registerVariable("Experience", PlayerExperienceVariable.class);
        registerVariable("ExperienceLevel", PlayerExperienceLevelVariable.class);
        registerVariable("CurrentWorld", PlayerCurrentWorldVariable.class);
        registerVariable("CurrentPositionX", PlayerCurrentPositionXVariable.class);
        registerVariable("CurrentPositionY", PlayerCurrentPositionYVariable.class);
        registerVariable("CurrentPositionZ", PlayerCurrentPositionZVariable.class);

        registerVariable("Sneaking", PlayerSneakingVariable.class);
        registerVariable("Health", PlayerHealthVariable.class);
        registerVariable("GameMode", PlayerGameModeVariable.class);
        registerVariable("Flying", PlayerFlyingVariable.class);
        registerVariable("DayOfWeek", DayOfWeekVariable.class);
        registerVariable("CurrentBiome", PlayerCurrentBiomeVariable.class);

        registerVariable("Chance", ChanceVariable.class);
        registerVariable("Advancement", AdvancementVariable.class);
        registerVariable("Inventory", InventoryVariable.class);
        registerVariable("ContainerInventory", ContainerInventoryVariable.class);

        registerVariable("TagBoolean", BooleanTagVariable.class);
        registerVariable("TagInteger", IntegerTagVariable.class);
        registerVariable("TagFloat", FloatTagVariable.class);
        registerVariable("TagDouble", DoubleTagVariable.class);
        registerVariable("TagString", StringTagVariable.class);

        registerVariable("QuestOnCooldown", QuestOnCooldownVariable.class);
        registerVariable("QuestAbleToAcceptVariable", QuestAbleToAcceptVariable.class);
        registerVariable("QuestReachedMaxAcceptsVariable", QuestReachedMaxAcceptsVariable.class);


        if(main.getIntegrationsManager().isPlaceholderAPIEnabled()){
            registerVariable("PlaceholderAPINumber", PlaceholderAPINumberVariable.class);
            registerVariable("PlaceholderAPIString", PlaceholderAPIStringVariable.class);
        }
        if(main.getIntegrationsManager().isTownyEnabled()){
            registerVariable("TownyNationTownCount", TownyNationTownCountVariable.class);
            registerVariable("TownyTownResidentCount", TownyTownResidentCountVariable.class);
            registerVariable("TownyTownPlotCount", TownyTownPlotCountVariable.class);
            registerVariable("TownyNationName", TownyNationNameVariable.class);
        }

        if(main.getIntegrationsManager().isProjectKorraEnabled()){
            registerVariable("ProjectKorraElements", ProjectKorraElementsVariable.class);
            registerVariable("ProjectKorraSubElements", ProjectKorraSubElementsVariable.class);
        }

        if(main.getIntegrationsManager().isBetonQuestEnabled()){
            registerVariable("BetonQuestCondition", BetonQuestConditionVariable.class);
        }


    }

    public Command.Builder<CommandSender> registerVariableCommands(String variableString, Command.Builder<CommandSender> builder){
        Command.Builder<CommandSender> newBuilder = builder.literal(variableString, ArgumentDescription.of("Variable Name"));

        Variable<?> variable = getVariableFromString(variableString);
        if(variable != null){
            if(variable.getRequiredStrings() != null){
                for(StringArgument<CommandSender> stringArgument : variable.getRequiredStrings()){
                    newBuilder = newBuilder.argument(stringArgument, ArgumentDescription.of("Optional String Argument"));
                }
            }
            if(variable.getRequiredNumbers() != null){
                for(NumberVariableValueArgument<CommandSender> numberVariableValueArgument : variable.getRequiredNumbers()){
                    newBuilder = newBuilder.argument(numberVariableValueArgument, ArgumentDescription.of("Optional Number Argument"));
                }
            }
            if(variable.getRequiredBooleans() != null){
                for(BooleanArgument<CommandSender> booleanArgument : variable.getRequiredBooleans()){
                    newBuilder = newBuilder.argument(booleanArgument, ArgumentDescription.of("Optional Boolean Argument"));
                }
            }
            if(variable.getRequiredBooleanFlags() != null){
                for(CommandFlag<?> commandFlag : variable.getRequiredBooleanFlags()){
                    newBuilder = newBuilder.flag(commandFlag);
                }
            }
        }
        return newBuilder;
    }


    public void registerVariable(final String identifier, final Class<? extends Variable<?>> variable) {
        main.getLogManager().info("Registering Variable <highlight>" + identifier);
        variables.put(identifier, variable);



        /*if(main.getActionManager() != null){
            main.getActionManager().updateVariableActions();
        }*/
        if(!main.getDataManager().isCurrentlyLoading()){
            if(main.getConditionsManager() != null){
                main.getConditionsManager().updateVariableConditions();
            }
            if(main.getActionManager() != null){
                main.getActionManager().updateVariableActions();
            }
            alreadyFullRegisteredVariables.add(identifier);
        }


        /*try {
            Method commandHandler = Variable.getMethod("handleCommands", main.getClass(), PaperCommandManager.class, Command.Builder.class, VariableFor.class);
            commandHandler.invoke(Variable, main, main.getCommandManager().getPaperCommandManager(), main.getCommandManager().getAdminEditAddRequirementCommandBuilder(), VariableFor.QUEST);
            commandHandler.invoke(Variable, main, main.getCommandManager().getPaperCommandManager(), main.getCommandManager().getAdminEditObjectiveAddVariableCommandBuilder(), VariableFor.OBJECTIVE);
            commandHandler.invoke(Variable, main, main.getCommandManager().getPaperCommandManager(), main.getCommandManager().getAdminAddVariableCommandBuilder(), VariableFor.variablesYML); //For Actions.yml
            commandHandler.invoke(Variable, main, main.getCommandManager().getPaperCommandManager(), main.getCommandManager().getAdminEditActionsAddVariableCommandBuilder(), VariableFor.Action); //For Actions.yml
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

    public void addVariable(Variable<?> Variable, CommandContext<CommandSender> context) {

    }

    public final Variable<?> getVariableFromString(final String variableString) {
        Class<? extends Variable<?>> variableClass = getVariableClass(variableString);
        try{
            return variableClass.getDeclaredConstructor(NotQuests.class).newInstance(main);
        }catch (Exception e){
            return null;
        }
    }


    public double evaluateExpression(String expression, final Player player, final Object... objects){

        expression = evaluateExpressionVariables(expression, player, objects);

        main.getLogManager().debug("To evaluate: <highlight>" + expression);

        CompiledExpression exp = Crunch.compileExpression(expression);


        return exp.evaluate();
    }




    public String evaluateExpressionVariables(String expression, final Player player, final Object... objects){
        boolean foundOne = false;
        for(String variableString : main.getVariablesManager().getVariableIdentifiers()){
            if(!expression.contains(variableString)){
                continue;
            }
            Variable<?> variable = main.getVariablesManager().getVariableFromString(variableString);
            if(variable == null || (variable.getVariableDataType() != VariableDataType.NUMBER && variable.getVariableDataType() != VariableDataType.BOOLEAN)){
                main.getLogManager().debug("Null variable: <highlight>" + variableString);
                continue;
            }

            //Extra Arguments:
            if(expression.contains(variableString + "(")){
                foundOne = true;
                String everythingAfterBracket = expression.substring(expression.indexOf(variableString+"(") +  variableString.length()+1 );
                String insideBracket = everythingAfterBracket.substring(0, everythingAfterBracket.indexOf(")"));
                main.getLogManager().debug("Inside Bracket: " + insideBracket);
                String[] extraArguments = insideBracket.split(",");
                for(String extraArgument : extraArguments){
                    main.getLogManager().debug("Extra: " + extraArgument);
                    if(extraArgument.startsWith("--")){
                        variable.addAdditionalBooleanArgument(extraArgument.replace("--", ""), true);
                        main.getLogManager().debug("AddBoolFlag: " + extraArgument.replace("--", ""));
                    }else{
                        String[] split = extraArgument.split(":");
                        String key = split[0];
                        String value = split[1];
                        for(StringArgument<CommandSender> stringArgument : variable.getRequiredStrings()){
                            if(stringArgument.getName().equalsIgnoreCase(key)){
                                variable.addAdditionalStringArgument(key, value);
                                main.getLogManager().debug("AddString: " + key + " val: " + value);
                            }
                        }
                        for(NumberVariableValueArgument<CommandSender> numberVariableValueArgument : variable.getRequiredNumbers()){
                            variable.addAdditionalNumberArgument(key, value);
                            main.getLogManager().debug("AddNumb: " + key + " val: " + value);
                        }
                        for(BooleanArgument<CommandSender> booleanArgument : variable.getRequiredBooleans()){
                            variable.addAdditionalBooleanArgument(key, Boolean.parseBoolean(value));
                            main.getLogManager().debug("AddBool: " + key + " val: " + value);
                        }
                    }
                }

                variableString = variableString+"(" + insideBracket + ")"; //For the replacing with the actual number below
            }



            Object valueObject = variable.getValue(player, objects);
            if(valueObject instanceof Number n){
                expression = expression.replace(variableString, ""+n.doubleValue());
            }else  if(valueObject instanceof Boolean b) {
                expression = expression.replace(variableString, ""+ (b ? 1 : 0) );

            }else{
                main.getLogManager().debug("Wrong valueObject for " + variableString +". Null?: " + (valueObject == null) );
            }
        }
        if(!foundOne){
            return expression;
        }

        return evaluateExpressionVariables(expression, player, objects);
    }
}