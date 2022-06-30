/*
 * NotQuests - A Questing plugin for Minecraft Servers
 * Copyright (C) 2022 Alessio Gravili
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package rocks.gravili.notquests.paper.managers.items;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.managers.data.Category;

public class ItemsManager {
  private final NotQuests main;
  private final HashMap<String, NQItem> identifiersAndItems;

  public ItemsManager(final NotQuests main) {
    this.main = main;
    identifiersAndItems = new HashMap<>();
    loadItems();
  }

  public final NQItem getItem(final String itemIdentifier) {
    if (itemIdentifier == null) {
      return null;
    }
    return identifiersAndItems.get(itemIdentifier.toLowerCase(Locale.ROOT));
  }

  public final Collection<NQItem> getItems() {
    return identifiersAndItems.values();
  }

  public final Collection<String> getItemsIdentifiers() {
    return identifiersAndItems.keySet();
  }

  public void loadItems() {
    final ArrayList<String> categoriesStringList = new ArrayList<>();
    for (final Category category : main.getDataManager().getCategories()) {
      categoriesStringList.add(category.getCategoryFullName());
    }
    main.getLogManager()
        .info(
            "Scheduled Items Data load for following categories: <highlight>"
                + categoriesStringList);

    for (final Category category : main.getDataManager().getCategories()) {
      loadItems(category);
      main.getLogManager()
          .info("  Loading Items for category <highlight>" + category.getCategoryFullName());
    }
  }

  public void loadItems(final Category category) {
    // First load from items.yml:
    if (category.getItemsConfig() == null) {
      main.getLogManager()
          .severe(
              "Error: Cannot load items of category <highlight>"
                  + category.getCategoryFullName()
                  + "</highlight>, because it doesn't have an items config. This category has been skipped.");
      return;
    }

    final ConfigurationSection itemsConfigurationSection =
        category.getItemsConfig().getConfigurationSection("items");
    if (itemsConfigurationSection != null) {
      for (final String itemIdentifier : itemsConfigurationSection.getKeys(false)) {
        if (identifiersAndItems.get(itemIdentifier) != null) {
          main.getDataManager()
              .disablePluginAndSaving(
                  "Plugin disabled, because there was an error while loading items.yml item data: The item "
                      + itemIdentifier
                      + " already exists.");
          return;
        }
        main.getLogManager().info("Loading item <highlight>" + itemIdentifier);

        String materialString =
            itemsConfigurationSection.getString(itemIdentifier + ".material", "");

        Material material;
        try {
          material = Material.valueOf(materialString.toUpperCase(Locale.ROOT));
        } catch (Exception e) {
          main.getLogManager()
              .warn(
                  "Loading of item "
                      + itemIdentifier
                      + " has been skipped, because the material cannot be found.");
          continue;
        }

        ItemStack itemStack = new ItemStack(material);

        NQItem nqItem = new NQItem(main, itemIdentifier, itemStack);
        nqItem.setCategory(category);

        String displayName =
            itemsConfigurationSection.getString(itemIdentifier + ".displayName", "");
        nqItem.setDisplayName(displayName, false);

        identifiersAndItems.put(itemIdentifier.toLowerCase(Locale.ROOT), nqItem);
      }
    }
  }

  public void addItem(final NQItem nqItem) {
    if (identifiersAndItems.get(nqItem.getItemName()) != null) {
      return;
    }

    identifiersAndItems.put(nqItem.getItemName(), nqItem);

    nqItem
        .getCategory()
        .getItemsConfig()
        .set("items." + nqItem.getItemName() + ".material", nqItem.getItemStack().getType().name());

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

  public final Material getMaterial(final String name) {
    Material material;
    try {
      material = Material.valueOf(name.toUpperCase(Locale.ROOT));
    } catch (Exception ignored) {
      material = getItem(name).getItemStack().getType();
    }
    return material;
  }
}
