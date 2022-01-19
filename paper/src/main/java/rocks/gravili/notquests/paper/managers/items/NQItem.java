package rocks.gravili.notquests.paper.managers.items;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.managers.data.Category;

import javax.annotation.Nullable;
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

    public void setDisplayName(@Nullable final String displayName, boolean save){
        if(displayName == null || displayName.isBlank()){
            ItemMeta itemMeta = itemStack.getItemMeta();
            itemMeta.displayName(null);
            itemStack.setItemMeta(itemMeta);
            if(save){
                getCategory().getItemsConfig().set("items." + getItemName() + ".displayName", null);
                getCategory().saveItemsConfig();
            }
            return;
        }
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.displayName(main.parse("<!italic>"+displayName));
        itemStack.setItemMeta(itemMeta);
        if(save){
            getCategory().getItemsConfig().set("items." + getItemName() + ".displayName", displayName);
            getCategory().saveItemsConfig();
        }
    }
}
