package rocks.gravili.notquests.paper.structs.objectives;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.checkerframework.checker.nullness.qual.Nullable;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.framework.NQCommandBuilder;
import rocks.gravili.notquests.paper.commands.framework.NQCommandManager;
import rocks.gravili.notquests.paper.commands.framework.NQDescription;
import rocks.gravili.notquests.paper.commands.framework.NQFlag;
import rocks.gravili.notquests.paper.structs.ActiveObjective;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

import java.util.Map;

import static rocks.gravili.notquests.paper.commands.arguments.variables.NumberVariableArgument.numberVariableArgument;

public class ShearSheepObjective extends Objective {

    private boolean cancelShearing = false;

    public ShearSheepObjective(NotQuests main) {
        super(main);
    }

    public static void handleCommands(
            NotQuests main,
            NQCommandManager manager,
            NQCommandBuilder addObjectiveBuilder,
            final int level) {
        manager.command(addObjectiveBuilder
                .required("amount", numberVariableArgument("amount", null), NQDescription.of("Amount of shears needed"))
                .flag(NQFlag.builder("cancelShearing").withDescription(NQDescription.of("Makes it so the shearing will be cancelled while this objective is active")).build())
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
            final boolean unlockedDuringPluginStartupQuestLoadingProcess) {
    }

    @Override
    public void onObjectiveCompleteOrLock(
            final ActiveObjective activeObjective,
            final boolean lockedOrCompletedDuringPluginStartupQuestLoadingProcess,
            final boolean completed) {
    }


    public final boolean isCancelShearing() {
        return cancelShearing;
    }

    public void setCancelShearing(final boolean cancelShearing) {
        this.cancelShearing = cancelShearing;
    }
}