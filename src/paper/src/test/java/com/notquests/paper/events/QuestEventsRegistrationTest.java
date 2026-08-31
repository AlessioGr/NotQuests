package com.notquests.paper.events;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

class QuestEventsRegistrationTest {
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
        "onBlockBreakObjective",
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
