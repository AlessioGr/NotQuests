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

package rocks.gravili.notquests.paper.conversation;

import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.managers.data.Category;
import rocks.gravili.notquests.paper.structs.QuestPlayer;
import rocks.gravili.notquests.paper.structs.actions.*;
import rocks.gravili.notquests.paper.structs.conditions.*;
import rocks.gravili.notquests.paper.structs.variables.Variable;
import rocks.gravili.notquests.paper.structs.variables.VariableDataType;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ConversationManager {
    private final NotQuests main;
    private final ArrayList<Conversation> conversations;
    private final Speaker playerSpeaker;

    private final HashMap<UUID, ConversationPlayer> openConversations;

    final ArrayList<ConversationLine> linesForOneFile = new ArrayList<>();


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
        return main.getConfiguration().previousConversationsHistorySize;
    }

    public File getConversationsFolder(final Category category) {
        return category.getConversationsFolder();
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
        final Conversation testConversation = new Conversation(main, null, null, "test", 0, main.getDataManager().getDefaultCategory());

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

        final ConversationPlayer openConversation = getOpenConversation(questPlayer.getUUID());
        if (openConversation != null) {
            main.sendMessage(player, main.getLanguageManager().getString("chat.conversations.ended-previous-conversation", player, conversation));
            stopConversation(openConversation);
        }

        ConversationPlayer conversationPlayer = new ConversationPlayer(main, questPlayer, player, conversation);
        openConversations.put(questPlayer.getUUID(), conversationPlayer);

        conversationPlayer.play();

    }

    /*public boolean prepareConversationsFolder() {
        //Create the Conversations Folder if it does not exist yet (the NotQuests/conversations folder)
        if (conversationsFolder == null) {
            conversationsFolder = new File(main.getMain().getDataFolder().getPath() + "/conversations/");
        }

        if (!conversationsFolder.exists()) {
            main.getLogManager().info("Conversations Folder not found. Creating a new one...");

            if (!conversationsFolder.mkdirs()) {
                main.getDataManager().disablePluginAndSaving("There was an error creating the NotQuests conversations folder.");
                return false;
            }
        }
        return true;
    }*/

    public void loadConversationsFromConfig() {
        conversations.clear();
        openConversations.clear();
        for (final Category category : main.getDataManager().getCategories()) {
            loadConversationsFromConfig(category);
        }
    }

    public void loadConversationsFromConfig(final Category category) {


        /*if (!prepareConversationsFolder()) {
            return;
        }*/

        for (File conversationFile : main.getUtilManager().listFilesRecursively(category.getConversationsFolder())) {
            linesForOneFile.clear();
            main.getLogManager().info("Reading conversation file <highlight>" + conversationFile.getName() + "</highlight>...");

            final YamlConfiguration config = new YamlConfiguration();
            try {
                config.load(conversationFile);
            } catch (IOException | InvalidConfigurationException e) {
                e.printStackTrace();
                main.getLogManager().warn("Failed reading conversation file <highlight>" + conversationFile.getName() + "</highlight>. It's being skipped.");
                continue;
            }

            final int npcID = config.getInt("npcID", -1);

            final Conversation conversation = new Conversation(main, conversationFile, config, conversationFile.getName().replace(".yml", ""), npcID, category);


            //First add all speakers
            final ConfigurationSection speakersAndLinesConfigurationSection = config.getConfigurationSection("Lines");
            if (speakersAndLinesConfigurationSection == null) {
                main.getLogManager().warn("Conversation file <highlight>" + conversationFile.getName() + "</highlight>. Has no lines. It's being skipped.");
                continue;
            }

            final ArrayList<Speaker> allSpeakers = new ArrayList<>();
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
                    main.getLogManager().warn("Speaker <highlight>" + speaker.getSpeakerName() + "</highlight> could not be added to conversation <highlight2>" + conversation.getIdentifier() + "</highlight2>. Does the speaker already exist?");
                }

            }

            //This ArrayList will be filled
            final ArrayList<ConversationLine> conversationLines = new ArrayList<>();


            //Prepare all starter conversation lines to feed them into deep diving
            final String starterLines = config.getString("start", "").replace(" ", "");
            for (final String starterLine : starterLines.split(",")) {
                final String initialLine = "Lines." + starterLine;
                final String message = config.getString(initialLine + ".text", "Missing Message");
                final ArrayList<Action> actions = parseActionString(config.getStringList(initialLine + ".actions"));
                final boolean shouting = config.getBoolean(initialLine + ".shout", false);

                //Message
                if (message.equals("Missing Message")) {
                    main.getLogManager().warn("Warning: couldn't find message for starter line <highlight>" + starterLine + "</highlight> of conversation <highlight>" + conversationFile.getName() + "</highlight>");
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
                //Actions
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

            main.getLogManager().debug("Deep diving conversation line <highlight>" + fullIdentifier + "</highlight>...");


            final String nextString = config.getString("Lines." + fullIdentifier + ".next", "").replace(" ", "");
            ;
            if (!nextString.isBlank()) {
                //Dive deep
                final ArrayList<ConversationLine> keepDiving = new ArrayList<>();

                outerLoop:
                for (final String nextLineFullIdentifier : nextString.split(",")) {
                    main.getLogManager().debug("Deep diving next string <highlight>" + nextLineFullIdentifier + "</highlight> for conversation line <highlight>" + fullIdentifier + "</highlight>...");


                    final String initialLine = "Lines." + nextLineFullIdentifier;

                    final String message = config.getString(initialLine + ".text", "");
                    final String next = config.getString(initialLine + ".next", "");
                    final ArrayList<Action> actions = parseActionString(config.getStringList(initialLine + ".actions"));
                    final boolean shouting = config.getBoolean(initialLine + ".shout", false);

                    main.getLogManager().debug("---- Message: <highlight>" + message + "</highlight> | Next: <highlight>" + next + "</highlight>");

                    //Skip if we already added this line
                    for (final ConversationLine existingLine : linesForOneFile) {
                        final String fullExistingLineIdentifier = existingLine.getFullIdentifier();
                        if (nextLineFullIdentifier.equalsIgnoreCase(fullExistingLineIdentifier)) {
                            conversationLine.addNext(existingLine);
                            continue outerLoop; //Skip this line
                        }
                    }


                    final String nextLineSpeakerName = nextLineFullIdentifier.split("\\.")[0].replaceAll("\\s", "");
                    main.getLogManager().debug("Trying to find speaker: <highlight>" + nextLineSpeakerName + "</highlight>...");

                    Speaker foundSpeaker = null;
                    for (final Speaker speaker : conversation.getSpeakers()) {
                        if (speaker.getSpeakerName().equals(nextLineSpeakerName)) {
                            foundSpeaker = speaker;
                        }
                    }
                    if (foundSpeaker == null) {
                        main.getLogManager().warn("Warning: couldn't find speaker for next conversation line <highlight>" + nextLineFullIdentifier + "</highlight>. Skipping line...");
                        continue;
                    }

                    main.getLogManager().debug("---- Speaker: <highlight>" + foundSpeaker.getSpeakerName() + "</highlight> | Is Player?: <highlight>" + foundSpeaker.isPlayer() + "</highlight>");

                    if (!conversation.hasSpeaker(foundSpeaker) && !conversation.addSpeaker(foundSpeaker, false)) {
                        main.getLogManager().warn("Speaker <highlight>" + foundSpeaker.getSpeakerName() + "</highlight> could not be added to conversation <highlight2>" + conversation.getIdentifier() + "</highlight2>. Does the speaker already exist?");
                    }

                    ConversationLine newLine = new ConversationLine(foundSpeaker, nextLineFullIdentifier.split("\\.")[1], message);
                    //Actions
                    if (actions != null && actions.size() > 0) {
                        for (Action action : actions) {
                            main.getLogManager().debug("Found an action!");
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

            conditionLineLoop:
            for (String conditionString : allConditionsString) {
                main.getLogManager().debug("<GREEN>Trying to find condition in: " + conditionString);
                boolean negated = false;
                if (conditionString.startsWith("!")) {
                    negated = true;
                    conditionString = conditionString.replaceFirst("!", "");
                }
                if (conditionString.toLowerCase(Locale.ROOT).startsWith("condition ")) {
                    final String strippedCondition = conditionString.replace("condition ", "").replace(" ", "");

                    final Condition foundCondition = main.getConditionsYMLManager().getCondition(strippedCondition);
                    if (foundCondition != null) {
                        main.getLogManager().debug("Found conversation line condition: " + foundCondition.getConditionName());
                        foundCondition.setNegated(negated);
                        conditions.add(foundCondition);
                    } else {
                        main.getLogManager().warn("Unable to find conversation line condition (1): " + conditionString);
                    }

                }else {
                    final ArrayList<String> singleLineConditionStringArguments = new ArrayList<>(Arrays.asList(conditionString.split(" ")));

                    final Class<? extends Condition> conditionClass = main.getConditionsManager().getConditionClass(singleLineConditionStringArguments.get(0));
                    if(conditionClass == null){
                        //Check for NumberAction or StringAction first

                        for(String variableString : main.getVariablesManager().getVariableIdentifiers()) {
                            if(!variableString.equalsIgnoreCase(singleLineConditionStringArguments.get(0))){
                                continue;
                            }
                            main.getLogManager().info("Found variable for condition string " + conditionString + ": " + variableString);

                            Variable<?> variable = main.getVariablesManager().getVariableFromString(variableString);
                            if (variable == null || !variable.isCanSetValue()) {
                                continue;
                            }
                            if(variable.getVariableDataType() == VariableDataType.NUMBER){
                                final NumberCondition condition = new NumberCondition(main);
                                try{
                                    condition.deserializeFromSingleLineString(singleLineConditionStringArguments);
                                }catch (Exception e){
                                    main.getLogManager().warn("Unable to find create condition: " + singleLineConditionStringArguments.get(0) + ". The condition string seems to be incorrect. Condition String: " + String.join(" ", singleLineConditionStringArguments));
                                    e.printStackTrace();
                                    continue conditionLineLoop;
                                }
                                main.getLogManager().debug("Found conversation line condition: " + condition.getConditionName());
                                condition.setNegated(negated);
                                conditions.add(condition);
                                continue conditionLineLoop;
                            }else if(variable.getVariableDataType() == VariableDataType.STRING){
                                final StringCondition condition = new StringCondition(main);
                                try{
                                    condition.deserializeFromSingleLineString(singleLineConditionStringArguments);
                                }catch (Exception e){
                                    main.getLogManager().warn("Unable to find create condition: " + singleLineConditionStringArguments.get(0) + ". The condition string seems to be incorrect. Condition String: " + String.join(" ", singleLineConditionStringArguments));
                                    e.printStackTrace();
                                    continue conditionLineLoop;
                                }
                                main.getLogManager().debug("Found conversation line condition: " + condition.getConditionName());
                                condition.setNegated(negated);
                                conditions.add(condition);
                                continue conditionLineLoop;
                            }else if(variable.getVariableDataType() == VariableDataType.BOOLEAN){
                                final BooleanCondition condition = new BooleanCondition(main);
                                try{
                                    condition.deserializeFromSingleLineString(singleLineConditionStringArguments);
                                }catch (Exception e){
                                    main.getLogManager().warn("Unable to find create condition: " + singleLineConditionStringArguments.get(0) + ". The condition string seems to be incorrect. Condition String: " + String.join(" ", singleLineConditionStringArguments));
                                    e.printStackTrace();
                                    continue conditionLineLoop;
                                }
                                main.getLogManager().debug("Found conversation line condition: " + condition.getConditionName());
                                condition.setNegated(negated);
                                conditions.add(condition);
                                continue conditionLineLoop;
                            }else if(variable.getVariableDataType() == VariableDataType.LIST){

                                final ListCondition condition = new ListCondition(main);
                                try{
                                    condition.deserializeFromSingleLineString(singleLineConditionStringArguments);
                                }catch (Exception e){
                                    main.getLogManager().warn("Unable to find create condition: " + singleLineConditionStringArguments.get(0) + ". The condition string seems to be incorrect. Condition String: " + String.join(" ", singleLineConditionStringArguments));
                                    e.printStackTrace();
                                    continue conditionLineLoop;
                                }
                                main.getLogManager().debug("Found conversation line condition: " + condition.getConditionName());
                                condition.setNegated(negated);
                                conditions.add(condition);
                                continue conditionLineLoop;
                            }else{
                                main.getLogManager().warn("Unable to find conversation line condition's data type: " + singleLineConditionStringArguments.get(0) + ". Data type: " + variable.getVariableDataType().toString());
                                continue;
                            }
                        }

                        main.getLogManager().warn("Unable to find conversation line condition type: " + singleLineConditionStringArguments.get(0));
                        continue;
                    }

                    try{
                        final Condition condition = conditionClass.getDeclaredConstructor(NotQuests.class).newInstance(main);
                        singleLineConditionStringArguments.remove(0);
                        try{
                            condition.deserializeFromSingleLineString(singleLineConditionStringArguments);
                        }catch (Exception e){
                            main.getLogManager().warn("Unable to find create condition: " + singleLineConditionStringArguments.get(0) + ". The condition string seems to be incorrect. Condition String: " + String.join(" ", singleLineConditionStringArguments));
                            e.printStackTrace();
                            continue conditionLineLoop;
                        }
                        main.getLogManager().debug("Found conversation line condition: " + condition.getConditionName());
                        conditions.add(condition);

                    }catch (Exception e){
                        main.getLogManager().warn("Unable to instantiate conversation line condition: " + singleLineConditionStringArguments.get(0));
                    }
                }
            }
            return conditions;
        }
        return null;
    }

    public final ArrayList<Action> parseActionString(final List<String> allActionsString) {
        if (allActionsString != null && !allActionsString.isEmpty()) {
            ArrayList<Action> actions = new ArrayList<>();
            actionLineLoop:
            for (String actionString : allActionsString) {
                main.getLogManager().debug("<GREEN>Trying to find action in: " + actionString);
                if (actionString.toLowerCase(Locale.ROOT).startsWith("action ")) {

                    final Action foundAction = main.getActionsYMLManager().getAction(actionString.replace("action ", "").replace(" ", ""));
                    if (foundAction != null) {
                        main.getLogManager().debug("Found conversation line action: " + foundAction.getActionName());
                        actions.add(foundAction);
                    } else {
                        main.getLogManager().warn("Unable to find conversation line action: " + actionString);
                    }

                } else {
                    final ArrayList<String> singleLineActionStringArguments = new ArrayList<>(Arrays.asList(actionString.split(" ")));

                    final Class<? extends Action> actionClass = main.getActionManager().getActionClass(singleLineActionStringArguments.get(0));
                    if(actionClass == null){
                        //Check for NumberAction or StringAction first

                        for(String variableString : main.getVariablesManager().getVariableIdentifiers()) {
                            if(!variableString.equalsIgnoreCase(singleLineActionStringArguments.get(0))){
                                continue;
                            }
                            main.getLogManager().info("Found variable for action string " + actionString + ": " + variableString);

                            Variable<?> variable = main.getVariablesManager().getVariableFromString(variableString);
                            if (variable == null || !variable.isCanSetValue()) {
                                continue;
                            }
                            if(variable.getVariableDataType() == VariableDataType.NUMBER){
                                final NumberAction action = new NumberAction(main);
                                try{
                                    action.deserializeFromSingleLineString(singleLineActionStringArguments);
                                }catch (Exception e){
                                    main.getLogManager().warn("Unable to find create action: " + singleLineActionStringArguments.get(0) + ". The action string seems to be incorrect.");
                                    e.printStackTrace();
                                    continue actionLineLoop;
                                }
                                main.getLogManager().debug("Found conversation line action: " + action.getActionName());
                                actions.add(action);
                                continue actionLineLoop;
                            }else if(variable.getVariableDataType() == VariableDataType.STRING){
                                final StringAction action = new StringAction(main);
                                try{
                                    action.deserializeFromSingleLineString(singleLineActionStringArguments);
                                }catch (Exception e){
                                    main.getLogManager().warn("Unable to find create action: " + singleLineActionStringArguments.get(0) + ". The action string seems to be incorrect.");
                                    e.printStackTrace();
                                    continue actionLineLoop;
                                }
                                main.getLogManager().debug("Found conversation line action: " + action.getActionName());
                                actions.add(action);
                                continue actionLineLoop;
                            }else if(variable.getVariableDataType() == VariableDataType.BOOLEAN){
                                final BooleanAction action = new BooleanAction(main);
                                try{
                                    action.deserializeFromSingleLineString(singleLineActionStringArguments);
                                }catch (Exception e){
                                    main.getLogManager().warn("Unable to find create action: " + singleLineActionStringArguments.get(0) + ". The action string seems to be incorrect.");
                                    e.printStackTrace();
                                    continue actionLineLoop;
                                }
                                main.getLogManager().debug("Found conversation line action: " + action.getActionName());
                                actions.add(action);
                                continue actionLineLoop;
                            }else if(variable.getVariableDataType() == VariableDataType.LIST){
                                final ListAction action = new ListAction(main);
                                try{
                                    action.deserializeFromSingleLineString(singleLineActionStringArguments);
                                }catch (Exception e){
                                    main.getLogManager().warn("Unable to find create action: " + singleLineActionStringArguments.get(0) + ". The action string seems to be incorrect.");
                                    e.printStackTrace();
                                    continue actionLineLoop;
                                }
                                main.getLogManager().debug("Found conversation line action: " + action.getActionName());
                                actions.add(action);
                                continue actionLineLoop;
                            }else{
                                main.getLogManager().warn("Unable to find conversation line action: " + singleLineActionStringArguments.get(0));
                                continue;
                            }
                        }

                        main.getLogManager().warn("Unable to find conversation line action: " + singleLineActionStringArguments.get(0));

                        continue;
                    }

                    try{
                        final Action action = actionClass.getDeclaredConstructor(NotQuests.class).newInstance(main);
                        singleLineActionStringArguments.remove(0);
                        try{
                            action.deserializeFromSingleLineString(singleLineActionStringArguments);
                        }catch (Exception e){
                            main.getLogManager().warn("Unable to find create action: " + singleLineActionStringArguments.get(0) + ". The action string seems to be incorrect.");
                            e.printStackTrace();
                            continue actionLineLoop;
                        }
                        main.getLogManager().debug("Found conversation line action: " + action.getActionName());
                        actions.add(action);

                    }catch (Exception e){
                        main.getLogManager().warn("Unable to instantiate conversation line action: " + singleLineActionStringArguments.get(0));
                    }
                }
            }
            return actions;
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

    public String analyze(final ConversationLine conversationLine, String beginningSpaces) {
        StringBuilder toReturn = new StringBuilder();
        toReturn.append(beginningSpaces).append(" <unimportant>└<highlight>").append(conversationLine.getIdentifier()).append(":\n");
        toReturn.append(beginningSpaces).append("  <unimportant>Speaker:</unimportant> <main>").append(conversationLine.getSpeaker().getSpeakerName()).append("\n");


        if (conversationLine.getConditions().size() > 0) {
            toReturn.append(beginningSpaces).append("  <unimportant>1. Condition:</unimportant> <main>").append(conversationLine.getConditions().get(0).getConditionType()).append("\n");
        }
        if (conversationLine.getActions().size() > 0) {
            toReturn.append(beginningSpaces).append("  <unimportant>1. Action:</unimportant> <main>").append(conversationLine.getActions().get(0).getActionType()).append("\n");
        }

        toReturn.append(beginningSpaces).append("  <unimportant>Message:</unimportant> <main>").append(conversationLine.getMessage()).append("<RESET>\n");


        if (conversationLine.getNext().size() >= 1) {
            toReturn.append(beginningSpaces).append("  <unimportant>Next:</unimportant> \n");
            int counter = 0;
            for (ConversationLine next : conversationLine.getNext()) {
                counter++;
                String nextS = analyze(next, beginningSpaces + "  ");
                //main.getLogManager().debug(" ");
                //main.getLogManager().debug("Next of " + conversationLine.getIdentifier() + "\n" +nextS);
                //main.getLogManager().debug(" ");
                toReturn.append(nextS);
            }
        }


        return toReturn.toString().toString();

    }
}
