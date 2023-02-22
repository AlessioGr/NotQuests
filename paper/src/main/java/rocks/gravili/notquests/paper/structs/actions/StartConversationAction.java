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
import rocks.gravili.notquests.paper.commands.arguments.ConversationSelector;
import rocks.gravili.notquests.paper.conversation.Conversation;
import rocks.gravili.notquests.paper.conversation.ConversationPlayer;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

public class StartConversationAction extends Action {

  private String conversationToStart = "";
  private boolean endPrevious = false;

  public StartConversationAction(final NotQuests main) {
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
                ConversationSelector.of("conversation to start", main),
                ArgumentDescription.of("Name of the Conversation which should be started."))
            .flag(
                manager
                    .flagBuilder("endPrevious")
                    .withDescription(
                        ArgumentDescription.of(
                            "Ends the previous conversation furst if the player is already in another conversation")))
            .handler(
                (context) -> {
                  final Conversation foundConversation = context.get("conversation to start");
                  final boolean endPrevious = context.flags().isPresent("endPrevious");

                  StartConversationAction startConversationAction =
                      new StartConversationAction(main);
                  startConversationAction.setConversationToStart(foundConversation.getIdentifier());
                  startConversationAction.setEndPrevious(endPrevious);

                  main.getActionManager().addAction(startConversationAction, context, actionFor);
                }));
  }

  public final String getConversationToStart() {
    return conversationToStart;
  }

  public void setConversationToStart(final String conversationToStart) {
    this.conversationToStart = conversationToStart;
  }

  public final boolean isEndPrevious() {
    return endPrevious;
  }

  public void setEndPrevious(final boolean endPrevious) {
    this.endPrevious = endPrevious;
  }

  @Override
  public void executeInternally(final QuestPlayer questPlayer, Object... objects) {
    Conversation foundConversation =
        main.getConversationManager().getConversation(getConversationToStart());
    if (foundConversation == null) {
      main.getLogManager()
          .warn(
              "Tried to execute StartConversation action with null quest. Cannot find the following Conversation: "
                  + getConversationToStart());
      questPlayer.sendDebugMessage(
          "Tried to execute StartConversation action with null quest. Cannot find the following Conversation: "
              + getConversationToStart());
      return;
    }
    ConversationPlayer openConversation =
        main.getConversationManager().getOpenConversation(questPlayer.getUniqueId());

    questPlayer.sendDebugMessage("(StartConversationAction) endPrevious: " + endPrevious);

    if (isEndPrevious() && openConversation != null) {
      questPlayer.sendDebugMessage(
          "(StartConversationAction) endPrevious is true: stopping previous conversation");
      main.getConversationManager().stopConversation(openConversation);
    }

    main.getConversationManager().playConversation(questPlayer, foundConversation, null);
  }

  @Override
  public void save(FileConfiguration configuration, String initialPath) {
    configuration.set(initialPath + ".specifics.conversation", getConversationToStart());
    configuration.set(initialPath + ".specifics.endPrevious", isEndPrevious());
  }

  @Override
  public void load(final FileConfiguration configuration, String initialPath) {
    this.conversationToStart = configuration.getString(initialPath + ".specifics.conversation");
    this.endPrevious = configuration.getBoolean(initialPath + ".specifics.endPrevious");
  }

  @Override
  public void deserializeFromSingleLineString(ArrayList<String> arguments) {
    this.conversationToStart = arguments.get(0);
    if (arguments.size() >= 2) {
      this.endPrevious =
          String.join(" ", arguments).toLowerCase(Locale.ROOT).contains("--endprevious");
    } else {
      this.endPrevious = false;
    }
  }

  @Override
  public String getActionDescription(final QuestPlayer questPlayer, final Object... objects) {
    return "Starts Conversation: " + getConversationToStart();
  }
}
