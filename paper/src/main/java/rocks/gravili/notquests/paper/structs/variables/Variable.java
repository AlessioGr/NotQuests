package rocks.gravili.notquests.paper.structs.variables;

import cloud.commandframework.arguments.flags.CommandFlag;
import cloud.commandframework.arguments.standard.BooleanArgument;
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.arguments.standard.StringArgument;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.arguments.variables.NumberVariableValueArgument;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public abstract class Variable<T> {
    protected final NotQuests main;
    private final ArrayList<StringArgument<CommandSender>> requiredStrings;
    private final ArrayList<NumberVariableValueArgument<CommandSender>> requiredNumbers;
    private final ArrayList<BooleanArgument<CommandSender>> requiredBooleans;
    private final ArrayList<CommandFlag<CommandSender>> requiredBooleanFlags;


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
                        .getActualTypeArguments()[0];;

        if(typeOf == String.class || typeOf == Character.class){
            variableDataType = VariableDataType.STRING;
        }else if(typeOf == Boolean.class){
            variableDataType = VariableDataType.BOOLEAN;
        }else if(typeOf == String[].class || typeOf == ArrayList.class){
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
    protected void addRequiredBooleanFlag(final CommandFlag<CommandSender> commandFlag){
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
    public final ArrayList<CommandFlag<CommandSender>> getRequiredBooleanFlags(){
        return requiredBooleanFlags;
    }


    protected final String getRequiredStringValue(String key){
        return additionalStringArguments.get(key);
    }

    protected final double getRequiredNumberValue(String key, Player player){
        return main.getVariablesManager().evaluateExpression(additionalNumberArguments.get(key), player);
    }
    protected final boolean getRequiredBooleanValue(String key){
        return additionalBooleanArguments.get(key);
    }

    public abstract T getValue(final Player player, final Object... objects);

    public abstract boolean setValue(final T newValue, final Player player, final Object... objects);

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
}
