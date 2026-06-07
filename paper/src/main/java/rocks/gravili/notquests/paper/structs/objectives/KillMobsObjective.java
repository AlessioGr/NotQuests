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
import org.incendo.cloud.Command;
import org.incendo.cloud.description.Description;
import org.incendo.cloud.paper.PaperCommandManager;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.ActiveObjective;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

import java.util.Map;

import static rocks.gravili.notquests.paper.commands.arguments.EntityTypeParser.entityTypeParser;
import static rocks.gravili.notquests.paper.commands.arguments.variables.NumberVariableValueParser.numberVariableParser;

public class KillMobsObjective extends Objective {

  private String mobToKillType;
  private String nameTagContainsAny = "";
  private String nameTagEquals = "";

  public KillMobsObjective(NotQuests main) {
    super(main);
  }

  public static void handleCommands(
      NotQuests main,
      PaperCommandManager<CommandSender> manager,
      Command.Builder<CommandSender> addObjectiveBuilder,
      final int level) {
    addObjectiveBuilder =
        addObjectiveBuilder
            .required("entityType", entityTypeParser(main), Description.of("Type of Entity the player has to kill."))
            .required("amount", numberVariableParser("amount", null), Description.of("Amount of kills needed"))
            .flag(main.getCommandManager().nametag_equals)
            .flag(main.getCommandManager().nametag_containsany);

    addObjectiveBuilder =
        addObjectiveBuilder.handler(
            (context) -> {
              final String entityType = context.get("entityType");
              final String amountToKillExpression = context.get("amount");

              final String[] a =
                  context
                      .flags()
                      .getValue(main.getCommandManager().nametag_equals, new String[] {""});
              final String[] b =
                  context
                      .flags()
                      .getValue(main.getCommandManager().nametag_containsany, new String[] {""});
              final String nametag_equals = String.join(" ", a);
              final String nametag_containsany = String.join(" ", b);

              KillMobsObjective killMobsObjective = new KillMobsObjective(main);

              killMobsObjective.setMobToKillType(entityType);
              killMobsObjective.setProgressNeededExpression(amountToKillExpression);

              // Add flags
              killMobsObjective.setNameTagEquals(nametag_equals);
              killMobsObjective.setNameTagContainsAny(nametag_containsany);

              main.getObjectiveManager().addObjective(killMobsObjective, context, level);

              if (!nametag_equals.isBlank()) {
                context.sender().sendMessage(main.parse("<main>With nametag_equals flag: <highlight>" + nametag_equals + "</highlight>!"));
              }
              if (!nametag_containsany.isBlank()) {
                context.sender().sendMessage(main.parse("main>With nametag_containsany flag: <highlight>" + nametag_containsany + "</highlight>!"));
              }
            });

    manager.command(addObjectiveBuilder);
  }

  @Override
  public String getTaskDescriptionInternal(
      final QuestPlayer questPlayer, final @Nullable ActiveObjective activeObjective) {
    return main.getLanguageManager()
        .getString(
            "chat.objectives.taskDescription.killMobs.base",
            questPlayer,
            activeObjective,
            Map.of("%MOBTOKILL%", getMobToKill()));
  }

  public void setMobToKillType(final String mobToKillType) {
    this.mobToKillType = mobToKillType;
  }

  @Override
  public void save(FileConfiguration configuration, String initialPath) {
    configuration.set(initialPath + ".specifics.mobToKill", getMobToKill());

    // Extra args
    if (!getNameTagContainsAny().isBlank()) {
      configuration.set(initialPath + ".extras.nameTagContainsAny", getNameTagContainsAny());
    }
    if (!getNameTagEquals().isBlank()) {
      configuration.set(initialPath + ".extras.nameTagEquals", getNameTagEquals());
    }
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

  public final String getMobToKill() {
    return mobToKillType;
  }

  // Extra args
  public final String getNameTagContainsAny() {
    return nameTagContainsAny;
  }

  public void setNameTagContainsAny(final String nameTagContainsAny) {
    this.nameTagContainsAny = nameTagContainsAny;
  }

  public final String getNameTagEquals() {
    return nameTagEquals;
  }

  public void setNameTagEquals(final String nameTagEquals) {
    this.nameTagEquals = nameTagEquals;
  }

  @Override
  public void load(FileConfiguration configuration, String initialPath) {
    mobToKillType = configuration.getString(initialPath + ".specifics.mobToKill");

    // Extras
    final String nameTagContains =
        configuration.getString(initialPath + ".extras.nameTagContainsAny", "");
    if (!nameTagContains.isBlank()) {
      setNameTagContainsAny(nameTagContains);
    }

    final String nameTagEquals = configuration.getString(initialPath + ".extras.nameTagEquals", "");
    if (!nameTagEquals.isBlank()) {
      setNameTagEquals(nameTagEquals);
    }
  }
}
