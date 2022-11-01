package rocks.gravili.notquests.paper.structs.objectives;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.paper.PaperCommandManager;
import java.util.Map;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.checkerframework.checker.nullness.qual.Nullable;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.arguments.variables.NumberVariableValueArgument;
import rocks.gravili.notquests.paper.structs.ActiveObjective;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

public class ShearSheepObjective extends Objective {

  private boolean cancelShearing = false;

  public ShearSheepObjective(NotQuests main) {
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
                NumberVariableValueArgument.newBuilder("amount", main, null),
                ArgumentDescription.of("Amount of shears needed"))
                .flag(
                manager
                    .flagBuilder("cancelShearing")
                    .withDescription(
                        ArgumentDescription.of(
                            "Makes it so the shearing will be cancelled while this objective is active")))
            .flag(main.getCommandManager().maxDistance)
            .handler(
                (context) -> {
                  final String amountExpression = context.get("amount");

                  final boolean cancelShearing = context.flags().isPresent("cancelShearing");

                  final ShearSheepObjective shearSheepObjective = new ShearSheepObjective(main);
                  shearSheepObjective.setCancelShearing(cancelShearing);
                  shearSheepObjective.setProgressNeededExpression(amountExpression);

                  main.getObjectiveManager().addObjective(shearSheepObjective, context, level);
                }));
  }

  @Override
  public String getTaskDescriptionInternal(
      final QuestPlayer questPlayer, final @Nullable ActiveObjective activeObjective) {

    return main.getLanguageManager()
        .getString(
            "chat.objectives.taskDescription.shearSheep.base",
            questPlayer,
            activeObjective,
            Map.of(
                "%AMOUNTOFSHEEP%",
                ""
                    + (activeObjective != null
                    ? activeObjective.getProgressNeeded()
                    : getProgressNeededExpression())));
  }

  @Override
  public void save(FileConfiguration configuration, String initialPath) {
    configuration.set(initialPath + ".specifics.cancelShearing", isCancelShearing());
  }

  @Override
  public void load(FileConfiguration configuration, String initialPath) {
    cancelShearing =
        configuration.getBoolean(initialPath + ".specifics.cancelShearing", false);
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


  public final boolean isCancelShearing() {
    return cancelShearing;
  }

  public void setCancelShearing(final boolean cancelShearing) {
    this.cancelShearing = cancelShearing;
  }
}