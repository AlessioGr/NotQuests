/*
 * NotQuests - A Questing plugin for Minecraft Servers
 * Copyright (C) 2022 Alessio Gravili
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

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.incendo.cloud.Command;
import org.incendo.cloud.description.Description;
import org.incendo.cloud.paper.PaperCommandManager;
import org.incendo.cloud.parser.flag.CommandFlag;
import org.incendo.cloud.suggestion.Suggestion;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.arguments.variables.BooleanVariableValueParser;
import rocks.gravili.notquests.paper.commands.arguments.variables.NumberVariableValueParser;
import rocks.gravili.notquests.paper.commands.arguments.variables.StringVariableValueParser;
import rocks.gravili.notquests.paper.managers.expressions.NumberExpression;
import rocks.gravili.notquests.paper.structs.QuestPlayer;
import rocks.gravili.notquests.paper.structs.variables.Variable;
import rocks.gravili.notquests.paper.structs.variables.VariableDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

import static org.incendo.cloud.parser.standard.StringParser.stringParser;
import static rocks.gravili.notquests.paper.commands.arguments.variables.NumberVariableValueParser.numberVariableParser;


public class NumberAction extends Action {

    private static boolean alreadyLoadedOnce = false;
    private String variableName;
    private String mathOperator;
    private HashMap<String, String> additionalStringArguments;
    private HashMap<String, NumberExpression> additionalNumberArguments;
    private HashMap<String, NumberExpression> additionalBooleanArguments;
    private Variable<?> cachedVariable;
    private NumberExpression numberExpression;

    public NumberAction(final NotQuests main) {
        super(main);
        additionalStringArguments = new HashMap<>();
        additionalNumberArguments = new HashMap<>();
        additionalBooleanArguments = new HashMap<>();
    }

    public static void handleCommands(NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> builder, ActionFor actionFor) {
        for (final String variableString : main.getVariablesManager().getVariableIdentifiers()) {

            final Variable<?> variable = main.getVariablesManager().getVariableFromString(variableString);

            if (variable == null || !variable.isCanSetValue() || variable.getVariableDataType() != VariableDataType.NUMBER) {
                continue;
            }
            if (main.getVariablesManager().alreadyFullRegisteredVariables.contains(variableString)) {
                continue;
            }

            if (!alreadyLoadedOnce && main.getConfiguration().isVerboseStartupMessages()) {
                main.getLogManager().info("  Registering number action: <highlight>" + variableString);
            }


            manager.command(main.getVariablesManager().registerVariableCommands(variableString, builder)
                    .required("operator", stringParser(), Description.of( "Math operator."), (context, lastString) -> {
                        ArrayList<Suggestion> completions = new ArrayList<>();
                        completions.add(Suggestion.suggestion("set"));
                        completions.add(Suggestion.suggestion("add"));
                        completions.add(Suggestion.suggestion("deduct"));
                        completions.add(Suggestion.suggestion("multiply"));
                        completions.add(Suggestion.suggestion("divide"));
                        main.getUtilManager().sendFancyCommandCompletion(context.sender(), lastString.input().split(" "), "[Math Operator]", "[...]");

                        return CompletableFuture.completedFuture(completions);
                    })
                    .required("amount", numberVariableParser("amount", variable), Description.of("Amount"))
                    .handler((context) -> {
                        final String amountExpression = context.get("amount");
                        final String mathOperator = context.get("operator");

                        NumberAction numberAction = new NumberAction(main);
                        numberAction.setVariableName(variable.getVariableType());
                        numberAction.setMathOperator(mathOperator);



                        HashMap<String, String> additionalStringArguments = new HashMap<>();
                        for(StringVariableValueParser<CommandSender> stringParser : variable.getRequiredStrings()){
                            additionalStringArguments.put(stringParser.getIdentifier(), context.get(stringParser.getIdentifier()));
                        }
                        numberAction.setAdditionalStringArguments(additionalStringArguments);

                        HashMap<String, NumberExpression> additionalNumberArguments = new HashMap<>();
                        for(NumberVariableValueParser<CommandSender> numberParser : variable.getRequiredNumbers()){
                            additionalNumberArguments.put(numberParser.getIdentifier(), new NumberExpression(main, context.get(numberParser.getIdentifier())));
                        }
                        numberAction.setAdditionalNumberArguments(additionalNumberArguments);

                        HashMap<String, NumberExpression> additionalBooleanArguments = new HashMap<>();
                        for(BooleanVariableValueParser<CommandSender> booleanParser : variable.getRequiredBooleans()){
                            additionalBooleanArguments.put(booleanParser.getIdentifier(), new NumberExpression(main, context.get(booleanParser.getIdentifier())));
                        }
                        for(CommandFlag<?> commandFlag : variable.getRequiredBooleanFlags()){
                            additionalBooleanArguments.put(commandFlag.name(), context.flags().isPresent(commandFlag.name()) ? NumberExpression.ofStatic(main, 1) : NumberExpression.ofStatic(main, 0));
                        }
                        numberAction.setAdditionalBooleanArguments(additionalBooleanArguments);

                        numberAction.initializeExpressionAndCachedVariable(amountExpression, variable.getVariableType());

                        main.getActionManager().addAction(numberAction, context, actionFor);

                    })
            );
        }
        alreadyLoadedOnce = true;
    }

    public final String getMathOperator() {
        return mathOperator;
    }

    public void setMathOperator(final String mathOperator) {
        this.mathOperator = mathOperator;
    }

    public final String getVariableName() {
        return variableName;
    }

    public void setVariableName(final String variableName){
        this.variableName = variableName;
    }

    private void setAdditionalStringArguments(HashMap<String, String> additionalStringArguments) {
        this.additionalStringArguments = additionalStringArguments;
    }

    private void setAdditionalNumberArguments(HashMap<String, NumberExpression> additionalNumberArguments) {
        this.additionalNumberArguments = additionalNumberArguments;
    }

    private void setAdditionalBooleanArguments(HashMap<String, NumberExpression> additionalBooleanArguments) {
        this.additionalBooleanArguments = additionalBooleanArguments;
    }

    public void initializeExpressionAndCachedVariable(final String expression, final String variableName) {
        if (numberExpression == null) {
            numberExpression = new NumberExpression(main, expression);
            cachedVariable = main.getVariablesManager().getVariableFromString(variableName);
        }
    }

    @Override
    public void executeInternally(final QuestPlayer questPlayer, Object... objects) {
        if (cachedVariable == null) {
            main.sendMessage(questPlayer.getPlayer(), "<ERROR>Error: variable <highlight>" + variableName + "</highlight> not found. Report this to the Server owner.");
            return;
        }

        if (additionalStringArguments != null && !additionalStringArguments.isEmpty()) {
            cachedVariable.setAdditionalStringArguments(additionalStringArguments);
        }
        if(additionalNumberArguments != null && !additionalNumberArguments.isEmpty()){
            cachedVariable.setAdditionalNumberArguments(additionalNumberArguments);
        }
        if(additionalBooleanArguments != null && !additionalBooleanArguments.isEmpty()){
            cachedVariable.setAdditionalBooleanArguments(additionalBooleanArguments);
        }


        final Object currentValueObject = cachedVariable.getValue(questPlayer, questPlayer, objects);

        double currentValue;
        if (currentValueObject instanceof final Number number) {
            currentValue = number.doubleValue();
        }else{
            currentValue = (double) currentValueObject;
        }

        double nextNewValue = numberExpression.calculateValue(questPlayer);


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
                main.sendMessage(questPlayer.getPlayer(), "<ERROR>Error: variable operator <highlight>" + getMathOperator() + "</highlight> cannot be used, because you cannot divide by 0. Report this to the Server owner.");
                return;
            }
            nextNewValue = currentValue / nextNewValue;
        }else{
            main.sendMessage(questPlayer.getPlayer(), "<ERROR>Error: variable operator <highlight>" + getMathOperator() + "</highlight> is invalid. Report this to the Server owner.");
            return;
        }

        questPlayer.sendDebugMessage("New Value: " + nextNewValue);


        if(currentValueObject instanceof Long){
            ((Variable<Long>) cachedVariable).setValue(Double.valueOf(nextNewValue).longValue(), questPlayer, objects);
        } else if(currentValueObject instanceof Float){
            ((Variable<Float>) cachedVariable).setValue(Double.valueOf(nextNewValue).floatValue(), questPlayer, objects);
        } else if(currentValueObject instanceof Double){
            ((Variable<Double>) cachedVariable).setValue(nextNewValue, questPlayer, objects);
        } else if(currentValueObject instanceof Integer){
            ((Variable<Integer>) cachedVariable).setValue(Double.valueOf(nextNewValue).intValue(), questPlayer, objects);
        }else{
            main.getLogManager().warn("Cannot execute number action, because the number type " + currentValueObject.getClass().getName() + " is invalid.");
        }

    }

    @Override
    public String getActionDescription(final QuestPlayer questPlayer, final Object... objects) {
        return variableName + ": " + numberExpression.calculateValue(questPlayer);
    }



    @Override
    public void save(final FileConfiguration configuration, String initialPath) {
        configuration.set(initialPath + ".specifics.expression", numberExpression.getRawExpression());

        configuration.set(initialPath + ".specifics.variableName", getVariableName());
        configuration.set(initialPath + ".specifics.operator", getMathOperator());

        for(final String key : additionalStringArguments.keySet()){
            configuration.set(initialPath + ".specifics.additionalStrings." + key, additionalStringArguments.get(key));
        }
        for(final String key : additionalNumberArguments.keySet()){
            configuration.set(initialPath + ".specifics.additionalNumbers." + key, additionalNumberArguments.get(key).getRawExpression());
        }
        for(final String key : additionalBooleanArguments.keySet()){
            configuration.set(initialPath + ".specifics.additionalBooleans." + key, additionalBooleanArguments.get(key).getRawExpression());
        }
    }


    @Override
    public void load(final FileConfiguration configuration, String initialPath) {
        this.variableName = configuration.getString(initialPath + ".specifics.variableName");
        this.mathOperator = configuration.getString(initialPath + ".specifics.operator", "");

        initializeExpressionAndCachedVariable(configuration.getString(initialPath + ".specifics.expression", ""), variableName);

        final ConfigurationSection additionalStringsConfigurationSection = configuration.getConfigurationSection(initialPath + ".specifics.additionalStrings");
        if (additionalStringsConfigurationSection != null) {
            for (String key : additionalStringsConfigurationSection.getKeys(false)) {
                additionalStringArguments.put(key, configuration.getString(initialPath + ".specifics.additionalStrings." + key, ""));
            }
        }

        final ConfigurationSection additionalIntegersConfigurationSection = configuration.getConfigurationSection(initialPath + ".specifics.additionalNumbers");
        if (additionalIntegersConfigurationSection != null) {
            for (String key : additionalIntegersConfigurationSection.getKeys(false)) {
                additionalNumberArguments.put(key, new NumberExpression(main, configuration.getString(initialPath + ".specifics.additionalNumbers." + key, "0")));
            }
        }

        final ConfigurationSection additionalBooleansConfigurationSection = configuration.getConfigurationSection(initialPath + ".specifics.additionalBooleans");
        if (additionalBooleansConfigurationSection != null) {
            for (String key : additionalBooleansConfigurationSection.getKeys(false)) {
                additionalBooleanArguments.put(key, new NumberExpression(main, configuration.getString(initialPath + ".specifics.additionalBooleans." + key, "false")));
            }
        }
    }

    @Override
    public void deserializeFromSingleLineString(ArrayList<String> arguments) {

        this.variableName = arguments.get(0);

        this.mathOperator = arguments.get(1);

        initializeExpressionAndCachedVariable(arguments.get(2), variableName);

        if (arguments.size() >= 4) {

            Variable<?> variable = main.getVariablesManager().getVariableFromString(variableName);
            if (variable == null || !variable.isCanSetValue() || variable.getVariableDataType() != VariableDataType.NUMBER) {
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
                        additionalStringArguments.put(variable.getRequiredStrings().get(counter-4).getIdentifier(), argument);
                        counterStrings++;
                    } else if(variable.getRequiredNumbers().size() > counterNumbers){
                        additionalNumberArguments.put(variable.getRequiredNumbers().get(counter - 4).getIdentifier(), new NumberExpression(main, argument));
                        counterNumbers++;
                    } else if(variable.getRequiredBooleans().size()  > counterBooleans){
                        additionalBooleanArguments.put(variable.getRequiredBooleans().get(counter - 4).getIdentifier(), new NumberExpression(main, argument));
                        counterBooleans++;
                    } else if(variable.getRequiredBooleanFlags().size()  > counterBooleanFlags){
                        additionalBooleanArguments.put(variable.getRequiredBooleanFlags().get(counter - 4).name(), new NumberExpression(main, argument));
                        counterBooleanFlags++;
                    }
                }
            }
        }

    }

}