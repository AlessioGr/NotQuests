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

import rocks.gravili.notquests.Structs.Actions.Action;
import rocks.gravili.notquests.Structs.Conditions.Condition;

import java.util.ArrayList;

public class ConversationLine {
    private final Speaker speaker;
    private final String message; //minimessage
    private final ArrayList<ConversationLine> next;
    private final ArrayList<Action> actions;
    private final ArrayList<Condition> conditions;
    private String color = "<GRAY>";
    private boolean shout = false;


    private final String identifier;
    private final String fullIdentifier;

    public ConversationLine(final Speaker speaker, final String identifier, final String message) {
        this.speaker = speaker;
        this.identifier = identifier;
        this.message = message;
        next = new ArrayList<>();
        conditions = new ArrayList<>();
        actions = new ArrayList<>();

        this.fullIdentifier = speaker.getSpeakerName() + "." + identifier;
    }

    public final ArrayList<Condition> getConditions() {
        return conditions;
    }

    public void addCondition(final Condition condition) {
        this.conditions.add(condition);
    }

    public final String getFullIdentifier() {
        return fullIdentifier;
    }

    public final String getIdentifier() {
        return identifier;
    }

    public final String getMessage() {
        return message;
    }

    public final Speaker getSpeaker() {
        return speaker;
    }

    public final ArrayList<ConversationLine> getNext() {
        return next;
    }

    public void addNext(final ConversationLine nextLine) {
        this.next.add(nextLine);
    }

    public final String getColor() {
        return color;
    }

    public void setColor(final String color) {
        this.color = color;
    }

    public final boolean isShouting() {
        return shout;
    }

    public void setShouting(final boolean shouting) {
        this.shout = shouting;
    }

    public final ArrayList<Action> getActions() {
        return actions;
    }

    public void addAction(final Action newAction) {
        this.actions.add(newAction);
    }
}
