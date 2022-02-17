package rocks.gravili.notquests.paper.structs.variables;

import org.bukkit.entity.Player;
import rocks.gravili.notquests.paper.NotQuests;

import java.util.List;

public class PlayerCurrentPositionYVariable extends Variable<Double>{
    public PlayerCurrentPositionYVariable(NotQuests main) {
        super(main);
        setCanSetValue(true);
    }

    @Override
    public Double getValue(Player player, Object... objects) {
        if (player != null) {
            return player.getLocation().getY();
        } else {
            return null;
        }
    }

    @Override
    public boolean setValueInternally(Double newValue, Player player, Object... objects) {
        if (player != null) {
            player.teleportAsync(player.getLocation().add(0, newValue, 0));
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
        return "Y Position";
    }

    @Override
    public String getSingular() {
        return "Y Position";
    }
}
