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
import cloud.commandframework.paper.PaperCommandManager;
import java.util.Map;
import java.util.UUID;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.Nullable;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.arguments.ItemStackSelectionArgument;
import rocks.gravili.notquests.paper.commands.arguments.NQNPCSelector;
import rocks.gravili.notquests.paper.commands.arguments.variables.NumberVariableValueArgument;
import rocks.gravili.notquests.paper.commands.arguments.wrappers.ItemStackSelection;
import rocks.gravili.notquests.paper.commands.arguments.wrappers.NQNPCResult;
import rocks.gravili.notquests.paper.managers.npc.NQNPC;
import rocks.gravili.notquests.paper.managers.npc.NQNPCID;
import rocks.gravili.notquests.paper.structs.ActiveObjective;
import rocks.gravili.notquests.paper.structs.Quest;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

public class DeliverItemsObjective extends Objective {

    private NQNPC recipientNPC;
    private ItemStackSelection itemStackSelection;



    //For Citizens NPCs
    public DeliverItemsObjective(NotQuests main) {
        super(main);
    }

    public static void handleCommands(NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> addObjectiveBuilder,
        final int level) {
        manager.command(addObjectiveBuilder
                .argument(ItemStackSelectionArgument.of("materials", main), ArgumentDescription.of("Material of the item which needs to be delivered"))
                .argument(NumberVariableValueArgument.newBuilder("amount", main, null), ArgumentDescription.of("Amount of items which need to be delivered"))
                .argument(NQNPCSelector.of("NPC", main, false, true), ArgumentDescription.of("NPC to whom the items should be delivered."))
                .handler((context) -> {
                    final Quest quest = context.get("quest");
                    final String amountToDeliverExpression = context.get("amount");


                    final ItemStackSelection itemStackSelection = context.get("materials");


                    final NQNPCResult nqNPCResult = context.get("NPC");

                    if (nqNPCResult.isRightClickSelect()) {//Armor Stands
                        if (context.getSender() instanceof final Player player) {
                            main.getNPCManager().handleRightClickNQNPCSelectionWithAction(
                                (nqNPC) -> {
                                    final DeliverItemsObjective deliverItemsObjective = new DeliverItemsObjective(main);
                                    deliverItemsObjective.setItemStackSelection(itemStackSelection);

                                    deliverItemsObjective.setProgressNeededExpression(amountToDeliverExpression);
                                    deliverItemsObjective.setRecipientNPC(nqNPC);

                                    main.getObjectiveManager().addObjective(deliverItemsObjective, context, level);
                                },
                                player,
                                "<success>You have been given an item with which you can add the DeliverItems Objective to an NPC by rightclicking the NPC. Check your inventory!",
                                "<LIGHT_PURPLE>Add DeliverItems Objective to NPC",
                                "<WHITE>Right-click an NPC to add the following objective to it:",
                                "<YELLOW>DeliverItems <WHITE>Objective of Quest <highlight>" + quest.getIdentifier()  + "</highlight>."
                            );

                        } else {
                            context.getSender().sendMessage(main.parse("<error>Error: this command can only be run as a player."));
                        }
                    }else {
                        final NQNPC nqNPC = nqNPCResult.getNQNPC();

                        final DeliverItemsObjective deliverItemsObjective = new DeliverItemsObjective(main);
                        deliverItemsObjective.setItemStackSelection(itemStackSelection);

                        deliverItemsObjective.setProgressNeededExpression(amountToDeliverExpression);
                        deliverItemsObjective.setRecipientNPC(nqNPC);

                        main.getObjectiveManager().addObjective(deliverItemsObjective, context, level);
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

    public final NQNPC getRecipientNPC() {
        return recipientNPC;
    }

    public void setRecipientNPC(final NQNPC recipientNPC) {
        this.recipientNPC = recipientNPC;
    }

    @Override
    public String getTaskDescriptionInternal(final QuestPlayer questPlayer, final @Nullable ActiveObjective activeObjective) {
        String toReturn;
        toReturn = main.getLanguageManager().getString("chat.objectives.taskDescription.deliverItems.base", questPlayer, activeObjective, Map.of(
                "%ITEMTODELIVERTYPE%", getItemStackSelection().getAllMaterialsListedTranslated("main"),
                "%ITEMTODELIVERNAME%", "",
                "%(%", "",
                "%)%", ""
        ));

        if (recipientNPC != null) {;
            toReturn += "\n" + main.getLanguageManager().getString("chat.objectives.taskDescription.deliverItems.deliver-to-npc", questPlayer, activeObjective, Map.of(
                "%NPCNAME%", recipientNPC.getName() != null ? recipientNPC.getName() : recipientNPC.getID().getEitherAsString()
            ));
        } else {
            toReturn += "\n" + main.getLanguageManager().getString("chat.objectives.taskDescription.deliverItems.deliver-to-npc-not-available", questPlayer, activeObjective);
        }
        return toReturn;
    }

    @Override
    public void save(FileConfiguration configuration, String initialPath) {
        getItemStackSelection().saveToFileConfiguration(configuration, initialPath + ".specifics.itemStackSelection");
        recipientNPC.saveToConfig(configuration, initialPath + ".specifics.recipientNPC");
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

        recipientNPC = NQNPC.fromConfig(main, configuration, initialPath + ".specifics.recipientNPC");

        try{
            if (recipientNPC == null) { //Convert
                recipientNPC = main.getNPCManager().getOrCreateNQNpc("citizens", NQNPCID.fromInteger(configuration.getInt(initialPath + ".specifics.recipientNPCID")));

                if (recipientNPC == null) {
                    recipientNPC = main.getNPCManager().getOrCreateNQNpc("armorstand", NQNPCID.fromUUID(UUID.fromString(configuration.getString(initialPath + ".specifics.recipientArmorStandID", ""))));
                }
            }
        }catch (Exception e){
            main.getLogManager().warn("Some error happened when reading/converting NqNPC (which was null) for DeliverItemsObjective (Objective Holder: <highlight>%s</highlight>, config path: <highlight>%s</highlight>)", getObjectiveHolder().getIdentifier(), initialPath);
            if(main.getConfiguration().debug){
                e.printStackTrace();
            }
        }


    }
}