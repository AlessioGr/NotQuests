package rocks.gravili.notquests.paper.structs.variables;

import org.bukkit.entity.Player;
import rocks.gravili.notquests.paper.NotQuests;

import java.util.List;

public class PlayerSneakingVariable extends Variable<Boolean>{
    public PlayerSneakingVariable(NotQuests main) {
        super(main);
        setCanSetValue(true);
    }

    @Override
    public Boolean getValue(Player player, Object... objects) {
        if (player != null) {
            return player.isSneaking();
        } else {
            return false;
        }
    }

    @Override
    public boolean setValue(Boolean newValue, Player player, Object... objects) {
        if (player != null) {
            player.setSneaking(newValue);
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
        return "Sneaking";
    }

    @Override
    public String getSingular() {
        return "Sneaking";
    }
}
