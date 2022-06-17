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

package rocks.gravili.notquests.paper.structs.actions;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.paper.PaperCommandManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.arguments.MaterialOrHandArgument;
import rocks.gravili.notquests.paper.commands.arguments.wrappers.MaterialOrHand;
import rocks.gravili.notquests.paper.managers.items.NQItem;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

import java.util.ArrayList;
import java.util.Locale;

public class GiveItemAction extends Action {

    private ItemStack item = null;
    private String nqItemName = "";
    private int nqItemAmount = 1;

    public GiveItemAction(final NotQuests main) {
        super(main);
    }

    public static void handleCommands(NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> builder, ActionFor rewardFor) {
        manager.command(builder
                .argument(MaterialOrHandArgument.of("material", main), ArgumentDescription.of("Material of the item which the player should receive. If you use 'hand', the item you are holding in your main hand will be used."))
                .argument(IntegerArgument.<CommandSender>newBuilder("amount").withMin(1), ArgumentDescription.of("Amount of items which the player will receive."))
                .handler((context) -> {
                    final MaterialOrHand materialOrHand = context.get("material");
                    final int itemRewardAmount = context.get("amount");

                    ItemStack itemStack;
                    if (materialOrHand.material.equalsIgnoreCase("any")) {
                        context.getSender().sendMessage(main.parse(
                                "<error>You cannot use <highlight>'any'</highlight> here!"
                        ));
                        return;
                    }

                    itemStack = main.getItemsManager().getItemStack(materialOrHand);
                    itemStack.setAmount(itemRewardAmount);

                    GiveItemAction giveItemAction = new GiveItemAction(main);
                    if(main.getItemsManager().getItem(materialOrHand.material) != null){
                        giveItemAction.setNQItem(main.getItemsManager().getItem(materialOrHand.material).getItemName());
                        giveItemAction.setNqItemAmount(itemRewardAmount);
                    }else{
                        giveItemAction.setItem(itemStack);
                    }

                    main.getActionManager().addAction(giveItemAction, context);
                }));
    }

    public void setNQItem(final String nqItemName){
        this.nqItemName = nqItemName;
    }
    public final String getNQItem(){
        return nqItemName;
    }

    public void setNqItemAmount(final int nqItemAmount){
        this.nqItemAmount = nqItemAmount;
    }
    public final int getNqItemAmount(){
        return nqItemAmount;
    }

    public void setItem(final ItemStack item) {
        this.item = item;
    }

    @Override
    public void executeInternally(final QuestPlayer questPlayer, Object... objects) {
        if (getItemReward() == null) {
            main.getLogManager().warn("Tried to give item reward with invalid reward item");
            return;
        }
        if (questPlayer.getPlayer() == null) {
            main.getLogManager().warn("Tried to give item reward with invalid player object");
            return;
        }

        if (Bukkit.isPrimaryThread()) {
            questPlayer.getPlayer().getInventory().addItem(getItemReward());
        } else {
            Bukkit.getScheduler().runTask(main.getMain(), () -> questPlayer.getPlayer().getInventory().addItem(getItemReward())); //TODO: Check if I can't just run it async if it already is async`?
        }


    }

    @Override
    public String getActionDescription(final QuestPlayer questPlayer, final Object... objects) {
        return "Item: " + getItemReward().getType().name();
    }

    @Override
    public void save(final FileConfiguration configuration, String initialPath) {
        if(!getNQItem().isBlank()){
            configuration.set(initialPath + ".specifics.nqitem", getNQItem());
            configuration.set(initialPath + ".specifics.nqitemamount", getNqItemAmount());
        }else {
            configuration.set(initialPath + ".specifics.item", getItemReward());
        }
    }


    public final ItemStack getItemReward() {
        if(!getNQItem().isBlank()){
            ItemStack itemStack = main.getItemsManager().getItem(getNQItem()).getItemStack().clone();
            itemStack.setAmount(getNqItemAmount());
            return itemStack;
        }else{
            return item;
        }
    }

    @Override
    public void load(final FileConfiguration configuration, String initialPath) {
        this.nqItemName = configuration.getString(initialPath + ".specifics.nqitem", "");
        this.nqItemAmount = configuration.getInt(initialPath + ".specifics.nqitemamount", 1);

        if(nqItemName.isBlank()){
            this.item = configuration.getItemStack(initialPath + ".specifics.item");
            if(this.item == null){
                this.item = configuration.getItemStack(initialPath + ".specifics.rewardItem");
            }
        }
    }

    @Override
    public void deserializeFromSingleLineString(ArrayList<String> arguments) {
        String itemName = arguments.get(0);

        NQItem nqItem = main.getItemsManager().getItem(itemName);
        if(nqItem == null){
            final ItemStack itemStack = new ItemStack(Material.valueOf(arguments.get(0).toUpperCase(Locale.ROOT)));
            if(arguments.size() >= 2){
                itemStack.setAmount(Integer.parseInt(arguments.get(1)));
            }
            this.item = itemStack;
        }else{
            this.nqItemName = nqItem.getItemName();
            nqItemAmount = Integer.parseInt(arguments.get(1));
        }




    }
}