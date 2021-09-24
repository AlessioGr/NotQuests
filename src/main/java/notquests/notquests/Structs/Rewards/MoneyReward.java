package notquests.notquests.Structs.Rewards;

import notquests.notquests.NotQuests;
import notquests.notquests.Structs.Quest;
import org.bukkit.entity.Player;


public class MoneyReward extends Reward {

    private final NotQuests main;
    private final long rewardedMoney;

    public MoneyReward(final NotQuests main, long rewardedMoney) {
        super(RewardType.Money);
        this.main = main;
        this.rewardedMoney = rewardedMoney;

    }

    @Override
    public void giveReward(final Player player, final Quest quest) {
        if (!main.isVaultEnabled() || main.getEconomy() == null) {
            player.sendMessage("§cError: cannot give you the money reward because Vault (needed for money stuff to work) is not installed on the server.");
            return;
        }
        if (rewardedMoney > 0) {
            main.getEconomy().depositPlayer(player, rewardedMoney);
        } else if (rewardedMoney < 0) {
            main.getEconomy().withdrawPlayer(player, rewardedMoney);
        }
    }

    public final long getRewardedMoney() {
        return rewardedMoney;
    }
}