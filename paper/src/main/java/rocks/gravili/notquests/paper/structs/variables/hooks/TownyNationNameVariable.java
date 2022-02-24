package rocks.gravili.notquests.paper.structs.variables.hooks;

import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.QuestPlayer;
import rocks.gravili.notquests.paper.structs.variables.Variable;

import java.util.List;

public class TownyNationNameVariable extends Variable<String> {
    public TownyNationNameVariable(NotQuests main) {
        super(main);
        setCanSetValue(true);
    }

    @Override
    public String getValue(QuestPlayer questPlayer, Object... objects) {
        if (!main.getIntegrationsManager().isTownyEnabled()) {
            return "";
        }
        if (questPlayer != null) {
            Resident resident = TownyUniverse.getInstance().getResident(questPlayer.getPlayer().getUniqueId());
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
    public boolean setValueInternally(String newValue, QuestPlayer questPlayer, Object... objects) {
        if (!main.getIntegrationsManager().isTownyEnabled()) {
            return false;
        }
        if (questPlayer != null) {
            Resident resident = TownyUniverse.getInstance().getResident(questPlayer.getUniqueId());
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
    public List<String> getPossibleValues(QuestPlayer questPlayer, Object... objects) {
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
