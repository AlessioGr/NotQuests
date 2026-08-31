package com.notquests.paper.integrations.betonquest.conditions;

import org.betonquest.betonquest.api.instruction.Instruction;
import org.betonquest.betonquest.api.profile.Profile;
import org.betonquest.betonquest.api.quest.condition.PlayerCondition;
import org.betonquest.betonquest.api.quest.condition.PlayerConditionFactory;

import com.notquests.paper.NotQuests;
import com.notquests.paper.integrations.betonquest.BetonQuestInstructionUtil;

public class BQConditionsCondition implements PlayerConditionFactory {
  private final NotQuests main;

  public BQConditionsCondition(final NotQuests main) {
    this.main = main;
  }

  @Override
  public PlayerCondition parsePlayer(final Instruction instruction) {
    final String conditionLine = BetonQuestInstructionUtil.valueLine(instruction);
    return profile -> check(profile, conditionLine);
  }

  private boolean check(final Profile profile, final String conditionLine) {
    return main.getCorePlugin().checkBetonQuestConditionLine(
        profile.getProfileUUID().toString(), conditionLine);
  }
}
