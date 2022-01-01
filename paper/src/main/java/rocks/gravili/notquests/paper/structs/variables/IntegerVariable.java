package rocks.gravili.notquests.paper.structs.variables;

import org.bukkit.entity.Player;
import rocks.gravili.notquests.paper.NotQuests;

import java.util.List;

public abstract class IntegerVariable extends Variable {
    public IntegerVariable(NotQuests main) {
        super(main);
    }

    //public abstract int getValue(Player player, Object... objects);

    @Override
    public List<String> getPossibleValues(Player player, Object... objects) {
        return null;
    }
}
