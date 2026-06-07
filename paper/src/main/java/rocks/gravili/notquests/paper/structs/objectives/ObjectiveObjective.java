package rocks.gravili.notquests.paper.structs.objectives;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.incendo.cloud.Command;
import org.incendo.cloud.description.Description;
import org.incendo.cloud.paper.PaperCommandManager;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.ActiveObjective;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

import java.util.Map;

import static org.incendo.cloud.parser.standard.StringParser.greedyStringParser;

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


        manager.command(addObjectiveBuilder
                .required("Objective Holder Name", greedyStringParser(), Description.description("Name of the objective holder"))
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
