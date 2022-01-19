package rocks.gravili.notquests.paper.structs.variables;

import cloud.commandframework.arguments.flags.CommandFlag;
import cloud.commandframework.arguments.standard.BooleanArgument;
import cloud.commandframework.arguments.standard.StringArgument;
import com.sun.jna.platform.unix.solaris.LibKstat;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.arguments.variables.NumberVariableValueArgument;
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
    private final ArrayList<BooleanArgument<CommandSender>> requiredBooleans;
    private final ArrayList<CommandFlag<Void>> requiredBooleanFlags;


    private HashMap<String, String> additionalStringArguments;
    private HashMap<String, String> additionalNumberArguments; //Second string is an expression
    private HashMap<String, Boolean> additionalBooleanArguments;

    private boolean canSetValue = false;

    private final VariableDataType variableDataType;

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
    protected void addRequiredBoolean(final BooleanArgument<CommandSender> booleanArgument){
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
    public final ArrayList<BooleanArgument<CommandSender>> getRequiredBooleans(){
        return requiredBooleans;
    }
    public final ArrayList<CommandFlag<Void>> getRequiredBooleanFlags(){
        return requiredBooleanFlags;
    }


    protected final String getRequiredStringValue(String key){
        return additionalStringArguments.get(key);
    }

    protected final double getRequiredNumberValue(String key, Player player){
        return main.getVariablesManager().evaluateExpression(additionalNumberArguments.get(key), player);
    }
    protected final boolean getRequiredBooleanValue(String key){
        return additionalBooleanArguments.getOrDefault(key, false);
    }

    public abstract T getValue(final Player player, final Object... objects);

    public boolean setValue(final T newValue, final Player player, final Object... objects){
        if(!isCanSetValue()){
            return false;
        }
        boolean result = setValueInternally(newValue, player, objects);

        final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId());
        if(questPlayer != null && questPlayer.isHasActiveConditionObjectives()) {
            for(ActiveQuest activeQuest : questPlayer.getActiveQuests()){
                for(ActiveObjective activeObjective : activeQuest.getActiveObjectives()){
                    if(activeObjective.getObjective() instanceof ConditionObjective conditionObjective){
                        if(!activeObjective.isUnlocked()){
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

    public abstract boolean setValueInternally(final T newValue, final Player player, final Object... objects);


    public abstract List<String> getPossibleValues(final Player player, final Object... objects);

    public final String getVariableType() {
        return main.getVariablesManager().getVariableType(this.getClass());
    }

    public abstract String getPlural();
    public abstract String getSingular();

    public void setAdditionalStringArguments(HashMap<String, String> additionalStringArguments) {
        this.additionalStringArguments = additionalStringArguments;
    }

    public void setAdditionalNumberArguments(HashMap<String, String> additionalNumberArguments) {
        this.additionalNumberArguments = additionalNumberArguments;
    }
    public void setAdditionalBooleanArguments(HashMap<String, Boolean> additionalBooleanArguments) {
        this.additionalBooleanArguments = additionalBooleanArguments;
    }

    public void addAdditionalBooleanArgument(String key, boolean value){
        additionalBooleanArguments.put(key, value);
    }
    public void addAdditionalStringArgument(String key, String value){
        additionalStringArguments.put(key, value);
    }
    public void addAdditionalNumberArgument(String key, String value){
        additionalStringArguments.put(key, value);
    }
}
