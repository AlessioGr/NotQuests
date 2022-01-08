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
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.paper.PaperCommandManager;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.arguments.NumberVariableValueArgument;
import rocks.gravili.notquests.paper.structs.QuestPlayer;
import rocks.gravili.notquests.paper.structs.conditions.NumberCondition;
import rocks.gravili.notquests.paper.structs.variables.Variable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class NumberAction extends Action {

    private String variableName;
    private String mathOperator;

    private HashMap<String, String> additionalStringArguments;

    private long newValue;

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

    public void setNewValue(final int newValue){
        this.newValue = newValue;
    }

    public final long getNewValue(){
        return newValue;
    }

    private void setAdditionalStringArguments(HashMap<String, String> additionalStringArguments) {
        this.additionalStringArguments = additionalStringArguments;
    }



    public NumberAction(final NotQuests main) {
        super(main);
        additionalStringArguments = new HashMap<>();
    }

    public static void handleCommands(NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> builder, ActionFor rewardFor) {


        for(String variableString : main.getVariablesManager().getVariableIdentifiers()){

            Variable<?> variable = main.getVariablesManager().getVariableFromString(variableString);

            if(variable == null || !variable.isCanSetValue()){
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
                    .argument(NumberVariableValueArgument.newBuilder("amount", main), ArgumentDescription.of("Amount"))
                    .meta(CommandMeta.DESCRIPTION, "Creates a new Number action")
                    .handler((context) -> {

                        final int amount = context.get("amount");

                        final String mathOperator = context.get("operator");

                        NumberAction numberAction = new NumberAction(main);
                        numberAction.setVariableName(variableString);
                        numberAction.setMathOperator(mathOperator);
                        numberAction.setNewValue(amount);


                        if(variable != null){
                            HashMap<String, String> additionalStringArguments = new HashMap<>();
                            for(StringArgument<CommandSender> stringArgument : variable.getRequiredStrings()){
                                additionalStringArguments.put(stringArgument.getName(), context.get(stringArgument.getName()));
                            }
                            numberAction.setAdditionalStringArguments(additionalStringArguments);
                        }

                        main.getActionManager().addAction(numberAction, context);

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

        Object currentValue = variable.getValue(player, questPlayer, objects);

        double nextNewValue = newValue;

        if(getMathOperator().equalsIgnoreCase("set")){
            nextNewValue = (double)newValue;
        }else if(getMathOperator().equalsIgnoreCase("add")){
            nextNewValue = (double)currentValue + newValue;
        }else if(getMathOperator().equalsIgnoreCase("deduct")){
            nextNewValue = (double)currentValue - newValue;
        }else if(getMathOperator().equalsIgnoreCase("multiply")){
            nextNewValue = (double)currentValue * newValue;
        }else if(getMathOperator().equalsIgnoreCase("divide")){
            if(newValue == 0){
                main.sendMessage(player, "<ERROR>Error: variable operator <highlight>" + getMathOperator() + "</highlight> cannot be used, because you cannot divide by 0. Report this to the Server owner.");
                return;
            }
            nextNewValue = (double)currentValue / newValue;
        }else{
            main.sendMessage(player, "<ERROR>Error: variable operator <highlight>" + getMathOperator() + "</highlight> is invalid. Report this to the Server owner.");
            return;
        }

        questPlayer.sendDebugMessage("New Value: " + nextNewValue);


        if(currentValue instanceof Long){
            ((Variable<Long>)variable).setValue(Double.valueOf(nextNewValue).longValue(), player, objects);
        } else if(currentValue instanceof Float){
            ((Variable<Float>)variable).setValue(Double.valueOf(nextNewValue).floatValue(), player, objects);
        } else if(currentValue instanceof Double){
            ((Variable<Double>)variable).setValue(nextNewValue, player, objects);
        } else if(currentValue instanceof Integer){
            ((Variable<Integer>)variable).setValue(Double.valueOf(nextNewValue).intValue(), player, objects);
        }else{
            main.getLogManager().warn("Cannot execute number action, because the number type " + currentValue.getClass().getName() + " is invalid.");
        }

    }

    @Override
    public String getActionDescription() {
        return variableName + ": " + getNewValue();
    }

    @Override
    public void save(final FileConfiguration configuration, String initialPath) {
        configuration.set(initialPath + ".specifics.newValue", getNewValue());

        configuration.set(initialPath + ".specifics.variableName", getVariableName());
        configuration.set(initialPath + ".specifics.operator", getMathOperator());

        for(final String key : additionalStringArguments.keySet()){
            configuration.set(initialPath + ".specifics.additionalStrings." + key, additionalStringArguments.get(key));
        }
    }


    @Override
    public void load(final FileConfiguration configuration, String initialPath) {
        this.newValue = configuration.getLong(initialPath + ".specifics.newValue");

        this.variableName = configuration.getString(initialPath + ".specifics.variableName");
        this.mathOperator = configuration.getString(initialPath + ".specifics.operator", "");

        final ConfigurationSection additionalStringsConfigurationSection = configuration.getConfigurationSection(initialPath + ".specifics.additionalStrings");
        if (additionalStringsConfigurationSection != null) {
            for (String key : additionalStringsConfigurationSection.getKeys(false)) {
                additionalStringArguments.put(key, configuration.getString(initialPath + ".specifics.additionalStrings." + key, ""));
            }
        }
    }

}