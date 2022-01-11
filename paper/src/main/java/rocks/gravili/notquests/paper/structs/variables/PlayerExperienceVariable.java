package rocks.gravili.notquests.paper.structs.variables;

import org.bukkit.entity.Player;
import rocks.gravili.notquests.paper.NotQuests;

import java.util.List;

public class PlayerExperienceVariable extends Variable<Float>{
    public PlayerExperienceVariable(NotQuests main) {
        super(main);
        setCanSetValue(true);
    }

    @Override
    public Float getValue(Player player, Object... objects) {
        if (player != null) {
            return player.getExp();
        } else {
            return null;
        }
    }

    @Override
    public boolean setValue(Float newValue, Player player, Object... objects) {
        if (player != null) {
            player.setExp(newValue);
            return true;
        } else {
            return false;
        }
    }


    @Override
    public List<String> getPossibleValues(Player player, Object... objects) {
        return null;
    }

    @Override
    public String getPlural() {
        return "Money";
    }

    @Override
    public String getSingular() {
        return "Money";
    }
}
