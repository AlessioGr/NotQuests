package com.notquests.paper.conversation;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.notquests.core.conversation.ConversationManager;
import com.notquests.core.platform.PlatformPlayer;

import java.util.UUID;

public final class PaperConversationDisplay implements ConversationManager.Display {
  @Override
  public void message(
      final PlatformPlayer questPlayer,
      final ConversationManager.DisplayMessage message) {
    final Player player = player(questPlayer);
    if (player == null || message == null) {
      return;
    }
    if (message.replay() != null) {
      player.sendMessage(message.replay());
    }
    player.sendMessage(message.component());
  }

  private static Player player(final PlatformPlayer questPlayer) {
    if (questPlayer == null || !questPlayer.hasPlayer()) {
      return null;
    }
    try {
      return Bukkit.getPlayer(UUID.fromString(questPlayer.playerIdentifier()));
    } catch (final IllegalArgumentException ignored) {
      return Bukkit.getPlayer(questPlayer.playerName());
    }
  }
}
