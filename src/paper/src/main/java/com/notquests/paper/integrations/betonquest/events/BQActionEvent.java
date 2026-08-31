package com.notquests.paper.integrations.betonquest.events;

import org.betonquest.betonquest.api.QuestException;
import org.betonquest.betonquest.api.instruction.Instruction;
import org.betonquest.betonquest.api.profile.Profile;
import org.betonquest.betonquest.api.quest.action.PlayerAction;
import org.betonquest.betonquest.api.quest.action.PlayerActionFactory;

import com.notquests.paper.NotQuests;
import com.notquests.paper.integrations.betonquest.BetonQuestInstructionUtil;

public class BQActionEvent implements PlayerActionFactory {
  private final NotQuests main;

  public BQActionEvent(final NotQuests main) {
    this.main = main;
  }

  @Override
  public PlayerAction parsePlayer(final Instruction instruction) {
    final String actionLine = BetonQuestInstructionUtil.valueLine(instruction);
    return profile -> executeAction(profile, actionLine);
  }

  private void executeAction(final Profile profile, final String actionLine) throws QuestException {
    final String failure = main.getCorePlugin().executeBetonQuestActionLine(
        profile.getProfileUUID().toString(), actionLine);
    if (!failure.isBlank()) {
      throw new QuestException(failure);
    }
  }
}
