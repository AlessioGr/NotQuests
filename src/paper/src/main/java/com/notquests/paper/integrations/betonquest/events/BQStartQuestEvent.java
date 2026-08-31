package com.notquests.paper.integrations.betonquest.events;

import org.betonquest.betonquest.api.QuestException;
import org.betonquest.betonquest.api.instruction.Instruction;
import org.betonquest.betonquest.api.profile.Profile;
import org.betonquest.betonquest.api.quest.action.PlayerAction;
import org.betonquest.betonquest.api.quest.action.PlayerActionFactory;

import com.notquests.paper.NotQuests;

public class BQStartQuestEvent implements PlayerActionFactory {
  private final NotQuests main;

  public BQStartQuestEvent(final NotQuests main) {
    this.main = main;
  }

  @Override
  public PlayerAction parsePlayer(final Instruction instruction) throws QuestException {
    final String questName = instruction.nextElement();
    final String flags = String.join(" ", instruction.getValueParts());
    final boolean forced = flags.contains("-force");
    final boolean silent = flags.contains("-silent");
    final boolean triggers = !flags.contains("-notriggers");
    return profile -> startQuest(profile, questName, forced, silent, triggers);
  }

  private void startQuest(
      final Profile profile,
      final String questName,
      final boolean forced,
      final boolean silent,
      final boolean triggers)
      throws QuestException {
    final String failure = main.getCorePlugin().betonQuestStartQuest(
        profile.getProfileUUID().toString(),
        questName,
        forced,
        silent,
        triggers);
    if (!failure.isBlank()) {
      throw new QuestException(failure);
    }
  }
}
