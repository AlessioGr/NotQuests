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

package rocks.gravili.notquests.paper.conversation;

import java.util.ArrayList;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.conversation.interactionhandlers.ConversationInteractionHandler;
import rocks.gravili.notquests.paper.managers.npc.NQNPC;
import rocks.gravili.notquests.paper.structs.QuestPlayer;
import rocks.gravili.notquests.paper.structs.actions.Action;
import rocks.gravili.notquests.paper.structs.conditions.Condition;
import rocks.gravili.notquests.paper.structs.conditions.Condition.ConditionResult;

public class ConversationPlayer {
  private final NotQuests main;
  private final QuestPlayer questPlayer;
  private final Player player;
  private final NQNPC npc;

  private final Conversation conversation;
  private final ArrayList<ConversationLine> currentPlayerLines;

  public ConversationPlayer(
      final NotQuests main, final QuestPlayer questPlayer, final Player player, final Conversation conversation, NQNPC npc) {
    this.main = main;
    this.questPlayer = questPlayer;
    this.player = player;
    this.npc = npc;

    this.conversation = conversation;

    currentPlayerLines = new ArrayList<>();
  }

  public void play() {
    final ArrayList<ConversationLine> conversationLinesWhichFulfillsCondition =
        findConversationLinesWhichFulfillsCondition(conversation.getStartingLines());
    if (conversationLinesWhichFulfillsCondition == null
        || conversationLinesWhichFulfillsCondition.isEmpty()) {
      main.getConversationManager().stopConversation(this);
      return;
    }

    final ConversationLine startingLine = conversationLinesWhichFulfillsCondition.get(0);
    if (startingLine != null) {
      next(startingLine, true);
    }
  }

