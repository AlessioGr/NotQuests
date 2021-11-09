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

package rocks.gravili.Structs.Objectives;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import rocks.gravili.NotQuests;
import rocks.gravili.Structs.Quest;

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
    public String getObjectiveTaskDescription(final String eventualColor, final Player player) {
        return main.getLanguageManager().getString("chat.objectives.taskDescription.collectItems.base", player)
                .replaceAll("%EVENTUALCOLOR%", eventualColor)
                .replaceAll("%ITEMTOCOLLECTTYPE%", "" + getItemToCollect().getType())
                .replaceAll("%ITEMTOCOLLECTNAME%", "" + getItemToCollect().getItemMeta().getDisplayName());
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
