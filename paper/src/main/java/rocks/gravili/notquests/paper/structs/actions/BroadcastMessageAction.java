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
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.arguments.MiniMessageSelector;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

public class BroadcastMessageAction extends Action {

  private String messageToBroadcast = "";

  public BroadcastMessageAction(final NotQuests main) {
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
                MiniMessageSelector.<CommandSender>newBuilder("Broadcast Message", main)
                    .withPlaceholders()
                    .build(),
                ArgumentDescription.of("Message to broadcast"))
            .handler(
                (context) -> {
                  final String messageToBroadcast =
                      String.join(" ", (String[]) context.get("Broadcast Message"));

                  BroadcastMessageAction broadcastMessageAction = new BroadcastMessageAction(main);
                  broadcastMessageAction.setMessageToBroadcast(messageToBroadcast);

                  main.getActionManager().addAction(broadcastMessageAction, context, actionFor);
                }));
  }

  public final String getMessageToBroadcast() {
    return messageToBroadcast;
  }

  public void setMessageToBroadcast(final String messageToBroadcast) {
    this.messageToBroadcast = messageToBroadcast;
  }

  @Override
  public void executeInternally(final QuestPlayer questPlayer, Object... objects) {
    if (getMessageToBroadcast().isBlank()) {
      main.getLogManager().warn("Tried to execute SendMessage action with empty message.");
      return;
    }

    Bukkit.broadcast(
        main.parse(
            main.getUtilManager()
                .applyPlaceholders(
                    getMessageToBroadcast(),
                    questPlayer.getPlayer(),
                    questPlayer,
                    getObjectiveHolder(),
                    objects)));
  }

  @Override
  public void save(FileConfiguration configuration, String initialPath) {
    configuration.set(initialPath + ".specifics.message", getMessageToBroadcast());
  }

  @Override
  public void load(final FileConfiguration configuration, String initialPath) {
    this.messageToBroadcast = configuration.getString(initialPath + ".specifics.message", "");
  }

  @Override
  public void deserializeFromSingleLineString(ArrayList<String> arguments) {
    this.messageToBroadcast = String.join(" ", arguments);
  }

  @Override
  public String getActionDescription(final QuestPlayer questPlayer, final Object... objects) {
    return "Broadcasts Message: " + getMessageToBroadcast();
  }
}
