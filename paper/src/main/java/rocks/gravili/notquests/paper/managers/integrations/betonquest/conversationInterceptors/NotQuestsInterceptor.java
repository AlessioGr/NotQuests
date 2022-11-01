package rocks.gravili.notquests.paper.managers.integrations.betonquest.conversationInterceptors;

import java.util.ArrayList;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.chat.BaseComponent;
import org.betonquest.betonquest.BetonQuest;
import org.betonquest.betonquest.api.profiles.Profile;
import org.betonquest.betonquest.conversation.Conversation;
import org.betonquest.betonquest.conversation.Interceptor;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import rocks.gravili.notquests.paper.NotQuests;

public class NotQuestsInterceptor implements Interceptor, Listener {

  protected final Conversation conv;
  protected final Profile profile;

  private final NotQuests main;

  public NotQuestsInterceptor(final Conversation conv, final Profile profile) {
    this.conv = conv;
    this.profile = profile;
    Bukkit.getPluginManager().registerEvents(this, BetonQuest.getInstance());
    main = NotQuests.getInstance();
  }

  /**
   * Send message, bypassing Interceptor
   */
  @Override
  public void sendMessage(final String message) {
    final Component parsedMessage = main.parse(
        main.getMiniMessage().serialize(LegacyComponentSerializer.legacy('§').deserialize(message.replace("&", "§")))
            .replace("\\<", "<")
            .replace("</white><bold><white> </white></bold><white>", "")
    );

    if (main.getConfiguration().deletePreviousConversations && main.getConversationManager() != null) {
      final ArrayList<Component> hist =
          main.getConversationManager().getConversationChatHistory().getOrDefault(profile.getProfileUUID(), new ArrayList<>());

      hist.add(parsedMessage);
      main.getConversationManager().getConversationChatHistory().put(profile.getProfileUUID(), hist);
    }

    profile.getPlayer().ifPresent(player -> player.sendMessage(parsedMessage));
  }

  @Override
  public void sendMessage(final BaseComponent... message) {
    final String mmString = main.getMiniMessage().serialize(BungeeComponentSerializer.get().deserialize(message))
        .replace("\\<", "<")
        .replace("</white><bold><white> </white></bold><white>", "");
    final Component parsedMessage = main.parse(mmString);

    if (main.getConfiguration().deletePreviousConversations && main.getConversationManager() != null) {
      final ArrayList<Component> hist =
          main.getConversationManager().getConversationChatHistory().getOrDefault(profile.getProfileUUID(), new ArrayList<>());

      hist.add(parsedMessage);
      main.getConversationManager().getConversationChatHistory().put(profile.getProfileUUID(), hist);
    }

    profile.getPlayer().ifPresent(player -> player.sendMessage(parsedMessage));
  }


  @Override
  public void end() {
    HandlerList.unregisterAll(this);

    //main.getConversationManager().removeOldMessages();
  }
}
