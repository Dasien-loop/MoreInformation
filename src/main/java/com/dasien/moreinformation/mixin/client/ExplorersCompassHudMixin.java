package com.dasien.moreinformation.mixin.client;

import com.dasien.moreinformation.Config;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.chaosthedude.explorerscompass.client.ClientEventHandler", remap = false)
public abstract class ExplorersCompassHudMixin {
    @Inject(method = "onRenderTick", at = @At("HEAD"), cancellable = true, require = 0)
    private void moreinformation$hideOriginalHud(CallbackInfo ci) {
        if (Config.integrationsEnabled && Config.showExplorersCompass) {
            ci.cancel();
        }
    }
}
