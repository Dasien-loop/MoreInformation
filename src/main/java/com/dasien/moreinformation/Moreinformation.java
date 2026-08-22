package com.dasien.moreinformation;

import com.mojang.logging.LogUtils;
import com.dasien.moreinformation.client.ClientBootstrap;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

@Mod(Moreinformation.MODID)
public final class Moreinformation {
    public static final String MODID = "moreinformation";
    private static final Logger LOGGER = LogUtils.getLogger();

    public Moreinformation(IEventBus modBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.CLIENT, Config.SPEC, "moreinformation-client.toml");
        modBus.addListener(Config::onLoad);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientBootstrap.register(modBus);
        }
        LOGGER.debug("MoreInformation initialized");
    }
}
