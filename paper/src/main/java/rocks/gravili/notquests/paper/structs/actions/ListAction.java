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

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.arguments.flags.CommandFlag;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.paper.PaperCommandManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.arguments.variables.BooleanVariableValueArgument;
import rocks.gravili.notquests.paper.commands.arguments.variables.ListVariableValueArgument;
import rocks.gravili.notquests.paper.commands.arguments.variables.NumberVariableValueArgument;
import rocks.gravili.notquests.paper.managers.expressions.NumberExpression;
import rocks.gravili.notquests.paper.structs.QuestPlayer;
import rocks.gravili.notquests.paper.structs.variables.Variable;
import rocks.gravili.notquests.paper.structs.variables.VariableDataType;


public class ListAction extends Action {

    private static boolean alreadyLoadedOnce = false;
    private String variableName;
    private String operator;
    private HashMap<String, String> additionalStringArguments;
    private HashMap<String, NumberExpression> additionalNumberArguments;
    private HashMap<String, NumberExpression> additionalBooleanArguments;
    private String newValueExpression;

    public ListAction(final NotQuests main) {
        super(main);
        additionalStringArguments = new HashMap<>();
        additionalNumberArguments = new HashMap<>();
        additionalBooleanArguments = new HashMap<>();
    }

    public static void handleCommands(NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> builder, ActionFor actionFor) {
        for(String variableString : main.getVariablesManager().getVariableIdentifiers()){

            Variable<?> variable = main.getVariablesManager().getVariableFromString(variableString);

            if(variable == null || !variable.isCanSetValue() || variable.getVariableDataType() != VariableDataType.LIST){
                continue;
            }
            if(main.getVariablesManager().alreadyFullRegisteredVariables.contains(variableString)){
                continue;
            }

            if (!alreadyLoadedOnce && main.getConfiguration().isVerboseStartupMessages()) {
                main.getLogManager().info("  Registering list action: <highlight>" + variableString);
            }


            manager.command(main.getVariablesManager().registerVariableCommands(variableString, builder)
                    .argument(StringArgument.<CommandSender>newBuilder("operator").withSuggestionsProvider((context, lastString) -> {
                        ArrayList<String> completions = new ArrayList<>();

                        completions.add("set");
                        completions.add("add");
                        completions.add("remove");
                        completions.add("clear");

                        final List<String> allArgs = context.getRawInput();
                        main.getUtilManager().sendFancyCommandCompletion(context.getSender(), allArgs.toArray(new String[0]), "[Operator]", "[...]");

                        return completions;
                    }).build(), ArgumentDescription.of("Operator."))
                    .argument(ListVariableValueArgument.newBuilder("expression", main, variable), ArgumentDescription.of("Expression"))
                    .handler((context) -> {

                        final String expression = context.get("expression");

                        final String operator = context.get("operator");

                        ListAction listAction = new ListAction(main);
                        listAction.setVariableName(variable.getVariableType());
                        listAction.setOperator(operator);
                        listAction.setNewValueExpression(expression);


                        HashMap<String, String> additionalStringArguments = new HashMap<>();
                        for(StringArgument<CommandSender> stringArgument : variable.getRequiredStrings()){
                            additionalStringArguments.put(stringArgument.getName(), context.get(stringArgument.getName()));
                        }
                        listAction.setAdditionalStringArguments(additionalStringArguments);

                        HashMap<String, NumberExpression> additionalNumberArguments = new HashMap<>();
                        for(NumberVariableValueArgument<CommandSender> numberVariableValueArgument : variable.getRequiredNumbers()){
                            additionalNumberArguments.put(numberVariableValueArgument.getName(), new NumberExpression(main, context.get(numberVariableValueArgument.getName())));
                        }
                        listAction.setAdditionalNumberArguments(additionalNumberArguments);

                        HashMap<String, NumberExpression> additionalBooleanArguments = new HashMap<>();
                        for(BooleanVariableValueArgument<CommandSender> booleanArgument : variable.getRequiredBooleans()){
                            additionalBooleanArguments.put(booleanArgument.getName(), new NumberExpression(main, context.get(booleanArgument.getName())));
                        }
                        for(CommandFlag<?> commandFlag : variable.getRequiredBooleanFlags()){
                            additionalBooleanArguments.put(commandFlag.getName(), context.flags().isPresent(commandFlag.getName()) ? NumberExpression.ofStatic(main, 1) : NumberExpression.ofStatic(main, 0));
                        }
                        listAction.setAdditionalBooleanArguments(additionalBooleanArguments);

                        main.getActionManager().addAction(listAction, context, actionFor);
                    })
            );
        }
        alreadyLoadedOnce = true;
    }

    public final String getOperator() {
        return operator;
    }

    public void setOperator(final String mathOperator) {
        this.operator = mathOperator;
    }

    public final String getVariableName(){
        return variableName;
    }

    public void setVariableName(final String variableName){
        this.variableName = variableName;
    }

