package com.notquests.paper.events.hooks;

import net.kyori.adventure.text.Component;
import org.betonquest.betonquest.api.bukkit.event.ConversationOptionEvent;
import org.betonquest.betonquest.api.bukkit.event.PlayerObjectiveChangeEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import com.notquests.paper.NotQuests;

public class BetonQuestEvents implements Listener {
  private final NotQuests main;

  public BetonQuestEvents(final NotQuests main) {
    this.main = main;
  }

  @EventHandler
  public void onBetonQuestObjectiveStateChange(final PlayerObjectiveChangeEvent event) {
    main.getCorePlugin()
        .betonQuestObjectiveStateChanged(
            event.getProfile().getProfileUUID().toString(),
            event.getState().name(),
            event.getObjectiveID().getFull());
  }

  @EventHandler
  public void onConversationOption(final ConversationOptionEvent event) {
    if (event.getProfile().getOnlineProfile().isEmpty()) {
      return;
    }
    final Player player = event.getProfile().getOnlineProfile().get().getPlayer();
    final Component replay = main.getCorePlugin().conversationOptionReplay(player.getUniqueId().toString());
    if (player.isOnline() && replay != null) {
      player.sendMessage(replay);
    }
  }
}
