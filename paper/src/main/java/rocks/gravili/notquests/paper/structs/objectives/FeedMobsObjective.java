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
import rocks.gravili.notquests.paper.commands.framework.NQCommandBuilder;
import rocks.gravili.notquests.paper.commands.framework.NQCommandManager;
import rocks.gravili.notquests.paper.commands.framework.NQDescription;
import rocks.gravili.notquests.paper.structs.ActiveObjective;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

import static rocks.gravili.notquests.paper.commands.arguments.EntityTypeArgument.entityTypeArgument;
import static rocks.gravili.notquests.paper.commands.arguments.variables.NumberVariableArgument.numberVariableArgument;

public class FeedMobsObjective extends Objective {
  private String entityToFeedType = "";

  public FeedMobsObjective(NotQuests main) {
    super(main);
  }

  public static void handleCommands(
      NotQuests main,
      NQCommandManager manager,
      NQCommandBuilder addObjectiveBuilder,
      final int level) {
    manager.command(addObjectiveBuilder
            .required("entityType", entityTypeArgument(main, false), NQDescription.of("Type of Entity the player has to feed."))
            .required("amount", numberVariableArgument("amount", null), NQDescription.of("Amount of times the player needs to feed this entity."))
            .handler(
                (context) -> {
                  final String entityType = context.get("entityType");
                  final String amountExpression = context.get("amount");

                  FeedMobsObjective feedMobsObjective = new FeedMobsObjective(main);
                  feedMobsObjective.setEntityToFeedType(entityType);
                  feedMobsObjective.setProgressNeededExpression(amountExpression);

                  main.getObjectiveManager().addObjective(feedMobsObjective, context, level);
                }));
  }

  @Override
  public String getTaskDescriptionInternal(
      final QuestPlayer questPlayer, final @Nullable ActiveObjective activeObjective) {
    return main.getLanguageManager()
        .getString("chat.objectives.taskDescription.feedMobs.base", questPlayer, activeObjective)
        .replace("%ENTITYTOFEED%", getEntityToFeedType());
  }

  @Override
  public void save(FileConfiguration configuration, String initialPath) {
    configuration.set(initialPath + ".specifics.mobToFeed", getEntityToFeedType());
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

  public final String getEntityToFeedType() {
    return entityToFeedType;
  }

  public void setEntityToFeedType(final String entityToFeedType) {
    this.entityToFeedType = entityToFeedType;
  }

  @Override
  public void load(FileConfiguration configuration, String initialPath) {
    this.entityToFeedType = configuration.getString(initialPath + ".specifics.mobToFeed");
  }
}
