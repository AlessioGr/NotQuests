package com.notquests.builtin.actions;

import com.notquests.core.NotQuestsPlugin;
import com.notquests.core.actions.SavedActions.Chain;
import com.notquests.core.platform.NotQuestsAdapter;

import java.time.Duration;
import java.util.List;
import java.util.Locale;

public final class ActionAction {
    private static final String ACTIONS = "actions";
    private static final String AMOUNT = "amount";
    private static final String IGNORE_CONDITIONS = "ignoreConditions";
    private static final String MIN_RANDOM = "minRandom";
    private static final String MAX_RANDOM = "maxRandom";
    private static final String EXECUTED_ACTION_DELAY = "executedActionDelay";
    private static final String ONLY_COUNT_RANDOM_IF_CONDITIONS_FULFILLED = "onlyCountForRandomIfConditionsFulfilled";

    private static final Duration NO_DELAY_OVERRIDE = Duration.ofMillis(-1);

    private ActionAction() {}

    public static void register(final NotQuestsAdapter adapter) {
        register(NotQuestsPlugin.create(), adapter);
    }

    public static void register(final NotQuestsPlugin plugin, final NotQuestsAdapter adapter) {
        adapter.actions()
                .action("Action")
                .displayName("Action Chain")
                .description("Executes one or more saved actions, optionally multiple times or as a random subset.")
                .field(
                        ACTIONS,
                        adapter.fields().stringList(plugin::savedActionNames).config("specifics.actions"),
                        "Saved action name or comma-separated saved action names to execute.")
                .field(
                        AMOUNT,
                        adapter.fields().integer(1).config("specifics.amount"),
                        "Number of times to execute the selected saved actions.")
                .flag(
                        IGNORE_CONDITIONS,
                        adapter.fields().presenceFlag().config("specifics.ignoreConditions"),
                        "Execute referenced actions without checking their attached conditions.")
                .flag(
                        MIN_RANDOM,
                        adapter.fields().integer(-1).config("specifics.minRandom"),
                        "Minimum number of referenced actions to randomly choose each time this action runs.")
                .flag(
                        MAX_RANDOM,
                        adapter.fields().integer(-1).config("specifics.maxRandom"),
                        "Maximum number of referenced actions to randomly choose each time this action runs.")
                .flag(
                        EXECUTED_ACTION_DELAY,
                        adapter.fields().duration(NO_DELAY_OVERRIDE).config("specifics.executedActionDelay"),
                        "Delay applied to each referenced action, overriding that action's own delay.")
                .flag(
                        ONLY_COUNT_RANDOM_IF_CONDITIONS_FULFILLED,
                        adapter.fields().presenceFlag().config("specifics.onlyCountForRandomIfConditionsFulfilled"),
                        "When choosing random actions, skip actions whose conditions fail without counting them toward the random limit.")
                .singleLine((action, arguments) -> {
                    action.setValue(ACTIONS, arguments.get(0));
                    action.setValue(AMOUNT, arguments.size() >= 2 ? Integer.parseInt(arguments.get(1)) : 1);
                    action.setValue(IGNORE_CONDITIONS, flagPresent(arguments, "--ignoreconditions"));
                    action.setValue(MIN_RANDOM, integerFlag(arguments, "--minrandom", -1));
                    action.setValue(MAX_RANDOM, integerFlag(arguments, "--maxrandom", -1));
                })
                .execute((action, questPlayer, objects) -> plugin.executeSavedActions(
                        Chain.builder()
                                .actionNames(action.text(ACTIONS))
                                .amount(Math.max(1, action.integer(AMOUNT, 1)))
                                .ignoreConditions(action.flag(IGNORE_CONDITIONS))
                                .randomRange(action.integer(MIN_RANDOM, -1), action.integer(MAX_RANDOM, -1))
                                .delay(action.duration(EXECUTED_ACTION_DELAY, NO_DELAY_OVERRIDE))
                                .onlyCountRandomIfConditionsFulfilled(
                                        action.flag(ONLY_COUNT_RANDOM_IF_CONDITIONS_FULFILLED))
                                .objects(objects)
                                .build(),
                        adapter::schedule,
                        questPlayer,
                        adapter::warn))
                .actionDescription((action, questPlayer, objects) -> "Executes saved actions: " + action.text(ACTIONS))
                .register();
    }

    private static boolean flagPresent(final List<String> arguments, final String flag) {
        final String normalized = flag.toLowerCase(Locale.ROOT);
        for (final String argument : arguments) {
            if (argument.toLowerCase(Locale.ROOT).equals(normalized)) {
                return true;
            }
        }
        return false;
    }

    private static int integerFlag(
            final List<String> arguments, final String flag, final int fallback) {
        final String normalized = flag.toLowerCase(Locale.ROOT);
        for (int i = 0; i + 1 < arguments.size(); i++) {
            if (arguments.get(i).toLowerCase(Locale.ROOT).equals(normalized)) {
                return Integer.parseInt(arguments.get(i + 1));
            }
        }
        return fallback;
    }
}
