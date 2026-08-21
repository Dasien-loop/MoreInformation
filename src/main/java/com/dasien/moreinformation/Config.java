package com.dasien.moreinformation;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraft.resources.ResourceLocation;

import java.time.ZoneId;
import java.util.List;

/** Client-only settings for the information HUD. */
@Mod.EventBusSubscriber(modid = Moreinformation.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.BooleanValue HUD_ENABLED;
    private static final ForgeConfigSpec.BooleanValue SHOW_PLAYER;
    private static final ForgeConfigSpec.BooleanValue SHOW_TIME;
    private static final ForgeConfigSpec.BooleanValue SHOW_WORLD;
    private static final ForgeConfigSpec.BooleanValue SHOW_BIOME;
    private static final ForgeConfigSpec.BooleanValue SHOW_WEATHER;
    private static final ForgeConfigSpec.BooleanValue SHOW_ENTITIES;
    private static final ForgeConfigSpec.BooleanValue SHOW_POSITION;
    private static final ForgeConfigSpec.BooleanValue SHOW_LIGHT;
    private static final ForgeConfigSpec.ConfigValue<String> CLOCK_TIME_ZONE;
    private static final ForgeConfigSpec.BooleanValue INTEGRATIONS_ENABLED;
    private static final ForgeConfigSpec.BooleanValue SHOW_ECLIPTIC_SEASONS;
    private static final ForgeConfigSpec.BooleanValue SHOW_BLOOD_MAGIC;
    private static final ForgeConfigSpec.BooleanValue SHOW_TFC;
    private static final ForgeConfigSpec.BooleanValue SHOW_L2_HOSTILITY;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> ENABLED_PROVIDERS;

    static {
        BUILDER.comment("MoreInformation client HUD settings").push("hud");
        HUD_ENABLED = BUILDER.comment("Show the information HUD").define("enabled", true);
        SHOW_PLAYER = BUILDER.comment("Show player name, FPS and real-world clock").define("showPlayer", true);
        CLOCK_TIME_ZONE = BUILDER.comment("Time zone used by the real-world clock, for example Asia/Shanghai or UTC")
                .define("clockTimeZone", "Asia/Shanghai", Config::isValidTimeZone);
        SHOW_TIME = BUILDER.comment("Show Minecraft calendar time").define("showTime", true);
        SHOW_WORLD = BUILDER.comment("Show dimension, weather and loaded chunks").define("showWorld", true);
        SHOW_BIOME = BUILDER.comment("Show biome name").define("showBiome", true);
        SHOW_WEATHER = BUILDER.comment("Show vanilla weather").define("showWeather", true);
        SHOW_ENTITIES = BUILDER.comment("Show entity statistics").define("showEntities", true);
        SHOW_POSITION = BUILDER.comment("Show coordinates and facing").define("showPosition", true);
        SHOW_LIGHT = BUILDER.comment("Show light levels").define("showLight", true);
        BUILDER.pop();

        BUILDER.comment("Optional integrations supplied by other mods").push("integrations");
        INTEGRATIONS_ENABLED = BUILDER.comment("Allow registered third-party information providers").define("enabled", true);
        SHOW_ECLIPTIC_SEASONS = BUILDER.comment("Show Ecliptic Seasons information when installed").define("showEclipticSeasons", true);
        SHOW_BLOOD_MAGIC = BUILDER.comment("Show Blood Magic LP information when installed").define("showBloodMagic", true);
        SHOW_TFC = BUILDER.comment("Show TerraFirmaCraft climate information when installed").define("showTerraFirmaCraft", true);
        SHOW_L2_HOSTILITY = BUILDER.comment("Show L2Hostility information when installed").define("showL2Hostility", true);
        ENABLED_PROVIDERS = BUILDER.comment("Additional external provider ids to display, for example \"examplemod:mana\"")
                .defineListAllowEmpty("enabledProviders", List.of(),
                        value -> value instanceof String && !((String) value).isBlank());
        BUILDER.pop();
    }

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    public static volatile boolean hudEnabled = true;
    public static volatile boolean showPlayer = true;
    public static volatile boolean showTime = true;
    public static volatile boolean showWorld = true;
    public static volatile boolean showBiome = true;
    public static volatile boolean showWeather = true;
    public static volatile boolean showEntities = true;
    public static volatile boolean showPosition = true;
    public static volatile boolean showLight = true;
    public static volatile String clockTimeZone = "Asia/Shanghai";
    public static volatile boolean integrationsEnabled = true;
    public static volatile boolean showEclipticSeasons = true;
    public static volatile boolean showBloodMagic = true;
    public static volatile boolean showTerraFirmaCraft = true;
    public static volatile boolean showL2Hostility = true;
    public static volatile List<String> enabledProviders = List.of();

    private Config() {
    }

    @SubscribeEvent
    static void onLoad(ModConfigEvent event) {
        if (event.getConfig().getSpec() != SPEC) {
            return;
        }
        hudEnabled = HUD_ENABLED.get();
        showPlayer = SHOW_PLAYER.get();
        showTime = SHOW_TIME.get();
        showWorld = SHOW_WORLD.get();
        showBiome = SHOW_BIOME.get();
        showWeather = SHOW_WEATHER.get();
        showEntities = SHOW_ENTITIES.get();
        showPosition = SHOW_POSITION.get();
        showLight = SHOW_LIGHT.get();
        clockTimeZone = CLOCK_TIME_ZONE.get();
        integrationsEnabled = INTEGRATIONS_ENABLED.get();
        showEclipticSeasons = SHOW_ECLIPTIC_SEASONS.get();
        showBloodMagic = SHOW_BLOOD_MAGIC.get();
        showTerraFirmaCraft = SHOW_TFC.get();
        showL2Hostility = SHOW_L2_HOSTILITY.get();
        enabledProviders = List.copyOf(ENABLED_PROVIDERS.get());
    }

    public static boolean isProviderEnabled(ResourceLocation id) {
        return switch (id.toString()) {
            case "eclipticseasons:season" -> showEclipticSeasons;
            case "bloodmagic:lp" -> showBloodMagic;
            case "tfc:climate" -> showTerraFirmaCraft;
            case "l2hostility:hostility" -> showL2Hostility;
            default -> enabledProviders.contains(id.toString());
        };
    }

    private static boolean isValidTimeZone(Object value) {
        if (!(value instanceof String zoneId)) return false;
        try {
            ZoneId.of(zoneId);
            return true;
        } catch (RuntimeException ignored) {
            return false;
        }
    }
}
