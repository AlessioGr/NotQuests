package rocks.gravili.notquests.paper.managers.items;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.managers.data.Category;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;


public class ItemsManager {
    private final NotQuests main;
    private final HashMap<String, NQItem> identifiersAndItems;


    public ItemsManager(final NotQuests main) {
        this.main = main;
        identifiersAndItems = new HashMap<>();
        loadItems();
    }

    public final NQItem getItem(final String itemIdentifier){
        if(itemIdentifier == null){
            return null;
        }
        return identifiersAndItems.get(itemIdentifier.toLowerCase(Locale.ROOT));
    }

    public final Collection<NQItem> getItems(){
        return identifiersAndItems.values();
    }
    public final Collection<String> getItemsIdentifiers(){
        return identifiersAndItems.keySet();
    }

    public void loadItems() {
        ArrayList<String> categoriesStringList = new ArrayList<>();
        for (final Category category : main.getDataManager().getCategories()) {
            categoriesStringList.add(category.getCategoryFullName());
        }
        main.getLogManager().info("Scheduled Items Data load for following categories: <highlight>" + categoriesStringList.toString() );

        for (final Category category : main.getDataManager().getCategories()) {
            loadItems(category);
            main.getLogManager().info("Loading Items for category <highlight>" + category.getCategoryFullName());
        }
    }

    public void loadItems(final Category category) {
        //First load from items.yml:
        if(category.getItemsConfig() == null){
            main.getLogManager().severe("Error: Cannot load items of category <highlight>" + category.getCategoryFullName() + "</highlight>, because it doesn't have an items config. This category has been skipped.");
            return;
        }

        final ConfigurationSection itemsConfigurationSection = category.getItemsConfig().getConfigurationSection("items");
        if (itemsConfigurationSection != null) {
            for (final String itemIdentifier : itemsConfigurationSection.getKeys(false)) {
                if (identifiersAndItems.get(itemIdentifier) != null) {
                    main.getDataManager().disablePluginAndSaving("Plugin disabled, because there was an error while loading items.yml item data: The item " + itemIdentifier + " already exists.");
                    return;
                }
                main.getLogManager().info("Loading item <highlight>" + itemIdentifier);

                String materialString = itemsConfigurationSection.getString(itemIdentifier + ".material", "");

                Material material;
                try{
                    material = Material.valueOf(materialString);
                }catch (Exception e){
                    main.getLogManager().warn("Loading of item " + itemIdentifier + " has been skipped, because the material cannot be found.");
                    continue;

                }

                ItemStack itemStack = new ItemStack(material);

                NQItem nqItem = new NQItem(main, itemIdentifier, itemStack);
                nqItem.setCategory(category);

                String displayName = itemsConfigurationSection.getString(itemIdentifier + ".displayName", "");
                nqItem.setDisplayName(displayName, false);



                if (nqItem != null) {
                    identifiersAndItems.put(itemIdentifier.toLowerCase(Locale.ROOT), nqItem);
                } else {
                    main.getDataManager().disablePluginAndSaving("Plugin disabled, because there was an error while loading items.yml item data.");
                }
            }
        }

    }

    public void addItem(final NQItem nqItem){
        if (identifiersAndItems.get(nqItem.getItemName()) != null) {
            return;
        }

        identifiersAndItems.put(nqItem.getItemName(), nqItem);

        nqItem.getCategory().getItemsConfig().set("items." + nqItem.getItemName() + ".material", nqItem.getItemStack().getType().name());

        nqItem.getCategory().saveItemsConfig();
    }

    public void deleteItem(NQItem nqItem) {
        if (identifiersAndItems.get(nqItem.getItemName()) == null) {
            return;
        }

        identifiersAndItems.remove(nqItem.getItemName());
        nqItem.getCategory().getItemsConfig().set("items." + nqItem.getItemName(), null);
        nqItem.getCategory().saveItemsConfig();
    }

    public final Material getMaterial(final String name){
        Material material;
        try{
            material = Material.valueOf(name.toUpperCase(Locale.ROOT));
        }catch (Exception ignored){
            material = getItem(name).getItemStack().getType();
        }
        return material;
    }
    public final ItemStack getItemStack(final String name){
        ItemStack itemStack;
        try{
            itemStack = new ItemStack(Material.valueOf(name));
        }catch (Exception ignored){
            itemStack = getItem(name).getItemStack();
        }
        return itemStack;
    }
}
