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
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@OnlyIn(Dist.CLIENT)
public final class HudManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(Moreinformation.MODID + ".HudManager");
    private static final int FAST_INTERVAL = 5;
    private static final int SLOW_INTERVAL = 20;
    private static final int START_X = 4;
    private static final int START_Y = 4;
    private static final int LINE_HEIGHT = 10;
    private static final int WHITE = 0xFFFFFF;

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
            MinecraftForge.EVENT_BUS.register(HudManager.class);
        }
    }

    public static void registerOverlay(RegisterGuiOverlaysEvent event) {
        event.registerAbove(VanillaGuiOverlay.DEBUG_TEXT.id(), "information", HudManager::render);
    }

    @SubscribeEvent
    public static void hideCompassOverlays(RenderGuiOverlayEvent.Pre event) {
        if (!Config.integrationsEnabled) return;
        ResourceLocation id = event.getOverlay().id();
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

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
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
            if (!snapshot.lines().isEmpty()) {
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
        if (Config.showTime) timeLine = hudLine(HudIcon.TIME, buildTimeLine(minecraft.level));
        if (Config.showPosition) positionLine = hudLine(HudIcon.POSITION, buildPositionLine(minecraft.player, pos));
        if (Config.showLight) lightLine = hudLine(HudIcon.LIGHT, buildLightLine(minecraft.level, pos));
        updateProviders(minecraft);
    }

    private static void updateSlow(Minecraft minecraft) {
        if (Config.showPlayer) {
            PlayerInfo info = minecraft.player.connection.getPlayerInfo(minecraft.player.getUUID());
            playerLine = hudLine(HudIcon.NETWORK, buildPlayerLine(minecraft), info == null ? -1 : info.getLatency());
        }
        if (Config.showWorld) worldLine = hudLine(HudIcon.WORLD, buildWorldLine(minecraft));
        if (Config.showEntities) {
            LevelRendererAccessor renderer = (LevelRendererAccessor) minecraft.levelRenderer;
             entityLine = hudLine(HudIcon.ENTITIES, HudTemplate.apply(template(Config.entitiesTemplate, "moreinformation.hud.template.entities"), Map.of(
                    "entities", token(String.valueOf(minecraft.level.getEntityCount()), ChatFormatting.YELLOW),
                    "rendered", token(String.valueOf(renderer.moreinformation$getRenderedEntities()), ChatFormatting.GREEN))));
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
        List<HudLine> lines = new ArrayList<>(8 + providerLines.size());
        if (Config.hudEnabled) {
            if (Config.showPerformance) lines.add(perfLine);
            if (Config.showPlayer) lines.add(playerLine);
            if (Config.showTime) lines.add(timeLine);
            if (Config.showWorld) lines.add(worldLine);
            if (Config.showBiome || Config.showWeather) lines.add(biomeLine);
            if (Config.showEntities) lines.add(entityLine);
            if (Config.showPosition) lines.add(positionLine);
            if (Config.showLight) lines.add(lightLine);
            lines.addAll(providerLines);
        }
        snapshot = new HudSnapshot(lines);
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
            return server.getAverageTickTime();
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
        return HudTemplate.apply(template(Config.timeTemplate, "moreinformation.hud.template.time"), Map.of(
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

    private static Component buildPositionLine(net.minecraft.client.player.LocalPlayer player, BlockPos pos) {
        ChunkPos chunk = new ChunkPos(pos);
        return HudTemplate.apply(template(Config.positionTemplate, "moreinformation.hud.template.position"), Map.of(
                "block", token(pos.getX() + " " + pos.getY() + " " + pos.getZ(), ChatFormatting.YELLOW),
                "chunk", token(chunk.x + " " + chunk.z, ChatFormatting.YELLOW),
                "offset", token(Math.floorMod(pos.getX(), 16) + " " + Math.floorMod(pos.getY(), 16) + " " + Math.floorMod(pos.getZ(), 16), ChatFormatting.YELLOW),
                "facing", token(Component.translatable("moreinformation.hud.direction." + player.getDirection().getName()).getString(), ChatFormatting.GREEN)));
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
        return new HudLine(Component.empty(), icon, net.minecraft.world.item.ItemStack.EMPTY, -1);
    }

    private static HudLine hudLine(HudIcon icon, Component text) {
        return new HudLine(text, icon, net.minecraft.world.item.ItemStack.EMPTY, -1);
    }

    private static HudLine hudLine(HudIcon icon, Component text, int iconLevel) {
        return new HudLine(text, icon, net.minecraft.world.item.ItemStack.EMPTY, iconLevel);
    }

    private static List<HudLine> toProviderLines(HudInfoProvider provider, List<Component> components) {
        if (components == null || components.isEmpty()) return List.of();
        net.minecraft.world.item.ItemStack icon = provider.icon();
        if (icon == null) icon = net.minecraft.world.item.ItemStack.EMPTY;
        net.minecraft.world.item.ItemStack snapshotIcon = icon.copy();
        return components.stream().map(component -> new HudLine(component, null, snapshotIcon, -1)).toList();
    }

    private static int configHash() {
        return Objects.hash(Config.hudEnabled, Config.hudPosition, Config.hudOffsetX, Config.hudOffsetY, Config.showPerformance, Config.showPlayer, Config.showTime, Config.showWorld,
                Config.showBiome, Config.showWeather, Config.showEntities, Config.showPosition, Config.showLight,
                Config.performanceTemplate, Config.playerTemplate, Config.timeTemplate, Config.worldTemplate,
                Config.biomeTemplate, Config.entitiesTemplate, Config.positionTemplate, Config.lightTemplate,
                Config.integrationsEnabled, Config.showEclipticSeasons, Config.showBloodMagic,
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

    private static void render(net.minecraftforge.client.gui.overlay.ForgeGui gui, GuiGraphics graphics,
                               float partialTick, int screenWidth, int screenHeight) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!Config.hudEnabled || minecraft.options.hideGui || minecraft.options.renderDebug || snapshot.lines().isEmpty()) {
            return;
        }
        gui.setupOverlayRenderState(false, false);
        List<HudLine> lines = snapshot.lines();
        int contentWidth = 14;
        for (HudLine line : lines) {
            contentWidth = Math.max(contentWidth, 14 + minecraft.font.width(line.text()));
        }
        int contentHeight = lines.size() * LINE_HEIGHT;
        boolean right = HudMath.hudRight(Config.hudPosition);
        int x = HudMath.placedX(HudMath.anchorX(right, screenWidth, contentWidth, START_X), Config.hudOffsetX, screenWidth, contentWidth);
        int y = HudMath.placedY(HudMath.anchorY(HudMath.hudVertical(Config.hudPosition), screenHeight, contentHeight, START_Y), Config.hudOffsetY, screenHeight, contentHeight);
        for (HudLine line : lines) {
            renderIcon(minecraft, graphics, line.icon(), line.customIcon(), line.iconLevel(), x, y);
            graphics.drawString(minecraft.font, line.text(), x + 14, y, WHITE, true);
            y += LINE_HEIGHT;
        }
    }

    private static void renderIcon(Minecraft minecraft, GuiGraphics graphics, HudIcon icon,
                                   net.minecraft.world.item.ItemStack customIcon, int level, int x, int y) {
        if (icon == null && (customIcon == null || customIcon.isEmpty())) return;
        if (icon == HudIcon.NETWORK) {
            int bar = level < 0 ? 5 : level < 150 ? 0 : level < 300 ? 1 : level < 600 ? 2 : level < 1000 ? 3 : 4;
            graphics.blit(ResourceLocation.withDefaultNamespace("textures/gui/icons.png"), x, y + 1, 0, 176 + bar * 8, 10, 8);
            return;
        }
        graphics.pose().pushPose();
        graphics.pose().translate(x, y + 1, 0.0F);
        graphics.pose().scale(0.5F, 0.5F, 1.0F);
        graphics.renderFakeItem(icon == null ? customIcon : icon.createStack(), 0, 0);
        graphics.pose().popPose();
    }
}
