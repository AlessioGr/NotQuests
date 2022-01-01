package rocks.gravili.notquests.paper.structs.variables;

import org.bukkit.entity.Player;
import rocks.gravili.notquests.paper.NotQuests;

import java.util.List;

public abstract class Variable<T> {
    protected final NotQuests main;

    public Variable(final NotQuests main){
        this.main = main;
    }

    public abstract T getValue(final Player player, final Object... objects);

    public abstract List<String> getPossibleValues(final Player player, final Object... objects);

    public final String getVariableType() {
        return main.getVariablesManager().getVariableType(this.getClass());
    }

    public abstract String getPlural();
    public abstract String getSingular();

}
