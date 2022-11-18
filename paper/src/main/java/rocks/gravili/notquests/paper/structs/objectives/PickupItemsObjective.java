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
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.checkerframework.checker.nullness.qual.Nullable;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.arguments.ItemStackSelectionArgument;
import rocks.gravili.notquests.paper.commands.arguments.variables.NumberVariableValueArgument;
import rocks.gravili.notquests.paper.commands.arguments.wrappers.ItemStackSelection;
import rocks.gravili.notquests.paper.structs.ActiveObjective;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

public class PickupItemsObjective extends Objective {

  private ItemStackSelection itemStackSelection;
  private boolean deductIfItemIsDropped = true;
  private boolean deductIfItemIsPlaced = true;
  private boolean deductIfItemIsRemovedFromInventory = true; //TODO: Implement


  public PickupItemsObjective(NotQuests main) {
    super(main);
  }

  public static void handleCommands(
      NotQuests main,
      PaperCommandManager<CommandSender> manager,
      Command.Builder<CommandSender> addObjectiveBuilder,
      final int level) {
    manager.command(
        addObjectiveBuilder
            .argument(
                ItemStackSelectionArgument.of("materials", main),
                ArgumentDescription.of("Material of the item which needs to be collected"))
            .argument(
                NumberVariableValueArgument.newBuilder("amount", main, null),
                ArgumentDescription.of("Amount of items which need to be collected"))
            .flag(
                manager
                    .flagBuilder("doNotDeductIfItemIsDropped")
                    .withDescription(
                        ArgumentDescription.of(
                            "Makes it so Quest progress is NOT removed if the item is dropped.")))
            .flag(
                    manager
                            .flagBuilder("doNotDeductIfItemIsPlaced")
                            .withDescription(
                                    ArgumentDescription.of(
                                            "Makes it so Quest progress is NOT removed if the item is placed.")))
                .flag(
                        manager
                                .flagBuilder("doNotDeductIfItemIsRemovedFromInventory")
                                .withDescription(
                                        ArgumentDescription.of(
                                                "Makes it so Quest progress is NOT removed if the item is removed from inventory.")))
            .handler(
                (context) -> {
                  final String amountExpression = context.get("amount");
                  final boolean deductIfItemIsDropped =
                      !context.flags().isPresent("doNotDeductIfItemIsDropped");
                  final boolean deductIfItemIsPlaced =
                          !context.flags().isPresent("doNotDeductIfItemIsPlaced");
                  final boolean deductIfItemIsRemovedFromInventory =
                          !context.flags().isPresent("doNotDeductIfItemIsRemovedFromInventory");

                  final ItemStackSelection itemStackSelection = context.get("materials");

                  PickupItemsObjective pickupItemsObjective = new PickupItemsObjective(main);
                  pickupItemsObjective.setItemStackSelection(itemStackSelection);

                  pickupItemsObjective.setProgressNeededExpression(amountExpression);
                  pickupItemsObjective.setDeductIfItemIsDropped(deductIfItemIsDropped);
                  pickupItemsObjective.setDeductIfItemIsPlaced(deductIfItemIsPlaced);
                  pickupItemsObjective.setDeductIfItemIsRemovedFromInventory(deductIfItemIsRemovedFromInventory);


                  main.getObjectiveManager().addObjective(pickupItemsObjective, context, level);
                }));
  }

  public final ItemStackSelection getItemStackSelection() {
    return itemStackSelection;
  }

  public void setItemStackSelection(final ItemStackSelection itemStackSelection) {
    this.itemStackSelection = itemStackSelection;
  }

  @Override
  public String getTaskDescriptionInternal(
      final QuestPlayer questPlayer, final @Nullable ActiveObjective activeObjective) {
    return main.getLanguageManager()
        .getString(
            "chat.objectives.taskDescription.pickupItems.base",
            questPlayer,
            activeObjective,
            Map.of(
                "%ITEMTOPICKUPTYPE%", getItemStackSelection().getAllMaterialsListedTranslated("main"),
                "%ITEMTOPICKUPNAME%", "",
                "%(%", "",
                "%)%", ""));
  }

  @Override
  public void save(FileConfiguration configuration, String initialPath) {
    getItemStackSelection()
        .saveToFileConfiguration(configuration, initialPath + ".specifics.itemStackSelection");

    configuration.set(initialPath + ".specifics.deductIfItemDropped", isDeductIfItemIsDropped());
    configuration.set(initialPath + ".specifics.deductIfItemPlaced", isDeductIfItemIsPlaced());
    configuration.set(initialPath + ".specifics.deductIfItemRemovedFromInventory", isDeductIfItemIsRemovedFromInventory());

  }

  @Override
  public void load(FileConfiguration configuration, String initialPath) {
    this.itemStackSelection = new ItemStackSelection(main);
    itemStackSelection.loadFromFileConfiguration(
        configuration, initialPath + ".specifics.itemStackSelection");

    // Convert old to new
    if (configuration.contains(initialPath + ".specifics.nqitem")
        || configuration.contains(initialPath + ".specifics.itemToCollect.itemstack")) {
      main.getLogManager().info("Converting old PickupItemsObjective to new one...");
      final String nqItemName = configuration.getString(initialPath + ".specifics.nqitem", "");

      if (nqItemName.isBlank()) {
        itemStackSelection.addItemStack(
            configuration.getItemStack(initialPath + ".specifics.itemToCollect.itemstack"));
      } else {
        itemStackSelection.addNqItemName(nqItemName);
      }
      itemStackSelection.saveToFileConfiguration(
          configuration, initialPath + ".specifics.itemStackSelection");
      configuration.set(initialPath + ".specifics.nqitem", null);
      configuration.set(initialPath + ".specifics.itemToCollect.itemstack", null);
      // Let's hope it saves somewhere, else conversion will happen again...
    }

    deductIfItemIsDropped =
        configuration.getBoolean(initialPath + ".specifics.deductIfItemDropped", true);
    deductIfItemIsPlaced =
            configuration.getBoolean(initialPath + ".specifics.deductIfItemPlaced", true);
    deductIfItemIsRemovedFromInventory =
            configuration.getBoolean(initialPath + ".specifics.deductIfItemRemovedFromInventory", true);
  }

  @Override
  public void onObjectiveUnlock(
      final ActiveObjective activeObjective,
      final boolean unlockedDuringPluginStartupQuestLoadingProcess) {}

  @Override
  public void onObjectiveCompleteOrLock(
      final ActiveObjective activeObjective,
      final boolean lockedOrCompletedDuringPluginStartupQuestLoadingProcess,
      final boolean completed) {}

  public final boolean isDeductIfItemIsDropped() {
    return deductIfItemIsDropped;
  }

  public void setDeductIfItemIsDropped(final boolean deductIfItemIsDropped) {
    this.deductIfItemIsDropped = deductIfItemIsDropped;
  }

  public final boolean isDeductIfItemIsPlaced() {
    return deductIfItemIsPlaced;
  }

  public void setDeductIfItemIsPlaced(final boolean deductIfItemIsPlaced) {
    this.deductIfItemIsPlaced = deductIfItemIsPlaced;
  }

  public final boolean isDeductIfItemIsRemovedFromInventory() {
    return deductIfItemIsRemovedFromInventory;
  }

  public void setDeductIfItemIsRemovedFromInventory(final boolean deductIfItemIsRemovedFromInventory) {
    this.deductIfItemIsRemovedFromInventory = deductIfItemIsRemovedFromInventory;
  }
}
