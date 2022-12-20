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

package rocks.gravili.notquests.paper.structs.variables.hooks;

import cloud.commandframework.arguments.standard.StringArgument;
import java.util.ArrayList;
import java.util.List;
import org.betonquest.betonquest.BetonQuest;
import org.betonquest.betonquest.api.config.quest.QuestPackage;
import org.betonquest.betonquest.config.Config;
import org.betonquest.betonquest.exceptions.ObjectNotFoundException;
import org.betonquest.betonquest.id.ConditionID;
import org.betonquest.betonquest.utils.PlayerConverter;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.QuestPlayer;
import rocks.gravili.notquests.paper.structs.variables.Variable;

public class BetonQuestConditionVariable extends Variable<Boolean> {
  private ConditionID cachedConditionID = null;

  public BetonQuestConditionVariable(NotQuests main) {
    super(main);

    addRequiredString(
        StringArgument.<CommandSender>newBuilder("package")
            .withSuggestionsProvider(
                (context, lastString) -> {
                  final ArrayList<String> completions =
                      new ArrayList<>(Config.getPackages().keySet());

                  final List<String> allArgs = context.getRawInput();
                  main.getUtilManager()
                      .sendFancyCommandCompletion(
                          context.getSender(),
                          allArgs.toArray(new String[0]),
                          "[Package Name]",
                          "[Event Name]");

                  return completions;
                })
            .build());

    addRequiredString(
        StringArgument.<CommandSender>newBuilder("condition")
            .withSuggestionsProvider(
                (context, lastString) -> {
                  String packageName = context.get("package");
                  final QuestPackage configPack = Config.getPackages().get(packageName);
                  ConfigurationSection conditionsFileConfiguration =
                      configPack.getConfig().getConfigurationSection("conditions");
                  if (conditionsFileConfiguration == null) {
                    return new ArrayList<>();
                  }
                  final ArrayList<String> completions =
                      new ArrayList<>(conditionsFileConfiguration.getKeys(false));

                  final List<String> allArgs = context.getRawInput();
                  main.getUtilManager()
                      .sendFancyCommandCompletion(
                          context.getSender(),
                          allArgs.toArray(new String[0]),
                          "[Condition Name]",
                          "[...]");

                  return completions;
                })
            .build());
  }

  public final ConditionID getConditionID() {
    if (cachedConditionID == null) {
      final QuestPackage configPack = Config.getPackages().get(getRequiredStringValue("package"));
      try {
        cachedConditionID = new ConditionID(configPack, getRequiredStringValue("condition"));
      } catch (final ObjectNotFoundException e) {
        main.getLogManager()
            .warn(
                "Tried to check BetonQuestCondition Variable, but the BetonQuest condition was not found: "
                    + e.getMessage());
        return null;
      }
    }
    return cachedConditionID;
  }

  @Override
  public Boolean getValueInternally(QuestPlayer questPlayer, Object... objects) {
    return questPlayer != null
        && BetonQuest.condition(
            PlayerConverter.getID(questPlayer.getPlayer() != null ? questPlayer.getPlayer() : Bukkit.getOfflinePlayer(questPlayer.getUniqueId())),
        getConditionID());


  }

  @Override
  public boolean setValueInternally(Boolean newValue, QuestPlayer questPlayer, Object... objects) {
    return false;
  }

  @Override
  public List<String> getPossibleValues(QuestPlayer questPlayer, Object... objects) {
    return null;
  }

  @Override
  public String getPlural() {
    String together = getRequiredStringValue("package") + "." + getRequiredStringValue("condition");
    if (together.equalsIgnoreCase(".")) {
      return "BetonQuest Conditions";
    } else {
      return "BetonQuest " + together + " Conditions";
    }
  }

  @Override
  public String getSingular() {
    String together = getRequiredStringValue("package") + "." + getRequiredStringValue("condition");
    if (together.equalsIgnoreCase(".")) {
      return "BetonQuest Condition";
    } else {
      return "BetonQuest " + together + " Condition";
    }
  }
}
