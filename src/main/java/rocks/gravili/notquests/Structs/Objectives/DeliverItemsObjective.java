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

package rocks.gravili.notquests.Structs.Objectives;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import rocks.gravili.notquests.NotQuests;
import rocks.gravili.notquests.Structs.Quest;

import java.util.UUID;

public class DeliverItemsObjective extends Objective {

    private final NotQuests main;
    private final ItemStack itemToDeliver;
    private final int recipientNPCID;
    private final UUID recipientArmorStandUUID;

    //For Citizens NPCs
    public DeliverItemsObjective(NotQuests main, final Quest quest, final int objectiveID, final ItemStack itemToDeliver, final int amountToDeliver, final int recipientNPCID) {
        super(main, quest, objectiveID,amountToDeliver);
        this.main = main;
        this.itemToDeliver = itemToDeliver;
        this.recipientNPCID = recipientNPCID;
        this.recipientArmorStandUUID = null;
    }

    //For Armor Stands
    public DeliverItemsObjective(NotQuests main, final Quest quest, final int objectiveID, final ItemStack itemToDeliver, final int amountToDeliver, final UUID recipientArmorStandUUID) {
        super(main, quest, objectiveID, amountToDeliver);
        this.main = main;
        this.itemToDeliver = itemToDeliver;
        this.recipientNPCID = -1;
        this.recipientArmorStandUUID = recipientArmorStandUUID;
    }


    public DeliverItemsObjective(NotQuests main, Quest quest, int objectiveNumber, int progressNeeded) {
        super(main, quest, objectiveNumber, progressNeeded);
        final String questName = quest.getQuestName();
        this.main = main;

        itemToDeliver = main.getDataManager().getQuestsData().getItemStack("quests." + questName + ".objectives." + objectiveNumber + ".specifics.itemToCollect.itemstack");
        recipientNPCID = main.getDataManager().getQuestsData().getInt("quests." + questName + ".objectives." + objectiveNumber + ".specifics.recipientNPCID");

        if (recipientNPCID != -1) {
            recipientArmorStandUUID = null;
        } else {
            final String armorStandUUIDString = main.getDataManager().getQuestsData().getString("quests." + questName + ".objectives." + objectiveNumber + ".specifics.recipientArmorStandID");
            if (armorStandUUIDString != null) {
                recipientArmorStandUUID = UUID.fromString(armorStandUUIDString);
            } else {
                recipientArmorStandUUID = null;
            }
        }
    }

    @Override
    public String getObjectiveTaskDescription(final String eventualColor, final Player player) {
        String toReturn = main.getLanguageManager().getString("chat.objectives.taskDescription.deliverItems.base", player)
                .replaceAll("%EVENTUALCOLOR%", eventualColor)
                .replaceAll("%ITEMTODELIVERTYPE%", "" + getItemToDeliver().getType())
                .replaceAll("%ITEMTODELIVERNAME%", "" + getItemToDeliver().getItemMeta().getDisplayName());

        if (main.isCitizensEnabled() && getRecipientNPCID() != -1) {
            final NPC npc = CitizensAPI.getNPCRegistry().getById(getRecipientNPCID());
            if (npc != null) {
                toReturn += "\n      §7" + eventualColor + "Deliver it to §f" + eventualColor + npc.getName();
            } else {
                toReturn += "\n      §7" + eventualColor + "The delivery NPC is currently not available!";
            }
        } else {

            if (getRecipientNPCID() != -1) {
                toReturn += "    §cError: Citizens plugin not installed. Contact an admin.";
            } else { //Armor Stands
                final UUID armorStandUUID = getRecipientArmorStandUUID();
                if (armorStandUUID != null) {
                    toReturn += "    §7" + eventualColor + "Deliver it to §f" + eventualColor + main.getArmorStandManager().getArmorStandName(armorStandUUID);
                } else {
                    toReturn += "    §7" + eventualColor + "The target Armor Stand is currently not available!";
                }
            }

        }
        return toReturn;
    }

    @Override
    public void save() {
        main.getDataManager().getQuestsData().set("quests." + getQuest().getQuestName() + ".objectives." + getObjectiveID() + ".specifics.itemToCollect.itemstack", getItemToDeliver());

        main.getDataManager().getQuestsData().set("quests." + getQuest().getQuestName() + ".objectives." + getObjectiveID() + ".specifics.recipientNPCID", getRecipientNPCID());
        if (getRecipientArmorStandUUID() != null) {
            main.getDataManager().getQuestsData().set("quests." + getQuest().getQuestName() + ".objectives." + getObjectiveID() + ".specifics.recipientArmorStandID", getRecipientArmorStandUUID().toString());
        } else {
            main.getDataManager().getQuestsData().set("quests." + getQuest().getQuestName() + ".objectives." + getObjectiveID() + ".specifics.recipientArmorStandID", null);
        }
    }

    public final ItemStack getItemToDeliver() {
        return itemToDeliver;
    }

    //Probably never used, because we use the objective progress instead
    public final long getAmountToDeliver() {
        return super.getProgressNeeded();
    }

    public final int getRecipientNPCID() {
        return recipientNPCID;
    }

    public final UUID getRecipientArmorStandUUID() {
        return recipientArmorStandUUID;
    }


}