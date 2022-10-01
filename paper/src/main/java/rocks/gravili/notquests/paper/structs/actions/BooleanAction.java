/*
 * NotQuests - A Questing plugin for Minecraft Servers
 * Copyright (C) 2022 Alessio Gravili
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
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.paper.PaperCommandManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.arguments.variables.BooleanVariableValueArgument;
import rocks.gravili.notquests.paper.commands.arguments.variables.NumberVariableValueArgument;
import rocks.gravili.notquests.paper.managers.expressions.NumberExpression;
import rocks.gravili.notquests.paper.structs.QuestPlayer;
import rocks.gravili.notquests.paper.structs.variables.Variable;
import rocks.gravili.notquests.paper.structs.variables.VariableDataType;

public class BooleanAction extends Action {

  private static boolean alreadyLoadedOnce = false;
  private String variableName;
  private String operator;
  private HashMap<String, String> additionalStringArguments;
  private HashMap<String, NumberExpression> additionalNumberArguments;
  private HashMap<String, NumberExpression> additionalBooleanArguments;
  private Variable<?> cachedVariable;
  private NumberExpression numberExpression;

  public BooleanAction(final NotQuests main) {
    super(main);
    additionalStringArguments = new HashMap<>();
    additionalNumberArguments = new HashMap<>();
    additionalBooleanArguments = new HashMap<>();
  }

  public static void handleCommands(
      NotQuests main,
      PaperCommandManager<CommandSender> manager,
      Command.Builder<CommandSender> builder,
      ActionFor actionFor) {

    for (final String variableString : main.getVariablesManager().getVariableIdentifiers()) {

      final Variable<?> variable = main.getVariablesManager().getVariableFromString(variableString);

      if (variable == null
          || !variable.isCanSetValue()
          || variable.getVariableDataType() != VariableDataType.BOOLEAN) {
        continue;
      }
      if (main.getVariablesManager().alreadyFullRegisteredVariables.contains(variableString)) {
        continue;
      }

      if (!alreadyLoadedOnce && main.getConfiguration().isVerboseStartupMessages()) {
        main.getLogManager().info("  Registering boolean action: <highlight>" + variableString);
      }

      manager.command(
          main.getVariablesManager()
              .registerVariableCommands(variableString, builder)
              .argument(
                  StringArgument.<CommandSender>newBuilder("operator")
                      .withSuggestionsProvider(
                          (context, lastString) -> {
                            ArrayList<String> completions = new ArrayList<>();

                            completions.add("set");
                            completions.add("setNot");

                            final List<String> allArgs = context.getRawInput();
                            main.getUtilManager()
                                .sendFancyCommandCompletion(
                                    context.getSender(),
                                    allArgs.toArray(new String[0]),
                                    "[Operator]",
                                    "[...]");

                            return completions;
                          })
                      .build(),
                  ArgumentDescription.of("Operator."))
              .argument(
                  BooleanVariableValueArgument.newBuilder("expression", main, variable),
                  ArgumentDescription.of("Expression"))
              .handler(
                  (context) -> {
                    final String expression = context.get("expression");

                    final String operator = context.get("operator");

                    BooleanAction booleanAction = new BooleanAction(main);
                    booleanAction.setVariableName(variable.getVariableType());
                    booleanAction.setOperator(operator);
                    booleanAction.initializeExpressionAndCachedVariable(
                        expression, variable.getVariableType());

                    HashMap<String, String> additionalStringArguments = new HashMap<>();
                    for (StringArgument<CommandSender> stringArgument :
                        variable.getRequiredStrings()) {
                      additionalStringArguments.put(
                          stringArgument.getName(), context.get(stringArgument.getName()));
                    }

                    booleanAction.setAdditionalStringArguments(additionalStringArguments);

                    HashMap<String, NumberExpression> additionalNumberArguments = new HashMap<>();
                    for (NumberVariableValueArgument<CommandSender> numberVariableValueArgument :
                        variable.getRequiredNumbers()) {
                      additionalNumberArguments.put(
                          numberVariableValueArgument.getName(),
                          new NumberExpression(
                              main, context.get(numberVariableValueArgument.getName())));
                    }
                    booleanAction.setAdditionalNumberArguments(additionalNumberArguments);

                    HashMap<String, NumberExpression> additionalBooleanArguments = new HashMap<>();
                    for (BooleanVariableValueArgument<CommandSender> booleanArgument :
                        variable.getRequiredBooleans()) {
                      additionalBooleanArguments.put(
                          booleanArgument.getName(),
                          new NumberExpression(main, context.get(booleanArgument.getName())));
                    }
                    for (CommandFlag<?> commandFlag : variable.getRequiredBooleanFlags()) {
                      additionalBooleanArguments.put(
                          commandFlag.getName(),
                          context.flags().isPresent(commandFlag.getName())
                              ? NumberExpression.ofStatic(main, 1)
                              : NumberExpression.ofStatic(main, 0));
                    }
                    booleanAction.setAdditionalBooleanArguments(additionalBooleanArguments);

                    main.getActionManager().addAction(booleanAction, context, actionFor);
                  }));
    }
    alreadyLoadedOnce = true;
  }

  public final String getOperator() {
    return operator;
  }

  public void setOperator(final String mathOperator) {
    this.operator = mathOperator;
  }

  public final String getVariableName() {
    return variableName;
  }

  public void setVariableName(final String variableName) {
    this.variableName = variableName;
  }

  private void setAdditionalStringArguments(HashMap<String, String> additionalStringArguments) {
    this.additionalStringArguments = additionalStringArguments;
  }

  private void setAdditionalNumberArguments(
      HashMap<String, NumberExpression> additionalNumberArguments) {
    this.additionalNumberArguments = additionalNumberArguments;
  }

  private void setAdditionalBooleanArguments(
      HashMap<String, NumberExpression> additionalBooleanArguments) {
    this.additionalBooleanArguments = additionalBooleanArguments;
  }

  public void initializeExpressionAndCachedVariable(
      final String expression, final String variableName) {
    if (numberExpression == null) {
      numberExpression = new NumberExpression(main, expression);
      cachedVariable = main.getVariablesManager().getVariableFromString(variableName);
    }
  }

  @Override
  public void executeInternally(final QuestPlayer questPlayer, Object... objects) {
    if (cachedVariable == null) {
      main.sendMessage(
          questPlayer.getPlayer(),
          "<ERROR>Error: variable <highlight>"
              + variableName
              + "</highlight> not found. Report this to the Server owner.");
      return;
    }

    if (additionalStringArguments != null && !additionalStringArguments.isEmpty()) {
      cachedVariable.setAdditionalStringArguments(additionalStringArguments);
    }
    if (additionalNumberArguments != null && !additionalNumberArguments.isEmpty()) {
      cachedVariable.setAdditionalNumberArguments(additionalNumberArguments);
    }
    if (additionalBooleanArguments != null && !additionalBooleanArguments.isEmpty()) {
      cachedVariable.setAdditionalBooleanArguments(additionalBooleanArguments);
    }

    Object currentValueObject = cachedVariable.getValue(questPlayer, objects);

    /*boolean currentValue = false;
    if (currentValueObject instanceof Boolean bool) {
        currentValue = bool;
    }else{
        currentValue = (boolean) currentValueObject;
    }*/

    boolean nextNewValue = numberExpression.calculateBooleanValue(questPlayer);

    if (getOperator().equalsIgnoreCase("set")) {
      nextNewValue = nextNewValue;
    } else if (getOperator().equalsIgnoreCase("setNot")) {
      nextNewValue = !nextNewValue;
    } else {
      main.sendMessage(
          questPlayer.getPlayer(),
          "<ERROR>Error: variable operator <highlight>"
              + getOperator()
              + "</highlight> is invalid. Report this to the Server owner.");
      return;
    }

    questPlayer.sendDebugMessage("New Value: " + nextNewValue);

    if (currentValueObject instanceof Boolean) {
      ((Variable<Boolean>) cachedVariable).setValue(nextNewValue, questPlayer, objects);
    } else {
      main.getLogManager()
          .warn(
              "Cannot execute boolean action, because the number type "
                  + currentValueObject.getClass().getName()
                  + " is invalid.");
    }
  }

  @Override
  public String getActionDescription(final QuestPlayer questPlayer, final Object... objects) {
    return variableName + ": " + numberExpression.calculateBooleanValue(questPlayer);
  }

  @Override
  public void save(final FileConfiguration configuration, String initialPath) {
    configuration.set(initialPath + ".specifics.expression", numberExpression.getRawExpression());

    configuration.set(initialPath + ".specifics.variableName", getVariableName());
    configuration.set(initialPath + ".specifics.operator", getOperator());

    for (final String key : additionalStringArguments.keySet()) {
      configuration.set(
          initialPath + ".specifics.additionalStrings." + key, additionalStringArguments.get(key));
    }
    for (final String key : additionalNumberArguments.keySet()) {
      configuration.set(
          initialPath + ".specifics.additionalNumbers." + key,
          additionalNumberArguments.get(key).getRawExpression());
    }
    for (final String key : additionalBooleanArguments.keySet()) {
      configuration.set(
          initialPath + ".specifics.additionalBooleans." + key,
          additionalBooleanArguments.get(key).getRawExpression());
    }
  }

  @Override
  public void load(final FileConfiguration configuration, String initialPath) {
    this.variableName = configuration.getString(initialPath + ".specifics.variableName");
    this.operator = configuration.getString(initialPath + ".specifics.operator", "");

    initializeExpressionAndCachedVariable(
        configuration.getString(initialPath + ".specifics.expression", ""), variableName);

    final ConfigurationSection additionalStringsConfigurationSection =
        configuration.getConfigurationSection(initialPath + ".specifics.additionalStrings");
    if (additionalStringsConfigurationSection != null) {
      for (String key : additionalStringsConfigurationSection.getKeys(false)) {
        additionalStringArguments.put(
            key, configuration.getString(initialPath + ".specifics.additionalStrings." + key, ""));
      }
    }

    final ConfigurationSection additionalIntegersConfigurationSection =
        configuration.getConfigurationSection(initialPath + ".specifics.additionalNumbers");
    if (additionalIntegersConfigurationSection != null) {
      for (String key : additionalIntegersConfigurationSection.getKeys(false)) {
        additionalNumberArguments.put(
            key,
            new NumberExpression(
                main,
                configuration.getString(initialPath + ".specifics.additionalNumbers." + key, "0")));
      }
    }

    final ConfigurationSection additionalBooleansConfigurationSection =
        configuration.getConfigurationSection(initialPath + ".specifics.additionalBooleans");
    if (additionalBooleansConfigurationSection != null) {
      for (String key : additionalBooleansConfigurationSection.getKeys(false)) {
        additionalBooleanArguments.put(
            key,
            new NumberExpression(
                main,
                configuration.getString(
                    initialPath + ".specifics.additionalBooleans." + key, "false")));
      }
    }
  }

  @Override
  public void deserializeFromSingleLineString(ArrayList<String> arguments) {

    this.variableName = arguments.get(0);

    this.operator = arguments.get(1);
    initializeExpressionAndCachedVariable(arguments.get(2), variableName);

    if (arguments.size() >= 4) {

      Variable<?> variable = main.getVariablesManager().getVariableFromString(variableName);
      if (variable == null
          || !variable.isCanSetValue()
          || variable.getVariableDataType() != VariableDataType.BOOLEAN) {
        return;
      }

      int counter = 0;
      int counterStrings = 0;
      int counterNumbers = 0;
      int counterBooleans = 0;
      int counterBooleanFlags = 0;

      for (String argument : arguments) {
        counter++;
        if (counter >= 4) {
          if (variable.getRequiredStrings().size() > counterStrings) {
            additionalStringArguments.put(
                variable.getRequiredStrings().get(counter - 4).getName(), argument);
            counterStrings++;
          } else if (variable.getRequiredNumbers().size() > counterNumbers) {
            additionalNumberArguments.put(
                variable.getRequiredNumbers().get(counter - 4).getName(),
                new NumberExpression(main, argument));
            counterNumbers++;
          } else if (variable.getRequiredBooleans().size() > counterBooleans) {
            additionalBooleanArguments.put(
                variable.getRequiredBooleans().get(counter - 4).getName(),
                new NumberExpression(main, argument));
            counterBooleans++;
          } else if (variable.getRequiredBooleanFlags().size() > counterBooleanFlags) {
            additionalBooleanArguments.put(
                variable.getRequiredBooleanFlags().get(counter - 4).getName(),
                new NumberExpression(main, argument));
            counterBooleanFlags++;
          }
        }
      }
    }
  }
}
