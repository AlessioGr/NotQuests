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

package rocks.gravili.notquests.paper.commands;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.arguments.standard.StringArrayArgument;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.paper.PaperCommandManager;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.arguments.ActiveQuestSelector;
import rocks.gravili.notquests.paper.commands.arguments.CategorySelector;
import rocks.gravili.notquests.paper.commands.arguments.QuestSelector;
import rocks.gravili.notquests.paper.conversation.ConversationLine;
import rocks.gravili.notquests.paper.conversation.ConversationPlayer;
import rocks.gravili.notquests.paper.managers.data.Category;
import rocks.gravili.notquests.paper.structs.ActiveQuest;
import rocks.gravili.notquests.paper.structs.Quest;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

public class UserCommands {
  private final NotQuests main;
  private final PaperCommandManager<CommandSender> manager;
  private final Command.Builder<CommandSender> builder;
  private final Component firstLevelCommands;

  private final ItemStack chest, abort, coins, books, info;

  public UserCommands(
      final NotQuests main,
      PaperCommandManager<CommandSender> manager,
      Command.Builder<CommandSender> builder) {
    this.main = main;
    this.manager = manager;
    this.builder = builder;

    chest = new ItemStack(Material.PLAYER_HEAD);
    {
      SkullMeta meta = (SkullMeta) chest.getItemMeta();
      PlayerProfile prof = Bukkit.createProfile(UUID.randomUUID(), null);
      prof.getProperties()
          .add(
              new ProfileProperty(
                  "textures",
                  "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOTcxNDUxNmU2NTY1YjgxMmZmNWIzOWVhMzljZDI4N2FmZWM4ZmNjMDZkOGYzMDUyMWNjZDhmMWI0Y2JmZGM2YiJ9fX0="));
      meta.setPlayerProfile(prof);
      chest.setItemMeta(meta);
    }

    abort = new ItemStack(Material.PLAYER_HEAD);
    {
      SkullMeta meta = (SkullMeta) abort.getItemMeta();
      PlayerProfile prof = Bukkit.createProfile(UUID.randomUUID(), null);
      prof.getProperties()
          .add(
              new ProfileProperty(
                  "textures",
                  "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZWQwYTE0MjA4NDRjZTIzN2E0NWQyZTdlNTQ0ZDEzNTg0MWU5ZjgyZDA5ZTIwMzI2N2NmODg5NmM4NTE1ZTM2MCJ9fX0="));
      meta.setPlayerProfile(prof);
      abort.setItemMeta(meta);
    }

    coins = new ItemStack(Material.PLAYER_HEAD);
    {
      SkullMeta meta = (SkullMeta) coins.getItemMeta();
      PlayerProfile prof = Bukkit.createProfile(UUID.randomUUID(), null);
      prof.getProperties()
          .add(
              new ProfileProperty(
                  "textures",
                  "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODM4MWM1MjlkNTJlMDNjZDc0YzNiZjM4YmI2YmEzZmRlMTMzN2FlOWJmNTAzMzJmYWE4ODllMGEyOGU4MDgxZiJ9fX0="));
      meta.setPlayerProfile(prof);
      coins.setItemMeta(meta);
    }

    books = new ItemStack(Material.PLAYER_HEAD);
    {
      SkullMeta meta = (SkullMeta) books.getItemMeta();
      PlayerProfile prof = Bukkit.createProfile(UUID.randomUUID(), null);
      prof.getProperties()
          .add(
              new ProfileProperty(
                  "textures",
                  "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZWVlOGQ2ZjVjYjdhMzVhNGRkYmRhNDZmMDQ3ODkxNWRkOWViYmNlZjkyNGViOGNhMjg4ZTkxZDE5YzhjYiJ9fX0="));
      meta.setPlayerProfile(prof);
      books.setItemMeta(meta);
    }

    info = new ItemStack(Material.PLAYER_HEAD);
    {
      SkullMeta meta = (SkullMeta) info.getItemMeta();
      PlayerProfile prof = Bukkit.createProfile(UUID.randomUUID(), null);
      prof.getProperties()
          .add(
              new ProfileProperty(
                  "textures",
                  "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvY2Q5MWY1MTI2NmVkZGM2MjA3ZjEyYWU4ZDdhNDljNWRiMDQxNWFkYTA0ZGFiOTJiYjc2ODZhZmRiMTdmNGQ0ZSJ9fX0="));
      meta.setPlayerProfile(prof);
      info.setItemMeta(meta);
    }

    firstLevelCommands =
        Component.text("NotQuests Player Commands:", NamedTextColor.BLUE, TextDecoration.BOLD)
            .append(Component.newline())
            .append(
                main.parse("<YELLOW>/nquests <GOLD>take <DARK_AQUA>[Quest Name]")
                    .clickEvent(ClickEvent.suggestCommand("/nquests take "))
                    .hoverEvent(
                        HoverEvent.showText(
                            Component.text("Takes/Starts a Quest", NamedTextColor.GREEN))))
            .append(Component.newline())
            .append(
                main.parse("<YELLOW>/nquests <GOLD>abort <DARK_AQUA>[Quest Name]")
                    .clickEvent(ClickEvent.suggestCommand("/nquests abort "))
                    .hoverEvent(
                        HoverEvent.showText(Component.text("Fails a Quest", NamedTextColor.GREEN))))
            .append(Component.newline())
            .append(
                main.parse("<YELLOW>/nquests <GOLD>preview <DARK_AQUA>[Quest Name]")
                    .clickEvent(ClickEvent.suggestCommand("/nquests preview "))
                    .hoverEvent(
                        HoverEvent.showText(
                            Component.text(
                                "Shows more information about a Quest", NamedTextColor.GREEN))))
            .append(Component.newline())
            .append(
                main.parse("<YELLOW>/nquests <GOLD>activeQuests")
                    .clickEvent(ClickEvent.runCommand("/nquests activeQuests"))
                    .hoverEvent(
                        HoverEvent.showText(
                            Component.text("Shows all your active Quests", NamedTextColor.GREEN))))
            .append(Component.newline())
            .append(
                main.parse("<YELLOW>/nquests <GOLD>progress <DARK_AQUA>[Quest Name]")
                    .clickEvent(ClickEvent.suggestCommand("/nquests progress "))
                    .hoverEvent(
                        HoverEvent.showText(
                            Component.text(
                                "Shows the progress of an active Quest", NamedTextColor.GREEN))))
            .append(Component.newline())
            .append(
                main.parse("<YELLOW>/nquests <GOLD>questPoints")
                    .clickEvent(ClickEvent.runCommand("/nquests questPoints"))
                    .hoverEvent(
                        HoverEvent.showText(
                            Component.text(
                                "Shows how many Quest Points you have", NamedTextColor.GREEN))))
            .append(Component.newline());

    if (main.getConfiguration().isUserCommandsUseGUI()) {
      constructGUICommands();
    } else {
      constructTextCommands();
    }
    constructCommands();
  }

