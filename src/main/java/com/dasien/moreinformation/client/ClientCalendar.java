package com.dasien.moreinformation.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.UUID;

/** Client-local calendar; never changes the world's own time. */
public final class ClientCalendar {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientCalendar.class);
    private static Level level;
    private static CalendarClock clock;
    private static Path file;
    private static long lastSaveNanos;
    private static boolean warned;

    private ClientCalendar() {}

    public static void acceptTime(long rawTime) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        // Negative packet time encodes a stopped daylight cycle.
        long time = rawTime == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(rawTime);
        if (minecraft.level != level) {
            disconnect();
            level = minecraft.level;
            String world;
            if (minecraft.getSingleplayerServer() != null) {
                world = "local:" + minecraft.getSingleplayerServer().getWorldPath(LevelResource.ROOT)
                        .toAbsolutePath().normalize();
            } else if (minecraft.getCurrentServer() != null) {
                world = "server:" + minecraft.getCurrentServer().ip;
            } else {
                world = null;
            }
            if (world != null) {
                String key = world + "|" + level.dimension().location();
                file = minecraft.gameDirectory.toPath().resolve("moreinformation/calendars")
                        .resolve(UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8)) + ".txt");
            }
            try {
                clock = file == null ? new CalendarClock(time) : CalendarStore.load(file, time);
            } catch (IOException error) {
                warn(error);
                clock = new CalendarClock(time);
            }
            save();
        } else {
            long oldDay = clock.day();
            clock.accept(time);
            if (clock.day() != oldDay || System.nanoTime() - lastSaveNanos >= 60_000_000_000L) save();
        }
    }

    public static long day(Level current) {
        return current == level && clock != null ? clock.day()
                : Math.max(0L, Math.floorDiv(current.getDayTime(), 24000L));
    }

    static void disconnect() {
        save();
        level = null;
        clock = null;
        file = null;
        warned = false;
    }

    private static void save() {
        if (file == null || clock == null) return;
        try {
            CalendarStore.save(file, clock);
        } catch (IOException error) {
            warn(error);
        }
        lastSaveNanos = System.nanoTime();
    }

    private static void warn(IOException error) {
        if (!warned) {
            LOGGER.warn("Cannot load or save the local HUD calendar", error);
            warned = true;
        }
    }
}
