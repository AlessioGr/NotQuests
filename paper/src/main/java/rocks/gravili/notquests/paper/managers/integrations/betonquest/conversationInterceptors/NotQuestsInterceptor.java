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
    final Component parsedMessage = LegacyComponentSerializer.legacy('§').deserialize(message.replace("&", "§"));
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
    final Component parsedMessage = BungeeComponentSerializer.legacy().deserialize(message);
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

    removeOldMessages();
  }

  /** Resends the chat history without ANY conversation messages */
  public void removeOldMessages() {
    if (!main.getConfiguration().deletePreviousConversations) {
      return;
    }
    // Send back old messages
    ArrayList<Component> allChatHistory =
        main.getConversationManager().getChatHistory().get(player.getUniqueId());

    main.getLogManager().debug("Conversation stop stage 1");

    if (allChatHistory == null) {
      return;
    }

    ArrayList<Component> allConversationHistory =
        main.getConversationManager()
            .getConversationChatHistory()
            .get(player.getUniqueId());
    main.getLogManager().debug("Conversation stop stage 1.5");
    if (allConversationHistory == null) {
      return;
    }
    main.getLogManager().debug("Conversation stop stage 2");


    Component collectiveComponent = Component.text("");
    for (int i = 0; i < allChatHistory.size(); i++) {
      Component component = allChatHistory.get(i);
      if (component != null) {
        // audience.sendMessage(component.append(Component.text("fg9023zf729ofz")));
        collectiveComponent = collectiveComponent.append(component).append(Component.newline());
      }
    }
    player.sendMessage(collectiveComponent);

    allChatHistory.removeAll(allConversationHistory);
    allConversationHistory.clear();
    main.getConversationManager()
        .getChatHistory()
        .put(player.getUniqueId(), allChatHistory);
    main.getConversationManager()
        .getConversationChatHistory()
        .put(player.getUniqueId(), allConversationHistory);

    // maybe this won't send the huge, 1-component-chat-history again
    allConversationHistory.add(collectiveComponent);
  }



}
