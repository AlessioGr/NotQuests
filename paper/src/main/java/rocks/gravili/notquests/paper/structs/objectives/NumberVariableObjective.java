package rocks.gravili.notquests.paper.structs.objectives;

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
import org.checkerframework.checker.nullness.qual.Nullable;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.arguments.variables.BooleanVariableValueArgument;
import rocks.gravili.notquests.paper.commands.arguments.variables.NumberVariableValueArgument;
import rocks.gravili.notquests.paper.managers.expressions.NumberExpression;
import rocks.gravili.notquests.paper.structs.ActiveObjective;
import rocks.gravili.notquests.paper.structs.QuestPlayer;
import rocks.gravili.notquests.paper.structs.variables.Variable;
import rocks.gravili.notquests.paper.structs.variables.VariableDataType;

public class NumberVariableObjective extends Objective { // TODO: Not done yet
  private String variableName;
  private String mathOperator;
  private HashMap<String, String> additionalStringArguments;
  private HashMap<String, NumberExpression> additionalNumberArguments;
  private HashMap<String, NumberExpression> additionalBooleanArguments;
  private Variable<?> cachedVariable;
  private boolean checkOnlyWhenCorrespondingVariableValueChanged = false;

  public NumberVariableObjective(NotQuests main) {
    super(main);
    additionalStringArguments = new HashMap<>();
    additionalNumberArguments = new HashMap<>();
    additionalBooleanArguments = new HashMap<>();
  }

  public static void handleCommands(
      final NotQuests main,
      final PaperCommandManager<CommandSender> manager,
      final Command.Builder<CommandSender> addObjectiveBuilder,
      final int level) {

    for (final String variableString : main.getVariablesManager().getVariableIdentifiers()) {

      final Variable<?> variable = main.getVariablesManager().getVariableFromString(variableString);

      if (variable == null
          || !variable.isCanSetValue()
          || variable.getVariableDataType() != VariableDataType.NUMBER) {
        continue;
      }
      if (main.getVariablesManager().alreadyFullRegisteredVariables.contains(variableString)) {
        continue;
      }

      manager.command(
          main.getVariablesManager()
              .registerVariableCommands(variableString, addObjectiveBuilder)
              .argument(
                  StringArgument.<CommandSender>newBuilder("operator")
                      .withSuggestionsProvider(
                          (context, lastString) -> {
                            ArrayList<String> completions = new ArrayList<>();
                            completions.add("equals");
                            completions.add("lessThan");
                            completions.add("moreThan");
                            completions.add("moreOrEqualThan");
                            completions.add("lessOrEqualThan");

                            final List<String> allArgs = context.getRawInput();
                            main.getUtilManager()
                                .sendFancyCommandCompletion(
                                    context.getSender(),
                                    allArgs.toArray(new String[0]),
                                    "[Math Comparison Operator]",
                                    "[...]");

                            return completions;
                          })
                      .build(),
                  ArgumentDescription.of("Math operator."))
              .argument(
                  NumberVariableValueArgument.newBuilder("amount", main, variable),
                  ArgumentDescription.of("Amount"))
              .flag(
                  manager
                      .flagBuilder("checkOnlyWhenCorrespondingVariableValueChanged")
                      .withDescription(
                          ArgumentDescription.of(
                              "This checks this objective only, when the corresponding variable value is changed via an action, instead of checking every x seconds.")))
              .handler(
                  (context) -> {
                    String amountExpression = context.get("amount");

                    final String mathOperator = context.get("operator");

                    final boolean checkOnlyWhenCorrespondingVariableValueChanged =
                        context.flags().isPresent("checkOnlyWhenCorrespondingVariableValueChanged");

                    final NumberVariableObjective numberVariableObjective =
                        new NumberVariableObjective(main);
                    numberVariableObjective.setCheckOnlyWhenCorrespondingVariableValueChanged(
                        checkOnlyWhenCorrespondingVariableValueChanged);

                    numberVariableObjective.setVariableName(variable.getVariableType());
                    numberVariableObjective.setMathOperator(mathOperator);

                    if(mathOperator.equalsIgnoreCase("moreThan")){
                      amountExpression = amountExpression + "+1";
                    }


                    numberVariableObjective.setProgressNeededExpression(amountExpression);

                    HashMap<String, String> additionalStringArguments = new HashMap<>();
                    for (StringArgument<CommandSender> stringArgument : variable.getRequiredStrings()) {
                      additionalStringArguments.put(stringArgument.getName(), context.get(stringArgument.getName()));
                    }
                    numberVariableObjective.setAdditionalStringArguments(additionalStringArguments);

                    HashMap<String, NumberExpression> additionalNumberArguments = new HashMap<>();
                    for (NumberVariableValueArgument<CommandSender> numberVariableValueArgument : variable.getRequiredNumbers()) {
                      additionalNumberArguments.put(numberVariableValueArgument.getName(), new NumberExpression(main, context.get(numberVariableValueArgument.getName())));
                    }
                    numberVariableObjective.setAdditionalNumberArguments(additionalNumberArguments);

                    HashMap<String, NumberExpression> additionalBooleanArguments = new HashMap<>();
                    for (BooleanVariableValueArgument<CommandSender> booleanArgument : variable.getRequiredBooleans()) {
                      additionalBooleanArguments.put(booleanArgument.getName(), new NumberExpression(main, context.get(booleanArgument.getName())));
                    }
                    for (CommandFlag<?> commandFlag : variable.getRequiredBooleanFlags()) {
                      additionalBooleanArguments.put(commandFlag.getName(), context.flags().isPresent(commandFlag.getName()) ? NumberExpression.ofStatic(main, 1) : NumberExpression.ofStatic(main, 0));
                    }
                    numberVariableObjective.setAdditionalBooleanArguments(additionalBooleanArguments);


                    main.getObjectiveManager().addObjective(numberVariableObjective, context, level);
                  }));
    }
  }

