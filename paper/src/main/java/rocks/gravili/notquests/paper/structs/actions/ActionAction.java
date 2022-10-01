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
import cloud.commandframework.arguments.flags.CommandFlag;
import cloud.commandframework.arguments.standard.DurationArgument;
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.paper.PaperCommandManager;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.arguments.MultipleActionsSelector;
import rocks.gravili.notquests.paper.structs.QuestPlayer;
import rocks.gravili.notquests.paper.structs.conditions.Condition;

public class ActionAction extends Action {

  private ArrayList<Action> actions = null;
  private int amount = 1;
  private boolean ignoreConditions = false;

  private int minRandom = -1;
  private int maxRandom = -1;
  private boolean onlyCountForRandomIfConditionsFulfilled = false;

  private long executedActionDelay = -1; // Cooldown in milliseconds. -1 or smaller => no cooldown.

  public ActionAction(final NotQuests main) {
    super(main);
  }

  public static void handleCommands(
      NotQuests main,
      PaperCommandManager<CommandSender> manager,
      Command.Builder<CommandSender> builder,
      ActionFor actionFor) {

    CommandFlag<Integer> minRandomFlag =
        CommandFlag.newBuilder("minRandom")
            .withArgument(
                IntegerArgument.<CommandSender>newBuilder("minRandom")
                    .asOptionalWithDefault(1)
                    .withMin(0))
            .withDescription(
                ArgumentDescription.of(
                    "If this is set, it will only execute a random amount of quests with this minimum"))
            .build();

    CommandFlag<Integer> maxRandomFlag =
        CommandFlag.newBuilder("maxRandom")
            .withArgument(
                IntegerArgument.<CommandSender>newBuilder("maxRandom")
                    .asOptionalWithDefault(1)
                    .withMin(1))
            .withDescription(
                ArgumentDescription.of(
                    "If this is set, it will only execute a random amount of quests with this maximum"))
            .build();

    CommandFlag<Duration> executedActionDelay =
        CommandFlag.newBuilder("executedActionDelay")
            .withArgument(DurationArgument.of("executedActionDelay"))
            .withDescription(ArgumentDescription.of("Delay its actions will be executed in milliseconds. This overrides the existing delay of sub-actions."))
            .build();

    manager.command(
        builder
            .argument(
                MultipleActionsSelector.of("Actions", main),
                ArgumentDescription.of("Name of the actions which will be executed"))
            .argument(
                IntegerArgument.<CommandSender>newBuilder("amount")
                    .asOptionalWithDefault(1)
                    .withMin(1),
                ArgumentDescription.of("Amount of times the action will be executed."))
            .flag(
                manager
                    .flagBuilder("ignoreConditions")
                    .withDescription(ArgumentDescription.of("Ignores action conditions")))
            .flag(minRandomFlag)
            .flag(maxRandomFlag)
            .flag(executedActionDelay)
            .flag(
                manager
                    .flagBuilder("onlyCountForRandomIfConditionsFulfilled")
                    .withDescription(
                        ArgumentDescription.of(
                            "Does not count an action to the min or max random counter if its conditions are not fulfilled, if this flag is set")))
            .handler(
                (context) -> {
                  final ArrayList<Action> foundActions = context.get("Actions");
                  final int amount = context.get("amount");
                  final boolean ignoreConditions = context.flags().isPresent("ignoreConditions");

                  final int minRandom = context.flags().getValue(minRandomFlag, -1);
                  final int maxRandom = context.flags().getValue(maxRandomFlag, -1);
                  final boolean onlyCountForRandomIfConditionsFulfilled =
                      context.flags().isPresent("onlyCountForRandomIfConditionsFulfilled");

                  final ActionAction actionAction = new ActionAction(main);
                  actionAction.setActions(foundActions);
                  actionAction.setAmount(amount);

                  actionAction.setMinRandom(minRandom);
                  actionAction.setMaxRandom(maxRandom);
                  actionAction.setOnlyCountForRandomIfConditionsFulfilled(
                      onlyCountForRandomIfConditionsFulfilled);

                  actionAction.setIgnoreConditions(ignoreConditions);

                  if (context.flags().contains(executedActionDelay)) {
                    final Duration delayDuration =
                        context
                            .flags()
                            .getValue(
                                executedActionDelay,
                                null);
                    if(delayDuration != null){
                      actionAction.setExecutedActionDelay(delayDuration.toMillis());
                    }
                  }

                  main.getActionManager().addAction(actionAction, context, actionFor);
                }));
  }

