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

import rocks.gravili.NotQuests;
import rocks.gravili.Structs.Quest;
import org.bukkit.inventory.ItemStack;

public class CraftItemsObjective extends Objective {

    private final NotQuests main;
    private final ItemStack itemToCraft;

    public CraftItemsObjective(NotQuests main, final Quest quest, final int objectiveID, ItemStack itemToCraft, int amountToCraft) {
        super(main, quest, objectiveID, amountToCraft);
        this.main = main;
        this.itemToCraft = itemToCraft;
    }

    public CraftItemsObjective(NotQuests main, Quest quest, int objectiveNumber, int progressNeeded) {
        super(main, quest, objectiveNumber, progressNeeded);
        final String questName = quest.getQuestName();

        this.main = main;
        itemToCraft = main.getDataManager().getQuestsData().getItemStack("quests." + questName + ".objectives." + objectiveNumber + ".specifics.itemToCraft.itemstack");

    }

    @Override
    public String getObjectiveTaskDescription(String eventualColor) {
        return "    §7" + eventualColor + "Items to craft: §f" + eventualColor + getItemToCraft().getType() + " (" + getItemToCraft().getItemMeta().getDisplayName() + ")";
    }

    @Override
    public void save() {
        main.getDataManager().getQuestsData().set("quests." + getQuest().getQuestName() + ".objectives." + getObjectiveID() + ".specifics.itemToCraft.itemstack", getItemToCraft());
    }

    public final ItemStack getItemToCraft() {
        return itemToCraft;
    }

    public final long getAmountToCraft() {
        return super.getProgressNeeded();
    }


}
