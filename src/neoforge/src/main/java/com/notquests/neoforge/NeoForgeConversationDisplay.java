package com.notquests.neoforge;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import com.notquests.core.conversation.ConversationManager;
import com.notquests.core.platform.PlatformPlayer;

import java.util.UUID;
import java.util.function.Supplier;

final class NeoForgeConversationDisplay implements ConversationManager.Display {
    private final NeoForgeText text;
    private final Supplier<MinecraftServer> server;

    NeoForgeConversationDisplay(
            final NeoForgeText text,
            final Supplier<MinecraftServer> server) {
        this.text = text;
        this.server = server;
    }

    @Override
    public void message(
            final PlatformPlayer questPlayer,
            final ConversationManager.DisplayMessage message) {
        final ServerPlayer player = player(questPlayer);
        if (player == null || message == null) {
            return;
        }
        if (message.replay() != null) {
            player.sendSystemMessage(Component.literal("\n".repeat(100)).append(text.component(message.replay())));
        }
        player.sendSystemMessage(text.component(message.component()));
    }

    private ServerPlayer player(final PlatformPlayer questPlayer) {
        final MinecraftServer minecraftServer = server.get();
        if (minecraftServer == null || questPlayer == null || !questPlayer.hasPlayer()) {
            return null;
        }
        try {
            return minecraftServer.getPlayerList().getPlayer(UUID.fromString(questPlayer.playerIdentifier()));
        } catch (final IllegalArgumentException ignored) {
            return minecraftServer.getPlayerList().getPlayerByName(questPlayer.playerName());
        }
    }
}
