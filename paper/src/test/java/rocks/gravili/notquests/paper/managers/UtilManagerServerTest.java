/*
 * NotQuests - A Questing plugin for Minecraft Servers
 * Copyright (C) 2022 Alessio Gravili
 *
 * Licensed under the GNU General Public License v3. See the LICENSE file.
 */

package rocks.gravili.notquests.paper.managers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/**
 * Exercises real {@link UtilManager} methods that depend on the Bukkit API — which is exactly why a
 * mock server (MockBukkit) is needed. {@code getOnlineUUID} resolves a player via
 * {@code Bukkit.getPlayer(name)}, and {@code isItemEmpty} inspects a real {@link ItemStack} (whose
 * creation needs the server's item factory). No NotQuests plugin is loaded — just the unit under
 * test against a mock server.
 */
class UtilManagerServerTest {

  private ServerMock server;
  private final UtilManager util = new UtilManager(null);

  @BeforeEach
  void setUp() {
    server = MockBukkit.mock();
  }

  @AfterEach
  void tearDown() {
    MockBukkit.unmock();
  }

  @Test
  @DisplayName("getOnlineUUID returns the UUID of an online player")
  void getOnlineUuidForOnlinePlayer() {
    final PlayerMock player = server.addPlayer("Steve");
    assertEquals(player.getUniqueId(), util.getOnlineUUID("Steve"));
  }

  @Test
  @DisplayName("getOnlineUUID returns null for a player who is not online")
  void getOnlineUuidForAbsentPlayer() {
    server.addPlayer("Steve");
    assertNull(util.getOnlineUUID("Notch"), "no online player named Notch -> null");
  }

  @Test
  @DisplayName("isItemEmpty treats null and AIR as empty, real items as non-empty")
  void isItemEmptyContract() {
    assertTrue(util.isItemEmpty(null), "null is empty");
    assertTrue(util.isItemEmpty(new ItemStack(Material.AIR)), "AIR is empty");
    assertFalse(util.isItemEmpty(new ItemStack(Material.STONE)), "a real item is not empty");
  }
}
