package com.notquests.paper.builtin.variables.hooks;

import org.betonquest.betonquest.api.QuestException;

import com.notquests.core.platform.NotQuestsAdapter;
import com.notquests.core.registry.NotQuestsRegistry.Variables.Context;
import com.notquests.core.registry.NotQuestsRegistry.Variables;
import com.notquests.paper.NotQuests;
import com.notquests.paper.PaperNotQuestsAdapter;
import com.notquests.paper.integrations.betonquest.BetonQuestIntegration;

import java.util.List;

public final class BetonQuestConditionVariable {
  private BetonQuestConditionVariable() {}

  public static void register(final NotQuests main, final NotQuestsAdapter adapter) {
    final BetonQuestIntegration betonQuest = main.integrations().betonQuest();
    adapter.variables()
        .booleanVariable("BetonQuestCondition")
        .displayName("BetonQuest Condition")
        .description("Evaluates a BetonQuest condition for the target player.")
        .singular("BetonQuest Condition")
        .plural("BetonQuest Conditions")
        .field("package", adapter.fields().text(betonQuest::packageNames), "BetonQuest package that contains the condition.")
        .field("condition", adapter.fields().text(betonQuest::allConditionNames), "BetonQuest condition to test in the selected package.")
        .get(context -> conditionFulfilled(main, betonQuest, context))
        .possibleValues((questPlayer, objects) -> List.of("true", "false"))
        .register();
  }

  private static Boolean conditionFulfilled(
      final NotQuests main,
      final BetonQuestIntegration betonQuest,
      final Variables.Context context) {
    final String packageName = context.text("package");
    final String conditionName = context.text("condition");
    return main.getCorePlugin().checkBetonQuestConditionVariable(
        context.questPlayer(),
        packageName,
        conditionName,
        target -> {
          try {
            return betonQuest.testCondition(
                PaperNotQuestsAdapter.asPaperPlayer(target),
                packageName,
                conditionName);
          } catch (final QuestException exception) {
            throw new IllegalStateException(exception);
          }
        });
  }
}
