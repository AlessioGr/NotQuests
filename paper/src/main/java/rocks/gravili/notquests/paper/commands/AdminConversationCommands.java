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

package rocks.gravili.notquests.paper.commands;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.paper.PaperCommandManager;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.apache.commons.io.IOUtils;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.arguments.CategorySelector;
import rocks.gravili.notquests.paper.commands.arguments.ConversationSelector;
import rocks.gravili.notquests.paper.commands.arguments.NQNPCSelector;
import rocks.gravili.notquests.paper.commands.arguments.SpeakerSelector;
import rocks.gravili.notquests.paper.commands.arguments.wrappers.NQNPCResult;
import rocks.gravili.notquests.paper.conversation.Conversation;
import rocks.gravili.notquests.paper.conversation.ConversationLine;
import rocks.gravili.notquests.paper.conversation.ConversationManager;
import rocks.gravili.notquests.paper.conversation.Speaker;
import rocks.gravili.notquests.paper.managers.data.Category;
import rocks.gravili.notquests.paper.managers.npc.NQNPC;

public class AdminConversationCommands {
  private final NotQuests main;
  private final PaperCommandManager<CommandSender> manager;
  private final Command.Builder<CommandSender> conversationBuilder;

  private final ConversationManager conversationManager;

  public AdminConversationCommands(
      final NotQuests main,
      PaperCommandManager<CommandSender> manager,
      Command.Builder<CommandSender> conversationBuilder,
      final ConversationManager conversationManager) {
    this.main = main;
    this.manager = manager;
    this.conversationBuilder = conversationBuilder;

    this.conversationManager = conversationManager;

    manager.command(
        conversationBuilder
            .literal("create")
            .argument(
                StringArgument.<CommandSender>newBuilder("Conversation Name")
                    .withSuggestionsProvider(
                        (context, lastString) -> {
                          final List<String> allArgs = context.getRawInput();
                          main.getUtilManager()
                              .sendFancyCommandCompletion(
                                  context.getSender(),
                                  allArgs.toArray(new String[0]),
                                  "[New Conversation Name]",
                                  "");

                          ArrayList<String> completions = new ArrayList<>();

                          completions.add("<Enter new Conversation Name>");
                          return completions;
                        })
                    .single()
                    .build(),
                ArgumentDescription.of("Conversation Name"))
            .flag(
                manager
                    .flagBuilder("demo")
                    .withDescription(
                        ArgumentDescription.of("Fills the new conversation file with demo data")))
            .flag(main.getCommandManager().categoryFlag)
            .meta(CommandMeta.DESCRIPTION, "Creates a new conversation file.")
            .handler(
                (context) -> {
                  String conversationName = context.get("Conversation Name");
                  final boolean demo = context.flags().isPresent("demo");

                  conversationName = conversationName.replaceAll("[^0-9a-zA-Z-._]", "_");

                  final Conversation existingConversation =
                      main.getConversationManager().getConversation(conversationName);

                  Category category = main.getDataManager().getDefaultCategory();
                  if (context.flags().contains(main.getCommandManager().categoryFlag)) {
                    category =
                        context
                            .flags()
                            .getValue(
                                main.getCommandManager().categoryFlag,
                                main.getDataManager().getDefaultCategory());
                  }

                  if (category == null) {
                    context
                        .getSender()
                        .sendMessage(
                            main.parse("<error>Error: Category for conversation is null."));
                    return;
                  }

                  if (existingConversation == null) {
                    File newConversationFile =
                        new File(
                            main.getConversationManager().getConversationsFolder(category).getPath()
                                + "/"
                                + conversationName
                                + ".yml");

                    try {
                      if (!newConversationFile.exists()) {
                        if (!newConversationFile.createNewFile()) {
                          context
                              .getSender()
                              .sendMessage(
                                  main.parse("<error>Error: couldn't create conversation file."));
                          return;
                        }
                        InputStream inputStream;
                        if (!demo) {
                          inputStream = main.getMain().getResource("conversations/empty.yml");
                        } else {
                          inputStream = main.getMain().getResource("conversations/demo.yml");
                        }

                        // Instead of creating a new language file, we will copy the one from inside
                        // of the plugin jar into the plugin folder:
                        if (inputStream != null) {
                          try (OutputStream outputStream =
                              new FileOutputStream(newConversationFile)) {
                            IOUtils.copy(inputStream, outputStream);
                            main.getConversationManager().loadConversationsFromConfig();
                            context
                                .getSender()
                                .sendMessage(
                                    main.parse(
                                        "<success>The conversation has been created successfully! There are currently no commands to edit them - you have to edit the conversation file. You can find it at <highlight>"
                                            + category
                                                .getConversationsFolder()
                                                .getPath()
                                                .replace("\\", "/")
                                            + "/"
                                            + conversationName
                                            + ".yml"));
                          } catch (Exception e) {
                            context
                                .getSender()
                                .sendMessage(
                                    main.parse(
                                        "<error>Error: couldn't create conversation file. There was an exception. (2)"));
                            return;
                          }
                        }
                      }

                    } catch (Exception e) {
                      context
                          .getSender()
                          .sendMessage(
                              main.parse(
                                  "<error>Error: couldn't create conversation file. There was an exception."));
                    }

                  } else {
                    context
                        .getSender()
                        .sendMessage(
                            main.parse(
                                "<error>Error: the conversation <highlight>"
                                    + existingConversation.getIdentifier()
                                    + "</highlight> already exists!"));
                  }
                }));

    if (main.getConfiguration().debug) {
      manager.command(
          conversationBuilder
              .literal("test")
              .senderType(Player.class)
              .meta(CommandMeta.DESCRIPTION, "Starts a test conversation.")
              .handler(
                  (context) -> {
                    final Player player = (Player) context.getSender();

                    context
                        .getSender()
                        .sendMessage(main.parse("<main>Playing test conversation..."));
                    conversationManager.playConversation(
                        main.getQuestPlayerManager().getOrCreateQuestPlayer(player.getUniqueId()),
                        conversationManager.createTestConversation(), null);
                  }));
    }

    manager.command(
        conversationBuilder
            .literal("list")
            .meta(CommandMeta.DESCRIPTION, "Lists all conversations.")
            .handler(
                (context) -> {
                  context.getSender().sendMessage(main.parse("<highlight>All conversations:"));
                  int counter = 1;
                  for (final Conversation conversation :
                      conversationManager.getAllConversations()) {
                    context
                        .getSender()
                        .sendMessage(
                            main.parse(
                                "<highlight>"
                                    + counter
                                    + ".</highlight> <main>"
                                    + conversation.getIdentifier()));

                    context
                        .getSender()
                        .sendMessage(
                            main.parse(
                                "<unimportant>--- Attached to NPC:</unimportant> <main>"
                                    + conversation.getNPCs().toString())); //TODO: Fix this

                    context
                        .getSender()
                        .sendMessage(
                            main.parse(
                                "<unimportant>--- Amount of starting conversation lines:</unimportant> <main>"
                                    + conversation.getStartingLines().size()));
                  }
                }));

    manager.command(
        conversationBuilder
            .literal("analyze")
            .argument(
                ConversationSelector.of("conversation", main),
                ArgumentDescription.of("Name of the Conversation."))
            .flag(
                manager
                    .flagBuilder("printToConsole")
                    .withDescription(ArgumentDescription.of("Prints the output to the console")))
            .meta(CommandMeta.DESCRIPTION, "Analyze specific conversations.")
            .handler(
                (context) -> {
                  final Conversation foundConversation = context.get("conversation");

                  final boolean printToConsole = context.flags().contains("printToConsole");

                  for (final ConversationLine conversationLine :
                      foundConversation.getStartingLines()) {
                    final String analyzed =
                        main.getConversationManager().analyze(conversationLine, "  ");

                    if (printToConsole) {
                      main.getLogManager().info("\n" + analyzed);
                      context
                          .getSender()
                          .sendMessage(
                              main.parse(
                                  "<success>Analyze Output has been printed to your console!"));
                    } else {
                      context.getSender().sendMessage(main.parse(analyzed));
                    }
                  }
                }));

    manager.command(
        conversationBuilder
            .literal("start")
            .argument(
                ConversationSelector.of("conversation", main),
                ArgumentDescription.of("Name of the Conversation."))
            .senderType(Player.class)
            .meta(CommandMeta.DESCRIPTION, "Starts a conversation.")
            .handler(
                (context) -> {
                  final Player player = (Player) context.getSender();

                  final Conversation foundConversation = context.get("conversation");

                  context
                      .getSender()
                      .sendMessage(
                          main.parse(
                              "<main>Playing <highlight>"
                                  + foundConversation.getIdentifier()
                                  + "</highlight> conversation..."));
                  conversationManager.playConversation(
                      main.getQuestPlayerManager().getOrCreateQuestPlayer(player.getUniqueId()),
                      foundConversation, null);
                }));

    final Command.Builder<CommandSender> conversationEditBuilder =
        conversationBuilder
            .literal("edit")
            .argument(
                ConversationSelector.of("conversation", main),
                ArgumentDescription.of("Name of the Conversation."));

    manager.command(
        conversationEditBuilder
            .literal("npcs")
            .literal("add")
            .argument(NQNPCSelector.of("NPC", main, false, true), ArgumentDescription.of("ID of the NPC which should start the conversation"))
            .meta(CommandMeta.DESCRIPTION, "Add conversation to NPC")
            .handler(
                (context) -> {
                  final Conversation foundConversation = context.get("conversation");
                  final NQNPCResult nqNPCResult = context.get("NPC");

                  if (nqNPCResult.isRightClickSelect()) {//Armor Stands
                    if (context.getSender() instanceof final Player player) {
                      main.getNPCManager().handleRightClickNQNPCSelectionWithAction(
                          (nqnpc) -> {
                            foundConversation.addNPC( nqnpc );
                            context
                                .getSender()
                                .sendMessage(
                                    main.parse(
                                        "<main>NPCs of conversation <highlight>"
                                            + foundConversation.getIdentifier()
                                            + "</highlight> has been added by <highlight2>"
                                            + nqnpc.getID().toString()
                                            + "</highlight2>!"));
                          },
                          player,
                          "<success>You have been given an item with which you can add the conversation <highlight>"
                              + foundConversation.getIdentifier()
                              + "</highlight> to an armor stand. Check your inventory!",
                          "<LIGHT_PURPLE>Add conversation <highlight>"
                              + foundConversation.getIdentifier()
                              + "</highlight> to this Armor Stand",
                          "<WHITE>Right-click an Armor Stand to add the conversation <highlight>"
                              + foundConversation.getIdentifier()
                              + "</highlight> to it."
                      );

                    } else {
                      context.getSender().sendMessage(main.parse("<error>Error: this command can only be run as a player."));
                    }
                  }else {
                    final NQNPC nqNPC = nqNPCResult.getNQNPC();
                    foundConversation.addNPC( nqNPC );

                    context
                        .getSender()
                        .sendMessage(
                            main.parse(
                                "<main>NPCs of conversation <highlight>"
                                    + foundConversation.getIdentifier()
                                    + "</highlight> has been added by <highlight2>"
                                    + nqNPC.getID().toString()
                                    + "</highlight2>!"));
                  }


                }));

    manager.command( //TODO: Generalize with an npc remove command
        conversationEditBuilder
            .literal("armorstand")
            .literal("remove", "delete")
            .senderType(Player.class)
            .meta(
                CommandMeta.DESCRIPTION,
                "Gives you an item to remove all conversations from an armorstand")
            .handler(
                (context) -> {
                  final Player player = (Player) context.getSender();

                  ItemStack itemStack = new ItemStack(Material.PAPER, 1);
                  // give a specialitem. clicking an armorstand with that special item will remove
                  // the pdb.

                  NamespacedKey key = new NamespacedKey(main.getMain(), "notquests-item");
                  NamespacedKey conversationIdentifierKey =
                      new NamespacedKey(main.getMain(), "notquests-conversation");

                  ItemMeta itemMeta = itemStack.getItemMeta();
                  List<Component> lore = new ArrayList<>();

                  assert itemMeta != null;

                  itemMeta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, 9);

                  itemMeta.displayName(
                      main.parse("<LIGHT_PURPLE>Remove all conversations from this Armor Stand"));
                  lore.add(
                      main.parse(
                          "<WHITE>Right-click an Armor Stand to remove all conversations attached to it."));

                  itemMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                  itemMeta.lore(lore);

                  itemStack.setItemMeta(itemMeta);

                  player.getInventory().addItem(itemStack);

                  context
                      .getSender()
                      .sendMessage(
                          main.parse(
                              "<success>You have been given an item with which you remove all conversations from an armor stand. Check your inventory!"));
                }));

