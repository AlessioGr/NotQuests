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

package rocks.gravili.notquests.paper.structs.variables;

import cloud.commandframework.arguments.standard.StringArgument;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.command.CommandSender;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

public class PermissionVariable extends Variable<Boolean> {
  public PermissionVariable(NotQuests main) {
    super(main);
    if (main.getIntegrationsManager().isLuckpermsEnabled()) {
      setCanSetValue(true);
    }

    addRequiredString(
        StringArgument.<CommandSender>newBuilder("Permission")
            .withSuggestionsProvider(
                (context, lastString) -> {
                  final List<String> allArgs = context.getRawInput();
                  main.getUtilManager()
                      .sendFancyCommandCompletion(
                          context.getSender(),
                          allArgs.toArray(new String[0]),
                          "[Permission Node]",
                          "[...]");

                  ArrayList<String> suggestions = new ArrayList<>();
                  suggestions.add("<Enter Permission node>");
                  return suggestions;
                })
            .single()
            .build());
  }

  @Override
  public Boolean getValueInternally(QuestPlayer questPlayer, Object... objects) {
    return questPlayer != null
        && questPlayer.getPlayer().hasPermission(getRequiredStringValue("Permission"));
  }

  @Override
  public boolean setValueInternally(Boolean newValue, QuestPlayer questPlayer, Object... objects) {
    if (!main.getIntegrationsManager().isLuckpermsEnabled()) {
      return false;
    }

    if (newValue) {
      main.getIntegrationsManager()
          .getLuckPermsManager()
          .givePermission(questPlayer.getUniqueId(), getRequiredStringValue("Permission"));
    } else {
      main.getIntegrationsManager()
          .getLuckPermsManager()
          .denyPermission(questPlayer.getUniqueId(), getRequiredStringValue("Permission"));
    }

    return true;
  }

  @Override
  public List<String> getPossibleValues(QuestPlayer questPlayer, Object... objects) {
    return null;
  }

  @Override
  public String getPlural() {
    return "Permissions";
  }

  @Override
  public String getSingular() {
    return "Permission";
  }
}
