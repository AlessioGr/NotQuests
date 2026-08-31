package com.notquests.core.actions;

import com.notquests.core.conditions.Condition;
import com.notquests.core.conditions.ConditionCheck;
import com.notquests.core.platform.PlatformPlayer;
import com.notquests.core.registry.NotQuestsRegistry.Actions;
import com.notquests.core.registry.NotQuestsRegistry.Conditions;
import com.notquests.core.structs.Quest.ConditionSettings;
import com.notquests.core.structs.Quest;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public final class SavedActions {
    private final ConcurrentHashMap<String, SavedAction> actions = new ConcurrentHashMap<>();

    public List<String> names() {
        return actions.keySet().stream().sorted().toList();
    }

    public List<SavedAction> actions() {
        return actions.values().stream()
                .sorted(Comparator.comparing(SavedAction::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public void clear() {
        actions.clear();
    }

    public void save(
            final String actionName,
            final Actions.Type actionType,
            final Actions.Data data) {
        save(actionName, actionType, data, null);
    }

    public void save(
            final String actionName,
            final Actions.Type actionType,
            final Actions.Data data,
            final Duration executionDelay) {
        saveAndReturn(actionName, actionType, data, executionDelay);
    }

    public SavedAction saveAndReturn(
            final String actionName,
            final Actions.Type actionType,
            final Actions.Data data,
            final Duration executionDelay) {
        if (actionName == null || actionName.isBlank()) {
            throw new IllegalArgumentException("Saved action name cannot be blank.");
        }
        if (actionType == null) {
            throw new IllegalArgumentException("Saved action type cannot be null.");
        }
        final SavedAction action = new SavedAction(actionName, actionType, data, executionDelay);
        actions.put(actionName, action);
        return action;
    }

    public SavedAction action(final String actionName) {
        if (actionName == null || actionName.isBlank()) {
            return null;
        }
        return actions.get(actionName);
    }

    public boolean delete(final String actionName) {
        if (actionName == null || actionName.isBlank()) {
            return false;
        }
        return actions.remove(actionName) != null;
    }

    public Execution execute(
            final Chain request,
            final PlatformPlayer questPlayer,
            final Consumer<String> warningSink) {
        return execute(request, ActionScheduler.immediate(), questPlayer, warningSink);
    }

    public Execution execute(
            final Chain request,
            final ActionScheduler scheduler,
            final PlatformPlayer questPlayer,
            final Consumer<String> warningSink) {
        final ActionScheduler actionScheduler = scheduler == null ? ActionScheduler.immediate() : scheduler;
        final ArrayList<String> warnings = new ArrayList<>();
        final AtomicInteger executed = new AtomicInteger();
        final Consumer<String> warningsAndSink = warning -> {
            if (warning != null && !warning.isBlank()) {
                warnings.add(warning);
            }
            if (warningSink != null) {
                warningSink.accept(warning);
            }
        };
        ChainRunner.<SavedAction>builder()
                .resolveWith(actionName -> actions.get(actionName.trim()))
                .conditionsFulfilledBy(action -> conditionsFulfilled(action, questPlayer, warningsAndSink))
                .executeWith((action, ignoreConditions, delay, actionObjects) -> {
                    if (action.getType().executor() == null) {
                        return;
                    }
                    if (!ignoreConditions && !conditionsFulfilled(action, questPlayer, warningsAndSink)) {
                        return;
                    }
                    final Runnable executeAction =
                            () -> {
                                action.getType().executor().execute(action.getData(), questPlayer, actionObjects);
                                executed.incrementAndGet();
                            };
                    final Duration effectiveDelay = effectiveDelay(action.getExecutionDelay(), delay);
                    if (effectiveDelay == null || effectiveDelay.isNegative() || effectiveDelay.isZero()) {
                        executeAction.run();
                    } else {
                        actionScheduler.schedule(effectiveDelay, executeAction);
                    }
                })
                .warnWith(warningsAndSink)
                .build()
                .run(request);
        return new Execution(executed.get(), List.copyOf(warnings));
    }

    private static Duration effectiveDelay(final Duration savedDelay, final Duration requestDelay) {
        if (requestDelay != null && !requestDelay.isNegative() && !requestDelay.isZero()) {
            return requestDelay;
        }
        return savedDelay;
    }

    private static boolean conditionsFulfilled(
            final SavedAction action,
            final PlatformPlayer questPlayer,
            final Consumer<String> warningSink) {
        for (final SavedCondition condition : action.getConditions()) {
            if (condition.getType().checker() == null) {
                continue;
            }
            try {
                final String result = ConditionCheck.check(condition.getType(), condition.getData(), questPlayer);
                if (result != null && !result.isBlank()) {
                    if (warningSink != null) {
                        warningSink.accept(result);
                    }
                    return false;
                }
            } catch (final RuntimeException exception) {
                if (warningSink != null) {
                    warningSink.accept("Saved action condition failed: " + exception.getMessage());
                }
                return false;
            }
        }
        return true;
    }

    public record Execution(int executedActions, List<String> warnings) {
        public Execution {
            warnings = warnings == null ? List.of() : List.copyOf(warnings);
        }

        public boolean executedAny() {
            return executedActions > 0;
        }
    }

    public record ActionSettings(String name, long executionDelayMillis) {
        public ActionSettings {
            name = name == null ? "" : name;
        }

        public static ActionSettings from(final Action entry, final String fallbackName) {
            if (entry == null) {
                return new ActionSettings(fallbackName, -1);
            }
            final String name = entry.getDisplayName().isBlank() ? fallbackName : entry.getDisplayName();
            return new ActionSettings(name, executionDelayMillis(entry.data()));
        }

        private static long executionDelayMillis(final Actions.Data data) {
            if (data == null) {
                return -1;
            }
            final long delay = durationMillis(data.value("executionDelay"));
            return delay != -1 ? delay : durationMillis(data.value("executionDelayMillis"));
        }

        private static long durationMillis(final Object value) {
            if (value instanceof final Duration duration) {
                return duration.toMillis();
            }
            if (value instanceof final Number number) {
                return number.longValue();
            }
            if (value instanceof final String string && !string.isBlank()) {
                try {
                    return Long.parseLong(string);
                } catch (final NumberFormatException ignored) {
                    return -1;
                }
            }
            return -1;
        }
    }

    public record ActionCondition(
            int id,
            String typeId,
            Conditions.Data data,
            ConditionSettings settings) {
        public ActionCondition {
            typeId = typeId == null ? "" : typeId;
        }
    }

    /** One request to run a saved action chain. */
    public record Chain(
            String actionNames,
            int amount,
            int minRandom,
            int maxRandom,
            Duration delay,
            boolean ignoreConditions,
            boolean onlyCountRandomIfConditionsFulfilled,
            Object[] objects) {
        public Chain {
            amount = Math.max(1, amount);
            objects = objects == null ? new Object[0] : objects;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private String actionNames = "";
            private int amount = 1;
            private int minRandom = -1;
            private int maxRandom = -1;
            private Duration delay;
            private boolean ignoreConditions;
            private boolean onlyCountRandomIfConditionsFulfilled;
            private Object[] objects = new Object[0];

            private Builder() {}

            public Builder actionNames(final String actionNames) {
                this.actionNames = actionNames == null ? "" : actionNames;
                return this;
            }

            public Builder amount(final int amount) {
                this.amount = amount;
                return this;
            }

            public Builder randomRange(final int minRandom, final int maxRandom) {
                this.minRandom = minRandom;
                this.maxRandom = maxRandom;
                return this;
            }

            public Builder delay(final Duration delay) {
                this.delay = delay;
                return this;
            }

            public Builder ignoreConditions(final boolean ignoreConditions) {
                this.ignoreConditions = ignoreConditions;
                return this;
            }

            public Builder onlyCountRandomIfConditionsFulfilled(final boolean onlyCountRandomIfConditionsFulfilled) {
                this.onlyCountRandomIfConditionsFulfilled = onlyCountRandomIfConditionsFulfilled;
                return this;
            }

            public Builder objects(final Object... objects) {
                this.objects = objects == null ? new Object[0] : objects;
                return this;
            }

            public Chain build() {
                return new Chain(
                        actionNames,
                        amount,
                        minRandom,
                        maxRandom,
                        delay,
                        ignoreConditions,
                        onlyCountRandomIfConditionsFulfilled,
                        objects);
            }
        }
    }

    public static final class ChainRunner<T> {
        private final Function<String, T> actionLookup;
        private final Predicate<T> conditionsFulfilled;
        private final ActionExecutor<T> executor;
        private final Consumer<String> warningSink;

        private ChainRunner(final Builder<T> builder) {
            this.actionLookup = Objects.requireNonNull(builder.actionLookup, "actionLookup");
            this.conditionsFulfilled = builder.conditionsFulfilled == null ? ignored -> true : builder.conditionsFulfilled;
            this.executor = Objects.requireNonNull(builder.executor, "executor");
            this.warningSink = builder.warningSink == null ? ignored -> {} : builder.warningSink;
        }

        public static <T> Builder<T> builder() {
            return new Builder<>();
        }

        public void run(final Chain request) {
            final List<T> resolvedActions = resolve(request.actionNames());
            if (resolvedActions.isEmpty()) {
                if (request.actionNames() == null || request.actionNames().isBlank()) {
                    warningSink.accept("Cannot execute saved action chain: no saved actions were provided.");
                }
                return;
            }

            for (int run = 0; run < request.amount(); run++) {
                if (!usesRandomSelection(request)) {
                    for (final T action : resolvedActions) {
                        executor.execute(action, request.ignoreConditions(), request.delay(), request.objects());
                    }
                    continue;
                }

                final List<T> shuffled = new ArrayList<>(resolvedActions);
                Collections.shuffle(shuffled);
                final int amountToExecute = randomAmount(request.minRandom(), request.maxRandom());
                int executed = 0;
                for (int i = 0; executed < amountToExecute && i < shuffled.size(); i++) {
                    final T action = shuffled.get(i);
                    if (!request.ignoreConditions()
                            && request.onlyCountRandomIfConditionsFulfilled()
                            && !conditionsFulfilled.test(action)) {
                        continue;
                    }
                    executor.execute(action, request.ignoreConditions(), request.delay(), request.objects());
                    executed++;
                }
            }
        }

        private List<T> resolve(final String actionNames) {
            if (actionNames == null || actionNames.isBlank()) {
                return List.of();
            }
            final List<T> resolvedActions = new ArrayList<>();
            for (final String actionName : actionNames.split(",")) {
                final String trimmed = actionName.trim();
                final T action = actionLookup.apply(trimmed);
                if (action != null) {
                    resolvedActions.add(action);
                } else {
                    warningSink.accept("Action chain references unknown action '" + trimmed + "'.");
                }
            }
            return resolvedActions;
        }

        private static boolean usesRandomSelection(final Chain request) {
            return request.minRandom() != -1 || request.maxRandom() != -1;
        }

        private static int randomAmount(final int minRandom, final int maxRandom) {
            final int low = Math.max(0, minRandom);
            final int high = maxRandom < low ? low : maxRandom;
            return low == high ? low : ThreadLocalRandom.current().nextInt(high + 1 - low) + low;
        }

        public static final class Builder<T> {
            private Function<String, T> actionLookup;
            private Predicate<T> conditionsFulfilled;
            private ActionExecutor<T> executor;
            private Consumer<String> warningSink;

            private Builder() {}

            public Builder<T> resolveWith(final Function<String, T> actionLookup) {
                this.actionLookup = actionLookup;
                return this;
            }

            public Builder<T> conditionsFulfilledBy(final Predicate<T> conditionsFulfilled) {
                this.conditionsFulfilled = conditionsFulfilled;
                return this;
            }

            public Builder<T> executeWith(final ActionExecutor<T> executor) {
                this.executor = executor;
                return this;
            }

            public Builder<T> warnWith(final Consumer<String> warningSink) {
                this.warningSink = warningSink;
                return this;
            }

            public ChainRunner<T> build() {
                return new ChainRunner<>(this);
            }
        }

        @FunctionalInterface
        public interface ActionExecutor<T> {
            void execute(T action, boolean ignoreConditions, Duration delay, Object... objects);
        }
    }

    public static final class SavedAction {
        private final Actions.Type type;
        private final Action action;
        private final Duration executionDelay;
        private final String name;
        private final ArrayList<SavedCondition> conditions = new ArrayList<>();
        private String category = "";

        private SavedAction(
                final String name,
                final Actions.Type type,
                final Actions.Data data,
                final Duration executionDelay) {
            this.name = name == null ? "" : name;
            this.type = type;
            this.action = new Action(0, type.id(), data);
            this.executionDelay = executionDelay == null || executionDelay.isNegative() ? null : executionDelay;
        }

        public String getName() {
            return name;
        }

        public Actions.Type getType() {
            return type;
        }

        public Action getData() {
            return action;
        }

        public Duration getExecutionDelay() {
            return executionDelay;
        }

        public synchronized String getCategory() {
            return category;
        }

        public synchronized void setCategory(final String category) {
            this.category = category == null ? "" : category;
        }

        public synchronized int addCondition(
                final Conditions.Type type,
                final Conditions.Data data) {
            return addCondition(getFreeConditionID(), type, data);
        }

        public synchronized int addCondition(
                final int id,
                final Conditions.Type type,
                final Conditions.Data data) {
            final SavedCondition condition = new SavedCondition(id, type, data);
            conditions.add(condition);
            return condition.getID();
        }

        public synchronized SavedCondition addCondition(
                final int id,
                final Conditions.Type type,
                final Conditions.Data data,
                final long progressNeeded,
                final boolean negated,
                final String description,
                final String hiddenExpression) {
            final SavedCondition condition = new SavedCondition(id, type, data);
            condition.setProgressNeeded(progressNeeded);
            condition.setNegated(negated);
            condition.setDescription(description);
            condition.setHiddenExpression(hiddenExpression);
            conditions.add(condition);
            return condition;
        }

        public synchronized SavedCondition addCondition(
                final int id,
                final Conditions.Type type,
                final Conditions.Data data,
                final ConditionSettings settings) {
            final SavedCondition condition = new SavedCondition(id, type, data);
            condition.applySettings(settings);
            conditions.add(condition);
            return condition;
        }

        public synchronized List<SavedCondition> getConditions() {
            return List.copyOf(conditions);
        }

        public synchronized SavedCondition getConditionFromID(final int id) {
            return conditions.stream().filter(condition -> condition.getID() == id).findFirst().orElse(null);
        }

        public synchronized boolean removeCondition(final int id) {
            return conditions.removeIf(condition -> condition.getID() == id);
        }

        public synchronized void clearConditions() {
            conditions.clear();
        }

        public synchronized int getFreeConditionID() {
            for (int i = 1; i < Integer.MAX_VALUE; i++) {
                final int id = i;
                if (conditions.stream().noneMatch(condition -> condition.getID() == id)) {
                    return id;
                }
            }
            return conditions.size() + 1;
        }
    }

    public static final class SavedCondition {
        private final int id;
        private final Conditions.Type type;
        private final Condition condition;

        private SavedCondition(
                final int id,
                final Conditions.Type type,
                final Conditions.Data data) {
            this.id = id;
            this.type = type;
            this.condition = new Condition(id, type.id(), data);
        }

        public int getID() {
            return id;
        }

        public Conditions.Type getType() {
            return type;
        }

        public Condition getData() {
            return condition;
        }

        public String getDescription() {
            return condition.getDescription();
        }

        public void setDescription(final String description) {
            condition.setDescription(description);
        }

        public String getHiddenExpression() {
            return condition.getHiddenExpression();
        }

        public void setHiddenExpression(final String hiddenExpression) {
            condition.setHiddenExpression(hiddenExpression);
        }

        public long getProgressNeeded() {
            return condition.getProgressNeeded();
        }

        public void setProgressNeeded(final long progressNeeded) {
            condition.setProgressNeeded(progressNeeded);
        }

        public boolean isNegated() {
            return condition.isNegated();
        }

        public void setNegated(final boolean negated) {
            condition.setNegated(negated);
        }

        public void applySettings(final ConditionSettings settings) {
            if (settings == null) {
                return;
            }
            setProgressNeeded(settings.progressNeeded());
            setNegated(settings.negated());
            setDescription(settings.description());
            setHiddenExpression(settings.hiddenExpression());
        }
    }

    @FunctionalInterface
    public interface ActionScheduler {
        void schedule(Duration delay, Runnable action);

        static ActionScheduler immediate() {
            return (delay, action) -> action.run();
        }
    }
}
