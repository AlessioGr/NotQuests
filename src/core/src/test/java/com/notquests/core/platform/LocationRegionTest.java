package com.notquests.core.platform;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LocationRegionTest {
    @Test
    void containsLocationsInsideCornersRegardlessOfCornerOrder() {
        final LocationRegion region =
                new LocationRegion(new TestLocation("world", 10, 70, 10), new TestLocation("world", 0, 60, 0));

        assertTrue(region.contains(new TestLocation("world", 5, 65, 5)));
        assertFalse(region.contains(new TestLocation("world", 11, 65, 5)));
        assertFalse(region.contains(new TestLocation("nether", 5, 65, 5)));
    }

    @Test
    void checksRadiusInTheSameWorld() {
        final NQLocation center = new TestLocation("world", 0, 64, 0);

        assertTrue(LocationRegion.withinRadius(center, 5, new TestLocation("world", 3, 64, 4)));
        assertFalse(LocationRegion.withinRadius(center, 5, new TestLocation("world", 6, 64, 0)));
        assertFalse(LocationRegion.withinRadius(center, 5, new TestLocation("nether", 3, 64, 4)));
    }

    private record TestLocation(String worldName, double x, double y, double z) implements NQLocation {
        @Override public float yaw() { return 0; }

        @Override public float pitch() { return 0; }
    }
}
