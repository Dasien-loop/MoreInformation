package com.dasien.moreinformation.api;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Registry for optional client HUD information providers. */
@OnlyIn(Dist.CLIENT)
public final class HudInfoRegistry {
    private static final Map<ResourceLocation, HudInfoProvider> PROVIDERS = new LinkedHashMap<>();

    private HudInfoRegistry() {
    }

    public static synchronized void register(HudInfoProvider provider) {
        if (provider == null || provider.id() == null) {
            throw new IllegalArgumentException("HUD provider and id must not be null");
        }
        if (PROVIDERS.putIfAbsent(provider.id(), provider) != null) {
            throw new IllegalArgumentException("HUD provider already registered: " + provider.id());
        }
    }

    public static synchronized List<HudInfoProvider> orderedProviders() {
        List<HudInfoProvider> providers = new ArrayList<>(PROVIDERS.values());
        providers.sort(Comparator.comparingInt(HudInfoProvider::order).thenComparing(p -> p.id().toString()));
        return List.copyOf(providers);
    }
}
