package rocks.gravili.notquests.paper.structs.variables;

import org.bukkit.Location;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

import java.util.List;

public class PlayerCurrentPositionXVariable extends Variable<Double>{
    public PlayerCurrentPositionXVariable(NotQuests main) {
        super(main);
        setCanSetValue(true);
    }

    @Override
    public Double getValue(QuestPlayer questPlayer, Object... objects) {
        if (questPlayer != null) {
            return questPlayer.getPlayer().getLocation().getX();
        } else {
            return null;
        }
    }

    @Override
    public boolean setValueInternally(Double newValue, QuestPlayer questPlayer, Object... objects) {
        if (questPlayer != null) {
            questPlayer.getPlayer().teleportAsync(new Location(questPlayer.getPlayer().getLocation().getWorld(), newValue, questPlayer.getPlayer().getLocation().getY(), questPlayer.getPlayer().getLocation().getZ()));
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
        return "X Position";
    }

    @Override
    public String getSingular() {
        return "X Position";
    }
}
