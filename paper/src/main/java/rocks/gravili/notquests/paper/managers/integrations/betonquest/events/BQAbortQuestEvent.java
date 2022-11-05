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

package rocks.gravili.notquests.paper.managers.integrations.betonquest.events;

import org.betonquest.betonquest.Instruction;
import org.betonquest.betonquest.api.QuestEvent;
import org.betonquest.betonquest.api.profiles.Profile;
import org.betonquest.betonquest.exceptions.InstructionParseException;
import org.betonquest.betonquest.exceptions.QuestRuntimeException;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.ActiveQuest;
import rocks.gravili.notquests.paper.structs.Quest;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

public class BQAbortQuestEvent extends QuestEvent {

  private final NotQuests main;
  private Quest quest;

  /**
   * Creates new instance of the event. The event should parse instruction string without doing
   * anything else. If anything goes wrong, throw {@link InstructionParseException} with error
   * message describing the problem.
   *
   * @param instruction the Instruction object representing this event; you need to extract all
   *     required data from it and throw {@link InstructionParseException} if there is anything
   *     wrong
   * @throws InstructionParseException when the is an error in the syntax or argument parsing
   */
  public BQAbortQuestEvent(Instruction instruction) throws InstructionParseException {
    super(instruction, false);
    this.main = NotQuests.getInstance();

    final String questName = instruction.getPart(1);

    boolean foundQuest = false;
    for (Quest quest : main.getQuestManager().getAllQuests()) {
      if (quest.getIdentifier() .equalsIgnoreCase(questName)) {
        foundQuest = true;
        this.quest = quest;
        break;
      }
    }

    if (!foundQuest) {
      throw new InstructionParseException(
          "NotQuests Quest with the name '" + questName + "' does not exist.");
    }
  }

  @Override
  protected Void execute(final Profile profile) throws QuestRuntimeException {

    if (quest != null) {
      if (profile != null) {
        final QuestPlayer questPlayer =
            main.getQuestPlayerManager().getActiveQuestPlayer(profile.getProfileUUID());
        if (questPlayer != null) {

          ActiveQuest activeQuestToRemove = null;
          for (final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
            if (activeQuest.getQuest().getIdentifier().equalsIgnoreCase(quest.getIdentifier() )) {
              activeQuestToRemove = activeQuest;
              break;
            }
          }

          if (activeQuestToRemove != null) {
            questPlayer.getActiveQuests().remove(activeQuestToRemove);
          }
        }
      }

    } else {
      throw new QuestRuntimeException("NotQuests Quest of this BetonQuest event does not exist.");
    }

    return null;
  }
}
