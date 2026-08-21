package com.dasien.moreinformation.client;

import java.util.Locale;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** Pure formatting helpers kept separate for cheap unit testing. */
public final class HudMath {
    private static final DateTimeFormatter CLOCK_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.ROOT);

    private HudMath() {
    }

    public static String formatDuration(long seconds) {
        long safeSeconds = Math.max(0, seconds);
        return String.format(Locale.ROOT, "%02d:%02d", safeSeconds / 60L, safeSeconds % 60L);
    }

    public static String formatClock(Instant instant, String zoneId) {
        return CLOCK_FORMAT.withZone(ZoneId.of(zoneId)).format(instant);
    }

    public static long year(long day) {
        return Math.floorDiv(day, 365L) + 1L;
    }

    public static long dayOfYear(long day) {
        return Math.floorMod(day, 365L);
    }
}
