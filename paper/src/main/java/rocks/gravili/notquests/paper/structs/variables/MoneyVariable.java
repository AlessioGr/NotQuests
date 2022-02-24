package rocks.gravili.notquests.paper.structs.variables;

import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

import java.util.List;

public class MoneyVariable extends Variable<Double>{
    public MoneyVariable(NotQuests main) {
        super(main);
        setCanSetValue(true);
    }

    @Override
    public Double getValue(QuestPlayer questPlayer, Object... objects) {
        if (questPlayer != null) {
            if (!main.getIntegrationsManager().isVaultEnabled() || main.getIntegrationsManager().getVaultManager().getEconomy() == null) {
                return 0D;
            } else {
                return main.getIntegrationsManager().getVaultManager().getEconomy().getBalance(questPlayer.getPlayer(), questPlayer.getPlayer().getWorld().getName());
            }
        } else {
            return 0D;
        }
    }

    @Override
    public boolean setValueInternally(Double newValue, QuestPlayer questPlayer, Object... objects) {
        if (questPlayer != null) {
            if (!main.getIntegrationsManager().isVaultEnabled() || main.getIntegrationsManager().getVaultManager().getEconomy() == null) {
                return false;
            } else {
                final double currentBalance = main.getIntegrationsManager().getVaultManager().getEconomy().getBalance(questPlayer.getPlayer());
                if (newValue > currentBalance) {
                    //player.sendMessage("Deposited " + (newValue-currentBalance));
                    main.getIntegrationsManager().getVaultManager().getEconomy().depositPlayer(questPlayer.getPlayer(), newValue - currentBalance);
                } else {
                    //player.sendMessage("Withdraw " + (currentBalance - newValue));

                    main.getIntegrationsManager().getVaultManager().getEconomy().withdrawPlayer(questPlayer.getPlayer(), currentBalance - newValue);
                }
                return true;
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
        return "Money";
    }

    @Override
    public String getSingular() {
        return "Money";
    }
}
