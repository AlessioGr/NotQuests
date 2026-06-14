package rocks.gravili.notquests.paper.structs.objectives;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.checkerframework.checker.nullness.qual.Nullable;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.framework.NQArguments;
import rocks.gravili.notquests.paper.commands.framework.NQCommandBuilder;
import rocks.gravili.notquests.paper.commands.framework.NQCommandManager;
import rocks.gravili.notquests.paper.commands.framework.NQDescription;
import rocks.gravili.notquests.paper.structs.ActiveObjective;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

import java.util.Map;

//Basically just a holder for sub-objectives
public class ObjectiveObjective extends Objective {
    private String objectiveHolderName;

    public ObjectiveObjective(NotQuests main) {
        super(main);
    }

    public static void handleCommands(
            NotQuests main,
            NQCommandManager manager,
            NQCommandBuilder addObjectiveBuilder,
            final int level) {


        manager.command(addObjectiveBuilder
                .required("Objective Holder Name", NQArguments.greedyStringArgument(), NQDescription.of("Name of the objective holder"))
                .handler((context) -> {
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
        if (activeObjective.getActiveObjectives().isEmpty()) {
            activeObjective.setProgress(activeObjective.getProgressNeeded(), false);
            activeObjective.getActiveObjectiveHolder().removeCompletedObjectives(!unlockedDuringPluginStartupQuestLoadingProcess);
            activeObjective.getQuestPlayer().removeCompletedQuests();
        }
    }

    @Override
    public void onObjectiveCompleteOrLock(
            final ActiveObjective activeObjective,
            final boolean lockedOrCompletedDuringPluginStartupQuestLoadingProcess,
            final boolean completed) {
    }


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
