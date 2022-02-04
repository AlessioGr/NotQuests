package rocks.gravili.notquests.paper.structs.variables;

import org.bukkit.entity.Player;
import rocks.gravili.notquests.paper.NotQuests;

import java.util.List;

//Useful for some conditions
public class TrueVariable extends Variable<Boolean>{
    public TrueVariable(NotQuests main) {
        super(main);
    }

    @Override
    public Boolean getValue(Player player, Object... objects) {
        return true;
    }

    @Override
    public boolean setValueInternally(Boolean newValue, Player player, Object... objects) {
        return false;
    }


    @Override
    public List<String> getPossibleValues(Player player, Object... objects) {
        return null;
    }

    @Override
    public String getPlural() {
        return "True";
    }

    @Override
    public String getSingular() {
        return "True";
    }
}
