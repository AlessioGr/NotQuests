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

package rocks.gravili.notquests.paper.structs.variables.reflectionVariables;

import cloud.commandframework.arguments.standard.StringArgument;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.command.CommandSender;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.QuestPlayer;
import rocks.gravili.notquests.paper.structs.variables.Variable;

public class ReflectionStaticBooleanVariable extends Variable<Boolean> {
  public ReflectionStaticBooleanVariable(NotQuests main) {
    super(main);
    setCanSetValue(true);

    addRequiredString(
        StringArgument.<CommandSender>newBuilder("Class Path")
            .withSuggestionsProvider(
                (context, lastString) -> {
                  final List<String> allArgs = context.getRawInput();
                  main.getUtilManager()
                      .sendFancyCommandCompletion(
                          context.getSender(),
                          allArgs.toArray(new String[0]),
                          "[Class Path]",
                          "[...]");

                  ArrayList<String> suggestions = new ArrayList<>();
                  suggestions.add("<Enter class path>");
                  return suggestions;
                })
            .single()
            .build());

    addRequiredString(
        StringArgument.<CommandSender>newBuilder("Field")
            .withSuggestionsProvider(
                (context, lastString) -> {
                  final List<String> allArgs = context.getRawInput();
                  main.getUtilManager()
                      .sendFancyCommandCompletion(
                          context.getSender(),
                          allArgs.toArray(new String[0]),
                          "[Field name]",
                          "[...]");

                  ArrayList<String> suggestions = new ArrayList<>();
                  suggestions.add("<Enter field name>");
                  return suggestions;
                })
            .single()
            .build());
  }

  @Override
  public Boolean getValueInternally(QuestPlayer questPlayer, Object... objects) {
    final String classPath = getRequiredStringValue("Class Path");
    final String fieldName = getRequiredStringValue("Field Name");

    try{
      Class<?> foundClass = Class.forName(classPath);

      Field field = foundClass.getDeclaredField(fieldName);
      field.setAccessible(true);

      return field.getBoolean(null);
    }catch (Exception e){
      main.getLogManager().warn("Reflection in ReflectionStaticBooleanVariable failed. Error: " + e.getMessage());
    }


    return null;
  }

  @Override
  public boolean setValueInternally(Boolean newValue, QuestPlayer questPlayer, Object... objects) {
    final String classPath = getRequiredStringValue("Class Path");
    final String fieldName = getRequiredStringValue("Field Name");

    try{
      Class<?> foundClass = Class.forName(classPath);

      Field field = foundClass.getDeclaredField(fieldName);
      field.setAccessible(true);

      field.setBoolean(null, newValue);
      return true;
    }catch (Exception e){
      main.getLogManager().warn("Reflection in ReflectionStaticBooleanVariable failed. Error: " + e.getMessage());
    }
    return false;
  }

  @Override
  public List<String> getPossibleValues(QuestPlayer questPlayer, Object... objects) {
    return null;
  }

  @Override
  public String getPlural() {
    return "Boolean from static reflection";
  }

  @Override
  public String getSingular() {
    return "Boolean from static reflection";
  }
}
