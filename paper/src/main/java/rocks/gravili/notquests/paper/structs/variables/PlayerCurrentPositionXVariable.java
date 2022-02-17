package rocks.gravili.notquests.paper.structs.variables;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.paper.NotQuests;

import java.util.List;

public class PlayerCurrentPositionXVariable extends Variable<Double>{
    public PlayerCurrentPositionXVariable(NotQuests main) {
        super(main);
        setCanSetValue(true);
    }

    @Override
    public Double getValue(Player player, Object... objects) {
        if (player != null) {
            return player.getLocation().getX();
        } else {
            return null;
        }
    }

    @Override
    public boolean setValueInternally(Double newValue, Player player, Object... objects) {
        if (player != null) {
            player.teleportAsync(new Location(player.getLocation().getWorld(), newValue, player.getLocation().getY(), player.getLocation().getZ()));
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
        return "X Position";
    }

    @Override
    public String getSingular() {
        return "X Position";
    }
}
