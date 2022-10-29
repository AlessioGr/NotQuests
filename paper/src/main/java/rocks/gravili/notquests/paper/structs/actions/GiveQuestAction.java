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
import cloud.commandframework.paper.PaperCommandManager;
import java.util.ArrayList;
import java.util.Locale;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.arguments.QuestSelector;
import rocks.gravili.notquests.paper.structs.Quest;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

public class GiveQuestAction extends Action {

  private String questToGiveName = "";
  private boolean forceGive = false;

  public GiveQuestAction(final NotQuests main) {
    super(main);
  }

  public static void handleCommands(
      NotQuests main,
      PaperCommandManager<CommandSender> manager,
      Command.Builder<CommandSender> builder,
      ActionFor actionFor) {
    manager.command(
        builder
            .argument(
                QuestSelector.of("quest to give", main),
                ArgumentDescription.of("Name of the Quest which should be given to the player."))
            .flag(
                manager
                    .flagBuilder("forceGive")
                    .withDescription(
                        ArgumentDescription.of(
                            "Force-gives the Quest to the player, disregarding most Quest requirements/cooldowns/...")))
            .handler(
                (context) -> {
                  final Quest foundQuest = context.get("quest to give");
                  final boolean forceGive = context.flags().isPresent("forceGive");

                  GiveQuestAction giveQuestAction = new GiveQuestAction(main);
                  giveQuestAction.setQuestToGiveName(foundQuest.getIdentifier() );
                  giveQuestAction.setForceGive(forceGive);

                  main.getActionManager().addAction(giveQuestAction, context, actionFor);
                }));
  }

  public final String getQuestToGiveName() {
    return questToGiveName;
  }

  public void setQuestToGiveName(final String questName) {
    this.questToGiveName = questName;
  }

  public final boolean isForceGive() {
    return forceGive;
  }

  public void setForceGive(final boolean forceGive) {
    this.forceGive = forceGive;
  }

  @Override
  public void executeInternally(final QuestPlayer questPlayer, Object... objects) {
    Quest foundQuest = main.getQuestManager().getQuest(getQuestToGiveName());
    if (foundQuest == null) {
      main.getLogManager()
          .warn(
              "Tried to execute GiveQuest action with null quest. Cannot find the following Quest: "
                  + getQuestToGiveName());
      return;
    }
    if (!isForceGive()) {
      main.getQuestPlayerManager().acceptQuest(questPlayer, foundQuest, true, true);
    } else {
      main.getQuestPlayerManager().forceAcceptQuestSilent(questPlayer.getUniqueId(), foundQuest);
    }
  }

  @Override
  public void save(FileConfiguration configuration, String initialPath) {
    configuration.set(initialPath + ".specifics.quest", getQuestToGiveName());
    configuration.set(initialPath + ".specifics.forceGive", isForceGive());
  }

  @Override
  public void load(final FileConfiguration configuration, String initialPath) {
    this.questToGiveName = configuration.getString(initialPath + ".specifics.quest");
    this.forceGive = configuration.getBoolean(initialPath + ".specifics.forceGive");
  }

  @Override
  public void deserializeFromSingleLineString(ArrayList<String> arguments) {
    this.questToGiveName = arguments.get(0);

    this.forceGive = String.join(" ", arguments).toLowerCase(Locale.ROOT).contains("--forcegive");
  }

  @Override
  public String getActionDescription(final QuestPlayer questPlayer, final Object... objects) {
    return "Gives Quest: " + getQuestToGiveName();
  }
}
