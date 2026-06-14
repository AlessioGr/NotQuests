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
import rocks.gravili.notquests.paper.commands.arguments.wrappers.NQNPCResult;
import rocks.gravili.notquests.paper.commands.framework.NQArguments;
import rocks.gravili.notquests.paper.commands.framework.NQCommandBuilder;
import rocks.gravili.notquests.paper.commands.framework.NQCommandManager;
import rocks.gravili.notquests.paper.commands.framework.NQDescription;
import rocks.gravili.notquests.paper.commands.framework.NQFlag;
import rocks.gravili.notquests.paper.conversation.Conversation;
import rocks.gravili.notquests.paper.conversation.ConversationLine;
import rocks.gravili.notquests.paper.conversation.ConversationManager;
import rocks.gravili.notquests.paper.conversation.Speaker;
import rocks.gravili.notquests.paper.managers.data.Category;
import rocks.gravili.notquests.paper.managers.npc.NQNPC;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import static rocks.gravili.notquests.paper.commands.arguments.CategoryArgument.categoryArgument;
import static rocks.gravili.notquests.paper.commands.arguments.ConversationArgument.conversationArgument;
import static rocks.gravili.notquests.paper.commands.arguments.NQNPCArgument.nqNPCArgument;
import static rocks.gravili.notquests.paper.commands.arguments.SpeakerArgument.speakerArgument;

public class AdminConversationCommands {
    private final NotQuests main;
    private final NQCommandManager manager;
    private final NQCommandBuilder conversationBuilder;

    private final ConversationManager conversationManager;

