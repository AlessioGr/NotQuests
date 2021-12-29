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

package rocks.gravili.notquests.spigot.structs.objectives;

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
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import rocks.gravili.notquests.spigot.NotQuests;
import rocks.gravili.notquests.spigot.commands.NotQuestColors;
import rocks.gravili.notquests.spigot.commands.arguments.MaterialOrHandArgument;
import rocks.gravili.notquests.spigot.commands.arguments.wrappers.MaterialOrHand;
import rocks.gravili.notquests.spigot.structs.ActiveObjective;
import rocks.gravili.notquests.spigot.structs.Quest;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class DeliverItemsObjective extends Objective {

    private ItemStack itemToDeliver = null;
    private int recipientNPCID = -1;
    private UUID recipientArmorStandUUID = null;
    private boolean deliverAnyItem = false;


    //For Citizens NPCs
    public DeliverItemsObjective(NotQuests main) {
        super(main);
    }

    public static void handleCommands(NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> addObjectiveBuilder) {
        manager.command(addObjectiveBuilder.literal("DeliverItems")
                .argument(MaterialOrHandArgument.of("material", main), ArgumentDescription.of("Material of the item which needs to be delivered."))
                .argument(IntegerArgument.<CommandSender>newBuilder("amount").withMin(1), ArgumentDescription.of("Amount of items which need to be delivered."))
                .argument(StringArgument.<CommandSender>newBuilder("NPC or Armorstand").withSuggestionsProvider((context, lastString) -> {
                    ArrayList<String> completions = new ArrayList<>();
                    if (main.getIntegrationsManager().isCitizensEnabled()) {
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

                    boolean deliverAnyItem = false;

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
                        if (materialOrHand.material.equalsIgnoreCase("any")) {
                            deliverAnyItem = true;
                            itemToDeliver = null;
                        } else {
                            itemToDeliver = new ItemStack(Material.valueOf(materialOrHand.material), 1);
                        }
                    }


                    final String npcIDOrArmorstand = context.get("NPC or Armorstand");


                    if (!npcIDOrArmorstand.equalsIgnoreCase("armorstand")) {
                        if (!main.getIntegrationsManager().isCitizensEnabled()) {
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
                        DeliverItemsObjective deliverItemsObjective = new DeliverItemsObjective(main);
                        deliverItemsObjective.setItemToDeliver(itemToDeliver);
                        deliverItemsObjective.setProgressNeeded(amountToDeliver);
                        deliverItemsObjective.setRecipientNPCID(npcID);
                        deliverItemsObjective.setDeliverAnyItem(deliverAnyItem);

                        main.getObjectiveManager().addObjective(deliverItemsObjective, context);
                    } else {//Armorstands
                        if (context.getSender() instanceof Player player) {


                            Random rand = new Random();
                            int randomNum = rand.nextInt((Integer.MAX_VALUE - 1) + 1) + 1;

                            main.getDataManager().getItemStackCache().put(randomNum, itemToDeliver);


                            ItemStack itemStack = new ItemStack(Material.PAPER, 1);
                            //give a specialitem. clicking an armorstand with that special item will remove the pdb.

                            NamespacedKey key = new NamespacedKey(main.getMain(), "notquests-item");
                            NamespacedKey QuestNameKey = new NamespacedKey(main.getMain(), "notquests-questname");

                            NamespacedKey ItemStackKey = new NamespacedKey(main.getMain(), "notquests-itemstackcache");
                            NamespacedKey ItemStackAmountKey = new NamespacedKey(main.getMain(), "notquests-itemstackamount");
                            NamespacedKey deliverAnyKey = new NamespacedKey(main.getMain(), "notquests-anyitemstack");


                            ItemMeta itemMeta = itemStack.getItemMeta();
                            //Only paper List<Component> lore = new ArrayList<>();
                            List<String> lore = new ArrayList<>();

                            assert itemMeta != null;

                            itemMeta.getPersistentDataContainer().set(QuestNameKey, PersistentDataType.STRING, quest.getQuestName());
                            itemMeta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, 7);


                            itemMeta.getPersistentDataContainer().set(ItemStackKey, PersistentDataType.INTEGER, randomNum);
                            itemMeta.getPersistentDataContainer().set(ItemStackAmountKey, PersistentDataType.INTEGER, amountToDeliver);

                            if (deliverAnyItem) {
                                itemMeta.getPersistentDataContainer().set(deliverAnyKey, PersistentDataType.BYTE, (byte) 1);
                            } else {
                                itemMeta.getPersistentDataContainer().set(deliverAnyKey, PersistentDataType.BYTE, (byte) 0);
                            }


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

    public final boolean isDeliverAnyItem() {
        return deliverAnyItem;
    }

    public void setDeliverAnyItem(final boolean deliverAnyItem) {
        this.deliverAnyItem = deliverAnyItem;
    }

    public void setItemToDeliver(final ItemStack itemToDeliver) {
        this.itemToDeliver = itemToDeliver;
    }

    public void setRecipientNPCID(final int recipientNPCID) {
        this.recipientNPCID = recipientNPCID;
    }

    public void setRecipientArmorStandUUID(final UUID recipientArmorStandUUID) {
        this.recipientArmorStandUUID = recipientArmorStandUUID;
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
        if (!isDeliverAnyItem()) {
            if (getItemToDeliver().getItemMeta() != null) {
                displayName = getItemToDeliver().getItemMeta().getDisplayName();
            } else {
                displayName = getItemToDeliver().getType().name();
            }
        } else {
            displayName = "Any";
        }

        String itemType = isDeliverAnyItem() ? "Any" : getItemToDeliver().getType().name();


        String toReturn;
        if (!displayName.isBlank()) {
            toReturn = main.getLanguageManager().getString("chat.objectives.taskDescription.deliverItems.base", player)
                    .replace("%EVENTUALCOLOR%", eventualColor)
                    .replace("%ITEMTODELIVERTYPE%", "" + itemType)
                    .replace("%ITEMTODELIVERNAME%", "" + displayName)
                    .replace("%(%", "(")
                    .replace("%)%", "<RESET>)");
        } else {
            toReturn = main.getLanguageManager().getString("chat.objectives.taskDescription.deliverItems.base", player)
                    .replace("%EVENTUALCOLOR%", eventualColor)
                    .replace("%ITEMTODELIVERTYPE%", "" + itemType)
                    .replace("%ITEMTODELIVERNAME%", "")
                    .replace("%(%", "")
                    .replace("%)%", "");
        }


        if (main.getIntegrationsManager().isCitizensEnabled() && getRecipientNPCID() != -1) {
            final NPC npc = CitizensAPI.getNPCRegistry().getById(getRecipientNPCID());
            if (npc != null) {
                toReturn += "\n      <GRAY>" + eventualColor + "Deliver it to <WHITE>" + eventualColor + npc.getName();
            } else {
                toReturn += "\n      <GRAY>" + eventualColor + "The delivery NPC is currently not available!";
            }
        } else {

            if (getRecipientNPCID() != -1) {
                toReturn += "    <RED>Error: Citizens plugin not installed. Contact an admin.";
            } else { //Armor Stands
                final UUID armorStandUUID = getRecipientArmorStandUUID();
                if (armorStandUUID != null) {
                    toReturn += "    <GRAY>" + eventualColor + "Deliver it to <WHITE>" + eventualColor + main.getArmorStandManager().getArmorStandName(armorStandUUID);
                } else {
                    toReturn += "    <GRAY>" + eventualColor + "The target Armor Stand is currently not available!";
                }
            }

        }
        return toReturn;
    }

    @Override
    public void save(FileConfiguration configuration, String initialPath) {
        configuration.set(initialPath + ".specifics.itemToCollect.itemstack", getItemToDeliver());

        configuration.set(initialPath + ".specifics.recipientNPCID", getRecipientNPCID());
        if (getRecipientArmorStandUUID() != null) {
            configuration.set(initialPath + ".specifics.recipientArmorStandID", getRecipientArmorStandUUID().toString());
        } else {
            configuration.set(initialPath + ".specifics.recipientArmorStandID", null);
        }
        configuration.set(initialPath + ".specifics.deliverAnyItem", isDeliverAnyItem());
    }

    @Override
    public void load(FileConfiguration configuration, String initialPath) {
        itemToDeliver = configuration.getItemStack(initialPath + ".specifics.itemToCollect.itemstack");
        recipientNPCID = configuration.getInt(initialPath + ".specifics.recipientNPCID");

        if (recipientNPCID != -1) {
            recipientArmorStandUUID = null;
        } else {
            final String armorStandUUIDString = configuration.getString(initialPath + ".specifics.recipientArmorStandID");
            if (armorStandUUIDString != null) {
                recipientArmorStandUUID = UUID.fromString(armorStandUUIDString);
            } else {
                recipientArmorStandUUID = null;
            }
        }

        deliverAnyItem = configuration.getBoolean(initialPath + ".specifics.deliverAnyItem", false);

    }
}