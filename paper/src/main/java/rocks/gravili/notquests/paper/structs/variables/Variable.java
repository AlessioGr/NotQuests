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

package rocks.gravili.notquests.paper.structs.variables;

import cloud.commandframework.arguments.flags.CommandFlag;
import cloud.commandframework.arguments.standard.StringArgument;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.arguments.variables.BooleanVariableValueArgument;
import rocks.gravili.notquests.paper.commands.arguments.variables.NumberVariableValueArgument;
import rocks.gravili.notquests.paper.managers.expressions.NumberExpression;
import rocks.gravili.notquests.paper.structs.ActiveObjective;
import rocks.gravili.notquests.paper.structs.ActiveQuest;
import rocks.gravili.notquests.paper.structs.QuestPlayer;
import rocks.gravili.notquests.paper.structs.conditions.*;
import rocks.gravili.notquests.paper.structs.objectives.ConditionObjective;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public abstract class Variable<T> {
    protected final NotQuests main;
    private final ArrayList<StringArgument<CommandSender>> requiredStrings;
    private final ArrayList<NumberVariableValueArgument<CommandSender>> requiredNumbers;
    private final ArrayList<BooleanVariableValueArgument<CommandSender>> requiredBooleans;
    private final ArrayList<CommandFlag<Void>> requiredBooleanFlags;

    private final ArrayList<String> setOnlyRequiredValues = new ArrayList<>(); //TODO: Implement
    private final ArrayList<String> getOnlyRequiredValues = new ArrayList<>(); //TODO: Implement

    private HashMap<String, String> additionalStringArguments;
    private HashMap<String, NumberExpression> additionalNumberArguments;
    private HashMap<String, NumberExpression> additionalBooleanArguments;

    private boolean canSetValue = false;

    private final VariableDataType variableDataType;


    public final ArrayList<String> getSetOnlyRequiredValues() {
        return setOnlyRequiredValues;
    }

    public final ArrayList<String> getGetOnlyRequiredValues() {
        return getOnlyRequiredValues;
    }

    public final HashMap<String, String> getAdditionalStringArguments() {
        return additionalStringArguments;
    }

    public final HashMap<String, NumberExpression> getAdditionalBooleanArguments() {
        return additionalBooleanArguments;
    }

    public void setAdditionalBooleanArguments(HashMap<String, NumberExpression> additionalBooleanArguments) {
        this.additionalBooleanArguments = additionalBooleanArguments;
    }


    public void addSetOnlyRequiredValue(final String value) {
        setOnlyRequiredValues.add(value);
    }

    public void addGetOnlyRequiredValue(final String value) {
        getOnlyRequiredValues.add(value);
    }


    public Variable(final NotQuests main){
        this.main = main;
        requiredStrings = new ArrayList<>();
        requiredNumbers = new ArrayList<>();
        requiredBooleans = new ArrayList<>();
        requiredBooleanFlags = new ArrayList<>();
        additionalStringArguments = new HashMap<>();
        additionalNumberArguments = new HashMap<>();
        additionalBooleanArguments = new HashMap<>();


        Class<T> typeOf = (Class<T>)
                ((ParameterizedType)getClass()
                        .getGenericSuperclass())
                        .getActualTypeArguments()[0];

        if(typeOf == String.class || typeOf == Character.class){
            variableDataType = VariableDataType.STRING;
        }else if(typeOf == Boolean.class){
            variableDataType = VariableDataType.BOOLEAN;
        }else if(typeOf == String[].class){
            variableDataType = VariableDataType.LIST;
        }else if(typeOf == ItemStack[].class){
            variableDataType = VariableDataType.ITEMSTACKLIST;
        }else if(typeOf == ArrayList.class){
            main.getLogManager().warn("Error: ArrayList variables are not supported yet. Using LIST variable...");
            variableDataType = VariableDataType.LIST;
        }else{
            variableDataType = VariableDataType.NUMBER;
        }
    }

    public final VariableDataType getVariableDataType(){
        return variableDataType;
    }

    protected void setCanSetValue(final boolean canSetValue){
        this.canSetValue = canSetValue;
    }

    public final boolean isCanSetValue(){
        return canSetValue;
    }

    protected void addRequiredString(final StringArgument<CommandSender> stringArgument){
        requiredStrings.add(stringArgument);
    }

    protected void addRequiredNumber(final NumberVariableValueArgument<CommandSender> numberVariableValueArgument){
        requiredNumbers.add(numberVariableValueArgument);
    }

    protected void addRequiredBoolean(final BooleanVariableValueArgument<CommandSender> booleanArgument){
        requiredBooleans.add(booleanArgument);
    }

    protected void addRequiredBooleanFlag(final CommandFlag<Void> commandFlag){
        requiredBooleanFlags.add(commandFlag);
    }

    public final ArrayList<StringArgument<CommandSender>> getRequiredStrings(){
        return requiredStrings;
    }

    public final ArrayList<NumberVariableValueArgument<CommandSender>> getRequiredNumbers(){
        return requiredNumbers;
    }

    public final ArrayList<BooleanVariableValueArgument<CommandSender>> getRequiredBooleans(){
        return requiredBooleans;
    }

    public final ArrayList<CommandFlag<Void>> getRequiredBooleanFlags() {
        return requiredBooleanFlags;
    }

    public final HashMap<String, NumberExpression> getAdditionalNumberArguments() {
        return additionalNumberArguments;
    }

    public void setAdditionalNumberArguments(HashMap<String, NumberExpression> additionalNumberArguments) {
        this.additionalNumberArguments = additionalNumberArguments;
    }

    protected final String getRequiredStringValue(final String key) {
        return additionalStringArguments.getOrDefault(key, "");
    }


    public abstract T getValue(final QuestPlayer questPlayer, final Object... objects);

    public boolean setValue(final T newValue, final QuestPlayer questPlayer, final Object... objects) {
        if (!isCanSetValue()) {
            return false;
        }
        boolean result = setValueInternally(newValue, questPlayer, objects);

        if (questPlayer != null && questPlayer.isHasActiveConditionObjectives()) {
            for (ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
                for (ActiveObjective activeObjective : activeQuest.getActiveObjectives()) {
                    if (activeObjective.getObjective() instanceof ConditionObjective conditionObjective) {
                        if (!activeObjective.isUnlocked()) {
                            continue;
                        }

                        Condition condition = conditionObjective.getCondition();
                        if(condition == null){
                            continue;
                        }
                        String activeObjectiveVariableName = "";
                        if(condition instanceof BooleanCondition booleanCondition){
                            activeObjectiveVariableName = booleanCondition.getVariableName();
                        }else if(condition instanceof ListCondition listCondition){
                            activeObjectiveVariableName = listCondition.getVariableName();
                        }else if(condition instanceof NumberCondition numberCondition){
                            activeObjectiveVariableName = numberCondition.getVariableName();
                        }else if(condition instanceof StringCondition stringCondition){
                            activeObjectiveVariableName = stringCondition.getVariableName();
                        }
                        if(activeObjectiveVariableName.equalsIgnoreCase(getVariableType())){
                            if (!condition.check(questPlayer).isBlank()) {
                                continue;
                            }

                            activeObjective.addProgress(1);
                        }
                    }
                }
                activeQuest.removeCompletedObjectives(true);
            }
            questPlayer.removeCompletedQuests();
        }


        return result;

    }

    public abstract boolean setValueInternally(final T newValue, final QuestPlayer questPlayer, final Object... objects);


    public abstract List<String> getPossibleValues(final QuestPlayer questPlayer, final Object... objects);

    public final String getVariableType() {
        return main.getVariablesManager().getVariableType(this.getClass());
    }

    public abstract String getPlural();

    public abstract String getSingular();

    public void setAdditionalStringArguments(HashMap<String, String> additionalStringArguments) {
        this.additionalStringArguments = additionalStringArguments;
    }

    protected final double getRequiredNumberValue(final String key, final QuestPlayer questPlayer) {
        return additionalNumberArguments.get(key).calculateValue(questPlayer);
    }

    protected final boolean getRequiredBooleanValue(final String key, final QuestPlayer questPlayer) {
        final NumberExpression numberExpression = additionalBooleanArguments.getOrDefault(key, null);
        if (numberExpression != null) {
            return additionalBooleanArguments.get(key).calculateBooleanValue(questPlayer);
        } else {
            return false;
        }
    }

    public void addAdditionalBooleanArgument(String key, NumberExpression value) {
        additionalBooleanArguments.put(key, value);
    }

    public void addAdditionalNumberArgument(String key, NumberExpression value) {
        additionalNumberArguments.put(key, value);
    }

    public void addAdditionalStringArgument(String key, String value) {
        additionalStringArguments.put(key, value);
    }

}
