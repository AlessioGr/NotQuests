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
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.paper.PaperCommandManager;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.managers.npc.NQNPC;
import rocks.gravili.notquests.paper.structs.ActiveObjective;
import rocks.gravili.notquests.paper.structs.ActiveQuest;
import rocks.gravili.notquests.paper.structs.Quest;
import rocks.gravili.notquests.paper.structs.QuestPlayer;
import rocks.gravili.notquests.paper.structs.objectives.Objective;
import rocks.gravili.notquests.paper.structs.objectives.TriggerCommandObjective;

public class TriggerCommandAction extends Action {

    private String triggerCommandName = "";


    public TriggerCommandAction(final NotQuests main) {
        super(main);
    }

    public static void handleCommands(NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> builder, ActionFor actionFor) {
        manager.command(builder
                .argument(StringArgument.<CommandSender>newBuilder("Trigger Name").withSuggestionsProvider(
                        (context, lastString) -> {
                            final List<String> allArgs = context.getRawInput();
                            main.getUtilManager().sendFancyCommandCompletion(context.getSender(), allArgs.toArray(new String[0]), "[Trigger Name]", "");

                            ArrayList<String> completions = new ArrayList<>();
                            for (final Quest quest : main.getQuestManager().getAllQuests()) {
                                for (final Objective objective : quest.getObjectives()) {
                                    if (objective instanceof final TriggerCommandObjective triggerCommandObjective) {
                                        completions.add(triggerCommandObjective.getTriggerName());
                                    }
                                }
                            }

                            return completions;
                        }
                ).single().build(), ArgumentDescription.of("Name of the trigger which should be triggered."))
                .handler((context) -> {
                    final String triggerName = context.get("Trigger Name");

                    TriggerCommandAction triggerCommandAction = new TriggerCommandAction(main);
                    triggerCommandAction.setTriggerCommand(triggerName);

                    main.getActionManager().addAction(triggerCommandAction, context, actionFor);
                }));
    }

    public final String getTriggerCommand() {
        return triggerCommandName;
    }

    public void setTriggerCommand(final String triggerCommandName) {
        this.triggerCommandName = triggerCommandName;
    }


    @Override
    public void executeInternally(final QuestPlayer questPlayer, Object... objects) {

        if (questPlayer == null || questPlayer.getPlayer() == null) {
            return;
        }

        if (questPlayer.getActiveQuests().size() > 0) {
            for (ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
                for (ActiveObjective activeObjective : activeQuest.getActiveObjectives()) {
                    if (activeObjective.isUnlocked()) {
                        if (activeObjective.getObjective() instanceof TriggerCommandObjective triggerCommandObjective) {
                            if (triggerCommandObjective.getTriggerName().equalsIgnoreCase(getTriggerCommand())) {
                                activeObjective.addProgress(1, (NQNPC) null);
                            }
                        }
                    }

                }
                activeQuest.removeCompletedObjectives(true);
            }
            questPlayer.removeCompletedQuests();
        }
    }

    @Override
    public void save(FileConfiguration configuration, String initialPath) {
        configuration.set(initialPath + ".specifics.triggerName", getTriggerCommand());
    }

    @Override
    public void load(final FileConfiguration configuration, String initialPath) {
        this.triggerCommandName = configuration.getString(initialPath + ".specifics.triggerName");
    }

    @Override
    public void deserializeFromSingleLineString(ArrayList<String> arguments) {
        this.triggerCommandName = arguments.get(0);
    }


    @Override
    public String getActionDescription(final QuestPlayer questPlayer, final Object... objects) {
        return "Triggers TriggerCommand: " + getTriggerCommand();
    }
}
