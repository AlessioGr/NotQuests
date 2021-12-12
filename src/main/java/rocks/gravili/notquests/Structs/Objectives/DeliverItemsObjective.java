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

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.paper.PaperCommandManager;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import rocks.gravili.notquests.Commands.NotQuestColors;
import rocks.gravili.notquests.Commands.newCMDs.arguments.MaterialOrHandArgument;
import rocks.gravili.notquests.Commands.newCMDs.arguments.wrappers.MaterialOrHand;
import rocks.gravili.notquests.NotQuests;
import rocks.gravili.notquests.Structs.ActiveObjective;
import rocks.gravili.notquests.Structs.Quest;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
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

        itemToDeliver = main.getDataManager().getQuestsConfig().getItemStack("quests." + questName + ".objectives." + objectiveNumber + ".specifics.itemToCollect.itemstack");
        recipientNPCID = main.getDataManager().getQuestsConfig().getInt("quests." + questName + ".objectives." + objectiveNumber + ".specifics.recipientNPCID");

        if (recipientNPCID != -1) {
            recipientArmorStandUUID = null;
        } else {
            final String armorStandUUIDString = main.getDataManager().getQuestsConfig().getString("quests." + questName + ".objectives." + objectiveNumber + ".specifics.recipientArmorStandID");
            if (armorStandUUIDString != null) {
                recipientArmorStandUUID = UUID.fromString(armorStandUUIDString);
            } else {
                recipientArmorStandUUID = null;
            }
        }
    }

    public static void handleCommands(NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> addObjectiveBuilder) {
        manager.command(addObjectiveBuilder.literal("DeliverItems")
                .argument(MaterialOrHandArgument.of("material", main), ArgumentDescription.of("Material of the item which needs to be delivered."))
                .argument(IntegerArgument.<CommandSender>newBuilder("amount").withMin(1), ArgumentDescription.of("Amount of items which need to be delivered."))
                .argument(StringArgument.<CommandSender>newBuilder("NPC or Armorstand").withSuggestionsProvider((context, lastString) -> {
                    ArrayList<String> completions = new ArrayList<>();
                    if (main.isCitizensEnabled()) {
                        for (final NPC npc : CitizensAPI.getNPCRegistry().sorted()) {
                            completions.add("" + npc.getId());
                        }
                    }

                    completions.add("armorstand");
                    final List<String> allArgs = context.getRawInput();
                    final Audience audience = main.adventure().sender(context.getSender());
                    main.getUtilManager().sendFancyCommandCompletion(audience, allArgs.toArray(new String[0]), "[Recipient NPC ID / 'armorstand']", "");

                    return completions;
                }).build(), ArgumentDescription.of("ID of the Citizens NPC or 'armorstand' to whom the items should be delivered."))
                .meta(CommandMeta.DESCRIPTION, "Adds a new DeliverItems Objective to a quest.")
                .handler((context) -> {
                    final Audience audience = main.adventure().sender(context.getSender());
                    final Quest quest = context.get("quest");
                    final int amountToDeliver = context.get("amount");

                    final MaterialOrHand materialOrHand = context.get("material");
                    ItemStack itemToDeliver;
                    if (materialOrHand.hand) { //"hand"
                        if (context.getSender() instanceof Player player) {
                            itemToDeliver = player.getInventory().getItemInMainHand();
                        } else {
                            audience.sendMessage(MiniMessage.miniMessage().parse(
                                    NotQuestColors.errorGradient + "This must be run by a player."
                            ));
                            return;
                        }
                    } else {
                        itemToDeliver = new ItemStack(materialOrHand.material, 1);
                    }


                    final String npcIDOrArmorstand = context.get("NPC or Armorstand");


                    if (!npcIDOrArmorstand.equalsIgnoreCase("armorstand")) {
                        if (!main.isCitizensEnabled()) {
                            audience.sendMessage(MiniMessage.miniMessage().parse(
                                    NotQuestColors.errorGradient + "Error: Any kind of NPC stuff has been disabled, because you don't have the Citizens plugin installed on your server. You need to install the Citizens plugin in order to use Citizen NPCs. You can, however, use armor stands as an alternative. To do that, just enter 'armorstand' instead of the NPC ID."
                            ));
                            return;
                        }
                        int npcID;
                        try {
                            npcID = Integer.parseInt(npcIDOrArmorstand);
                        } catch (NumberFormatException e) {
                            audience.sendMessage(
                                    MiniMessage.miniMessage().parse(
                                            NotQuestColors.errorGradient + "Invalid NPC ID."
                                    )
                            );
                            return;
                        }
                        DeliverItemsObjective deliverItemsObjective = new DeliverItemsObjective(main, quest, quest.getObjectives().size() + 1, itemToDeliver, amountToDeliver, npcID);

                        quest.addObjective(deliverItemsObjective, true);
                        audience.sendMessage(MiniMessage.miniMessage().parse(
                                NotQuestColors.successGradient + "DeliverItems Objective successfully added to Quest " + NotQuestColors.highlightGradient
                                        + quest.getQuestName() + "</gradient>!</gradient>"
                        ));
                    } else {//Armorstands
                        if (context.getSender() instanceof Player player) {


                            Random rand = new Random();
                            int randomNum = rand.nextInt((Integer.MAX_VALUE - 1) + 1) + 1;

                            main.getDataManager().getItemStackCache().put(randomNum, itemToDeliver);


                            ItemStack itemStack = new ItemStack(Material.PAPER, 1);
                            //give a specialitem. clicking an armorstand with that special item will remove the pdb.

                            NamespacedKey key = new NamespacedKey(main, "notquests-item");
                            NamespacedKey QuestNameKey = new NamespacedKey(main, "notquests-questname");

                            NamespacedKey ItemStackKey = new NamespacedKey(main, "notquests-itemstackcache");
                            NamespacedKey ItemStackAmountKey = new NamespacedKey(main, "notquests-itemstackamount");

                            ItemMeta itemMeta = itemStack.getItemMeta();
                            //Only paper List<Component> lore = new ArrayList<>();
                            List<String> lore = new ArrayList<>();

                            assert itemMeta != null;

                            itemMeta.getPersistentDataContainer().set(QuestNameKey, PersistentDataType.STRING, quest.getQuestName());
                            itemMeta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, 7);


                            itemMeta.getPersistentDataContainer().set(ItemStackKey, PersistentDataType.INTEGER, randomNum);
                            itemMeta.getPersistentDataContainer().set(ItemStackAmountKey, PersistentDataType.INTEGER, amountToDeliver);


                            //Only paper itemMeta.displayName(Component.text("§dCheck Armor Stand", NamedTextColor.LIGHT_PURPLE));
                            itemMeta.setDisplayName("§dAdd DeliverItems Objective to Armor Stand");
                            //Only paper lore.add(Component.text("§fRight-click an Armor Stand to see which Quests are attached to it."));
                            lore.add("§fRight-click an Armor Stand to add the following objective to it:");
                            lore.add("§eDeliverItems §fObjective of Quest §b" + quest.getQuestName() + "§f.");

                            itemMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                            //Only paper itemMeta.lore(lore);

                            itemMeta.setLore(lore);
                            itemStack.setItemMeta(itemMeta);

                            player.getInventory().addItem(itemStack);

                            audience.sendMessage(
                                    MiniMessage.miniMessage().parse(
                                            NotQuestColors.successGradient + "You have been given an item with which you can add the DeliverItems Objective to an armor stand. Check your inventory!"
                                    )
                            );


                        } else {
                            audience.sendMessage(
                                    MiniMessage.miniMessage().parse(
                                            NotQuestColors.errorGradient + "Must be a player!"
                                    )
                            );
                        }
                    }


                }));
    }

    @Override
    public void save() {
        main.getDataManager().getQuestsConfig().set("quests." + getQuest().getQuestName() + ".objectives." + getObjectiveID() + ".specifics.itemToCollect.itemstack", getItemToDeliver());

        main.getDataManager().getQuestsConfig().set("quests." + getQuest().getQuestName() + ".objectives." + getObjectiveID() + ".specifics.recipientNPCID", getRecipientNPCID());
        if (getRecipientArmorStandUUID() != null) {
            main.getDataManager().getQuestsConfig().set("quests." + getQuest().getQuestName() + ".objectives." + getObjectiveID() + ".specifics.recipientArmorStandID", getRecipientArmorStandUUID().toString());
        } else {
            main.getDataManager().getQuestsConfig().set("quests." + getQuest().getQuestName() + ".objectives." + getObjectiveID() + ".specifics.recipientArmorStandID", null);
        }
    }

    @Override
    public void onObjectiveUnlock(final ActiveObjective activeObjective) {

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

    @Override
    public String getObjectiveTaskDescription(final String eventualColor, final Player player) {
        final String displayName;
        if (getItemToDeliver().getItemMeta() != null) {
            displayName = getItemToDeliver().getItemMeta().getDisplayName();
        } else {
            displayName = getItemToDeliver().getType().name();
        }
        String toReturn;
        if (!displayName.isBlank()) {
            toReturn = main.getLanguageManager().getString("chat.objectives.taskDescription.deliverItems.base", player)
                    .replace("%EVENTUALCOLOR%", eventualColor)
                    .replace("%ITEMTODELIVERTYPE%", "" + getItemToDeliver().getType())
                    .replace("%ITEMTODELIVERNAME%", "" + displayName)
                    .replace("%(%", "(")
                    .replace("%)%", "§f)");
        } else {
            toReturn = main.getLanguageManager().getString("chat.objectives.taskDescription.deliverItems.base", player)
                    .replace("%EVENTUALCOLOR%", eventualColor)
                    .replace("%ITEMTODELIVERTYPE%", "" + getItemToDeliver().getType())
                    .replace("%ITEMTODELIVERNAME%", "")
                    .replace("%(%", "")
                    .replace("%)%", "");
        }


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
}