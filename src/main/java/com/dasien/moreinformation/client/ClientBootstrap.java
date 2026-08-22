package com.dasien.moreinformation.client;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

public final class ClientBootstrap {
    private ClientBootstrap() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(ClientBootstrap::onClientSetup);
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            CompatProviders.register();
            HudManager.register();
        });
    }
}
