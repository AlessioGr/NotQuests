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
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.arguments.variables.BooleanVariableValueArgument;
import rocks.gravili.notquests.paper.commands.arguments.variables.NumberVariableValueArgument;
import rocks.gravili.notquests.paper.commands.arguments.variables.StringVariableValueArgument;
import rocks.gravili.notquests.paper.managers.expressions.NumberExpression;
import rocks.gravili.notquests.paper.structs.QuestPlayer;
import rocks.gravili.notquests.paper.structs.variables.Variable;
import rocks.gravili.notquests.paper.structs.variables.VariableDataType;

public class StringCondition extends Condition {

    private static boolean alreadyLoadedOnce = false;
    private String string;
    private String variableName;
    private String stringOperator;
    private HashMap<String, String> additionalStringArguments;
    private HashMap<String, NumberExpression> additionalNumberArguments;
    private HashMap<String, NumberExpression> additionalBooleanArguments;


    public StringCondition(NotQuests main) {
        super(main);
        additionalStringArguments = new HashMap<>();
        additionalNumberArguments = new HashMap<>();
        additionalBooleanArguments = new HashMap<>();
    }

    public static void handleCommands(NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> builder, ConditionFor conditionFor) {
        for (String variableString : main.getVariablesManager().getVariableIdentifiers()) {

            Variable<?> variable = main.getVariablesManager().getVariableFromString(variableString);

            if (variable == null || variable.getVariableDataType() != VariableDataType.STRING) {
                continue;
            }
            if (main.getVariablesManager().alreadyFullRegisteredVariables.contains(variableString)) {
                continue;
            }

            if (!alreadyLoadedOnce && main.getConfiguration().isVerboseStartupMessages()) {
                main.getLogManager().info("  Registering string condition: <highlight>" + variableString);
            }


            manager.command(main.getVariablesManager().registerVariableCommands(variableString, builder)
                    .argument(StringArgument.<CommandSender>newBuilder("operator").withSuggestionsProvider((context, lastString) -> {
                        ArrayList<String> completions = new ArrayList<>();
                        completions.add("equals");
                        completions.add("equalsIgnoreCase");
                        completions.add("contains");
                        completions.add("startsWith");
                        completions.add("endsWith");
                        completions.add("isEmpty");

                        final List<String> allArgs = context.getRawInput();
                        main.getUtilManager().sendFancyCommandCompletion(context.getSender(), allArgs.toArray(new String[0]), "[String Comparison Operator]", "[...]");

                        return completions;
                    }).build(), ArgumentDescription.of("String operator."))
                    .argument(StringVariableValueArgument.newBuilder("string", main, variable), ArgumentDescription.of("String"))
                    .handler((context) -> {

                        String string = context.get("string");
                        string = string.replace("__", " ");

                        final String stringOperator = context.get("operator");

                        StringCondition stringCondition = new StringCondition(main);
                        stringCondition.setVariableName(variable.getVariableType());

                        stringCondition.setStringOperator(stringOperator);
                        stringCondition.setString(string);


                        HashMap<String, String> additionalStringArguments = new HashMap<>();
                        for (StringArgument<CommandSender> stringArgument : variable.getRequiredStrings()) {
                            additionalStringArguments.put(stringArgument.getName(), context.get(stringArgument.getName()));
                        }
                        stringCondition.setAdditionalStringArguments(additionalStringArguments);

                        HashMap<String, NumberExpression> additionalNumberArguments = new HashMap<>();
                        for (NumberVariableValueArgument<CommandSender> numberVariableValueArgument : variable.getRequiredNumbers()) {
                            additionalNumberArguments.put(numberVariableValueArgument.getName(), new NumberExpression(main, context.get(numberVariableValueArgument.getName())));
                        }
                        stringCondition.setAdditionalNumberArguments(additionalNumberArguments);

                        HashMap<String, NumberExpression> additionalBooleanArguments = new HashMap<>();
                        for (BooleanVariableValueArgument<CommandSender> booleanArgument : variable.getRequiredBooleans()) {
                            additionalBooleanArguments.put(booleanArgument.getName(), new NumberExpression(main, context.get(booleanArgument.getName())));
                        }
                        for (CommandFlag<?> commandFlag : variable.getRequiredBooleanFlags()) {
                            additionalBooleanArguments.put(commandFlag.getName(), context.flags().isPresent(commandFlag.getName()) ? NumberExpression.ofStatic(main, 1) : NumberExpression.ofStatic(main, 0));
                        }
                        stringCondition.setAdditionalBooleanArguments(additionalBooleanArguments);

                        main.getConditionsManager().addCondition(stringCondition, context, conditionFor);
                    })
            );


        }
        alreadyLoadedOnce = true;


    }

    public final String getStringOperator() {
        return stringOperator;
    }

    public void setStringOperator(final String stringOperator) {
        this.stringOperator = stringOperator;
    }

    public final String getVariableName() {
        return variableName;
    }

    public void setVariableName(final String variableName){
        this.variableName = variableName;
    }

