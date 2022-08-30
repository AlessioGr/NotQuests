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

package rocks.gravili.notquests.paper.managers;

import java.io.IOException;
import java.util.HashMap;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.managers.data.Category;
import rocks.gravili.notquests.paper.structs.conditions.Condition;

public class ConditionsYMLManager {
  private final NotQuests main;
  private final HashMap<String, Condition> conditionsAndIdentifiers;

  public ConditionsYMLManager(final NotQuests main) {
    this.main = main;
    conditionsAndIdentifiers = new HashMap<>();
  }

  public void loadConditions() {
    for (final Category category : main.getDataManager().getCategories()) {
      loadConditions(category);
    }
  }

  public void loadConditions(final Category category) {
    // First load from conditions.yml:
    if (category.getConditionsConfig() == null) {
      main.getLogManager()
          .severe(
              "Error: Cannot load conditions of category <highlight>"
                  + category.getCategoryFullName()
                  + "</highlight>, because it doesn't have a conditions config. This category has been skipped.");
      return;
    }

    final ConfigurationSection conditionsConfigurationSection =
        category.getConditionsConfig().getConfigurationSection("conditions");
    if (conditionsConfigurationSection != null) {
      for (final String conditionIdentifier : conditionsConfigurationSection.getKeys(false)) {
        if (conditionsAndIdentifiers.get(conditionIdentifier) != null) {
          main.getDataManager()
              .disablePluginAndSaving(
                  "Plugin disabled, because there was an error while loading conditions.yml conditions data: The conditions "
                      + conditionIdentifier
                      + " already exists.",
                  category);
          return;
        }
        if (conditionIdentifier.isBlank()) {
          main.getLogManager()
              .warn(
                  "Skipping loading the condition of category <highlight2>"
                      + category.getCategoryFullName()
                      + "</highlight2> because the condition identifier is empty.");
          continue;
        }

        final Condition condition;

        try{
          condition = Condition.loadConditionFromConfig(
              main,
              "conditions." + conditionIdentifier,
              category.getConditionsConfig(),
              category,
              conditionIdentifier,
              -1,
              null,
              null
              );
        }catch (final Exception e){
          main.getDataManager()
              .disablePluginAndSaving(
                  "Error parsing condition of conditions.yml condition with name <highlight>"
                      + conditionIdentifier
                      + "</highlight>. Error: " + e.getMessage(),
                  e,
                  category);
          return;
        }

        conditionsAndIdentifiers.put(conditionIdentifier, condition);
      }
    }
  }

  public void saveConditions(final Category category) {
    try {
      category.getConditionsConfig().save(category.getConditionsFile());
      main.getLogManager().info("Saved Data to conditions.yml");
    } catch (IOException e) {
      main.getLogManager().severe("Error saving condition. Condition were not saved...");
    }
  }

  /*public final FileConfiguration category.getConditionsConfig() {
      return conditionsConfig;
  }*/

  public final HashMap<String, Condition> getConditionsAndIdentifiers() {
    return conditionsAndIdentifiers;
  }

  public final Condition getCondition(final @NotNull String conditionIdentifier) {
    return conditionsAndIdentifiers.get(conditionIdentifier);
  }

  public final String addCondition(final String conditionIdentifier, final Condition condition) {
    final boolean nameAlreadyExists =
        getConditionsAndIdentifiers().get(conditionIdentifier) != null;
    condition.setConditionName(conditionIdentifier);

    if (!nameAlreadyExists) {
      conditionsAndIdentifiers.put(conditionIdentifier, condition);

      condition
          .getCategory()
          .getConditionsConfig()
          .set(
              "conditions." + conditionIdentifier + ".conditionType", condition.getConditionType());
      condition
          .getCategory()
          .getConditionsConfig()
          .set(
              "conditions." + conditionIdentifier + ".progressNeeded",
              condition.getProgressNeeded());
      condition
          .getCategory()
          .getConditionsConfig()
          .set("conditions." + conditionIdentifier + ".negated", condition.isNegated());
      condition
          .getCategory()
          .getConditionsConfig()
          .set("conditions." + conditionIdentifier + ".description", condition.getDescription());

      condition
          .getCategory()
          .getConditionsConfig()
          .set("conditions." + conditionIdentifier + ".hiddenStatusExpression", condition.getHiddenExpression().getRawExpression());

      condition.save(
          condition.getCategory().getConditionsConfig(), "conditions." + conditionIdentifier);

      saveConditions(condition.getCategory());

      return "<success>"
          + main.getConditionsManager().getConditionType(condition.getClass())
          + " Condition with the name <highlight>"
          + conditionIdentifier
          + "</highlight> has been created successfully!";
    } else {
      return "<error>Condition <highlight>" + conditionIdentifier + "</highlight> already exists!";
    }
  }

  public final String removeCondition(final String conditionToDeleteIdentifier) {
    conditionsAndIdentifiers
        .get(conditionToDeleteIdentifier)
        .getCategory()
        .getConditionsConfig()
        .set("conditions." + conditionToDeleteIdentifier, null);
    saveConditions(conditionsAndIdentifiers.get(conditionToDeleteIdentifier).getCategory());
    conditionsAndIdentifiers.remove(conditionToDeleteIdentifier);

    return "<success>Condition <highlight>"
        + conditionToDeleteIdentifier
        + "</highlight> successfully deleted!";
  }

  public final String removeCondition(final Condition condition) {
    condition
        .getCategory()
        .getConditionsConfig()
        .set("conditions." + condition.getConditionName(), null);
    saveConditions(condition.getCategory());
    conditionsAndIdentifiers.remove(condition.getConditionName());

    return "<success>Condition <highlight>"
        + condition.getConditionName()
        + "</highlight> successfully deleted!";
  }
}
