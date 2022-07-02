/*
 * NotQuests - A Questing plugin for Minecraft Servers
 * Copyright (C) 2021-2022 Alessio Gravili
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

package rocks.gravili.notquests.paper.structs.objectives;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.paper.PaperCommandManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.checkerframework.checker.nullness.qual.Nullable;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.arguments.ItemStackSelectionArgument;
import rocks.gravili.notquests.paper.commands.arguments.variables.NumberVariableValueArgument;
import rocks.gravili.notquests.paper.commands.arguments.wrappers.ItemStackSelection;
import rocks.gravili.notquests.paper.structs.ActiveObjective;
import rocks.gravili.notquests.paper.structs.Quest;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

public class DeliverItemsObjective extends Objective {

    private int recipientNPCID = -1;
    private UUID recipientArmorStandUUID = null;
    private ItemStackSelection itemStackSelection;



    //For Citizens NPCs
    public DeliverItemsObjective(NotQuests main) {
        super(main);
    }

    public static void handleCommands(NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> addObjectiveBuilder) {
        manager.command(addObjectiveBuilder
                .argument(ItemStackSelectionArgument.of("materials", main), ArgumentDescription.of("Material of the item which needs to be delivered"))
                .argument(NumberVariableValueArgument.newBuilder("amount", main, null), ArgumentDescription.of("Amount of items which need to be delivered"))
                .argument(StringArgument.<CommandSender>newBuilder("NPC or Armorstand").withSuggestionsProvider((context, lastString) -> {
                    ArrayList<String> completions = new ArrayList<>();
                    if (main.getIntegrationsManager().isCitizensEnabled()) {
                        for (final NPC npc : CitizensAPI.getNPCRegistry().sorted()) {
                            completions.add("" + npc.getId());
                        }
                    }
                    completions.add("armorstand");
                    final List<String> allArgs = context.getRawInput();
                    main.getUtilManager().sendFancyCommandCompletion(context.getSender(), allArgs.toArray(new String[0]), "[Recipient NPC ID / 'armorstand']", "");

                    return completions;
                }).build(), ArgumentDescription.of("ID of the Citizens NPC or 'armorstand' to whom the items should be delivered."))
                .handler((context) -> {
                    final Quest quest = context.get("quest");
                    final String amountToDeliverExpression = context.get("amount");


                    final ItemStackSelection itemStackSelection = context.get("materials");


                    final String npcIDOrArmorstand = context.get("NPC or Armorstand");


                    if (!npcIDOrArmorstand.equalsIgnoreCase("armorstand")) {
                        if (!main.getIntegrationsManager().isCitizensEnabled()) {
                            context.getSender().sendMessage(main.parse(
                                    "<error>Error: Any kind of NPC stuff has been disabled, because you don't have the Citizens plugin installed on your server. You need to install the Citizens plugin in order to use Citizen NPCs. You can, however, use armor stands as an alternative. To do that, just enter 'armorstand' instead of the NPC ID."
                            ));
                            return;
                        }
                        int npcID;
                        try {
                            npcID = Integer.parseInt(npcIDOrArmorstand);
                        } catch (NumberFormatException e) {
                            context.getSender().sendMessage(
                                    main.parse(
                                            "<error>Invalid NPC ID."
                                    )
                            );
                            return;
                        }
                        DeliverItemsObjective deliverItemsObjective = new DeliverItemsObjective(main);
                        deliverItemsObjective.setItemStackSelection(itemStackSelection);

                        deliverItemsObjective.setProgressNeededExpression(amountToDeliverExpression);
                        deliverItemsObjective.setRecipientNPCID(npcID);

                        main.getObjectiveManager().addObjective(deliverItemsObjective, context);
                    } else {//Armorstands
                        if (context.getSender() instanceof Player player) {


                            Random rand = new Random();
                            int randomNum = rand.nextInt((Integer.MAX_VALUE - 1) + 1) + 1;

                            main.getDataManager().getItemStackSelectionCache().put(randomNum, itemStackSelection);




                            ItemStack itemStack = new ItemStack(Material.PAPER, 1);
                            //give a specialitem. clicking an armorstand with that special item will remove the pdb.

                            NamespacedKey key = new NamespacedKey(main.getMain(), "notquests-item");
                            NamespacedKey QuestNameKey = new NamespacedKey(main.getMain(), "notquests-questname");

                            NamespacedKey ItemStackKey = new NamespacedKey(main.getMain(), "notquests-itemstackcache");
                            NamespacedKey ItemStackAmountKey = new NamespacedKey(main.getMain(), "notquests-itemstackamount");


                            ItemMeta itemMeta = itemStack.getItemMeta();
                            List<Component> lore = new ArrayList<>();

                            assert itemMeta != null;

                            itemMeta.getPersistentDataContainer().set(QuestNameKey, PersistentDataType.STRING, quest.getQuestName());
                            itemMeta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, 7);


                            itemMeta.getPersistentDataContainer().set(ItemStackKey, PersistentDataType.INTEGER, randomNum);
                            itemMeta.getPersistentDataContainer().set(ItemStackAmountKey, PersistentDataType.STRING, amountToDeliverExpression);


                            itemMeta.displayName(main.parse(
                                    "<LIGHT_PURPLE>Add DeliverItems Objective to Armor Stand"
                            ));

                            lore.add(main.parse(
                                    "<WHITE>Right-click an Armor Stand to add the following objective to it:"
                            ));
                            lore.add(main.parse(
                                    "<YELLOW>DeliverItems <WHITE>Objective of Quest <highlight>" + quest.getQuestName() + "</highlight>."
                            ));

                            itemMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

                            itemMeta.lore(lore);

                            itemStack.setItemMeta(itemMeta);

                            player.getInventory().addItem(itemStack);

                            context.getSender().sendMessage(
                                    main.parse(
                                            "<success>You have been given an item with which you can add the DeliverItems Objective to an armor stand. Check your inventory!"
                                    )
                            );


                        } else {
                            context.getSender().sendMessage(
                                    main.parse(
                                            "<error>Must be a player!"
                                    )
                            );
                        }
                    }


                }));
    }

    public final ItemStackSelection getItemStackSelection(){
        return itemStackSelection;
    }

    public void setItemStackSelection(final ItemStackSelection itemStackSelection){
        this.itemStackSelection = itemStackSelection;
    }

    @Override
    public void onObjectiveUnlock(final ActiveObjective activeObjective, final boolean unlockedDuringPluginStartupQuestLoadingProcess) {
    }

    @Override
    public void onObjectiveCompleteOrLock(final ActiveObjective activeObjective, final boolean lockedOrCompletedDuringPluginStartupQuestLoadingProcess, final boolean completed) {
    }

    public final int getRecipientNPCID() {
        return recipientNPCID;
    }

    public void setRecipientNPCID(final int recipientNPCID) {
        this.recipientNPCID = recipientNPCID;
    }

    public final UUID getRecipientArmorStandUUID() {
        return recipientArmorStandUUID;
    }

    public void setRecipientArmorStandUUID(final UUID recipientArmorStandUUID) {
        this.recipientArmorStandUUID = recipientArmorStandUUID;
    }

    @Override
    public String getObjectiveTaskDescription(final QuestPlayer questPlayer, final @Nullable ActiveObjective activeObjective) {


        String toReturn;
        toReturn = main.getLanguageManager().getString("chat.objectives.taskDescription.deliverItems.base", questPlayer, activeObjective, Map.of(
                "%ITEMTODELIVERTYPE%", getItemStackSelection().getAllMaterialsListed(),
                "%ITEMTODELIVERNAME%", "",
                "%(%", "",
                "%)%", ""
        ));


        if (main.getIntegrationsManager().isCitizensEnabled() && getRecipientNPCID() != -1) {
            final NPC npc = CitizensAPI.getNPCRegistry().getById(getRecipientNPCID());
            if (npc != null) {
                final String mmNpcName = main.getMiniMessage().serialize(LegacyComponentSerializer.legacyAmpersand().deserialize(npc.getName().replace("§","&")));

                toReturn += "\n" + main.getLanguageManager().getString("chat.objectives.taskDescription.deliverItems.deliver-to-npc", questPlayer, activeObjective, Map.of(
                        "%NPCNAME%", mmNpcName
                ));
            } else {
                toReturn += "\n" + main.getLanguageManager().getString("chat.objectives.taskDescription.deliverItems.deliver-to-npc-not-available", questPlayer, activeObjective);
            }
        } else {

            if (getRecipientNPCID() != -1) {
                toReturn += main.getLanguageManager().getString("chat.objectives.taskDescription.deliverItems.deliver-to-npc-citizens-not-found", questPlayer, activeObjective);
            } else { //Armor Stands
                final UUID armorStandUUID = getRecipientArmorStandUUID();
                if (armorStandUUID != null) {
                    toReturn += "\n" + main.getLanguageManager().getString("chat.objectives.taskDescription.deliverItems.deliver-to-armorstand", questPlayer, activeObjective, Map.of(
                            "%ARMORSTANDNAME%", main.getArmorStandManager().getArmorStandName(armorStandUUID)
                    ));
                } else {
                    toReturn += "\n" + main.getLanguageManager().getString("chat.objectives.taskDescription.deliverItems.deliver-to-armorstand-not-available", questPlayer, activeObjective);
                }
            }

        }
        return toReturn;
    }

    @Override
    public void save(FileConfiguration configuration, String initialPath) {
        getItemStackSelection().saveToFileConfiguration(configuration, initialPath + ".specifics.itemStackSelection");

        configuration.set(initialPath + ".specifics.recipientNPCID", getRecipientNPCID());
        if (getRecipientArmorStandUUID() != null) {
            configuration.set(initialPath + ".specifics.recipientArmorStandID", getRecipientArmorStandUUID().toString());
        } else {
            configuration.set(initialPath + ".specifics.recipientArmorStandID", null);
        }
    }

    @Override
    public void load(FileConfiguration configuration, String initialPath) {
        this.itemStackSelection = new ItemStackSelection(main);
        itemStackSelection.loadFromFileConfiguration(configuration, initialPath + ".specifics.itemStackSelection");

        //Convert old to new
        if(configuration.contains(initialPath + ".specifics.nqitem") || configuration.contains(initialPath + ".specifics.itemToCollect.itemstack")){
            main.getLogManager().info("Converting old DeliverItemsObjective to new one...");
            final String nqItemName = configuration.getString(initialPath + ".specifics.nqitem", "");

            if(nqItemName.isBlank()){
                itemStackSelection.addItemStack(configuration.getItemStack(initialPath + ".specifics.itemToCollect.itemstack"));
            }else{
                itemStackSelection.addNqItemName(nqItemName);
            }
            itemStackSelection.saveToFileConfiguration(configuration, initialPath + ".specifics.itemStackSelection");
            configuration.set(initialPath + ".specifics.nqitem", null);
            configuration.set(initialPath + ".specifics.itemToCollect.itemstack", null);
            //Let's hope it saves somewhere, else conversion will happen again...
        }

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
    }
}