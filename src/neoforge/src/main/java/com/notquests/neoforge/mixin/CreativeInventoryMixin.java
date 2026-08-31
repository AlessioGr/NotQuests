package com.notquests.neoforge.mixin;

import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.notquests.neoforge.NeoForgeObjectiveEvents;

/** Protects the journal from creative inventory packets, which bypass normal menu clicks. */
@Mixin(ServerGamePacketListenerImpl.class)
abstract class CreativeInventoryMixin {
    @Shadow public ServerPlayer player;

    @Inject(method = "handleSetCreativeModeSlot", at = @At("HEAD"), cancellable = true)
    private void notQuests$protectJournal(
            final ServerboundSetCreativeModeSlotPacket packet,
            final CallbackInfo callback) {
        if (NeoForgeObjectiveEvents.onCreativeInventorySlot(
                player, packet.slotNum(), packet.itemStack())) {
            callback.cancel();
        }
    }
}
