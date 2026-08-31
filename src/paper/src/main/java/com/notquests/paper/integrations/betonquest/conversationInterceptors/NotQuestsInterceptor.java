package com.notquests.paper.integrations.betonquest.conversationInterceptors;

import net.kyori.adventure.text.Component;
import org.betonquest.betonquest.api.profile.OnlineProfile;
import org.betonquest.betonquest.conversation.interceptor.Interceptor;
import org.bukkit.entity.Player;

import com.notquests.paper.NotQuests;

public class NotQuestsInterceptor implements Interceptor {
  private final NotQuests main;
  private final Player player;

  public NotQuestsInterceptor(final NotQuests main, final OnlineProfile onlineProfile) {
    this.main = main;
    this.player = onlineProfile.getPlayer();
  }

  @Override
  public void begin() {
    // No listener registration needed in BetonQuest 3's interceptor API.
  }

  @Override
  public void sendMessage(final Component message) {
    main.getCorePlugin().conversationDisplayMessage(player.getUniqueId().toString(), message);
    if (player.isOnline()) {
      main.sendMessage(player, message);
    }
  }

  @Override
  public void end() {
    // Nothing to unregister.
  }
}
