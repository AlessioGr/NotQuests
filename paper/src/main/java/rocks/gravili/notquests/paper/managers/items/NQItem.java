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

import java.util.Locale;
import javax.annotation.Nullable;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.managers.data.Category;

public class NQItem {
  private final NotQuests main;
  private final String itemName;
  private final ItemStack itemStack;
  private Category category;

  public NQItem(final NotQuests main, final String itemName, final ItemStack itemStack) {
    this.main = main;
    this.itemName = itemName.toLowerCase(Locale.ROOT);
    this.itemStack = itemStack;
    category = main.getDataManager().getDefaultCategory();
  }

  public final ItemStack getItemStack() {
    return itemStack;
  }

  public final String getItemName() {
    return itemName;
  }

  public final Category getCategory() {
    return category;
  }

  public void setCategory(final Category category) {
    this.category = category;
  }

  public void setDisplayName(@Nullable final String displayName, boolean save) {
    if (displayName == null || displayName.isBlank()) {
      ItemMeta itemMeta = itemStack.getItemMeta();
      itemMeta.displayName(null);
      itemStack.setItemMeta(itemMeta);
      if (save) {
        getCategory().getItemsConfig().set("items." + getItemName() + ".displayName", null);
        getCategory().saveItemsConfig();
      }
      return;
    }
    ItemMeta itemMeta = itemStack.getItemMeta();
    itemMeta.displayName(main.parse("<!italic>" + displayName));
    itemStack.setItemMeta(itemMeta);
    if (save) {
      getCategory().getItemsConfig().set("items." + getItemName() + ".displayName", displayName);
      getCategory().saveItemsConfig();
    }
  }
}
