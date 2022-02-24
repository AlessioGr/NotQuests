package rocks.gravili.notquests.paper.structs.variables;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

import java.util.List;

public class PlayerNameVariable extends Variable<String>{
    public PlayerNameVariable(NotQuests main) {
        super(main);
        setCanSetValue(true);
    }

    @Override
    public String getValue(QuestPlayer questPlayer, Object... objects) {
        if (questPlayer != null) {
            return questPlayer.getPlayer().getName();
        } else {
            return null;
        }
    }

    @Override
    public boolean setValueInternally(String newValue, QuestPlayer questPlayer, Object... objects) {
        if (questPlayer != null) {
            questPlayer.getPlayer().customName(Component.text(newValue));
            return true;
        } else {
            return false;
        }
    }


    @Override
    public List<String> getPossibleValues(QuestPlayer questPlayer, Object... objects) {
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
