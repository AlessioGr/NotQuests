package rocks.gravili.notquests.paper.structs.variables;

import org.bukkit.entity.Player;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

import java.util.List;

public class QuestPointsVariable extends Variable<Long>{
    public QuestPointsVariable(NotQuests main) {
        super(main);
    }

    @Override
    public Long getValue(Player player, Object... objects) {
        final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId());
        if(questPlayer == null){
            return 0L;
        }
        return questPlayer.getQuestPoints();
    }

    @Override
    public List<String> getPossibleValues(Player player, Object... objects) {
        return null;
    }

    @Override
    public String getPlural() {
        return "Quest Points";
    }

    @Override
    public String getSingular() {
        return "Quest Point";
    }
}
