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
import cloud.commandframework.arguments.standard.StringArrayArgument;
import cloud.commandframework.paper.PaperCommandManager;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.betonquest.betonquest.BetonQuest;
import org.betonquest.betonquest.Instruction;
import org.betonquest.betonquest.api.QuestEvent;
import org.betonquest.betonquest.api.profiles.Profile;
import org.betonquest.betonquest.config.Config;
import org.betonquest.betonquest.exceptions.QuestRuntimeException;
import org.betonquest.betonquest.quest.event.legacy.QuestEventFactory;
import org.betonquest.betonquest.utils.PlayerConverter;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.QuestPlayer;
import rocks.gravili.notquests.paper.structs.actions.Action;
import rocks.gravili.notquests.paper.structs.actions.ActionFor;

public class BetonQuestFireInlineEventAction extends Action {

  private String event = "";
  private QuestEvent cachedEvent = null;

  public BetonQuestFireInlineEventAction(final NotQuests main) {
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
                StringArrayArgument.of(
                    "event",
                    (context, lastString) -> {
                      final List<String> allArgs = context.getRawInput();
                      main.getUtilManager()
                          .sendFancyCommandCompletion(
                              context.getSender(),
                              allArgs.toArray(new String[0]),
                              "<Enter new BetonQuest inline event>",
                              "");
                      ArrayList<String> completions = new ArrayList<>();
                      String rawInput = context.getRawInputJoined();
                      String curInput =
                          context
                              .getRawInputJoined()
                              .substring(
                                  context.getRawInputJoined().indexOf("BetonQuestFireInlineEvent ")
                                      + 26);
                      String[] curInputSplit = curInput.split(" ");

                      int length = curInputSplit.length;
                      if (curInput.endsWith(" ")) {
                        length++;
                      }
                      /*context.getSender().sendMessage(curInput);
                      context.getSender().sendMessage(curInputSplit);
                      context.getSender().sendMessage("Length: " + length);*/

                      if (length <= 1) {
                        Map<String, Class<? extends QuestEvent>> eventTypes = null;
                        try {
                          Class<?> betonQuestClass = BetonQuest.class;
                          Field eventTypesField = betonQuestClass.getDeclaredField("EVENT_TYPES");
                          eventTypesField.setAccessible(true);
                          eventTypes =
                              (Map<String, Class<? extends QuestEvent>>) eventTypesField.get(null);
                        } catch (Exception ignored) {
                          // ignored.printStackTrace();
                        }
                        if (eventTypes != null) {
                          completions.addAll(eventTypes.keySet());
                        }
                      } else {
                        final String eventClassName = curInputSplit[0];
                        final QuestEventFactory questEventFactory =
                            BetonQuest.getInstance().getEventFactory(eventClassName);
                        if (questEventFactory == null) {
                          return null;
                        }

                        switch (eventClassName) {
                          case "cancel":
                            if (length == 2) {
                              completions.add("<name of a quest canceler, as defined in main.yml>");
                            }
                            break;
                          case "chat":
                            completions.add("<enter chat message>");
                            break;
                          case "chestclear":
                            if (length == 2) {
                              completions.add("<location>");
                            }
                            break;
                          case "chestgive":
                            if (length == 2) {
                              completions.add("<location>");
                            } else if (length == 3) {
                              completions.add("<items>");
                            }
                            break;
                          case "chesttake":
                            if (length == 2) {
                              completions.add("<location>");
                            } else if (length == 3) {
                              completions.add("<items>");
                            }
                            break;
                          case "clear":
                            if (length == 2) {
                              completions.add("<mobs>");
                            } else if (length == 3) {
                              completions.add("<location>");
                            } else if (length == 4) {
                              completions.add("<radius around location>");
                            } else if (length == 5) {
                              completions.add("<Optional arguments>");
                            }
                            break;
                          default:
                            completions.add("<arguments>");
                        }
                      }

                      return completions;
                    }),
                ArgumentDescription.of("BetonQuest Event"))
            .handler(
                (context) -> {
                  final String event = String.join(" ", (String[]) context.get("event"));

                  // QuestEvent questEvent;

                  BetonQuestFireInlineEventAction betonQuestFireInlineEventAction =
                      new BetonQuestFireInlineEventAction(main);
                  betonQuestFireInlineEventAction.setEvent(event);

                  main.getActionManager().addAction(betonQuestFireInlineEventAction, context, actionFor);
                }));
  }

  public final String getEvent() {
    return event;
  }

  public void setEvent(final String event) {
    this.event = event;
  }

  public final QuestEvent getQuestEvent() {
    if (cachedEvent == null) {
      final QuestEventFactory questEventFactory =
          BetonQuest.getInstance().getEventFactory(getEvent().split(" ")[0]);
      if (questEventFactory == null) {
        // if it's null then there is no such type registered, log
        // an error
        main.getLogManager()
            .warn("Error: Event type " + getEvent().split(" ")[0] + " was not found!");
        return null;
      }

      String instruction = "";
      int counter = 0;
      for (String part : getEvent().split(" ")) {
        if (++counter == 2) {
          instruction += part;
        } else if (counter > 2) {
          instruction += " " + part;
        }
      }

      Instruction instructionObject =
          new Instruction(
              Config.getPackages().values().stream().findFirst().get(),
              null,
              instruction); // TODO: 1.19 check

      try {
        cachedEvent = questEventFactory.parseEventInstruction(instructionObject);
      } catch (Exception e) {
        main.getLogManager()
            .warn("Something went wrong creating BetonQuest Event from: " + getEvent());
      }
    }
    return cachedEvent;
  }

  @Override
  public void executeInternally(final QuestPlayer questPlayer, Object... objects) {
    if (getQuestEvent() != null) {
      try {
        final Profile profile = PlayerConverter.getID(questPlayer.getPlayer() != null ? questPlayer.getPlayer() : Bukkit.getOfflinePlayer(questPlayer.getUniqueId()));
        getQuestEvent().fire(profile);
      } catch (final QuestRuntimeException e) {
        main.getLogManager()
            .warn(
                "Error while firing BetonQuest '"
                    + getEvent().split(" ")[0]
                    + "' event: "
                    + e.getMessage());
      }
    }
  }

  @Override
  public void save(FileConfiguration configuration, String initialPath) {
    configuration.set(initialPath + ".specifics.event", getEvent());
  }

  @Override
  public void load(final FileConfiguration configuration, String initialPath) {
    this.event = configuration.getString(initialPath + ".specifics.event");
  }

  @Override
  public void deserializeFromSingleLineString(ArrayList<String> arguments) {
    this.event = String.join(" ", arguments);
  }

  @Override
  public String getActionDescription(final QuestPlayer questPlayer, final Object... objects) {
    return "Executes Event: " + getEvent();
  }
}
