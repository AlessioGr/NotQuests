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
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.paper.PaperCommandManager;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.arguments.NumberVariableValueArgument;
import rocks.gravili.notquests.paper.commands.arguments.VariableSelector;
import rocks.gravili.notquests.paper.structs.QuestPlayer;
import rocks.gravili.notquests.paper.structs.variables.Variable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class NumberCondition extends Condition {

    private String variableName;
    private String mathOperator;

    private HashMap<String, String> additionalStringArguments;


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


    public NumberCondition(NotQuests main) {
        super(main);
        additionalStringArguments = new HashMap<>();
    }


    @Override
    public String check(final QuestPlayer questPlayer, final boolean enforce) {
        final long numberRequirement = getProgressNeeded();

        Variable<?> variable = main.getVariablesManager().getVariableFromString(variableName);

        if(variable == null){
            return "<ERROR>Error: variable </highlight>" + variableName + "<highlight> not found. Report this to the Server owner.";
        }

        if(additionalStringArguments != null && !additionalStringArguments.isEmpty()){
            variable.setAdditionalStringArguments(additionalStringArguments);
        }

        Object value = variable.getValue(questPlayer.getPlayer(), questPlayer);

        if(getMathOperator().equalsIgnoreCase("moreThan")){
            if(value instanceof Long l){
                if (l <= numberRequirement) {
                    return "<YELLOW>You need <highlight>" + (numberRequirement+1 - l) + "</highlight> more " + variable.getPlural() + ".";
                }
            }else if(value instanceof Float f){
                if (f <= numberRequirement) {
                    return "<YELLOW>You need <highlight>" + (numberRequirement+1 - f) + "</highlight> more " + variable.getPlural() + ".";
                }
            }else if(value instanceof Double d){
                if (d <= numberRequirement) {
                    return "<YELLOW>You need <highlight>" + (numberRequirement+1 - d) + "</highlight> more " + variable.getPlural() + ".";
                }
            }else if(value instanceof Integer i){
                if (i <= numberRequirement) {
                    return "<YELLOW>You need <highlight>" + (numberRequirement+1 - i) + "</highlight> more " + variable.getPlural() + ".";
                }
            }else{
                if ((long)value <= numberRequirement) {
                    return "<YELLOW>You need <highlight>" + (numberRequirement+1 - (long)value) + "</highlight> more " + variable.getPlural() + ".";
                }
            }
        }else if(getMathOperator().equalsIgnoreCase("moreOrEqualThan")){
            if(value instanceof Long l){
                if (l < numberRequirement) {
                    return "<YELLOW>You need <highlight>" + (numberRequirement - l) + "</highlight> more " + variable.getPlural() + ".";
                }
            }else if(value instanceof Float f){
                if (f < numberRequirement) {
                    return "<YELLOW>You need <highlight>" + (numberRequirement - f) + "</highlight> more " + variable.getPlural() + ".";
                }
            }else if(value instanceof Double d){
                if (d < numberRequirement) {
                    return "<YELLOW>You need <highlight>" + (numberRequirement - d) + "</highlight> more " + variable.getPlural() + ".";
                }
            }else if(value instanceof Integer i){
                if (i < numberRequirement) {
                    return "<YELLOW>You need <highlight>" + (numberRequirement - i) + "</highlight> more " + variable.getPlural() + ".";
                }
            }else{
                if ((long)value < numberRequirement) {
                    return "<YELLOW>You need <highlight>" + (numberRequirement - (long)value) + "</highlight> more " + variable.getPlural() + ".";
                }
            }
        }else if(getMathOperator().equalsIgnoreCase("lessThan")){
            if(value instanceof Long l){
                if (l >= numberRequirement) {
                    return "<YELLOW>You have <highlight>" + (l+1 - numberRequirement) + "</highlight> too many " + variable.getPlural() + ".";
                }
            }else if(value instanceof Float f){
                if (f >= numberRequirement) {
                    return "<YELLOW>You have <highlight>" + (f+1 - numberRequirement) + "</highlight> too many " + variable.getPlural() + ".";
                }
            }else if(value instanceof Double d){
                if (d >= numberRequirement) {
                    return "<YELLOW>You have <highlight>" + (d+1 - numberRequirement) + "</highlight> too many " + variable.getPlural() + ".";
                }
            }else if(value instanceof Integer i){
                if (i >= numberRequirement) {
                    return "<YELLOW>You have <highlight>" + (i+1 - numberRequirement) + "</highlight> too many " + variable.getPlural() + ".";
                }
            }else{
                if ((long)value >= numberRequirement) {
                    return "<YELLOW>You have <highlight>" + ((long)value+1 - numberRequirement) + "</highlight> too many " + variable.getPlural() + ".";
                }
            }
        }else if(getMathOperator().equalsIgnoreCase("lessOrEqualThan")){
            if(value instanceof Long l){
                if (l > numberRequirement) {
                    return "<YELLOW>You have <highlight>" + (l - numberRequirement) + "</highlight> too many " + variable.getPlural() + ".";
                }
            }else if(value instanceof Float f){
                if (f > numberRequirement) {
                    return "<YELLOW>You have <highlight>" + (f - numberRequirement) + "</highlight> too many " + variable.getPlural() + ".";
                }
            }else if(value instanceof Double d){
                if (d > numberRequirement) {
                    return "<YELLOW>You have <highlight>" + (d - numberRequirement) + "</highlight> too many " + variable.getPlural() + ".";
                }
            }else if(value instanceof Integer i){
                if (i > numberRequirement) {
                    return "<YELLOW>You have <highlight>" + (i - numberRequirement) + "</highlight> too many " + variable.getPlural() + ".";
                }
            }else{
                if ((long)value >= numberRequirement) {
                    return "<YELLOW>You have <highlight>" + ((long)value - numberRequirement) + "</highlight> too many " + variable.getPlural() + ".";
                }
            }
        }else if(getMathOperator().equalsIgnoreCase("equals")){
            if(value instanceof Long l){
                if (l != numberRequirement) {
                    return "<YELLOW>You need EXACTLY <highlight>" + numberRequirement+ "</highlight> " + variable.getPlural() + " - no more or less.";
                }
            }else if(value instanceof Float f){
                if (f != numberRequirement) {
                    return "<YELLOW>You need EXACTLY <highlight>" + numberRequirement+ "</highlight> " + variable.getPlural() + " - no more or less.";
                }
            }else if(value instanceof Double d){
                if (d != numberRequirement) {
                    return "<YELLOW>You need EXACTLY <highlight>" + numberRequirement+ "</highlight> " + variable.getPlural() + " - no more or less.";
                }
            }else if(value instanceof Integer i){
                if (i != numberRequirement) {
                    return "<YELLOW>You need EXACTLY <highlight>" + numberRequirement+ "</highlight> " + variable.getPlural() + " - no more or less.";
                }
            }else{
                if ((long)value != numberRequirement) {
                    return "<YELLOW>You need EXACTLY <highlight>" + numberRequirement+ "</highlight> " + variable.getPlural() + " - no more or less.";
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

        for(final String key : additionalStringArguments.keySet()){
            configuration.set(initialPath + ".specifics.additionalStrings." + key, additionalStringArguments.get(key));
        }
    }

    @Override
    public void load(FileConfiguration configuration, String initialPath) {
        this.variableName = configuration.getString(initialPath + ".specifics.variableName");
        this.mathOperator = configuration.getString(initialPath + ".specifics.operator", "");

        final ConfigurationSection additionalStringsConfigurationSection = configuration.getConfigurationSection(initialPath + ".specifics.additionalStrings");
        if (additionalStringsConfigurationSection != null) {
            for (String key : additionalStringsConfigurationSection.getKeys(false)) {
                additionalStringArguments.put(key, configuration.getString(initialPath + ".specifics.additionalStrings." + key, ""));
            }
        }
    }

    @Override
    public String getConditionDescription() {
        //description += "\n<GRAY>--- Will quest points be deducted?: No";

        return "<GRAY>-- " + variableName + " needed: " + getProgressNeeded() + "</GRAY>";
    }


    public static void handleCommands(NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> builder, ConditionFor conditionFor) {
        for(String variableString : main.getVariablesManager().getVariableIdentifiers()){


            manager.command(main.getVariablesManager().registerVariableCommands(variableString, builder)
                    .argument(StringArgument.<CommandSender>newBuilder("operator").withSuggestionsProvider((context, lastString) -> {
                        ArrayList<String> completions = new ArrayList<>();

                        completions.add("equals");
                        completions.add("lessThan");
                        completions.add("moreThan");
                        completions.add("moreOrEqualThan");
                        completions.add("lessOrEqualThan");


                        final List<String> allArgs = context.getRawInput();
                        main.getUtilManager().sendFancyCommandCompletion(context.getSender(), allArgs.toArray(new String[0]), "[Math Comparison Operator]", "");

                        return completions;
                    }).build(), ArgumentDescription.of("Math operator."))
                    .argument(NumberVariableValueArgument.newBuilder("amount", main), ArgumentDescription.of("Amount"))
                    .meta(CommandMeta.DESCRIPTION, "Creates a new Number condition")
                    .handler((context) -> {

                        final int amount = context.get("amount");

                        final String mathOperator = context.get("operator");


                        NumberCondition numberCondition = new NumberCondition(main);
                        numberCondition.setVariableName(variableString);

                        numberCondition.setMathOperator(mathOperator);
                        //numberCondition.setProgressNeeded(variable.getValue());

                        numberCondition.setProgressNeeded(amount);
                        //questPointsCondition.setDeductQuestPoints(deductQuestPoints);


                        Variable<?> variable = main.getVariablesManager().getVariableFromString(variableString);

                        if(variable != null){
                            HashMap<String, String> additionalStringArguments = new HashMap<>();
                            for(StringArgument<CommandSender> stringArgument : variable.getRequiredStrings()){
                                additionalStringArguments.put(stringArgument.getName(), context.get(stringArgument.getName()));
                            }
                            numberCondition.setAdditionalStringArguments(additionalStringArguments);
                        }

                        main.getConditionsManager().addCondition(numberCondition, context);
                    })
            );
        }


    }

    private void setAdditionalStringArguments(HashMap<String, String> additionalStringArguments) {
        this.additionalStringArguments = additionalStringArguments;
    }


}
