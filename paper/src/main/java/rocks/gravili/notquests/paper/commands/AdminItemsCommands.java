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

package rocks.gravili.notquests.paper.commands;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.bukkit.arguments.selector.SinglePlayerSelector;
import cloud.commandframework.bukkit.parsers.selector.SinglePlayerSelectorArgument;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.paper.PaperCommandManager;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.arguments.ItemStackSelectionArgument;
import rocks.gravili.notquests.paper.commands.arguments.MiniMessageSelector;
import rocks.gravili.notquests.paper.commands.arguments.NQItemSelector;
import rocks.gravili.notquests.paper.commands.arguments.wrappers.ItemStackSelection;
import rocks.gravili.notquests.paper.managers.data.Category;
import rocks.gravili.notquests.paper.managers.items.NQItem;

public class AdminItemsCommands {
  private final NotQuests main;
  private final PaperCommandManager<CommandSender> manager;
  private final Command.Builder<CommandSender> editBuilder;

  public AdminItemsCommands(
      final NotQuests main,
      PaperCommandManager<CommandSender> manager,
      Command.Builder<CommandSender> editBuilder) {
    this.main = main;
    this.manager = manager;
    this.editBuilder = editBuilder;

    manager.command(
        editBuilder
            .literal("create")
            .argument(StringArgument.of("name"), ArgumentDescription.of("Item Name"))
            .argument(
                ItemStackSelectionArgument.of("material", main),
                ArgumentDescription.of(
                    "Material of what this item should be based on. If you use 'hand', the item you are holding in your main hand will be used."))
            .meta(CommandMeta.DESCRIPTION, "Creates a new Item.")
            .handler(
                (context) -> {
                  final String itemName = context.get("name");

                  for (Material material : Material.values()) {
                    if (itemName.equalsIgnoreCase(material.name())) {
                      context
                          .getSender()
                          .sendMessage(
                              main.parse(
                                  "<error>Error: The item <highlight>"
                                      + itemName
                                      + "</highlight> already exists! You cannot use item names identical to vanilla Minecraft item names."));
                      return;
                    }
                  }

                  if (main.getItemsManager().getItem(itemName) != null) {
                    context
                        .getSender()
                        .sendMessage(
                            main.parse(
                                "<error>Error: The item <highlight>"
                                    + itemName
                                    + "</highlight> already exists!"));
                    return;
                  }

                  final ItemStackSelection itemStackSelection = context.get("material");

                  final ItemStack itemStack;
                  if (itemStackSelection.isAny()) {
                    context
                        .getSender()
                        .sendMessage(
                            main.parse("<error>You cannot use <highlight>'any'</highlight> here!"));
                    return;
                  }
                  itemStack = itemStackSelection.toFirstItemStack();

                  NQItem nqItem = new NQItem(main, itemName, itemStack);

                  if (context.flags().contains(main.getCommandManager().categoryFlag)) {
                    final Category category =
                        context
                            .flags()
                            .getValue(
                                main.getCommandManager().categoryFlag,
                                main.getDataManager().getDefaultCategory());
                    nqItem.setCategory(category);
                  }
                  main.getItemsManager().addItem(nqItem);

                  context
                      .getSender()
                      .sendMessage(
                          main.parse(
                              "<success>The item <highlight>"
                                  + itemName
                                  + "</highlight> has been added successfully!"));
                }));

    manager.command(
        editBuilder
            .literal("list")
            .meta(CommandMeta.DESCRIPTION, "Lists all items")
            .handler(
                (context) -> {
                  context.getSender().sendMessage(main.parse("<highlight>All Items:"));
                  int counter = 1;

                  for (NQItem nqItem : main.getItemsManager().getItems()) {
                    context
                        .getSender()
                        .sendMessage(
                            main.parse(
                                "<highlight>"
                                    + counter
                                    + ".</highlight> <main>"
                                    + nqItem.getItemName()
                                    + "</main> <highlight2>Type: <main>"
                                    + nqItem.getItemStack().getType().name()
                                    + " <highlight2>Display Name:</highlight2> <white><reset>"
                                    + main.getMiniMessage()
                                        .serialize(nqItem.getItemStack().displayName())));
                    counter++;
                  }
                }));

    Command.Builder<CommandSender> admitItemsEditBuilder =
        editBuilder
            .literal("edit", "e")
            .argument(
                NQItemSelector.of("item", main),
                ArgumentDescription.of("NotQuests Item which you want to edit."));

    handleEditCommands(admitItemsEditBuilder);
  }

