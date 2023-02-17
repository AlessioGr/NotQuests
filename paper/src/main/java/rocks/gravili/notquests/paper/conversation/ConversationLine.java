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
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import rocks.gravili.notquests.paper.structs.actions.Action;
import rocks.gravili.notquests.paper.structs.conditions.Condition;

public class ConversationLine {
  private final Speaker speaker;
  private final List<String> messages; // minimessage
  private final ArrayList<ConversationLine> next;
  private final ArrayList<Action> actions;
  private final ArrayList<Condition> conditions;
  private final String identifier;
  private final String fullIdentifier;
  private String color = "<GRAY>";
  private boolean shout = false;
  private boolean skipMessage = false;

  private int delayInMS;

  public ConversationLine(final Speaker speaker, final String identifier, final List<String> messages) {
    this.speaker = speaker;
    this.identifier = identifier;
    this.messages = messages;
    next = new ArrayList<>();
    conditions = new ArrayList<>();
    actions = new ArrayList<>();

    this.fullIdentifier = speaker.getSpeakerName() + "." + identifier;
    this.delayInMS = speaker.getDelayInMS();
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

  public final List<String> getMessages() {
    if (isShouting()) {
      return this.messages.stream().map(s -> "<bold>" + s + "</bold>").toList();
    } else {
      return this.messages;
    }
  }

  public final String getOneMessage() {
    int random = ThreadLocalRandom.current().nextInt(this.messages.size());
    if (isShouting()) {
      return "<bold>" + this.messages.get(random) + "</bold>";
    } else {
      return this.messages.get(random);
    }
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

  public final boolean isSkipMessage() {
    return skipMessage;
  }

  public void setSkipMessage(final boolean skipMessage) {
    this.skipMessage = skipMessage;
  }

  public void setDelayInMS(final int delayInMS) {
    this.delayInMS = delayInMS;
  }

  public final int getDelayInMS() {
    return delayInMS;
  }

  public final int getDelayInTicks() {
    return delayInMS/50;
  }
}
