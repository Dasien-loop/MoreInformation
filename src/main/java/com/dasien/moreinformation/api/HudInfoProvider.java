package com.dasien.moreinformation.api;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

/** Optional integration point for another mod's client-side information. */
@OnlyIn(Dist.CLIENT)
public interface HudInfoProvider {
    ResourceLocation id();

    default String requiredModId() {
        return id().getNamespace();
    }

    default int order() {
        return 0;
    }

    default int refreshIntervalTicks() {
        return 20;
    }

    default ItemStack icon() {
        return ItemStack.EMPTY;
    }

    List<Component> collect(HudContext context);
}
