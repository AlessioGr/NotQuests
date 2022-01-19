package rocks.gravili.notquests.paper.managers.items;

import org.bukkit.inventory.ItemStack;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.managers.data.Category;

import java.util.Locale;

public class NQItem {
    private final NotQuests main;
    private final String itemName;
    private final ItemStack itemStack;
    private Category category;

    public NQItem(final NotQuests main, final String itemName, final ItemStack itemStack){
        this.main = main;
        this.itemName = itemName.toLowerCase(Locale.ROOT);
        this.itemStack = itemStack;
        category = main.getDataManager().getDefaultCategory();
    }

    public final ItemStack getItemStack(){
        return itemStack;
    }

    public final String getItemName(){
        return itemName;
    }

    public final Category getCategory() {
        return category;
    }

    public void setCategory(final Category category) {
        this.category = category;
    }
}
