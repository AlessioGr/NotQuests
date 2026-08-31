package com.notquests.neoforge;

import net.minecraft.resources.Identifier;

final class NeoForgeWorldNames {
    private NeoForgeWorldNames() {}

    static String displayName(final Identifier identifier) {
        return displayName(identifier.getNamespace(), identifier.getPath());
    }

    static boolean matches(final Identifier identifier, final String input) {
        return matches(identifier.getNamespace(), identifier.getPath(), input);
    }

    static String displayName(final String namespace, final String path) {
        if ("minecraft".equals(namespace)) {
            return switch (path) {
                case "overworld" -> "world";
                case "the_nether" -> "world_nether";
                case "the_end" -> "world_the_end";
                default -> path;
            };
        }
        return namespace + ":" + path;
    }

    static boolean matches(final String namespace, final String path, final String input) {
        return input != null
                && ((namespace + ":" + path).equalsIgnoreCase(input)
                        || path.equalsIgnoreCase(input)
                        || displayName(namespace, path).equalsIgnoreCase(input));
    }
}