    manager.command(
        conversationEditBuilder
            .literal("speakers")
            .literal("add", "create")
            .argument(
                StringArgument.<CommandSender>newBuilder("Speaker Name")
                    .withSuggestionsProvider(
                        (context, lastString) -> {
                          final List<String> allArgs = context.getRawInput();
                          main.getUtilManager()
                              .sendFancyCommandCompletion(
                                  context.getSender(),
                                  allArgs.toArray(new String[0]),
                                  "[New Speaker Name]",
                                  "");

                          ArrayList<String> completions = new ArrayList<>();

                          completions.add("<Enter new Speaker Name>");
                          return completions;
                        })
                    .single()
                    .build(),
                ArgumentDescription.of("Speaker Name"))
            .flag(main.getCommandManager().speakerColor)
            .meta(CommandMeta.DESCRIPTION, "Adds / creates a new speaker for the conversation.")
            .handler(
                (context) -> {
                  final Conversation foundConversation = context.get("conversation");

                  final String speakerName = context.get("Speaker Name");
                  final String speakerColor =
                      context.flags().getValue(main.getCommandManager().speakerColor, "");

                  Speaker speaker = new Speaker(speakerName, foundConversation);
                  if (speakerColor != null && !speakerColor.isBlank()) {
                    speaker.setColor(speakerColor);
                  }

                  if (foundConversation.addSpeaker(speaker, true)) {
                    context
                        .getSender()
                        .sendMessage(
                            main.parse(
                                "<success>Speaker <highlight>"
                                    + speaker.getSpeakerName()
                                    + "</highlight> was successfully added to conversation <highlight2>"
                                    + foundConversation.getIdentifier()
                                    + "</highlight2>!"));
                  } else {
                    context
                        .getSender()
                        .sendMessage(
                            main.parse(
                                "<error>Speaker <highlight>"
                                    + speaker.getSpeakerName()
                                    + "</highlight> could not be added to <highlight2>"
                                    + foundConversation.getIdentifier()
                                    + "</highlight2>! Does it already exist?"));
                  }
                }));

