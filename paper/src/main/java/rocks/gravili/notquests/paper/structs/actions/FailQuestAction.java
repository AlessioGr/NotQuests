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
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.arguments.QuestSelector;
import rocks.gravili.notquests.paper.structs.ActiveQuest;
import rocks.gravili.notquests.paper.structs.Quest;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

public class FailQuestAction extends Action {

  private String questToFailName = "";

  public FailQuestAction(final NotQuests main) {
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
                QuestSelector.of("quest to fail", main),
                ArgumentDescription.of("Name of the Quest which should be failed for the player."))
            .handler(
                (context) -> {
                  final Quest foundQuest = context.get("quest to fail");

                  FailQuestAction failQuestAction = new FailQuestAction(main);
                  failQuestAction.setQuestToFailName(foundQuest.getIdentifier() );

                  main.getActionManager().addAction(failQuestAction, context, actionFor);
                }));
  }

  public final String getQuestToFailName() {
    return questToFailName;
  }

  public void setQuestToFailName(final String questName) {
    this.questToFailName = questName;
  }

  @Override
  public void executeInternally(final QuestPlayer questPlayer, Object... objects) {
    Quest foundQuest = main.getQuestManager().getQuest(getQuestToFailName());
    if (foundQuest == null) {
      main.getLogManager()
          .warn(
              "Tried to execute FailQuest action with null quest. Cannot find the following Quest: "
                  + getQuestToFailName());
      return;
    }

    if (questPlayer == null) {
      return;
    }

    ActiveQuest foundActiveQuest = questPlayer.getActiveQuest(foundQuest);

    if (foundActiveQuest == null) {
      return;
    }

    questPlayer.failQuest(foundActiveQuest);
  }

  @Override
  public void save(FileConfiguration configuration, String initialPath) {
    configuration.set(initialPath + ".specifics.quest", getQuestToFailName());
  }

  @Override
  public void load(final FileConfiguration configuration, String initialPath) {
    this.questToFailName = configuration.getString(initialPath + ".specifics.quest");
  }

  @Override
  public void deserializeFromSingleLineString(ArrayList<String> arguments) {
    this.questToFailName = arguments.get(0);
  }

  @Override
  public String getActionDescription(final QuestPlayer questPlayer, final Object... objects) {
    return "Fails Quest: " + getQuestToFailName();
  }
}
