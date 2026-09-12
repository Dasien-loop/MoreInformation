package com.dasien.moreinformation.api;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HudInfoRegistryTest {
    @Test
    void rejectsDuplicateProviderIds() {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("test", "provider");
        HudInfoProvider provider = new TestProvider(id, 2);
        HudInfoRegistry.register(provider);
        assertThrows(IllegalArgumentException.class, () -> HudInfoRegistry.register(new TestProvider(id, 1)));
    }

    @Test
    void ordersProvidersByOrderThenId() {
        HudInfoRegistry.register(new TestProvider(ResourceLocation.fromNamespaceAndPath("test", "z"), 2));
        HudInfoRegistry.register(new TestProvider(ResourceLocation.fromNamespaceAndPath("test", "a"), 1));
        assertEquals("test:a", HudInfoRegistry.orderedProviders().get(0).id().toString());
    }

    private record TestProvider(ResourceLocation id, int order) implements HudInfoProvider {
        @Override
        public List<Component> collect(HudContext context) {
            return List.of(Component.literal(id.toString()));
        }
    }
}
