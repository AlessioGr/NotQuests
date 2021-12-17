/*
 * NotQuests - A Questing plugin for Minecraft Servers
 * Copyright (C) 2021 Alessio Gravili
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

package rocks.gravili.notquests.Conversation;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import rocks.gravili.notquests.Commands.NotQuestColors;
import rocks.gravili.notquests.NotQuests;
import rocks.gravili.notquests.Structs.Actions.Action;
import rocks.gravili.notquests.Structs.Conditions.Condition;
import rocks.gravili.notquests.Structs.QuestPlayer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static rocks.gravili.notquests.Commands.NotQuestColors.highlight2Gradient;
import static rocks.gravili.notquests.Commands.NotQuestColors.highlightGradient;

public class ConversationManager {
    private final NotQuests main;
    private final ArrayList<Conversation> conversations;
    private final Speaker playerSpeaker;

    private final HashMap<UUID, ConversationPlayer> openConversations;

    final ArrayList<ConversationLine> linesForOneFile = new ArrayList<>();

    private File conversationsFolder;

    HashMap<UUID, ArrayList<Component>> chatHistory;
    HashMap<UUID, ArrayList<Component>> conversationChatHistory;

    public ConversationManager(final NotQuests main) {
        this.main = main;
        conversations = new ArrayList<>();

        openConversations = new HashMap<>();

        playerSpeaker = new Speaker("You");
        playerSpeaker.setPlayer(true);

        chatHistory = new HashMap<>();
        conversationChatHistory = new HashMap<>();


        //playConversation(Bukkit.getPlayer("NoeX"), createTestConversation());
        loadConversationsFromConfig();

    }

    public final int getMaxChatHistory() {
        return main.getDataManager().getConfiguration().previousConversationsHistorySize;
    }

    public File getConversationsFolder() {
        return conversationsFolder;
    }

    public Conversation getConversationForNPCID(final int npcID) {
        for (final Conversation conversation : conversations) {
            if (conversation.getNPCID() == npcID) {
                return conversation;
            }
        }
        return null;
    }

    public ConversationPlayer getOpenConversation(final UUID uuid) {
        return openConversations.get(uuid);
    }

    public final HashMap<UUID, ConversationPlayer> getOpenConversations() {
        return openConversations;
    }

    public Conversation createTestConversation() {
        final Conversation testConversation = new Conversation(main, null, null, "test", 0);

        final Speaker gustav = new Speaker("Gustav");

        testConversation.addSpeaker(gustav, false);

        testConversation.addSpeaker(playerSpeaker, false);


        final ConversationLine gustav1 = new ConversationLine(gustav, "gustav1", "Hello, I'm Gustav! What's your name?");

        final ConversationLine player1 = new ConversationLine(playerSpeaker, "player1", "I'm player!");
        final ConversationLine player2 = new ConversationLine(playerSpeaker, "player2", "None of your business!");

        gustav1.addNext(player1);
        gustav1.addNext(player2);

        final ConversationLine gustav2 = new ConversationLine(gustav, "gustav2", "Nice to meet you!");
        final ConversationLine gustav3 = new ConversationLine(gustav, "gustav3", "Yeah, fuck you!");

        final ConversationLine player3 = new ConversationLine(playerSpeaker, "player2", "That was mean...");
        gustav3.addNext(player3);

        final ConversationLine gustav4 = new ConversationLine(gustav, "gustav3", "You are mean too!");
        player3.addNext(gustav4);

        final ConversationLine gustav5 = new ConversationLine(gustav, "gustav3", "I don't like you!");

        gustav4.addNext(gustav5);

        player1.addNext(gustav2);

        player2.addNext(gustav3);

        testConversation.addStarterConversationLine(gustav1);

        return testConversation;
    }


    public void playConversation(final Player player, final Conversation conversation) {
        final QuestPlayer questPlayer = main.getQuestPlayerManager().getOrCreateQuestPlayer(player.getUniqueId());


        if (getOpenConversation(questPlayer.getUUID()) == null) {
            ConversationPlayer conversationPlayer = new ConversationPlayer(main, questPlayer, player, conversation);
            openConversations.put(questPlayer.getUUID(), conversationPlayer);

            conversationPlayer.play();
        } else {
            main.adventure().player(player).sendMessage(
                    MiniMessage.miniMessage().parse(
                            NotQuestColors.errorGradient + "You are already in a conversation!"
                    )
            );
        }

    }

    public boolean prepareConversationsFolder() {
        //Create the Conversations Folder if it does not exist yet (the NotQuests/conversations folder)
        conversationsFolder = new File(main.getDataFolder().getPath() + "/conversations/");
        if (!conversationsFolder.exists()) {
            main.getLogManager().info("Conversations Folder not found. Creating a new one...");

            if (!conversationsFolder.mkdirs()) {
                main.getDataManager().disablePluginAndSaving("There was an error creating the NotQuests conversations folder.");
                return false;
            }
        }
        return true;
    }


    public void loadConversationsFromConfig() {
        conversations.clear();
        openConversations.clear();

        if (!prepareConversationsFolder()) {
            return;
        }

        for (File conversationFile : main.getUtilManager().listFilesRecursively(conversationsFolder)) {
            linesForOneFile.clear();
            main.getLogManager().info("Reading conversation file <AQUA>" + conversationFile.getName() + "</AQUA>...");

            final YamlConfiguration config = new YamlConfiguration();
            try {
                config.load(conversationFile);
            } catch (IOException | InvalidConfigurationException e) {
                e.printStackTrace();
                main.getLogManager().warn("Failed reading conversation file <AQUA>" + conversationFile.getName() + "</AQUA>. It's being skipped.");
                continue;
            }

            final int npcID = config.getInt("npcID", -1);

            final Conversation conversation = new Conversation(main, conversationFile, config, conversationFile.getName().replace(".yml", ""), npcID);


            //First add all speakers
            final ArrayList<Speaker> allSpeakers = new ArrayList<>();
            final ConfigurationSection speakersAndLinesConfigurationSection = config.getConfigurationSection("Lines");
            if (speakersAndLinesConfigurationSection != null) {
                for (final String speakerName : speakersAndLinesConfigurationSection.getKeys(false)) {
                    final Speaker speaker = new Speaker(speakerName);
                    final String color = speakersAndLinesConfigurationSection.getString(speakerName + ".color", "<WHITE>");
                    if (!color.isBlank()) {
                        speaker.setColor(color);
                    }


                    if (speakerName.equalsIgnoreCase("player")) {
                        speaker.setPlayer(true);
                    }
                    allSpeakers.add(speaker);
                    if (!conversation.hasSpeaker(speaker) && !conversation.addSpeaker(speaker, false)) {
                        main.getLogManager().warn("Speaker " + highlightGradient + speaker.getSpeakerName() + "</gradient> could not be added to conversation " + highlight2Gradient + conversation.getIdentifier() + "</gradient>. Does the speaker already exist?");
                    }


                    /*final ConfigurationSection speakerLines = speakersAndLinesConfigurationSection.getConfigurationSection(speakerName);

                    if (speakerLines == null) {
                        main.getLogManager().warn("No lines found for conversation <AQUA>" + conversationFile.getName() + "</AQUA>.");
                        continue;
                    }

                    for (final String line : speakerLines.getKeys(false)) {
                        if (line.equals("color")) {
                            continue;
                        }
                    }*/

                }

            }

            //This ArrayList will be filled
            final ArrayList<ConversationLine> conversationLines = new ArrayList<>();


            //Prepare all starter conversation lines to feed them into deep diving
            final String starterLines = config.getString("start", "").replace("", " ");
            for (final String starterLine : starterLines.split(",")) {
                final String initialLine = "Lines." + starterLine;
                final String message = config.getString(initialLine + ".text", "-");
                final ArrayList<Action> actions = parseActionString(config.getStringList(initialLine + ".actions"));
                final boolean shouting = config.getBoolean(initialLine + ".shout", false);

                //Message
                if (message.equals("-")) {
                    main.getLogManager().warn("Warning: couldn't find message for starter line <AQUA>" + starterLine + "</AQUA> of conversation <AQUA>" + conversationFile.getName() + "</AQUA>");
                }

                //Speaker
                Speaker foundSpeaker = null;
                for (final Speaker speaker : allSpeakers) {
                    if (speaker.getSpeakerName().equals(starterLine.split("\\.")[0].replaceAll("\\s", ""))) {
                        foundSpeaker = speaker;
                    }
                }
                if (foundSpeaker == null) {
                    main.getLogManager().warn("Warning: couldn't find speaker for a conversation line. Skipping...");
                    continue;
                }

                //Construct the ConversationLine
                ConversationLine startLine = new ConversationLine(foundSpeaker, starterLine.split("\\.")[1], message);
                if (actions != null && actions.size() > 0) {
                    for (Action action : actions) {
                        startLine.addAction(action);
                    }
                }
                startLine.setShouting(shouting);

                //Conditions
                final ArrayList<Condition> conditions = parseConditionsString(config.getStringList(initialLine + ".conditions"));
                if (conditions != null && conditions.size() > 0) {
                    for (Condition condition : conditions) {
                        startLine.addCondition(condition);
                    }
                }

                conversationLines.add(startLine);
                conversation.addStarterConversationLine(startLine);
            }


            //Now here we have all starter conversation lines. We need to dive deep into them!

            linesForOneFile.addAll(conversationLines);
            deepDiveAndConnectStarterLines(conversation, conversationLines, config);


            conversations.add(conversation);

        }
    }


    public void deepDiveAndConnectStarterLines(final Conversation conversation, final ArrayList<ConversationLine> lines, final YamlConfiguration config) {

        for (final ConversationLine conversationLine : lines) {

            final String fullIdentifier = conversationLine.getFullIdentifier();

            main.getLogManager().debug("Deep diving conversation line <AQUA>" + fullIdentifier + "</AQUA>...");


            final String nextString = config.getString("Lines." + fullIdentifier + ".next", "");
            if (!nextString.isBlank()) {
                //Dive deep
                final ArrayList<ConversationLine> keepDiving = new ArrayList<>();

                outerLoop:
                for (final String nextLineFullIdentifier : nextString.split(",")) {
                    final String initialLine = "Lines." + nextLineFullIdentifier;

                    final String message = config.getString(initialLine + ".text", "");
                    final String next = config.getString(initialLine + ".next", "");
                    final ArrayList<Action> actions = parseActionString(config.getStringList(initialLine + ".actions"));
                    final boolean shouting = config.getBoolean(initialLine + ".shout", false);

                    main.getLogManager().debug("Deep diving next string <AQUA>" + nextLineFullIdentifier + "</AQUA> for conversation line <AQUA>" + fullIdentifier + "</AQUA>...");
                    main.getLogManager().debug("---- Message: <AQUA>" + message + "</AQUA> | Next: <AQUA>" + next + "</AQUA>");

                    //Skip if we already added this line
                    for (final ConversationLine existingLine : linesForOneFile) {
                        final String fullExistingLineIdentifier = existingLine.getFullIdentifier();
                        if (nextLineFullIdentifier.equalsIgnoreCase(fullExistingLineIdentifier)) {
                            conversationLine.addNext(existingLine);
                            continue outerLoop; //Skip this line
                        }
                    }


                    final String nextLineSpeakerName = nextLineFullIdentifier.split("\\.")[0].replaceAll("\\s", "");
                    main.getLogManager().debug("Trying to find speaker: <AQUA>" + nextLineSpeakerName + "</AQUA>...");

                    Speaker foundSpeaker = null;
                    for (final Speaker speaker : conversation.getSpeakers()) {
                        if (speaker.getSpeakerName().equals(nextLineSpeakerName)) {
                            foundSpeaker = speaker;
                        }
                    }
                    if (foundSpeaker == null) {
                        main.getLogManager().warn("Warning: couldn't find speaker for next conversation line <AQUA>" + nextLineFullIdentifier + "</AQUA>. Skipping line...");
                        continue;
                    }

                    main.getLogManager().debug("---- Speaker: <AQUA>" + foundSpeaker.getSpeakerName() + "</AQUA> | Is Player?: <AQUA>" + foundSpeaker.isPlayer() + "</AQUA>");

                    if (!conversation.hasSpeaker(foundSpeaker) && !conversation.addSpeaker(foundSpeaker, false)) {
                        main.getLogManager().warn("Speaker " + highlightGradient + foundSpeaker.getSpeakerName() + "</gradient> could not be added to conversation " + highlight2Gradient + conversation.getIdentifier() + "</gradient>. Does the speaker already exist?");
                    }

                    ConversationLine newLine = new ConversationLine(foundSpeaker, nextLineFullIdentifier.split("\\.")[1], message);
                    if (actions != null && actions.size() > 0) {
                        for (Action action : actions) {
                            newLine.addAction(action);
                        }
                    }

                    newLine.setShouting(shouting);

                    //Conditions
                    final ArrayList<Condition> conditions = parseConditionsString(config.getStringList(initialLine + ".conditions"));
                    if (conditions != null && conditions.size() > 0) {
                        for (Condition condition : conditions) {
                            newLine.addCondition(condition);
                        }
                    }


                    conversationLine.addNext(newLine);
                    linesForOneFile.add(newLine);

                    if (!next.isBlank()) {
                        keepDiving.add(newLine);
                    }

                }


                //Dive again to next level
                deepDiveAndConnectStarterLines(conversation, keepDiving, config);

            }

        }
    }

    public final HashMap<UUID, ArrayList<Component>> getChatHistory() {
        return chatHistory;
    }

    public final HashMap<UUID, ArrayList<Component>> getConversationChatHistory() {
        return conversationChatHistory;
    }


    public final ArrayList<Conversation> getAllConversations() {
        return conversations;
    }

    public void stopConversation(final ConversationPlayer conversationPlayer) {
        conversationPlayer.getQuestPlayer().sendDebugMessage("Stopping conversation...");
        openConversations.remove(conversationPlayer.getQuestPlayer().getUUID());

        //Send back old messages
        /*ArrayList<Component> allChatHistory = main.getPacketManager().getChatHistory().get(conversationPlayer.getQuestPlayer().getUUID());
        ArrayList<Component> allConversationHistory = main.getPacketManager().getConversationChatHistory().get(conversationPlayer.getQuestPlayer().getUUID());

        main.getLogManager().log(Level.INFO, "Conversation stop stage 1");

        if(allChatHistory == null ){
            return;
        }
        main.getLogManager().log(Level.INFO, "Conversation stop stage 1.5");
        if(allConversationHistory == null){
            return;
        }
        main.getLogManager().log(Level.INFO, "Conversation stop stage 2");

        final Audience audience = main.adventure().player(conversationPlayer.getQuestPlayer().getPlayer());

        for( int i = 0; i < allChatHistory.size(); i++ )
        {
            main.getLogManager().log(Level.INFO, "Conversation stop stage 3");
            Component component = allChatHistory.get(i);
            if(component != null && !allConversationHistory.contains(component)){
                // audience.sendMessage(component.append(Component.text("fg9023zf729ofz")));
                main.getLogManager().log(Level.INFO, "Conversation stop stage 4");
                audience.sendMessage(component);
            }


        }*/
    }

    public final ArrayList<Condition> parseConditionsString(final List<String> allConditionsString) {
        if (allConditionsString != null && !allConditionsString.isEmpty()) {
            ArrayList<Condition> conditions = new ArrayList<>();
            for (String conditionString : allConditionsString) {
                main.getLogManager().debug("<GREEN>Trying to find condition in: " + conditionString);

                final Condition foundCondition = main.getConditionsManager().getConditionFromString(conditionString);

                if (foundCondition != null) {
                    main.getLogManager().debug("Found conversation line condition: " + foundCondition.getConditionType());
                }

                return conditions;

            }
            return null;
        }
        return null;
    }

    public final ArrayList<Action> parseActionString(final List<String> allActionsString) {
        if (allActionsString != null && !allActionsString.isEmpty()) {
            ArrayList<Action> actions = new ArrayList<>();
            for (String actionString : allActionsString) {
                main.getLogManager().debug("<GREEN>Trying to find action in: " + actionString);
                if (actionString.startsWith("action ")) {

                    final Action foundAction = main.getActionsManager().getAction(actionString);
                    if (foundAction != null) {
                        main.getLogManager().debug("Found conversation line action: " + foundAction.getActionName());
                    }

                    return actions;
                } else {
                    main.getLogManager().warn("Inline-defining actions is not possible in this version yet.");
                }
            }
            return null;
        }
        return null;
    }


    public final Conversation getConversation(final String identifier) {
        for (final Conversation conversation : conversations) {
            if (conversation.getIdentifier().equals(identifier)) {
                return conversation;
            }
        }
        return null;

    }

    public final Conversation getConversationAttachedToArmorstand(final ArmorStand armorstand) {
        PersistentDataContainer armorstandPDB = armorstand.getPersistentDataContainer();
        NamespacedKey attachedQuestsKey = main.getArmorStandManager().getAttachedConversationKey();

        if (armorstandPDB.has(attachedQuestsKey, PersistentDataType.STRING)) {
            String attachedConversation = armorstandPDB.get(attachedQuestsKey, PersistentDataType.STRING);
            return getConversation(attachedConversation);
        }
        return null;

    }
}