  public void constructCommands() {
    manager.command(
        builder
            .literal("take")
            .senderType(Player.class)
            .argument(
                QuestSelector.<CommandSender>newBuilder("Quest Name", main)
                    .takeEnabledOnly()
                    .build(),
                ArgumentDescription.of("Quest Name"))
            .meta(CommandMeta.DESCRIPTION, "Starts a Quest.")
            .handler(
                (context) -> {
                  final Quest quest = context.get("Quest Name");

                  final Player player = (Player) context.getSender();

                  final String result =
                      main.getQuestPlayerManager().acceptQuest(player, quest, true, true);
                  if (!result.equals("accepted")) {
                    main.sendMessage(context.getSender(), result);
                  }
                }));

    manager.command(
        builder
            .literal("questPoints")
            .senderType(Player.class)
            .meta(CommandMeta.DESCRIPTION, "Starts a Quest.")
            .handler(
                (context) -> {
                  final Player player = (Player) context.getSender();
                  QuestPlayer questPlayer =
                      main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId());

                  if (questPlayer != null) {
                    context
                        .getSender()
                        .sendMessage(
                            main.parse(
                                main.getLanguageManager()
                                    .getString("chat.questpoints.query", player, questPlayer)));
                  } else {
                    context
                        .getSender()
                        .sendMessage(
                            main.parse(
                                main.getLanguageManager()
                                    .getString("chat.questpoints.none", player)));
                  }
                }));

