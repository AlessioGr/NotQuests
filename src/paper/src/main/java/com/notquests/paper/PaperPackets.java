package com.notquests.paper;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.chat.ChatTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChatMessage;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.papermc.paper.adventure.PaperAdventure;
import net.kyori.adventure.text.Component;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.notquests.core.NotQuestsPlatform.PacketBridge;

import java.util.Objects;

/** Paper's native outgoing-chat packet observation for conversation replay. */
public final class PaperPackets implements Listener, PacketBridge {
  private static final String CHANNEL_HANDLER = "notquests-packets";

  private final NotQuests main;
  private final boolean usePacketEvents;
  private boolean enabled;

  public PaperPackets(
      final NotQuests main,
      final boolean enabled,
      final boolean usePacketEvents) {
    this.main = main;
    this.usePacketEvents = usePacketEvents;
    this.enabled = enabled;
  }

  public void load() {
    if (enabled && usePacketEvents) {
      try {
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(main.getMain()));
        PacketEvents.getAPI().load();
      } catch (final Throwable exception) {
        fail(exception);
      }
    }
  }

  @Override
  public void start() {
    if (!enabled) {
      return;
    }
    if (usePacketEvents) {
      try {
        PacketEvents.getAPI().getEventManager().registerListener(
            new PacketEventsChatListener(main, this), PacketListenerPriority.LOW);
        PacketEvents.getAPI().getSettings().bStats(false).checkForUpdates(false).debug(false);
        PacketEvents.getAPI().init();
      } catch (final Throwable exception) {
        fail(exception);
      }
      return;
    }
    Bukkit.getPluginManager().registerEvents(this, main.getMain());
    Bukkit.getOnlinePlayers().forEach(this::observeChat);
  }

  @Override
  public void close() {
    if (usePacketEvents && enabled) {
      try {
        PacketEvents.getAPI().terminate();
      } catch (final Throwable exception) {
        fail(exception);
      }
    } else {
      Bukkit.getOnlinePlayers().forEach(this::stopObservingChat);
    }
    enabled = false;
  }

  @EventHandler(priority = EventPriority.HIGH)
  public void onJoin(final PlayerJoinEvent event) {
    observeChat(event.getPlayer());
  }

  @EventHandler(priority = EventPriority.HIGH)
  public void onQuit(final PlayerQuitEvent event) {
    stopObservingChat(event.getPlayer());
  }

  private void observeChat(final Player player) {
    if (!enabled || usePacketEvents) {
      return;
    }
    try {
      final Channel channel = channel(connection(serverPlayer(player).connection));
      if (channel.pipeline().get(CHANNEL_HANDLER) == null) {
        channel.pipeline().addBefore(
            "packet_handler",
            CHANNEL_HANDLER,
            new DirectChatListener(main, this, player));
      }
    } catch (final Throwable exception) {
      fail(exception);
    }
  }

  private void stopObservingChat(final Player player) {
    if (usePacketEvents) {
      return;
    }
    try {
      final Channel channel = channel(connection(serverPlayer(player).connection));
      if (channel.pipeline().get(CHANNEL_HANDLER) != null) {
        channel.pipeline().remove(CHANNEL_HANDLER);
      }
    } catch (final Throwable exception) {
      fail(exception);
    }
  }

  private void fail(final Throwable exception) {
    if (!enabled) {
      return;
    }
    enabled = false;
    main.getCorePlugin().packetMagicFailed(exception);
  }

  private static Connection connection(final ServerGamePacketListenerImpl listener) {
    return listener.connection;
  }

  private static ServerPlayer serverPlayer(final Player player) {
    return ((CraftPlayer) player).getHandle();
  }

  private static Channel channel(final Connection connection) {
    return Objects.requireNonNull(connection.channel, "Player network channel");
  }

  private static final class DirectChatListener extends ChannelDuplexHandler {
    private final NotQuests main;
    private final PaperPackets packets;
    private final Player player;

    private DirectChatListener(
        final NotQuests main,
        final PaperPackets packets,
        final Player player) {
      this.main = main;
      this.packets = packets;
      this.player = player;
    }

    @Override
    public void write(
        final ChannelHandlerContext context,
        final Object packet,
        final ChannelPromise promise) throws Exception {
      super.write(context, packet, promise);
      if (!packets.enabled
          || !(packet instanceof ClientboundSystemChatPacket chat)
          || chat.overlay()
          || chat.content() == null) {
        return;
      }
      try {
        final Component component = PaperAdventure.asAdventure(chat.content());
        main.getCorePlugin().rememberNonConversationDisplayMessage(
            player.getUniqueId().toString(), component);
      } catch (final Throwable exception) {
        packets.fail(exception);
      }
    }
  }

  private static final class PacketEventsChatListener implements PacketListener {
    private final NotQuests main;
    private final PaperPackets packets;

    private PacketEventsChatListener(final NotQuests main, final PaperPackets packets) {
      this.main = main;
      this.packets = packets;
    }

    @Override
    public void onPacketSend(final PacketSendEvent event) {
      if (!packets.enabled || event.getPacketType() != PacketType.Play.Server.CHAT_MESSAGE) {
        return;
      }
      try {
        final var message = new WrapperPlayServerChatMessage(event).getMessage();
        if (message.getType() == ChatTypes.GAME_INFO || message.getChatContent() == null) {
          return;
        }
        final Player player = (Player) event.getPlayer();
        main.getCorePlugin().rememberNonConversationDisplayMessage(
            player.getUniqueId().toString(), message.getChatContent());
      } catch (final Throwable exception) {
        packets.fail(exception);
      }
    }
  }
}
