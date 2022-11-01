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

package rocks.gravili.notquests.paper.structs;

/**
 * This is a special object for completed quests. Unlike the ActiveQuest object, it does not need to
 * contain the progress, as it's already expected that progress = complete. Apart from, obviously,
 * the quest object, to know what quest it was, it additionally contains the time it was completed
 * (System.currentTimeMilis thingy) and the questPlayer object, to know who finished the active
 * quest.
 *
 * <p>The timeCompleted is needed for the quest cooldown to work. All completed quests for a player
 * are saved in the Database.
 *
 * @author Alessio Gravili
 */
public class CompletedQuest {
  private final Quest quest;

  private final long timeCompleted;

  private final QuestPlayer questPlayer;

  public CompletedQuest(final Quest quest, final QuestPlayer questPlayer) {
    this.quest = quest;
    this.questPlayer = questPlayer;
    timeCompleted = System.currentTimeMillis();
  }

  public CompletedQuest(
      final Quest quest, final QuestPlayer questPlayer, final long timeCompleted) {
    this.quest = quest;
    this.questPlayer = questPlayer;
    this.timeCompleted = timeCompleted;
  }

  public final Quest getQuest() {
    return quest;
  }

  public final QuestPlayer getQuestPlayer() {
    return questPlayer;
  }

  public final long getTimeCompleted() {
    return timeCompleted;
  }

  public final String getQuestIdentifier() {
    return quest.getIdentifier();
  }
}
