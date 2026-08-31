package com.notquests.builtin.triggers;

import com.notquests.core.platform.NotQuestsAdapter;

public final class WorldEnterTrigger {
    public static final String WORLD = "world to enter";

    private WorldEnterTrigger() {}

    public static void register(final NotQuestsAdapter platform) {
        platform.triggers().trigger("WORLDENTER")
                .displayName("World Enter")
                .description("Runs the selected action after the quest player enters a specific world.")
                .field(
                        WORLD,
                        platform.fields().worldName("ALL").config("specifics.worldToEnter"),
                        "World name that must be entered, or ALL for any world.")
                .field(
                        "amount",
                        platform.fields().integer(1).config("amountNeeded"),
                        "Number of matching world-enter events required before this trigger runs.")
                .triggerDescription(trigger -> "World to enter: <WHITE>" + trigger.text(WORLD))
                .register();
    }
}
