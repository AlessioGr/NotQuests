package rocks.gravili.notquests.paper.structs.variables;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.paper.NotQuests;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PlayerGameModeVariable extends Variable<String>{
    public PlayerGameModeVariable(NotQuests main) {
        super(main);
        setCanSetValue(true);
    }

    @Override
    public String getValue(Player player, Object... objects) {
        if (player != null) {
            return player.getGameMode().name().toLowerCase(Locale.ROOT);
        } else {
            return null;
        }
    }

    @Override
    public boolean setValueInternally(String newValue, Player player, Object... objects) {
        if (player != null) {
            player.setGameMode(GameMode.valueOf(newValue));
            return true;
        } else {
            return false;
        }
    }


    @Override
    public List<String> getPossibleValues(Player player, Object... objects) {
        List<String> possibleValues = new ArrayList<>();
        for(GameMode gameMode : GameMode.values()){
            possibleValues.add(gameMode.name().toLowerCase(Locale.ROOT));
        }
        return possibleValues;
    }

    @Override
    public String getPlural() {
        return "GameMode";
    }

    @Override
    public String getSingular() {
        return "GameMode";
    }
}
