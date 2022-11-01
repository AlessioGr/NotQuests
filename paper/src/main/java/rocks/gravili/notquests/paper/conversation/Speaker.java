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

public class Speaker {
  private final String speakerName;
  private final String speakerDisplayName;

  private String color = "<WHITE>";
  private int talkSpeed = 10;
  private boolean player = false;

  private int delayInMS;

  private final Conversation conversation;

  public Speaker(final String speakerName, final Conversation conversation) {
    this.speakerName = speakerName;
    this.speakerDisplayName =
        speakerName.replace("__", "{UNDERSCORE}").replace("_", " ").replace("{UNDERSCORE}", "_");

    this.conversation = conversation;
    this.delayInMS = conversation.getDelayInMS();
  }

  public final String getSpeakerName() {
    return speakerName;
  }

  public final String getSpeakerDisplayName() {
    return speakerDisplayName;
  }

  public final String getColor() {
    return color;
  }

  public void setColor(final String newColor) {
    this.color = newColor;
  }

  public final int getTalkSpeed() {
    return talkSpeed;
  }

  public void setTalkSpeed(final int talkSpeed) {
    this.talkSpeed = talkSpeed;
  }

  public final boolean isPlayer() {
    return player;
  }

  public void setPlayer(final boolean player) {
    this.player = player;
  }

  public void setDelayInMS(final int delayInMS) {
    this.delayInMS = delayInMS;
  }

  public final int getDelayInMS() {
    return delayInMS;
  }

  public final Conversation getConversation(){
    return conversation;
  }
}
