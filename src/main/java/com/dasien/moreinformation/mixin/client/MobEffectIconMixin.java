package com.dasien.moreinformation.mixin.client;

import com.dasien.moreinformation.client.VanillaEffectsVisibility;
import net.minecraft.world.effect.MobEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Client-only icon visibility view; the underlying effect and its saved flag are never changed. */
@Mixin(MobEffectInstance.class)
public abstract class MobEffectIconMixin {
    @Shadow
    private boolean showIcon;

    // NeoForge's layer event only prevents drawing. It has no hook for other mods' showIcon()
    // queries; data packs and ATs cannot supply a dynamic, local-player-only return value.
    @Inject(method = "showIcon", at = @At("RETURN"), cancellable = true)
    private void moreinformation$viewIconVisibility(CallbackInfoReturnable<Boolean> cir) {
        boolean original = cir.getReturnValueZ();
        if (original && !VanillaEffectsVisibility.showIcon((MobEffectInstance) (Object) this, original)) {
            cir.setReturnValue(false);
        }
    }

    // Both CODEC (including save()) and STREAM_CODEC serialize through asDetails(). They must
    // use the real flag, not the presentation view, even when invoked on the render thread.
    @Redirect(method = "asDetails", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/effect/MobEffectInstance;showIcon()Z"))
    private boolean moreinformation$serializeOriginalIcon(MobEffectInstance effect) {
        return this.showIcon;
    }
}
