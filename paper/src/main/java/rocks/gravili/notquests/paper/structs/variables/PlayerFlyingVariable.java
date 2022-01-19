package rocks.gravili.notquests.paper.structs.variables;

import org.bukkit.entity.Player;
import rocks.gravili.notquests.paper.NotQuests;

import java.util.List;

public class PlayerFlyingVariable extends Variable<Boolean>{
    public PlayerFlyingVariable(NotQuests main) {
        super(main);
        setCanSetValue(true);
    }

    @Override
    public Boolean getValue(Player player, Object... objects) {
        if (player != null) {
            return player.isFlying();
        } else {
            return false;
        }
    }

    @Override
    public boolean setValueInternally(Boolean newValue, Player player, Object... objects) {
        if (player != null) {
            player.setFlying(newValue);
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
        return "Flying";
    }

    @Override
    public String getSingular() {
        return "Flying";
    }
}
