package com.notquests.core.platform;

public record LocationRegion(NQLocation min, NQLocation max) {
    public LocationRegion {
        if (min == null || max == null) {
            throw new IllegalArgumentException("Region corners cannot be null.");
        }
    }

    public NQLocation center() {
        return NQLocation.at(
                min.worldName(),
                (min.x() + max.x()) / 2.0,
                (min.y() + max.y()) / 2.0,
                (min.z() + max.z()) / 2.0);
    }

    public double enclosingRadius() {
        return Math.sqrt(center().distanceSquared(max));
    }

    public boolean contains(final NQLocation location) {
        if (location == null || !location.sameWorld(min)) {
            return false;
        }
        return location.x() >= Math.min(min.x(), max.x())
                && location.x() <= Math.max(min.x(), max.x())
                && location.y() >= Math.min(min.y(), max.y())
                && location.y() <= Math.max(min.y(), max.y())
                && location.z() >= Math.min(min.z(), max.z())
                && location.z() <= Math.max(min.z(), max.z());
    }

    public static boolean withinRadius(
            final NQLocation center,
            final double radius,
            final NQLocation location) {
        if (center == null || location == null) {
            return false;
        }
        final double safeRadius = Math.max(0, radius);
        return center.distanceSquared(location) <= safeRadius * safeRadius;
    }
}
