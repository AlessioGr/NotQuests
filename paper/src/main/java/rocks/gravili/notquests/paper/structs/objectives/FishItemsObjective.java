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

public class FishItemsObjective extends Objective {

  private ItemStackSelection itemStackSelection;

  public FishItemsObjective(NotQuests main) {
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
                ArgumentDescription.of("Material of the item which needs to be fished"))
            .argument(
                NumberVariableValueArgument.newBuilder("amount", main, null),
                ArgumentDescription.of("Amount of items which need to be fished"))
            .handler(
                (context) -> {
                  final String amountExpression = context.get("amount");

                  final ItemStackSelection itemStackSelection = context.get("materials");

                  FishItemsObjective fishItemsObjective = new FishItemsObjective(main);
                  fishItemsObjective.setItemStackSelection(itemStackSelection);

                  fishItemsObjective.setProgressNeededExpression(amountExpression);

                  main.getObjectiveManager().addObjective(fishItemsObjective, context, level);
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
            "chat.objectives.taskDescription.fishItems.base",
            questPlayer,
            activeObjective,
            Map.of(
                "%ITEMTOFISHTYPE%", getItemStackSelection().getAllMaterialsListedTranslated("main"),
                "%ITEMTOFISHNAME%", "",
                "%(%", "",
                "%)%", ""));
  }

  @Override
  public void save(FileConfiguration configuration, String initialPath) {
    getItemStackSelection()
        .saveToFileConfiguration(configuration, initialPath + ".specifics.itemStackSelection");
  }

  @Override
  public void load(FileConfiguration configuration, String initialPath) {
    this.itemStackSelection = new ItemStackSelection(main);
    itemStackSelection.loadFromFileConfiguration(
        configuration, initialPath + ".specifics.itemStackSelection");

    // Convert old to new
    if (configuration.contains(initialPath + ".specifics.nqitem")
        || configuration.contains(initialPath + ".specifics.itemToFish.itemstack")) {
      main.getLogManager().info("Converting old FishItemsObjective to new one...");
      final String nqItemName = configuration.getString(initialPath + ".specifics.nqitem", "");

      if (nqItemName.isBlank()) {
        itemStackSelection.addItemStack(
            configuration.getItemStack(initialPath + ".specifics.itemToFish.itemstack"));
      } else {
        itemStackSelection.addNqItemName(nqItemName);
      }
      itemStackSelection.saveToFileConfiguration(
          configuration, initialPath + ".specifics.itemStackSelection");
      configuration.set(initialPath + ".specifics.nqitem", null);
      configuration.set(initialPath + ".specifics.itemToFish.itemstack", null);
      // Let's hope it saves somewhere, else conversion will happen again...
    }
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
}