  public void handleEditCommands(Command.Builder<CommandSender> builder) {
    manager.command(
        builder
            .literal("give")
            .argument(
                SinglePlayerSelectorArgument.of("player"),
                ArgumentDescription.of("Player who should receive the item"))
            .argument(
                IntegerArgument.<CommandSender>newBuilder("amount").withMin(1),
                ArgumentDescription.of("Amount of items the player should receive"))
            .meta(CommandMeta.DESCRIPTION, "Gives the player the item.")
            .handler(
                (context) -> {
                  NQItem nqItem = context.get("item");
                  final int amount = context.get("amount");
                  final SinglePlayerSelector singlePlayerSelector = context.get("player");

                  if (singlePlayerSelector.hasAny() && singlePlayerSelector.getPlayer() != null) {
                    ItemStack itemStack = nqItem.getItemStack().clone();
                    itemStack.setAmount(amount);
                    singlePlayerSelector.getPlayer().getInventory().addItem(itemStack);

                    context
                        .getSender()
                        .sendMessage(
                            main.parse(
                                "<success>The item <highlight>"
                                    + nqItem.getItemName()
                                    + "</highlight> has given to player <highlight2>"
                                    + singlePlayerSelector.getPlayer().getName()
                                    + "</highlight2>!"));
                  } else {
                    context
                        .getSender()
                        .sendMessage(
                            main.parse("<error>" + "Player is not online or was not found!"));
                  }
                }));

    manager.command(
        builder
            .literal("remove", "delete")
            .meta(CommandMeta.DESCRIPTION, "Removes a NotQuests Item.")
            .handler(
                (context) -> {
                  NQItem nqItem = context.get("item");

                  main.getItemsManager().deleteItem(nqItem);

                  context
                      .getSender()
                      .sendMessage(
                          main.parse(
                              "<success>The item <highlight>"
                                  + nqItem.getItemName()
                                  + "</highlight> has been deleted successfully!"));
                }));

    manager.command(
        builder
            .literal("displayName")
            .literal("set")
            .meta(CommandMeta.DESCRIPTION, "Sets an item's display name.")
            .argument(
                MiniMessageSelector.<CommandSender>newBuilder("Display Name", main).build(),
                ArgumentDescription.of("New display name"))
            .handler(
                (context) -> {
                  NQItem nqItem = context.get("item");
                  final String displayName =
                      String.join(" ", (String[]) context.get("Display Name"));

                  nqItem.setDisplayName(displayName, true);

                  context
                      .getSender()
                      .sendMessage(
                          main.parse(
                              "<success>The display name of item <highlight>"
                                  + nqItem.getItemName()
                                  + "</highlight> has been set to: <white><reset>"
                                  + displayName));
                }));
    manager.command(
        builder
            .literal("displayName")
            .literal("remove")
            .meta(CommandMeta.DESCRIPTION, "Removes an item's display name.")
            .handler(
                (context) -> {
                  NQItem nqItem = context.get("item");

                  nqItem.setDisplayName(null, true);

                  context
                      .getSender()
                      .sendMessage(
                          main.parse(
                              "<success>The display name of item <highlight>"
                                  + nqItem.getItemName()
                                  + "</highlight> has been removed!"));
                }));

    manager.command(
        builder
            .literal("displayName")
            .literal("show", "check", "view")
            .meta(CommandMeta.DESCRIPTION, "Shows an item's current display name.")
            .handler(
                (context) -> {
                  NQItem nqItem = context.get("item");

                  context
                      .getSender()
                      .sendMessage(
                          main.parse(
                              "<success>The display name of item <highlight>"
                                  + nqItem.getItemName()
                                  + "</highlight> is: \n<white><reset>"
                                  + main.getMiniMessage()
                                      .serialize(nqItem.getItemStack().displayName())));
                }));
  }
}
