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

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.NotQuests;
import rocks.gravili.notquests.Structs.QuestPlayer;

import java.util.ArrayList;

public class ConversationPlayer {
    private final NotQuests main;
    private final QuestPlayer questPlayer;
    private final Player player;
    private final Audience audience;

    private final Conversation conversation;

    private final ArrayList<ConversationLine> currentPlayerLines;

    public ConversationPlayer(NotQuests main, QuestPlayer questPlayer, Player player, final Conversation conversation) {
        this.main = main;
        this.questPlayer = questPlayer;
        this.player = player;
        this.audience = main.adventure().player(player);

        this.conversation = conversation;

        currentPlayerLines = new ArrayList<>();

    }

    public void play() {
        final ConversationLine startingLine = findConversationLinesWhichFulfillsCondition(conversation.getStartingLines()).get(0);
        if (startingLine != null) {


            next(startingLine);
        }

    }

    public boolean next(final ConversationLine currentLine) {
        sendLine(currentLine);

        ArrayList<ConversationLine> next = findConversationLinesWhichFulfillsCondition(currentLine.getNext());

        if (next == null) {
            return false;
        } else {
            if (next.size() == 1) {

                if (!next.get(0).getSpeaker().isPlayer()) {
                    return next(next.get(0));
                } else {
                    nextPlayer(next);
                }


            } else { //Multiple player options
                nextPlayer(next);
            }

            return true;
        }
    }

    public boolean nextPlayer(final ArrayList<ConversationLine> playerLines) {
        currentPlayerLines.clear();
        currentPlayerLines.addAll(playerLines);

        audience.sendMessage(MiniMessage.miniMessage().parse(
                "<BLUE>Choose your answer:"
        ));
        for (final ConversationLine playerLine : playerLines) {
            sendOptionLine(playerLine);
        }
        return true;
    }


    public ArrayList<ConversationLine> findConversationLinesWhichFulfillsCondition(final ArrayList<ConversationLine> conversationLines) {
        final ArrayList<ConversationLine> nextLines = new ArrayList<>();
        if (conversationLines.size() == 0) {
            return null;
        } else {
            for (final ConversationLine conversationLineToCheck : conversationLines) {
                if (conversationLineToCheck.getSpeaker().isPlayer()) {
                    nextLines.add(conversationLineToCheck);
                } else {
                    if (nextLines.isEmpty()) {
                        nextLines.add(conversationLineToCheck);
                        return nextLines; //TODO CONDITIONS

                    }
                }
            }
            return nextLines;
        }

    }

    public void sendLine(final ConversationLine conversationLine) {
        audience.sendMessage(MiniMessage.miniMessage().parse(
                conversationLine.getSpeaker().getColor() + "[" + conversationLine.getSpeaker().getSpeakerName() + "] <GRAY>" + conversationLine.getMessage()
        ));
    }

    public void sendOptionLine(final ConversationLine conversationLine) {
        Component toSend = MiniMessage.miniMessage().parse(
                conversationLine.getSpeaker().getColor() + "[" + conversationLine.getSpeaker().getSpeakerName() + "] <GRAY>" + conversationLine.getMessage()
        ).clickEvent(ClickEvent.runCommand("/notquests continueConversation " + conversationLine.getMessage())).hoverEvent(HoverEvent.showText(Component.text("Click to answer", NamedTextColor.AQUA)));


        audience.sendMessage(toSend);
    }


    public void chooseOption(final String option) {
        questPlayer.sendDebugMessage("Conversation option triggered: " + option);
        for (final ConversationLine playerOptionLine : currentPlayerLines) {
            if (playerOptionLine.getMessage().equalsIgnoreCase(option)) {

                questPlayer.sendDebugMessage("Conversation option found!");

                ArrayList<ConversationLine> next = findConversationLinesWhichFulfillsCondition(playerOptionLine.getNext());

                if (next == null) {
                    currentPlayerLines.clear();

                    return;
                } else {
                    if (next.size() == 1) {

                        if (!next.get(0).getSpeaker().isPlayer()) {
                            next(next.get(0));
                        } else {
                            nextPlayer(next);
                        }


                    } else { //Multiple player options
                        nextPlayer(next);
                    }
                    currentPlayerLines.clear();
                    return;
                }

            }
        }

    }
}