  @Override
  public String getTaskDescriptionInternal(
      final QuestPlayer questPlayer, final @Nullable ActiveObjective activeObjective) {
    if (variableName != null) {
      final double expressionValue = activeObjective != null ? activeObjective.getProgressNeeded() : getProgressNeededExpression().calculateValue(questPlayer);

      if (getMathOperator().equalsIgnoreCase("moreThan")) {
        return "<GRAY>-- " + variableName + " needed: More than " + (expressionValue-1) + "</GRAY>"; //-1 there to adjust for progress adjustment
      } else if (getMathOperator().equalsIgnoreCase("moreOrEqualThan")) {
        return "<GRAY>-- " + variableName + " needed: More or equal than " + expressionValue + "</GRAY>";
      } else if (getMathOperator().equalsIgnoreCase("lessThan")) {
        return "<GRAY>-- " + variableName + " needed: Less than " + expressionValue + "</GRAY>";
      } else if (getMathOperator().equalsIgnoreCase("lessOrEqualThan")) {
        return "<GRAY>-- " + variableName + " needed: Less or equal than" + expressionValue + "</GRAY>";
      } else if (getMathOperator().equalsIgnoreCase("equals")) {
        return "<GRAY>-- " + variableName + " needed: Exactly " + expressionValue + "</GRAY>";
      }

      return "<GRAY>-- " + variableName + " needed: " + expressionValue+ "</GRAY>";

    } else {
      return "<YELLOW>Error: Variable not found.";
    }
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
    configuration.set(initialPath + ".specifics.variableName", getVariableName());
    configuration.set(initialPath + ".specifics.operator", getMathOperator());

    for(final String key : additionalStringArguments.keySet()){
      configuration.set(initialPath + ".specifics.additionalStrings." + key, additionalStringArguments.get(key));
    }
    for(final String key : additionalNumberArguments.keySet()){
      configuration.set(initialPath + ".specifics.additionalNumbers." + key, additionalNumberArguments.get(key).getRawExpression());
    }
    for(final String key : additionalBooleanArguments.keySet()){
      configuration.set(initialPath + ".specifics.additionalBooleans." + key, additionalBooleanArguments.get(key).getRawExpression());
    }

    configuration.set(
        initialPath + ".specifics.checkOnlyWhenCorrespondingVariableValueChanged",
        isCheckOnlyWhenCorrespondingVariableValueChanged());
  }

