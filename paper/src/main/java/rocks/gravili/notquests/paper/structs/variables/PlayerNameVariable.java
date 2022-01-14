package rocks.gravili.notquests.paper.structs.variables;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.paper.NotQuests;

import java.util.List;

public class PlayerNameVariable extends Variable<String>{
    public PlayerNameVariable(NotQuests main) {
        super(main);
        setCanSetValue(true);
    }

    @Override
    public String getValue(Player player, Object... objects) {
        if (player != null) {
            return player.getName();
        } else {
            return null;
        }
    }

    @Override
    public boolean setValue(String newValue, Player player, Object... objects) {
        if (player != null) {
            player.setCustomName(newValue);
            return true;
        } else {
            return false;
        }
    }


    @Override
    public List<String> getPossibleValues(Player player, Object... objects) {
        return Bukkit.getOnlinePlayers().stream().map(playerObject -> playerObject.getName()).toList();
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
