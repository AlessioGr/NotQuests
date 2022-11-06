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
import org.betonquest.betonquest.api.Condition;
import org.betonquest.betonquest.api.profiles.Profile;
import org.betonquest.betonquest.exceptions.InstructionParseException;
import org.betonquest.betonquest.exceptions.QuestRuntimeException;
import org.betonquest.betonquest.id.ID;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.ActiveObjective;
import rocks.gravili.notquests.paper.structs.ActiveQuest;
import rocks.gravili.notquests.paper.structs.Quest;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

public class BQActiveQuestObjectiveCompleted extends Condition {

  private final NotQuests main;
  private final int objectiveID;
  private Quest quest;

  /**
   * Creates new instance of the condition. The condition should parse instruction string at this
   * point and extract all the data from it. If anything goes wrong, throw {@link
   * InstructionParseException} with an error message describing the problem.
   *
   * @param instruction the Instruction object; you can get one from ID instance with {@link
   *     ID#generateInstruction() ID.generateInstruction()} or create it from an instruction string
   */
  public BQActiveQuestObjectiveCompleted(Instruction instruction) throws InstructionParseException {
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

    try {
      this.objectiveID = Integer.parseInt(instruction.getPart(2));
    } catch (NumberFormatException ignored) {
      throw new InstructionParseException(
          "Cannot read the objective ID. Second argument needs to be a number.");
    }
  }

  @Override
  protected Boolean execute(final Profile profile) throws QuestRuntimeException {
    if (quest != null) {
      if (profile != null) {
        final QuestPlayer questPlayer =
            main.getQuestPlayerManager().getActiveQuestPlayer(profile.getProfileUUID());
        if (questPlayer != null) {

          for (final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
            if (activeQuest.getQuest().getIdentifier().equalsIgnoreCase(quest.getIdentifier() )) {
              for (final ActiveObjective objective : activeQuest.getCompletedObjectives()) {
                if (objective.getObjectiveID() == objectiveID) {
                  return true;
                }
              }
              return false;
            }
          }
          return false;
        }
        return false;
      }

    } else {
      throw new QuestRuntimeException("NotQuests Quest of this BetonQuest event does not exist.");
    }
    return null;
  }
}
