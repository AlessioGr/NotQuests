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

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.arguments.flags.CommandFlag;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.paper.PaperCommandManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.arguments.variables.BooleanVariableValueArgument;
import rocks.gravili.notquests.paper.commands.arguments.variables.NumberVariableValueArgument;
import rocks.gravili.notquests.paper.managers.expressions.NumberExpression;
import rocks.gravili.notquests.paper.structs.QuestPlayer;
import rocks.gravili.notquests.paper.structs.variables.Variable;
import rocks.gravili.notquests.paper.structs.variables.VariableDataType;

public class BooleanCondition extends Condition {

    private static boolean alreadyLoadedOnce = false;
    private String variableName;
    private String operator;
    private HashMap<String, String> additionalStringArguments;
    private HashMap<String, NumberExpression> additionalNumberArguments;
    private HashMap<String, NumberExpression> additionalBooleanArguments;
    private Variable<?> cachedVariable;
    private NumberExpression numberExpression;


    public BooleanCondition(NotQuests main) {
        super(main);
        additionalStringArguments = new HashMap<>();
        additionalNumberArguments = new HashMap<>();
        additionalBooleanArguments = new HashMap<>();
    }

    public static void handleCommands(final NotQuests main, final PaperCommandManager<CommandSender> manager, final Command.Builder<CommandSender> builder, final ConditionFor conditionFor) {
        for (final String variableString : main.getVariablesManager().getVariableIdentifiers()) {

            final Variable<?> variable = main.getVariablesManager().getVariableFromString(variableString);

            if (variable == null || variable.getVariableDataType() != VariableDataType.BOOLEAN) {
                continue;
            }

            if (main.getVariablesManager().alreadyFullRegisteredVariables.contains(variableString)) {
                continue;
            }

            if (!alreadyLoadedOnce && main.getConfiguration().isVerboseStartupMessages()) {
                main.getLogManager().info("  Registering boolean condition: <highlight>" + variableString);
            }


            manager.command(main.getVariablesManager().registerVariableCommands(variableString, builder)
                    .argument(StringArgument.<CommandSender>newBuilder("operator").withSuggestionsProvider((context, lastString) -> {
                        ArrayList<String> completions = new ArrayList<>();
                        completions.add("and");
                        completions.add("equals");
                        completions.add("or");


                        final List<String> allArgs = context.getRawInput();
                        main.getUtilManager().sendFancyCommandCompletion(context.getSender(), allArgs.toArray(new String[0]), "[Comparison Operator]", "[...]");

                        return completions;
                    }).build(), ArgumentDescription.of("Comparison operator."))
                    .argument(BooleanVariableValueArgument.newBuilder("expression", main, variable), ArgumentDescription.of("Expression"))
                    .handler((context) -> {

                        final String expression = context.get("expression");
                        final String operator = context.get("operator");

                        BooleanCondition booleanCondition = new BooleanCondition(main);
                        booleanCondition.setOperator(operator);
                        booleanCondition.setVariableName(variable.getVariableType());

                        booleanCondition.initializeExpressionAndCachedVariable(expression, variable.getVariableType());

                        HashMap<String, String> additionalStringArguments = new HashMap<>();
                        for (StringArgument<CommandSender> stringArgument : variable.getRequiredStrings()) {
                            additionalStringArguments.put(stringArgument.getName(), context.get(stringArgument.getName()));
                        }
                        booleanCondition.setAdditionalStringArguments(additionalStringArguments);

                        HashMap<String, NumberExpression> additionalNumberArguments = new HashMap<>();
                        for (NumberVariableValueArgument<CommandSender> numberVariableValueArgument : variable.getRequiredNumbers()) {
                            additionalNumberArguments.put(numberVariableValueArgument.getName(), new NumberExpression(main, context.get(numberVariableValueArgument.getName())));
                        }
                        booleanCondition.setAdditionalNumberArguments(additionalNumberArguments);

                        HashMap<String, NumberExpression> additionalBooleanArguments = new HashMap<>();
                        for (BooleanVariableValueArgument<CommandSender> booleanArgument : variable.getRequiredBooleans()) {
                            additionalBooleanArguments.put(booleanArgument.getName(), new NumberExpression(main, context.get(booleanArgument.getName())));
                        }
                        for (CommandFlag<?> commandFlag : variable.getRequiredBooleanFlags()) {
                            additionalBooleanArguments.put(commandFlag.getName(), context.flags().isPresent(commandFlag.getName()) ? NumberExpression.ofStatic(main, 1) : NumberExpression.ofStatic(main, 0));
                        }
                        booleanCondition.setAdditionalBooleanArguments(additionalBooleanArguments);


                        main.getConditionsManager().addCondition(booleanCondition, context, conditionFor);
                    })
            );


        }
        alreadyLoadedOnce = true;


    }

    public final String getOperator() {
        return operator;
    }

    public void setOperator(final String operator) {
        this.operator = operator;
    }

    public final String getVariableName() {
        return variableName;
    }

    public void setVariableName(final String variableName) {
        this.variableName = variableName;
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
            return main.getLanguageManager().getString("chat.conditions.boolean.variable-not-found", questPlayer.getPlayer(), questPlayer, Map.of(
                    "%VARIABLENAME%", variableName
            ));
        }

        final boolean booleanRequirement = numberExpression.calculateBooleanValue(questPlayer);


        if(additionalStringArguments != null && !additionalStringArguments.isEmpty()){
            cachedVariable.setAdditionalStringArguments(additionalStringArguments);
        }
        if(additionalNumberArguments != null && !additionalNumberArguments.isEmpty()){
            cachedVariable.setAdditionalNumberArguments(additionalNumberArguments);
        }
        if(additionalBooleanArguments != null && !additionalBooleanArguments.isEmpty()){
            cachedVariable.setAdditionalBooleanArguments(additionalBooleanArguments);
        }

