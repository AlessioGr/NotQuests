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
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.QuestPlayer;
import rocks.gravili.notquests.paper.structs.actions.Action;
import rocks.gravili.notquests.paper.structs.conditions.Condition;

import java.util.ArrayList;

public class ConversationPlayer {
    private final NotQuests main;
    private final QuestPlayer questPlayer;
    private final Player player;

    private final Conversation conversation;

    private final ArrayList<ConversationLine> currentPlayerLines;

    public ConversationPlayer(NotQuests main, QuestPlayer questPlayer, Player player, final Conversation conversation) {
        this.main = main;
        this.questPlayer = questPlayer;
        this.player = player;

        this.conversation = conversation;

        currentPlayerLines = new ArrayList<>();

    }

    public void play() {
        final ArrayList<ConversationLine> conversationLinesWhichFulfillsCondition = findConversationLinesWhichFulfillsCondition(conversation.getStartingLines());
        if(conversationLinesWhichFulfillsCondition == null || conversationLinesWhichFulfillsCondition.isEmpty()){
            main.getConversationManager().stopConversation(this);
            return;
        }

        final ConversationLine startingLine = conversationLinesWhichFulfillsCondition.get(0);
        if (startingLine != null) {
            next(startingLine, true);
        }

    }

    /**
     * Continues the conversation. Called, when either (1) starting line is called or (2) when the next line is NOT a clickable option line.
     *
     * @param currentLine
     * @return
     */
    public boolean next(final ConversationLine currentLine, boolean deletePrevious) {
        sendLine(currentLine, deletePrevious);

        ArrayList<ConversationLine> next = findConversationLinesWhichFulfillsCondition(currentLine.getNext());

        if (next == null || next.isEmpty()) {
            questPlayer.sendDebugMessage("Next of <highlight>" + currentLine.getFullIdentifier() + "</highlight> is empty. Ending conversation...");
            main.getConversationManager().stopConversation(this);
            return false;
        } else {
            if (next.size() == 1) {
                if (!next.get(0).getSpeaker().isPlayer()) {
                    questPlayer.sendDebugMessage("Line <highlight>" + currentLine.getFullIdentifier() + "</highlight> has one next: <highlight>" + next.get(0).getFullIdentifier());

                    if (!currentLine.getSpeaker().isPlayer()) { //Two consecutive non-playerl ines
                        return next(next.get(0), false); //Setting it to false won't remove the previous lines (if packet stuff enabled). Otherwise, the first line wouldn't even be visible to the player
                    } else {
                        return next(next.get(0), true);
                    }

                } else {
                    questPlayer.sendDebugMessage("Line <highlight>" + currentLine.getFullIdentifier() + "</highlight> has one PLAYER next: <highlight>" + next.get(0).getFullIdentifier());

                    nextPlayer(next);
                }


            } else { //Multiple player options
                nextPlayer(next);
            }

            return true;
        }
    }

    /**
     * Called when the next lines (playerLines) are clickable option lines
     *
     * @param playerLines
     * @return
     */
    public void nextPlayer(final ArrayList<ConversationLine> playerLines) {
        questPlayer.sendDebugMessage("Clearing currentPlayerLines (3)");
        currentPlayerLines.clear();
        questPlayer.sendDebugMessage("Adding " + playerLines.size() + " currentPlayerLines");
        currentPlayerLines.addAll(playerLines);


        final String chooseAnswerPrefixMiniMessage = main.getLanguageManager().getString("chat.conversations.choose-answer-prefix", player, conversation);
        main.sendMessage(player, chooseAnswerPrefixMiniMessage);



        if (main.getConfiguration().deletePreviousConversations) {
            ArrayList<Component> hist = main.getConversationManager().getConversationChatHistory().get(player.getUniqueId());
            if (hist == null) {
                hist = new ArrayList<>();
            }
            if(!chooseAnswerPrefixMiniMessage.isBlank()){
                hist.add(main.parse(chooseAnswerPrefixMiniMessage));
            }

            main.getConversationManager().getConversationChatHistory().put(player.getUniqueId(), hist);
        }


        for (final ConversationLine playerLine : playerLines) {
            sendOptionLine(playerLine);
        }

        if (main.getConfiguration().deletePreviousConversations) {
            ArrayList<Component> hist = main.getConversationManager().getConversationChatHistory().get(player.getUniqueId());
            if (hist == null) {
                hist = new ArrayList<>();
            }
            hist.add(Component.empty());
        }


        player.sendMessage(Component.empty());


        return;
    }


