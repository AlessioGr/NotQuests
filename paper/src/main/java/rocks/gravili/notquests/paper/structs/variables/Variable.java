package rocks.gravili.notquests.paper.structs.variables;

import org.bukkit.entity.Player;
import rocks.gravili.notquests.paper.NotQuests;

import java.util.List;

public abstract class Variable {
    private final NotQuests main;

    public Variable(final NotQuests main){
        this.main = main;
    }

    public abstract Object getValue(final Player player, final Object... objects);

    public abstract List<String> getPossibleValues(final Player player, final Object... objects);
}
