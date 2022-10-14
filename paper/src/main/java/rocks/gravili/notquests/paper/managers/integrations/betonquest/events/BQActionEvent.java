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

import java.util.ArrayList;
import java.util.List;
import org.betonquest.betonquest.Instruction;
import org.betonquest.betonquest.api.QuestEvent;
import org.betonquest.betonquest.api.profiles.Profile;
import org.betonquest.betonquest.exceptions.InstructionParseException;
import org.betonquest.betonquest.exceptions.QuestRuntimeException;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.actions.Action;

public class BQActionEvent extends QuestEvent {

  private final NotQuests main;
  private Action action;

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
  public BQActionEvent(Instruction instruction) throws InstructionParseException {
    super(instruction, false);
    this.main = NotQuests.getInstance();

    String instructionString = "";
    int counter = 0;
    // main.getLogManager().info("instruction before: " + instruction.toString());
    for (String instructionPart : instruction.toString().split(" ")) {
      // main.getLogManager().info("instruction part: " + instructionPart);
      String[] semicolonSplit = instructionPart.split(";");
      if (semicolonSplit.length >= 4) {
        instructionPart =
            semicolonSplit[3]
                + " "
                + semicolonSplit[0]
                + " "
                + semicolonSplit[1]
                + " "
                + semicolonSplit[2];
      }

      if (++counter == 1) {
        instructionString += instructionPart;
      } else {
        instructionString += " " + instructionPart;
      }
    }
    // main.getLogManager().info("instruction after: " + instructionString);

    final List<String> allActionsString = new ArrayList<>();
    allActionsString.add(instructionString.replace("nq_action ", ""));

    try {
      action = main.getConversationManager().parseActionString(allActionsString).get(0);
    } catch (Exception e) {
      throw new RuntimeException("Invalid Action line: " + e.getLocalizedMessage());
    }

    if (action == null) {
      throw new InstructionParseException(
          "NotQuests Action with the name '" + instruction.toString() + "' does not exist.");
    }
  }

  @Override
  protected Void execute(final Profile profile) throws QuestRuntimeException {

    // execute action here
    if (action != null) {
      action.execute(main.getQuestPlayerManager().getOrCreateQuestPlayer(profile.getProfileUUID()));

    } else {
      main.getLogManager()
          .warn("Error executing action (triggered by BetonQuests) - action was not found.");
      throw new QuestRuntimeException(
          "Error executing NotQuests action (triggered by BetonQuests) - action was not found.");
    }

    return null;
  }
}
