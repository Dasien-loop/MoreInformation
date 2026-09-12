package com.dasien.moreinformation.client;

import com.dasien.moreinformation.Config;
import com.dasien.moreinformation.Moreinformation;
import com.dasien.moreinformation.api.HudContext;
import com.dasien.moreinformation.api.HudInfoProvider;
import com.dasien.moreinformation.api.HudInfoRegistry;
import com.dasien.moreinformation.mixin.client.LevelRendererAccessor;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@OnlyIn(Dist.CLIENT)
public final class HudManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(Moreinformation.MODID + ".HudManager");
    private static final int FAST_INTERVAL = 5;
    private static final int SLOW_INTERVAL = 20;
    private static final int START_X = 4;
    private static final int START_Y = 4;
    private static final int LINE_HEIGHT = 10;
    private static final int EFFECT_ICON_SIZE = 16;
    private static final int EFFECT_ICON_GAP = 2;
    private static final String[] MOON_PHASES = {
            "full_moon", "waning_gibbous", "last_quarter", "waning_crescent",
            "new_moon", "waxing_crescent", "first_quarter", "waxing_gibbous"};
    private static final int WHITE = 0xFFFFFF;

    private static final ResourceLocation PING_UNKNOWN = ResourceLocation.withDefaultNamespace("icon/ping_unknown");
    private static final ResourceLocation PING_1 = ResourceLocation.withDefaultNamespace("icon/ping_1");
    private static final ResourceLocation PING_2 = ResourceLocation.withDefaultNamespace("icon/ping_2");
    private static final ResourceLocation PING_3 = ResourceLocation.withDefaultNamespace("icon/ping_3");
    private static final ResourceLocation PING_4 = ResourceLocation.withDefaultNamespace("icon/ping_4");
    private static final ResourceLocation PING_5 = ResourceLocation.withDefaultNamespace("icon/ping_5");

    private static volatile HudSnapshot snapshot = HudSnapshot.empty();
    private static boolean registered;
    private static long clientTick;
    private static Object lastWorld;
    private static BlockPos lastBiomePos;
    private static ResourceLocation lastBiomeId;
    private static boolean lastRaining;
    private static boolean lastThundering;
    private static int lastConfigHash;
    private static long nextFastTick;
    private static long nextSlowTick;
    private static long lastPerfGameTime = Long.MIN_VALUE;
    private static long lastPerfNanoTime;
    private static boolean lastShiftDown;
    private static boolean rendererAccessorMissing;
    private static HudLine perfLine = emptyLine(HudIcon.PERFORMANCE);
    private static HudLine playerLine = emptyLine(HudIcon.NETWORK);
    private static HudLine timeLine = emptyLine(HudIcon.TIME);
    private static HudLine worldLine = emptyLine(HudIcon.WORLD);
    private static HudLine biomeLine = emptyLine(HudIcon.BIOME);
    private static HudLine entityLine = emptyLine(HudIcon.ENTITIES);
    private static HudLine positionLine = emptyLine(HudIcon.POSITION);
    private static HudLine lightLine = emptyLine(HudIcon.LIGHT);
    private static List<HudLine> providerLines = List.of();
    private static final Map<ResourceLocation, Long> providerNextTicks = new HashMap<>();
    private static final Map<ResourceLocation, List<HudLine>> providerLineCache = new HashMap<>();
    private static final Map<ResourceLocation, Boolean> providerErrorLogged = new HashMap<>();

    private HudManager() {
    }

    public static void register() {
        if (!registered) {
            registered = true;
            NeoForge.EVENT_BUS.register(HudManager.class);
        }
    }

    @SubscribeEvent
    public static void hideVanillaOverlays(RenderGuiLayerEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!Config.hudEnabled || minecraft.options.hideGui || minecraft.getDebugOverlay().showDebugScreen()) return;
        // The vanilla effect list is the only place it stays visible while a container screen is open,
        // so this overlay keeps rendering there and the player never loses their potion timers.
        if (Config.showEffects && !containerScreenOpen() && isPotionIcons(event.getName())) {
            event.setCanceled(true);
            return;
        }
        if (!Config.integrationsEnabled) return;
        if (!Config.showNaturesCompass && !Config.showExplorersCompass) return;
        if (containerScreenOpen()) return;
        ResourceLocation id = event.getName();
        String namespace = id.getNamespace();
        String path = id.getPath();
        if (Config.showNaturesCompass && (namespace.equals("naturescompass") || path.contains("natures_compass"))) {
            event.setCanceled(true);
            return;
        }
        if (Config.showExplorersCompass && (namespace.equals("explorerscompass") || namespace.equals("explorerscompassedited")
                || path.contains("explorers_compass") || path.contains("explorerscompass"))) {
            event.setCanceled(true);
        }
    }

    private static boolean isPotionIcons(ResourceLocation id) {
        return id.equals(VanillaGuiLayers.EFFECTS);
    }

    private static boolean containerScreenOpen() {
        return Minecraft.getInstance().screen instanceof AbstractContainerScreen<?>;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        clientTick++;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            resetWorldState();
            snapshot = HudSnapshot.empty();
            return;
        }
        if (lastWorld != minecraft.level) {
            lastWorld = minecraft.level;
            lastBiomePos = null;
            lastBiomeId = null;
            lastRaining = false;
            lastThundering = false;
            providerNextTicks.clear();
            providerLineCache.clear();
            providerErrorLogged.clear();
            nextFastTick = 0;
            nextSlowTick = 0;
            lastPerfGameTime = Long.MIN_VALUE;
        }

        boolean changed = false;
        int configHash = configHash();
        if (configHash != lastConfigHash) {
            lastConfigHash = configHash;
            nextFastTick = 0;
            nextSlowTick = 0;
            lastBiomePos = null;
            lastBiomeId = null;
            providerNextTicks.clear();
            providerLineCache.clear();
            providerLines = List.of();
            changed = true;
        }
        if (!Config.hudEnabled) {
            if (!snapshot.isEmpty()) {
                snapshot = HudSnapshot.empty();
            }
            return;
        }
        if (clientTick >= nextFastTick) {
            updateFast(minecraft);
            nextFastTick = clientTick + FAST_INTERVAL;
            changed = true;
        }
        if (clientTick >= nextSlowTick) {
            updateSlow(minecraft);
            nextSlowTick = clientTick + SLOW_INTERVAL;
            changed = true;
        }
        changed |= updateBiomeIfNeeded(minecraft);
        boolean shiftDown = Screen.hasShiftDown();
        if (shiftDown != lastShiftDown) {
            lastShiftDown = shiftDown;
            providerNextTicks.clear();
            updateProviders(minecraft);
            changed = true;
        }
        if (changed) {
            rebuildSnapshot();
        }
    }

    private static void updateFast(Minecraft minecraft) {
        BlockPos pos = minecraft.player.blockPosition();
        if (Config.showPerformance) perfLine = hudLine(HudIcon.PERFORMANCE, buildPerformanceLine(minecraft));
        if (Config.showTime || Config.showMoon) timeLine = hudLine(HudIcon.TIME, buildTimeLine(minecraft.level));
        if (Config.showPosition) positionLine = hudLine(HudIcon.POSITION, buildPositionLine(minecraft.level, minecraft.player, pos));
        if (Config.showLight) lightLine = hudLine(HudIcon.LIGHT, buildLightLine(minecraft.level, pos));
        updateProviders(minecraft);
    }

    private static void updateSlow(Minecraft minecraft) {
        if (Config.showPlayer) {
            PlayerInfo info = minecraft.player.connection.getPlayerInfo(minecraft.player.getUUID());
            playerLine = hudLine(HudIcon.NETWORK, buildPlayerLine(minecraft), info == null ? -1 : info.getLatency());
        }
        if (Config.showWorld) worldLine = hudLine(HudIcon.WORLD, buildWorldLine(minecraft));
        if (Config.showEntities || Config.showHostile) {
            int rendered = renderedEntities(minecraft);
            String format = template(Config.entitiesTemplate, "moreinformation.hud.template.entities");
            if (format.equals("实体：{entities} 渲染：{rendered}")
                    || format.equals("Entities: {entities} Rendered: {rendered}")) {
                format = I18n.get("moreinformation.hud.template.entities");
            }
            entityLine = hudLine(HudIcon.ENTITIES, HudTemplate.apply(format, Map.of(
                    "entities", token(String.valueOf(minecraft.level.getEntityCount()), ChatFormatting.YELLOW),
                    "hostile", token(String.valueOf(hostileCount(minecraft.level)), ChatFormatting.RED),
                    "rendered", token(rendered < 0 ? "?" : String.valueOf(rendered), ChatFormatting.GREEN))));
        }
    }

    /** Degrades failed accessor calls to an unknown count; mixin startup errors are not recoverable here. */
    private static int renderedEntities(Minecraft minecraft) {
        if (rendererAccessorMissing) return -1;
        try {
            return ((LevelRendererAccessor) minecraft.levelRenderer).moreinformation$getRenderedEntities();
        } catch (Throwable error) {
            rendererAccessorMissing = true;
            LOGGER.warn("Rendered entity count is unavailable; showing '?' instead", error);
            return -1;
        }
    }

    private static boolean updateBiomeIfNeeded(Minecraft minecraft) {
        if (!Config.showBiome && !Config.showWeather) {
            return false;
        }
        BlockPos pos = minecraft.player.blockPosition();
        boolean raining = minecraft.level.isRaining();
        boolean thundering = minecraft.level.isThundering();
        boolean positionChanged = !Objects.equals(lastBiomePos, pos);
        boolean weatherChanged = raining != lastRaining || thundering != lastThundering;
        if (positionChanged || weatherChanged) {
            ResourceLocation biomeId = positionChanged
                    ? minecraft.level.getBiome(pos).unwrapKey().map(key -> key.location()).orElse(null)
                    : lastBiomeId;
            lastBiomePos = pos;
            lastBiomeId = biomeId;
            lastRaining = raining;
            lastThundering = thundering;
            biomeLine = hudLine(HudIcon.BIOME, buildBiomeLine(minecraft, pos, biomeId));
            return true;
        }
        return false;
    }

    private static void updateProviders(Minecraft minecraft) {
        if (!Config.integrationsEnabled) {
            providerLines = List.of();
            providerLineCache.clear();
            return;
        }
        List<HudLine> lines = new ArrayList<>();
        HudContext context = new HudContext(minecraft, minecraft.level, minecraft.player, clientTick);
        for (HudInfoProvider provider : HudInfoRegistry.orderedProviders()) {
            ResourceLocation id = provider.id();
            if (!Config.isProviderEnabled(id) || !ModList.get().isLoaded(provider.requiredModId())) {
                continue;
            }
            long due = providerNextTicks.getOrDefault(id, 0L);
            if (clientTick >= due) {
                try {
                    List<Component> result = provider.collect(context);
                    providerLineCache.put(id, toProviderLines(provider, result));
                    providerNextTicks.put(id, clientTick + Math.max(20, provider.refreshIntervalTicks()));
                } catch (Throwable error) {
                    if (!providerErrorLogged.getOrDefault(id, false)) {
                        LOGGER.warn("HUD provider {} failed; it will be skipped", id, error);
                        providerErrorLogged.put(id, true);
                    }
                    providerNextTicks.put(id, clientTick + SLOW_INTERVAL);
                }
            }
            lines.addAll(providerLineCache.getOrDefault(id, List.of()));
        }
        providerLines = List.copyOf(lines);
    }

    private static void rebuildSnapshot() {
        Minecraft minecraft = Minecraft.getInstance();
        List<HudLine> lines = new ArrayList<>(8 + providerLines.size());
        if (Config.hudEnabled) {
            if (Config.showPerformance) lines.add(perfLine);
            if (Config.showPlayer) lines.add(playerLine);
            if (Config.showTime || Config.showMoon) lines.add(timeLine);
            if (Config.showWorld) lines.add(worldLine);
            if (Config.showBiome || Config.showWeather) lines.add(biomeLine);
            if (Config.showEntities || Config.showHostile) lines.add(entityLine);
            if (Config.showPosition) lines.add(positionLine);
            if (Config.showLight) lines.add(lightLine);
            lines.addAll(providerLines);
        } else {
            lines.clear();
        }
        int contentWidth = HudSnapshot.ICON_WIDTH;
        for (HudLine line : lines) {
            contentWidth = Math.max(contentWidth, HudSnapshot.ICON_WIDTH + minecraft.font.width(line.text()));
        }
        snapshot = new HudSnapshot(lines, contentWidth, Config.showEffects ? buildEffectsStrip(minecraft) : EffectsStrip.EMPTY);
    }

    /** Status effects live above the text block; the strip is measured here so rendering stays cheap. */
    private static EffectsStrip buildEffectsStrip(Minecraft minecraft) {
        if (minecraft.player == null || minecraft.player.getActiveEffects().isEmpty()) {
            return EffectsStrip.EMPTY;
        }
        float itemScale = HudMath.effectIconScale(Config.effectsIconScale);
        int iconWidth = HudMath.scaledSize(EFFECT_ICON_SIZE, itemScale);
        List<EffectEntry> entries = new ArrayList<>();
        List<MobEffectInstance> sorted = new ArrayList<>(minecraft.player.getActiveEffects());
        sorted.sort(Comparator.comparingInt((MobEffectInstance effect) ->
                        effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL ? 0 : 1)
                .thenComparing(effect -> BuiltInRegistries.MOB_EFFECT.getKey(effect.getEffect().value()).toString()));
        for (MobEffectInstance effect : sorted) {
            entries.add(effectEntry(minecraft, effect));
        }
        int width = 0;
        for (EffectEntry entry : entries) {
            width += iconWidth + EFFECT_ICON_GAP + entry.textWidth() + 8;
        }
        if (!entries.isEmpty()) width -= 8;
        return new EffectsStrip(itemScale, entries, width);
    }

    private static EffectEntry effectEntry(Minecraft minecraft, MobEffectInstance effect) {
        Component text = effectLabel(effect);
        Component duration = Component.literal(HudMath.formatEffectDuration(effect.getDuration()))
                .withStyle(ChatFormatting.GRAY);
        ItemStack icon = new ItemStack(Items.POTION);
        icon.set(DataComponents.POTION_CONTENTS, new PotionContents(Optional.empty(), Optional.empty(), List.of(effect)));
        return new EffectEntry(icon, text, duration, Math.max(minecraft.font.width(text), minecraft.font.width(duration)));
    }

    private static MutableComponent effectLabel(MobEffectInstance effect) {
        MutableComponent label = Component.literal(effect.getEffect().value().getDisplayName().getString())
                .withStyle(effectDurationColor(effect));
        if (effect.getAmplifier() > 0) {
            label.append(Component.literal(" ")).append(Component.translatable(
                    "enchantment.level." + (effect.getAmplifier() + 1)));
        }
        return label;
    }

    private static ChatFormatting effectDurationColor(MobEffectInstance effect) {
        return effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL ? ChatFormatting.RED : ChatFormatting.GREEN;
    }

    /** {@code entitiesForRendering()} only exists on the client level, hence the narrower parameter. */
    private static int hostileCount(ClientLevel level) {
        int hostile = 0;
        for (Entity entity : level.entitiesForRendering()) {
            if (entity.getType().getCategory() == MobCategory.MONSTER) {
                hostile++;
            }
        }
        return hostile;
    }

    private static Component buildPerformanceLine(Minecraft minecraft) {
        float mspt = sampleMspt(minecraft);
        float tps = HudMath.tpsFromMspt(mspt);
        Runtime runtime = Runtime.getRuntime();
        long used = runtime.totalMemory() - runtime.freeMemory();
        long max = runtime.maxMemory();
        return HudTemplate.apply(template(Config.performanceTemplate, "moreinformation.hud.template.performance"), Map.of(
                "tps", token(HudMath.formatOneDecimal(tps), tpsColor(tps)),
                "mspt", token(HudMath.formatOneDecimal(mspt), msptColor(mspt)),
                "ram", token(HudMath.formatRam(used, max), ramColor(used, max))));
    }

    private static float sampleMspt(Minecraft minecraft) {
        net.minecraft.client.server.IntegratedServer server = minecraft.getSingleplayerServer();
        if (server != null) {
            return server.getAverageTickTimeNanos() / 1_000_000.0F;
        }
        long gameTime = minecraft.level.getGameTime();
        long now = System.nanoTime();
        float mspt = 50.0F;
        if (lastPerfGameTime != Long.MIN_VALUE) {
            float estimated = HudMath.estimateMspt(gameTime - lastPerfGameTime, now - lastPerfNanoTime);
            if (estimated > 0.0F) {
                mspt = estimated;
            }
        }
        lastPerfGameTime = gameTime;
        lastPerfNanoTime = now;
        return mspt;
    }

    private static ChatFormatting tpsColor(float tps) {
        if (tps >= 19.5F) return ChatFormatting.GREEN;
        if (tps >= 15.0F) return ChatFormatting.YELLOW;
        return ChatFormatting.RED;
    }

    private static ChatFormatting msptColor(float mspt) {
        if (mspt <= 50.0F) return ChatFormatting.GREEN;
        if (mspt <= 66.0F) return ChatFormatting.YELLOW;
        return ChatFormatting.RED;
    }

    private static ChatFormatting ramColor(long used, long max) {
        float percent = HudMath.percent(used, max);
        if (percent < 70.0F) return ChatFormatting.GREEN;
        if (percent < 90.0F) return ChatFormatting.YELLOW;
        return ChatFormatting.RED;
    }

    private static Component buildPlayerLine(Minecraft minecraft) {
        return HudTemplate.apply(template(Config.playerTemplate, "moreinformation.hud.template.player"), Map.of(
                "player", token(minecraft.getUser().getName(), ChatFormatting.GREEN),
                "fps", token(String.valueOf(minecraft.getFps()), ChatFormatting.YELLOW),
                "clock", token(HudMath.formatClock(Instant.now(), Config.clockTimeZone), ChatFormatting.YELLOW)));
    }

    private static Component buildTimeLine(Level level) {
        long day = Math.floorDiv(level.getDayTime(), 24000L);
        long dayTicks = Math.floorMod(level.getDayTime(), 24000L);
        long minutes = Math.floorMod(dayTicks + 6000L, 24000L) * 60L / 1000L;
        String time = String.format(Locale.ROOT, "%02d:%02d", minutes / 60L, minutes % 60L);
        String period = Component.translatable(dayTicks < 12000L ? "moreinformation.hud.daytime" : "moreinformation.hud.night").getString();
        String format = template(Config.timeTemplate, "moreinformation.hud.template.time");
        if (!format.contains("{phase}")) format += "  " + I18n.get("moreinformation.hud.template.moon");
        String phase = I18n.get("moreinformation.hud.moon_phase."
                + MOON_PHASES[Math.floorMod(level.getMoonPhase(), MOON_PHASES.length)]);
        return HudTemplate.apply(format, Map.of(
                "phase", token(phase, ChatFormatting.AQUA),
                "year", token(String.valueOf(HudMath.year(day)), ChatFormatting.YELLOW),
                "day", token(String.valueOf(HudMath.dayOfYear(day)), ChatFormatting.YELLOW),
                "time", token(time, ChatFormatting.YELLOW),
                "period", token(period, ChatFormatting.GREEN)));
    }

    private static Component buildWorldLine(Minecraft minecraft) {
        Level level = minecraft.level;
        String dimension = translated(level.dimension().location(), "dimension");
        return HudTemplate.apply(template(Config.worldTemplate, "moreinformation.hud.template.world"), Map.of(
                "world", token(dimension, ChatFormatting.AQUA),
                "chunks", token(String.valueOf(level.getChunkSource().getLoadedChunksCount()), ChatFormatting.YELLOW)));
    }

    private static Component buildBiomeLine(Minecraft minecraft, BlockPos pos, ResourceLocation biomeId) {
        Level level = minecraft.level;
        String weather = Component.translatable(level.isThundering()
                ? "moreinformation.hud.weather.thunder"
                : level.isRaining() ? "moreinformation.hud.weather.rain" : "moreinformation.hud.weather.clear").getString();
        String defaultKey = Config.showBiome && Config.showWeather
                ? "moreinformation.hud.template.biome_weather"
                : Config.showBiome ? "moreinformation.hud.template.biome" : "moreinformation.hud.template.weather";
        return HudTemplate.apply(template(Config.biomeTemplate, defaultKey), Map.of(
                "biome", token(biomeId == null ? "?" : translated(biomeId, "biome"), ChatFormatting.AQUA),
                "weather", token(weather, ChatFormatting.AQUA)));
    }

    private static Component buildPositionLine(Level level, net.minecraft.client.player.LocalPlayer player, BlockPos pos) {
        ChunkPos chunk = new ChunkPos(pos);
        String format = template(Config.positionTemplate, "moreinformation.hud.template.position");
        if (!format.contains("{portal}")) format += "{portal}";
        return HudTemplate.apply(format, Map.of(
                "block", token(pos.getX() + " " + pos.getY() + " " + pos.getZ(), ChatFormatting.YELLOW),
                "chunk", token(chunk.x + " " + chunk.z, ChatFormatting.YELLOW),
                "offset", token(Math.floorMod(pos.getX(), 16) + " " + Math.floorMod(pos.getY(), 16) + " " + Math.floorMod(pos.getZ(), 16), ChatFormatting.YELLOW),
                "portal", token(portalCoordinates(level, pos), ChatFormatting.LIGHT_PURPLE),
                "facing", token(Component.translatable("moreinformation.hud.direction." + player.getDirection().getName()).getString(), ChatFormatting.GREEN)));
    }

    /**
     * Scale X/Z between the overworld and nether, retaining Y as a height reference.
     * These are converted coordinates, not a prediction of the destination portal location.
     */
    private static String portalCoordinates(Level level, BlockPos pos) {
        if (level.dimension() == Level.OVERWORLD) {
            return portalText("nether", pos, 8);
        }
        if (level.dimension() == Level.NETHER) {
            return portalText("overworld", pos, -8);
        }
        return "";
    }

    private static String portalText(String dimensionKey, BlockPos pos, int divisor) {
        return " " + Component.translatable("moreinformation.hud.position." + dimensionKey).getString()
                + " " + HudMath.portalCoordinates(pos.getX(), pos.getY(), pos.getZ(), divisor);
    }

    private static Component buildLightLine(Level level, BlockPos pos) {
        int sky = level.getBrightness(LightLayer.SKY, pos);
        int block = level.getBrightness(LightLayer.BLOCK, pos);
        int combined = level.getChunkSource().getLightEngine().getRawBrightness(pos, 0);
        return HudTemplate.apply(template(Config.lightTemplate, "moreinformation.hud.template.light"), Map.of(
                "light", token(String.valueOf(combined), ChatFormatting.YELLOW),
                "sky", token(String.valueOf(sky), ChatFormatting.YELLOW),
                "block", token(String.valueOf(block), ChatFormatting.YELLOW)));
    }

    private static String translated(ResourceLocation id, String type) {
        String key = type + "." + id.getNamespace() + "." + id.getPath();
        return I18n.exists(key) ? Component.translatable(key).getString() : id.toString();
    }

    private static String template(String configured, String defaultKey) {
        return configured == null || configured.isBlank() ? I18n.get(defaultKey) : configured;
    }

    private static HudTemplate.Token token(String text, ChatFormatting color) {
        return new HudTemplate.Token(text, color);
    }

    private static HudLine emptyLine(HudIcon icon) {
        return new HudLine(Component.empty(), icon, ItemStack.EMPTY, -1);
    }

    private static HudLine hudLine(HudIcon icon, Component text) {
        return new HudLine(text, icon, ItemStack.EMPTY, -1);
    }

    private static HudLine hudLine(HudIcon icon, Component text, int iconLevel) {
        return new HudLine(text, icon, ItemStack.EMPTY, iconLevel);
    }

    private static List<HudLine> toProviderLines(HudInfoProvider provider, List<Component> components) {
        if (components == null || components.isEmpty()) return List.of();
        ItemStack icon = provider.icon();
        if (icon == null) icon = ItemStack.EMPTY;
        net.minecraft.world.item.ItemStack snapshotIcon = icon.copy();
        return components.stream().map(component -> new HudLine(component, null, snapshotIcon, -1)).toList();
    }

    private static int configHash() {
        return Objects.hash(Config.hudEnabled, Config.hudPosition, Config.hudOffsetX, Config.hudOffsetY, Config.hudScale, Config.showPerformance, Config.showPlayer, Config.showTime, Config.showWorld,
                Config.showBiome, Config.showWeather, Config.showEntities, Config.showPosition, Config.showLight,
                Config.showMoon, Config.showEffects, Config.showHostile, Config.effectsIconScale,
                Config.performanceTemplate, Config.playerTemplate, Config.timeTemplate, Config.worldTemplate,
                Config.biomeTemplate, Config.entitiesTemplate, Config.positionTemplate, Config.lightTemplate,
                Config.moonTemplate, Config.hostileTemplate,
                Config.integrationsEnabled, Config.showEclipticSeasons,
                Config.showTerraFirmaCraft, Config.showL2Hostility, Config.showProjectE,
                Config.showNaturesCompass, Config.showExplorersCompass, Config.showNaturesAura, Config.clockTimeZone);
    }

    private static void resetWorldState() {
        lastWorld = null;
        lastBiomePos = null;
        lastBiomeId = null;
        lastRaining = false;
        lastThundering = false;
        lastPerfGameTime = Long.MIN_VALUE;
        lastShiftDown = false;
        providerNextTicks.clear();
        providerLineCache.clear();
        providerErrorLogged.clear();
    }

    @SubscribeEvent
    public static void render(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        GuiGraphics graphics = event.getGuiGraphics();
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        if (!Config.hudEnabled || minecraft.options.hideGui || minecraft.getDebugOverlay().showDebugScreen() || snapshot.isEmpty()) {
            return;
        }
        HudSnapshot current = snapshot;
        List<HudLine> lines = current.lines();
        EffectsStrip effects = containerScreenOpen() ? EffectsStrip.EMPTY : current.effects();
        // Reserve the same bounds even with no effects or while a container hides the strip.
        int iconWidth = HudMath.scaledSize(EFFECT_ICON_SIZE, HudMath.effectIconScale(Config.effectsIconScale));
        int rowHeight = HudMath.effectRowHeight(iconWidth, LINE_HEIGHT, EFFECT_ICON_GAP);
        int minimumWidth = Config.showEffects
                ? Math.max(current.contentWidth(), iconWidth + 118) : current.contentWidth();
        float scale = HudMath.fitScale(Config.hudScale, minimumWidth,
                lines.size() * LINE_HEIGHT + (Config.showEffects ? 2 * rowHeight : 0), screenWidth, screenHeight);
        HudMath.EffectLayout effectLayout = HudMath.effectLayout(effects.entries().size(),
                (int) (screenWidth / scale), iconWidth, rowHeight);
        HudMath.EffectLayout reservedLayout = HudMath.reservedEffectLayout(Config.showEffects,
                (int) (screenWidth / scale), iconWidth, rowHeight);
        int contentWidth = Math.max(current.contentWidth(), reservedLayout.width());
        int effectsHeight = reservedLayout.height();
        int contentHeight = effectsHeight + lines.size() * LINE_HEIGHT;
        int scaledWidth = HudMath.scaledSize(contentWidth, scale);
        int scaledHeight = HudMath.scaledSize(contentHeight, scale);
        int x = HudMath.placedX(HudMath.anchorX(HudMath.hudRight(Config.hudPosition), screenWidth, scaledWidth, START_X),
                Config.hudOffsetX, screenWidth, scaledWidth);
        int y = HudMath.placedY(HudMath.anchorY(HudMath.hudVertical(Config.hudPosition), screenHeight, scaledHeight, START_Y),
                Config.hudOffsetY, screenHeight, scaledHeight);
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        if (Config.showEffects && !effects.isEmpty()) {
            renderEffectsLine(minecraft, graphics, effects, effectLayout);
        }
        int lineY = effectsHeight;
        for (HudLine line : lines) {
            renderIcon(minecraft, graphics, line.icon(), line.customIcon(), line.iconLevel(), 0, lineY);
            graphics.drawString(minecraft.font, line.text(), HudSnapshot.ICON_WIDTH, lineY, WHITE, true);
            lineY += LINE_HEIGHT;
        }
        graphics.pose().popPose();
    }

    private static int effectsLineHeight(EffectsStrip effects) {
        int iconHeight = HudMath.scaledSize(EFFECT_ICON_SIZE, effects.itemScale());
        return HudMath.effectRowHeight(iconHeight, LINE_HEIGHT, EFFECT_ICON_GAP);
    }

    /** Icons across the top of the HUD block, each one labelled with its remaining time. */
    private static void renderEffectsLine(Minecraft minecraft, GuiGraphics graphics, EffectsStrip effects, HudMath.EffectLayout layout) {
        int iconWidth = HudMath.scaledSize(EFFECT_ICON_SIZE, effects.itemScale());
        int iconHeight = HudMath.scaledSize(EFFECT_ICON_SIZE, effects.itemScale());
        int rowHeight = effectsLineHeight(effects);
        for (int index = 0; index < layout.visible(); index++) {
            EffectEntry entry = effects.entries().get(index);
            int cursorX = (index % layout.columns()) * (layout.slotWidth() + 8);
            int rowY = (index / layout.columns()) * rowHeight;
            int slotHeight = Math.max(iconHeight, 2 * LINE_HEIGHT);
            graphics.pose().pushPose();
            graphics.pose().translate(cursorX, rowY + (slotHeight - iconHeight) / 2.0F, 0.0F);
            graphics.pose().scale(effects.itemScale(), effects.itemScale(), 1.0F);
            graphics.renderFakeItem(entry.stack(), 0, 0);
            graphics.pose().popPose();
            int textX = cursorX + iconWidth + EFFECT_ICON_GAP;
            int textY = rowY + (slotHeight - 2 * LINE_HEIGHT) / 2;
            int labelWidth = Math.max(0, layout.slotWidth() - iconWidth - EFFECT_ICON_GAP);
            Component label = entry.text();
            if (minecraft.font.width(label) > labelWidth) {
                label = Component.literal(minecraft.font.plainSubstrByWidth(label.getString(),
                        Math.max(0, labelWidth - minecraft.font.width("..."))) + "...")
                        .withStyle(entry.text().getStyle());
            }
            graphics.drawString(minecraft.font, label, textX, textY, WHITE, true);
            graphics.drawString(minecraft.font, entry.duration(), textX, textY + LINE_HEIGHT, WHITE, true);
        }
        if (layout.hidden() > 0) {
            int overflowX = layout.columns() * (layout.slotWidth() + 8);
            graphics.drawString(minecraft.font, "+" + layout.hidden(), overflowX,
                    rowHeight + (rowHeight - LINE_HEIGHT) / 2, WHITE, true);
        }
    }

    private static void renderIcon(Minecraft minecraft, GuiGraphics graphics, HudIcon icon,
                                   net.minecraft.world.item.ItemStack customIcon, int level, int x, int y) {
        if (icon == null && (customIcon == null || customIcon.isEmpty())) return;
        if (icon == HudIcon.NETWORK) {
            graphics.blitSprite(pingSprite(level), x, y + 1, 10, 8);
            return;
        }
        graphics.pose().pushPose();
        graphics.pose().translate(x, y + 1, 0.0F);
        graphics.pose().scale(0.5F, 0.5F, 1.0F);
        graphics.renderFakeItem(icon == null ? customIcon : icon.createStack(), 0, 0);
        graphics.pose().popPose();
    }

    private static ResourceLocation pingSprite(int latency) {
        if (latency < 0) return PING_UNKNOWN;
        if (latency < 150) return PING_5;
        if (latency < 300) return PING_4;
        if (latency < 600) return PING_3;
        if (latency < 1000) return PING_2;
        return PING_1;
    }
}
