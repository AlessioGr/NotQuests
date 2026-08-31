package com.notquests.core.triggers;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.IntPredicate;

public final class ActiveTrigger {
    private static final String ALL_WORLDS = "ALL";

    private final String questName;
    private final int triggerId;
    private final String triggerType;
    private final String actionName;
    private final int applyOn;
    private final String worldName;
    private final long amountNeeded;
    private final Trigger trigger;
    private long currentProgress;

    public ActiveTrigger(final String questName, final Trigger trigger) {
        this.questName = questName == null ? "" : questName;
        this.trigger = trigger;
        this.triggerId = trigger == null ? 1 : trigger.id();
        this.triggerType = trigger == null ? "" : trigger.typeId();
        this.actionName = text(trigger, "action");
        this.applyOn = integer(trigger, "applyOn", 0);
        this.worldName = text(trigger, "worldName").isBlank() ? ALL_WORLDS : text(trigger, "worldName");
        final long configuredAmount = integer(trigger, "amount", 1);
        this.amountNeeded = Math.max(1, configuredAmount);
    }

    public String questName() {
        return questName;
    }

    public int triggerId() {
        return triggerId;
    }

    public String triggerType() {
        return triggerType;
    }

    public String actionName() {
        return actionName;
    }

    public int applyOn() {
        return applyOn;
    }

    public String worldName() {
        return worldName;
    }

    public long amountNeeded() {
        return amountNeeded;
    }

    public long currentProgress() {
        return currentProgress;
    }

    public Trigger trigger() {
        return trigger;
    }

    public void setCurrentProgress(final long currentProgress) {
        this.currentProgress = Math.max(0, currentProgress);
    }

    public boolean addProgress(final long amount) {
        setCurrentProgress(currentProgress + Math.max(0, amount));
        return currentProgress >= amountNeeded;
    }

    public boolean matches(
            final Event event,
            final IntPredicate objectiveUnlocked) {
        if (event == null || !triggerType.equalsIgnoreCase(event.triggerType())) {
            return false;
        }
        if (!event.questName().isBlank() && !questName.equalsIgnoreCase(event.questName())) {
            return false;
        }
        if (!matchesWorldFlag(event.worldName())) {
            return false;
        }
        if (!matchesTriggerSpecificWorld(event.worldName())) {
            return false;
        }
        if (!matchesNpcDeath(event)) {
            return false;
        }
        if (applyOn <= 0) {
            return event.objectiveId() <= 0;
        }
        if (event.objectiveId() > 0) {
            return applyOn == event.objectiveId();
        }
        return objectiveUnlocked != null && objectiveUnlocked.test(applyOn);
    }

    private boolean matchesWorldFlag(final String eventWorldName) {
        return worldName.equalsIgnoreCase(ALL_WORLDS)
                || (!eventWorldName.isBlank() && worldName.equalsIgnoreCase(eventWorldName));
    }

    private boolean matchesTriggerSpecificWorld(final String eventWorldName) {
        if ("WORLDENTER".equalsIgnoreCase(triggerType)) {
            return matchesSpecificWorld(text(trigger, "world to enter"), eventWorldName);
        }
        if ("WORLDLEAVE".equalsIgnoreCase(triggerType)) {
            return matchesSpecificWorld(text(trigger, "world to leave"), eventWorldName);
        }
        return true;
    }

    private boolean matchesNpcDeath(final Event event) {
        if (!"NPCDEATH".equalsIgnoreCase(triggerType)) {
            return true;
        }
        final String configuredNpc = textIgnoringCase(trigger, "NPC");
        if (configuredNpc.isBlank()) {
            return true;
        }
        final String eventNpc = attributeIgnoringCase(event, "NPC");
        return !eventNpc.isBlank() && configuredNpc.equalsIgnoreCase(eventNpc);
    }

    private static String textIgnoringCase(final Trigger trigger, final String key) {
        if (trigger == null || key == null) {
            return "";
        }
        for (final Map.Entry<String, Object> value : trigger.data().values().entrySet()) {
            if (value.getKey().equalsIgnoreCase(key) && value.getValue() != null) {
                return value.getValue().toString();
            }
        }
        return "";
    }

    private static String attributeIgnoringCase(final Event event, final String key) {
        if (event == null || key == null) {
            return "";
        }
        for (final Map.Entry<String, String> attribute : event.attributes().entrySet()) {
            if (attribute.getKey().equalsIgnoreCase(key)) {
                return attribute.getValue();
            }
        }
        return "";
    }

    private static boolean matchesSpecificWorld(final String configuredWorld, final String eventWorldName) {
        if (configuredWorld == null || configuredWorld.isBlank() || configuredWorld.equalsIgnoreCase(ALL_WORLDS)) {
            return true;
        }
        return !eventWorldName.isBlank() && configuredWorld.equalsIgnoreCase(eventWorldName);
    }

    private static String text(final Trigger trigger, final String key) {
        return trigger == null ? "" : trigger.data().text(key);
    }

    private static int integer(final Trigger trigger, final String key, final int fallback) {
        if (trigger == null) {
            return fallback;
        }
        final Object value = trigger.data().value(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value != null) {
            try {
                return Integer.parseInt(value.toString());
            } catch (final NumberFormatException ignored) {
            }
        }
        return fallback;
    }

    public record Event(
            String triggerType,
            String questName,
            int objectiveId,
            String worldName,
            Map<String, String> attributes) {
        public Event {
            triggerType = triggerType == null ? "" : triggerType;
            questName = questName == null ? "" : questName;
            worldName = worldName == null ? "" : worldName;
            attributes = Map.copyOf(attributes == null ? Map.of() : attributes);
        }

        public static Event quest(
                final String triggerType,
                final String questName,
                final String worldName) {
            return new Event(triggerType, questName, 0, worldName, Map.of());
        }

        public static Event objective(
                final String triggerType,
                final String questName,
                final int objectiveId,
                final String worldName) {
            return new Event(triggerType, questName, objectiveId, worldName, Map.of());
        }

        public static Event player(
                final String triggerType,
                final String worldName) {
            return new Event(triggerType, "", 0, worldName, Map.of());
        }

        public Event withAttribute(final String key, final Object value) {
            if (key == null || key.isBlank() || value == null) {
                return this;
            }
            final LinkedHashMap<String, String> values = new LinkedHashMap<>(attributes);
            values.put(key, String.valueOf(value));
            return new Event(triggerType, questName, objectiveId, worldName, values);
        }
    }
}
