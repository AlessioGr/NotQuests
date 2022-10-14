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

package rocks.gravili.notquests.paper.managers.integrations.betonquest.conditions;

import java.util.ArrayList;
import java.util.List;
import org.betonquest.betonquest.Instruction;
import org.betonquest.betonquest.api.profiles.Profile;
import org.betonquest.betonquest.exceptions.InstructionParseException;
import org.betonquest.betonquest.exceptions.QuestRuntimeException;
import org.betonquest.betonquest.id.ID;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.conditions.Condition;

public class BQConditionsCondition
    extends org.betonquest.betonquest.api
        .Condition { // TODO: Make it dynamic for future or API requirements

  private final NotQuests main;
  private Condition condition = null;

  /**
   * Creates new instance of the condition. The condition should parse instruction string at this
   * point and extract all the data from it. If anything goes wrong, throw {@link
   * InstructionParseException} with an error message describing the problem.
   *
   * @param instruction the Instruction object; you can get one from ID instance with {@link
   *     ID#generateInstruction() ID.generateInstruction()} or create it from an instruction string
   */
  public BQConditionsCondition(Instruction instruction) throws InstructionParseException {
    super(instruction, false);
    this.main = NotQuests.getInstance();

    final List<String> allConditionsString = new ArrayList<>();
    allConditionsString.add(instruction.toString().replace("nq_condition ", ""));

    try {
      condition = main.getConversationManager().parseConditionsString(allConditionsString).get(0);
    } catch (Exception e) {
      throw new RuntimeException("Invalid Condition line: " + e.getLocalizedMessage());
    }
  }

  @Override
  protected Boolean execute(final Profile profile) throws QuestRuntimeException {
    if (condition != null) {
      return condition
          .check(main.getQuestPlayerManager().getOrCreateQuestPlayer(profile.getProfileUUID()))
          .fulfilled();
    } else {
      throw new QuestRuntimeException("Condition was not found.");
    }
  }
}
