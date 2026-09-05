package com.notquests.neoforge;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundDisguisedChatPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerChatPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

import com.notquests.core.NotQuestsPlatform;
import com.notquests.core.NotQuestsPlugin;

/** NeoForge's native outgoing-chat observation used by core conversation replay. */
public final class NeoForgePackets implements NotQuestsPlatform.PacketBridge {
    private static volatile NeoForgePackets active;

    private final NotQuestsPlugin plugin;
    private final NeoForgeText text;

    NeoForgePackets(final NotQuestsPlugin plugin, final NeoForgeText text) {
        this.plugin = plugin;
        this.text = text;
    }

    @Override
    public void start() {
        active = this;
    }

    @Override
    public void close() {
        if (active == this) {
            active = null;
        }
    }

    public static void outgoing(
            final ServerGamePacketListenerImpl connection,
            final Packet<?> packet) {
        final NeoForgePackets observer = active;
        if (observer == null || connection == null || connection.player == null || packet == null) {
            return;
        }
        try {
            final Component message;
            if (packet instanceof final ClientboundSystemChatPacket systemChat) {
                if (systemChat.overlay()) {
                    return;
                }
                message = systemChat.content();
            } else if (packet instanceof final ClientboundPlayerChatPacket playerChat) {
                if (playerChat.filterMask().isFullyFiltered()) {
                    return;
                }
                final Component content = playerChat.filterMask().isEmpty()
                        ? (playerChat.unsignedContent() == null
                            ? Component.literal(playerChat.body().content())
                            : playerChat.unsignedContent())
                        : playerChat.filterMask().applyWithFormatting(playerChat.body().content());
                message = playerChat.chatType().decorate(content);
            } else if (packet instanceof final ClientboundDisguisedChatPacket disguisedChat) {
                message = disguisedChat.chatType().decorate(disguisedChat.message());
            } else {
                return;
            }
            observer.plugin.rememberNonConversationDisplayMessage(
                    connection.player.getUUID().toString(),
                    observer.text.adventure(message));
        } catch (final Throwable exception) {
            observer.close();
            observer.plugin.packetMagicFailed(exception);
        }
    }
}
