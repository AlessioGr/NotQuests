package rocks.gravili.notquests.paper.structs.variables.hooks;

import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.QuestPlayer;
import rocks.gravili.notquests.paper.structs.variables.Variable;

import java.util.List;

public class TownyNationTownCountVariable extends Variable<Integer> {
    public TownyNationTownCountVariable(NotQuests main) {
        super(main);
    }

    @Override
    public Integer getValue(QuestPlayer questPlayer, Object... objects) {
        if (!main.getIntegrationsManager().isTownyEnabled()) {
            return 0;
        }
        if (questPlayer != null) {
            Resident resident = TownyUniverse.getInstance().getResident(questPlayer.getUniqueId());
            if (resident != null && resident.getTownOrNull() != null && resident.hasNation() && resident.getNationOrNull() != null) {
                Nation nation = resident.getNationOrNull();
                return nation.getNumTowns();
            } else {
                return 0;
            }

        } else {
            return 0;

        }
    }

    @Override
    public boolean setValueInternally(Integer newValue, QuestPlayer questPlayer, Object... objects) {
        return false;
    }

    @Override
    public List<String> getPossibleValues(QuestPlayer questPlayer, Object... objects) {
        return null;
    }

    @Override
    public String getPlural() {
        return "Towns in Nation";
    }

    @Override
    public String getSingular() {
        return "Town in Nation";
    }
}
