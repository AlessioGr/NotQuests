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
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.paper.PaperCommandManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.arguments.variables.BooleanVariableValueArgument;
import rocks.gravili.notquests.paper.commands.arguments.variables.ItemStackListVariableValueArgument;
import rocks.gravili.notquests.paper.commands.arguments.variables.NumberVariableValueArgument;
import rocks.gravili.notquests.paper.managers.expressions.NumberExpression;
import rocks.gravili.notquests.paper.structs.QuestPlayer;
import rocks.gravili.notquests.paper.structs.variables.Variable;
import rocks.gravili.notquests.paper.structs.variables.VariableDataType;

public class ItemStackListCondition extends Condition {

    private static boolean alreadyLoadedOnce = false;
    private String variableName;
    private String operator;
    private ItemStack itemStack;
    private HashMap<String, String> additionalStringArguments;
    private HashMap<String, NumberExpression> additionalNumberArguments;
    private HashMap<String, NumberExpression> additionalBooleanArguments;


    public ItemStackListCondition(NotQuests main) {
        super(main);
        additionalStringArguments = new HashMap<>();
        additionalNumberArguments = new HashMap<>();
        additionalBooleanArguments = new HashMap<>();
    }

    public static void handleCommands(NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> builder, ConditionFor conditionFor) {
        for(String variableString : main.getVariablesManager().getVariableIdentifiers()){

            Variable<?> variable = main.getVariablesManager().getVariableFromString(variableString);

            if(variable == null || variable.getVariableDataType() != VariableDataType.ITEMSTACKLIST){
                continue;
            }
            if(main.getVariablesManager().alreadyFullRegisteredVariables.contains(variableString)){
                continue;
            }

            if (!alreadyLoadedOnce && main.getConfiguration().isVerboseStartupMessages()) {
                main.getLogManager().info("  Registering ItemStackList condition: <highlight>" + variableString);
            }

            manager.command(main.getVariablesManager().registerVariableCommands(variableString, builder)
                    .argument(StringArgument.<CommandSender>newBuilder("operator").withSuggestionsProvider((context, lastString) -> {
                        ArrayList<String> completions = new ArrayList<>();
                        completions.add("equals");
                        completions.add("contains");


                        final List<String> allArgs = context.getRawInput();
                        main.getUtilManager().sendFancyCommandCompletion(context.getSender(), allArgs.toArray(new String[0]), "[List Operator]", "[...]");

                        return completions;
                    }).build(), ArgumentDescription.of("List operator."))
                    .argument(ItemStackListVariableValueArgument.newBuilder("expression", main, variable), ArgumentDescription.of("Expression"))
                    .argument(IntegerArgument.<CommandSender>newBuilder("amount").withMin(1), ArgumentDescription.of("Amount of items"))
                    .handler((context) -> {

                        final String expression = context.get("expression");
                        int amount = context.get("amount");

                        ItemStack itemStack;
                        if (expression.equalsIgnoreCase("hand")) {
                            if (context.getSender() instanceof Player player) {
                                itemStack = player.getInventory().getItemInMainHand().clone();
                                itemStack.setAmount(amount);
                            } else {
                                context.getSender().sendMessage(main.parse(
                                        "<error>This must be run by a player."
                                ));
                                return;
                            }
                        } else {
                            if (expression.equalsIgnoreCase("any")) {
                                context.getSender().sendMessage(main.parse(
                                        "<error>You cannot use <highlight>'any'</highlight> here!"
                                ));
                                return;
                            }
                            itemStack = new ItemStack(Material.valueOf(expression.toUpperCase(Locale.ROOT)), amount);
                        }
                        final String operator = context.get("operator");

                        ItemStackListCondition listCondition = new ItemStackListCondition(main);

                        listCondition.setItemStack(itemStack);
                        listCondition.setOperator(operator);
                        listCondition.setVariableName(variable.getVariableType());


                        HashMap<String, String> additionalStringArguments = new HashMap<>();
                        for(StringArgument<CommandSender> stringArgument : variable.getRequiredStrings()){
                            additionalStringArguments.put(stringArgument.getName(), context.get(stringArgument.getName()));
                        }
                        listCondition.setAdditionalStringArguments(additionalStringArguments);

                        HashMap<String, NumberExpression> additionalNumberArguments = new HashMap<>();
                        for(NumberVariableValueArgument<CommandSender> numberVariableValueArgument : variable.getRequiredNumbers()){
                            additionalNumberArguments.put(numberVariableValueArgument.getName(), new NumberExpression(main, context.get(numberVariableValueArgument.getName())));
                        }
                        listCondition.setAdditionalNumberArguments(additionalNumberArguments);

                        HashMap<String, NumberExpression> additionalBooleanArguments = new HashMap<>();
                        for(BooleanVariableValueArgument<CommandSender> booleanArgument : variable.getRequiredBooleans()){
                            additionalBooleanArguments.put(booleanArgument.getName(), new NumberExpression(main, context.get(booleanArgument.getName())));
                        }
                        for(CommandFlag<?> commandFlag : variable.getRequiredBooleanFlags()){
                            additionalBooleanArguments.put(commandFlag.getName(), context.flags().isPresent(commandFlag.getName()) ? NumberExpression.ofStatic(main, 1) : NumberExpression.ofStatic(main, 0));
                        }
                        listCondition.setAdditionalBooleanArguments(additionalBooleanArguments);

                        main.getConditionsManager().addCondition(listCondition, context, conditionFor);
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

    public void setVariableName(final String variableName){
        this.variableName = variableName;
    }

    public final ItemStack getItemStack(){
        return itemStack;
    }


    /*public final String[] evaluateExpression(final QuestPlayer questPlayer){
        return getExpression().split(",");
    }*/

    public void setItemStack(final ItemStack itemStack){
        this.itemStack = itemStack;
    }

    @Override
    public String checkInternally(final QuestPlayer questPlayer) {
        ItemStack[] listRequirement = new ItemStack[1];
        listRequirement[0] = getItemStack();

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

        Object value = variable.getValue(questPlayer);

        if(value == null){
            return "<YELLOW>You don't have any " + variable.getPlural() + "!";
        }

        ItemStack[] itemStackArray;
        if(value instanceof ItemStack[] itemStackArray1){
            itemStackArray = itemStackArray1;
        }else if(value instanceof ArrayList<?> arrayList){
            itemStackArray = arrayList.toArray(new ItemStack[0]);
        }else{
            itemStackArray = (ItemStack[])value;
        }

        ArrayList<ItemStack> currentValueArrayList = new ArrayList<>();

        for (ItemStack o : itemStackArray){
            if (o != null) {
                currentValueArrayList.add(o);
            }
        }

        if(getOperator().equalsIgnoreCase("equals")){

            boolean fulfilled = true;
            int amountNeeded = getItemStack().getAmount();
            for(ItemStack itemStack : currentValueArrayList){
                if(!itemStack.isSimilar(getItemStack())){
                    fulfilled = false;
                    break;
                }else{
                    amountNeeded -= itemStack.getAmount();
                }
            }
            if(amountNeeded != 0){
                fulfilled = false;
            }


            if(!fulfilled){
                return "<YELLOW>The " + variable.getPlural() + " need to contain ONLY: <highlight>" + main.getMiniMessage().serialize(getItemStack().displayName()) + " x " + getItemStack().getAmount() + "</highlight>.";
            }
        }else if(getOperator().equalsIgnoreCase("contains")) {
            boolean fulfilled = false;
            int amountNeeded = getItemStack().getAmount();
            for(ItemStack itemStack : currentValueArrayList){
                if(itemStack.isSimilar(getItemStack())){
                    amountNeeded -= itemStack.getAmount();
                }
            }
            if(amountNeeded <= 0){
                fulfilled = true;
            }
            if(!fulfilled){
                return "<YELLOW>The " + variable.getPlural() + " need to contain: <highlight>" + main.getMiniMessage().serialize(getItemStack().displayName()) + " x " + getItemStack().getAmount() + "</highlight>.";
            }
        }else{
            return "<ERROR>Error: variable operator <highlight>" + getOperator() + "</highlight> is invalid. Report this to the Server owner.";
        }

        return "";
    }

    @Override
    public void save(FileConfiguration configuration, final String initialPath) {
        configuration.set(initialPath + ".specifics.variableName", getVariableName());
        configuration.set(initialPath + ".specifics.operator", getOperator());
        configuration.set(initialPath + ".specifics.itemStack", getItemStack());

        for (final String key : additionalStringArguments.keySet()) {
            configuration.set(initialPath + ".specifics.additionalStrings." + key, additionalStringArguments.get(key));
        }
        for (final String key : additionalNumberArguments.keySet()) {
            configuration.set(initialPath + ".specifics.additionalNumbers." + key, additionalNumberArguments.get(key).getRawExpression());
        }
        for (final String key : additionalBooleanArguments.keySet()) {
            configuration.set(initialPath + ".specifics.additionalBooleans." + key, additionalBooleanArguments.get(key).getRawExpression());
        }
    }

    @Override
    public void load(FileConfiguration configuration, String initialPath) {
        this.variableName = configuration.getString(initialPath + ".specifics.variableName");
        this.operator = configuration.getString(initialPath + ".specifics.operator", "");
        this.itemStack = configuration.getItemStack(initialPath + ".specifics.itemStack", null);

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

        if (getOperator().equalsIgnoreCase("equals")) {
            return "<GRAY>-- " + variableName + " needs to be equal " + main.getMiniMessage().serialize(getItemStack().displayName()) + "</GRAY>";
        } else if (getOperator().equalsIgnoreCase("contains")) {
            return "<GRAY>-- " + variableName + " needs to be contain " + main.getMiniMessage().serialize(getItemStack().displayName()) + "</GRAY>";
        }
        return "<GRAY>Error: invalid expression.</GRAY>";
    }

    @Override
    public void deserializeFromSingleLineString(ArrayList<String> arguments) {
        this.variableName = arguments.get(0);

        this.operator = arguments.get(1);
        setItemStack(new ItemStack(Material.valueOf(arguments.get(2).toUpperCase(Locale.ROOT)), Integer.parseInt(arguments.get(3))));

        if (arguments.size() >= 5) {

            Variable<?> variable = main.getVariablesManager().getVariableFromString(variableName);
            if (variable == null || !variable.isCanSetValue() || variable.getVariableDataType() != VariableDataType.ITEMSTACKLIST) {
                return;
            }

            int counter = 0;
            int counterStrings = 0;
            int counterNumbers = 0;
            int counterBooleans = 0;
            int counterBooleanFlags = 0;

            for (String argument : arguments) {
                counter++;
                if (counter >= 5) {
                    if (variable.getRequiredStrings().size() > counterStrings) {
                        additionalStringArguments.put(variable.getRequiredStrings().get(counter - 5).getName(), argument);
                        counterStrings++;
                    } else if (variable.getRequiredNumbers().size() > counterNumbers) {
                        additionalNumberArguments.put(variable.getRequiredNumbers().get(counter - 5).getName(), new NumberExpression(main, argument));
                        counterNumbers++;
                    } else if (variable.getRequiredBooleans().size() > counterBooleans) {
                        additionalBooleanArguments.put(variable.getRequiredBooleans().get(counter - 5).getName(), new NumberExpression(main, argument));
                        counterBooleans++;
                    } else if (variable.getRequiredBooleanFlags().size() > counterBooleanFlags) {
                        additionalBooleanArguments.put(variable.getRequiredBooleanFlags().get(counter - 5).getName(), new NumberExpression(main, argument));
                        counterBooleanFlags++;
                    }
                }
            }
        }
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
