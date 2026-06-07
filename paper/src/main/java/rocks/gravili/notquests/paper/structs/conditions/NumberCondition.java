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

package rocks.gravili.notquests.paper.structs.conditions;

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

public class NumberCondition extends Condition {

    private static boolean alreadyLoadedOnce = false;
    private String variableName;
    private String mathOperator;
    private HashMap<String, String> additionalStringArguments;
    private HashMap<String, NumberExpression> additionalNumberArguments;
    private HashMap<String, NumberExpression> additionalBooleanArguments;
    private Variable<?> cachedVariable;
    private NumberExpression numberExpression;


    public NumberCondition(NotQuests main) {
        super(main);
        additionalStringArguments = new HashMap<>();
        additionalNumberArguments = new HashMap<>();
        additionalBooleanArguments = new HashMap<>();
    }

    public static void handleCommands(NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> builder, ConditionFor conditionFor) {
        for (String variableString : main.getVariablesManager().getVariableIdentifiers()) {

            Variable<?> variable = main.getVariablesManager().getVariableFromString(variableString);

            if (variable == null || variable.getVariableDataType() != VariableDataType.NUMBER) {
                continue;
            }
            if (main.getVariablesManager().alreadyFullRegisteredVariables.contains(variableString)) {
                continue;
            }

            if (!alreadyLoadedOnce && main.getConfiguration().isVerboseStartupMessages()) {
                main.getLogManager().info("  Registering number condition: <highlight>" + variableString);
            }


            manager.command(main.getVariablesManager().registerVariableCommands(variableString, builder)
                    .required("operator", stringParser(), Description.of("List operator."), (context, lastString) -> {
                        ArrayList<Suggestion> completions = new ArrayList<>();
                        completions.add(Suggestion.suggestion("equals"));
                        completions.add(Suggestion.suggestion("lessThan"));
                        completions.add(Suggestion.suggestion("moreThan"));
                        completions.add(Suggestion.suggestion("moreOrEqualThan"));
                        completions.add(Suggestion.suggestion("lessOrEqualThan"));
                        main.getUtilManager().sendFancyCommandCompletion(context.sender(), lastString.input().split(" "), "[List Operator]", "[...]");
                        return CompletableFuture.completedFuture(completions);
                    })
                    .required("amount", numberVariableParser("amount", null), Description.of("Amount"))
                    .handler((context) -> {

                        final String amountExpression = context.get("amount");


                        final String mathOperator = context.get("operator");

                        final NumberCondition numberCondition = new NumberCondition(main);
                        numberCondition.setVariableName(variableString);

                        numberCondition.setMathOperator(mathOperator);
                        //numberCondition.setProgressNeeded(variable.getValue());

                        //questPointsCondition.setDeductQuestPoints(deductQuestPoints);


                        HashMap<String, String> additionalStringArguments = new HashMap<>();
                        for(StringVariableValueParser stringParser : variable.getRequiredStrings()){
                            additionalStringArguments.put(stringParser.getIdentifier(), context.get(stringParser.getIdentifier()));
                        }
                        numberCondition.setAdditionalStringArguments(additionalStringArguments);

                        HashMap<String, NumberExpression> additionalNumberArguments = new HashMap<>();
                        for(NumberVariableValueParser numberParser : variable.getRequiredNumbers()){
                            additionalNumberArguments.put(numberParser.getIdentifier(), new NumberExpression(main, context.get(numberParser.getIdentifier())));
                        }
                        numberCondition.setAdditionalNumberArguments(additionalNumberArguments);

                        HashMap<String, NumberExpression> additionalBooleanArguments = new HashMap<>();
                        for(BooleanVariableValueParser booleanParser : variable.getRequiredBooleans()){
                            additionalBooleanArguments.put(booleanParser.getIdentifier(), new NumberExpression(main, context.get(booleanParser.getIdentifier())));
                        }
                        for(CommandFlag<?> commandFlag : variable.getRequiredBooleanFlags()){
                            additionalBooleanArguments.put(commandFlag.name(), context.flags().isPresent(commandFlag.name()) ? NumberExpression.ofStatic(main, 1) : NumberExpression.ofStatic(main, 0));
                        }
                        numberCondition.setAdditionalBooleanArguments(additionalBooleanArguments);

                        numberCondition.initializeExpressionAndCachedVariable(amountExpression, variableString);

                        main.getConditionsManager().addCondition(numberCondition, context, conditionFor);
                    })
            );


        }
        alreadyLoadedOnce = true;


    }

    public void initializeExpressionAndCachedVariable(final String expression, final String variableName) {
        if (numberExpression == null) {
            numberExpression = new NumberExpression(main, expression);
            cachedVariable = main.getVariablesManager().getVariableFromString(variableName);
        }
    }

