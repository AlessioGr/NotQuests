package com.notquests.neoforge.mixin;

import net.minecraft.network.protocol.Packet;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.notquests.neoforge.NeoForgePackets;

@Mixin(ServerCommonPacketListenerImpl.class)
abstract class ServerChatPacketMixin {
    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;)V", at = @At("HEAD"))
    private void notQuests$observeOutgoingChat(final Packet<?> packet, final CallbackInfo callback) {
        if ((Object) this instanceof final ServerGamePacketListenerImpl connection) {
            NeoForgePackets.outgoing(connection, packet);
        }
    }
}
