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

package rocks.gravili.notquestsspigot.objectives.hooks.projectkorra;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.arguments.standard.LongArgument;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.paper.PaperCommandManager;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import rocks.gravili.notquestsspigot.NotQuests;
import rocks.gravili.notquestsspigot.commands.NotQuestColors;
import rocks.gravili.notquestsspigot.objectives.Objective;
import rocks.gravili.notquestsspigot.structs.ActiveObjective;

import java.util.List;

public class ProjectKorraUseAbilityObjective extends Objective {
    private String abilityName = "";

    public ProjectKorraUseAbilityObjective(NotQuests main) {
        super(main);
    }

    public static void handleCommands(NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> addObjectiveBuilder) {
        if (!main.getIntegrationsManager().isProjectKorraEnabled()) {
            return;
        }

        manager.command(addObjectiveBuilder.literal("ProjectKorraUseAbility")
                .argument(StringArgument.<CommandSender>newBuilder("Ability").withSuggestionsProvider(
                        (context, lastString) -> {
                            final List<String> allArgs = context.getRawInput();
                            final Audience audience = main.adventure().sender(context.getSender());
                            main.getUtilManager().sendFancyCommandCompletion(audience, allArgs.toArray(new String[0]), "[Ability Name]", "");

                            return main.getIntegrationsManager().getProjectKorraManager().getAbilityCompletions();
                        }
                ).single().build(), ArgumentDescription.of("Name of the ability"))
                .argument(LongArgument.<CommandSender>newBuilder("amount").withMin(1), ArgumentDescription.of("Amount of times to use the ability"))
                .meta(CommandMeta.DESCRIPTION, "Adds a new ProjectKorraUseAbility Objective to a quest")
                .handler((context) -> {
                    String abilityName = context.get("Ability");

                    if (!main.getIntegrationsManager().getProjectKorraManager().isAbility(abilityName)) {
                        Audience audience = main.adventure().sender(context.getSender());
                        audience.sendMessage(MiniMessage.miniMessage().parse(
                                NotQuestColors.errorGradient + "Error: The ability " + NotQuestColors.highlightGradient + abilityName + "</gradient> was not found."
                        ));
                        return;
                    }

                    ProjectKorraUseAbilityObjective projectKorraUseAbilityObjective = new ProjectKorraUseAbilityObjective(main);
                    projectKorraUseAbilityObjective.setProgressNeeded(context.get("amount"));
                    projectKorraUseAbilityObjective.setAbilityName(abilityName);

                    main.getObjectiveManager().addObjective(projectKorraUseAbilityObjective, context);
                }));
    }

    public final String getAbilityName() {
        return abilityName;
    }

    public void setAbilityName(final String abilityName) {
        this.abilityName = abilityName;
    }

    @Override
    public String getObjectiveTaskDescription(final String eventualColor, final Player player) {
        return main.getLanguageManager().getString("chat.objectives.taskDescription.ProjectKorraUseAbility.base", player)
                .replace("%EVENTUALCOLOR%", eventualColor)
                .replace("%ABILITY%", getAbilityName());
    }

    @Override
    public void save(FileConfiguration configuration, String initialPath) {
        configuration.set(initialPath + ".specifics.ability", getAbilityName());
    }

    @Override
    public void load(FileConfiguration configuration, String initialPath) {
        abilityName = configuration.getString(initialPath + ".specifics.ability");
    }

    @Override
    public void onObjectiveUnlock(final ActiveObjective activeObjective) {

    }
}
