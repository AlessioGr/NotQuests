package com.notquests.neoforge;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.server.permission.PermissionAPI;
import net.neoforged.neoforge.server.permission.events.PermissionGatherEvent;
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;
import net.neoforged.neoforge.server.permission.nodes.PermissionTypes;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

final class NeoForgePermissions {
    private static Map<String, PermissionNode<Boolean>> nodes = Map.of();

    private NeoForgePermissions() {}

    static void register(final Map<String, Boolean> permissionDefaults) {
        nodes = nativeNodes(permissionDefaults);
        NeoForge.EVENT_BUS.addListener(NeoForgePermissions::registerNodes);
    }

    static boolean hasPermission(final ServerPlayer player, final String permission) {
        if (player == null || permission == null || permission.isBlank()) {
            return false;
        }
        final PermissionNode<Boolean> node = nodes.get(permission.toLowerCase(Locale.ROOT));
        return node != null && Boolean.TRUE.equals(PermissionAPI.getPermission(player, node));
    }

    private static void registerNodes(final PermissionGatherEvent.Nodes event) {
        for (final PermissionNode<Boolean> node : nodes.values()) {
            event.addNodes(node);
        }
    }

    private static Map<String, PermissionNode<Boolean>> nativeNodes(
            final Map<String, Boolean> permissionDefaults) {
        final LinkedHashMap<String, PermissionNode<Boolean>> nativeNodes = new LinkedHashMap<>();
        permissionDefaults.forEach((permission, availableToEveryone) -> {
            final String path = permission.startsWith("notquests.")
                    ? permission.substring("notquests.".length())
                    : permission;
            nativeNodes.put(permission.toLowerCase(Locale.ROOT),
                    permissionNode(path, availableToEveryone));
        });
        return Map.copyOf(nativeNodes);
    }

    private static PermissionNode<Boolean> permissionNode(
            final String path,
            final boolean availableToEveryone) {
        return new PermissionNode<>(
                NotQuestsNeoForge.MOD_ID,
                path,
                PermissionTypes.BOOLEAN,
                (player, playerId, context) -> availableToEveryone || isOperator(player));
    }

    private static boolean isOperator(final ServerPlayer player) {
        return player != null
                && player.level().getServer().getPlayerList().isOp(player.nameAndId());
    }
}