  @Override
  public void onObjectiveUnlock(
      final ActiveObjective activeObjective,
      final boolean unlockedDuringPluginStartupQuestLoadingProcess) {
    activeObjective.getQuestPlayer().setHasActiveVariableObjectives(true);
    updateProgress(activeObjective);
  }

  @Override
  public void onObjectiveCompleteOrLock(
      final ActiveObjective activeObjective,
      final boolean lockedOrCompletedDuringPluginStartupQuestLoadingProcess,
      final boolean completed) {
    activeObjective.getQuestPlayer().setHasActiveVariableObjectives(false);
  }

  @Override
  public void load(FileConfiguration configuration, String initialPath) {
    this.variableName = configuration.getString(initialPath + ".specifics.variableName");
    this.mathOperator = configuration.getString(initialPath + ".specifics.operator", "");
    initializeExpressionAndCachedVariable(variableName);

    final ConfigurationSection additionalStringsConfigurationSection = configuration.getConfigurationSection(initialPath + ".specifics.additionalStrings");
    if (additionalStringsConfigurationSection != null) {
      for (String key : additionalStringsConfigurationSection.getKeys(false)) {
        additionalStringArguments.put(key, configuration.getString(initialPath + ".specifics.additionalStrings." + key, ""));
      }
    }

    final ConfigurationSection additionalIntegersConfigurationSection = configuration.getConfigurationSection(initialPath + ".specifics.additionalNumbers");
    if (additionalIntegersConfigurationSection != null) {
      for (String key : additionalIntegersConfigurationSection.getKeys(false)) {
        additionalNumberArguments.put(key, new NumberExpression(main, configuration.getString(initialPath + ".specifics.additionalNumbers." + key, "0")));
      }
    }

    final ConfigurationSection additionalBooleansConfigurationSection = configuration.getConfigurationSection(initialPath + ".specifics.additionalBooleans");
    if (additionalBooleansConfigurationSection != null) {
      for (String key : additionalBooleansConfigurationSection.getKeys(false)) {
        additionalBooleanArguments.put(key, new NumberExpression(main, configuration.getString(initialPath + ".specifics.additionalBooleans." + key, "false")));
      }
    }

    checkOnlyWhenCorrespondingVariableValueChanged =
        configuration.getBoolean(
            ".specifics.checkOnlyWhenCorrespondingVariableValueChanged", false);
  }

  public final String getMathOperator() {
    return mathOperator;
  }