  /**
   * Continues the conversation. Called, when either (1) starting line is called or (2) when the
   * next line is NOT a clickable option line.
   *
   * @param currentLine
   * @return
   */
  public boolean next(final ConversationLine currentLine, boolean deletePrevious) {
    if(currentLine.getDelayInMS() <= 0){
      sendLine(currentLine, deletePrevious);
    }else {
      Bukkit.getScheduler().runTaskLater(main.getMain(), () -> sendLine(currentLine, deletePrevious), currentLine.getDelayInTicks());
    }


    final ArrayList<ConversationLine> next =
        findConversationLinesWhichFulfillsCondition(currentLine.getNext());

    if (next == null || next.isEmpty()) {
      questPlayer.sendDebugMessage(
          "Next of <highlight>"
              + currentLine.getFullIdentifier()
              + "</highlight> is empty. Ending conversation...");
      main.getConversationManager().stopConversation(this);
      return false;
    } else {
      if (next.size() == 1) {
        if (!next.get(0).getSpeaker().isPlayer()) {
          questPlayer.sendDebugMessage(
              "Line <highlight>"
                  + currentLine.getFullIdentifier()
                  + "</highlight> has one next: <highlight>"
                  + next.get(0).getFullIdentifier());

          if (!currentLine.getSpeaker().isPlayer()) { // Two consecutive non-playerl ines
            return next(
                next.get(0),
                false); // Setting it to false won't remove the previous lines (if packet stuff
                        // enabled). Otherwise, the first line wouldn't even be visible to the
                        // player
          } else {
            return next(next.get(0), true);
          }

        } else {
          questPlayer.sendDebugMessage(
              "Line <highlight>"
                  + currentLine.getFullIdentifier()
                  + "</highlight> has one PLAYER next: <highlight>"
                  + next.get(0).getFullIdentifier());

          nextPlayer(next);
        }

      } else { // Multiple player options
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
    if(playerLines.isEmpty()){
      questPlayer.sendDebugMessage("Clearing currentPlayerLines and skipping: there are no playerLines.");
      currentPlayerLines.clear();
    }
    if(playerLines.get(0).getDelayInMS() <= 0){
      nextPlayerInternal(playerLines);
    }else {
      Bukkit.getScheduler().runTaskLater(main.getMain(), () -> nextPlayerInternal(playerLines), playerLines.get(0).getDelayInTicks());
    }


  }

  private void nextPlayerInternal(final ArrayList<ConversationLine> playerLines) {
    questPlayer.sendDebugMessage("Clearing currentPlayerLines (3)");
    currentPlayerLines.clear();
    questPlayer.sendDebugMessage("Adding " + playerLines.size() + " currentPlayerLines");
    currentPlayerLines.addAll(playerLines);

    final String chooseAnswerPrefixMiniMessage =
            main.getLanguageManager()
                    .getString("chat.conversations.choose-answer-prefix", player, conversation);
    main.sendMessage(player, chooseAnswerPrefixMiniMessage);

    if (main.getConfiguration().deletePreviousConversations) {
      final ArrayList<Component> hist =
              main.getConversationManager().getConversationChatHistory().getOrDefault(player.getUniqueId(), new ArrayList<>());

      if (!chooseAnswerPrefixMiniMessage.isBlank()) {
        hist.add(main.parse(chooseAnswerPrefixMiniMessage));
      }

      main.getConversationManager().getConversationChatHistory().put(player.getUniqueId(), hist);
    }

    for (final ConversationLine playerLine : playerLines) {
      sendOptionLine(playerLine);
    }

    if (main.getConfiguration().deletePreviousConversations) {
      final ArrayList<Component> hist =
              main.getConversationManager().getConversationChatHistory().getOrDefault(player.getUniqueId(), new ArrayList<>());

      hist.add(Component.empty());
      main.getConversationManager().getConversationChatHistory().put(player.getUniqueId(), hist);
    }

    player.sendMessage(Component.empty());
  }


  public ArrayList<ConversationLine> findConversationLinesWhichFulfillsCondition(
      final ArrayList<ConversationLine> conversationLines) {
    if (conversationLines == null || conversationLines.size() == 0) {
      return null;
    } else {
      final ArrayList<ConversationLine> nextLines = new ArrayList<>();

      conversationLineLoop:
      for (final ConversationLine conversationLineToCheck : conversationLines) {
        if (conversationLineToCheck.getSpeaker().isPlayer()) {
          if (conversationLineToCheck.getConditions().isEmpty()) {
            nextLines.add(conversationLineToCheck);
          } else { // Check conditions
            for (final Condition condition : conversationLineToCheck.getConditions()) {
              final ConditionResult result = condition.check(getQuestPlayer());
              if (!result.fulfilled()) {
                questPlayer.sendDebugMessage(
                    "Skipping player conversation line <highlight>"
                        + conversationLineToCheck.getFullIdentifier()
                        + "</highlight> because the following condition is not met: <highlight2>"
                        + condition.getConditionIdentifier()
                        + "</highlight2>. Condition result: <highlight2>"
                        + result.message());
                continue conversationLineLoop;
              }
            }
            // If this is reached, all conditions passed
            nextLines.add(conversationLineToCheck);
          }
        } else {
          if (nextLines
              .isEmpty()) { // So we don't mingle it with player lines if there already is one.
            if (conversationLineToCheck
                .getConditions()
                .isEmpty()) { // No unfulfilled conditions`=> Return it. We only need to return the
                              // first fulfilled condition if its not player lines.
              nextLines.add(conversationLineToCheck);
              return nextLines;
            } else { // Check conditions
              for (final Condition condition : conversationLineToCheck.getConditions()) {
                final ConditionResult result = condition.check(getQuestPlayer());
                if (!result.fulfilled()) {
                  questPlayer.sendDebugMessage(
                      "Skipping conversation line <highlight>"
                          + conversationLineToCheck.getFullIdentifier()
                          + "</highlight> because the following condition is not met: <highlight2>"
                          + condition.getConditionIdentifier()
                          + "</highlight2>. Condition result: <highlight2>"
                          + result.message());
                  continue conversationLineLoop;
                }
              }
              // If this is reached, all conditions passed
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
  public void sendLine(final ConversationLine conversationLine, final boolean deletePrevious) {
    if(!conversationLine.isSkipMessage()){
      for(final ConversationInteractionHandler interactionHandler : main.getConversationManager().getInteractionHandlers()){
        interactionHandler.sendText(conversationLine.getOneMessage(), conversationLine.getSpeaker(), player, questPlayer, conversation, conversationLine, deletePrevious, this);
      }
    }

    if (conversationLine.getActions() != null && !conversationLine.getActions().isEmpty()) {
      for (final Action action : conversationLine.getActions()) {
        questPlayer.sendDebugMessage("Executing action for conversation line...");
        main.getActionManager().executeActionWithConditions(action, questPlayer, player, true);
      }
    }
  }


  /**
   * Sends the player a clickable option text. The conversation will not continue until the player
   * clicks it
   *
   * @param conversationLine
   */
  public void sendOptionLine(final ConversationLine conversationLine) {
    for(final ConversationInteractionHandler interactionHandler : main.getConversationManager().getInteractionHandlers()){
      interactionHandler.sendOption(conversationLine.getOneMessage(), conversationLine.getSpeaker(), player, questPlayer, conversation, conversationLine, this);
    }
  }


  /**
   * Called when the player chooses an answer (clicks in chat).
   *
   * @param option option which the player chooses = exact message
   */
  public void chooseOption(final ConversationLine option) {
    questPlayer.sendDebugMessage(
        "Conversation option triggered: "
            + option
            + ". currentPlayerLines count: "
            + currentPlayerLines.size());

    if(!currentPlayerLines.contains(option)){
      questPlayer.sendDebugMessage("Option is not in current player lines. Ignoring.");
      return;
    }

    // Trigger its actions first:
    if (option.getActions() != null && !option.getActions().isEmpty()) {
      for (final Action action : option.getActions()) {
        questPlayer.sendDebugMessage("Executing action for conversation line...");
        main.getActionManager().executeActionWithConditions(action, questPlayer, player, true);
      }
    }

    questPlayer.sendDebugMessage("Conversation option found!");

    final ArrayList<ConversationLine> next =
        findConversationLinesWhichFulfillsCondition(option.getNext());

    if (next == null || next.isEmpty()) {
      questPlayer.sendDebugMessage("Clearing currentPlayerLines (1)");
      currentPlayerLines.clear();
      main.getConversationManager().stopConversation(this);
    } else {
      questPlayer.sendDebugMessage("Clearing currentPlayerLines (2)");
      currentPlayerLines.clear();
      if (next.size() == 1) {

        if (!next.get(0).getSpeaker().isPlayer()) {
          next(next.get(0), true);
        } else {
          nextPlayer(next);
        }

      } else { // Multiple player options
        nextPlayer(next);
      }
    }
  }

  public final QuestPlayer getQuestPlayer() {
    return questPlayer;
  }

  public final ArrayList<ConversationLine> getCurrentPlayerLines() {
    return currentPlayerLines;
  }

  public final Conversation getConversation(){
    return conversation;
  }

  public NQNPC getNpc() {
    return this.npc;
  }
}
