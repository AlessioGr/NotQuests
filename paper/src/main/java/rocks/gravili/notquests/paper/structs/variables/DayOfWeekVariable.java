package rocks.gravili.notquests.paper.structs.variables;

import org.bukkit.entity.Player;
import rocks.gravili.notquests.paper.NotQuests;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DayOfWeekVariable extends Variable<String>{
    public DayOfWeekVariable(NotQuests main) {
        super(main);
    }

    @Override
    public String getValue(Player player, Object... objects) {
        DayOfWeek dayOfWeek = LocalDate.now().getDayOfWeek();

        return dayOfWeek.name().toLowerCase(Locale.ROOT);

    }

    @Override
    public boolean setValueInternally(String newValue, Player player, Object... objects) {
        return false;
    }


    @Override
    public List<String> getPossibleValues(Player player, Object... objects) {
        List<String> possibleValues = new ArrayList<>();
        for(DayOfWeek dayOfWeek : DayOfWeek.values()){
            possibleValues.add(dayOfWeek.name().toLowerCase(Locale.ROOT));
        }
        return possibleValues;
    }

    @Override
    public String getPlural() {
        return "Day of Week";
    }

    @Override
    public String getSingular() {
        return "Day of Week";
    }
}