    public AdminConversationCommands(final NotQuests main,
                                     NQCommandManager manager,
                                     NQCommandBuilder conversationBuilder,
                                     final ConversationManager conversationManager) {
        this.main = main;
        this.manager = manager;
        this.conversationBuilder = conversationBuilder;

        this.conversationManager = conversationManager;

        manager.command(conversationBuilder
                .literal("create")
                .required("conversation-name", NQArguments.stringArgument(), NQDescription.of("conversation-name"), (context, input) -> {
                    List<String> completions = new ArrayList<>();
                    completions.add("<Enter new conversation-name>");
                    return completions;
                })
                .flag(NQFlag.presence("demo", NQDescription.of("Fills the new conversation file with demo data")))
                .flag(main.getCommandManager().categoryFlag).commandDescription(NQDescription.of("Creates a new conversation file."))
                .handler((context) -> {
                    String conversationName = context.get("conversation-name");
                    final boolean demo = context.flags().isPresent("demo");

                    conversationName = conversationName.replaceAll("[^0-9a-zA-Z-._]", "_");

                    final Conversation existingConversation = main.getConversationManager().getConversation(conversationName);

                    Category category = main.getDataManager().getDefaultCategory();
                    if (context.flags().isPresent(main.getCommandManager().categoryFlag)) {
                        category = context.flags().getValue(
                                main.getCommandManager().categoryFlag,
                                main.getDataManager().getDefaultCategory()
                        );
                    }

                    if (category == null) {
                        context.sender().sendMessage(main.parse("<error>Error: Category for conversation is null."));
                        return;
                    }

                    if (existingConversation == null) {
                        File newConversationFile =
                                new File(main.getConversationManager().getConversationsFolder(category).getPath() + "/" + conversationName + ".yml");
                        try {
                            if (!newConversationFile.exists()) {
                                if (!newConversationFile.createNewFile()) {
                                    context.sender().sendMessage(main.parse("<error>Error: couldn't create conversation file."));
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
                                    try (OutputStream outputStream = new FileOutputStream(newConversationFile)) {
                                        IOUtils.copy(inputStream, outputStream);
                                        main.getConversationManager().loadConversationsFromConfig();
                                        context.sender().sendMessage(main.parse("<success>The conversation has been created successfully! " +
                                                "There are currently no commands to edit them - you have to edit " +
                                                "the conversation file. You can find it at <highlight>"
                                                + category
                                                .getConversationsFolder().getPath().replace("\\", "/")
                                                + "/"
                                                + conversationName
                                                + ".yml"));
                                    } catch (Exception e) {
                                        context.sender().sendMessage(main.parse(
                                                "<error>Error: couldn't create conversation file. There was an exception. (2)")
                                        );
                                    }
                                }
                            }

                        } catch (Exception e) {
                            context.sender().sendMessage(main.parse("<error>Error: couldn't create conversation file. There was an exception.")
                            );
                        }

                    } else {
                        context.sender().sendMessage(main.parse("<error>Error: the conversation <highlight>"
                                + existingConversation.getIdentifier()
                                + "</highlight> already exists!")
                        );
                    }
                }));

        if (main.getConfiguration().debug) {
            manager.command(conversationBuilder
                    .literal("test")
                    .senderType(Player.class).commandDescription(NQDescription.of("Starts a test conversation."))
                    .handler((context) -> {
                        final Player player = (Player) context.sender();
                        context.sender().sendMessage(main.parse("<main>Playing test conversation..."));
                        conversationManager.playConversation(main.getQuestPlayerManager().getOrCreateQuestPlayer(player.getUniqueId()), conversationManager.createTestConversation(), null);
                    }));
        }

        manager.command(conversationBuilder
                .literal("list").commandDescription(NQDescription.of("Lists all conversations."))
                .handler((context) -> {
                    context.sender().sendMessage(main.parse("<highlight>All conversations:"));
                    int counter = 1;
                    for (final Conversation conversation : conversationManager.getAllConversations()) {
                        context.sender().sendMessage(main.parse("<highlight>" + counter + ".</highlight> <main>" + conversation.getIdentifier()));
                        context.sender().sendMessage(main.parse("<unimportant>--- Attached to NPC:</unimportant> <main>" + formatAttachedNPCs(conversation.getNPCs())));
                        context.sender().sendMessage(main.parse("<unimportant>--- Amount of starting conversation lines:</unimportant> <main>" + conversation.getStartingLines().size()));
                    }
                }));

        manager.command(conversationBuilder
                .literal("analyze")
                .required("conversation", conversationArgument(main), NQDescription.of("Name of the Conversation."))
                .flag(NQFlag.presence("printToConsole", NQDescription.of("Prints the output to the console"))).commandDescription(NQDescription.of("Analyze specific conversations."))
                .handler((context) -> {
                    final Conversation foundConversation = context.get("conversation");
                    final boolean printToConsole = context.flags().isPresent("printToConsole");
                    for (final ConversationLine conversationLine : foundConversation.getStartingLines()) {
                        final String analyzed = main.getConversationManager().analyze(conversationLine, "  ");

                        if (printToConsole) {
                            main.getLogManager().info("\n" + analyzed);
                            context.sender().sendMessage(main.parse("<success>Analyze Output has been printed to your console!"));
                        } else {
                            context.sender().sendMessage(main.parse(analyzed));
                        }
                    }
                }));

        manager.command(conversationBuilder
                .literal("start")
                .required("conversation", conversationArgument(main), NQDescription.of("Name of the Conversation."))
                .senderType(Player.class).commandDescription(NQDescription.of("Starts a conversation."))
                .handler((context) -> {
                    final Player player = (Player) context.sender();
                    final Conversation foundConversation = context.get("conversation");

                    context.sender().sendMessage(main.parse("<main>Playing <highlight>"
                            + foundConversation.getIdentifier()
                            + "</highlight> conversation..."));
                    conversationManager.playConversation(main.getQuestPlayerManager().getOrCreateQuestPlayer(player.getUniqueId()),
                            foundConversation, null);
                }));

        final NQCommandBuilder conversationEditBuilder = conversationBuilder.literal("edit")
                .required("conversation", conversationArgument(main), NQDescription.of("Name of the Conversation."));

        manager.command(conversationEditBuilder
                .literal("npcs")
                .literal("add")
                .required("NPC", nqNPCArgument(main, false, true), NQDescription.of("ID of the NPC which should start the conversation")).commandDescription(NQDescription.of("Add conversation to NPC"))
                .handler((context) -> {
                    final Conversation foundConversation = context.get("conversation");
                    final NQNPCResult nqNPCResult = context.get("NPC");

                    if (nqNPCResult.isRightClickSelect()) {//Armor Stands
                        if (context.sender() instanceof final Player player) {
                            main.getNPCManager().handleRightClickNQNPCSelectionWithAction(
                                    (nqnpc) -> {
                                        foundConversation.addNPC(nqnpc);
                                        context.sender().sendMessage(main.parse("<main>NPCs of conversation <highlight>"
                                                + foundConversation.getIdentifier()
                                                + "</highlight> has been added by <highlight2>"
                                                + nqnpc.getNPCType() + ":" + nqnpc.getID().getEitherAsString()
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
                            context.sender().sendMessage(main.parse("<error>Error: this command can only be run as a player."));
                        }
                    } else {
                        final NQNPC nqNPC = nqNPCResult.getNQNPC();
                        foundConversation.addNPC(nqNPC);

                        context.sender().sendMessage(main.parse("<main>NPCs of conversation <highlight>"
                                + foundConversation.getIdentifier()
                                + "</highlight> has been added by <highlight2>"
                                + nqNPC.getNPCType() + ":" + nqNPC.getID().getEitherAsString()
                                + "</highlight2>!"));
                    }
                }));

        //TODO: Generalize with an npc remove command
        manager.command(conversationEditBuilder
                .literal("armorstand")
                .literal("remove", "delete")
                .senderType(Player.class).commandDescription(NQDescription.of("Gives you an item to remove all conversations from an armorstand"))
                .handler((context) -> {
                    final Player player = (Player) context.sender();

                    ItemStack itemStack = new ItemStack(Material.PAPER, 1);
                    // give a specialitem. clicking an armorstand with that special item will remove
                    // the pdb.

                    NamespacedKey key = new NamespacedKey(main.getMain(), "notquests-item");
                    NamespacedKey conversationIdentifierKey = new NamespacedKey(main.getMain(), "notquests-conversation");

                    ItemMeta itemMeta = itemStack.getItemMeta();
                    List<Component> lore = new ArrayList<>();

                    assert itemMeta != null;

                    itemMeta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, 9);

                    itemMeta.displayName(main.parse("<LIGHT_PURPLE>Remove all conversations from this Armor Stand"));
                    lore.add(main.parse("<WHITE>Right-click an Armor Stand to remove all conversations attached to it."));

                    itemMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                    itemMeta.lore(lore);

                    itemStack.setItemMeta(itemMeta);

                    player.getInventory().addItem(itemStack);

                    context.sender().sendMessage(main.parse("<success>You have been given an item with which you remove all " +
                            "conversations from an armor stand. Check your inventory!")
                    );
                }));

        manager.command(conversationEditBuilder
                .literal("speakers")
                .literal("add", "create")
                .required("speaker-name", NQArguments.stringArgument(), NQDescription.of("Speaker Name"), (context, input) -> {
                    List<String> completions = new ArrayList<>();
                    completions.add("<Enter new Speaker Name>");
                    return completions;
                })
                .flag(main.getCommandManager().speakerColor).commandDescription(NQDescription.of("Adds / creates a new speaker for the conversation."))
                .handler((context) -> {
                    final Conversation foundConversation = context.get("conversation");

                    final String speakerName = context.get("speaker-name");
                    final String speakerColor = context.flags().getValue(main.getCommandManager().speakerColor, "");

                    Speaker speaker = new Speaker(speakerName, foundConversation);
                    if (speakerColor != null && !speakerColor.isBlank()) {
                        speaker.setColor(speakerColor);
                    }

                    if (foundConversation.addSpeaker(speaker, true)) {
                        context.sender().sendMessage(main.parse("<success>Speaker <highlight>"
                                + speaker.getSpeakerName()
                                + "</highlight> was successfully added to conversation <highlight2>"
                                + foundConversation.getIdentifier()
                                + "</highlight2>!")
                        );
                    } else {
                        context.sender().sendMessage(main.parse("<error>Speaker <highlight>"
                                + speaker.getSpeakerName()
                                + "</highlight> could not be added to <highlight2>"
                                + foundConversation.getIdentifier()
                                + "</highlight2>! Does it already exist?")
                        );
                    }
                }));

        manager.command(conversationEditBuilder
                .literal("speakers")
                .literal("list", "show").commandDescription(NQDescription.of("Adds / creates a new speaker for the conversation."))
                .handler((context) -> {
                    final Conversation foundConversation = context.get("conversation");

                    if (foundConversation.getSpeakers().isEmpty()) {
                        context.sender().sendMessage(main.parse("<success>This conversation has no speakers."));
                    } else {
                        context.sender().sendMessage(main.parse("<highlight>Speakers of conversation <highlight2>"
                                + foundConversation.getIdentifier()
                                + "</highlight2>:")
                        );
                        int counter = 0;
                        for (final Speaker speaker : foundConversation.getSpeakers()) {
                            counter++;

                            context.sender().sendMessage(main.parse("<highlight>"
                                    + counter
                                    + ".</highlight> <main>Name:</main> <highlight2>"
                                    + speaker.getSpeakerName()
                                    + "</highlight2> Color: <highlight2>"
                                    + speaker.getColor()
                                    + speaker.getColor().replace("<", "").replace(">", "")
                                    + "</highlight2>")
                            );
                        }
                    }
                }));

        manager.command(conversationEditBuilder
                .literal("speakers")
                .literal("remove", "delete")
                .required("speaker", speakerArgument(main, "conversation")).commandDescription(NQDescription.of("Adds / creates a new speaker for the conversation."))
                .handler((context) -> {
                    final Conversation foundConversation = context.get("conversation");

                    final Speaker speaker = rocks.gravili.notquests.paper.commands.arguments.SpeakerArgument.resolveSpeaker(foundConversation, context.get("speaker"));

                    if (foundConversation.hasSpeaker(speaker) && foundConversation.removeSpeaker(speaker, true)) {
                        // TODO: Reload conversation here
                        context.sender().sendMessage(main.parse("<success>Speaker <highlight>"
                                + speaker.getSpeakerName()
                                + "</highlight> was successfully removed from conversation <highlight2>"
                                + foundConversation.getIdentifier()
                                + "</highlight2>!")
                        );
                    } else {
                        context.sender().sendMessage(main.parse("<error>Speaker <highlight>"
                                + speaker.getSpeakerName()
                                + "</highlight> could not be removed from <highlight2>"
                                + foundConversation.getIdentifier()
                                + "</highlight2>! Does it exist?")
                        );
                    }
                }));

        manager.command(conversationEditBuilder
                .literal("category")
                .literal("show").commandDescription(NQDescription.of("Shows the current category of this Conversation.."))
                .handler((context) -> {
                    final Conversation conversation = context.get("conversation");
                    context.sender().sendMessage(main.parse(
                            "<main>Category for coversation <highlight>"
                                    + conversation.getIdentifier()
                                    + "</highlight>: <highlight2>"
                                    + conversation.getCategory().getCategoryFullName()
                                    + "</highlight2>.")
                    );
                }));

        manager.command(conversationEditBuilder
                .literal("category")
                .literal("set")
                .required("category", categoryArgument(main), NQDescription.of("New category for this Conversation.")).commandDescription(NQDescription.of("Changes the current category of this Conversation."))
                .handler((context) -> {
                    final Conversation conversation = context.get("conversation");
                    final Category category = context.get("category");
                    if (conversation.getCategory().getCategoryFullName().equalsIgnoreCase(category.getCategoryFullName())) {
                        context.sender().sendMessage(main.parse("<error> Error: The conversation <highlight>"
                                + conversation.getIdentifier()
                                + "</highlight> already has the category <highlight2>"
                                + conversation.getCategory().getCategoryFullName()
                                + "</highlight2>."));
                        return;
                    }

                    context.sender().sendMessage(main.parse("<success>Category for conversation <highlight>"
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

    static String formatAttachedNPCs(final List<NQNPC> npcs) {
        if (npcs == null || npcs.isEmpty()) {
            return "none";
        }
        final List<String> formatted = new ArrayList<>();
        for (final NQNPC npc : npcs) {
            if (npc == null) {
                continue;
            }
            final String id = npc.getNPCType() + ":" + npc.getID().getEitherAsString();
            final String name = npc.getName();
            formatted.add(name == null || name.isBlank() ? id : id + " (" + name + ")");
        }
        return formatted.isEmpty() ? "none" : String.join(", ", formatted);
    }

    public void handleLinesCommands() {
        /*manager.command(conversationBuilder.literal("edit")
        .argument(ConversationSelector.of("conversation", main), Description.of("Name of the Conversation."))
        .literal("lines")
        .literal("add", "create")
        .meta(CommandMeta.DESCRIPTION, "Creates a line for a conversation.")
        .handler((context) -> {
            final Audience audience = main.adventure().sender(context.sender());

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
