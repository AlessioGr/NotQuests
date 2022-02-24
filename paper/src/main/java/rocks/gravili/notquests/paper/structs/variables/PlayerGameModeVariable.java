package rocks.gravili.notquests.paper.structs.variables;

import org.bukkit.GameMode;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PlayerGameModeVariable extends Variable<String>{
    public PlayerGameModeVariable(NotQuests main) {
        super(main);
        setCanSetValue(true);
    }

    @Override
    public String getValue(QuestPlayer questPlayer, Object... objects) {
        if (questPlayer != null) {
            return questPlayer.getPlayer().getGameMode().name().toLowerCase(Locale.ROOT);
        } else {
            return null;
        }
    }

    @Override
    public boolean setValueInternally(String newValue, QuestPlayer questPlayer, Object... objects) {
        if (questPlayer != null) {
            questPlayer.getPlayer().setGameMode(GameMode.valueOf(newValue));
            return true;
        } else {
            return false;
        }
    }


    @Override
    public List<String> getPossibleValues(QuestPlayer questPlayer, Object... objects) {
        List<String> possibleValues = new ArrayList<>();
        for (GameMode gameMode : GameMode.values()) {
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
