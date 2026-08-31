package com.notquests.builtin.actions;

import com.notquests.core.NotQuestsPlugin;
import com.notquests.core.platform.NQLocation;
import com.notquests.core.platform.NotQuestsAdapter;

import java.util.concurrent.ThreadLocalRandom;

public final class SpawnMobAction {
    private static final String ENTITY_TYPE = "entityType";
    private static final String AMOUNT = "amount";
    private static final String USE_PLAYER_LOCATION = "usePlayerLocation";
    private static final String LOCATION = "location";
    private static final String SPAWN_RADIUS_X = "spawnRadiusX";
    private static final String SPAWN_RADIUS_Y = "spawnRadiusY";
    private static final String SPAWN_RADIUS_Z = "spawnRadiusZ";

    private SpawnMobAction() {}

    public static void register(
            final NotQuestsPlugin plugin,
            final NotQuestsAdapter adapter) {
        adapter.actions()
                .action("SpawnMob")
                .displayName("Spawn Mob")
                .description("Spawns vanilla, MythicMobs, or EcoMobs entities at a player or fixed location.")
                .field(
                        ENTITY_TYPE,
                        adapter.fields().entityType().config("specifics.mobToSpawn"),
                        "Entity type or custom mob id to spawn.")
                .field(
                        AMOUNT,
                        adapter.fields().integer(1).config("specifics.amount"),
                        "Number of mobs to spawn.")
                .flag(
                        USE_PLAYER_LOCATION,
                        adapter.fields().presenceFlag().config("specifics.usePlayerLocation"),
                        "Whether mobs spawn at the target player's current location.")
                .flag(
                        LOCATION,
                        adapter.fields().storedLocation().config("specifics.spawnLocation"),
                        "Fixed location where mobs should spawn.")
                .flag(
                        SPAWN_RADIUS_X,
                        adapter.fields().integer(0).config("specifics.spawnRadiusX"),
                        "Horizontal X radius used to randomize each spawn location.")
                .flag(
                        SPAWN_RADIUS_Y,
                        adapter.fields().integer(0).config("specifics.spawnRadiusY"),
                        "Vertical Y radius used to randomize each spawn location.")
                .flag(
                        SPAWN_RADIUS_Z,
                        adapter.fields().integer(0).config("specifics.spawnRadiusZ"),
                        "Horizontal Z radius used to randomize each spawn location.")
                .singleLine((action, arguments) -> {
                    action.setValue(ENTITY_TYPE, arguments.get(0));
                    action.setValue(AMOUNT, Integer.parseInt(arguments.get(1)));
                    final boolean usePlayerLocation = arguments.size() < 3
                            || arguments.get(2).equalsIgnoreCase("PlayerLocation");
                    action.setValue(USE_PLAYER_LOCATION, usePlayerLocation);
                    final int locationOffset = arguments.size() > 2 && arguments.get(2).equalsIgnoreCase("Location") ? 1 : 0;
                    if (!usePlayerLocation && arguments.size() >= locationOffset + 6) {
                        action.setValue(
                                LOCATION,
                                adapter.location(
                                        arguments.get(locationOffset + 2),
                                        Double.parseDouble(arguments.get(locationOffset + 3)),
                                        Double.parseDouble(arguments.get(locationOffset + 4)),
                                        Double.parseDouble(arguments.get(locationOffset + 5))));
                    }
                })
                .execute((action, questPlayer, objects) -> {
                    if (questPlayer == null || !questPlayer.hasPlayer()) {
                        return;
                    }
                    final NQLocation base = action.flag(USE_PLAYER_LOCATION)
                            ? NQLocation.at(
                                    questPlayer.worldName(),
                                    questPlayer.positionX(),
                                    questPlayer.positionY() + 1.0d,
                                    questPlayer.positionZ(),
                                    (float) questPlayer.yawDegrees(),
                                    (float) questPlayer.pitchDegrees())
                            : action.location(LOCATION);
                    if (base == null) {
                        return;
                    }
                    final int radiusX = Math.max(0, action.integer(SPAWN_RADIUS_X, 0));
                    final int radiusY = Math.max(0, action.integer(SPAWN_RADIUS_Y, 0));
                    final int radiusZ = Math.max(0, action.integer(SPAWN_RADIUS_Z, 0));
                    for (int index = 0; index < Math.max(1, action.integer(AMOUNT, 1)); index++) {
                        plugin.spawnMob(
                                questPlayer,
                                action.text(ENTITY_TYPE),
                                NQLocation.at(
                                        base.worldName(),
                                        base.x() + randomOffset(radiusX),
                                        base.y() + randomOffset(radiusY),
                                        base.z() + randomOffset(radiusZ),
                                        base.yaw(),
                                        base.pitch()));
                    }
                })
                .actionDescription((action, questPlayer, objects) -> "Spawns mob: " + action.text(ENTITY_TYPE))
                .register();
    }

    private static int randomOffset(final int radius) {
        return radius <= 0
                ? 0
                : ThreadLocalRandom.current().nextInt(-radius, radius + 1);
    }
}
