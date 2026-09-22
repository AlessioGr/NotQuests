package com.notquests.paper;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.papermc.paper.adventure.PaperAdventure;
import net.kyori.adventure.text.Component;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundDisguisedChatPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerChatPacket;
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
  private volatile boolean enabled;

  public PaperPackets(
      final NotQuests main,
      final boolean enabled) {
    this.main = main;
    this.enabled = enabled;
  }

  @Override
  public void start() {
    if (!enabled) {
      return;
    }
    Bukkit.getPluginManager().registerEvents(this, main.getMain());
    Bukkit.getOnlinePlayers().forEach(this::observeChat);
  }

  @Override
  public void close() {
    Bukkit.getOnlinePlayers().forEach(this::stopObservingChat);
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
    if (!enabled) {
      return;
    }
    try {
      final Channel channel = channel(connection(serverPlayer(player).connection));
      if (channel.pipeline().get(CHANNEL_HANDLER) == null) {
        channel.pipeline().addAfter(
            "encoder",
            CHANNEL_HANDLER,
            new DirectChatListener(main, this, player));
      }
    } catch (final Throwable exception) {
      fail(exception);
    }
  }

  private void stopObservingChat(final Player player) {
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
      if (!packets.enabled || !(packet instanceof ClientboundSystemChatPacket
          || packet instanceof ClientboundPlayerChatPacket
          || packet instanceof ClientboundDisguisedChatPacket)) {
        super.write(context, packet, promise);
        return;
      }
      final ChannelPromise sent = promise.unvoid();
      super.write(context, packet, sent);
      try {
        final net.minecraft.network.chat.Component message;
        if (packet instanceof final ClientboundSystemChatPacket chat) {
          if (chat.overlay()) {
            return;
          }
          message = chat.content();
        } else if (packet instanceof final ClientboundPlayerChatPacket chat) {
          if (chat.filterMask().isFullyFiltered()) {
            return;
          }
          final net.minecraft.network.chat.Component content = chat.filterMask().isEmpty()
              ? chat.unsignedContent().orElseGet(
                  () -> net.minecraft.network.chat.Component.literal(chat.body().content()))
              : chat.filterMask().applyWithFormatting(chat.body().content());
          message = chat.chatType().decorate(content);
        } else if (packet instanceof final ClientboundDisguisedChatPacket chat) {
          message = chat.chatType().decorate(chat.message());
        } else {
          return;
        }
        final Component component = PaperAdventure.asAdventure(message);
        sent.addListener(completed -> {
          if (completed.isSuccess() && packets.enabled) {
            main.getCorePlugin().rememberNonConversationDisplayMessage(
                player.getUniqueId().toString(), component);
          }
        });
      } catch (final Throwable exception) {
        packets.fail(exception);
      }
    }
  }

}
