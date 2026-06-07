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

import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

import static org.incendo.cloud.parser.standard.IntegerParser.integerParser;
import static org.incendo.cloud.parser.standard.StringParser.stringParser;
import static rocks.gravili.notquests.paper.commands.arguments.variables.ItemStackListVariableValueParser.itemStackListVariableParser;


public class ItemStackListAction extends Action {

    private static boolean alreadyLoadedOnce = false;
    private String variableName;
    private String operator;
    private HashMap<String, String> additionalStringArguments;
    private HashMap<String, NumberExpression> additionalNumberArguments;
    private HashMap<String, NumberExpression> additionalBooleanArguments;
    private ItemStack itemStack;

    public ItemStackListAction(final NotQuests main) {
        super(main);
        additionalStringArguments = new HashMap<>();
        additionalNumberArguments = new HashMap<>();
        additionalBooleanArguments = new HashMap<>();
    }

    public static void handleCommands(NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> builder, ActionFor actionFor) {
        for (String variableString : main.getVariablesManager().getVariableIdentifiers()) {

            Variable<?> variable = main.getVariablesManager().getVariableFromString(variableString);

            if (variable == null || !variable.isCanSetValue() || variable.getVariableDataType() != VariableDataType.ITEMSTACKLIST) {
                continue;
            }
            if (main.getVariablesManager().alreadyFullRegisteredVariables.contains(variableString)) {
                continue;
            }

            if (!alreadyLoadedOnce && main.getConfiguration().isVerboseStartupMessages()) {
                main.getLogManager().info("  Registering ItemStackList action: <highlight>" + variableString);
            }


            manager.command(main.getVariablesManager().registerVariableCommands(variableString, builder)
                    .required("operator", stringParser(), Description.of("Operator."), (context, lastString) -> {
                        ArrayList<Suggestion> completions = new ArrayList<>();
                        completions.add(Suggestion.suggestion("set"));
                        completions.add(Suggestion.suggestion("add"));
                        completions.add(Suggestion.suggestion("remove"));
                        completions.add(Suggestion.suggestion("clear"));
                        main.getUtilManager().sendFancyCommandCompletion(context.sender(), lastString.input().split(" "), "[Operator]", "[...]");

                        return CompletableFuture.completedFuture(completions);
                    })

                    .required("expression", itemStackListVariableParser("expression", variable), Description.of("Expression"))
                    .required("amount", integerParser(1), Description.of("Amount of items"))
                    .handler((context) -> {

                        final String expression = context.get("expression");
                        int amount = context.get("amount");

                        ItemStack itemStack;
                        if (expression.equalsIgnoreCase("hand")) {
                            if (context.sender() instanceof Player player) {
                                itemStack = player.getInventory().getItemInMainHand().clone();
                                itemStack.setAmount(amount);
                            } else {
                                context.sender().sendMessage(main.parse(
                                        "<error>This must be run by a player."
                                ));
                                return;
                            }
                        } else {
                            if (expression.equalsIgnoreCase("any")) {
                                context.sender().sendMessage(main.parse(
                                        "<error>You cannot use <highlight>'any'</highlight> here!"
                                ));
                                return;
                            }
                            itemStack = new ItemStack(Material.valueOf(expression.toUpperCase(Locale.ROOT)), amount);
                        }


                        final String operator = context.get("operator");

                        ItemStackListAction listAction = new ItemStackListAction(main);
                        listAction.setVariableName(variable.getVariableType());
                        listAction.setOperator(operator);
                        listAction.setItemStack(itemStack);


                        HashMap<String, String> additionalStringArguments = new HashMap<>();
                        for (StringVariableValueParser<CommandSender> stringParser : variable.getRequiredStrings()) {
                            additionalStringArguments.put(stringParser.getIdentifier(), context.get(stringParser.getIdentifier()));
                        }
                        listAction.setAdditionalStringArguments(additionalStringArguments);

                        HashMap<String, NumberExpression> additionalNumberArguments = new HashMap<>();
                        for (NumberVariableValueParser<CommandSender> numberParser : variable.getRequiredNumbers()) {
                            additionalNumberArguments.put(numberParser.getIdentifier(), new NumberExpression(main, context.get(numberParser.getIdentifier())));
                        }
                        listAction.setAdditionalNumberArguments(additionalNumberArguments);

                        HashMap<String, NumberExpression> additionalBooleanArguments = new HashMap<>();
                        for (BooleanVariableValueParser<CommandSender> booleanParser : variable.getRequiredBooleans()) {
                            additionalBooleanArguments.put(booleanParser.getIdentifier(), new NumberExpression(main, context.get(booleanParser.getIdentifier())));
                        }
                        for (CommandFlag<?> commandFlag : variable.getRequiredBooleanFlags()) {
                            additionalBooleanArguments.put(commandFlag.name(), context.flags().isPresent(commandFlag.name()) ? NumberExpression.ofStatic(main, 1) : NumberExpression.ofStatic(main, 0));
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

    public final String getVariableName() {
        return variableName;
    }

    public void setVariableName(final String variableName) {
        this.variableName = variableName;
    }

    public final ItemStack getItemStack() {
        return itemStack;
    }

    public void setItemStack(final ItemStack itemStack) {
        this.itemStack = itemStack;
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

    /*public final String[] evaluateExpression(final QuestPlayer questPlayer){
        return getNewValueExpression().split(",");
    }*/

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
        if (additionalNumberArguments != null && !additionalNumberArguments.isEmpty()) {
            variable.setAdditionalNumberArguments(additionalNumberArguments);
        }
        if (additionalBooleanArguments != null && !additionalBooleanArguments.isEmpty()) {
            variable.setAdditionalBooleanArguments(additionalBooleanArguments);
        }


        Object currentValueObject = variable.getValue(questPlayer, objects);

        if (currentValueObject == null) {
            main.sendMessage(questPlayer.getPlayer(), "Error executing " + getActionName() + " action: Current value object is null.");
            main.getLogManager().warn("Error executing " + getActionName() + " action: Current value object is null.");
            return;
        }

        ItemStack[] currentValue;
        if (currentValueObject instanceof ItemStack[] itemStackList) {
            currentValue = itemStackList;
        } else if (currentValueObject instanceof ArrayList<?> itemStackList) {
            currentValue = itemStackList.toArray(new ItemStack[0]);
        } else {
            currentValue = (ItemStack[]) currentValueObject;
        }

        if (currentValue == null) {
            main.sendMessage(questPlayer.getPlayer(), "Error executing " + getActionName() + " action: Current value is null.");
            main.getLogManager().warn("Error executing " + getActionName() + " action: Current value is null.");
            return;
        }

        ArrayList<ItemStack> currentValueArrayList = new ArrayList<>();

        for (ItemStack o : currentValue) {
            if (o != null) {
                currentValueArrayList.add(o);
            }
        }


        if (getItemStack() == null) {
            main.sendMessage(questPlayer.getPlayer(), "Error executing " + getActionName() + " action: New itemStack is null.");
            main.getLogManager().warn("Error executing " + getActionName() + " action: New itemStack is null.");
            return;
        }

        ArrayList<ItemStack> nextNewValueList = new ArrayList<>();
        ItemStack[] nextNewValue = null;

        if (getOperator().equalsIgnoreCase("set")) {
            variable.addAdditionalBooleanArgument("set", NumberExpression.ofStatic(main, 1));
            int amountLeft = getItemStack().getAmount();
            if (getItemStack().getAmount() > getItemStack().getMaxStackSize()) {
                while (amountLeft > 0) {
                    ItemStack clone = getItemStack().clone();
                    clone.setAmount(clone.getMaxStackSize());
                    nextNewValueList.add(clone);
                    amountLeft -= clone.getMaxStackSize();
                }
            } else {
                nextNewValueList.add(getItemStack());
            }
            nextNewValue = nextNewValueList.toArray(new ItemStack[nextNewValueList.size()]);
        } else if (getOperator().equalsIgnoreCase("add")) {
            variable.addAdditionalBooleanArgument("add", NumberExpression.ofStatic(main, 1));

            int amountLeft = getItemStack().getAmount();
            if (getItemStack().getAmount() > getItemStack().getMaxStackSize()) {
                while (amountLeft > 0) {
                    ItemStack clone = getItemStack().clone();
                    clone.setAmount(clone.getMaxStackSize());
                    nextNewValueList.add(clone);
                    amountLeft -= clone.getMaxStackSize();
                }
            } else {
                nextNewValueList.add(getItemStack());
            }

            /*nextNewValueList.addAll(currentValueArrayList);
            int amountLeft = getItemStack().getAmount();
            for(ItemStack itemStack : nextNewValueList){
                if(amountLeft == 0){
                    break;
                }
                if(itemStack.isSimilar(getItemStack())){
                    int whatCanBeAdded = itemStack.getMaxStackSize() - itemStack.getAmount();
                    if(whatCanBeAdded > 0){
                        if(amountLeft > whatCanBeAdded){
                            itemStack.setAmount(itemStack.getMaxStackSize());
                            amountLeft = whatCanBeAdded;
                        }else{
                            itemStack.setAmount(itemStack.getAmount() + amountLeft);
                            amountLeft = 0;
                        }
                    }
                }
            }
            if(amountLeft > 0){
                if(getItemStack().getAmount() > getItemStack().getMaxStackSize()){
                    while (amountLeft > 0){
                        ItemStack clone = getItemStack().clone();
                        clone.setAmount(clone.getMaxStackSize());
                        nextNewValueList.add(clone);
                        amountLeft -= clone.getMaxStackSize();
                    }
                }else{
                    nextNewValueList.add(getItemStack());
                }
            }
            player.sendMessage("S: " + nextNewValueList.toString());*/


            nextNewValue = nextNewValueList.toArray(new ItemStack[nextNewValueList.size()]);
        } else if (getOperator().equalsIgnoreCase("remove")) {
            int amountLeft = getItemStack().getAmount();
            if (getItemStack().getAmount() > getItemStack().getMaxStackSize()) {
                while (amountLeft > 0) {
                    ItemStack clone = getItemStack().clone();
                    clone.setAmount(clone.getMaxStackSize());
                    nextNewValueList.add(clone);
                    amountLeft -= clone.getMaxStackSize();
                }
            } else {
                nextNewValueList.add(getItemStack());
            }
            variable.addAdditionalBooleanArgument("remove", NumberExpression.ofStatic(main, 1));
            /* nextNewValueList.addAll(currentValueArrayList);
            nextNewValueList.removeAll(currentValueArrayList);*/

            nextNewValue = nextNewValueList.toArray(new ItemStack[nextNewValueList.size()]);
        } else if (getOperator().equalsIgnoreCase("clear")) {
            variable.addAdditionalBooleanArgument("clear", NumberExpression.ofStatic(main, 1));
            nextNewValue = new ItemStack[0];
        } else {
            main.sendMessage(questPlayer.getPlayer(), "<ERROR>Error: variable operator <highlight>" + getOperator() + "</highlight> is invalid. Report this to the Server owner.");
            return;
        }
        if (nextNewValue == null) {
            return;
        }

        questPlayer.sendDebugMessage("New Value: " + Arrays.toString(nextNewValue));


        if (currentValueObject instanceof ItemStack[]) {
            ((Variable<ItemStack[]>) variable).setValue(nextNewValue, questPlayer, objects);
        } else if (currentValueObject instanceof ArrayList<?>) {
            ((Variable<ArrayList<ItemStack>>) variable).setValue((ArrayList<ItemStack>) Arrays.asList(nextNewValue), questPlayer, objects);
        } else {
            main.getLogManager().warn("Cannot execute ItemStackList action, because the number type " + currentValueObject.getClass().getName() + " is invalid.");
        }

    }

    @Override
    public String getActionDescription(final QuestPlayer questPlayer, final Object... objects) {
        return variableName + ": " + main.getMiniMessage().serialize(getItemStack().displayName());
    }


    @Override
    public void save(final FileConfiguration configuration, String initialPath) {
        configuration.set(initialPath + ".specifics.itemStack", getItemStack());

        configuration.set(initialPath + ".specifics.variableName", getVariableName());
        configuration.set(initialPath + ".specifics.operator", getOperator());

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
    public void load(final FileConfiguration configuration, String initialPath) {
        this.itemStack = configuration.getItemStack(initialPath + ".specifics.itemStack", null);

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
        this.itemStack = new ItemStack(Material.valueOf(arguments.get(2).toUpperCase(Locale.ROOT)), Integer.parseInt(arguments.get(3)));

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
                        additionalStringArguments.put(variable.getRequiredStrings().get(counter - 5).getIdentifier(), argument);
                        counterStrings++;
                    } else if (variable.getRequiredNumbers().size() > counterNumbers) {
                        additionalNumberArguments.put(variable.getRequiredNumbers().get(counter - 5).getIdentifier(), new NumberExpression(main, argument));
                        counterNumbers++;
                    } else if (variable.getRequiredBooleans().size() > counterBooleans) {
                        additionalBooleanArguments.put(variable.getRequiredBooleans().get(counter - 5).getIdentifier(), new NumberExpression(main, argument));
                        counterBooleans++;
                    } else if (variable.getRequiredBooleanFlags().size() > counterBooleanFlags) {
                        additionalBooleanArguments.put(variable.getRequiredBooleanFlags().get(counter - 5).name(), new NumberExpression(main, argument));
                        counterBooleanFlags++;
                    }
                }
            }
        }


    }

}