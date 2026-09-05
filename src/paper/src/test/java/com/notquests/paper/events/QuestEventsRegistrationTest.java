package com.notquests.paper.events;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.notquests.core.NotQuestsPlugin;
import com.notquests.core.structs.ActiveObjectives;
import com.notquests.paper.NotQuests;
import com.notquests.paper.PaperNotQuestsAdapter;
import com.notquests.paper.PaperPlayer;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

class QuestEventsRegistrationTest {
  @Test
  void protectedBlockBreaksDoNotProgressQuestsAndAllowedBreaksCleanUpAfterProgress() {
    final var server = MockBukkit.mock();
    try {
      final var player = server.addPlayer();
      final var block = server.addSimpleWorld("protected-mining").getBlockAt(0, 64, 0);
      block.setType(Material.STONE);
      block.getRelative(BlockFace.DOWN).setType(Material.AIR);
      final NotQuests main = mock(NotQuests.class);
      final NotQuestsPlugin core = mock(NotQuestsPlugin.class);
      final PaperNotQuestsAdapter adapter = mock(PaperNotQuestsAdapter.class);
      final PaperPlayer questPlayer = mock(PaperPlayer.class);
      when(main.getCorePlugin()).thenReturn(core);
      when(main.getRegistryAdapter()).thenReturn(adapter);
      when(adapter.activePaperPlayer(player.getUniqueId())).thenReturn(questPlayer);
      final var plugin = MockBukkit.createMockPlugin();
      final var pluginManager = server.getPluginManager();
      pluginManager.registerEvents(new QuestEvents(main), plugin);
      final AtomicBoolean protectedBlock = new AtomicBoolean(true);
      pluginManager.registerEvent(BlockBreakEvent.class, new Listener() {}, EventPriority.HIGH,
          (listener, event) -> ((BlockBreakEvent) event).setCancelled(protectedBlock.get()), plugin);

      final BlockBreakEvent denied = new BlockBreakEvent(block, player);
      pluginManager.callEvent(denied);

      assertTrue(denied.isCancelled());
      verifyNoInteractions(core);

      protectedBlock.set(false);
      final BlockBreakEvent allowed = new BlockBreakEvent(block, player);
      pluginManager.callEvent(allowed);

      assertFalse(allowed.isCancelled());
      final String blockKey = ActiveObjectives.blockKey(block.getWorld().getUID().toString(), 0, 64, 0);
      final var calls = inOrder(core);
      calls.verify(core).playerBrokeBlock(questPlayer, blockKey, "STONE", false, false, false);
      calls.verify(core).blockBreakFinished(blockKey, false);
      calls.verifyNoMoreInteractions();
    } finally {
      MockBukkit.unmock();
    }
  }

  @Test
  void identifiesOnlyDeathsAttributedToTheSamePlayer() {
    final UUID killerId = UUID.randomUUID();
    final Player killer = mock(Player.class);
    final Player samePlayer = mock(Player.class);
    final Player otherPlayer = mock(Player.class);
    when(killer.getUniqueId()).thenReturn(killerId);
    when(samePlayer.getUniqueId()).thenReturn(killerId);
    when(otherPlayer.getUniqueId()).thenReturn(UUID.randomUUID());

    assertTrue(QuestEvents.isSelfAttributedDeath(killer, samePlayer));
    assertFalse(QuestEvents.isSelfAttributedDeath(killer, otherPlayer));
    assertFalse(QuestEvents.isSelfAttributedDeath(null, samePlayer));
  }

  @Test
  void questProgressHandlersRunAtLowestAndReceiveCancelledEvents() throws Exception {
    final List<String> handlerNames = List.of(
        "onPlayerConsumeItem",
        "interactEvent",
        "playerChangeWorldEvent",
        "onBlockPlace",
        "onEntityDeath",
        "onPlayerJump",
        "onPlayerToggleSneak",
        "onPickupItem",
        "onDropItem",
        "onBrew",
        "onInventoryClick",
        "onSmithItem",
        "onPlayerMove",
        "onProjectileHit",
        "onBreedEntity",
        "onFeedEntity",
        "onTameEntity",
        "onFishItem",
        "onCraftItem",
        "onShearSheep",
        "onInteractEntity",
        "onPlayerCommand",
        "onEnchantItem");

    for (final String handlerName : handlerNames) {
      final Method handler = findHandler(handlerName);
      final EventHandler registration = handler.getAnnotation(EventHandler.class);
      assertEquals(EventPriority.LOWEST, registration.priority(), handlerName);
      assertFalse(registration.ignoreCancelled(), handlerName);
    }
  }

  private static Method findHandler(final String name) {
    for (final Method method : QuestEvents.class.getDeclaredMethods()) {
      if (method.getName().equals(name) && method.isAnnotationPresent(EventHandler.class)) {
        return method;
      }
    }
    throw new AssertionError("Missing @EventHandler method " + name);
  }
}
