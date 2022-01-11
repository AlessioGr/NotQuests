package rocks.gravili.notquests.paper.structs.variables;

import cloud.commandframework.arguments.standard.StringArgument;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.paper.NotQuests;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public abstract class Variable<T> {
    protected final NotQuests main;
    private final ArrayList<StringArgument<CommandSender>> requiredStrings;
    private HashMap<String, String> additionalStringArguments;
    private boolean canSetValue = false;

    private final VariableDataType variableDataType;

    public Variable(final NotQuests main){
        this.main = main;
        requiredStrings = new ArrayList<>();
        additionalStringArguments = new HashMap<>();


        Class<T> typeOf = (Class<T>)
                ((ParameterizedType)getClass()
                        .getGenericSuperclass())
                        .getActualTypeArguments()[0];;

        if(typeOf == String.class || typeOf == Character.class){
            variableDataType = VariableDataType.STRING;
        }else if(typeOf == Boolean.class){
            variableDataType = VariableDataType.BOOLEAN;
        }else if(typeOf == String[].class || typeOf == ArrayList.class){
            variableDataType = VariableDataType.BOOLEAN;
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

    protected void addRequiredString(final StringArgument<CommandSender> stringArument){
        requiredStrings.add(stringArument);
    }

    public final ArrayList<StringArgument<CommandSender>> getRequiredStrings(){
        return requiredStrings;
    }

    protected final String getRequiredStringValue(String key){
        return additionalStringArguments.get(key);
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
}
