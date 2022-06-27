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
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.paper.PaperCommandManager;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.arguments.MaterialOrHandArgument;
import rocks.gravili.notquests.paper.commands.arguments.variables.NumberVariableValueArgument;
import rocks.gravili.notquests.paper.commands.arguments.wrappers.MaterialOrHand;
import rocks.gravili.notquests.paper.structs.ActiveObjective;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

import java.util.Map;

public class CollectItemsObjective extends Objective {

    private ItemStack itemToCollect = null;
    private boolean deductIfItemIsDropped = true;
    private boolean collectAnyItem = false;
    private String nqItemName = "";

    public CollectItemsObjective(NotQuests main) {
        super(main);
    }

    public void setNQItem(final String nqItemName){
        this.nqItemName = nqItemName;
    }
    public final String getNQItem(){
        return nqItemName;
    }


    public static void handleCommands(NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> addObjectiveBuilder) {
        manager.command(addObjectiveBuilder
                .argument(MaterialOrHandArgument.of("material", main), ArgumentDescription.of("Material of the item which needs to be collected."))
                .argument(NumberVariableValueArgument.newBuilder("amount", main, null), ArgumentDescription.of("Amount of items which need to be collected"))
                .flag(
                        manager.flagBuilder("doNotDeductIfItemIsDropped")
                                .withDescription(ArgumentDescription.of("Makes it so Quest progress is not removed if the item is dropped."))
                )
                .handler((context) -> {
                    final String amountExpression = context.get("amount");
                    final boolean deductIfItemIsDropped = !context.flags().isPresent("doNotDeductIfItemIsDropped");

                    boolean collectAnyItem = false;

                    final MaterialOrHand materialOrHand = context.get("material");
                    ItemStack itemToCollect;
                    if (materialOrHand.material.equalsIgnoreCase("any")) {
                        collectAnyItem = true;
                        itemToCollect = null;
                    } else {
                        itemToCollect = main.getItemsManager().getItemStack(materialOrHand);
                    }

                    CollectItemsObjective collectItemsObjective = new CollectItemsObjective(main);


                    if(main.getItemsManager().getItem(materialOrHand.material) != null){
                        collectItemsObjective.setNQItem(main.getItemsManager().getItem(materialOrHand.material).getItemName());
                    }else{
                        collectItemsObjective.setItemToCollect(itemToCollect);
                    }



                    collectItemsObjective.setCollectAnyItem(collectAnyItem);
                    collectItemsObjective.setProgressNeededExpression(amountExpression);
                    collectItemsObjective.setDeductIfItemIsDropped(deductIfItemIsDropped);

                    main.getObjectiveManager().addObjective(collectItemsObjective, context);
                }));
    }

    public final boolean isCollectAnyItem() {
        return collectAnyItem;
    }

    public void setCollectAnyItem(final boolean collectAnyItem) {
        this.collectAnyItem = collectAnyItem;
    }

    public void setItemToCollect(final ItemStack itemToCollect) {
        this.itemToCollect = itemToCollect;
    }

    @Override
    public String getObjectiveTaskDescription(final QuestPlayer questPlayer, final @Nullable ActiveObjective activeObjective) {
        final String displayName;
        if (!isCollectAnyItem()) {
            if (getItemToCollect().getItemMeta() != null) {
                displayName = getItemToCollect().getItemMeta().getDisplayName();
            } else {
                displayName = getItemToCollect().getType().name();
            }
        } else {
            displayName = "Any";
        }

        String itemType = isCollectAnyItem() ? "Any" : getItemToCollect().getType().name();

        if (!displayName.isBlank()) {
            return main.getLanguageManager().getString("chat.objectives.taskDescription.collectItems.base", questPlayer, activeObjective, Map.of(
                    "%ITEMTOCOLLECTTYPE%", itemType,
                    "%ITEMTOCOLLECTNAME%", displayName,
                    "%(%", "(",
                    "%)%", "<RESET>)"
            ));
        } else {
            return main.getLanguageManager().getString("chat.objectives.taskDescription.collectItems.base", questPlayer, activeObjective, Map.of(
                    "%ITEMTOCOLLECTTYPE%", itemType,
                    "%ITEMTOCOLLECTNAME%", "",
                    "%(%", "",
                    "%)%", ""
            ));
        }

    }

    public void setDeductIfItemIsDropped(final boolean deductIfItemIsDropped) {
        this.deductIfItemIsDropped = deductIfItemIsDropped;
    }

    @Override
    public void save(FileConfiguration configuration, String initialPath) {
        if(!getNQItem().isBlank()){
            configuration.set(initialPath + ".specifics.nqitem", getNQItem());
        }else {
            configuration.set(initialPath + ".specifics.itemToCollect.itemstack", getItemToCollect());
        }

        configuration.set(initialPath + ".specifics.deductIfItemDropped", isDeductIfItemIsDropped());
        configuration.set(initialPath + ".specifics.collectAnyItem", isCollectAnyItem());
    }

    @Override
    public void load(FileConfiguration configuration, String initialPath) {
        this.nqItemName = configuration.getString(initialPath + ".specifics.nqitem", "");
        if(nqItemName.isBlank()){
            itemToCollect = configuration.getItemStack(initialPath + ".specifics.itemToCollect.itemstack");
        }
        deductIfItemIsDropped = configuration.getBoolean(initialPath + ".specifics.deductIfItemDropped", true);
        collectAnyItem = configuration.getBoolean(initialPath + ".specifics.collectAnyItem", false);
    }

    public final ItemStack getItemToCollect() {
        if(!getNQItem().isBlank()){
            return main.getItemsManager().getItem(getNQItem()).getItemStack().clone();
        }else{
            return itemToCollect;
        }
    }

    @Override
    public void onObjectiveUnlock(final ActiveObjective activeObjective, final boolean unlockedDuringPluginStartupQuestLoadingProcess) {
    }
    @Override
    public void onObjectiveCompleteOrLock(final ActiveObjective activeObjective, final boolean lockedOrCompletedDuringPluginStartupQuestLoadingProcess, final boolean completed) {
    }

    public final boolean isDeductIfItemIsDropped() {
        return deductIfItemIsDropped;
    }
}
