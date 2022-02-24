package rocks.gravili.notquests.paper.structs.variables;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

import java.util.ArrayList;
import java.util.List;

public class PlayerHealthVariable extends Variable<Double>{

    public PlayerHealthVariable(NotQuests main) {
        super(main);
        setCanSetValue(true);
    }

    @Override
    public Double getValue(QuestPlayer questPlayer, Object... objects) {
        if (questPlayer != null) {
            return questPlayer.getPlayer().getHealth();
        } else {
            return 0d;
        }
    }

    @Override
    public boolean setValueInternally(Double newValue, QuestPlayer questPlayer, Object... objects) {
        if (questPlayer != null) {
            questPlayer.getPlayer().setHealth(newValue);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public List<String> getPossibleValues(QuestPlayer questPlayer, Object... objects) {
        AttributeInstance maxValueInstance = questPlayer.getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (maxValueInstance == null) {
            return null;
        }

        List<String> possibleValues = new ArrayList<>();
        for (double health = 0; health <= maxValueInstance.getValue(); health += 0.5d) {
            possibleValues.add("" + health);
        }
        return possibleValues;
    }

    @Override
    public String getPlural() {
        return "Health";
    }

    @Override
    public String getSingular() {
        return "Health";
    }
}