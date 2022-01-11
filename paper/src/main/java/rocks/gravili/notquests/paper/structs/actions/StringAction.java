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
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.paper.PaperCommandManager;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.arguments.variables.StringVariableValueArgument;
import rocks.gravili.notquests.paper.structs.QuestPlayer;
import rocks.gravili.notquests.paper.structs.variables.Variable;
import rocks.gravili.notquests.paper.structs.variables.VariableDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class StringAction extends Action {

    private String variableName;
    private String stringOperator;

    private HashMap<String, String> additionalStringArguments;

    private String newValue;

    public final String getStringOperator(){
        return stringOperator;
    }
    public void setStringOperator(final String stringOperator){
        this.stringOperator = stringOperator;
    }

    public final String getVariableName(){
        return variableName;
    }

    public void setVariableName(final String variableName){
        this.variableName = variableName;
    }

    public void setNewValue(final String newValue){
        this.newValue = newValue;
    }

    public final String getNewValue(){
        return newValue;
    }

    private void setAdditionalStringArguments(HashMap<String, String> additionalStringArguments) {
        this.additionalStringArguments = additionalStringArguments;
    }



    public StringAction(final NotQuests main) {
        super(main);
        additionalStringArguments = new HashMap<>();
    }

    public static void handleCommands(NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> builder, ActionFor rewardFor) {

        for(String variableString : main.getVariablesManager().getVariableIdentifiers()){

            Variable<?> variable = main.getVariablesManager().getVariableFromString(variableString);

            if(variable == null || !variable.isCanSetValue() || variable.getVariableDataType() != VariableDataType.STRING){
                continue;
            }

            manager.command(main.getVariablesManager().registerVariableCommands(variableString, builder)
                    .argument(StringArgument.<CommandSender>newBuilder("operator").withSuggestionsProvider((context, lastString) -> {
                        ArrayList<String> completions = new ArrayList<>();

                        completions.add("set");
                        completions.add("append");


                        final List<String> allArgs = context.getRawInput();
                        main.getUtilManager().sendFancyCommandCompletion(context.getSender(), allArgs.toArray(new String[0]), "[String Comparison Operator]", "[...]");

                        return completions;
                    }).build(), ArgumentDescription.of("String operator."))
                    .argument(StringVariableValueArgument.newBuilder("string", main, variable), ArgumentDescription.of("String"))
                    .handler((context) -> {

                        final String string = context.get("string");

                        final String stringOperator = context.get("operator");

                        StringAction stringAction = new StringAction(main);

                        stringAction.setVariableName(variableString);
                        stringAction.setStringOperator(stringOperator);
                        stringAction.setNewValue(string);


                        HashMap<String, String> additionalStringArguments = new HashMap<>();
                        for(StringArgument<CommandSender> stringArgument : variable.getRequiredStrings()){
                            additionalStringArguments.put(stringArgument.getName(), context.get(stringArgument.getName()));
                        }
                        stringAction.setAdditionalStringArguments(additionalStringArguments);

                        main.getActionManager().addAction(stringAction, context);

                    })
            );
        }
    }

    @Override
    public void execute(final Player player, Object... objects) {
        Variable<?> variable = main.getVariablesManager().getVariableFromString(variableName);

        if(variable == null){
            main.sendMessage(player, "<ERROR>Error: variable </highlight>" + variableName + "<highlight> not found. Report this to the Server owner.");
            return;
        }

        if(additionalStringArguments != null && !additionalStringArguments.isEmpty()){
            variable.setAdditionalStringArguments(additionalStringArguments);
        }

        QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId());

        Object currentValueObject = variable.getValue(player, questPlayer, objects);

        String currentValue = "";
        if (currentValueObject instanceof String string) {
            currentValue = string;
        }else{
            currentValue = (String) currentValueObject;
        }

        String nextNewValue = newValue;

        if(getStringOperator().equalsIgnoreCase("set")){
            nextNewValue = newValue;
        }else if(getStringOperator().equalsIgnoreCase("append")){
            nextNewValue = currentValue + newValue;
        }else{
            main.sendMessage(player, "<ERROR>Error: variable operator <highlight>" + getStringOperator() + "</highlight> is invalid. Report this to the Server owner.");
            return;
        }

        questPlayer.sendDebugMessage("New Value: " + nextNewValue);



        if(currentValueObject instanceof String){
            ((Variable<String>)variable).setValue(nextNewValue, player, objects);
        } else if(currentValueObject instanceof Character){
            ((Variable<Character>)variable).setValue(nextNewValue.toCharArray()[0], player, objects);
        }else{
            main.getLogManager().warn("Cannot execute string action, string the number type " + currentValueObject.getClass().getName() + " is invalid.");
        }

    }

    @Override
    public String getActionDescription(final Player player, final Object... objects) {
        return variableName + ": " + getNewValue();
    }

    @Override
    public void save(final FileConfiguration configuration, String initialPath) {
        configuration.set(initialPath + ".specifics.newValue", getNewValue());

        configuration.set(initialPath + ".specifics.variableName", getVariableName());
        configuration.set(initialPath + ".specifics.operator", getStringOperator());

        for(final String key : additionalStringArguments.keySet()){
            configuration.set(initialPath + ".specifics.additionalStrings." + key, additionalStringArguments.get(key));
        }
    }


    @Override
    public void load(final FileConfiguration configuration, String initialPath) {
        this.newValue = configuration.getString(initialPath + ".specifics.newValue");

        this.variableName = configuration.getString(initialPath + ".specifics.variableName");
        this.stringOperator = configuration.getString(initialPath + ".specifics.operator", "");

        final ConfigurationSection additionalStringsConfigurationSection = configuration.getConfigurationSection(initialPath + ".specifics.additionalStrings");
        if (additionalStringsConfigurationSection != null) {
            for (String key : additionalStringsConfigurationSection.getKeys(false)) {
                additionalStringArguments.put(key, configuration.getString(initialPath + ".specifics.additionalStrings." + key, ""));
            }
        }
    }

    @Override
    public void deserializeFromSingleLineString(ArrayList<String> arguments) {
        this.variableName = arguments.get(0);

        this.stringOperator = arguments.get(1);
        this.newValue = arguments.get(2);

        if(arguments.size() >= 4){

            Variable<?> variable = main.getVariablesManager().getVariableFromString(variableName);
            if(variable == null || !variable.isCanSetValue() || variable.getVariableDataType() != VariableDataType.STRING){
                return;
            }

            int counter = 0;
            for (String argument : arguments){
                counter++;
                if(counter >= 4){
                    additionalStringArguments.put(variable.getRequiredStrings().get(counter-4).getName(), argument);
                }
            }
        }
    }

}