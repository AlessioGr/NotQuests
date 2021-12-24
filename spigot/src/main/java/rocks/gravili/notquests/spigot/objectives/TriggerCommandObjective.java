/*
 * NotQuests - A Questing plugin for Minecraft Servers
 * Copyright (C) 2021 Alessio Gravili
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

package rocks.gravili.notquests.spigot.objectives;


import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.paper.PaperCommandManager;
import net.kyori.adventure.audience.Audience;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.spigot.NotQuests;
import rocks.gravili.notquests.spigot.structs.ActiveObjective;

import java.util.ArrayList;
import java.util.List;

public class TriggerCommandObjective extends Objective {

    private String triggerName;


    public TriggerCommandObjective(NotQuests main) {
        super(main);
    }

    public static void handleCommands(NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> addObjectiveBuilder) {
        manager.command(addObjectiveBuilder.literal("TriggerCommand")
                .senderType(Player.class)
                .argument(StringArgument.<CommandSender>newBuilder("Trigger name").withSuggestionsProvider(
                        (context, lastString) -> {
                            final List<String> allArgs = context.getRawInput();
                            final Audience audience = main.adventure().sender(context.getSender());
                            main.getUtilManager().sendFancyCommandCompletion(audience, allArgs.toArray(new String[0]), "[New Trigger Name]", "[Amount of triggers needed]");

                            ArrayList<String> completions = new ArrayList<>();
                            completions.add("<Enter new TriggerCommand name>");

                            return completions;
                        }
                ).single().build(), ArgumentDescription.of("Triggercommand name"))
                .argument(IntegerArgument.<CommandSender>newBuilder("amount").withMin(1), ArgumentDescription.of("Amount of times the trigger needs to be triggered to complete this objective."))
                .meta(CommandMeta.DESCRIPTION, "Adds a new TriggerCommand Objective to a quest")
                .handler((context) -> {
                    final String triggerName = context.get("Trigger name");
                    final int amount = context.get("amount");

                    TriggerCommandObjective triggerCommandObjective = new TriggerCommandObjective(main);
                    triggerCommandObjective.setProgressNeeded(amount);
                    triggerCommandObjective.setTriggerName(triggerName);

                    main.getObjectiveManager().addObjective(triggerCommandObjective, context);
                }));
    }

    @Override
    public String getObjectiveTaskDescription(final String eventualColor, final Player player) {
        return main.getLanguageManager().getString("chat.objectives.taskDescription.triggerCommand.base", player)
                .replace("%EVENTUALCOLOR%", eventualColor)
                .replace("%TRIGGERNAME%", "" + getTriggerName());
    }

    public void setTriggerName(final String triggerName) {
        this.triggerName = triggerName;
    }

    @Override
    public void save(FileConfiguration configuration, String initialPath) {
        configuration.set(initialPath + ".specifics.triggerName", getTriggerName());
    }

    @Override
    public void onObjectiveUnlock(ActiveObjective activeObjective) {

    }

    public final String getTriggerName() {
        return triggerName;
    }

    @Override
    public void load(FileConfiguration configuration, String initialPath) {
        triggerName = configuration.getString(initialPath + ".specifics.triggerName");
    }
}
