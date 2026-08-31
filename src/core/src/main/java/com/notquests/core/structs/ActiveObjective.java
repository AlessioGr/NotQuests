package com.notquests.core.structs;

import com.notquests.core.conditions.Condition;
import com.notquests.core.items.ItemSelection;
import com.notquests.core.objectives.Objective;
import com.notquests.core.platform.NQLocation;
import com.notquests.core.platform.PlatformPlayer;
import com.notquests.core.registry.NotQuestsRegistry.Objectives;
import com.notquests.core.registry.NotQuestsRegistry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public final class ActiveObjective implements Objectives.Progress {
    private final String objectiveTypeId;
    private final String questName;
    private final int objectiveId;
    private final int[] objectivePath;
    private final String objectivePathKey;
    private final String holderPath;
    private final String ownerProgressOrder;
    private final Objectives.Type type;
    private final Objective objective;
    private final PlatformPlayer questPlayer;
    private final ConditionGate conditionGate;
    private final Consumer<ActiveObjective> completionHandler;
    private final ProgressChangeHandler progressChangeHandler;
    private final CopyOnWriteArrayList<ActiveObjective> children = new CopyOnWriteArrayList<>();
    private final Map<String, Object> values;
    private final double progressNeeded;
    private double progress;
    private boolean unlocked;
    private boolean completed;
    private boolean silentCompletion;
    private boolean suppressCompletionEffects;

    public static ActiveObjective configured(final double progressNeeded) {
        return new ActiveObjective(progressNeeded);
    }

    public static String actionBarTranslationKey(final double progressNeeded) {
        return progressNeeded == 1
                ? "objective-tracking.actionbar-progress-update.only-one-max-progress"
                : "objective-tracking.actionbar-progress-update.default";
    }

    public static String bossBarTranslationKey(final double progressNeeded) {
        return progressNeeded == 1
                ? "objective-tracking.bossbar-progress-update.only-one-max-progress"
                : "objective-tracking.bossbar-progress-update.default";
    }

    public static float clampedProgress(final double currentProgress, final double progressNeeded) {
        if (progressNeeded == 0) {
            return 0.0f;
        }
        final float progress = (float) (currentProgress / progressNeeded);
        if (progress < 0.0f) {
            return 0.0f;
        }
        if (progress > 1.0f) {
            return 1.0f;
        }
        return progress;
    }

    public static boolean shouldHideCompletedBossBar(final float progress, final boolean showCompleted) {
        return progress >= 1.0f && !showCompleted;
    }

    private ActiveObjective(final double progressNeeded) {
        this.questName = "";
        this.objectiveId = 0;
        this.objectivePath = new int[0];
        this.objectivePathKey = "";
        this.holderPath = "";
        this.ownerProgressOrder = "";
        this.objectiveTypeId = "";
        this.type = null;
        this.objective = null;
        this.questPlayer = null;
        this.conditionGate = ConditionGate.ALWAYS;
        this.completionHandler = ignored -> {};
        this.progressChangeHandler = (ignored, oldValue, newValue) -> {};
        this.values = Map.of();
        this.progressNeeded = progressNeeded;
    }

    ActiveObjective(
            final String questName,
            final int objectiveId,
            final int[] objectivePath,
            final String ownerProgressOrder,
            final Objectives.Type type,
            final Objective objective,
            final PlatformPlayer questPlayer,
            final double progressNeeded,
            final ConditionGate conditionGate,
            final Consumer<ActiveObjective> completionHandler,
            final ProgressChangeHandler progressChangeHandler) {
        this.questName = questName == null ? "" : questName;
        this.objectiveId = objectiveId;
        this.objectivePath = cleanPath(objectivePath, objectiveId);
        this.objectivePathKey = pathKey(this.objectivePath);
        this.holderPath = holderPath(this.questName, this.objectivePath);
        this.ownerProgressOrder = ownerProgressOrder == null ? "" : ownerProgressOrder;
        this.type = Objects.requireNonNull(type, "type");
        this.objectiveTypeId = type.id();
        this.objective = Objects.requireNonNull(objective, "objective");
        this.questPlayer = Objects.requireNonNull(questPlayer, "questPlayer");
        this.conditionGate = conditionGate == null ? ConditionGate.ALWAYS : conditionGate;
        this.completionHandler = completionHandler == null ? ignored -> {} : completionHandler;
        this.progressChangeHandler = progressChangeHandler == null ? (ignored, oldValue, newValue) -> {} : progressChangeHandler;
        this.values = new HashMap<>(objective.values());
        this.progressNeeded = progressNeeded;
    }

    public String getQuestIdentifier() {
        return questName;
    }

    public int getObjectiveID() {
        return objectiveId;
    }

    public int[] getObjectivePath() {
        return objectivePath.clone();
    }

    public String getObjectivePathKey() {
        return objectivePathKey;
    }

    public String getHolderPath() {
        return holderPath;
    }

    public boolean hasObjectivePath(final int[] path) {
        return objectivePathKey.equals(pathKey(path));
    }

    public boolean isDirectChildOf(final ActiveObjective parent) {
        if (parent == null || !questName.equalsIgnoreCase(parent.getQuestIdentifier())) {
            return false;
        }
        final int[] parentPath = parent.objectivePath;
        if (objectivePath.length != parentPath.length + 1) {
            return false;
        }
        for (int i = 0; i < parentPath.length; i++) {
            if (objectivePath[i] != parentPath[i]) {
                return false;
            }
        }
        return true;
    }

    String ownerProgressOrder() {
        return ownerProgressOrder;
    }

    public Objective getObjective() {
        return objective;
    }

    public PlatformPlayer getQuestPlayer() {
        return questPlayer;
    }

    @Override
    public PlatformPlayer questPlayer() {
        return getQuestPlayer();
    }

    public String getObjectiveTypeID() {
        return objectiveTypeId;
    }

    public Objectives.Type getType() {
        return type;
    }

    @Override
    public void copyTo(final Objectives.Draft objective) {
        values.forEach(objective::setValue);
    }

    public List<ActiveObjective> getActiveObjectives() {
        return List.copyOf(children);
    }

    ActiveObjective child(final int objectiveId) {
        return children.stream()
                .filter(child -> child.getObjectiveID() == objectiveId)
                .findFirst()
                .orElse(null);
    }

    boolean addChild(final ActiveObjective child) {
        if (child == null || !child.isDirectChildOf(this)) {
            return false;
        }
        removeChild(child.getObjectiveID());
        children.add(child);
        return true;
    }

    ActiveObjective removeChild(final int objectiveId) {
        final ActiveObjective child = child(objectiveId);
        if (child != null) {
            children.remove(child);
        }
        return child;
    }

    public double getCurrentProgress() {
        return progress;
    }

    @Override
    public double currentProgress() {
        return getCurrentProgress();
    }

    public boolean isComplete() {
        return completed || (progress >= progressNeeded && childObjectiveCount() == 0);
    }

    public boolean hasBeenCompleted() {
        return completed;
    }

    public boolean isSilentCompletion() {
        return silentCompletion;
    }

    public boolean isSuppressCompletionEffects() {
        return suppressCompletionEffects;
    }

    public void setCompletionOptions(final boolean silentCompletion, final boolean suppressCompletionEffects) {
        this.silentCompletion = silentCompletion;
        this.suppressCompletionEffects = suppressCompletionEffects;
    }

    boolean completedFlag() {
        return completed;
    }

    public void restoreProgress(final double progress, final boolean completed) {
        this.progress = progress;
        this.completed = completed;
    }

    void forceCompleteWithoutEffects() {
        progress = Math.max(progress, progressNeeded);
        completed = true;
    }

    public boolean isUnlocked() {
        return unlocked;
    }

    boolean updateUnlocked() {
        final boolean wasUnlocked = unlocked;
        unlocked = conditionGate.conditionsFulfilled(objective.getConditions("unlock"), questPlayer, false);
        return unlocked && !wasUnlocked;
    }

    boolean lock() {
        final boolean wasUnlocked = unlocked;
        unlocked = false;
        return wasUnlocked;
    }

    @Override
    public double progressNeeded() {
        return getProgressNeeded();
    }

    public double getProgressNeeded() {
        return progressNeeded;
    }

    @Override
    public int childObjectiveCount() {
        return getIncompleteChildObjectiveCount();
    }

    public int getIncompleteChildObjectiveCount() {
        return (int) children.stream().filter(child -> !child.completedFlag()).count();
    }

    @Override
    public void addProgress(final double amount) {
        if (amount <= 0 || completed || !unlocked || !canProgress(false)) {
            return;
        }
        final double oldProgress = progress;
        progress += amount;
        progressChangeHandler.handle(this, oldProgress, progress);
        completeIfReady();
    }

    @Override
    public void setProgress(final double progress, final boolean capAtZero) {
        if (completed || !unlocked || !canProgress(progress < this.progress)) {
            return;
        }
        final double oldProgress = this.progress;
        this.progress = capAtZero && progress < 0 ? 0 : progress;
        progressChangeHandler.handle(this, oldProgress, this.progress);
        completeIfReady();
    }

    @Override
    public void removeProgress(final double amount, final boolean capAtZero) {
        if (amount <= 0 || completed || !unlocked || !canProgress(true)) {
            return;
        }
        final double oldProgress = progress;
        progress -= amount;
        if (capAtZero && progress < 0) {
            progress = 0;
        }
        progressChangeHandler.handle(this, oldProgress, progress);
    }

    @Override
    public Object value(final String name) {
        return values.get(name);
    }

    @Override
    public String text(final String name) {
        final Object value = values.get(name);
        return value == null ? "" : value.toString();
    }

    @Override
    public boolean flag(final String name) {
        return Boolean.TRUE.equals(values.get(name));
    }

    @Override
    public int integer(final String name, final int fallback) {
        final Object value = values.get(name);
        return value instanceof Number number ? number.intValue() : fallback;
    }

    @Override
    public double number(final String name, final double fallback) {
        final Object value = values.get(name);
        return value instanceof Number number ? number.doubleValue() : fallback;
    }

    @Override
    public NQLocation location(final String name) {
        return values.get(name) instanceof NQLocation location ? location : null;
    }

    @Override
    public ItemSelection itemSelection(final String name) {
        return values.get(name) instanceof ItemSelection itemSelection ? itemSelection : null;
    }

    private boolean canProgress(final boolean progressDecrease) {
        return conditionGate.conditionsFulfilled(objective.getConditions("progress"), questPlayer, progressDecrease);
    }

    private boolean canComplete() {
        return conditionGate.conditionsFulfilled(objective.getConditions("complete"), questPlayer, false);
    }

    private void completeIfReady() {
        if (objective.getCompletionNPC() != null && !objective.getCompletionNPC().isBlank()) {
            return;
        }
        completeIfReadyIgnoringNpc();
    }

    public boolean completeWithNpc(final String npcSelector) {
        if (completed || npcSelector == null || npcSelector.isBlank()) {
            return false;
        }
        final String completionNpc = objective.getCompletionNPC();
        if (completionNpc == null || completionNpc.isBlank() || !completionNpc.equalsIgnoreCase(npcSelector)) {
            return false;
        }
        return completeIfReadyIgnoringNpc();
    }

    public boolean readyToComplete(final String npcSelector) {
        if (completed) {
            return true;
        }
        final String completionNpc = objective.getCompletionNPC();
        final boolean npcMatches = completionNpc == null
                || completionNpc.isBlank()
                || (npcSelector != null && completionNpc.equalsIgnoreCase(npcSelector));
        return npcMatches && progress >= progressNeeded && childObjectiveCount() == 0 && canComplete();
    }

    private boolean completeIfReadyIgnoringNpc() {
        if (completed || progress < progressNeeded || childObjectiveCount() > 0 || !canComplete()) {
            return false;
        }
        completed = true;
        completionHandler.accept(this);
        return true;
    }

    void checkCompletion() {
        completeIfReady();
    }

    private static int[] cleanPath(final int[] path, final int objectiveId) {
        if (path == null || path.length == 0) {
            return new int[] {objectiveId};
        }
        return path.clone();
    }

    static String pathKey(final int[] path) {
        if (path == null || path.length == 0) {
            return "";
        }
        final StringBuilder builder = new StringBuilder();
        for (final int id : path) {
            if (!builder.isEmpty()) {
                builder.append('.');
            }
            builder.append(id);
        }
        return builder.toString();
    }

    public record Update(
            boolean applied,
            double currentProgress,
            boolean unlocked,
            boolean completed,
            List<String> debugMessages,
            List<String> logMessages,
            List<String> severeMessages) {
        public Update {
            debugMessages = debugMessages == null ? List.of() : List.copyOf(debugMessages);
            logMessages = logMessages == null ? List.of() : List.copyOf(logMessages);
            severeMessages = severeMessages == null ? List.of() : List.copyOf(severeMessages);
        }

        public static Update missing() {
            return new Update(false, 0, false, false, List.of(), List.of(), List.of());
        }

        public static Update applied(
                final double currentProgress,
                final boolean unlocked,
                final boolean completed,
                final List<String> debugMessages,
                final List<String> logMessages) {
            return new Update(
                    true,
                    currentProgress,
                    unlocked,
                    completed,
                    debugMessages,
                    logMessages,
                    List.of());
        }

        public static Update severe(final String message) {
            return new Update(
                    false,
                    0,
                    false,
                    false,
                    List.of(),
                    List.of(),
                    List.of(message));
        }
    }

    public static int objectiveId(final int[] path) {
        return path == null || path.length == 0 ? 0 : path[path.length - 1];
    }

    public static String holderPath(final String questName, final int[] path) {
        if (questName == null || questName.isBlank() || path == null || path.length <= 1) {
            return questName == null ? "" : questName;
        }
        final StringBuilder builder = new StringBuilder(questName);
        for (int i = 0; i < path.length - 1; i++) {
            builder.append('.').append(path[i]);
        }
        return builder.toString();
    }

    @FunctionalInterface
    public interface ConditionGate {
        ConditionGate ALWAYS = (conditions, questPlayer, progressDecrease) -> true;

        boolean conditionsFulfilled(
                List<Condition> conditions,
                PlatformPlayer questPlayer,
                boolean progressDecrease);
    }

    @FunctionalInterface
    public interface ProgressChangeHandler {
        void handle(ActiveObjective progress, double oldProgress, double newProgress);
    }
}