    manager.command(
        conversationEditBuilder
            .literal("speakers")
            .literal("list", "show")
            .meta(CommandMeta.DESCRIPTION, "Adds / creates a new speaker for the conversation.")
            .handler(
                (context) -> {
                  final Conversation foundConversation = context.get("conversation");

                  if (foundConversation.getSpeakers().size() == 0) {
                    context
                        .getSender()
                        .sendMessage(main.parse("<success>This conversation has no speakers."));
                  } else {
                    context
                        .getSender()
                        .sendMessage(
                            main.parse(
                                "<highlight>Speakers of conversation <highlight2>"
                                    + foundConversation.getIdentifier()
                                    + "</highlight2>:"));
                    int counter = 0;
                    for (final Speaker speaker : foundConversation.getSpeakers()) {
                      counter++;

                      context
                          .getSender()
                          .sendMessage(
                              main.parse(
                                  "<highlight>"
                                      + counter
                                      + ".</highlight> <main>Name:</main> <highlight2>"
                                      + speaker.getSpeakerName()
                                      + "</highlight2> Color: <highlight2>"
                                      + speaker.getColor()
                                      + speaker.getColor().replace("<", "").replace(">", "")
                                      + "</highlight2>"));
                    }
                  }
                }));

    manager.command(
        conversationEditBuilder
            .literal("speakers")
            .literal("remove", "delete")
            .argument(SpeakerSelector.of("Speaker", main, "conversation"))
            .meta(CommandMeta.DESCRIPTION, "Adds / creates a new speaker for the conversation.")
            .handler(
                (context) -> {
                  final Conversation foundConversation = context.get("conversation");

                  final Speaker speaker = context.get("Speaker");

                  if (foundConversation.hasSpeaker(speaker)
                      && foundConversation.removeSpeaker(speaker, true)) {
                    // TODO: Reload conversation here
                    context
                        .getSender()
                        .sendMessage(
                            main.parse(
                                "<success>Speaker <highlight>"
                                    + speaker.getSpeakerName()
                                    + "</highlight> was successfully removed from conversation <highlight2>"
                                    + foundConversation.getIdentifier()
                                    + "</highlight2>!"));
                  } else {
                    context
                        .getSender()
                        .sendMessage(
                            main.parse(
                                "<error>Speaker <highlight>"
                                    + speaker.getSpeakerName()
                                    + "</highlight> could not be removed from <highlight2>"
                                    + foundConversation.getIdentifier()
                                    + "</highlight2>! Does it exist?"));
                  }
                }));

