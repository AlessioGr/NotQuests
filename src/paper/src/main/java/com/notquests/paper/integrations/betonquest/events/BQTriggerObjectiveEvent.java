package com.notquests.paper.integrations.betonquest.events;

import org.betonquest.betonquest.api.QuestException;
import org.betonquest.betonquest.api.instruction.Instruction;
import org.betonquest.betonquest.api.profile.Profile;
import org.betonquest.betonquest.api.quest.action.PlayerAction;
import org.betonquest.betonquest.api.quest.action.PlayerActionFactory;

import com.notquests.paper.NotQuests;

public class BQTriggerObjectiveEvent implements PlayerActionFactory {
  private final NotQuests main;

  public BQTriggerObjectiveEvent(final NotQuests main) {
    this.main = main;
  }

  @Override
  public PlayerAction parsePlayer(final Instruction instruction) throws QuestException {
    final String triggerName = instruction.nextElement();
    return profile -> trigger(profile, triggerName);
  }

  private void trigger(final Profile profile, final String triggerName) throws QuestException {
    final String failure = main.getCorePlugin().betonQuestTriggerObjective(
        profile.getProfileUUID().toString(), triggerName);
    if (!failure.isBlank()) {
      throw new QuestException(failure);
    }
  }
}
