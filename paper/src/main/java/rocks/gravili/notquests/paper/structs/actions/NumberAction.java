package rocks.gravili.notquests.paper.structs.actions;

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


import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.arguments.flags.CommandFlag;
import cloud.commandframework.arguments.standard.BooleanArgument;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.paper.PaperCommandManager;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import redempt.crunch.CompiledExpression;
import redempt.crunch.Crunch;
import redempt.crunch.functional.EvaluationEnvironment;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.arguments.variables.BooleanVariableValueArgument;
import rocks.gravili.notquests.paper.commands.arguments.variables.NumberVariableValueArgument;
import rocks.gravili.notquests.paper.structs.QuestPlayer;
import rocks.gravili.notquests.paper.structs.variables.Variable;
import rocks.gravili.notquests.paper.structs.variables.VariableDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class NumberAction extends Action {

    private String variableName;
    private String mathOperator;

    private HashMap<String, String> additionalStringArguments;
    private HashMap<String, String> additionalNumberArguments;
    private HashMap<String, String> additionalBooleanArguments;

    private String newValueExpression;


    private CompiledExpression exp;
    private final EvaluationEnvironment env = new EvaluationEnvironment();
    private int variableCounter = 0;
    Variable<?> cachedVariable = null;
    private Player playerToEvaluate = null;
    private QuestPlayer questPlayerToEvaluate = null;

    public final String getMathOperator(){
        return mathOperator;
    }
    public void setMathOperator(final String mathOperator){
        this.mathOperator = mathOperator;
    }

    public final String getVariableName(){
        return variableName;
    }

    public void setVariableName(final String variableName){
        this.variableName = variableName;
    }

    public void setNewValueExpression(final String newValueExpression){
        this.newValueExpression = newValueExpression;
    }

    public final String getNewValueExpression(){
        return newValueExpression;
    }

    private void setAdditionalStringArguments(HashMap<String, String> additionalStringArguments) {
        this.additionalStringArguments = additionalStringArguments;
    }
    private void setAdditionalNumberArguments(HashMap<String, String> additionalNumberArguments) {
        this.additionalNumberArguments = additionalNumberArguments;
    }
    private void setAdditionalBooleanArguments(HashMap<String, String> additionalBooleanArguments) {
        this.additionalBooleanArguments = additionalBooleanArguments;
    }



    public NumberAction(final NotQuests main) {
        super(main);
        additionalStringArguments = new HashMap<>();
        additionalNumberArguments = new HashMap<>();
        additionalBooleanArguments = new HashMap<>();
    }



    public static void handleCommands(NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> builder, ActionFor rewardFor) {


        for(String variableString : main.getVariablesManager().getVariableIdentifiers()){

            Variable<?> variable = main.getVariablesManager().getVariableFromString(variableString);

            if(variable == null || !variable.isCanSetValue() || variable.getVariableDataType() != VariableDataType.NUMBER){
                continue;
            }
            if(main.getVariablesManager().alreadyFullRegisteredVariables.contains(variableString)){
                continue;
            }

            manager.command(main.getVariablesManager().registerVariableCommands(variableString, builder)
                    .argument(StringArgument.<CommandSender>newBuilder("operator").withSuggestionsProvider((context, lastString) -> {
                        ArrayList<String> completions = new ArrayList<>();

                        completions.add("set");
                        completions.add("add");
                        completions.add("deduct");
                        completions.add("multiply");
                        completions.add("divide");


                        final List<String> allArgs = context.getRawInput();
                        main.getUtilManager().sendFancyCommandCompletion(context.getSender(), allArgs.toArray(new String[0]), "[Math Operator]", "[...]");

                        return completions;
                    }).build(), ArgumentDescription.of("Math operator."))
                    .argument(NumberVariableValueArgument.newBuilder("amount", main, variable), ArgumentDescription.of("Amount"))
                    .handler((context) -> {

                        final String amountExpression = context.get("amount");

                        final String mathOperator = context.get("operator");


                        NumberAction numberAction = new NumberAction(main);
                        numberAction.setVariableName(variable.getVariableType());
                        numberAction.setMathOperator(mathOperator);
                        numberAction.setNewValueExpression(amountExpression);


                        HashMap<String, String> additionalStringArguments = new HashMap<>();
                        for(StringArgument<CommandSender> stringArgument : variable.getRequiredStrings()){
                            additionalStringArguments.put(stringArgument.getName(), context.get(stringArgument.getName()));
                        }
                        numberAction.setAdditionalStringArguments(additionalStringArguments);

                        HashMap<String, String> additionalNumberArguments = new HashMap<>();
                        for(NumberVariableValueArgument<CommandSender> numberVariableValueArgument : variable.getRequiredNumbers()){
                            additionalNumberArguments.put(numberVariableValueArgument.getName(), context.get(numberVariableValueArgument.getName()));
                        }
                        numberAction.setAdditionalNumberArguments(additionalNumberArguments);

                        HashMap<String, String> additionalBooleanArguments = new HashMap<>();
                        for(BooleanVariableValueArgument<CommandSender> booleanArgument : variable.getRequiredBooleans()){
                            additionalBooleanArguments.put(booleanArgument.getName(), context.get(booleanArgument.getName()));
                        }
                        for(CommandFlag<?> commandFlag : variable.getRequiredBooleanFlags()){
                            additionalBooleanArguments.put(commandFlag.getName(), context.flags().isPresent(commandFlag.getName()) ? "true" : "false");
                        }
                        numberAction.setAdditionalBooleanArguments(additionalBooleanArguments);

                        numberAction.initializeExpressionAndCachedVariable();

                        main.getActionManager().addAction(numberAction, context);

                    })
            );
        }
    }


    public String getExpressionAndGenerateEnv(String expressions){
        boolean foundOne = false;
        for(String variableString : main.getVariablesManager().getVariableIdentifiers()){
            if(!expressions.contains(variableString)){
                continue;
            }
            Variable<?> variable = main.getVariablesManager().getVariableFromString(variableString);
            if(variable == null || (variable.getVariableDataType() != VariableDataType.NUMBER && variable.getVariableDataType() != VariableDataType.BOOLEAN)){
                main.getLogManager().debug("Null variable: <highlight>" + variableString);
                continue;
            }

            //Extra Arguments:
            if(expressions.contains(variableString + "(")){
                foundOne = true;
                String everythingAfterBracket = expressions.substring(expressions.indexOf(variableString+"(") +  variableString.length()+1 );
                String insideBracket = everythingAfterBracket.substring(0, everythingAfterBracket.indexOf(")"));
                main.getLogManager().debug("Inside Bracket: " + insideBracket);
                String[] extraArguments = insideBracket.split(",");
                for(String extraArgument : extraArguments){
                    main.getLogManager().debug("Extra: " + extraArgument);
                    if(extraArgument.startsWith("--")){
                        variable.addAdditionalBooleanArgument(extraArgument.replace("--", ""), "true");
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
                            if(numberVariableValueArgument.getName().equalsIgnoreCase(key)){
                                variable.addAdditionalNumberArgument(key, value); //TODO: continue; if found for performance benefits?
                                main.getLogManager().debug("AddNumb: " + key + " val: " + value);
                            }
                        }
                        for(BooleanVariableValueArgument<CommandSender> booleanArgument : variable.getRequiredBooleans()){
                            if(booleanArgument.getName().equalsIgnoreCase(key)){
                                variable.addAdditionalBooleanArgument(key, value);
                                main.getLogManager().debug("AddBool: " + key + " val: " + value);
                            }
                        }
                    }
                }

                variableString = variableString+"(" + insideBracket + ")"; //For the replacing with the actual number below
            }


            variableCounter++;
            expressions = expressions.replace(variableString, "var" + variableCounter);
            env.addLazyVariable("var" + variableCounter, () -> {
                final Object valueObject = variable.getValue(playerToEvaluate, questPlayerToEvaluate);
                if(valueObject instanceof final Number n){
                    return n.doubleValue();
                }else if(valueObject instanceof final Boolean b) {
                    return b ? 1 : 0;
                }
                return 0;
            });
        }
        if(!foundOne){
            return expressions;
        }

        return getExpressionAndGenerateEnv(expressions);
    }

    public void initializeExpressionAndCachedVariable(){
        if(exp == null){
            String expression = getExpressionAndGenerateEnv(getNewValueExpression());
            exp = Crunch.compileExpression(expression, env);
            cachedVariable = main.getVariablesManager().getVariableFromString(variableName);
        }

    }

    @Override
    public void executeInternally(final Player player, Object... objects) {
        this.playerToEvaluate = player;
        final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(playerToEvaluate.getUniqueId());
        this.questPlayerToEvaluate = questPlayer;
        initializeExpressionAndCachedVariable();


        if(cachedVariable == null){
            main.sendMessage(player, "<ERROR>Error: variable <highlight>" + variableName + "</highlight> not found. Report this to the Server owner.");
            return;
        }

        if(additionalStringArguments != null && !additionalStringArguments.isEmpty()){
            cachedVariable.setAdditionalStringArguments(additionalStringArguments);
        }
        if(additionalNumberArguments != null && !additionalNumberArguments.isEmpty()){
            cachedVariable.setAdditionalNumberArguments(additionalNumberArguments);
        }
        if(additionalBooleanArguments != null && !additionalBooleanArguments.isEmpty()){
            cachedVariable.setAdditionalBooleanArguments(additionalBooleanArguments);
        }




        final Object currentValueObject = cachedVariable.getValue(player, questPlayer, objects);

        double currentValue = 0d;
        if (currentValueObject instanceof final Number number) {
            currentValue = number.doubleValue();
        }else{
            currentValue = (double) currentValueObject;
        }

        //double nextNewValue = main.getVariablesManager().evaluateExpression(getNewValueExpression(), player, objects);
        double nextNewValue = exp.evaluate();


        if(getMathOperator().equalsIgnoreCase("set")){
            nextNewValue = nextNewValue;
        }else if(getMathOperator().equalsIgnoreCase("add")){
            nextNewValue = currentValue + nextNewValue;
        }else if(getMathOperator().equalsIgnoreCase("deduct")){
            nextNewValue = currentValue - nextNewValue;
        }else if(getMathOperator().equalsIgnoreCase("multiply")){
            nextNewValue = currentValue * nextNewValue;
        }else if(getMathOperator().equalsIgnoreCase("divide")){
            if(nextNewValue == 0){
                main.sendMessage(player, "<ERROR>Error: variable operator <highlight>" + getMathOperator() + "</highlight> cannot be used, because you cannot divide by 0. Report this to the Server owner.");
                return;
            }
            nextNewValue = currentValue / nextNewValue;
        }else{
            main.sendMessage(player, "<ERROR>Error: variable operator <highlight>" + getMathOperator() + "</highlight> is invalid. Report this to the Server owner.");
            return;
        }

        questPlayer.sendDebugMessage("New Value: " + nextNewValue);


        if(currentValueObject instanceof Long){
            ((Variable<Long>)cachedVariable).setValue(Double.valueOf(nextNewValue).longValue(), player, objects);
        } else if(currentValueObject instanceof Float){
            ((Variable<Float>)cachedVariable).setValue(Double.valueOf(nextNewValue).floatValue(), player, objects);
        } else if(currentValueObject instanceof Double){
            ((Variable<Double>)cachedVariable).setValue(nextNewValue, player, objects);
        } else if(currentValueObject instanceof Integer){
            ((Variable<Integer>)cachedVariable).setValue(Double.valueOf(nextNewValue).intValue(), player, objects);
        }else{
            main.getLogManager().warn("Cannot execute number action, because the number type " + currentValueObject.getClass().getName() + " is invalid.");
        }

    }

    @Override
    public String getActionDescription(final Player player, final Object... objects) {
        return variableName + ": " + main.getVariablesManager().evaluateExpression(getNewValueExpression(), player, objects);
    }



    @Override
    public void save(final FileConfiguration configuration, String initialPath) {
        configuration.set(initialPath + ".specifics.expression", getNewValueExpression());

        configuration.set(initialPath + ".specifics.variableName", getVariableName());
        configuration.set(initialPath + ".specifics.operator", getMathOperator());

        for(final String key : additionalStringArguments.keySet()){
            configuration.set(initialPath + ".specifics.additionalStrings." + key, additionalStringArguments.get(key));
        }
        for(final String key : additionalNumberArguments.keySet()){
            configuration.set(initialPath + ".specifics.additionalNumbers." + key, additionalNumberArguments.get(key));
        }
        for(final String key : additionalBooleanArguments.keySet()){
            configuration.set(initialPath + ".specifics.additionalBooleans." + key, additionalBooleanArguments.get(key));
        }
    }


    @Override
    public void load(final FileConfiguration configuration, String initialPath) {
        this.newValueExpression = configuration.getString(initialPath + ".specifics.expression", "");

        this.variableName = configuration.getString(initialPath + ".specifics.variableName");
        this.mathOperator = configuration.getString(initialPath + ".specifics.operator", "");

        final ConfigurationSection additionalStringsConfigurationSection = configuration.getConfigurationSection(initialPath + ".specifics.additionalStrings");
        if (additionalStringsConfigurationSection != null) {
            for (String key : additionalStringsConfigurationSection.getKeys(false)) {
                additionalStringArguments.put(key, configuration.getString(initialPath + ".specifics.additionalStrings." + key, ""));
            }
        }

        final ConfigurationSection additionalIntegersConfigurationSection = configuration.getConfigurationSection(initialPath + ".specifics.additionalNumbers");
        if (additionalIntegersConfigurationSection != null) {
            for (String key : additionalIntegersConfigurationSection.getKeys(false)) {
                additionalNumberArguments.put(key, configuration.getString(initialPath + ".specifics.additionalNumbers." + key, "0"));
            }
        }

        final ConfigurationSection additionalBooleansConfigurationSection = configuration.getConfigurationSection(initialPath + ".specifics.additionalBooleans");
        if (additionalBooleansConfigurationSection != null) {
            for (String key : additionalBooleansConfigurationSection.getKeys(false)) {
                additionalBooleanArguments.put(key, configuration.getBoolean(initialPath + ".specifics.additionalBooleans." + key, false) ? "true" : "false");
            }
        }
        initializeExpressionAndCachedVariable();
    }

    @Override
    public void deserializeFromSingleLineString(ArrayList<String> arguments) {

        this.variableName = arguments.get(0);

        this.mathOperator = arguments.get(1);
        this.newValueExpression = arguments.get(2);

        if(arguments.size() >= 4){

            Variable<?> variable = main.getVariablesManager().getVariableFromString(variableName);
            if(variable == null || !variable.isCanSetValue() || variable.getVariableDataType() != VariableDataType.NUMBER){
                return;
            }

            int counter = 0;
            int counterStrings = 0;
            int counterNumbers = 0;
            int counterBooleans = 0;
            int counterBooleanFlags = 0;

            for (String argument : arguments){
                counter++;
                if(counter >= 4){
                    if(variable.getRequiredStrings().size() > counterStrings){
                        additionalStringArguments.put(variable.getRequiredStrings().get(counter-4).getName(), argument);
                        counterStrings++;
                    } else if(variable.getRequiredNumbers().size() > counterNumbers){
                        additionalNumberArguments.put(variable.getRequiredNumbers().get(counter-4).getName(), argument);
                        counterNumbers++;
                    } else if(variable.getRequiredBooleans().size()  > counterBooleans){
                        additionalBooleanArguments.put(variable.getRequiredBooleans().get(counter-4).getName(), argument);
                        counterBooleans++;
                    } else if(variable.getRequiredBooleanFlags().size()  > counterBooleanFlags){
                        additionalBooleanArguments.put(variable.getRequiredBooleanFlags().get(counter-4).getName(), argument);
                        counterBooleanFlags++;
                    }
                }
            }
        }

        initializeExpressionAndCachedVariable();
    }

}