    public final String getNewValueExpression(){
        return newValueExpression;
    }

    public void setNewValueExpression(final String newValueExpression){
        this.newValueExpression = newValueExpression;
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

    public final String[] evaluateExpression(final QuestPlayer questPlayer) {
        return getNewValueExpression().split(",");
    }

    @Override
    public void executeInternally(final QuestPlayer questPlayer, Object... objects) {
        Variable<?> variable = main.getVariablesManager().getVariableFromString(variableName);

        if (variable == null) {
            main.sendMessage(questPlayer.getPlayer(), "<ERROR>Error: variable <highlight>" + variableName + "</highlight> not found. Report this to the Server owner.");
            return;
        }

        if (additionalStringArguments != null && !additionalStringArguments.isEmpty()) {
            variable.setAdditionalStringArguments(additionalStringArguments);
        }
        if(additionalNumberArguments != null && !additionalNumberArguments.isEmpty()){
            variable.setAdditionalNumberArguments(additionalNumberArguments);
        }
        if(additionalBooleanArguments != null && !additionalBooleanArguments.isEmpty()){
            variable.setAdditionalBooleanArguments(additionalBooleanArguments);
        }


        Object currentValueObject = variable.getValue(questPlayer, questPlayer, objects);

        String[] currentValue;
        if (currentValueObject instanceof String[] stringList) {
            currentValue = stringList;
        }else if (currentValueObject instanceof ArrayList<?> stringList) {
            currentValue = stringList.toArray(new String[0]);
        }else{
            currentValue = (String[]) currentValueObject;
        }


        String[] nextNewValue = evaluateExpression(questPlayer);

        if(getOperator().equalsIgnoreCase("set")){
            nextNewValue = nextNewValue;
        }else if(getOperator().equalsIgnoreCase("add")){
            ArrayList<String> nextNewValueList = new ArrayList<>(Arrays.asList(nextNewValue));
            nextNewValueList.addAll(List.of(currentValue));

            nextNewValue = nextNewValueList.toArray(new String[0]);
        }else if(getOperator().equalsIgnoreCase("remove")){
            ArrayList<String> nextNewValueList = new ArrayList<>(Arrays.asList(currentValue));
            nextNewValueList.removeAll(List.of(nextNewValue));

            nextNewValue = nextNewValueList.toArray(new String[0]);
        }else if(getOperator().equalsIgnoreCase("clear")){
            nextNewValue = new String[0];
        }else{
            main.sendMessage(questPlayer.getPlayer(), "<ERROR>Error: variable operator <highlight>" + getOperator() + "</highlight> is invalid. Report this to the Server owner.");
            return;
        }

        questPlayer.sendDebugMessage("New Value: " + Arrays.toString(nextNewValue));



        if(currentValueObject instanceof String[]){
            ((Variable<String[]>) variable).setValue(nextNewValue, questPlayer, objects);
        }else if(currentValueObject instanceof ArrayList<?>){
            ((Variable<ArrayList<?>>) variable).setValue((ArrayList<?>) Arrays.asList(nextNewValue), questPlayer, objects);
        }else{
            main.getLogManager().warn("Cannot execute list action, because the number type " + currentValueObject.getClass().getName() + " is invalid.");
        }

    }

    @Override
    public String getActionDescription(final QuestPlayer questPlayer, final Object... objects) {
        return variableName + ": " + Arrays.toString(evaluateExpression(questPlayer));
    }

    @Override
    public void save(final FileConfiguration configuration, String initialPath) {
        configuration.set(initialPath + ".specifics.expression", getNewValueExpression());

        configuration.set(initialPath + ".specifics.variableName", getVariableName());
        configuration.set(initialPath + ".specifics.operator", getOperator());

        for(final String key : additionalStringArguments.keySet()){
            configuration.set(initialPath + ".specifics.additionalStrings." + key, additionalStringArguments.get(key));
        }
        for(final String key : additionalNumberArguments.keySet()){
            configuration.set(initialPath + ".specifics.additionalNumbers." + key, additionalNumberArguments.get(key).getRawExpression());
        }
        for(final String key : additionalBooleanArguments.keySet()){
            configuration.set(initialPath + ".specifics.additionalBooleans." + key, additionalBooleanArguments.get(key).getClass());
        }
    }


    @Override
    public void load(final FileConfiguration configuration, String initialPath) {
        this.newValueExpression = configuration.getString(initialPath + ".specifics.expression", "");

        this.variableName = configuration.getString(initialPath + ".specifics.variableName");
        this.operator = configuration.getString(initialPath + ".specifics.operator", "");

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
        this.newValueExpression = arguments.get(2);

        if(arguments.size() >= 4){

            Variable<?> variable = main.getVariablesManager().getVariableFromString(variableName);
            if(variable == null || !variable.isCanSetValue() || variable.getVariableDataType() != VariableDataType.LIST){
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

}