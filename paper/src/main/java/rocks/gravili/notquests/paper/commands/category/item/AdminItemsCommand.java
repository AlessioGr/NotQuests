/*
 * NotQuests - A Questing plugin for Minecraft Servers
 * Copyright (C) 2022 Alessio Gravili
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

package rocks.gravili.notquests.paper.commands.category.item;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.arguments.wrappers.ItemStackSelection;
import rocks.gravili.notquests.paper.commands.framework.NQArguments;
import rocks.gravili.notquests.paper.commands.framework.NQCommandBuilder;
import rocks.gravili.notquests.paper.commands.framework.NQCommandManager;
import rocks.gravili.notquests.paper.commands.framework.NQDescription;
import rocks.gravili.notquests.paper.managers.data.Category;
import rocks.gravili.notquests.paper.managers.items.NQItem;

import java.util.Arrays;

import static rocks.gravili.notquests.paper.commands.arguments.ItemStackSelectionArgument.itemStackSelectionArgument;
import static rocks.gravili.notquests.paper.commands.arguments.NQNPCArgument.nqNPCArgument;

public class AdminItemsCommand {

    private final NotQuests notQuests;
    private final NQCommandManager manager;
    private final NQCommandBuilder builder;

    public AdminItemsCommand(NotQuests notQuests, NQCommandManager manager, NQCommandBuilder builder) {
        this.notQuests = notQuests;
        this.manager = manager;
        this.builder = builder;

        var editBuilder = builder.literal("items");
        manager.command(editBuilder
                .literal("create", NQDescription.of("Creates a new Item."))
                .required("name", NQArguments.stringArgument(), NQDescription.of("Item Name"))
                .required("material", itemStackSelectionArgument(notQuests), NQDescription.of("Material of what this item should be based on. If you use 'hand', the item you are holding in your notQuests hand will be used."))
                .handler(context -> {
                    final String itemName = context.get("name");

                    if (Arrays.stream(Material.values()).anyMatch(material -> material.name().equalsIgnoreCase(itemName))) {
                        context.sender().sendMessage(notQuests.parse(
                                "<error>Error: The item <highlight>"
                                        + itemName
                                        + "</highlight> already exists! You cannot use item names identical to vanilla Minecraft item names.")
                        );
                        return;
                    }

                    if (notQuests.getItemsManager().getItem(itemName) != null) {
                        context.sender().sendMessage(notQuests.parse(
                                "<error>Error: The item <highlight>"
                                        + itemName
                                        + "</highlight> already exists!")
                        );
                        return;
                    }

                    var itemStackSelection = (ItemStackSelection) context.get("material");

                    ItemStack itemStack;
                    if (itemStackSelection.isAny()) {
                        context.sender().sendMessage(notQuests.parse("<error>You cannot use <highlight>'any'</highlight> here!"));
                        return;
                    }
                    itemStack = itemStackSelection.toFirstItemStack();

                    NQItem nqItem = new NQItem(notQuests, itemName, itemStack);

                    if (context.flags().isPresent(notQuests.getCommandManager().categoryFlag)) {
                        final Category category = context.flags().getValue(
                                notQuests.getCommandManager().categoryFlag,
                                notQuests.getDataManager().getDefaultCategory()
                        );
                        nqItem.setCategory(category);
                    }
                    notQuests.getItemsManager().addItem(nqItem);

                    context.sender().sendMessage(notQuests.parse(
                            "<success>The item <highlight>"
                                    + itemName
                                    + "</highlight> has been added successfully!")
                    );
                }));

        manager.command(editBuilder.commandDescription(NQDescription.of("Lists all items"))
                .literal("list")
                .handler((context) -> {
                    context.sender().sendMessage(notQuests.parse("<highlight>All Items:"));
                    int counter = 1;

                    for (NQItem nqItem : notQuests.getItemsManager().getItems()) {
                        context.sender().sendMessage(notQuests.parse(
                                "<highlight>"
                                        + counter
                                        + ".</highlight> <main>"
                                        + nqItem.getItemName()
                                        + "</main> <highlight2>Type: <main>"
                                        + nqItem.getItemStack().getType().name()
                                        + " <highlight2>Display Name:</highlight2> <white><reset>"
                                        + notQuests.getMiniMessage()
                                        .serialize(nqItem.getItemStack().displayName()))
                        );
                        counter++;
                    }
                }));

        NQCommandBuilder admitItemsEditBuilder = editBuilder
                .literal("edit", "e")
                .required("item", nqNPCArgument(notQuests), NQDescription.of("NotQuests Item which you want to edit."));


        manager.command(admitItemsEditBuilder.commandDescription(NQDescription.of("Gives the player the item."))
                .literal("give")
                .required("player", rocks.gravili.notquests.paper.commands.framework.NQArguments.playerArgument(), NQDescription.of("Player who should receive the item"))
                .required("amount", NQArguments.integerArgument(), NQDescription.of("Amount of items the player should receive"))
                .handler((context) -> {
                    NQItem nqItem = context.get("item");
                    int amount = context.get("amount");
                    Player player = context.get("player");

                    ItemStack itemStack = nqItem.getItemStack().clone();
                    itemStack.setAmount(amount);
                    player.getInventory().addItem(itemStack);

                    context.sender().sendMessage(notQuests.parse(
                            "<success>The item <highlight>"
                                    + nqItem.getItemName()
                                    + "</highlight> has given to player <highlight2>"
                                    + player.getName()
                                    + "</highlight2>!")
                    );
                }));

        manager.command(admitItemsEditBuilder.commandDescription(NQDescription.of("Removes a NotQuests Item."))
                .literal("remove", "delete")
                .handler((context) -> {
                    NQItem nqItem = context.get("item");

                    notQuests.getItemsManager().deleteItem(nqItem);

                    context.sender().sendMessage(notQuests.parse(
                            "<success>The item <highlight>"
                                    + nqItem.getItemName()
                                    + "</highlight> has been deleted successfully!")
                    );
                }));

        manager.command(admitItemsEditBuilder.commandDescription(NQDescription.of("Sets an item's display name."))
                .literal("displayName")
                .literal("set")
                .required("display-name", NQArguments.greedyStringArgument(), NQDescription.of("New display name"))
                .handler((context) -> {
                    NQItem nqItem = context.get("item");
                    final String displayName = (String) context.get("display-name");

                    nqItem.setDisplayName(displayName, true);

                    context.sender().sendMessage(notQuests.parse(
                            "<success>The display name of item <highlight>"
                                    + nqItem.getItemName()
                                    + "</highlight> has been set to: <white><reset>"
                                    + displayName)
                    );
                }));

        manager.command(admitItemsEditBuilder.commandDescription(NQDescription.of("Removes an item's display name."))
                .literal("displayName")
                .literal("remove")
                .handler((context) -> {
                    NQItem nqItem = context.get("item");

                    nqItem.setDisplayName(null, true);

                    context.sender().sendMessage(notQuests.parse(
                            "<success>The display name of item <highlight>"
                                    + nqItem.getItemName()
                                    + "</highlight> has been removed!")
                    );
                }));

        manager.command(admitItemsEditBuilder.commandDescription(NQDescription.of("Shows an item's current display name."))
                .literal("displayName")
                .literal("show")
                .handler((context) -> {
                    NQItem nqItem = context.get("item");
                    context.sender().sendMessage(notQuests.parse(
                            "<success>The display name of item <highlight>"
                                    + nqItem.getItemName()
                                    + "</highlight> is: \n<white><reset>"
                                    + notQuests.getMiniMessage()
                                    .serialize(nqItem.getItemStack().displayName()))
                    );
                }));
    }
}
