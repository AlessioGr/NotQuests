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
import rocks.gravili.notquests.paper.commands.arguments.MiniMessageSelector;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

public class SendMessageAction extends Action {

  private String messageToSend = "";

  public SendMessageAction(final NotQuests main) {
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
                MiniMessageSelector.<CommandSender>newBuilder("Sending Message", main)
                    .withPlaceholders()
                    .build(),
                ArgumentDescription.of("Message to broadcast"))
            .handler(
                (context) -> {
                  final String messageToSend =
                      String.join(" ", (String[]) context.get("Sending Message"));

                  SendMessageAction sendMessageAction = new SendMessageAction(main);
                  sendMessageAction.setMessageToSend(messageToSend);

                  main.getActionManager().addAction(sendMessageAction, context, actionFor);
                }));
  }

  public final String getMessageToSend() {
    return messageToSend;
  }

  public void setMessageToSend(final String messageToSend) {
    this.messageToSend = messageToSend;
  }

  @Override
  public void executeInternally(final QuestPlayer questPlayer, Object... objects) {
    if (getMessageToSend().isBlank()) {
      main.getLogManager().warn("Tried to execute SendMessage action with empty message.");
      return;
    }

    questPlayer
        .getPlayer()
        .sendMessage(
            main.parse(
                main.getUtilManager()
                    .applyPlaceholders(
                        getMessageToSend(), questPlayer.getPlayer(), getObjectiveHolder(), objects)));
  }

  @Override
  public void save(FileConfiguration configuration, String initialPath) {
    configuration.set(initialPath + ".specifics.message", getMessageToSend());
  }

  @Override
  public void load(final FileConfiguration configuration, String initialPath) {
    this.messageToSend = configuration.getString(initialPath + ".specifics.message", "");
  }

  @Override
  public void deserializeFromSingleLineString(ArrayList<String> arguments) {
    this.messageToSend = String.join(" ", arguments);
  }

  @Override
  public String getActionDescription(final QuestPlayer questPlayerr, final Object... objects) {
    return "Sends Message: " + getMessageToSend();
  }
}
