package rocks.gravili.notquests.paper.structs.objectives;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.paper.PaperCommandManager;
import java.util.ArrayList;
import java.util.Map;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.Nullable;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.arguments.MiniMessageStringSelector;
import rocks.gravili.notquests.paper.structs.ActiveObjective;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

//Basically just a holder for sub-objectives
public class ObjectiveObjective extends Objective {
    private String objectiveHolderName;

    public ObjectiveObjective(NotQuests main) {
      super(main);
    }

    public static void handleCommands(
        NotQuests main,
        PaperCommandManager<CommandSender> manager,
        Command.Builder<CommandSender> addObjectiveBuilder,
        final int level) {


      manager.command(
          addObjectiveBuilder
              .senderType(Player.class)
              .argument(
                  MiniMessageStringSelector.<CommandSender>newBuilder("Objective Holder Name", main)
                      .withPlaceholders()
                      .quoted().build(),
                  ArgumentDescription.of("Objective Holder Name"))
              .handler(
                  (context) -> {
                    final String objectiveHolderName = context.get("Objective Holder Name");

                    ObjectiveObjective objectiveObjective = new ObjectiveObjective(main);
                    objectiveObjective.setObjectiveHolderName(objectiveHolderName);

                    main.getObjectiveManager().addObjective(objectiveObjective, context, level);
                  }));
    }

    @Override
    public String getTaskDescriptionInternal(
        final QuestPlayer questPlayer, final @Nullable ActiveObjective activeObjective) {
      return main.getLanguageManager()
          .getString(
              "chat.objectives.taskDescription.objective.base",
              questPlayer,
              activeObjective,
              Map.of("%OBJECTIVEHOLDERNAME%", getObjectiveHolderName()));
    }

    @Override
    public void save(FileConfiguration configuration, String initialPath) {
      configuration.set(initialPath + ".specifics.objectiveHolderName", getObjectiveHolderName());
    }

    @Override
    public void onObjectiveUnlock(
        final ActiveObjective activeObjective,
        final boolean unlockedDuringPluginStartupQuestLoadingProcess) {
      if(activeObjective.getActiveObjectives().isEmpty()){
        activeObjective.setProgress(activeObjective.getProgressNeeded(), false);
        activeObjective.getActiveObjectiveHolder().removeCompletedObjectives(!unlockedDuringPluginStartupQuestLoadingProcess);
        activeObjective.getQuestPlayer().removeCompletedQuests();
      }
    }

    @Override
    public void onObjectiveCompleteOrLock(
        final ActiveObjective activeObjective,
        final boolean lockedOrCompletedDuringPluginStartupQuestLoadingProcess,
        final boolean completed) {}



    public final String getObjectiveHolderName() {
      return this.objectiveHolderName;
    }

    public void setObjectiveHolderName(final String objectiveHolderName) {
      this.objectiveHolderName = objectiveHolderName;
    }

    @Override
    public void load(FileConfiguration configuration, String initialPath) {
      objectiveHolderName = configuration.getString(initialPath + ".specifics.objectiveHolderName");
    }
}
