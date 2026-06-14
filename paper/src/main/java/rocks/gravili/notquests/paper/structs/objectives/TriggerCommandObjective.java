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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static rocks.gravili.notquests.paper.commands.arguments.variables.NumberVariableArgument.numberVariableArgument;

public class TriggerCommandObjective extends Objective {

    private String triggerName;

    public TriggerCommandObjective(NotQuests main) {
        super(main);
    }

    public static void handleCommands(
            NotQuests main,
            NQCommandManager manager,
            NQCommandBuilder addObjectiveBuilder,
            final int level) {
        manager.command(addObjectiveBuilder
                .required("Trigger name", NQArguments.stringArgument(), NQDescription.of("Triggercommand name"), (context, input) -> {
                    List<String> completions = new ArrayList<>();
                    completions.add("<Enter new TriggerCommand name>");
                    return completions;
                })
                .required("amount", numberVariableArgument("amount", null), NQDescription.of("Amount of times the trigger needs to be triggered to complete this objective."))
                .handler((context) -> {
                    final String triggerName = context.get("Trigger name");
                    final String amountExpression = context.get("amount");

                    TriggerCommandObjective triggerCommandObjective =
                            new TriggerCommandObjective(main);
                    triggerCommandObjective.setProgressNeededExpression(amountExpression);
                    triggerCommandObjective.setTriggerName(triggerName);

                    main.getObjectiveManager().addObjective(triggerCommandObjective, context, level);
                }));
    }

    @Override
    public String getTaskDescriptionInternal(
            final QuestPlayer questPlayer, final @Nullable ActiveObjective activeObjective) {
        return main.getLanguageManager()
                .getString(
                        "chat.objectives.taskDescription.triggerCommand.base",
                        questPlayer,
                        activeObjective,
                        Map.of("%TRIGGERNAME%", getTriggerName()));
    }

    @Override
    public void save(FileConfiguration configuration, String initialPath) {
        configuration.set(initialPath + ".specifics.triggerName", getTriggerName());
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

    public final String getTriggerName() {
        return triggerName;
    }

    public void setTriggerName(final String triggerName) {
        this.triggerName = triggerName;
    }

    @Override
    public void load(FileConfiguration configuration, String initialPath) {
        triggerName = configuration.getString(initialPath + ".specifics.triggerName");
    }
}
