package com.notquests.paper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import com.notquests.core.NotQuestsPlugin.ObjectiveCompass;
import com.notquests.core.platform.NQLocation;
import com.notquests.paper.NotQuests;
import com.notquests.paper.PaperNotQuestsAdapter;

import java.util.UUID;

class PaperPlayerBeaconTest {
    private ServerMock server;
    private WorldMock world;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        world = server.addSimpleWorld("world");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void portableLocationsKeepCoordinatesAndRotationAtThePaperBoundary() {
        final NQLocation portable = NQLocation.at("world", 10.5, 64.25, -3.75, 123.5f, -42.25f);

        final Location paper = PaperNotQuestsAdapter.paperBukkitLocation(portable);

        assertEquals(world, paper.getWorld());
        assertEquals(10.5, paper.getX());
        assertEquals(64.25, paper.getY());
        assertEquals(-3.75, paper.getZ());
        assertEquals(123.5f, paper.getYaw());
        assertEquals(-42.25f, paper.getPitch());
    }

    @Test
    void clearingQuestVisualsRemovesBeamsAndBossBars() {
        final NotQuests main = mock(NotQuests.class, RETURNS_DEEP_STUBS);
        when(main.getCorePlugin().objectiveBeamUsesBeaconBlocks()).thenReturn(false);
        final PaperPlayer questPlayer = new PaperPlayer(main, UUID.randomUUID());
        final Player player = mock(Player.class);
        when(player.getWorld()).thenReturn(world);
        when(player.getLocation()).thenReturn(new Location(world, 11, 64, 10));
        questPlayer.attach(player);
        questPlayer.renderObjectiveMarkers(
                java.util.Map.of("objective-1", NQLocation.at("world", 10, 64, 10)),
                false,
                true);
        questPlayer.showProgressBossBar(net.kyori.adventure.text.Component.text("Progress"), 0.5f);

        questPlayer.renderObjectiveMarkers(java.util.Map.of(), false, true);
        questPlayer.hideProgressBossBar();
        questPlayer.hideLocationCompass();

        verify(player).hideBossBar(any(BossBar.class));
    }

    @Test
    void sameBlockLocationComparesWorldAndBlockCoordinates() {
        final Location first = new Location(world, 1.1, 64.9, 3.2);
        final Location sameBlock = new Location(world, 1.8, 64.1, 3.9);
        final Location differentBlock = new Location(world, 2.0, 64.1, 3.9);
        final Location differentWorld = new Location(server.addSimpleWorld("other"), 1.1, 64.9, 3.2);

        assertTrue(PaperPlayer.sameBlockLocation(first, sameBlock));
        assertFalse(PaperPlayer.sameBlockLocation(first, differentBlock));
        assertFalse(PaperPlayer.sameBlockLocation(first, differentWorld));
    }

    @Test
    void unchangedBeamLocationIsResetAndResentToKeepClientFakeBlockAlive() {
        final NotQuests main = mock(NotQuests.class, RETURNS_DEEP_STUBS);
        when(main.getCorePlugin().configuration().objectiveTrackingBeamMode()).thenReturn("end_gateway");
        final PaperPlayer questPlayer = new PaperPlayer(main, UUID.randomUUID());
        final Player player = mock(Player.class);
        when(player.getWorld()).thenReturn(world);
        when(player.getLocation()).thenReturn(new Location(world, 11, 64, 10));
        world.getBlockAt(10, 64, 10).setType(Material.CHEST);
        final java.util.Map<String, NQLocation> markers =
                java.util.Map.of("objective-1", NQLocation.at("world", 10, 64, 10));
        questPlayer.attach(player);
        questPlayer.renderObjectiveMarkers(markers, false, true);
        questPlayer.renderObjectiveMarkers(markers, false, false);

        verify(player, times(3)).sendBlockChange(eq(new Location(world, 10, 63, 10)), any(BlockData.class));
    }

    @Test
    void compassHelpersPointTowardTheMarker() {
        assertEquals(0.0, ObjectiveCompass.yawTo(0, 0, 0, 10));
        assertEquals(-90.0, ObjectiveCompass.yawTo(0, 0, 10, 0));
        assertEquals(-170.0, ObjectiveCompass.wrappedDegrees(190.0));

        assertEquals("^", ObjectiveCompass.direction(0.0));
        assertEquals("<", ObjectiveCompass.direction(-45.0));
        assertEquals(">>", ObjectiveCompass.direction(120.0));
        assertEquals("behind", ObjectiveCompass.direction(179.0));
        assertEquals(1.0f, ObjectiveCompass.progress(0.0));
        assertEquals(0.0f, ObjectiveCompass.progress(180.0));
        assertEquals(BossBar.Color.GREEN, PaperPlayer.compassColor(ObjectiveCompass.Severity.GREEN));
        assertEquals(BossBar.Color.YELLOW, PaperPlayer.compassColor(ObjectiveCompass.Severity.YELLOW));
        assertEquals(BossBar.Color.RED, PaperPlayer.compassColor(ObjectiveCompass.Severity.RED));
    }
}
