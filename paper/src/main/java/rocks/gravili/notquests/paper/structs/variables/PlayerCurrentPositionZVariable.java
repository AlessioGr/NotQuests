package rocks.gravili.notquests.paper.structs.variables;

import org.bukkit.entity.Player;
import rocks.gravili.notquests.paper.NotQuests;

import java.util.List;

public class PlayerCurrentPositionZVariable extends Variable<Double>{
    public PlayerCurrentPositionZVariable(NotQuests main) {
        super(main);
        setCanSetValue(true);
    }

    @Override
    public Double getValue(Player player, Object... objects) {
        if (player != null) {
            return player.getLocation().getZ();
        } else {
            return null;
        }
    }

    @Override
    public boolean setValueInternally(Double newValue, Player player, Object... objects) {
        if (player != null) {
            player.teleportAsync(player.getLocation().add(0, 0, newValue));
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
        return "Z Position";
    }

    @Override
    public String getSingular() {
        return "Z Position";
    }
}
