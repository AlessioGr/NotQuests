package com.notquests.paper.integrations.betonquest.events;

import org.betonquest.betonquest.api.QuestException;
import org.betonquest.betonquest.api.instruction.Instruction;
import org.betonquest.betonquest.api.profile.Profile;
import org.betonquest.betonquest.api.quest.action.PlayerAction;
import org.betonquest.betonquest.api.quest.action.PlayerActionFactory;

import com.notquests.paper.NotQuests;

public class BQAbortQuestEvent implements PlayerActionFactory {
  private final NotQuests main;

  public BQAbortQuestEvent(final NotQuests main) {
    this.main = main;
  }

  @Override
  public PlayerAction parsePlayer(final Instruction instruction) throws QuestException {
    final String questName = instruction.nextElement();
    return profile -> abortQuest(profile, questName);
  }

  private void abortQuest(final Profile profile, final String questName) throws QuestException {
    final String failure = main.getCorePlugin().betonQuestAbortQuest(
        profile.getProfileUUID().toString(), questName);
    if (!failure.isBlank()) {
      throw new QuestException(failure);
    }
  }
}
