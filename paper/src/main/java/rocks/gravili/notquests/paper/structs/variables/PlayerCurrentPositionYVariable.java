package rocks.gravili.notquests.paper.structs.variables;

import org.bukkit.Location;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

import java.util.List;

public class PlayerCurrentPositionYVariable extends Variable<Double>{
    public PlayerCurrentPositionYVariable(NotQuests main) {
        super(main);
        setCanSetValue(true);
    }

    @Override
    public Double getValue(QuestPlayer questPlayer, Object... objects) {
        if (questPlayer != null) {
            return questPlayer.getPlayer().getLocation().getY();
        } else {
            return null;
        }
    }

    @Override
    public boolean setValueInternally(Double newValue, QuestPlayer questPlayer, Object... objects) {
        if (questPlayer != null) {
            questPlayer.getPlayer().teleportAsync(new Location(questPlayer.getPlayer().getLocation().getWorld(), questPlayer.getPlayer().getLocation().getX(), newValue, questPlayer.getPlayer().getLocation().getZ()));
            return true;
        } else {
            return false;
        }
    }


    @Override
    public List<String> getPossibleValues(QuestPlayer questPlayer, Object... objects) {
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
