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

package rocks.gravili.notquests.paper.structs.triggers.types;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.paper.PaperCommandManager;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.triggers.Trigger;

public class WorldLeaveTrigger extends Trigger {

  private String worldToLeaveName;

  public WorldLeaveTrigger(final NotQuests main) {
    super(main);
  }

  public static void handleCommands(
      NotQuests main,
      PaperCommandManager<CommandSender> manager,
      Command.Builder<CommandSender> addTriggerBuilder) {
    manager.command(
        addTriggerBuilder
            .argument(
                StringArgument.<CommandSender>newBuilder("world to leave")
                    .withSuggestionsProvider(
                        (context, lastString) -> {
                          final List<String> allArgs = context.getRawInput();
                          main.getUtilManager()
                              .sendFancyCommandCompletion(
                                  context.getSender(),
                                  allArgs.toArray(new String[0]),
                                  "[World Name / 'ALL']",
                                  "[Amount of Leaves]");

                          ArrayList<String> completions = new ArrayList<>();

                          completions.add("ALL");

                          for (final World world : Bukkit.getWorlds()) {
                            completions.add(world.getName());
                          }

                          return completions;
                        })
                    .single()
                    .build(),
                ArgumentDescription.of("Name of the world which needs to be left"))
            .argument(
                IntegerArgument.<CommandSender>newBuilder("amount").withMin(1),
                ArgumentDescription.of("Amount of times the world needs to be left."))
            .flag(main.getCommandManager().applyOn)
            .flag(main.getCommandManager().triggerWorldString)
            .meta(CommandMeta.DESCRIPTION, "Triggers when the player leaves a specific world.")
            .handler(
                (context) -> {
                  final String worldToLeaveName = context.get("world to leave");

                  WorldLeaveTrigger worldLeaveTrigger = new WorldLeaveTrigger(main);
                  worldLeaveTrigger.setWorldToLeaveName(worldToLeaveName);

                  main.getTriggerManager().addTrigger(worldLeaveTrigger, context);
                }));
  }

  public final String getWorldToLeaveName() {
    return worldToLeaveName;
  }

  public void setWorldToLeaveName(final String worldToLeaveName) {
    this.worldToLeaveName = worldToLeaveName;
  }

  @Override
  public void save(FileConfiguration configuration, String initialPath) {
    configuration.set(initialPath + ".specifics.worldToLeave", getWorldToLeaveName());
  }

  @Override
  public String getTriggerDescription() {
    return "World to leave: <WHITE>" + getWorldToLeaveName();
  }

  @Override
  public void load(FileConfiguration configuration, String initialPath) {
    this.worldToLeaveName = configuration.getString(initialPath + ".specifics.worldToLeave", "ALL");
  }
}
