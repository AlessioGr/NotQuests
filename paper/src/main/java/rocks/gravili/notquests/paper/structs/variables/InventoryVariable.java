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

package rocks.gravili.notquests.paper.structs.variables;

import cloud.commandframework.ArgumentDescription;
import java.util.HashMap;
import java.util.List;
import org.bukkit.inventory.ItemStack;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

public class InventoryVariable extends Variable<ItemStack[]> {
  public InventoryVariable(NotQuests main) {
    super(main);
    setCanSetValue(true);
    addRequiredBooleanFlag(
        main.getCommandManager()
            .getPaperCommandManager()
            .flagBuilder("skipItemIfInventoryFull")
            .withDescription(
                ArgumentDescription.of("Does not drop the item if inventory full if flag set"))
            .build());
  }

  @Override
  public ItemStack[] getValueInternally(QuestPlayer questPlayer, Object... objects) {
    return questPlayer.getPlayer().getInventory().getContents();
  }

  @Override
  public boolean setValueInternally(
      ItemStack[] newValue, QuestPlayer questPlayer, Object... objects) {
    if (getRequiredBooleanValue("add", questPlayer)) {

      HashMap<Integer, ItemStack> left = questPlayer.getPlayer().getInventory().addItem(newValue);
      if (!getRequiredBooleanValue("skipItemIfInventoryFull", questPlayer)) {
        for (ItemStack leftItemStack : left.values()) {
          questPlayer
              .getPlayer()
              .getWorld()
              .dropItem(questPlayer.getPlayer().getLocation(), leftItemStack);
        }
      }
    } else if (getRequiredBooleanValue("remove", questPlayer)) {

      questPlayer.getPlayer().getInventory().removeItemAnySlot(newValue);
    } else {
      questPlayer.getPlayer().getInventory().setContents(newValue);
    }
    return true;
  }

  @Override
  public List<String> getPossibleValues(QuestPlayer questPlayer, Object... objects) {
    return null;
  }

  @Override
  public String getPlural() {
    return "Inventory";
  }

  @Override
  public String getSingular() {
    return "Inventory";
  }
}
