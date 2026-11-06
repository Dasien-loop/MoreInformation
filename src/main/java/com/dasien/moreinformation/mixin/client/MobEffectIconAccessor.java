package com.dasien.moreinformation.mixin.client;

import net.minecraft.world.effect.MobEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Raw synchronized state, used by vanilla packet serialization rather than the UI view. */
@Mixin(MobEffectInstance.class)
public interface MobEffectIconAccessor {
    @Accessor("showIcon")
    boolean moreinformation$getOriginalShowIcon();
}
