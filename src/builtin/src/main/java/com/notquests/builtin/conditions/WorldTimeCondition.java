package com.notquests.builtin.conditions;

import com.notquests.core.platform.NotQuestsAdapter;
import com.notquests.core.platform.PlatformPlayer;
import com.notquests.core.registry.NotQuestsRegistry.Conditions;

public final class WorldTimeCondition {
    private static final String MIN_TIME = "minTime";
    private static final String MAX_TIME = "maxTime";

    private WorldTimeCondition() {}

    public static void register(final NotQuestsAdapter platform) {
        platform.conditions().condition("WorldTime")
                .displayName("World Time")
                .description("Checks whether the player's current world time is inside a configured hour range.")
                .field(
                        MIN_TIME,
                        platform.fields().integer(0).config("specifics.minTime"),
                        "Earliest allowed hour in the Minecraft day, using 0-24 style time.")
                .field(
                        MAX_TIME,
                        platform.fields().integer(24).config("specifics.maxTime"),
                        "Latest allowed hour in the Minecraft day, using 0-24 style time.")
                .singleLine((condition, arguments) -> {
                    condition.setValue(MIN_TIME, Integer.parseInt(arguments.get(0)));
                    condition.setValue(MAX_TIME, Integer.parseInt(arguments.get(1)));
                })
                .check(WorldTimeCondition::check)
                .conditionDescription((condition, questPlayer, objects) ->
                        "<GRAY>-- World time: "
                                + condition.integer(MIN_TIME)
                                + " - "
                                + condition.integer(MAX_TIME, 24))
                .register();
    }

    private static String check(final Conditions.Data condition, final PlatformPlayer questPlayer) {
        final int minTime = condition.integer(MIN_TIME);
        final int maxTime = condition.integer(MAX_TIME, 24);
        long currentTime = questPlayer.currentWorldTimeTicks();
        currentTime = currentTime >= 18000 ? currentTime / 1000 - 18 : currentTime / 1000 + 6;

        if (maxTime >= minTime) {
            if (currentTime <= maxTime && currentTime >= minTime) {
                return "";
            }
        } else if (currentTime <= minTime) {
            if (currentTime <= maxTime) {
                return "";
            }
        } else if (currentTime >= minTime && currentTime <= 24) {
            return "";
        }
        return "<YELLOW>Come back between <highlight>"
                + minTime
                + "</highlight> and <highlight>"
                + maxTime
                + "</highlight> (It's now "
                + currentTime
                + ")";
    }
}
