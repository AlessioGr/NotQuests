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
import java.util.Iterator;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.command.CommandSender;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

public class AdvancementVariable extends Variable<Boolean> {
  public AdvancementVariable(NotQuests main) {
    super(main);
    setCanSetValue(true);

    addRequiredString(
        StringArgument.<CommandSender>newBuilder("Advancement")
            .withSuggestionsProvider(
                (context, lastString) -> {
                  final List<String> allArgs = context.getRawInput();
                  main.getUtilManager()
                      .sendFancyCommandCompletion(
                          context.getSender(),
                          allArgs.toArray(new String[0]),
                          "[Advancement Name]",
                          "[...]");

                  ArrayList<String> suggestions = new ArrayList<>();
                  Iterator<Advancement> advancements = Bukkit.getServer().advancementIterator();
                  while (advancements.hasNext()) {
                    Advancement advancement = advancements.next();
                    suggestions.add(advancement.getKey().getKey());
                  }
                  return suggestions;
                })
            .single()
            .build());
  }

  @Override
  public Boolean getValueInternally(QuestPlayer questPlayer, Object... objects) {
    NamespacedKey namespacedKey = NamespacedKey.fromString(getRequiredStringValue("Advancement"));
    if (namespacedKey == null) {
      return false;
    }
    Advancement advancement = Bukkit.getAdvancement(namespacedKey);
    if (advancement == null) {
      return false;
    }
    return questPlayer != null
        && questPlayer.getPlayer().getAdvancementProgress(advancement).isDone();
  }

  @Override
  public boolean setValueInternally(Boolean newValue, QuestPlayer questPlayer, Object... objects) {
    NamespacedKey namespacedKey = NamespacedKey.fromString(getRequiredStringValue("Advancement"));
    if (namespacedKey == null) {
      return false;
    }
    Advancement advancement = Bukkit.getAdvancement(namespacedKey);
    if (advancement == null) {
      return false;
    }

    AdvancementProgress progress = questPlayer.getPlayer().getAdvancementProgress(advancement);

    if (newValue) {
      for (String criteria : progress.getRemainingCriteria()) {
        progress.awardCriteria(criteria);
      }
    } else {
      for (String criteria : progress.getAwardedCriteria()) {
        progress.revokeCriteria(criteria);
      }
    }

    return true;
  }

  @Override
  public List<String> getPossibleValues(QuestPlayer questPlayer, Object... objects) {
    return null;
  }

  @Override
  public String getPlural() {
    return "Advancements";
  }

  @Override
  public String getSingular() {
    return "Advancement";
  }
}
