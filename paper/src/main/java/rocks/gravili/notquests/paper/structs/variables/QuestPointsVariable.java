package rocks.gravili.notquests.paper.structs.variables;

import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

import java.util.List;

public class QuestPointsVariable extends Variable<Long>{
    public QuestPointsVariable(NotQuests main) {
        super(main);
        setCanSetValue(true);
    }

    @Override
    public Long getValue(QuestPlayer questPlayer, Object... objects) {
        if (questPlayer == null) {
            return 0L;
        }
        return questPlayer.getQuestPoints();
    }

    @Override
    public boolean setValueInternally(Long newValue, QuestPlayer questPlayer, Object... objects) {
        if (questPlayer == null) {
            return false;
        }
        questPlayer.setQuestPoints(newValue, false);
        return true;
    }

    @Override
    public List<String> getPossibleValues(QuestPlayer questPlayer, Object... objects) {
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
