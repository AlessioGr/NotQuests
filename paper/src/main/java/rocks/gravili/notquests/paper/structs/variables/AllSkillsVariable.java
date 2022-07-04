package rocks.gravili.notquests.paper.structs.variables;

import java.util.List;
import com.neostorm.neostorm.Api;
import java.util.ArrayList;
import rocks.gravili.notquests.paper.structs.QuestPlayer;
import rocks.gravili.notquests.paper.NotQuests;

public class AllSkillsVariable extends Variable<Integer[]>
{
    public AllSkillsVariable(final NotQuests main) {
        super(main);
    }

    @Override
    public Integer[] getValue(final QuestPlayer questPlayer, final Object... objects) {
        final List<Integer> skills = new ArrayList<>();
        for (final String skill : Api.getStatTable()) {
            skills.add(Api.getStats(questPlayer.getPlayer(), skill));
        }
        Integer[] stockArr = new Integer[Api.getStatTable().length];
        stockArr = skills.toArray(stockArr);
        return stockArr;
    }

    @Override
    public boolean setValueInternally(final Integer[] newValue, final QuestPlayer questPlayer, final Object... objects) {
        return false;
    }

    @Override
    public List<String> getPossibleValues(final QuestPlayer questPlayer, final Object... objects) {
        return null;
    }

    @Override
    public String getPlural() {
        return "Skills";
    }

    @Override
    public String getSingular() {
        return "Skill";
    }
}