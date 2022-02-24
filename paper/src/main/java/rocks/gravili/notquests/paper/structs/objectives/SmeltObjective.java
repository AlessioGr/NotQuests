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

package rocks.gravili.notquests.paper.structs.objectives;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.paper.PaperCommandManager;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.arguments.MaterialOrHandArgument;
import rocks.gravili.notquests.paper.commands.arguments.wrappers.MaterialOrHand;
import rocks.gravili.notquests.paper.structs.ActiveObjective;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

import java.util.Map;

public class SmeltObjective extends Objective {

    private ItemStack itemToSmelt;
    private boolean smeltAnyItem = false;
    private String nqItemName = "";

    public SmeltObjective(NotQuests main) {
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
                .argument(MaterialOrHandArgument.of("material", main), ArgumentDescription.of("Output item of the smelting."))
                .argument(IntegerArgument.<CommandSender>newBuilder("amount").withMin(1), ArgumentDescription.of("Amount of items which need to be smelted."))
                .handler((context) -> {
                    final int amount = context.get("amount");

                    boolean smeltAnyItem = false;

                    final MaterialOrHand materialOrHand = context.get("material");
                    ItemStack itemToSmelt;
                    if (materialOrHand.hand) { //"hand"
                        if (context.getSender() instanceof Player player) {
                            itemToSmelt = player.getInventory().getItemInMainHand();
                        } else {
                            context.getSender().sendMessage(main.parse(
                                    "<error>This must be run by a player."
                            ));
                            return;
                        }
                    } else {
                        if (materialOrHand.material.equalsIgnoreCase("any")) {
                            smeltAnyItem = true;
                            itemToSmelt = null;
                        } else {
                            itemToSmelt = main.getItemsManager().getItemStack(materialOrHand.material);
                        }
                    }

                    SmeltObjective smeltObjective = new SmeltObjective(main);

                    if(main.getItemsManager().getItem(materialOrHand.material) != null){
                        smeltObjective.setNQItem(main.getItemsManager().getItem(materialOrHand.material).getItemName());
                    }else{
                        smeltObjective.setItemToSmelt(itemToSmelt);
                    }

                    smeltObjective.setProgressNeeded(amount);
                    smeltObjective.setSmeltAnyItem(smeltAnyItem);

                    main.getObjectiveManager().addObjective(smeltObjective, context);
                }));
    }

    public final boolean isSmeltAnyItem() {
        return smeltAnyItem;
    }

    public void setSmeltAnyItem(final boolean smeltAnyItem) {
        this.smeltAnyItem = smeltAnyItem;
    }

    public void setItemToSmelt(final ItemStack itemToSmelt) {
        this.itemToSmelt = itemToSmelt;
    }

    @Override
    public void onObjectiveUnlock(final ActiveObjective activeObjective, final boolean unlockedDuringPluginStartupQuestLoadingProcess) {
    }
    @Override
    public void onObjectiveCompleteOrLock(final ActiveObjective activeObjective, final boolean lockedOrCompletedDuringPluginStartupQuestLoadingProcess, final boolean completed) {
    }

    public final ItemStack getItemToSmelt() {
        if(!getNQItem().isBlank()){
            return main.getItemsManager().getItem(getNQItem()).getItemStack().clone();
        }else{
            return itemToSmelt;
        }
    }

    public final long getAmountToSmelt() {
        return super.getProgressNeeded();
    }

    @Override
    public String getObjectiveTaskDescription(final QuestPlayer questPlayer) {
        final String displayName;
        if (!isSmeltAnyItem()) {
            if (getItemToSmelt().getItemMeta() != null) {
                displayName = PlainTextComponentSerializer.plainText().serializeOr(getItemToSmelt().getItemMeta().displayName(), getItemToSmelt().getType().name());
            } else {
                displayName = getItemToSmelt().getType().name();
            }
        } else {
            displayName = "Any";
        }


        String itemType = isSmeltAnyItem() ? "Any" : getItemToSmelt().getType().name();


        if (!displayName.isBlank()) {
            return main.getLanguageManager().getString("chat.objectives.taskDescription.smelt.base", questPlayer, Map.of(
                    "%ITEMTOSMELTTYPE%", itemType,
                    "%ITEMTOSMELTNAME%", displayName,
                    "%(%", "(",
                    "%)%", "<RESET>)"
            ));
        } else {
            return main.getLanguageManager().getString("chat.objectives.taskDescription.smelt.base", questPlayer, Map.of(
                    "%ITEMTOSMELTTYPE%", itemType,
                    "%ITEMTOSMELTNAME%", "",
                    "%(%", "",
                    "%)%", ""
            ));
        }


    }

    @Override
    public void save(FileConfiguration configuration, String initialPath) {
        if(!getNQItem().isBlank()){
            configuration.set(initialPath + ".specifics.nqitem", getNQItem());
        }else {
            configuration.set(initialPath + ".specifics.itemToSmelt.itemstack", getItemToSmelt());
        }

        configuration.set(initialPath + ".specifics.smeltAnyItem", isSmeltAnyItem());
    }

    @Override
    public void load(FileConfiguration configuration, String initialPath) {
        this.nqItemName = configuration.getString(initialPath + ".specifics.nqitem", "");
        if(nqItemName.isBlank()){
            itemToSmelt = configuration.getItemStack(initialPath + ".specifics.itemToSmelt.itemstack");
        }

        smeltAnyItem = configuration.getBoolean(initialPath + ".specifics.smeltAnyItem");
    }
}
