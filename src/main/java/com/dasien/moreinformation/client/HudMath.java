package com.dasien.moreinformation.client;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Locale;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** Pure formatting helpers kept separate for cheap unit testing. */
public final class HudMath {
    private static final long DAYS_PER_YEAR = 365L;

    private static final DateTimeFormatter CLOCK_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.ROOT);
    private static final String[] BIG_UNITS = {
            "", "K", "M", "B", "T", "Qa", "Qi", "Sx", "Sp", "Oc", "No", "Dc"
    };
    private static final BigInteger COMPACT_THRESHOLD = BigInteger.valueOf(10_000L);

    private HudMath() {
    }

    public static String formatDuration(long seconds) {
        long safeSeconds = Math.max(0, seconds);
        return String.format(Locale.ROOT, "%02d:%02d", safeSeconds / 60L, safeSeconds % 60L);
    }

    /** Formats a status effect duration; a negative tick count means the effect never expires. */
    public static String formatEffectDuration(int ticks) {
        return ticks < 0 ? "∞" : formatDuration(ticks / 20L);
    }

    /** Status effect icons follow the HUD scale but keep their own multiplier. */
    public static float effectIconScale(double configured) {
        if (!(configured > 0.0D)) {
            return 1.0F;
        }
        return (float) Math.max(0.5D, Math.min(3.0D, configured));
    }

    /** Scales one coordinate the way a nether portal does: 8 divides, -8 multiplies. */
    public static int portalCoordinate(int coordinate, int factor) {
        return factor < 0 ? coordinate * -factor : Math.floorDiv(coordinate, factor);
    }

    public static String portalCoordinates(int x, int y, int z, int factor) {
        return portalCoordinate(x, factor) + " " + y + " " + portalCoordinate(z, factor);
    }

    public static String formatClock(Instant instant, String zoneId) {
        return CLOCK_FORMAT.withZone(ZoneId.of(zoneId)).format(instant);
    }

    /** Maps a zero-based elapsed Minecraft day to the displayed zero-based year. */
    public static long year(long day) {
        return Math.floorDiv(day, DAYS_PER_YEAR);
    }

    /** Maps a zero-based elapsed Minecraft day to the displayed zero-based day of year. */
    public static long dayOfYear(long day) {
        return Math.floorMod(day, DAYS_PER_YEAR);
    }

    public static String formatEmc(BigInteger value, boolean compact) {
        if (value == null) {
            return "0";
        }
        if (!compact || value.abs().compareTo(COMPACT_THRESHOLD) < 0) {
            return groupDigits(value.toString());
        }
        boolean negative = value.signum() < 0;
        BigDecimal amount = new BigDecimal(value.abs());
        int unit = 0;
        BigDecimal thousand = BigDecimal.valueOf(1000L);
        while (unit < BIG_UNITS.length - 1 && amount.compareTo(thousand) >= 0) {
            amount = amount.divide(thousand, 8, RoundingMode.DOWN);
            unit++;
        }
        amount = amount.setScale(amount.stripTrailingZeros().scale() <= 0 ? 0 : 2, RoundingMode.HALF_UP);
        if (amount.compareTo(thousand) >= 0 && unit < BIG_UNITS.length - 1) {
            amount = amount.divide(thousand, 2, RoundingMode.HALF_UP);
            unit++;
        }
        String number = amount.stripTrailingZeros().toPlainString();
        return (negative ? "-" : "") + number + BIG_UNITS[unit];
    }

    static String groupDigits(String digits) {
        if (digits == null || digits.isEmpty()) {
            return "0";
        }
        boolean negative = digits.charAt(0) == '-';
        String raw = negative ? digits.substring(1) : digits;
        if (raw.isEmpty()) {
            return "0";
        }
        StringBuilder grouped = new StringBuilder();
        int count = 0;
        for (int i = raw.length() - 1; i >= 0; i--) {
            if (count > 0 && count % 3 == 0) {
                grouped.append(',');
            }
            grouped.append(raw.charAt(i));
            count++;
        }
        if (negative) {
            grouped.append('-');
        }
        return grouped.reverse().toString();
    }

    public static float tpsFromMspt(float mspt) {
        if (!(mspt > 0.0F)) {
            return 20.0F;
        }
        return Math.min(20.0F, 1000.0F / mspt);
    }

    public static float estimateMspt(long tickDelta, long nanoDelta) {
        if (tickDelta <= 0L || nanoDelta <= 0L) {
            return 0.0F;
        }
        return (float) (nanoDelta / 1_000_000.0D / tickDelta);
    }

    public static String formatOneDecimal(float value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    public static long bytesToMegabytes(long bytes) {
        return Math.max(0L, bytes) / (1024L * 1024L);
    }

    public static String formatRam(long usedBytes, long maxBytes) {
        return bytesToMegabytes(usedBytes) + "/" + bytesToMegabytes(maxBytes) + "MB";
    }

    public static float percent(long used, long max) {
        if (max <= 0L) {
            return 0.0F;
        }
        return used * 100.0F / max;
    }

    public static boolean hudRight(String position) {
        return normalizeAnchor(position).contains("right");
    }

    public static String hudVertical(String position) {
        String normalized = normalizeAnchor(position);
        if (normalized.contains("middle") || normalized.contains("center")) {
            return "middle";
        }
        if (normalized.contains("bottom")) {
            return "bottom";
        }
        return "top";
    }

    public static int anchorX(boolean right, int screenWidth, int contentWidth, int padding) {
        return right ? Math.max(padding, screenWidth - padding - contentWidth) : padding;
    }

    public static int anchorY(String vertical, int screenHeight, int contentHeight, int padding) {
        if ("middle".equals(vertical)) {
            return Math.max(padding, (screenHeight - contentHeight) / 2);
        }
        if ("bottom".equals(vertical)) {
            return Math.max(padding, screenHeight - padding - contentHeight);
        }
        return padding;
    }

    public static float hudScale(double scale) {
        if (!Double.isFinite(scale) || scale <= 0.0D) {
            return 1.0F;
        }
        return (float) Math.max(0.25D, Math.min(4.0D, scale));
    }

    public static int scaledSize(int size, float scale) {
        if (scale <= 0.0F) return Math.max(0, size);
        return Math.max(0, Math.round(size * scale));
    }

    public static int placedX(int baseX, int offsetX, int screenWidth, int contentWidth) {
        return clamp(baseX + offsetX, 0, Math.max(0, screenWidth - contentWidth));
    }

    public static int placedY(int baseY, int offsetY, int screenHeight, int contentHeight) {
        return clamp(baseY + offsetY, 0, Math.max(0, screenHeight - contentHeight));
    }

    public static float fitScale(double configured, int width, int height, int screenWidth, int screenHeight) {
        float scale = hudScale(configured);
        scale = Math.min(scale, Math.max(1, screenWidth) / (float) Math.max(1, width));
        return Math.min(scale, Math.max(1, screenHeight) / (float) Math.max(1, height));
    }

    public static int effectRowHeight(int iconHeight, int textHeight, int gap) {
        return Math.max(iconHeight, 2 * textHeight) + gap;
    }

    public static EffectLayout effectLayout(int count, int availableWidth, int iconWidth, int rowHeight) {
        if (count <= 0) return new EffectLayout(1, 0, 0, 0, 0, 0);
        int width = Math.max(1, Math.min(360, availableWidth));
        int slotWidth = Math.min(iconWidth + 82, Math.max(1, width - 36));
        int columns = Math.max(1, Math.min(3, (width - 36 + 8) / (slotWidth + 8)));
        int visible = Math.min(count, columns * 2);
        int rows = (visible + columns - 1) / columns;
        int usedColumns = Math.min(visible, columns);
        int contentWidth = usedColumns * (slotWidth + 8) - 8 + (count > visible ? 36 : 0);
        return new EffectLayout(columns, visible, count - visible, slotWidth, contentWidth, rows * rowHeight);
    }

    public record EffectLayout(int columns, int visible, int hidden, int slotWidth, int width, int height) {}

    /** Two full rows plus overflow space, independent of the active effect count. */
    public static EffectLayout reservedEffectLayout(boolean enabled, int availableWidth, int iconWidth, int rowHeight) {
        return effectLayout(enabled ? 7 : 0, availableWidth, iconWidth, rowHeight);
    }

    static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    static String normalizeAnchor(String position) {
        return position == null ? "top_left" : position.trim().toLowerCase(Locale.ROOT).replace('-', '_');
    }
}
