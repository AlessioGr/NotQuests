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

package rocks.gravili.notquests.paper.structs.actions.hooks.betonquest;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.paper.PaperCommandManager;
import java.util.ArrayList;
import java.util.List;
import org.betonquest.betonquest.BetonQuest;
import org.betonquest.betonquest.api.config.quest.QuestPackage;
import org.betonquest.betonquest.api.profiles.Profile;
import org.betonquest.betonquest.config.Config;
import org.betonquest.betonquest.exceptions.ObjectNotFoundException;
import org.betonquest.betonquest.id.EventID;
import org.betonquest.betonquest.utils.PlayerConverter;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.QuestPlayer;
import rocks.gravili.notquests.paper.structs.actions.Action;
import rocks.gravili.notquests.paper.structs.actions.ActionFor;

public class BetonQuestFireEventAction extends Action {

  private String packageName = "";
  private String eventName = "";
  private EventID cachedEventID = null;

  public BetonQuestFireEventAction(final NotQuests main) {
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
                    .build(),
                ArgumentDescription.of("BetonQuest Event Package Name"))
            .argument(
                StringArgument.<CommandSender>newBuilder("event")
                    .withSuggestionsProvider(
                        (context, lastString) -> {
                          String packageName = context.get("package");

                          final QuestPackage configPack = Config.getPackages().get(packageName);
                          ConfigurationSection eventsFileConfiguration =
                              configPack.getConfig().getConfigurationSection("events");
                          if (eventsFileConfiguration == null) {
                            return new ArrayList<>();
                          }
                          final ArrayList<String> completions =
                              new ArrayList<>(eventsFileConfiguration.getKeys(false));

                          final List<String> allArgs = context.getRawInput();
                          main.getUtilManager()
                              .sendFancyCommandCompletion(
                                  context.getSender(),
                                  allArgs.toArray(new String[0]),
                                  "[Event Name]",
                                  "[...]");

                          return completions;
                        })
                    .build(),
                ArgumentDescription.of("BetonQuest Event Name"))
            .handler(
                (context) -> {
                  String packageName = context.get("package");
                  String eventName = context.get("event");

                  // QuestEvent questEvent;

                  BetonQuestFireEventAction betonQuestFireEventAction =
                      new BetonQuestFireEventAction(main);
                  betonQuestFireEventAction.setPackageName(packageName);
                  betonQuestFireEventAction.setEventName(eventName);

                  main.getActionManager().addAction(betonQuestFireEventAction, context, actionFor);
                }));
  }

  public final String getPackageName() {
    return packageName;
  }

  public void setPackageName(final String packageName) {
    this.packageName = packageName;
  }

  public final String getEventName() {
    return eventName;
  }

  public void setEventName(final String eventName) {
    this.eventName = eventName;
  }

  public final EventID getEventID() {
    if (cachedEventID == null) {
      final QuestPackage configPack = Config.getPackages().get(getPackageName());
      try {
        cachedEventID = new EventID(configPack, getEventName());
      } catch (final ObjectNotFoundException e) {
        main.getLogManager()
            .warn(
                "Tried to execute BetonQuestFireEvent Action, but the BetonQuest event was not found: "
                    + e.getMessage());
        return null;
      }
    }
    return cachedEventID;
  }

  @Override
  public void executeInternally(final QuestPlayer questPlayer, Object... objects) {
    if (getEventID() != null) {
      final Profile profile = PlayerConverter.getID(questPlayer.getPlayer() != null ? questPlayer.getPlayer() : Bukkit.getOfflinePlayer(questPlayer.getUniqueId()));
      BetonQuest.event(profile, getEventID());
    }
  }

  @Override
  public void save(FileConfiguration configuration, String initialPath) {
    configuration.set(initialPath + ".specifics.packageName", getPackageName());
    configuration.set(initialPath + ".specifics.eventName", getEventName());
  }

  @Override
  public void load(final FileConfiguration configuration, String initialPath) {
    this.packageName = configuration.getString(initialPath + ".specifics.packageName");
    this.eventName = configuration.getString(initialPath + ".specifics.eventName");
  }

  @Override
  public void deserializeFromSingleLineString(ArrayList<String> arguments) {
    this.packageName = arguments.get(0);
    this.eventName = arguments.get(1);
  }

  @Override
  public String getActionDescription(final QuestPlayer questPlayer, final Object... objects) {
    return "Executes Event: " + getEventName() + " of package " + getPackageName();
  }
}
