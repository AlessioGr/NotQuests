package rocks.gravili.notquests.paper.structs.objectives;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.incendo.cloud.Command;
import org.incendo.cloud.description.Description;
import org.incendo.cloud.paper.LegacyPaperCommandManager;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.ActiveObjective;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

import java.util.Map;

import static rocks.gravili.notquests.paper.commands.arguments.variables.NumberVariableValueParser.numberVariableParser;

public class MilkCowObjective extends Objective {

    private boolean cancelMilking = false;

    public MilkCowObjective(NotQuests main) {
        super(main);
    }

    public static void handleCommands(
            NotQuests main,
            LegacyPaperCommandManager<CommandSender> manager,
            Command.Builder<CommandSender> addObjectiveBuilder,
            final int level) {
        manager.command(addObjectiveBuilder
                .required("amount", numberVariableParser("amount", null), Description.of("Amount of cows to milk"))
                .flag(manager.flagBuilder("cancelMilking").withDescription(Description.of("Makes it so the milking will be cancelled while this objective is active")))
                .flag(main.getCommandManager().maxDistance)
                .handler(
                        (context) -> {
                            final String amountExpression = context.get("amount");

                            final boolean cancelMilking = context.flags().isPresent("cancelMilking");

                            final MilkCowObjective milkCowObjective = new MilkCowObjective(main);
                            milkCowObjective.setCancelMilking(cancelMilking);
                            milkCowObjective.setProgressNeededExpression(amountExpression);

                            main.getObjectiveManager().addObjective(milkCowObjective, context, level);
                        }));
    }

    @Override
    public String getTaskDescriptionInternal(
            final QuestPlayer questPlayer, final @Nullable ActiveObjective activeObjective) {

        return main.getLanguageManager()
                .getString(
                        "chat.objectives.taskDescription.milkCow.base",
                        questPlayer,
                        activeObjective,
                        Map.of(
                                "%AMOUNTOFCOWS%",
                                ""
                                        + (activeObjective != null
                                        ? activeObjective.getProgressNeeded()
                                        : getProgressNeededExpression())));
    }

    @Override
    public void save(FileConfiguration configuration, String initialPath) {
        configuration.set(initialPath + ".specifics.cancelMilking", isCancelMilking());
    }

    @Override
    public void load(FileConfiguration configuration, String initialPath) {
        cancelMilking =
                configuration.getBoolean(initialPath + ".specifics.cancelMilking", false);
    }

    @Override
    public void onObjectiveUnlock(
            final ActiveObjective activeObjective,
            final boolean unlockedDuringPluginStartupQuestLoadingProcess) {
    }

    @Override
    public void onObjectiveCompleteOrLock(
            final ActiveObjective activeObjective,
            final boolean lockedOrCompletedDuringPluginStartupQuestLoadingProcess,
            final boolean completed) {
    }


    public final boolean isCancelMilking() {
        return cancelMilking;
    }

    public void setCancelMilking(final boolean cancelMilking) {
        this.cancelMilking = cancelMilking;
    }
}
