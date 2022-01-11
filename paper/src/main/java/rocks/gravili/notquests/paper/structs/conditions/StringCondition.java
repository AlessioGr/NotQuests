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

package rocks.gravili.notquests.paper.structs.conditions;

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

public class StringCondition extends Condition {

    private String string;
    private String variableName;
    private String stringOperator;

    private HashMap<String, String> additionalStringArguments;


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


    public StringCondition(NotQuests main) {
        super(main);
        additionalStringArguments = new HashMap<>();
    }


    @Override
    public String checkInternally(final QuestPlayer questPlayer) {
        final String stringRequirement = getString();

        Variable<?> variable = main.getVariablesManager().getVariableFromString(variableName);

        if(variable == null){
            return "<ERROR>Error: variable </highlight>" + variableName + "<highlight> not found. Report this to the Server owner.";
        }

        if(additionalStringArguments != null && !additionalStringArguments.isEmpty()){
            variable.setAdditionalStringArguments(additionalStringArguments);
        }

        Object value = variable.getValue(questPlayer.getPlayer(), questPlayer);

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
            }else{
                return "<ERROR>Error: variable operator <highlight>" + getStringOperator() + "</highlight> is invalid. Report this to the Server owner.";
            }
        }else{
            return "<ERROR>Error: variable </highlight>" + variableName + "<highlight> is not a String.";
        }

        return "";
    }

    @Override
    public void save(FileConfiguration configuration, final String initialPath) {
        configuration.set(initialPath + ".specifics.variableName", getVariableName());
        configuration.set(initialPath + ".specifics.operator", getStringOperator());
        configuration.set(initialPath + ".specifics.string", getString());


        for(final String key : additionalStringArguments.keySet()){
            configuration.set(initialPath + ".specifics.additionalStrings." + key, additionalStringArguments.get(key));
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
    }

    @Override
    public void deserializeFromSingleLineString(ArrayList<String> arguments) {
        this.variableName = arguments.get(0);

        this.stringOperator = arguments.get(1);
        this.string = arguments.get(2).replace("__", " ");

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

    @Override
    public String getConditionDescription(Player player, Object... objects) {
        //description += "\n<GRAY>--- Will quest points be deducted?: No";

        if(getStringOperator().equalsIgnoreCase("equals")){
            return "<GRAY>-- " + variableName + " needs to be equal to " + getString() + "</GRAY>";
        }else if(getStringOperator().equalsIgnoreCase("equalsIgnoreCase")){
            return "<GRAY>-- " + variableName + " needs to be equal (case-insensitive) to " + getString() + "</GRAY>";
        }else if(getStringOperator().equalsIgnoreCase("contains")){
            return "<GRAY>-- " + variableName + " needs to contain " + getString() + "</GRAY>";
        }else if(getStringOperator().equalsIgnoreCase("startsWith")){
            return "<GRAY>-- " + variableName + " needs to start with " + getString() + "</GRAY>";
        }else if(getStringOperator().equalsIgnoreCase("endsWith")){
            return "<GRAY>-- " + variableName + " needs to end with " + getString() + "</GRAY>";
        }else if(getStringOperator().equalsIgnoreCase("isEmpty")){
            return "<GRAY>-- " + variableName + " needs to be empty</GRAY>";
        }

        return "<GRAY>-- Invalid String Operator";
    }


    public static void handleCommands(NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> builder, ConditionFor conditionFor) {
        for(String variableString : main.getVariablesManager().getVariableIdentifiers()){

            Variable<?> variable = main.getVariablesManager().getVariableFromString(variableString);

            if(variable == null || variable.getVariableDataType() != VariableDataType.STRING){
                continue;
            }

            main.getLogManager().info("Registering string condition: <highlight>" + variableString);


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
                        stringCondition.setVariableName(variableString);

                        stringCondition.setStringOperator(stringOperator);
                        stringCondition.setString(string);


                        HashMap<String, String> additionalStringArguments = new HashMap<>();
                        for(StringArgument<CommandSender> stringArgument : variable.getRequiredStrings()){
                            additionalStringArguments.put(stringArgument.getName(), context.get(stringArgument.getName()));
                        }
                        stringCondition.setAdditionalStringArguments(additionalStringArguments);

                        main.getConditionsManager().addCondition(stringCondition, context);
                    })
            );


        }


    }

    private void setString(final String string) {
        this.string = string;
    }

    public final String getString(){
        return string;
    }

    private void setAdditionalStringArguments(HashMap<String, String> additionalStringArguments) {
        this.additionalStringArguments = additionalStringArguments;
    }


}
