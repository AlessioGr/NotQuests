package rocks.gravili.notquests.paper.structs.variables;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.paper.NotQuests;

import java.util.ArrayList;
import java.util.List;

public class PlayerHealthVariable extends Variable<Double>{

    public PlayerHealthVariable(NotQuests main) {
        super(main);
        setCanSetValue(true);
    }

    @Override
    public Double getValue(Player player, Object... objects) {
        if (player != null) {
            return player.getHealth();
        } else {
            return 0d;
        }
    }

    @Override
    public boolean setValueInternally(Double newValue, Player player, Object... objects) {
        if (player != null) {
            player.setHealth(newValue);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public List<String> getPossibleValues(Player player, Object... objects) {
        AttributeInstance maxValueInstance = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if(maxValueInstance == null){
            return null;
        }

        List<String> possibleValues = new ArrayList<>();
        for(double health = 0; health <= maxValueInstance.getValue(); health+=0.5d){
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