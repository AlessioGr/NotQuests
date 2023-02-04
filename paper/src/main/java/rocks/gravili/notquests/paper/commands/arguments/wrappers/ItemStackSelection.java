package rocks.gravili.notquests.paper.commands.arguments.wrappers;

import java.util.ArrayList;
import java.util.Locale;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.Nullable;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.managers.items.NQItem;

public class ItemStackSelection {
  private final NotQuests main;
  private final ArrayList<NQItem> nqItems;
  private final ArrayList<ItemStack> itemStacks;
  private final ArrayList<Material> materials;
  private boolean any;

  public ItemStackSelection(final NotQuests main) {
    this.main = main;

    nqItems = new ArrayList<>();
    itemStacks = new ArrayList<>();
    materials = new ArrayList<>();
  }

  public final boolean isAny() {
    return any;
  }

  public void setAny(final boolean any) {
    this.any = any;
  }

  public void addNqItem(final NQItem nqItem) {
    this.nqItems.add(nqItem);
  }

  public void addNqItemName(final String nqItemName) {
    if(nqItemName != null) {
      addNqItem(main.getItemsManager().getItem(nqItemName));
    }
  }

  public void addItemStack(@Nullable final ItemStack itemStack) {
    if (itemStack != null) {
      this.itemStacks.add(itemStack);
    }
  }

  public void addMaterial(final Material material) {
    if(material != null) {
      this.materials.add(material);
    }
  }

  public void addMaterialName(final String materialName) {
    // Check if nqItem here too
    main.getLogManager().debug("Trying to add material name: " + materialName);
    if(materialName != null){
      addMaterial(Material.getMaterial(materialName.toUpperCase(Locale.ROOT)));
    }
  }

  public void saveToFileConfiguration(FileConfiguration configuration, String initialPath) {
    if (!nqItems.isEmpty()) {
      int counter = 1;
      for (final NQItem nqItem : nqItems) {
        configuration.set(initialPath + ".nqItems." + counter, nqItem.getItemName());
        counter++;
      }
    } else if (!itemStacks.isEmpty()) {
      int counter = 1;
      for (final ItemStack itemStack : itemStacks) {
        configuration.set(initialPath + ".itemStacks." + counter, itemStack);
        counter++;
      }
    } else if (!materials.isEmpty()) {
      int counter = 1;
      for (final Material material : materials) {
        configuration.set(initialPath + ".materials." + counter, material.name());
        counter++;
      }
    }
    configuration.set(initialPath + ".any", any);
  }

  public void loadFromFileConfiguration(FileConfiguration configuration, String initialPath) {
    if (!configuration.contains(initialPath)) {
      return;
    }

    if (configuration.contains(initialPath + ".nqItems")) {
      final ConfigurationSection nqItemSection =
          configuration.getConfigurationSection(initialPath + ".nqItems");
      if (nqItemSection != null) {
        for (final String nqItemID : nqItemSection.getKeys(false)) {
          final NQItem nqItem = main.getItemsManager().getItem(nqItemSection.getString(nqItemID));
          if (nqItem != null) {
            addNqItem(nqItem);
          }
        }
      }
    }

    if (configuration.contains(initialPath + ".itemStacks")) {
      final ConfigurationSection itemStacksSection =
          configuration.getConfigurationSection(initialPath + ".itemStacks");
      if (itemStacksSection != null) {
        for (final String itemStackID : itemStacksSection.getKeys(false)) {
          addItemStack(itemStacksSection.getItemStack(itemStackID));
        }
      }
    }

    if (configuration.contains(initialPath + ".materials")) {
      final ConfigurationSection materialsSection =
          configuration.getConfigurationSection(initialPath + ".materials");
      if (materialsSection != null) {
        for (final String materialID : materialsSection.getKeys(false)) {
          final String materialString = materialsSection.getString(materialID);
          if (materialString != null) {
            addMaterial(Material.getMaterial(materialString));
          }
        }
      }
    }

    this.any = configuration.getBoolean(initialPath + ".any");
  }

