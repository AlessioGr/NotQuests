package rocks.gravili.notquests.paper.structs.variables;

import org.bukkit.entity.Player;
import rocks.gravili.notquests.paper.NotQuests;

import java.util.List;

public class MoneyVariable extends Variable<Double>{
    public MoneyVariable(NotQuests main) {
        super(main);
        setCanSetValue(true);
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
    public boolean setValue(Double newValue, Player player, Object... objects) {
        if (player != null) {
            if (!main.getIntegrationsManager().isVaultEnabled() || main.getIntegrationsManager().getVaultManager().getEconomy() == null) {
                return false;
            }else {
                final double currentBalance = main.getIntegrationsManager().getVaultManager().getEconomy().getBalance(player);
                if(newValue > currentBalance){
                    //player.sendMessage("Deposited " + (newValue-currentBalance));
                    main.getIntegrationsManager().getVaultManager().getEconomy().depositPlayer(player, newValue-currentBalance);
                }else {
                    //player.sendMessage("Withdraw " + (currentBalance - newValue));

                    main.getIntegrationsManager().getVaultManager().getEconomy().withdrawPlayer(player, currentBalance - newValue);
                }
                return true;
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
        return "Money";
    }

    @Override
    public String getSingular() {
        return "Money";
    }
}
