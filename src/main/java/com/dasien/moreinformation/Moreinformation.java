package com.dasien.moreinformation;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(Moreinformation.MODID)
public final class Moreinformation {
    public static final String MODID = "moreinformation";
    private static final Logger LOGGER = LogUtils.getLogger();

    public Moreinformation() {
        var modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, Config.SPEC, "moreinformation-client.toml");
        LOGGER.debug("MoreInformation initialized");
    }
}
