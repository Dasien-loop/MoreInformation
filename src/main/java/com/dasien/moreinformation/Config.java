package com.dasien.moreinformation;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraft.resources.ResourceLocation;

import java.time.ZoneId;
import java.util.Locale;
import java.util.Set;

/** Client-only settings for the information HUD. */
@Mod.EventBusSubscriber(modid = Moreinformation.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.BooleanValue HUD_ENABLED;
    private static final ForgeConfigSpec.ConfigValue<String> HUD_POSITION;
    private static final ForgeConfigSpec.IntValue HUD_OFFSET_X;
    private static final ForgeConfigSpec.IntValue HUD_OFFSET_Y;
    private static final ForgeConfigSpec.BooleanValue SHOW_PERFORMANCE;
    private static final ForgeConfigSpec.BooleanValue SHOW_PLAYER;
    private static final ForgeConfigSpec.BooleanValue SHOW_TIME;
    private static final ForgeConfigSpec.BooleanValue SHOW_WORLD;
    private static final ForgeConfigSpec.BooleanValue SHOW_BIOME;
    private static final ForgeConfigSpec.BooleanValue SHOW_WEATHER;
    private static final ForgeConfigSpec.BooleanValue SHOW_ENTITIES;
    private static final ForgeConfigSpec.BooleanValue SHOW_POSITION;
    private static final ForgeConfigSpec.BooleanValue SHOW_LIGHT;
    private static final ForgeConfigSpec.ConfigValue<String> PERFORMANCE_TEMPLATE;
    private static final ForgeConfigSpec.ConfigValue<String> PLAYER_TEMPLATE;
    private static final ForgeConfigSpec.ConfigValue<String> TIME_TEMPLATE;
    private static final ForgeConfigSpec.ConfigValue<String> WORLD_TEMPLATE;
    private static final ForgeConfigSpec.ConfigValue<String> BIOME_TEMPLATE;
    private static final ForgeConfigSpec.ConfigValue<String> ENTITIES_TEMPLATE;
    private static final ForgeConfigSpec.ConfigValue<String> POSITION_TEMPLATE;
    private static final ForgeConfigSpec.ConfigValue<String> LIGHT_TEMPLATE;
    private static final ForgeConfigSpec.ConfigValue<String> CLOCK_TIME_ZONE;
    private static final ForgeConfigSpec.BooleanValue INTEGRATIONS_ENABLED;
    private static final ForgeConfigSpec.BooleanValue SHOW_ECLIPTIC_SEASONS;
    private static final ForgeConfigSpec.BooleanValue SHOW_BLOOD_MAGIC;
    private static final ForgeConfigSpec.BooleanValue SHOW_TFC;
    private static final ForgeConfigSpec.BooleanValue SHOW_L2_HOSTILITY;
    private static final ForgeConfigSpec.BooleanValue SHOW_PROJECTE;
    private static final ForgeConfigSpec.BooleanValue SHOW_NATURES_COMPASS;
    private static final ForgeConfigSpec.BooleanValue SHOW_EXPLORERS_COMPASS;
    private static final ForgeConfigSpec.BooleanValue SHOW_NATURES_AURA;
    private static final Set<String> HUD_POSITIONS = Set.of(
            "top_left", "middle_left", "bottom_left", "top_right", "middle_right", "bottom_right");

    static {
        BUILDER.comment(comment("hud")).translation("moreinformation.config.hud").push("hud");
        HUD_ENABLED = value("hud.enabled", "enabled", true);
        HUD_POSITION = BUILDER.comment(comment("hud.position")).translation("moreinformation.config.hud.position")
                .define("position", "top_left", Config::isValidHudPosition);
        HUD_OFFSET_X = BUILDER.comment(comment("hud.offsetX")).translation("moreinformation.config.hud.offsetX")
                .defineInRange("offsetX", 0, -4096, 4096);
        HUD_OFFSET_Y = BUILDER.comment(comment("hud.offsetY")).translation("moreinformation.config.hud.offsetY")
                .defineInRange("offsetY", 0, -4096, 4096);
        SHOW_PERFORMANCE = value("hud.showPerformance", "showPerformance", true);
        SHOW_PLAYER = value("hud.showPlayer", "showPlayer", true);
        CLOCK_TIME_ZONE = BUILDER.comment(comment("hud.clockTimeZone")).translation("moreinformation.config.hud.clockTimeZone")
                .define("clockTimeZone", "Asia/Shanghai", Config::isValidTimeZone);
        SHOW_TIME = value("hud.showTime", "showTime", false);
        SHOW_WORLD = value("hud.showWorld", "showWorld", false);
        SHOW_BIOME = value("hud.showBiome", "showBiome", false);
        SHOW_WEATHER = value("hud.showWeather", "showWeather", false);
        SHOW_ENTITIES = value("hud.showEntities", "showEntities", false);
        SHOW_POSITION = value("hud.showPosition", "showPosition", false);
        SHOW_LIGHT = value("hud.showLight", "showLight", false);
        BUILDER.comment(comment("hud.templates")).translation("moreinformation.config.hud.templates").push("templates");
        PERFORMANCE_TEMPLATE = text("hud.templates.performance", "performance", "moreinformation.hud.template.performance");
        PLAYER_TEMPLATE = text("hud.templates.player", "player", "moreinformation.hud.template.player");
        TIME_TEMPLATE = text("hud.templates.time", "time", "moreinformation.hud.template.time");
        WORLD_TEMPLATE = text("hud.templates.world", "world", "moreinformation.hud.template.world");
        BIOME_TEMPLATE = text("hud.templates.biome", "biome", "moreinformation.hud.template.biome_weather");
        ENTITIES_TEMPLATE = text("hud.templates.entities", "entities", "moreinformation.hud.template.entities");
        POSITION_TEMPLATE = text("hud.templates.position", "position", "moreinformation.hud.template.position");
        LIGHT_TEMPLATE = text("hud.templates.light", "light", "moreinformation.hud.template.light");
        BUILDER.pop();
        BUILDER.pop();

        BUILDER.comment(comment("integrations")).translation("moreinformation.config.integrations").push("integrations");
        INTEGRATIONS_ENABLED = value("integrations.enabled", "enabled", true);
        SHOW_ECLIPTIC_SEASONS = value("integrations.showEclipticSeasons", "showEclipticSeasons", false);
        SHOW_BLOOD_MAGIC = value("integrations.showBloodMagic", "showBloodMagic", false);
        SHOW_TFC = value("integrations.showTerraFirmaCraft", "showTerraFirmaCraft", false);
        SHOW_L2_HOSTILITY = value("integrations.showL2Hostility", "showL2Hostility", false);
        SHOW_PROJECTE = value("integrations.showProjectE", "showProjectE", false);
        SHOW_NATURES_COMPASS = value("integrations.showNaturesCompass", "showNaturesCompass", true);
        SHOW_EXPLORERS_COMPASS = value("integrations.showExplorersCompass", "showExplorersCompass", true);
        SHOW_NATURES_AURA = value("integrations.showNaturesAura", "showNaturesAura", false);
        BUILDER.pop();
    }

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    public static volatile boolean hudEnabled = true;
    public static volatile String hudPosition = "top_left";
    public static volatile int hudOffsetX = 0;
    public static volatile int hudOffsetY = 0;
    public static volatile boolean showPerformance = true;
    public static volatile boolean showPlayer = true;
    public static volatile boolean showTime = false;
    public static volatile boolean showWorld = false;
    public static volatile boolean showBiome = false;
    public static volatile boolean showWeather = false;
    public static volatile boolean showEntities = false;
    public static volatile boolean showPosition = false;
    public static volatile boolean showLight = false;
    public static volatile String performanceTemplate = ConfigLang.get("moreinformation.hud.template.performance");
    public static volatile String playerTemplate = ConfigLang.get("moreinformation.hud.template.player");
    public static volatile String timeTemplate = ConfigLang.get("moreinformation.hud.template.time");
    public static volatile String worldTemplate = ConfigLang.get("moreinformation.hud.template.world");
    public static volatile String biomeTemplate = ConfigLang.get("moreinformation.hud.template.biome_weather");
    public static volatile String entitiesTemplate = ConfigLang.get("moreinformation.hud.template.entities");
    public static volatile String positionTemplate = ConfigLang.get("moreinformation.hud.template.position");
    public static volatile String lightTemplate = ConfigLang.get("moreinformation.hud.template.light");
    public static volatile String clockTimeZone = "Asia/Shanghai";
    public static volatile boolean integrationsEnabled = true;
    public static volatile boolean showEclipticSeasons = false;
    public static volatile boolean showBloodMagic = false;
    public static volatile boolean showTerraFirmaCraft = false;
    public static volatile boolean showL2Hostility = false;
    public static volatile boolean showProjectE = false;
    public static volatile boolean showNaturesCompass = true;
    public static volatile boolean showExplorersCompass = true;
    public static volatile boolean showNaturesAura = false;

    private Config() {
    }

    @SubscribeEvent
    static void onLoad(ModConfigEvent event) {
        if (event.getConfig().getSpec() != SPEC) {
            return;
        }
        hudEnabled = HUD_ENABLED.get();
        hudPosition = HUD_POSITION.get();
        hudOffsetX = HUD_OFFSET_X.get();
        hudOffsetY = HUD_OFFSET_Y.get();
        showPerformance = SHOW_PERFORMANCE.get();
        showPlayer = SHOW_PLAYER.get();
        showTime = SHOW_TIME.get();
        showWorld = SHOW_WORLD.get();
        showBiome = SHOW_BIOME.get();
        showWeather = SHOW_WEATHER.get();
        showEntities = SHOW_ENTITIES.get();
        showPosition = SHOW_POSITION.get();
        showLight = SHOW_LIGHT.get();
        performanceTemplate = PERFORMANCE_TEMPLATE.get();
        playerTemplate = PLAYER_TEMPLATE.get();
        timeTemplate = TIME_TEMPLATE.get();
        worldTemplate = WORLD_TEMPLATE.get();
        biomeTemplate = BIOME_TEMPLATE.get();
        entitiesTemplate = ENTITIES_TEMPLATE.get();
        positionTemplate = POSITION_TEMPLATE.get();
        lightTemplate = LIGHT_TEMPLATE.get();
        clockTimeZone = CLOCK_TIME_ZONE.get();
        integrationsEnabled = INTEGRATIONS_ENABLED.get();
        showEclipticSeasons = SHOW_ECLIPTIC_SEASONS.get();
        showBloodMagic = SHOW_BLOOD_MAGIC.get();
        showTerraFirmaCraft = SHOW_TFC.get();
        showL2Hostility = SHOW_L2_HOSTILITY.get();
        showProjectE = SHOW_PROJECTE.get();
        showNaturesCompass = SHOW_NATURES_COMPASS.get();
        showExplorersCompass = SHOW_EXPLORERS_COMPASS.get();
        showNaturesAura = SHOW_NATURES_AURA.get();
    }

    public static boolean isProviderEnabled(ResourceLocation id) {
        return switch (id.toString()) {
            case "eclipticseasons:season" -> showEclipticSeasons;
            case "bloodmagic:lp" -> showBloodMagic;
            case "tfc:climate" -> showTerraFirmaCraft;
            case "l2hostility:hostility" -> showL2Hostility;
            case "projecte:emc" -> showProjectE;
            case "naturescompass:search" -> showNaturesCompass;
            case "explorerscompass:search" -> showExplorersCompass;
            case "naturesaura:aura" -> showNaturesAura;
            default -> true;
        };
    }

    private static ForgeConfigSpec.BooleanValue value(String key, String path, boolean defaultValue) {
        return BUILDER.comment(comment(key)).translation("moreinformation.config." + key).define(path, defaultValue);
    }

    private static ForgeConfigSpec.ConfigValue<String> text(String key, String path, String defaultKey) {
        return BUILDER.comment(comment(key)).translation("moreinformation.config." + key)
                .define(path, ConfigLang.get(defaultKey));
    }

    private static String comment(String key) {
        return ConfigLang.get("moreinformation.config." + key);
    }

    private static boolean isValidHudPosition(Object value) {
        return value instanceof String position
                && HUD_POSITIONS.contains(position.trim().toLowerCase(Locale.ROOT).replace('-', '_'));
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
