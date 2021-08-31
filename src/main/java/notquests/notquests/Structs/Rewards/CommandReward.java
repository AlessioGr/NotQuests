package notquests.notquests.Structs.Rewards;

import notquests.notquests.NotQuests;
import notquests.notquests.Structs.Quest;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

public class CommandReward extends Reward {

    private final NotQuests main;
    private final String consoleCommand;

    public CommandReward(final NotQuests main, String consoleCommand) {
        super(RewardType.ConsoleCommand);
        this.main = main;
        this.consoleCommand = consoleCommand;

    }

    @Override
    public void giveReward(final Player player, final Quest quest) {
        String rewardConsoleCommand = consoleCommand.replace("{PLAYER}", player.getName()).replace("{PLAYERUUID}", player.getUniqueId().toString());
        rewardConsoleCommand = rewardConsoleCommand.replace("{PLAYERX}", "" + player.getLocation().getX());
        rewardConsoleCommand = rewardConsoleCommand.replace("{PLAYERY}", "" + player.getLocation().getY());
        rewardConsoleCommand = rewardConsoleCommand.replace("{PLAYERZ}", "" + player.getLocation().getZ());
        rewardConsoleCommand = rewardConsoleCommand.replace("{WORLD}", "" + player.getWorld().getName());
        rewardConsoleCommand = rewardConsoleCommand.replace("{QUEST}", "" + quest.getQuestName());
        ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();

        if (Bukkit.isPrimaryThread()) {
            Bukkit.dispatchCommand(console, rewardConsoleCommand);
        } else {
            final String finalRewardConsoleCommand = rewardConsoleCommand;
            Bukkit.getScheduler().runTask(main, () -> Bukkit.dispatchCommand(console, finalRewardConsoleCommand));
        }


    }

    public final String getConsoleCommand() {
        return consoleCommand;
    }
}
