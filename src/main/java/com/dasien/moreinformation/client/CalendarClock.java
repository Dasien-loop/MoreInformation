package com.dasien.moreinformation.client;

/** Counts days from authoritative samples, not client-side interpolation. */
final class CalendarClock {
    private long rawTime;
    private long day;

    CalendarClock(long rawTime) {
        this(rawTime, Math.max(0L, Math.floorDiv(rawTime, 24000L)));
    }

    CalendarClock(long rawTime, long day) {
        this.rawTime = rawTime;
        this.day = Math.max(0L, day);
    }

    void accept(long time) {
        long advance;
        if (time >= rawTime) {
            advance = Math.floorDiv(time, 24000L) - Math.floorDiv(rawTime, 24000L);
        } else {
            advance = Math.floorMod(time, 24000L) < Math.floorMod(rawTime, 24000L) ? 1L : 0L;
        }
        day = advance > Long.MAX_VALUE - day ? Long.MAX_VALUE : day + advance;
        rawTime = time;
    }

    long day() { return day; }
    long rawTime() { return rawTime; }
}
