package com.dasien.moreinformation.mixin.client;

import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.world.effect.MobEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ClientboundUpdateMobEffectPacket.class)
public abstract class MobEffectPacketMixin {
    // This packet builds flags directly instead of using MobEffectInstance.asDetails(). Preserve
    // the real flag even if another client mod constructs a packet from the local player's effect.
    @Redirect(method = "<init>(ILnet/minecraft/world/effect/MobEffectInstance;Z)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/effect/MobEffectInstance;showIcon()Z"))
    private boolean moreinformation$serializeOriginalIcon(MobEffectInstance effect) {
        return ((MobEffectIconAccessor) (Object) effect).moreinformation$getOriginalShowIcon();
    }
}
