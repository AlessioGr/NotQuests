package rocks.gravili.notquests.paper.structs.variables;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.paper.NotQuests;

import java.util.List;
import java.util.stream.Collectors;

public class PlayerCurrentWorldVariable extends Variable<String>{
    public PlayerCurrentWorldVariable(NotQuests main) {
        super(main);
        setCanSetValue(true);
    }

    @Override
    public String getValue(Player player, Object... objects) {
        if (player != null) {
            return player.getWorld().getName();
        } else {
            return null;
        }
    }

    @Override
    public boolean setValueInternally(String newValue, Player player, Object... objects) {
        if (player != null) {
            final World world = Bukkit.getWorld(newValue);
            if(world == null){
                return false;
            }
            player.teleport(world.getSpawnLocation());
            return true;
        } else {
            return false;
        }
    }


    @Override
    public List<String> getPossibleValues(Player player, Object... objects) {
        return Bukkit.getWorlds().stream().map(world -> world.getName()).toList();
    }

    @Override
    public String getPlural() {
        return "Worlds";
    }

    @Override
    public String getSingular() {
        return "World";
    }
}