    manager.command(
        conversationEditBuilder
            .literal("category")
            .literal("show")
            .meta(CommandMeta.DESCRIPTION, "Shows the current category of this Conversation..")
            .handler(
                (context) -> {
                  final Conversation conversation = context.get("conversation");

                  context
                      .getSender()
                      .sendMessage(
                          main.parse(
                              "<main>Category for coversation <highlight>"
                                  + conversation.getIdentifier()
                                  + "</highlight>: <highlight2>"
                                  + conversation.getCategory().getCategoryFullName()
                                  + "</highlight2>."));
                }));

    manager.command(
        conversationEditBuilder
            .literal("category")
            .literal("set")
            .argument(
                CategorySelector.of("category", main),
                ArgumentDescription.of("New category for this Conversation."))
            .meta(CommandMeta.DESCRIPTION, "Changes the current category of this Conversation.")
            .handler(
                (context) -> {
                  final Conversation conversation = context.get("conversation");
                  final Category category = context.get("category");
                  if (conversation
                      .getCategory()
                      .getCategoryFullName()
                      .equalsIgnoreCase(category.getCategoryFullName())) {
                    context
                        .getSender()
                        .sendMessage(
                            main.parse(
                                "<error> Error: The conversation <highlight>"
                                    + conversation.getIdentifier()
                                    + "</highlight> already has the category <highlight2>"
                                    + conversation.getCategory().getCategoryFullName()
                                    + "</highlight2>."));
                    return;
                  }

                  context
                      .getSender()
                      .sendMessage(
                          main.parse(
                              "<success>Category for conversation <highlight>"
                                  + conversation.getIdentifier()
                                  + "</highlight> has successfully been changed from <highlight2>"
                                  + conversation.getCategory().getCategoryFullName()
                                  + "</highlight2> to <highlight2>"
                                  + category.getCategoryFullName()
                                  + "</highlight2>!"));

                  conversation.switchCategory(category);
                }));

    handleLinesCommands();
  }

  public void handleLinesCommands() {
    /*manager.command(conversationBuilder.literal("edit")
    .argument(ConversationSelector.of("conversation", main), ArgumentDescription.of("Name of the Conversation."))
    .literal("lines")
    .literal("add", "create")
    .meta(CommandMeta.DESCRIPTION, "Creates a line for a conversation.")
    .handler((context) -> {
        final Audience audience = main.adventure().sender(context.getSender());

        final Conversation foundConversation = context.get("conversation");

        audience.sendMessage(main.parse(
                highlightGradient + "Starting lines (max. 3 levels of next):"
        ));
        final int npcID = context.get("NPC");

        foundConversation.setNPC(npcID);

        audience.sendMessage(main.parse(
                mainGradient + "NPC of conversation " + highlightGradient + foundConversation.getIdentifier() + "</gradient> has been set to "
                        + highlight2Gradient + npcID + "</gradient>!"
        ));


    }));*/
  }
}
