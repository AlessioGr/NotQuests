package rocks.gravili.notquests.paper.structs.variables;

import org.bukkit.entity.Player;
import rocks.gravili.notquests.paper.NotQuests;

import java.util.List;

public class PlayerExperienceLevelVariable extends Variable<Integer>{
    public PlayerExperienceLevelVariable(NotQuests main) {
        super(main);
        setCanSetValue(true);
    }

    @Override
    public Integer getValue(Player player, Object... objects) {
        if (player != null) {
            return player.getLevel();
        } else {
            return null;
        }
    }

    @Override
    public boolean setValue(Integer newValue, Player player, Object... objects) {
        if (player != null) {
            player.setLevel(newValue);
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