    public ArrayList<ConversationLine> findConversationLinesWhichFulfillsCondition(final ArrayList<ConversationLine> conversationLines) {
        if (conversationLines == null || conversationLines.size() == 0) {
            return null;
        } else {
            final ArrayList<ConversationLine> nextLines = new ArrayList<>();

            conversationLineLoop:
            for (final ConversationLine conversationLineToCheck : conversationLines) {
                if (conversationLineToCheck.getSpeaker().isPlayer()) {
                    if (conversationLineToCheck.getConditions().isEmpty()) {
                        nextLines.add(conversationLineToCheck);
                    } else { //Check conditions
                        for (final Condition condition : conversationLineToCheck.getConditions()) {
                            final String result = condition.check(getQuestPlayer());
                            if (!result.isBlank()) {
                                questPlayer.sendDebugMessage("Skipping player conversation line <highlight>" + conversationLineToCheck.getFullIdentifier() + "</highlight> because the following condition is not met: <highlight2>" + condition.getConditionIdentifier() + "</highlight2>. Condition result: <highlight2>" + result);
                                continue conversationLineLoop;
                            }
                        }
                        //If this is reached, all conditions passed
                        nextLines.add(conversationLineToCheck);
                    }
                } else {
                    if (nextLines.isEmpty()) { //So we don't mingle it with player lines if there already is one.
                        if (conversationLineToCheck.getConditions().isEmpty()) { //No unfulfilled conditions`=> Return it. We only need to return the first fulfilled condition if its not player lines.
                            nextLines.add(conversationLineToCheck);
                            return nextLines;
                        } else { //Check conditions
                            for (final Condition condition : conversationLineToCheck.getConditions()) {
                                final String result = condition.check(getQuestPlayer());
                                if (!result.isBlank()) {
                                    questPlayer.sendDebugMessage("Skipping conversation line <highlight>" + conversationLineToCheck.getFullIdentifier() + "</highlight> because the following condition is not met: <highlight2>" + condition.getConditionIdentifier() + "</highlight2>. Condition result: <highlight2>" + result);
                                    continue conversationLineLoop;
                                }
                            }
                            //If this is reached, all conditions passed
                            nextLines.add(conversationLineToCheck);
                            return nextLines;

                        }
                    }
                }
            }
            return nextLines;
        }

    }



    /**
     * Sends the player a normal text line. This is NOT a clickable/option line.
     *
     * @param conversationLine
     */
    public void sendLine(final ConversationLine conversationLine, boolean deletePrevious) {
        Component line = main.parse(
                conversationLine.getSpeaker().getColor() + "[" + conversationLine.getSpeaker().getSpeakerDisplayName() + "] <GRAY>" + main.getUtilManager().applyPlaceholders(conversationLine.getMessage(), player)
        );
        if (deletePrevious) {
            removeOldMessages();
        }


        if (main.getConfiguration().deletePreviousConversations) {
            ArrayList<Component> hist = main.getConversationManager().getConversationChatHistory().get(player.getUniqueId());
            if (hist == null) {
                hist = new ArrayList<>();
            }
            hist.add(line);
            main.getConversationManager().getConversationChatHistory().put(player.getUniqueId(), hist);
        }

        player.sendMessage(line);

        if (conversationLine.getActions() != null && conversationLine.getActions().size() > 0) {
            for (final Action action : conversationLine.getActions()) {
                main.getLogManager().debug("Executing action for conversation line...");
                main.getActionManager().executeActionWithConditions(action, questPlayer, player, true);
            }
        }


    }


