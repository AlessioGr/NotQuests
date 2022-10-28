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
import rocks.gravili.notquests.paper.commands.arguments.ConditionSelector;
import rocks.gravili.notquests.paper.structs.ActiveObjective;
import rocks.gravili.notquests.paper.structs.QuestPlayer;
import rocks.gravili.notquests.paper.structs.conditions.Condition;

public class ConditionObjective extends Objective {
  private Condition condition = null;
  private boolean checkOnlyWhenCorrespondingVariableValueChanged = false;

  public ConditionObjective(NotQuests main) {
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
                ConditionSelector.of("condition", main), ArgumentDescription.of("Condition Name"))
            .flag(
                manager
                    .flagBuilder("checkOnlyWhenCorrespondingVariableValueChanged")
                    .withDescription(
                        ArgumentDescription.of(
                            "This checks this condition only, when the corresponding variable value is changed via an action, instead of checking every x seconds.")))
            .handler(
                (context) -> {
                  final Condition condition = context.get("condition");
                  final boolean checkOnlyWhenCorrespondingVariableValueChanged =
                      context.flags().isPresent("checkOnlyWhenCorrespondingVariableValueChanged");

                  ConditionObjective conditionObjective = new ConditionObjective(main);
                  conditionObjective.setCondition(condition);
                  conditionObjective.setCheckOnlyWhenCorrespondingVariableValueChanged(
                      checkOnlyWhenCorrespondingVariableValueChanged);

                  main.getObjectiveManager().addObjective(conditionObjective, context, level);
                }));
  }

  @Override
  public String getTaskDescriptionInternal(
      final QuestPlayer questPlayer, final @Nullable ActiveObjective activeObjective) {
    if (condition != null) {
      return condition.isHidden(questPlayer) ? "Hidden" : condition.getConditionDescription(questPlayer, getObjectiveHolder());
    } else {
      return "<YELLOW>Error: Condition not found.";
    }
  }

  public final Condition getCondition() {
    return condition;
  }

  public void setCondition(final Condition condition) {
    this.condition = condition;
  }

  public final boolean isCheckOnlyWhenCorrespondingVariableValueChanged() {
    return checkOnlyWhenCorrespondingVariableValueChanged;
  }

  public void setCheckOnlyWhenCorrespondingVariableValueChanged(
      final boolean checkOnlyWhenCorrespondingVariableValueChanged) {
    this.checkOnlyWhenCorrespondingVariableValueChanged =
        checkOnlyWhenCorrespondingVariableValueChanged;
  }

  @Override
  public void save(FileConfiguration configuration, String initialPath) {
    if (condition != null) {
      configuration.set(initialPath + ".specifics.condition", getCondition().getConditionName());
    }
    configuration.set(
        initialPath + ".specifics.checkOnlyWhenCorrespondingVariableValueChanged",
        isCheckOnlyWhenCorrespondingVariableValueChanged());
  }

  @Override
  public void onObjectiveUnlock(
      final ActiveObjective activeObjective,
      final boolean unlockedDuringPluginStartupQuestLoadingProcess) {
    activeObjective.getQuestPlayer().setHasActiveConditionObjectives(true);
  }

  @Override
  public void onObjectiveCompleteOrLock(
      final ActiveObjective activeObjective,
      final boolean lockedOrCompletedDuringPluginStartupQuestLoadingProcess,
      final boolean completed) {
    activeObjective.getQuestPlayer().setHasActiveConditionObjectives(false);
  }

  @Override
  public void load(FileConfiguration configuration, String initialPath) {
    String conditionName = configuration.getString(initialPath + ".specifics.condition", "");
    condition = main.getConditionsYMLManager().getCondition(conditionName);
    if (condition == null) {
      main.getLogManager()
          .warn(
              "Error: Cannot load Condition <highlight>"
                  + conditionName
                  + "</highlight> of Condition Objective for Quest <highlight2>"
                  + getObjectiveHolder().getIdentifier()
                  + "</highlight>, because the condition does not exist.");
    }
    checkOnlyWhenCorrespondingVariableValueChanged =
        configuration.getBoolean(
            ".specifics.checkOnlyWhenCorrespondingVariableValueChanged", false);
  }
}
