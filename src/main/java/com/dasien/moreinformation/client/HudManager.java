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
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
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
        if (changed) {
            rebuildSnapshot();
        }
    }

    private static void updateFast(Minecraft minecraft) {
        BlockPos pos = minecraft.player.blockPosition();
        if (Config.showTime) timeLine = hudLine(HudIcon.TIME, buildTimeLine(minecraft.level));
        if (Config.showPosition) positionLine = hudLine(HudIcon.POSITION, buildPositionLine(minecraft.player, pos));
        if (Config.showLight) lightLine = hudLine(HudIcon.LIGHT, buildLightLine(minecraft.level, pos));
    }

    private static void updateSlow(Minecraft minecraft) {
        if (Config.showPlayer) {
            PlayerInfo info = minecraft.player.connection.getPlayerInfo(minecraft.player.getUUID());
            playerLine = hudLine(HudIcon.NETWORK, buildPlayerLine(minecraft), info == null ? -1 : info.getLatency());
        }
        if (Config.showWorld) worldLine = hudLine(HudIcon.WORLD, buildWorldLine(minecraft));
        if (Config.showEntities) {
            LevelRendererAccessor renderer = (LevelRendererAccessor) minecraft.levelRenderer;
            entityLine = hudLine(HudIcon.ENTITIES, line("moreinformation.hud.entities")
                    .append(value(String.valueOf(minecraft.level.getEntityCount()), ChatFormatting.YELLOW))
                    .append(label("moreinformation.hud.entities.rendered"))
                    .append(value(String.valueOf(renderer.moreinformation$getRenderedEntities()), ChatFormatting.GREEN)));
        }
        updateProviders(minecraft);
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
        List<HudLine> lines = new ArrayList<>(7 + providerLines.size());
        if (Config.hudEnabled) {
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

    private static Component buildPlayerLine(Minecraft minecraft) {
        return line("moreinformation.hud.player")
                .append(value(minecraft.getUser().getName(), ChatFormatting.GREEN))
                .append(label("moreinformation.hud.fps"))
                .append(value(String.valueOf(minecraft.getFps()), ChatFormatting.YELLOW))
                .append(label("moreinformation.hud.session"))
                .append(value(HudMath.formatClock(Instant.now(), Config.clockTimeZone), ChatFormatting.YELLOW));
    }

    private static Component buildTimeLine(Level level) {
        long day = Math.floorDiv(level.getDayTime(), 24000L);
        long dayTicks = Math.floorMod(level.getDayTime(), 24000L);
        long minutes = Math.floorMod(dayTicks + 6000L, 24000L) * 60L / 1000L;
        String time = String.format(Locale.ROOT, "%02d:%02d", minutes / 60L, minutes % 60L);
        return line("moreinformation.hud.date")
                .append(value(String.valueOf(day / 365L + 1L), ChatFormatting.YELLOW))
                .append(label("moreinformation.hud.year"))
                .append(value(String.valueOf(day % 365L), ChatFormatting.YELLOW))
                .append(label("moreinformation.hud.day"))
                .append(label("moreinformation.hud.time"))
                .append(value(time, ChatFormatting.YELLOW))
                .append(value(dayTicks < 12000L ? " " + Component.translatable("moreinformation.hud.daytime").getString() : " " + Component.translatable("moreinformation.hud.night").getString(), ChatFormatting.GREEN));
    }

    private static Component buildWorldLine(Minecraft minecraft) {
        Level level = minecraft.level;
        String dimension = translated(level.dimension().location(), "dimension");
        return line("moreinformation.hud.world")
                .append(value(dimension, ChatFormatting.AQUA))
                .append(label("moreinformation.hud.chunks"))
                .append(value(String.valueOf(level.getChunkSource().getLoadedChunksCount()), ChatFormatting.YELLOW));
    }

    private static Component buildBiomeLine(Minecraft minecraft, BlockPos pos, ResourceLocation biomeId) {
        Level level = minecraft.level;
        Component weather = Component.translatable(level.isThundering()
                ? "moreinformation.hud.weather.thunder"
                : level.isRaining() ? "moreinformation.hud.weather.rain" : "moreinformation.hud.weather.clear");
        MutableComponent result = Component.empty();
        if (Config.showBiome) {
            result.append(line("moreinformation.hud.biome"))
                    .append(value(biomeId == null ? "?" : translated(biomeId, "biome"), ChatFormatting.AQUA));
        }
        if (Config.showWeather) {
            result.append(label("moreinformation.hud.weather"))
                    .append(weather.copy().withStyle(ChatFormatting.AQUA));
        }
        return result;
    }

    private static Component buildPositionLine(net.minecraft.client.player.LocalPlayer player, BlockPos pos) {
        ChunkPos chunk = new ChunkPos(pos);
        return line("moreinformation.hud.position")
                .append(label("moreinformation.hud.block"))
                .append(value(pos.getX() + " " + pos.getY() + " " + pos.getZ(), ChatFormatting.YELLOW))
                .append(label("moreinformation.hud.chunk"))
                .append(value(chunk.x + " " + chunk.z, ChatFormatting.YELLOW))
                .append(label("moreinformation.hud.offset"))
                .append(value(Math.floorMod(pos.getX(), 16) + " " + Math.floorMod(pos.getY(), 16) + " " + Math.floorMod(pos.getZ(), 16), ChatFormatting.YELLOW))
                .append(label("moreinformation.hud.facing"))
                .append(value(Component.translatable("moreinformation.hud.direction." + player.getDirection().getName()).getString(), ChatFormatting.GREEN));
    }

    private static Component buildLightLine(Level level, BlockPos pos) {
        int sky = level.getBrightness(LightLayer.SKY, pos);
        int block = level.getBrightness(LightLayer.BLOCK, pos);
        int combined = level.getChunkSource().getLightEngine().getRawBrightness(pos, 0);
        return line("moreinformation.hud.light")
                .append(value(String.valueOf(combined), ChatFormatting.YELLOW))
                .append(label("moreinformation.hud.light.details"))
                .append(label("moreinformation.hud.light.sky"))
                .append(value(String.valueOf(sky), ChatFormatting.YELLOW))
                .append(label("moreinformation.hud.light.block"))
                .append(value(String.valueOf(block), ChatFormatting.YELLOW))
                .append(label("moreinformation.hud.light.close"));
    }

    private static String translated(ResourceLocation id, String type) {
        String key = type + "." + id.getNamespace() + "." + id.getPath();
        return I18n.exists(key) ? Component.translatable(key).getString() : id.toString();
    }

    private static MutableComponent line(String key) {
        return label(key);
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

    private static MutableComponent label(String key) {
        return Component.translatable(key).withStyle(ChatFormatting.WHITE);
    }

    private static MutableComponent value(String text, ChatFormatting color) {
        return Component.literal(text).withStyle(color);
    }

    private static int configHash() {
        return Objects.hash(Config.hudEnabled, Config.showPlayer, Config.showTime, Config.showWorld,
                Config.showBiome, Config.showWeather, Config.showEntities, Config.showPosition, Config.showLight,
                Config.integrationsEnabled, Config.showEclipticSeasons, Config.showBloodMagic,
                Config.showTerraFirmaCraft, Config.showL2Hostility, Config.clockTimeZone, Config.enabledProviders);
    }

    private static void resetWorldState() {
        lastWorld = null;
        lastBiomePos = null;
        lastBiomeId = null;
        lastRaining = false;
        lastThundering = false;
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
        int y = START_Y;
        for (HudLine line : snapshot.lines()) {
            renderIcon(minecraft, graphics, line.icon(), line.customIcon(), line.iconLevel(), START_X, y);
            graphics.drawString(minecraft.font, line.text(), START_X + 14, y, WHITE, true);
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
