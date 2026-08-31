package com.notquests.builtin.actions;

import com.notquests.core.platform.NQLocation;
import com.notquests.core.platform.NotQuestsAdapter;

public final class TeleportAction {
    private static final String LOCATION = "location";
    private static final String YAW = "yaw";
    private static final String PITCH = "pitch";

    private TeleportAction() {}

    public static void register(final NotQuestsAdapter adapter) {
        adapter.actions()
                .action("Teleport")
                .displayName("Teleport")
                .description("Teleports the target player to a fixed world location.")
                .field(
                        LOCATION,
                        adapter.fields().storedLocation().config("specifics.location"),
                        "World location where the target player should be teleported.")
                .flag(
                        YAW,
                        adapter.fields().optionalDouble().config("specifics.yaw"),
                        "Optional yaw rotation applied after teleporting.")
                .flag(
                        PITCH,
                        adapter.fields().optionalDouble().config("specifics.pitch"),
                        "Optional pitch rotation applied after teleporting.")
                .singleLine((action, arguments) -> {
                    if (arguments.size() < 4) {
                        return;
                    }
                    action.setValue(
                            LOCATION,
                            adapter.location(
                                    arguments.get(0),
                                    Double.parseDouble(arguments.get(1)),
                                    Double.parseDouble(arguments.get(2)),
                                    Double.parseDouble(arguments.get(3))));
                })
                .execute((action, questPlayer, objects) -> {
                    final NQLocation location = action.location(LOCATION);
                    if (questPlayer == null || !questPlayer.hasPlayer() || location == null) {
                        return;
                    }
                    final double yaw = action.number(YAW, Double.NaN);
                    final double pitch = action.number(PITCH, Double.NaN);
                    questPlayer.teleport(
                            location,
                            Double.isNaN(yaw) ? null : yaw,
                            Double.isNaN(pitch) ? null : pitch);
                })
                .actionDescription((action, questPlayer, objects) -> {
                    final NQLocation location = action.location(LOCATION);
                    return "Teleports player to: " + (location == null ? "unknown location" : location.blockDescription());
                })
                .register();
    }
}
