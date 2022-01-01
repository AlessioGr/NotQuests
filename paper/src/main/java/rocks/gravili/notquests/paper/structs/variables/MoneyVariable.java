package rocks.gravili.notquests.paper.structs.variables;

import org.bukkit.entity.Player;
import rocks.gravili.notquests.paper.NotQuests;

import java.util.List;

public class MoneyVariable extends Variable<Double>{
    public MoneyVariable(NotQuests main) {
        super(main);
    }

    @Override
    public Double getValue(Player player, Object... objects) {
        if (player != null) {
            if (!main.getIntegrationsManager().isVaultEnabled() || main.getIntegrationsManager().getVaultManager().getEconomy() == null) {
                return 0D;
            }else {
                return main.getIntegrationsManager().getVaultManager().getEconomy().getBalance(player, player.getWorld().getName());
            }
        } else {
            return 0D;
        }
    }

    @Override
    public List<String> getPossibleValues(Player player, Object... objects) {
        return null;
    }

    @Override
    public String getPlural() {
        return "Money";
    }

    @Override
    public String getSingular() {
        return "Money";
    }
}
