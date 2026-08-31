package com.notquests.paper.integrations.betonquest.events;

import org.betonquest.betonquest.api.QuestException;
import org.betonquest.betonquest.api.instruction.Instruction;
import org.betonquest.betonquest.api.profile.Profile;
import org.betonquest.betonquest.api.quest.action.PlayerAction;
import org.betonquest.betonquest.api.quest.action.PlayerActionFactory;

import com.notquests.paper.NotQuests;

public class BQQuestPointsEvent implements PlayerActionFactory {
  private final NotQuests main;

  public BQQuestPointsEvent(final NotQuests main) {
    this.main = main;
  }

  @Override
  public PlayerAction parsePlayer(final Instruction instruction) throws QuestException {
    final String action = instruction.nextElement();
    final String amount = instruction.nextElement();
    final boolean silent = String.join(" ", instruction.getValueParts()).contains("-silent");
    return profile -> updateQuestPoints(profile, action, amount, silent);
  }

  private void updateQuestPoints(
      final Profile profile,
      final String action,
      final String amount,
      final boolean silent)
      throws QuestException {
    final String failure = main.getCorePlugin().betonQuestChangeQuestPoints(
        profile.getProfileUUID().toString(), action, amount, silent);
    if (!failure.isBlank()) {
      throw new QuestException(failure);
    }
  }
}
