package com.notquests.core.platform;

public interface NQLocation {
    static NQLocation at(final String worldName, final double x, final double y, final double z) {
        return new Location(worldName, x, y, z, 0, 0);
    }

    static NQLocation at(
            final String worldName,
            final double x,
            final double y,
            final double z,
            final float yaw,
            final float pitch) {
        return new Location(worldName, x, y, z, yaw, pitch);
    }

    String worldName();

    double x();

    double y();

    double z();

    float yaw();

    float pitch();

    default boolean sameWorld(final NQLocation other) {
        return other != null && worldName().equalsIgnoreCase(other.worldName());
    }

    default double distanceSquared(final NQLocation other) {
        if (!sameWorld(other)) {
            return Double.POSITIVE_INFINITY;
        }
        final double dx = x() - other.x();
        final double dy = y() - other.y();
        final double dz = z() - other.z();
        return dx * dx + dy * dy + dz * dz;
    }

    default String blockDescription() {
        return "X: " + x() + " Y: " + y() + " Z: " + z();
    }

    record Location(String worldName, double x, double y, double z, float yaw, float pitch)
            implements NQLocation {}
}
