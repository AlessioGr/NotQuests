package com.notquests.builtin.actions;

import com.notquests.core.platform.NQLocation;
import com.notquests.core.platform.NotQuestsAdapter;

public final class SpawnParticleAction {
    private static final String PARTICLE = "particle";
    private static final String COUNT = "count";
    private static final String USE_PLAYER_LOCATION = "usePlayerLocation";
    private static final String SHOW_TO_EVERYONE = "forEveryone";
    private static final String LOCATION = "location";
    private static final String OFFSET_X = "offsetX";
    private static final String OFFSET_Y = "offsetY";
    private static final String OFFSET_Z = "offsetZ";
    private static final String SPEED = "speed";

    private SpawnParticleAction() {}

    public static void register(final NotQuestsAdapter adapter) {
        adapter.actions()
                .action("SpawnParticle")
                .displayName("Spawn Particle")
                .description("Spawns a particle effect at the target player or a fixed location.")
                .field(
                        PARTICLE,
                        adapter.fields().text(adapter::particleTypeIds).config("specifics.particleName"),
                        "Particle effect to spawn. Only particles that do not require extra data are supported.")
                .field(
                        COUNT,
                        adapter.fields().integer(1).config("specifics.count"),
                        "Number of particles to spawn.")
                .flag(
                        USE_PLAYER_LOCATION,
                        adapter.fields().presenceFlag().config("specifics.usePlayerLocation"),
                        "Whether particles spawn at the target player's current location.")
                .flag(
                        SHOW_TO_EVERYONE,
                        adapter.fields().presenceFlag().config("specifics.showToEveryone"),
                        "Whether every online player should see the particle effect.")
                .flag(
                        LOCATION,
                        adapter.fields().storedLocation().config("specifics.location"),
                        "Fixed world location where the particles should spawn.")
                .flag(
                        OFFSET_X,
                        adapter.fields().doubleNumber(0).config("specifics.offsetX"),
                        "Random X spread around the particle location.")
                .flag(
                        OFFSET_Y,
                        adapter.fields().doubleNumber(0).config("specifics.offsetY"),
                        "Random Y spread around the particle location.")
                .flag(
                        OFFSET_Z,
                        adapter.fields().doubleNumber(0).config("specifics.offsetZ"),
                        "Random Z spread around the particle location.")
                .flag(
                        SPEED,
                        adapter.fields().doubleNumber(0).config("specifics.speed"),
                        "Particle speed value passed to Minecraft's particle system.")
                .singleLine((action, arguments) -> {
                    if (arguments.size() < 3) {
                        return;
                    }
                    action.setValue(PARTICLE, arguments.get(0));
                    action.setValue(COUNT, Integer.parseInt(arguments.get(1)));
                    final boolean usePlayerLocation = arguments.get(2).equalsIgnoreCase("PlayerLocation");
                    action.setValue(USE_PLAYER_LOCATION, usePlayerLocation);
                    if (!usePlayerLocation && arguments.size() >= 7) {
                        action.setValue(
                                LOCATION,
                                adapter.location(
                                        arguments.get(3),
                                        Double.parseDouble(arguments.get(4)),
                                        Double.parseDouble(arguments.get(5)),
                                        Double.parseDouble(arguments.get(6))));
                    }
                })
                .execute((action, questPlayer, objects) -> {
                    if (questPlayer == null || !questPlayer.hasPlayer()) {
                        return;
                    }
                    final boolean usePlayerLocation = action.flag(USE_PLAYER_LOCATION);
                    final NQLocation location = usePlayerLocation
                            ? NQLocation.at(
                                    questPlayer.worldName(),
                                    questPlayer.positionX(),
                                    questPlayer.positionY(),
                                    questPlayer.positionZ())
                            : action.location(LOCATION);
                    if (!questPlayer.spawnParticle(
                            action.text(PARTICLE),
                            Math.max(1, action.integer(COUNT, 1)),
                            action.flag(SHOW_TO_EVERYONE),
                            location,
                            Math.max(0, action.number(OFFSET_X, 0)),
                            Math.max(0, action.number(OFFSET_Y, 0)),
                            Math.max(0, action.number(OFFSET_Z, 0)),
                            Math.max(0, action.number(SPEED, 0)))) {
                        adapter.warn("Cannot execute SpawnParticle action with particle '" + action.text(PARTICLE) + "'.");
                    }
                })
                .actionDescription((action, questPlayer, objects) -> "Spawns particle: " + action.text(PARTICLE))
                .register();
    }
}