        Object value = cachedVariable.getValue(questPlayer);

        if(getOperator().equalsIgnoreCase("equals")){
            if(value instanceof Boolean bool){
                if (booleanRequirement != bool) {
                    return main.getLanguageManager().getString("chat.conditions.boolean.not-fulfilled", questPlayer.getPlayer(), questPlayer, Map.of(
                            "%OPERATOR%", getOperator(),
                            "%BOOLEANREQUIREMENT%", String.valueOf(booleanRequirement),
                            "%VARIABLESINGULAR%", cachedVariable.getSingular(),
                            "%VARIABLEPLURAL%", cachedVariable.getPlural()
                    ));
                }
            }else{
                if (booleanRequirement != (boolean)value) {
                    return main.getLanguageManager().getString("chat.conditions.boolean.not-fulfilled", questPlayer.getPlayer(), questPlayer, Map.of(
                            "%OPERATOR%", getOperator(),
                            "%BOOLEANREQUIREMENT%", String.valueOf(booleanRequirement),
                            "%VARIABLESINGULAR%", cachedVariable.getSingular(),
                            "%VARIABLEPLURAL%", cachedVariable.getPlural()
                    ));
                }
            }
        } else if(getOperator().equalsIgnoreCase("or")){
            if(value instanceof Boolean bool){
                if (!booleanRequirement && !bool) {
                    return main.getLanguageManager().getString("chat.conditions.boolean.not-fulfilled", questPlayer.getPlayer(), questPlayer, Map.of(
                            "%OPERATOR%", getOperator(),
                            "%BOOLEANREQUIREMENT%", String.valueOf(false),
                            "%VARIABLESINGULAR%", cachedVariable.getSingular(),
                            "%VARIABLEPLURAL%", cachedVariable.getPlural()
                    ));
                }
            }else{
                if (!booleanRequirement && !(boolean)value) {
                    return main.getLanguageManager().getString("chat.conditions.boolean.not-fulfilled", questPlayer.getPlayer(), questPlayer, Map.of(
                            "%OPERATOR%", getOperator(),
                            "%BOOLEANREQUIREMENT%", String.valueOf(false),
                            "%VARIABLESINGULAR%", cachedVariable.getSingular(),
                            "%VARIABLEPLURAL%", cachedVariable.getPlural()
                    ));
                }
            }
        } else if(getOperator().equalsIgnoreCase("and")){
            if(value instanceof Boolean bool){
                if (!booleanRequirement || !bool) {
                    return main.getLanguageManager().getString("chat.conditions.boolean.not-fulfilled", questPlayer.getPlayer(), questPlayer, Map.of(
                            "%OPERATOR%", getOperator(),
                            "%BOOLEANREQUIREMENT%", String.valueOf(false),
                            "%VARIABLESINGULAR%", cachedVariable.getSingular(),
                            "%VARIABLEPLURAL%", cachedVariable.getPlural()
                    ));
                }
            }else{
                if (!booleanRequirement || !(boolean)value) {
                    return main.getLanguageManager().getString("chat.conditions.boolean.not-fulfilled", questPlayer.getPlayer(), questPlayer, Map.of(
                            "%OPERATOR%", getOperator(),
                            "%BOOLEANREQUIREMENT%", String.valueOf(false),
                            "%VARIABLESINGULAR%", cachedVariable.getSingular(),
                            "%VARIABLEPLURAL%", cachedVariable.getPlural()
                    ));
                }
            }
        }else{
            return main.getLanguageManager().getString("chat.conditions.boolean.wrong-operator", questPlayer.getPlayer(), questPlayer, Map.of(
                    "%OPERATOR%", getOperator()
            ));
        }

        return "";
    }

    @Override
    public void save(FileConfiguration configuration, final String initialPath) {
        configuration.set(initialPath + ".specifics.variableName", getVariableName());
        configuration.set(initialPath + ".specifics.operator", getOperator());
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
        this.operator = configuration.getString(initialPath + ".specifics.operator", "");
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

        this.operator = arguments.get(1);
        initializeExpressionAndCachedVariable(arguments.get(2), variableName);

        if(arguments.size() >= 4){

            Variable<?> variable = main.getVariablesManager().getVariableFromString(variableName);
            if(variable == null || !variable.isCanSetValue() || variable.getVariableDataType() != VariableDataType.BOOLEAN){
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
                        additionalNumberArguments.put(variable.getRequiredNumbers().get(counter - 4).getName(), new NumberExpression(main, argument));
                        counterNumbers++;
                    } else if(variable.getRequiredBooleans().size()  > counterBooleans){
                        additionalBooleanArguments.put(variable.getRequiredBooleans().get(counter - 4).getName(), new NumberExpression(main, argument));
                        counterBooleans++;
                    } else if(variable.getRequiredBooleanFlags().size()  > counterBooleanFlags){
                        additionalBooleanArguments.put(variable.getRequiredBooleanFlags().get(counter - 4).getName(), new NumberExpression(main, argument));
                        counterBooleanFlags++;
                    }
                }
            }
        }
    }

    @Override
    public String getConditionDescriptionInternally(QuestPlayer questPlayer, Object... objects) {
        //description += "\n<GRAY>--- Will quest points be deducted?: No";
        final boolean booleanRequirement = numberExpression.calculateBooleanValue(questPlayer);


        if (getOperator().equalsIgnoreCase("equals")) {
            return "<GRAY>-- " + variableName + " needs to be " + booleanRequirement + "</GRAY>";
        }
        return "<GRAY>-- " + variableName + " needed: " + booleanRequirement + "</GRAY>";
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

}
