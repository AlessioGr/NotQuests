package com.notquests.neoforge.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.notquests.neoforge.NeoForgeObjectiveEvents;

@Mixin(SmithingMenu.class)
abstract class SmithingMenuMixin {
    @Inject(method = "onTake", at = @At("HEAD"))
    private void notquests$onTakeSmithingResult(
            final Player player,
            final ItemStack carried,
            final CallbackInfo callbackInfo) {
        NeoForgeObjectiveEvents.onSmithingResultTaken(player, carried);
    }
}
