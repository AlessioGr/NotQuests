package notquests.notquests.Structs.Rewards;

import notquests.notquests.Structs.Quest;
import org.bukkit.entity.Player;

public class Reward {
    private final RewardType rewardType;

    public Reward(final RewardType rewardType) {
        this.rewardType = rewardType;
    }

    public final RewardType getRewardType() {
        return rewardType;
    }

    public void giveReward(final Player player, final Quest quest) {

    }
}