  public void setMathOperator(final String mathOperator) {
    this.mathOperator = mathOperator;
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

  public void initializeExpressionAndCachedVariable(final String variableName) {
    cachedVariable = main.getVariablesManager().getVariableFromString(variableName);
  }

  public void updateProgress(final ActiveObjective activeObjective/*, final double newVariableValue*/){
    final QuestPlayer questPlayer = activeObjective.getQuestPlayer();
    questPlayer.sendDebugMessage("Updating progress for number variable objective. Variable: " + getVariableName());
    if (cachedVariable == null) {
      questPlayer.sendDebugMessage("Cached variable is null. Caching...");
      initializeExpressionAndCachedVariable(getVariableName());
    }

    if (additionalStringArguments != null && !additionalStringArguments.isEmpty()) {
      cachedVariable.setAdditionalStringArguments(additionalStringArguments);
    }
    if(additionalNumberArguments != null && !additionalNumberArguments.isEmpty()){
      cachedVariable.setAdditionalNumberArguments(additionalNumberArguments);
    }
    if(additionalBooleanArguments != null && !additionalBooleanArguments.isEmpty()){
      cachedVariable.setAdditionalBooleanArguments(additionalBooleanArguments);
    }

    Object value = cachedVariable.getValue(questPlayer);

    final double numberRequirement = activeObjective.getProgressNeeded();

    questPlayer.sendDebugMessage("Math operator: " + getMathOperator());

    if(getMathOperator().equalsIgnoreCase("moreThan") || getMathOperator().equalsIgnoreCase("moreOrEqualThan")){
      //Here we can just add the default progress. That's because when moreThan is used, the progress was already adjusted to be +1 higher than wanted
      questPlayer.sendDebugMessage("MoreOrEqualThan. value: " + value);

      if(value instanceof Long l){
        activeObjective.setProgress(l, false);
      }else if(value instanceof Float f){
        activeObjective.setProgress(f, false);
      }else if(value instanceof Double d){
        activeObjective.setProgress(d, false);

      }else if(value instanceof Integer i){
        activeObjective.setProgress(i, false);

      }else{
        activeObjective.setProgress((double)value , false);
      }
    }else if(getMathOperator().equalsIgnoreCase("lessThan")){ //TODO: Add proper progress calculation
      if(value instanceof Long l){
        if (l < numberRequirement) {
          activeObjective.setProgress(activeObjective.getProgressNeeded(), false);
        }
      }else if(value instanceof Float f){
        if (f < numberRequirement) {
          activeObjective.setProgress(activeObjective.getProgressNeeded(), false);
        }
      }else if(value instanceof Double d){
        if (d < numberRequirement) {
          activeObjective.setProgress(activeObjective.getProgressNeeded(), false);
        }
      }else if(value instanceof Integer i){
        if (i < numberRequirement) {
          activeObjective.setProgress(activeObjective.getProgressNeeded(), false);
        }
      }else{
        if ((double)value < numberRequirement) {
          activeObjective.setProgress(activeObjective.getProgressNeeded(), false);
        }
      }
    }else if(getMathOperator().equalsIgnoreCase("lessOrEqualThan")){ //TODO: Add proper progress calculation
      if(value instanceof Long l){
        if (l <= numberRequirement) {
          activeObjective.setProgress(activeObjective.getProgressNeeded(), false);
        }
      }else if(value instanceof Float f){
        if (f <= numberRequirement) {
          activeObjective.setProgress(activeObjective.getProgressNeeded(), false);
        }
      }else if(value instanceof Double d){
        if (d <= numberRequirement) {
          activeObjective.setProgress(activeObjective.getProgressNeeded(), false);
        }
      }else if(value instanceof Integer i){
        if (i <= numberRequirement) {
          activeObjective.setProgress(activeObjective.getProgressNeeded(), false);
        }
      }else{
        if ((double)value <= numberRequirement) {
          activeObjective.setProgress(activeObjective.getProgressNeeded(), false);
        }
      }
    }else if(getMathOperator().equalsIgnoreCase("equals")){ //TODO: Improve progress calculation
      if(value instanceof Long l){
        if (l == numberRequirement) {
          activeObjective.setProgress(activeObjective.getProgressNeeded(), false);
        } else if(l < numberRequirement) {
          activeObjective.setProgress(l, false);
        }//Don't add anything if it's bigger, else objective will be falsely marked as completed
      }else if(value instanceof Float f){
        if (f == numberRequirement) {
          activeObjective.setProgress(activeObjective.getProgressNeeded(), false);
        } else if(f < numberRequirement) {
          activeObjective.setProgress(f, false);
        }
      }else if(value instanceof Double d){
        if (d == numberRequirement) {
          activeObjective.setProgress(activeObjective.getProgressNeeded(), false);
        } else if(d < numberRequirement) {
          activeObjective.setProgress(d, false);
        }
      }else if(value instanceof Integer i){
        if (i == numberRequirement) {
          activeObjective.setProgress(activeObjective.getProgressNeeded(), false);
        } else if(i < numberRequirement) {
          activeObjective.setProgress(i, false);
        }
      }else{
        if ((double)value == numberRequirement) {
          activeObjective.setProgress(activeObjective.getProgressNeeded(), false);
        } else if((double)value < numberRequirement) {
          activeObjective.setProgress((double)value, false);
        }
      }
    }
  }
}
