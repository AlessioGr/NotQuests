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

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.paper.PaperCommandManager;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.checkerframework.checker.nullness.qual.Nullable;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.arguments.EntityTypeSelector;
import rocks.gravili.notquests.paper.commands.arguments.variables.NumberVariableValueArgument;
import rocks.gravili.notquests.paper.structs.ActiveObjective;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

public class BreedObjective extends Objective {
  private String entityToBreedType = "";

  public BreedObjective(NotQuests main) {
    super(main);
  }

  public static void handleCommands(
      NotQuests main,
      PaperCommandManager<CommandSender> manager,
      Command.Builder<CommandSender> addObjectiveBuilder,
      final int level) {
    manager.command(
        addObjectiveBuilder
            .argument(
                EntityTypeSelector.of("entityType", main, false),
                ArgumentDescription.of("Type of Entity the player has to breed."))
            .argument(
                NumberVariableValueArgument.newBuilder("amount", main, null),
                ArgumentDescription.of("Amount of times the player needs to breed this entity."))
            .handler(
                (context) -> {
                  final String entityType = context.get("entityType");
                  final String amountExpression = context.get("amount");

                  BreedObjective breedObjective = new BreedObjective(main);
                  breedObjective.setEntityToBreedType(entityType);
                  breedObjective.setProgressNeededExpression(amountExpression);

                  main.getObjectiveManager().addObjective(breedObjective, context, level);
                }));
  }

  @Override
  public String getTaskDescriptionInternal(
      final QuestPlayer questPlayer, final @Nullable ActiveObjective activeObjective) {
    return main.getLanguageManager()
        .getString("chat.objectives.taskDescription.breed.base", questPlayer, activeObjective)
        .replace("%ENTITYTOBREED%", getEntityToBreedType());
  }

  @Override
  public void save(FileConfiguration configuration, String initialPath) {
    configuration.set(initialPath + ".specifics.mobToBreed", getEntityToBreedType());
  }

  @Override
  public void onObjectiveUnlock(
      final ActiveObjective activeObjective,
      final boolean unlockedDuringPluginStartupQuestLoadingProcess) {}

  @Override
  public void onObjectiveCompleteOrLock(
      final ActiveObjective activeObjective,
      final boolean lockedOrCompletedDuringPluginStartupQuestLoadingProcess,
      final boolean completed) {}

  public final String getEntityToBreedType() {
    return entityToBreedType;
  }

  public void setEntityToBreedType(final String entityToBreedType) {
    this.entityToBreedType = entityToBreedType;
  }

  @Override
  public void load(FileConfiguration configuration, String initialPath) {
    this.entityToBreedType = configuration.getString(initialPath + ".specifics.mobToBreed");
  }
}
