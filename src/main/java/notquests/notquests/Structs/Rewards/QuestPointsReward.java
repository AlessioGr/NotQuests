package notquests.notquests.Structs.Rewards;

import notquests.notquests.NotQuests;
import notquests.notquests.Structs.Quest;
import notquests.notquests.Structs.QuestPlayer;
import org.bukkit.entity.Player;

import java.util.logging.Level;

public class QuestPointsReward extends Reward {

    private final NotQuests main;
    private final long rewardedQuestPoints;

    public QuestPointsReward(final NotQuests main, long rewardedQuestPoints) {
        super(RewardType.QuestPoints);
        this.main = main;
        this.rewardedQuestPoints = rewardedQuestPoints;

    }

    @Override
    public void giveReward(final Player player, final Quest quest) {
        QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId());
        if (questPlayer != null) {
            questPlayer.addQuestPoints(rewardedQuestPoints, true);

        } else {
            main.getLogManager().log(Level.WARNING, "§cError giving quest point reward to player §b" + player.getName());

            player.sendMessage("§cError giving quest point reward.");
        }

    }

    public final long getRewardedQuestPoints() {
        return rewardedQuestPoints;
    }
}