  public int getMaxRandom() {
    return maxRandom;
  }

  public void setMaxRandom(int maxRandom) {
    this.maxRandom = maxRandom;
  }

  public int getMinRandom() {
    return minRandom;
  }

  public void setMinRandom(int minRandom) {
    this.minRandom = minRandom;
  }

  public boolean isOnlyCountForRandomIfConditionsFulfilled() {
    return onlyCountForRandomIfConditionsFulfilled;
  }

  public void setOnlyCountForRandomIfConditionsFulfilled(
      boolean onlyCountForRandomIfConditionsFulfilled) {
    this.onlyCountForRandomIfConditionsFulfilled = onlyCountForRandomIfConditionsFulfilled;
  }

  public final ArrayList<Action> getActions() {
    return actions;
  }

  public void setActions(final ArrayList<Action> actions) {
    this.actions = actions;
  }

  public final int getAmount() {
    return amount;
  }

  public void setAmount(final int amount) {
    this.amount = amount;
  }

  public final boolean isIgnoreConditions() {
    return ignoreConditions;
  }

  public void setIgnoreConditions(final boolean ignoreConditions) {
    this.ignoreConditions = ignoreConditions;
  }

    @Override
  public void executeInternally(final QuestPlayer questPlayer, Object... objects) {
    if (actions == null || actions.isEmpty()) {
      main.getLogManager().warn("Tried to execute Action Action action with no valid actions.");
      return;
    }

    main.getLogManager()
        .debug("Executing Action action. IsIgnoreConditions: " + isIgnoreConditions());

      if (minRandom == -1 && maxRandom == -1) {
        for (final Action action : getActions()) {
          if (!isIgnoreConditions()) {
            if (amount == 1) {
              main.getActionManager()
                  .executeActionWithConditions(action, questPlayer, null, true, getExecutedActionDelay(), objects);
            } else {
              for (int i = 0; i < amount; i++) {
                main.getActionManager()
                    .executeActionWithConditions(action, questPlayer, null, true, getExecutedActionDelay(), objects);
              }
            }
          } else {
            if (amount == 1) {
              action.execute(questPlayer, getExecutedActionDelay(), objects);
            } else {
              for (int i = 0; i < amount; i++) {
                action.execute(questPlayer, getExecutedActionDelay(), objects);
              }
            }
          }
        }
      } else {
        Collections.shuffle(getActions());
        final Random r = new Random();
        final int low = getMinRandom();
        final int high = getMaxRandom();
        int amountOfActionsToExecute = (low == high) ? low : r.nextInt(high + 1 - low) + low;

        for (int a = 0; a < amount; a++) {
          for (int i = 0; i <= amountOfActionsToExecute - 1; i++) {
            if (i >= getActions().size()) {
              break;
            }
            final Action actionToExecute = getActions().get(i);

            if (!isIgnoreConditions() && isOnlyCountForRandomIfConditionsFulfilled()) {
              for (final Condition condition : actionToExecute.getConditions()) {
                if (!condition.check(questPlayer).fulfilled()) {
                  amountOfActionsToExecute++;
                  continue;
                }
              }
              actionToExecute.execute(questPlayer, getExecutedActionDelay(), objects);
            } else if (!isIgnoreConditions()) {
              main.getActionManager()
                  .executeActionWithConditions(actionToExecute, questPlayer, null, true, getExecutedActionDelay(), objects);
            } else {
              actionToExecute.execute(questPlayer, getExecutedActionDelay(), objects);
            }
          }
        }
      }
  }

