package com.dasien.moreinformation.client;

import com.dasien.moreinformation.Config;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.effect.MobEffectInstance;

/** Shared visibility rule for the vanilla layer and optional mods' avoidance regions. */
public final class VanillaEffectsVisibility {
    private VanillaEffectsVisibility() {
    }

    public static boolean shouldHide() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft != null && minecraft.player != null && minecraft.level != null
                && shouldHide(Config.hudEnabled, Config.showEffects, minecraft.options.hideGui,
                minecraft.getDebugOverlay().showDebugScreen(), minecraft.screen instanceof AbstractContainerScreen<?>);
    }

    /** Presentation-only view: leave other threads, entities and item effects untouched. */
    public static boolean showIcon(MobEffectInstance effect, boolean original) {
        if (!original || !RenderSystem.isOnRenderThread() || !shouldHide()) return original;
        Minecraft minecraft = Minecraft.getInstance();
        // Identity matters: an equal effect on an item/other entity is not the local player's instance.
        return showIcon(original, true, minecraft.player.getEffect(effect.getEffect()) == effect);
    }

    static boolean showIcon(boolean original, boolean replacementActive, boolean localPlayerEffect) {
        return original && !(replacementActive && localPlayerEffect);
    }

    static boolean shouldHide(boolean hudEnabled, boolean showEffects, boolean hideGui,
                              boolean debugScreen, boolean containerScreen) {
        return hudEnabled && showEffects && !hideGui && !debugScreen && !containerScreen;
    }
}
