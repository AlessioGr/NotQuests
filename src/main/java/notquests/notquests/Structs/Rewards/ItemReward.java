package notquests.notquests.Structs.Rewards;

import notquests.notquests.NotQuests;
import notquests.notquests.Structs.Quest;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ItemReward extends Reward {

    private final NotQuests main;
    private final ItemStack item;

    public ItemReward(final NotQuests main, ItemStack item) {
        super(RewardType.Item);
        this.main = main;
        this.item = item;

    }

    @Override
    public void giveReward(final Player player, final Quest quest) {

        if (Bukkit.isPrimaryThread()) {
            player.getInventory().addItem(item);
        } else {
            Bukkit.getScheduler().runTask(main, () -> player.getInventory().addItem(item)); //TODO: Check if I can't just run it async if it already is async`?
        }


    }

    public final ItemStack getItemReward() {
        return item;
    }
}