    manager.command(
        builder
            .literal("continueConversation")
            .senderType(Player.class)
            .argument(StringArrayArgument.of("optionMessage",
                (context, lastString) -> {
                  final List<String> allArgs = context.getRawInput();
                  main.getUtilManager().sendFancyCommandCompletion(context.getSender(), allArgs.toArray(new String[0]), "<Enter option message to continue conversation>", "");
                  ArrayList<String> completions = new ArrayList<>();

                  if( main.getConversationManager() == null){
                    return completions;
                  }

                  final Player player = (Player) context.getSender();
                  final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId());
                  if(questPlayer == null){
                    return completions;
                  }
                  final ConversationPlayer openConversationPlayer = main.getConversationManager().getOpenConversation(questPlayer.getUniqueId());
                  if(openConversationPlayer == null){
                    return completions;
                  }

                  for(final ConversationLine currentPlayerLine : openConversationPlayer.getCurrentPlayerLines()){
                    completions.add(currentPlayerLine.getMessage());
                  }

                  return completions;
                }
            ), ArgumentDescription.of("Option message to continue conversation"))
            .meta(CommandMeta.DESCRIPTION, "Selects an answer for the currently open conversation")
            .handler(
                (context) -> {
                  if(main.getConversationManager() == null){
                    return;
                  }
                  final Player player = (Player) context.getSender();
                  QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId());

                  if(questPlayer != null){
                    final ConversationPlayer conversationPlayer = main.getConversationManager().getOpenConversation(player.getUniqueId());
                    if(conversationPlayer != null){
                      final String[] optionMessageStrings = context.get("optionMessage");
                      final String optionMessage = String.join(" ", optionMessageStrings);

                      for(final ConversationLine currentPlayerLine : conversationPlayer.getCurrentPlayerLines()){
                        if(currentPlayerLine.getMessage().equals(optionMessage)){
                          conversationPlayer.chooseOption(currentPlayerLine);
                          return;
                        }
                      }
                    }else{
                      questPlayer.sendDebugMessage("Tried to choose conversation option, but the conversationPlayer was not found! Active conversationPlayers count: <highlight>" + main.getConversationManager().getOpenConversations().size());
                      questPlayer.sendDebugMessage("All active conversationPlayers: <highlight>" + main.getConversationManager().getOpenConversations().toString());
                      questPlayer.sendDebugMessage("Current QuestPlayer Object: <highlight>" + questPlayer);
                      questPlayer.sendDebugMessage("Current QuestPlayer: <highlight>" + questPlayer.getPlayer().getName());
                    }
                  }
                }));
  }

  public void constructGUICommands() {
    manager.command(
        builder
            .senderType(Player.class)
            .meta(CommandMeta.DESCRIPTION, "Opens NotQuests GUI.")
            .handler(
                (context) -> {
                  final Player player = (Player) context.getSender();
                  final QuestPlayer questPlayer =
                      main.getQuestPlayerManager().getOrCreateQuestPlayer(player.getUniqueId());

                  main.getGuiManager().showMainQuestsGUI(questPlayer);
                }));

    manager.command(
        builder
            .literal("take")
            .senderType(Player.class)
            .meta(CommandMeta.DESCRIPTION, "Starts a Quest.")
            .handler(
                (context) -> {
                  final Player player = (Player) context.getSender();
                  QuestPlayer questPlayer =
                      main.getQuestPlayerManager().getOrCreateQuestPlayer(player.getUniqueId());

                  main.getGuiManager().showTakeQuestsGUI(questPlayer);
                }));

    manager.command(
        builder
            .literal("activeQuests")
            .senderType(Player.class)
            .meta(CommandMeta.DESCRIPTION, "Shows your active Quests.")
            .handler(
                (context) -> {
                  final Player player = (Player) context.getSender();
                  final QuestPlayer questPlayer =
                      main.getQuestPlayerManager().getOrCreateQuestPlayer(player.getUniqueId());

                  main.getGuiManager().showActiveQuestsGUI(questPlayer);
                }));

    manager.command(
        builder
            .literal("abort")
            .senderType(Player.class)
            .meta(CommandMeta.DESCRIPTION, "Aborts an active Quest.")
            .handler(
                (context) -> {
                  final Player player = (Player) context.getSender();
                  final QuestPlayer questPlayer =
                      main.getQuestPlayerManager().getOrCreateQuestPlayer(player.getUniqueId());
                  if (questPlayer != null) {

                    main.getGuiManager().showAbortQuestsGUI(questPlayer);
                  } else {
                    context
                        .getSender()
                        .sendMessage(
                            main.parse(
                                main.getLanguageManager()
                                    .getString("chat.no-quests-accepted", player)));
                  }
                }));

    manager.command(
        builder
            .literal("preview")
            .senderType(Player.class)
            .meta(CommandMeta.DESCRIPTION, "Shows a Preview for a Quest.")
            .handler(
                (context) -> {
                  final Player player = (Player) context.getSender();
                  final QuestPlayer questPlayer =
                      main.getQuestPlayerManager().getOrCreateQuestPlayer(player.getUniqueId());

                  main.getGuiManager().showTakeQuestsGUI(questPlayer);
                }));

    manager.command(
        builder
            .literal("abort")
            .senderType(Player.class)
            .argument(
                ActiveQuestSelector.of("Active Quest", main, null),
                ArgumentDescription.of("Name of the active Quest which should be aborted/failed"))
            .meta(CommandMeta.DESCRIPTION, "Aborts an active Quest")
            .handler(
                (context) -> {
                  final Player player = (Player) context.getSender();
                  QuestPlayer questPlayer =
                      main.getQuestPlayerManager().getOrCreateQuestPlayer(player.getUniqueId());
                  final ActiveQuest activeQuest = context.get("Active Quest");

                  if (questPlayer != null && questPlayer.getActiveQuests().size() > 0) {
                    main.getGuiManager().showAbortQuestGUI(questPlayer, activeQuest);
                  } else {
                    context
                        .getSender()
                        .sendMessage(
                            main.parse(
                                main.getLanguageManager()
                                    .getString("chat.no-quests-accepted", player)));
                  }
                }));

    manager.command(
        builder
            .literal("preview")
            .senderType(Player.class)
            .argument(
                QuestSelector.<CommandSender>newBuilder("Quest Name", main)
                    .takeEnabledOnly()
                    .build(),
                ArgumentDescription.of("Quest Name"))
            .meta(CommandMeta.DESCRIPTION, "Previews a Quest")
            .handler(
                (context) -> {
                  final Player player = (Player) context.getSender();
                  final QuestPlayer questPlayer =
                      main.getQuestPlayerManager().getOrCreateQuestPlayer((player.getUniqueId()));
                  final Quest quest = context.get("Quest Name");

                  main.getGuiManager().showPreviewQuestGUI(questPlayer, quest);
                }));

    manager.command(
        builder
            .literal("progress")
            .senderType(Player.class)
            .argument(
                ActiveQuestSelector.of("Active Quest", main, null),
                ArgumentDescription.of(
                    "Name of the active Quest of which you want to see the progress"))
            .meta(CommandMeta.DESCRIPTION, "Shows progress for an active Quest")
            .handler(
                (context) -> {
                  final Player player = (Player) context.getSender();
                  QuestPlayer questPlayer =
                      main.getQuestPlayerManager().getOrCreateQuestPlayer(player.getUniqueId());
                  if (questPlayer != null && questPlayer.getActiveQuests().size() > 0) {
                    final ActiveQuest activeQuest = context.get("Active Quest");

                    main.getGuiManager().showQuestProgressGUI(questPlayer, activeQuest);

                    /*for (final ActiveObjective activeObjective : activeQuest.getCompletedObjectives()) {

                        final Material materialToUse = Material.FILLED_MAP;

                        int count = activeObjective.getObjectiveID();
                        if (!main.getConfiguration().showObjectiveItemAmount) {
                            count = 0;
                        }

                        String descriptionToDisplay = main.getLanguageManager().getString("gui.progress.button.completedObjective.description-empty", player, activeObjective, questPlayer);
                        if (!activeObjective.getObjective().getObjectiveDescription().isBlank()) {
                            descriptionToDisplay = activeObjective.getObjective().getObjectiveDescription(main.getConfiguration().guiObjectiveDescriptionMaxLineLength);
                        }

                        group.addElement(new StaticGuiElement('e',
                                new ItemStack(materialToUse),
                                count,
                                click -> {
                                    return true;
                                },
                                convert(
                                        main.getLanguageManager().getString("gui.progress.button.completedObjective.text", player, activeObjective, questPlayer)
                                                .replace("%OBJECTIVEDESCRIPTION%", descriptionToDisplay)
                                                .replace("%COMPLETEDOBJECTIVETASKDESCRIPTION%", main.getQuestManager().getObjectiveTaskDescription(activeObjective.getObjective(), true, player))
                                )
                        ));
                    }*/

                  } else {
                    context
                        .getSender()
                        .sendMessage(
                            main.parse(
                                main.getLanguageManager()
                                    .getString("chat.no-quests-accepted", player)));
                  }
                }));
    manager.command(
        builder
            .literal("category")
            .senderType(Player.class)
            .argument(
                CategorySelector.<CommandSender>newBuilder("Category", main)
                    .takeEnabledOnly()
                    .build(),
                ArgumentDescription.of("Category Name"))
            .meta(CommandMeta.DESCRIPTION, "Opens the category view")
            .handler(
                (context) -> {
                  final Player player = (Player) context.getSender();
                  final QuestPlayer questPlayer =
                      main.getQuestPlayerManager().getOrCreateQuestPlayer((player.getUniqueId()));
                  final Category category = context.get("Category");

                  main.getGuiManager().showTakeQuestsGUIOfCategory(questPlayer, category);
                }));
  }

  public void constructTextCommands() {
    manager.command(
        builder
            .senderType(Player.class)
            .meta(CommandMeta.DESCRIPTION, "Opens NotQuests GUI.")
            .handler(
                (context) -> {
                  context.getSender().sendMessage(Component.empty());
                  context.getSender().sendMessage(firstLevelCommands);
                }));

    manager.command(
        builder
            .literal("take")
            .senderType(Player.class)
            .meta(CommandMeta.DESCRIPTION, "Starts a Quest.")
            .handler(
                (context) -> {
                  context
                      .getSender()
                      .sendMessage(
                          main.parse(
                              "<RED>Please specify the <highlight>name of the quest</highlight> you wish to take.\n"
                                  + "<YELLOW>/nquests <GOLD>take <DARK_AQUA>[Quest Name]"));
                }));

    manager.command(
        builder
            .literal("activeQuests")
            .senderType(Player.class)
            .meta(CommandMeta.DESCRIPTION, "Shows your active Quests.")
            .handler(
                (context) -> {
                  final Player player = (Player) context.getSender();
                  final QuestPlayer questPlayer =
                      main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId());
                  if (questPlayer != null) {
                    context
                        .getSender()
                        .sendMessage(
                            main.parse(
                                main.getLanguageManager()
                                    .getString("chat.active-quests-label", player)));
                    int counter = 1;
                    for (final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
                      context
                          .getSender()
                          .sendMessage(
                              main.parse(
                                  "<GREEN>"
                                      + counter
                                      + ". <YELLOW>"
                                      + activeQuest.getQuest().getDisplayNameOrIdentifier()));
                      counter += 1;
                    }

                  } else {
                    context
                        .getSender()
                        .sendMessage(
                            main.parse(
                                main.getLanguageManager()
                                    .getString("chat.no-quests-accepted", player)));
                  }
                }));

    manager.command(
        builder
            .literal("abort")
            .senderType(Player.class)
            .meta(CommandMeta.DESCRIPTION, "Aborts an active Quest.")
            .handler(
                (context) -> {
                  final Player player = (Player) context.getSender();
                  final QuestPlayer questPlayer =
                      main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId());
                  if (questPlayer != null) {
                    context
                        .getSender()
                        .sendMessage(
                            main.parse(
                                "<RED>Please specify the <highlight>name of the quest</highlight> you wish to abort (fail).\n"
                                    + "<YELLOW>/nquests <GOLD>abort <DARK_AQUA>[Quest Name]"));
                  } else {
                    context
                        .getSender()
                        .sendMessage(
                            main.parse(
                                main.getLanguageManager()
                                    .getString("chat.no-quests-accepted", player)));
                  }
                }));

    manager.command(
        builder
            .literal("preview")
            .senderType(Player.class)
            .meta(CommandMeta.DESCRIPTION, "Shows a Preview for a Quest.")
            .handler(
                (context) -> {
                  final Player player = (Player) context.getSender();
                  context
                      .getSender()
                      .sendMessage(
                          main.parse(
                              "<RED>Please specify the <highlight>name of the quest</highlight> you wish to preview.\n"
                                  + "<YELLOW>/nquests <GOLD>preview <DARK_AQUA>[Quest Name]"));
                }));

    manager.command(
        builder
            .literal("abort")
            .senderType(Player.class)
            .argument(
                ActiveQuestSelector.of("Active Quest", main, null),
                ArgumentDescription.of("Name of the active Quest which should be aborted/failed"))
            .meta(CommandMeta.DESCRIPTION, "Aborts an active Quest")
            .handler(
                (context) -> {
                  final Player player = (Player) context.getSender();
                  QuestPlayer questPlayer =
                      main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId());
                  if (questPlayer != null && questPlayer.getActiveQuests().size() > 0) {
                    final ActiveQuest activeQuest = context.get("Active Quest");

                    questPlayer.failQuest(activeQuest);
                    main.sendMessage(
                        context.getSender(),
                        main.getLanguageManager()
                            .getString("chat.quest-aborted", player, activeQuest));

                  } else {
                    context
                        .getSender()
                        .sendMessage(
                            main.parse(
                                main.getLanguageManager()
                                    .getString("chat.no-quests-accepted", player)));
                  }
                }));

    manager.command(
        builder
            .literal("preview")
            .senderType(Player.class)
            .argument(
                QuestSelector.<CommandSender>newBuilder("Quest Name", main)
                    .takeEnabledOnly()
                    .build(),
                ArgumentDescription.of("Quest Name"))
            .meta(CommandMeta.DESCRIPTION, "Previews a Quest")
            .handler(
                (context) -> {
                  final Player player = (Player) context.getSender();

                  final Quest quest = context.get("Quest Name");

                  main.getQuestManager()
                      .sendSingleQuestPreview(
                          main.getQuestPlayerManager().getOrCreateQuestPlayer(player.getUniqueId()),
                          quest);
                }));

    manager.command(
        builder
            .literal("progress")
            .senderType(Player.class)
            .argument(
                ActiveQuestSelector.of("Active Quest", main, null),
                ArgumentDescription.of(
                    "Name of the active Quest of which you want to see the progress"))
            .meta(CommandMeta.DESCRIPTION, "Shows progress for an active Quest")
            .handler(
                (context) -> {
                  final Player player = (Player) context.getSender();
                  QuestPlayer questPlayer =
                      main.getQuestPlayerManager().getOrCreateQuestPlayer(player.getUniqueId());
                  if (questPlayer != null && questPlayer.getActiveQuests().size() > 0) {
                    final ActiveQuest activeQuest = context.get("Active Quest");

                    context
                        .getSender()
                        .sendMessage(
                            main.parse(
                                "<GREEN>Completed Objectives for Quest <highlight>"
                                    + activeQuest.getQuest().getDisplayNameOrIdentifier()
                                    + "<YELLOW>:"));
                    main.getQuestManager()
                        .sendCompletedObjectivesAndProgress(questPlayer, activeQuest);
                    context
                        .getSender()
                        .sendMessage(
                            main.parse(
                                "<GREEN>Active Objectives for Quest <highlight>"
                                    + activeQuest.getQuest().getDisplayNameOrIdentifier()
                                    + "<YELLOW>:"));
                    main.getQuestManager()
                        .sendActiveObjectivesAndProgress(questPlayer, activeQuest, 0);

                  } else {
                    context
                        .getSender()
                        .sendMessage(
                            main.parse(
                                main.getLanguageManager()
                                    .getString("chat.no-quests-accepted", player)));
                  }
                }));

  }
}