    @Override
    public String checkInternally(final QuestPlayer questPlayer) {
        if (cachedVariable == null) {
            return "<ERROR>Error: variable <highlight>" + variableName + "</highlight> not found. Report this to the Server owner.";
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

        Object value = cachedVariable.getValue(questPlayer);

        final double numberRequirement = numberExpression.calculateValue(questPlayer);

        if(getMathOperator().equalsIgnoreCase("moreThan")){
            if(value instanceof Long l){
                if (l <= numberRequirement) {
                    return "<YELLOW>You need <highlight>" + (numberRequirement+1 - l) + "</highlight> more " + cachedVariable.getPlural() + ".";
                }
            }else if(value instanceof Float f){
                if (f <= numberRequirement) {
                    return "<YELLOW>You need <highlight>" + (numberRequirement+1 - f) + "</highlight> more " + cachedVariable.getPlural() + ".";
                }
            }else if(value instanceof Double d){
                if (d <= numberRequirement) {
                    return "<YELLOW>You need <highlight>" + (numberRequirement+1 - d) + "</highlight> more " + cachedVariable.getPlural() + ".";
                }
            }else if(value instanceof Integer i){
                if (i <= numberRequirement) {
                    return "<YELLOW>You need <highlight>" + (numberRequirement+1 - i) + "</highlight> more " + cachedVariable.getPlural() + ".";
                }
            }else{
                if ((long)value <= numberRequirement) {
                    return "<YELLOW>You need <highlight>" + (numberRequirement+1 - (long)value) + "</highlight> more " + cachedVariable.getPlural() + ".";
                }
            }
        }else if(getMathOperator().equalsIgnoreCase("moreOrEqualThan")){
            if(value instanceof Long l){
                if (l < numberRequirement) {
                    return "<YELLOW>You need <highlight>" + (numberRequirement - l) + "</highlight> more " + cachedVariable.getPlural() + ".";
                }
            }else if(value instanceof Float f){
                if (f < numberRequirement) {
                    return "<YELLOW>You need <highlight>" + (numberRequirement - f) + "</highlight> more " + cachedVariable.getPlural() + ".";
                }
            }else if(value instanceof Double d){
                if (d < numberRequirement) {
                    return "<YELLOW>You need <highlight>" + (numberRequirement - d) + "</highlight> more " + cachedVariable.getPlural() + ".";
                }
            }else if(value instanceof Integer i){
                if (i < numberRequirement) {
                    return "<YELLOW>You need <highlight>" + (numberRequirement - i) + "</highlight> more " + cachedVariable.getPlural() + ".";
                }
            }else{
                if ((long)value < numberRequirement) {
                    return "<YELLOW>You need <highlight>" + (numberRequirement - (long)value) + "</highlight> more " + cachedVariable.getPlural() + ".";
                }
            }
        }else if(getMathOperator().equalsIgnoreCase("lessThan")){
            if(value instanceof Long l){
                if (l >= numberRequirement) {
                    return "<YELLOW>You have <highlight>" + (l+1 - numberRequirement) + "</highlight> too many " + cachedVariable.getPlural() + ".";
                }
            }else if(value instanceof Float f){
                if (f >= numberRequirement) {
                    return "<YELLOW>You have <highlight>" + (f+1 - numberRequirement) + "</highlight> too many " + cachedVariable.getPlural() + ".";
                }
            }else if(value instanceof Double d){
                if (d >= numberRequirement) {
                    return "<YELLOW>You have <highlight>" + (d+1 - numberRequirement) + "</highlight> too many " + cachedVariable.getPlural() + ".";
                }
            }else if(value instanceof Integer i){
                if (i >= numberRequirement) {
                    return "<YELLOW>You have <highlight>" + (i+1 - numberRequirement) + "</highlight> too many " + cachedVariable.getPlural() + ".";
                }
            }else{
                if ((long)value >= numberRequirement) {
                    return "<YELLOW>You have <highlight>" + ((long)value+1 - numberRequirement) + "</highlight> too many " + cachedVariable.getPlural() + ".";
                }
            }
        }else if(getMathOperator().equalsIgnoreCase("lessOrEqualThan")){
            if(value instanceof Long l){
                if (l > numberRequirement) {
                    return "<YELLOW>You have <highlight>" + (l - numberRequirement) + "</highlight> too many " + cachedVariable.getPlural() + ".";
                }
            }else if(value instanceof Float f){
                if (f > numberRequirement) {
                    return "<YELLOW>You have <highlight>" + (f - numberRequirement) + "</highlight> too many " + cachedVariable.getPlural() + ".";
                }
            }else if(value instanceof Double d){
                if (d > numberRequirement) {
                    return "<YELLOW>You have <highlight>" + (d - numberRequirement) + "</highlight> too many " + cachedVariable.getPlural() + ".";
                }
            }else if(value instanceof Integer i){
                if (i > numberRequirement) {
                    return "<YELLOW>You have <highlight>" + (i - numberRequirement) + "</highlight> too many " + cachedVariable.getPlural() + ".";
                }
            }else{
                if ((long)value >= numberRequirement) {
                    return "<YELLOW>You have <highlight>" + ((long)value - numberRequirement) + "</highlight> too many " + cachedVariable.getPlural() + ".";
                }
            }
        }else if(getMathOperator().equalsIgnoreCase("equals")){
            if(value instanceof Long l){
                if (l != numberRequirement) {
                    return "<YELLOW>You need EXACTLY <highlight>" + numberRequirement+ "</highlight> " + cachedVariable.getPlural() + " - no more or less.";
                }
            }else if(value instanceof Float f){
                if (f != numberRequirement) {
                    return "<YELLOW>You need EXACTLY <highlight>" + numberRequirement+ "</highlight> " + cachedVariable.getPlural() + " - no more or less.";
                }
            }else if(value instanceof Double d){
                if (d != numberRequirement) {
                    return "<YELLOW>You need EXACTLY <highlight>" + numberRequirement+ "</highlight> " + cachedVariable.getPlural() + " - no more or less.";
                }
            }else if(value instanceof Integer i){
                if (i != numberRequirement) {
                    return "<YELLOW>You need EXACTLY <highlight>" + numberRequirement+ "</highlight> " + cachedVariable.getPlural() + " - no more or less.";
                }
            }else{
                if ((long)value != numberRequirement) {
                    return "<YELLOW>You need EXACTLY <highlight>" + numberRequirement+ "</highlight> " + cachedVariable.getPlural() + " - no more or less.";
                }
            }
        }else{
            return "<ERROR>Error: variable operator <highlight>" + getMathOperator() + "</highlight> is invalid. Report this to the Server owner.";

        }


        return "";
    }

    @Override
    public void save(FileConfiguration configuration, final String initialPath) {
        configuration.set(initialPath + ".specifics.variableName", getVariableName());
        configuration.set(initialPath + ".specifics.operator", getMathOperator());
        configuration.set(initialPath + ".specifics.expression", numberExpression.getRawExpression());

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
    public void load(FileConfiguration configuration, String initialPath) {
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

            for (final String argument : arguments) {
                counter++;
                if (counter >= 4) {
                    if (variable.getRequiredStrings().size() > counterStrings) {
                        additionalStringArguments.put(variable.getRequiredStrings().get(counter - 4).getIdentifier(), argument);
                        counterStrings++;
                    } else if (variable.getRequiredNumbers().size() > counterNumbers) {
                        additionalNumberArguments.put(variable.getRequiredNumbers().get(counter - 4).getIdentifier(), new NumberExpression(main, argument));
                        counterNumbers++;
                    } else if (variable.getRequiredBooleans().size() > counterBooleans) {
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

    @Override
    public String getConditionDescriptionInternally(QuestPlayer questPlayer, Object... objects) {
        //description += "\n<GRAY>--- Will quest points be deducted?: No";

        final double expressionValue = numberExpression.calculateValue(questPlayer);

        if (getMathOperator().equalsIgnoreCase("moreThan")) {
            return "<GRAY>-- " + variableName + " needed: More than " + expressionValue + "</GRAY>";
        } else if (getMathOperator().equalsIgnoreCase("moreOrEqualThan")) {
            return "<GRAY>-- " + variableName + " needed: More or equal than " + expressionValue + "</GRAY>";
        } else if (getMathOperator().equalsIgnoreCase("lessThan")) {
            return "<GRAY>-- " + variableName + " needed: Less than " + expressionValue + "</GRAY>";
        } else if (getMathOperator().equalsIgnoreCase("lessOrEqualThan")) {
            return "<GRAY>-- " + variableName + " needed: Less or equal than" + expressionValue + "</GRAY>";
        } else if (getMathOperator().equalsIgnoreCase("equals")) {
            return "<GRAY>-- " + variableName + " needed: Exactly " + expressionValue + "</GRAY>";
        }

        return "<GRAY>-- " + variableName + " needed: " + expressionValue + "</GRAY>";
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

    public final String getMathOperator() {
        return mathOperator;
    }

    public void setMathOperator(final String mathOperator) {
        this.mathOperator = mathOperator;
    }

    public final String getVariableName() {
        return variableName;
    }

    public void setVariableName(final String variableName) {
        this.variableName = variableName;
    }
}
