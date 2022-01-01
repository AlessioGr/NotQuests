package rocks.gravili.notquests.paper.structs.variables.hooks;

import org.bukkit.entity.Player;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.variables.Variable;

import java.util.List;

public class UltimateClansClanLevelVariable extends Variable<Integer> {
    public UltimateClansClanLevelVariable(NotQuests main) {
        super(main);
    }

    @Override
    public Integer getValue(Player player, Object... objects) {
        if (!main.getIntegrationsManager().isUltimateClansEnabled()) {
            return 0;
        }
        if (player != null) {
            return main.getIntegrationsManager().getUltimateClansManager().getClanLevel(player);
        } else {
            return 0;
        }
    }

    @Override
    public List<String> getPossibleValues(Player player, Object... objects) {
        return null;
    }

    @Override
    public String getPlural() {
        return "Clan Level";
    }

    @Override
    public String getSingular() {
        return "Clan Levels";
    }
}