  @Override
  public void save(FileConfiguration configuration, String initialPath) {
    if (getActions() != null && !getActions().isEmpty()) {
      ArrayList<String> actionsStringList = new ArrayList<>();
      for (final Action action : getActions()) {
        actionsStringList.add(action.getActionName());
      }
      configuration.set(initialPath + ".specifics.actions", actionsStringList);
    } else {
      main.getLogManager()
          .warn(
              "Error: cannot save Action for action action, because it's null. Configuration path: "
                  + initialPath);
    }
    configuration.set(initialPath + ".specifics.amount", getAmount());
    configuration.set(initialPath + ".specifics.ignoreConditions", isIgnoreConditions());

    configuration.set(initialPath + ".specifics.minRandom", getMinRandom());
    configuration.set(initialPath + ".specifics.maxRandom", getMaxRandom());
    configuration.set(
        initialPath + ".specifics.onlyCountForRandomIfConditionsFulfilled",
        isOnlyCountForRandomIfConditionsFulfilled());
    if(getExecutedActionDelay() != -1){
      configuration.set(initialPath + ".specifics.executedActionDelay", getExecutedActionDelay());
    }
  }

  @Override
  public void load(final FileConfiguration configuration, String initialPath) {
    this.actions = new ArrayList<>();

    if (configuration.contains(initialPath + ".specifics.actions")) {
      final List<String> actionNames = configuration.getStringList(initialPath + ".specifics.actions");
      for (String actionName : actionNames) {
        final Action action = main.getActionsYMLManager().getAction(actionName);
        if (action == null) {
          main.getLogManager()
              .warn(
                  "Error: ActionAction cannot find the action with name "
                      + actionName
                      + ". Action Path: "
                      + initialPath);
        } else {
          actions.add(action);
        }
      }
    } else {
      String actionName = configuration.getString(initialPath + ".specifics.action");
      final Action action = main.getActionsYMLManager().getAction(actionName);
      if (action == null) {
        main.getLogManager()
            .warn(
                "Error: ActionAction cannot find the action with name "
                    + actionName
                    + ". Action Path: "
                    + initialPath);
      } else {
        actions.add(action);
      }
    }

    this.amount = configuration.getInt(initialPath + ".specifics.amount", 1);
    this.ignoreConditions =
        configuration.getBoolean(initialPath + ".specifics.ignoreConditions", false);

    this.minRandom = configuration.getInt(initialPath + ".specifics.minRandom", -1);
    this.maxRandom = configuration.getInt(initialPath + ".specifics.maxRandom", -1);
    this.onlyCountForRandomIfConditionsFulfilled =
        configuration.getBoolean(
            initialPath + ".specifics.onlyCountForRandomIfConditionsFulfilled", false);
    this.executedActionDelay = configuration.getInt(initialPath + ".specifics.executedActionDelay", -1);


  }

  @Override
  public void deserializeFromSingleLineString(ArrayList<String> arguments) {
    String actionNames = arguments.get(0);
    this.actions = new ArrayList<>();

    for (String actionName : actionNames.split(",")) {
      final Action action = main.getActionsYMLManager().getAction(actionName);
      if (action == null) {
        main.getLogManager()
            .warn(
                "Error: ActionAction cannot find the action with name "
                    + actionName
                    + ". Actions string: "
                    + arguments.get(0));
      } else {
        actions.add(action);
      }
    }

    if (arguments.size() >= 2) {
      this.amount = Integer.parseInt(arguments.get(1));
    } else {
      this.amount = 1;
    }

    this.ignoreConditions =
        String.join(" ", arguments).toLowerCase(Locale.ROOT).contains("--ignoreconditions");
  }

  @Override
  public String getActionDescription(final QuestPlayer questPlayer, final Object... objects) {
    return "Executes Actions: " + getActions().toString();
  }

  public long getExecutedActionDelay() {
    return executedActionDelay;
  }

  public void setExecutedActionDelay(long executedActionDelay) {
    this.executedActionDelay = executedActionDelay;
  }
}
