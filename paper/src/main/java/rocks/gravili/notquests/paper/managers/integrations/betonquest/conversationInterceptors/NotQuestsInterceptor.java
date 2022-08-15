package rocks.gravili.notquests.paper.managers.integrations.betonquest.conversationInterceptors;

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.chat.BaseComponent;
import org.betonquest.betonquest.BetonQuest;
import org.betonquest.betonquest.conversation.Conversation;
import org.betonquest.betonquest.conversation.Interceptor;
import org.betonquest.betonquest.utils.PlayerConverter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import rocks.gravili.notquests.paper.NotQuests;

public class NotQuestsInterceptor implements Interceptor, Listener {

  protected final Conversation conv;
  protected final Player player;

  private final NotQuests main;

  public NotQuestsInterceptor(final Conversation conv, final String playerID) {
    this.conv = conv;
    this.player = PlayerConverter.getPlayer(playerID);
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

    if (main.getConfiguration().deletePreviousConversations) {
      ArrayList<Component> hist =
          main.getConversationManager().getConversationChatHistory().get(player.getUniqueId());
      if (hist == null) {
        hist = new ArrayList<>();
      }
      hist.add(parsedMessage);
      main.getConversationManager().getConversationChatHistory().put(player.getUniqueId(), hist);
    }

    player.sendMessage(parsedMessage);
  }

  @Override
  public void sendMessage(final BaseComponent... message) {
    final String mmString = main.getMiniMessage().serialize(BungeeComponentSerializer.get().deserialize(message))
        .replace("\\<", "<")
        .replace("</white><bold><white> </white></bold><white>", "");
    final Component parsedMessage = main.parse(mmString);

    if (main.getConfiguration().deletePreviousConversations) {
      ArrayList<Component> hist =
          main.getConversationManager().getConversationChatHistory().get(player.getUniqueId());
      if (hist == null) {
        hist = new ArrayList<>();
      }
      hist.add(parsedMessage);
      main.getConversationManager().getConversationChatHistory().put(player.getUniqueId(), hist);
    }

    player.sendMessage(parsedMessage);    //Arrays.stream(message).forEach(m ->  NotQuests.getInstance().sendMessage(player,  LegacyComponentSerializer.legacy('&').serialize(m)));

  }


  @Override
  public void end() {
    HandlerList.unregisterAll(this);

    //main.getConversationManager().removeOldMessages();
  }
}
