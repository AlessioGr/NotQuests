/*
 * NotQuests - A Questing plugin for Minecraft Servers
 * Copyright (C) 2021 Alessio Gravili
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

package notquests.notquests.Structs.Objectives;

import notquests.notquests.NotQuests;
import notquests.notquests.Structs.Quest;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class CollectItemsObjective extends Objective {

    private final NotQuests main;
    private final ItemStack itemToCollect;

    public CollectItemsObjective(NotQuests main, final Quest quest, final int objectiveID, ItemStack itemToCollect, int amountToCollect) {
        super(main, quest, objectiveID, amountToCollect);
        this.main = main;
        this.itemToCollect = itemToCollect;
    }

    public CollectItemsObjective(NotQuests main, Quest quest, int objectiveNumber, int progressNeeded) {
        super(main, quest, objectiveNumber, progressNeeded);
        final String questName = quest.getQuestName();

        this.main = main;
        itemToCollect = main.getDataManager().getQuestsData().getItemStack("quests." + questName + ".objectives." + objectiveNumber + ".specifics.itemToCollect.itemstack");
   }

    @Override
    public String getObjectiveTaskDescription(String eventualColor) {
        return "    §7" + eventualColor + "Items to collect: §f" + eventualColor + getItemToCollect().getType() + " (" + getItemToCollect().getItemMeta().getDisplayName() + ")";
    }

    @Override
    public void save() {
        main.getDataManager().getQuestsData().set("quests." + getQuest().getQuestName() + ".objectives." + getObjectiveID() + ".specifics.itemToCollect.itemstack", getItemToCollect());
    }

    public final ItemStack getItemToCollect() {
        return itemToCollect;
    }

    public final long getAmountToCollect() {
        return super.getProgressNeeded();
    }


}
