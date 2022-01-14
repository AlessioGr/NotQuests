package rocks.gravili.notquests.paper.structs.variables.hooks;

import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.variables.Variable;

import java.util.List;

public class TownyNationNameVariable extends Variable<String> {
    public TownyNationNameVariable(NotQuests main) {
        super(main);
        setCanSetValue(true);
    }

    @Override
    public String getValue(Player player, Object... objects) {
        if (!main.getIntegrationsManager().isTownyEnabled()) {
            return "";
        }
        if (player != null) {
            Resident resident = TownyUniverse.getInstance().getResident(player.getUniqueId());
            if (resident != null && resident.getTownOrNull() != null && resident.hasNation() && resident.getNationOrNull() != null) {
                Nation nation = resident.getNationOrNull();
                return nation.getName().replace("_", " ");
            } else {
                return "";
            }

        } else {
            return "0";

        }
    }

    @Override
    public boolean setValue(String newValue, Player player, Object... objects) {
        if (!main.getIntegrationsManager().isTownyEnabled()) {
            return false;
        }
        if (player != null) {
            Resident resident = TownyUniverse.getInstance().getResident(player.getUniqueId());
            if (resident != null && resident.getTownOrNull() != null && resident.hasNation() && resident.getNationOrNull() != null) {
                Nation nation = resident.getNationOrNull();
                nation.setName(newValue);
                return true;
            } else {
                return false;
            }

        } else {
            return false;

        }
    }

    @Override
    public List<String> getPossibleValues(Player player, Object... objects) {
        return null;
    }

    @Override
    public String getPlural() {
        return "Nation names";
    }

    @Override
    public String getSingular() {
        return "Nation name";
    }
}
