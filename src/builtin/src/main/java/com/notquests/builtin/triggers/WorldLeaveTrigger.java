package com.notquests.builtin.triggers;

import com.notquests.core.platform.NotQuestsAdapter;

public final class WorldLeaveTrigger {
    public static final String WORLD = "world to leave";

    private WorldLeaveTrigger() {}

    public static void register(final NotQuestsAdapter platform) {
        platform.triggers().trigger("WORLDLEAVE")
                .displayName("World Leave")
                .description("Runs the selected action after the quest player leaves a specific world.")
                .field(
                        WORLD,
                        platform.fields().worldName("ALL").config("specifics.worldToLeave"),
                        "World name that must be left, or ALL for any world.")
                .field(
                        "amount",
                        platform.fields().integer(1).config("amountNeeded"),
                        "Number of matching world-leave events required before this trigger runs.")
                .triggerDescription(trigger -> "World to leave: <WHITE>" + trigger.text(WORLD))
                .register();
    }
}
