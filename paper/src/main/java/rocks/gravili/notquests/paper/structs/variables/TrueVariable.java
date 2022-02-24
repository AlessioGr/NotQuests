package rocks.gravili.notquests.paper.structs.variables;

import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

import java.util.List;

//Useful for some conditions
public class TrueVariable extends Variable<Boolean>{
    public TrueVariable(NotQuests main) {
        super(main);
    }

    @Override
    public Boolean getValue(QuestPlayer questPlayer, Object... objects) {
        return true;
    }

    @Override
    public boolean setValueInternally(Boolean newValue, QuestPlayer questPlayer, Object... objects) {
        return false;
    }


    @Override
    public List<String> getPossibleValues(QuestPlayer questPlayer, Object... objects) {
        return null;
    }

    @Override
    public String getPlural() {
        return "True";
    }

    @Override
    public String getSingular() {
        return "True";
    }
}
