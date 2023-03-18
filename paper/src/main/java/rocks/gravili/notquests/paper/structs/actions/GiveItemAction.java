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
import java.util.ArrayList;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.arguments.ItemStackSelectionArgument;
import rocks.gravili.notquests.paper.commands.arguments.wrappers.ItemStackSelection;
import rocks.gravili.notquests.paper.managers.items.NQItem;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

public class GiveItemAction extends Action {

  private ItemStackSelection itemStackSelection;
  private int nqItemAmount = 1;

  public GiveItemAction(final NotQuests main) {
    super(main);
  }

  public static void handleCommands(
      NotQuests main,
      PaperCommandManager<CommandSender> manager,
      Command.Builder<CommandSender> builder,
      ActionFor actionFor) {
    manager.command(
        builder
            .argument(
                ItemStackSelectionArgument.of("material", main),
                ArgumentDescription.of(
                    "Material of the item which the player should receive. If you use 'hand', the item you are holding in your main hand will be used."))
            .argument(
                IntegerArgument.<CommandSender>builder("amount").withMin(1),
                ArgumentDescription.of("Amount of items which the player will receive."))
            .handler(
                (context) -> {
                  final ItemStackSelection itemStackSelection = context.get("material");
                  final int itemRewardAmount = context.get("amount");

                  if (itemStackSelection.isAny()) {
                    context
                        .getSender()
                        .sendMessage(
                            main.parse(
                                "<error>You cannot use <highlight>'any'</highlight> here!" // TODO:
                                                                                           // Allow
                                                                                           // any
                                                                                           // and
                                                                                           // give
                                                                                           // random
                                                                                           // itemstack
                                ));
                    return;
                  }

                  GiveItemAction giveItemAction = new GiveItemAction(main);
                  giveItemAction.setItemStackSelection(itemStackSelection);
                  giveItemAction.setNqItemAmount(itemRewardAmount);

                  main.getActionManager().addAction(giveItemAction, context, actionFor);
                }));
  }

  public final ItemStackSelection getItemStackSelection() {
    return itemStackSelection;
  }

  public void setItemStackSelection(final ItemStackSelection itemStackSelection) {
    this.itemStackSelection = itemStackSelection;
  }

  public final int getNqItemAmount() {
    return nqItemAmount;
  }

  public void setNqItemAmount(final int nqItemAmount) {
    this.nqItemAmount = nqItemAmount;
  }

  @Override
  public void executeInternally(final QuestPlayer questPlayer, Object... objects) {
    if (getItemStackSelection() == null || getItemStackSelection().isEmptyOrAny()) {
      main.getLogManager().warn("Tried to give item reward with invalid reward item");
      return;
    }
    if (questPlayer.getPlayer() == null) {
      main.getLogManager().warn("Tried to give item reward with invalid player object");
      return;
    }

    if (Bukkit.isPrimaryThread()) {
      for (final ItemStack itemStack : getItemStackSelection().toItemStackList()) {
        itemStack.setAmount(getNqItemAmount());
        questPlayer.getPlayer().getInventory().addItem(itemStack);
      }
    } else {
      Bukkit.getScheduler()
          .runTask(
              main.getMain(),
              () -> {
                for (final ItemStack itemStack : getItemStackSelection().toItemStackList()) {
                  itemStack.setAmount(getNqItemAmount());
                  questPlayer.getPlayer().getInventory().addItem(itemStack);
                }
              }); // TODO: Check if I can't just run it async if it already is async`?
    }
  }

  @Override
  public String getActionDescription(final QuestPlayer questPlayer, final Object... objects) {
    return "Item: " + getItemStackSelection().getAllMaterialsListedTranslated("main");
  }

  @Override
  public void save(final FileConfiguration configuration, String initialPath) {
    getItemStackSelection()
        .saveToFileConfiguration(configuration, initialPath + ".specifics.itemStackSelection");

    configuration.set(initialPath + ".specifics.nqitemamount", getNqItemAmount());
  }

  @Override
  public void load(final FileConfiguration configuration, String initialPath) {
    this.nqItemAmount = configuration.getInt(initialPath + ".specifics.nqitemamount", 1);

    this.itemStackSelection = new ItemStackSelection(main);
    itemStackSelection.loadFromFileConfiguration(
        configuration, initialPath + ".specifics.itemStackSelection");

    // Convert old to new
    if (configuration.contains(initialPath + ".specifics.nqitem")
        || configuration.contains(initialPath + ".specifics.item")
        || configuration.contains(initialPath + ".specifics.rewardItem")) {
      main.getLogManager().info("Converting old GiveItemAction to new one...");
      final String nqItemName = configuration.getString(initialPath + ".specifics.nqitem", "");

      if (nqItemName.isBlank()) {
        itemStackSelection.addItemStack(
            configuration.getItemStack(initialPath + ".specifics.item"));
        itemStackSelection.addItemStack(
            configuration.getItemStack(initialPath + ".specifics.rewardItem"));
      } else {
        itemStackSelection.addNqItemName(nqItemName);
      }
      itemStackSelection.saveToFileConfiguration(
          configuration, initialPath + ".specifics.itemStackSelection");
      configuration.set(initialPath + ".specifics.nqitem", null);
      configuration.set(initialPath + ".specifics.item", null);
      configuration.set(initialPath + ".specifics.rewardItem", null);

      // Let's hope it saves somewhere, else conversion will happen again...
    }
  }

  @Override
  public void deserializeFromSingleLineString(final ArrayList<String> arguments) {
    String itemName = arguments.get(0);

    NQItem nqItem = main.getItemsManager().getItem(itemName);
    if (nqItem == null) {
      final ItemStack itemStack =
          new ItemStack(Material.valueOf(arguments.get(0).toUpperCase(Locale.ROOT)));
      if (arguments.size() >= 2) {
        itemStack.setAmount(Integer.parseInt(arguments.get(1)));
      }
      this.itemStackSelection = new ItemStackSelection(main);
      itemStackSelection.addItemStack(itemStack);
    } else {
      this.itemStackSelection = new ItemStackSelection(main);
      itemStackSelection.addNqItem(nqItem);
      nqItemAmount = Integer.parseInt(arguments.get(1));
    }
  }
}
