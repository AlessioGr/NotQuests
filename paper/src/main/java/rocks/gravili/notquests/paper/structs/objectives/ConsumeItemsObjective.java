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

public class ConsumeItemsObjective extends Objective {

    private ItemStack itemToConsume;
    private boolean consumeAnyItem = false;
    private String nqItemName = "";

    public ConsumeItemsObjective(NotQuests main) {
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
                .argument(MaterialOrHandArgument.of("material", main), ArgumentDescription.of("Material of the item which needs to be consumed."))
                .argument(IntegerArgument.<CommandSender>newBuilder("amount").withMin(1), ArgumentDescription.of("Amount of items which need to be consumed."))
                .handler((context) -> {
                    final int amount = context.get("amount");

                    boolean consumeAnyItem = false;

                    final MaterialOrHand materialOrHand = context.get("material");
                    ItemStack itemToConsume;
                    if (materialOrHand.hand) { //"hand"
                        if (context.getSender() instanceof Player player) {
                            itemToConsume = player.getInventory().getItemInMainHand();
                        } else {
                            context.getSender().sendMessage(main.parse(
                                    "<error>This must be run by a player."
                            ));
                            return;
                        }
                    } else {
                        if (materialOrHand.material.equalsIgnoreCase("any")) {
                            consumeAnyItem = true;
                            itemToConsume = null;
                        } else {
                            itemToConsume = main.getItemsManager().getItemStack(materialOrHand.material);
                        }
                    }

                    ConsumeItemsObjective consumeItemsObjective = new ConsumeItemsObjective(main);

                    if(main.getItemsManager().getItem(materialOrHand.material) != null){
                        consumeItemsObjective.setNQItem(main.getItemsManager().getItem(materialOrHand.material).getItemName());
                    }else{
                        consumeItemsObjective.setItemToConsume(itemToConsume);
                    }

                    consumeItemsObjective.setConsumeAnyItem(consumeAnyItem);
                    consumeItemsObjective.setProgressNeeded(amount);

                    main.getObjectiveManager().addObjective(consumeItemsObjective, context);

                }));
    }

    public final boolean isConsumeAnyItem() {
        return consumeAnyItem;
    }

    public void setConsumeAnyItem(final boolean consumeAnyItem) {
        this.consumeAnyItem = consumeAnyItem;
    }

    public void setItemToConsume(final ItemStack itemToConsume) {
        this.itemToConsume = itemToConsume;
    }

    @Override
    public void onObjectiveUnlock(final ActiveObjective activeObjective, final boolean unlockedDuringPluginStartupQuestLoadingProcess) {
    }
    @Override
    public void onObjectiveCompleteOrLock(final ActiveObjective activeObjective, final boolean lockedOrCompletedDuringPluginStartupQuestLoadingProcess, final boolean completed) {
    }

    public final ItemStack getItemToConsume() {
        if(!getNQItem().isBlank()){
            return main.getItemsManager().getItem(getNQItem()).getItemStack().clone();
        }else{
            return itemToConsume;
        }
    }

    public final long getAmountToConsume() {
        return super.getProgressNeeded();
    }

    @Override
    public String getObjectiveTaskDescription(final QuestPlayer questPlayer) {
        final String displayName;
        if (!isConsumeAnyItem()) {
            if (getItemToConsume().getItemMeta() != null) {
                displayName = PlainTextComponentSerializer.plainText().serializeOr(getItemToConsume().getItemMeta().displayName(), getItemToConsume().getType().name());
            } else {
                displayName = getItemToConsume().getType().name();
            }
        } else {
            displayName = "Any";
        }

        String itemType = isConsumeAnyItem() ? "Any" : getItemToConsume().getType().name();

        if (!displayName.isBlank()) {
            return main.getLanguageManager().getString("chat.objectives.taskDescription.consumeItems.base", questPlayer, Map.of(
                    "%ITEMTOCONSUMETYPE%", itemType,
                    "%ITEMTOCONSUMENAME%", displayName,
                    "%(%", "(",
                    "%)%", "<RESET>)"
            ));
        } else {
            return main.getLanguageManager().getString("chat.objectives.taskDescription.consumeItems.base", questPlayer, Map.of(
                    "%ITEMTOCONSUMETYPE%", itemType,
                    "%ITEMTOCONSUMENAME%", "",
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
            configuration.set(initialPath + ".specifics.itemToConsume.itemstack", getItemToConsume());
        }

        configuration.set(initialPath + ".specifics.consumeAnyItem", isConsumeAnyItem());
    }

    @Override
    public void load(FileConfiguration configuration, String initialPath) {
        this.nqItemName = configuration.getString(initialPath + ".specifics.nqitem", "");
        if(nqItemName.isBlank()){
            itemToConsume = configuration.getItemStack(initialPath + ".specifics.itemToConsume.itemstack");
        }
        consumeAnyItem = configuration.getBoolean(initialPath + ".specifics.consumeAnyItem", false);
    }
}
