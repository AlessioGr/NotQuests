package com.notquests.neoforge;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import com.notquests.core.platform.NQLocation;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

final class NeoForgeBeamTracker {
    private final Supplier<MinecraftServer> server;
    private final Map<UUID, Map<String, BeamLocation>> beamsByPlayer = new ConcurrentHashMap<>();

    NeoForgeBeamTracker(final Supplier<MinecraftServer> server) {
        this.server = server;
    }

    private boolean show(
            final ServerPlayer player,
            final NQLocation location,
            final String beamName,
            final boolean beaconMode) {
        if (player == null || location == null || beamName == null || beamName.isBlank()) {
            return false;
        }
        final ServerLevel level = level(server.get(), location.worldName());
        if (level == null) {
            return false;
        }
        final BeamLocation beamLocation = BeamLocation.from(location, beaconMode);
        beamsByPlayer
                .computeIfAbsent(player.getUUID(), ignored -> new ConcurrentHashMap<>())
                .put(beamName, beamLocation);
        sendShow(player, beamName, beamLocation);
        return true;
    }

    private boolean remove(final ServerPlayer player, final String beamName) {
        if (player == null || beamName == null || beamName.isBlank()) {
            return false;
        }
        final Map<String, BeamLocation> beams = beamsByPlayer.get(player.getUUID());
        if (beams == null) {
            return false;
        }
        final boolean removed = beams.remove(beamName) != null;
        if (beams.isEmpty()) {
            beamsByPlayer.remove(player.getUUID());
        }
        if (removed) {
            sendRemove(player, beamName);
        }
        return removed;
    }

    boolean render(
            final ServerPlayer player,
            final Map<String, NQLocation> markers,
            final boolean beaconMode,
            final boolean force) {
        if (player == null) {
            return false;
        }
        final Map<String, NQLocation> requested = markers == null ? Map.of() : markers;
        final Map<String, BeamLocation> rendered = beamsByPlayer
                .computeIfAbsent(player.getUUID(), ignored -> new ConcurrentHashMap<>());
        for (final String renderedName : List.copyOf(rendered.keySet())) {
            if (!requested.containsKey(renderedName)) {
                remove(player, renderedName);
            }
        }
        requested.forEach((name, location) -> {
            final BeamLocation requestedLocation = location == null
                    ? null
                    : BeamLocation.from(location, beaconMode);
            if (requestedLocation == null || level(server.get(), requestedLocation.worldName()) == null) {
                remove(player, name);
            } else if (force || !requestedLocation.equals(rendered.get(name))) {
                show(player, location, name, beaconMode);
            }
        });
        if (rendered.isEmpty()) {
            beamsByPlayer.remove(player.getUUID());
        }
        return true;
    }

    void removePlayer(final ServerPlayer player) {
        if (player == null) {
            return;
        }
        final Map<String, BeamLocation> beams = beamsByPlayer.remove(player.getUUID());
        if (beams != null) {
            beams.keySet().forEach(beamName -> sendRemove(player, beamName));
        }
    }

    void clear() {
        final MinecraftServer currentServer = server.get();
        if (currentServer != null) {
            for (final ServerPlayer player : currentServer.getPlayerList().getPlayers()) {
                final Map<String, BeamLocation> beams = beamsByPlayer.get(player.getUUID());
                if (beams != null) {
                    beams.keySet().forEach(beamName -> sendRemove(player, beamName));
                }
            }
        }
        beamsByPlayer.clear();
    }

    private void sendShow(final ServerPlayer player, final String beamName, final BeamLocation location) {
        final ServerLevel level = level(server.get(), location.worldName());
        if (level == null || player.level() != level) {
            return;
        }
        PacketDistributor.sendToPlayer(player, NeoForgeBeamPayload.show(beamName, location));
    }

    private void sendRemove(final ServerPlayer player, final String beamName) {
        PacketDistributor.sendToPlayer(player, NeoForgeBeamPayload.remove(beamName));
    }

    private static ServerLevel level(final MinecraftServer server, final String worldName) {
        if (server == null || worldName == null || worldName.isBlank()) {
            return null;
        }
        for (final ServerLevel level : server.getAllLevels()) {
            if (NeoForgeWorldNames.matches(level.dimension().identifier(), worldName)) {
                return level;
            }
        }
        return null;
    }

    record BeamLocation(String worldName, double x, double y, double z, boolean beaconMode) {
        private static BeamLocation from(final NQLocation location, final boolean beaconMode) {
            return new BeamLocation(
                    location.worldName(), location.x(), location.y(), location.z(), beaconMode);
        }
    }

}
