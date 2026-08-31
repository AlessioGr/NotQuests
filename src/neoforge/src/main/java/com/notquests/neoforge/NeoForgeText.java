package com.notquests.neoforge;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import com.notquests.core.NotQuestsPlugin;
import com.notquests.core.text.NotQuestsColors.Palette;
import com.notquests.core.text.NotQuestsMiniMessage;

import java.time.Duration;

final class NeoForgeText {
    private final NotQuestsPlugin plugin;
    private volatile MiniMessage miniMessage;
    private final GsonComponentSerializer gson = GsonComponentSerializer.gson();

    NeoForgeText(final MiniMessage miniMessage, final NotQuestsPlugin plugin) {
        this.miniMessage = miniMessage;
        this.plugin = plugin;
    }

    void usePalette(final Palette palette) {
        this.miniMessage = NotQuestsMiniMessage.create(palette);
    }

    Component component(final String miniMessageText) {
        return component(adventureComponent(miniMessageText));
    }

    Component component(final net.kyori.adventure.text.Component adventureComponent) {
        if (adventureComponent == null) {
            return Component.empty();
        }
        try {
            final JsonElement json = gson.serializeToTree(adventureComponent);
            return ComponentSerialization.CODEC
                    .parse(JsonOps.INSTANCE, json)
                    .result()
                    .orElse(Component.empty());
        } catch (final RuntimeException exception) {
            return Component.literal(adventureComponent.toString());
        }
    }

    net.kyori.adventure.text.Component adventure(final Component component) {
        if (component == null) {
            return net.kyori.adventure.text.Component.empty();
        }
        try {
            final JsonElement json = ComponentSerialization.CODEC
                    .encodeStart(JsonOps.INSTANCE, component)
                    .result()
                    .orElse(null);
            return json == null
                    ? net.kyori.adventure.text.Component.text(component.getString())
                    : gson.deserializeFromTree(json);
        } catch (final RuntimeException exception) {
            return net.kyori.adventure.text.Component.text(component.getString());
        }
    }

    net.kyori.adventure.text.Component adventureComponent(final String miniMessageText) {
        return NotQuestsMiniMessage.deserialize(miniMessage, miniMessageText);
    }

    void sendMessage(final ServerPlayer player, final String miniMessageText) {
        player.sendSystemMessage(component(miniMessageText));
    }

    void sendActionBar(final ServerPlayer player, final String miniMessageText) {
        player.connection.send(new ClientboundSetActionBarTextPacket(component(miniMessageText)));
    }

    void showTitle(
            final ServerPlayer player,
            final String title,
            final String subtitle,
            final Duration fadeIn,
            final Duration stay,
            final Duration fadeOut) {
        player.connection.send(new ClientboundSetTitlesAnimationPacket(
                ticks(fadeIn),
                ticks(stay),
                ticks(fadeOut)));
        player.connection.send(new ClientboundSetSubtitleTextPacket(component(subtitle)));
        player.connection.send(new ClientboundSetTitleTextPacket(component(title)));
    }

    void broadcast(final MinecraftServer server, final String miniMessageText) {
        final Component nativeComponent = component(miniMessageText);
        for (final ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.sendSystemMessage(nativeComponent);
        }
    }

    private static int ticks(final Duration duration) {
        if (duration == null || duration.isNegative() || duration.isZero()) {
            return 0;
        }
        final long ticks = Math.round(duration.toMillis() / 50.0d);
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0L, ticks));
    }
}