    @Override
    public String checkInternally(final QuestPlayer questPlayer) {
        final String stringRequirement = getString();

        Variable<?> variable = main.getVariablesManager().getVariableFromString(variableName);

        if(variable == null){
            return "<ERROR>Error: variable <highlight>" + variableName + "</highlight> not found. Report this to the Server owner.";
        }

        if(additionalStringArguments != null && !additionalStringArguments.isEmpty()){
            variable.setAdditionalStringArguments(additionalStringArguments);
        }
        if(additionalNumberArguments != null && !additionalNumberArguments.isEmpty()){
            variable.setAdditionalNumberArguments(additionalNumberArguments);
        }
        if(additionalBooleanArguments != null && !additionalBooleanArguments.isEmpty()){
            variable.setAdditionalBooleanArguments(additionalBooleanArguments);
        }

        Object value = variable.getValue(questPlayer, questPlayer);

        if(value instanceof final String stringValue){
            if(getStringOperator().equalsIgnoreCase("equals")){
                if(!stringValue.equals(stringRequirement)){
                    return "<YELLOW><highlight>" + variable.getSingular() + "</highlight> needs to equal " + stringRequirement + ".";
                }
            }else if(getStringOperator().equalsIgnoreCase("equalsIgnoreCase")){
                if(!stringValue.equalsIgnoreCase(stringRequirement)){
                    return "<YELLOW><highlight>" + variable.getSingular() + "</highlight> needs to equal (case-insensitive) " + stringRequirement + ".";
                }
            }else if(getStringOperator().equalsIgnoreCase("contains")){
                if(!stringValue.contains(stringRequirement)){
                    return "<YELLOW><highlight>" + variable.getSingular() + "</highlight> needs to contain " + stringRequirement + ".";
                }
            }else if(getStringOperator().equalsIgnoreCase("startsWith")){
                if(!stringValue.startsWith(stringRequirement)){
                    return "<YELLOW><highlight>" + variable.getSingular() + "</highlight> needs to start with " + stringRequirement + ".";
                }
            }else if(getStringOperator().equalsIgnoreCase("endsWith")){
                if(!stringValue.endsWith(stringRequirement)){
                    return "<YELLOW><highlight>" + variable.getSingular() + "</highlight> needs to end with " + stringRequirement + ".";
                }
            }else if(getStringOperator().equalsIgnoreCase("isEmpty")){
                if(!stringValue.isBlank()){
                    return "<YELLOW><highlight>" + variable.getSingular() + "</highlight> needs to be empty.";
                }
            } else {
                return "<ERROR>Error: variable operator <highlight>" + getStringOperator() + "</highlight> is invalid. Report this to the Server owner.";
            }
        } else {
            return "<ERROR>Error: variable <highlight>" + variableName + "</highlight> is not a String.";
        }

        return "";
    }

    @Override
    public void save(FileConfiguration configuration, final String initialPath) {
        configuration.set(initialPath + ".specifics.variableName", getVariableName());
        configuration.set(initialPath + ".specifics.operator", getStringOperator());
        configuration.set(initialPath + ".specifics.string", getString());


        for (final String key : additionalStringArguments.keySet()) {
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
        this.stringOperator = configuration.getString(initialPath + ".specifics.operator", "");
        this.string = configuration.getString(initialPath + ".specifics.string", "").replace("__", " ");

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
    public String getConditionDescriptionInternally(QuestPlayer questPlayer, Object... objects) {
        //description += "\n<GRAY>--- Will quest points be deducted?: No";

        if (getStringOperator().equalsIgnoreCase("equals")) {
            return "<GRAY>-- " + variableName + " needs to be equal to " + getString() + "</GRAY>";
        } else if (getStringOperator().equalsIgnoreCase("equalsIgnoreCase")) {
            return "<GRAY>-- " + variableName + " needs to be equal (case-insensitive) to " + getString() + "</GRAY>";
        } else if (getStringOperator().equalsIgnoreCase("contains")) {
            return "<GRAY>-- " + variableName + " needs to contain " + getString() + "</GRAY>";
        } else if (getStringOperator().equalsIgnoreCase("startsWith")) {
            return "<GRAY>-- " + variableName + " needs to start with " + getString() + "</GRAY>";
        } else if (getStringOperator().equalsIgnoreCase("endsWith")) {
            return "<GRAY>-- " + variableName + " needs to end with " + getString() + "</GRAY>";
        } else if (getStringOperator().equalsIgnoreCase("isEmpty")) {
            return "<GRAY>-- " + variableName + " needs to be empty</GRAY>";
        }

        return "<GRAY>-- Invalid String Operator";
    }

    @Override
    public void deserializeFromSingleLineString(ArrayList<String> arguments) {
        this.variableName = arguments.get(0);

        this.stringOperator = arguments.get(1);
        this.string = arguments.get(2).replace("__", " ");

        if (arguments.size() >= 4) {

            Variable<?> variable = main.getVariablesManager().getVariableFromString(variableName);
            if(variable == null || !variable.isCanSetValue() || variable.getVariableDataType() != VariableDataType.STRING){
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

    public final String getString() {
        return string;
    }

    private void setString(final String string) {
        this.string = string;
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