    /**
     * Sends the player a clickable option text. The conversation will not continue until the player clicks it
     *
     * @param conversationLine
     */
    public void sendOptionLine(final ConversationLine conversationLine) {
        Component toSend = main.parse(
                conversationLine.getSpeaker().getColor() + " > <GRAY>" + main.getUtilManager().applyPlaceholders(conversationLine.getMessage(), player)
        )
                .clickEvent(ClickEvent.runCommand("/notquests continueConversation " + conversationLine.getMessage()))
                .hoverEvent(HoverEvent.showText(main.parse(main.getLanguageManager().getString("chat.conversations.choose-answer-answer-hover-text", player, conversation, conversationLine))));

        if (main.getConfiguration().deletePreviousConversations) {
            ArrayList<Component> hist = main.getConversationManager().getConversationChatHistory().get(player.getUniqueId());
            if (hist == null) {
                hist = new ArrayList<>();
            }
            hist.add(toSend);
            main.getConversationManager().getConversationChatHistory().put(player.getUniqueId(), hist);
        }

        player.sendMessage(toSend);
    }

    /**
     * Called when the player chooses an answer (clicks in chat).
     *
     * @param option option which the player chooses = exact message
     */
    public void chooseOption(final String option) {
        questPlayer.sendDebugMessage("Conversation option triggered: " + option + ". currentPlayerLines count: " + currentPlayerLines.size());
        for (final ConversationLine playerOptionLine : currentPlayerLines) {
            questPlayer.sendDebugMessage("Looking through current player line: <highlight>" + playerOptionLine.getMessage());
            if (playerOptionLine.getMessage().equalsIgnoreCase(option)) {

                //Trigger its actions first:
                if (playerOptionLine.getActions() != null && playerOptionLine.getActions().size() > 0) {
                    for (final Action action : playerOptionLine.getActions()) {
                        main.getLogManager().debug("Executing action for conversation line...");
                        main.getActionManager().executeActionWithConditions(action, questPlayer, player, true);
                    }
                }


                questPlayer.sendDebugMessage("Conversation option found!");

                ArrayList<ConversationLine> next = findConversationLinesWhichFulfillsCondition(playerOptionLine.getNext());

                if (next == null || next.isEmpty()) {
                    questPlayer.sendDebugMessage("Clearing currentPlayerLines (1)");
                    currentPlayerLines.clear();
                    main.getConversationManager().stopConversation(this);
                    return;
                } else {
                    questPlayer.sendDebugMessage("Clearing currentPlayerLines (2)");
                    currentPlayerLines.clear();
                    if (next.size() == 1) {

                        if (!next.get(0).getSpeaker().isPlayer()) {
                            next(next.get(0), true);
                        } else {
                            nextPlayer(next);
                        }


                    } else { //Multiple player options
                        nextPlayer(next);
                    }

                    return;
                }

            }
        }

    }


    /**
     * Resends the chat history without ANY conversation messages
     */
    public void removeOldMessages() {
        if (!main.getConfiguration().deletePreviousConversations) {
            return;
        }
        //Send back old messages
        ArrayList<Component> allChatHistory = main.getConversationManager().getChatHistory().get(getQuestPlayer().getUUID());

        main.getLogManager().debug("Conversation stop stage 1");

        if (allChatHistory == null) {
            return;
        }

        ArrayList<Component> allConversationHistory = main.getConversationManager().getConversationChatHistory().get(getQuestPlayer().getUUID());
        main.getLogManager().debug("Conversation stop stage 1.5");
        if (allConversationHistory == null) {
            return;
        }
        main.getLogManager().debug("Conversation stop stage 2");

        final Player player = getQuestPlayer().getPlayer();


        Component collectiveComponent = Component.text("");
        for (int i = 0; i < allChatHistory.size(); i++) {
            Component component = allChatHistory.get(i);
            if (component != null) {
                // audience.sendMessage(component.append(Component.text("fg9023zf729ofz")));
                collectiveComponent = collectiveComponent.append(component).append(Component.newline());
            }
        }
        player.sendMessage(collectiveComponent);

        allChatHistory.removeAll(allConversationHistory);
        allConversationHistory.clear();
        main.getConversationManager().getChatHistory().put(getQuestPlayer().getUUID(), allChatHistory);
        main.getConversationManager().getConversationChatHistory().put(getQuestPlayer().getUUID(), allConversationHistory);

        //maybe this won't send the huge, 1-component-chat-history again
        allConversationHistory.add(collectiveComponent);

    }

    public final QuestPlayer getQuestPlayer() {
        return questPlayer;
    }
}