  public final String getAllMaterialsListedTranslated(String tag) {
    if (any) {
      return "Any";
    }
    final StringBuilder materialsString = new StringBuilder();
    for (final Material material : materials) {
      materialsString.append(", ").append("<lang:" + material.translationKey() + ">");
    }
    for (final NQItem nqItem : nqItems) {
      materialsString.append(", ").append(nqItem.getItemName());
    }
    for (final ItemStack itemStack : itemStacks) {
      materialsString.append(", ").append("<lang:" + itemStack.getType().translationKey() + ">");
    }

    return materialsString.length() >= 2
        ? materialsString.substring(2)
        : materialsString.toString();
  }
  public final String getAllMaterialsListed() {
    if (any) {
      return "Any";
    }
    final StringBuilder materialsString = new StringBuilder();
    for (final Material material : materials) {
      materialsString.append(", ").append(material.name());
    }
    for (final NQItem nqItem : nqItems) {
      materialsString.append(", ").append(nqItem.getItemName());
    }
    for (final ItemStack itemStack : itemStacks) {
      materialsString.append(", ").append(itemStack.getType().name());
    }

    return materialsString.length() >= 2
        ? materialsString.substring(2)
        : materialsString.toString();
  }

  // Material is often used when checking Blocks and not itemStacks
  public final boolean checkIfIsIncluded(final Material materialToCheck) {
    if (any) {
      return true;
    }
    for (final Material material : materials) {
      if (materialToCheck == material) {
        return true;
      }
    }

    for (final ItemStack itemStack : itemStacks) {
      if (itemStack.getType() == materialToCheck) {
        return true;
      }
    }

    for (final NQItem nqItem : nqItems) {
      if (nqItem.getItemStack().getType() == materialToCheck) {
        return true;
      }
    }

    return false;
  }

  @Override
  public String toString() {
    return "ItemStackSelection{" +
        "nqItems=" + nqItems +
        ", itemStacks=" + itemStacks +
        ", materials=" + materials +
        ", any=" + any +
        '}';
  }

  public final boolean checkIfIsIncluded(final ItemStack itemStackToCheck) {
    if (any) {
      return true;
    }
    for (final Material material : materials) {
      if (itemStackToCheck.getType() == material) {
        return true;
      }
    }

    for (final ItemStack itemStack : itemStacks) {
      if (itemStack.isSimilar(itemStackToCheck)) {
        return true;
      }
    }

    for (final NQItem nqItem : nqItems) {
      if (nqItem.getItemStack().isSimilar(itemStackToCheck)) {
        return true;
      }
    }

    return false;
  }

  public final @Nullable ItemStack toFirstItemStack() {
    if (any) {
      return null;
    }
    if (!nqItems.isEmpty()) {
      return nqItems.get(0).getItemStack();
    }
    if (!itemStacks.isEmpty()) {
      return itemStacks.get(0);
    }
    if (!materials.isEmpty()) {
      return new ItemStack(materials.get(0));
    }
    return null;
  }

  public final @NonNull ArrayList<ItemStack> toItemStackList() {
    final ArrayList<ItemStack> itemStackList = new ArrayList<>();

    if (any) {
      return itemStackList;
    }
    if (!nqItems.isEmpty()) {
      for (final NQItem nqItem : nqItems) {
        itemStackList.add(nqItem.getItemStack());
      }
    }
    if (!itemStacks.isEmpty()) {
      itemStackList.addAll(itemStacks);
    }
    if (!materials.isEmpty()) {
      for (final Material material : materials) {
        itemStackList.add(new ItemStack(material));
      }
    }
    return itemStackList;
  }

  public final boolean hasNQItem() {
    return !nqItems.isEmpty();
  }

  public final boolean isEmptyOrAny() {
    return any || materials.isEmpty() && itemStacks.isEmpty() && nqItems.isEmpty();
  }

  /*
  Add method for allMaterialsListed like this:

  final String displayName;
      if (!isCollectAnyItem()) {
          if (getItemToCollect().getItemMeta() != null) {
              displayName = getItemToCollect().getItemMeta().getDisplayName();
          } else {
              displayName = getItemToCollect().getType().name();
          }
      } else {
          displayName = "Any";
      }

      String itemType = isCollectAnyItem() ? "Any" : getItemToCollect().getType().name();

      if (!displayName.isBlank()) {
          return main.getLanguageManager().getString("chat.objectives.taskDescription.collectItems.base", questPlayer, activeObjective, Map.of(
                  "%ITEMTOCOLLECTTYPE%", itemType,
                  "%ITEMTOCOLLECTNAME%", displayName,
                  "%(%", "(",
                  "%)%", "<RESET>)"
          ));
      } else {
          return main.getLanguageManager().getString("chat.objectives.taskDescription.collectItems.base", questPlayer, activeObjective, Map.of(
                  "%ITEMTOCOLLECTTYPE%", itemType,
                  "%ITEMTOCOLLECTNAME%", "",
                  "%(%", "",
                  "%)%", ""
          ));
      }
   */
}
