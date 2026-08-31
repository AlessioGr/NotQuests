package com.notquests.neoforge.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.notquests.neoforge.NeoForgeObjectiveEvents;

/** Observes every menu input so core journal protection cannot be bypassed by a click mode. */
@Mixin(AbstractContainerMenu.class)
abstract class ContainerMenuMixin {
    @Inject(method = "clicked", at = @At("HEAD"), cancellable = true)
    private void notQuests$protectJournal(
            final int slotIndex,
            final int buttonNumber,
            final ContainerInput input,
            final Player player,
            final CallbackInfo callback) {
        if (NeoForgeObjectiveEvents.onContainerClicked(
                (AbstractContainerMenu) (Object) this,
                slotIndex,
                buttonNumber,
                input,
                player)) {
            callback.cancel();
        }
    }
}
