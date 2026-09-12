package com.dasien.moreinformation.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CalendarClockTest {
    @Test
    void earlierTimeMovesToTomorrowAndLaterTimeStaysToday() {
        CalendarClock clock = new CalendarClock(5 * 24000L + 12000);
        clock.accept(1000);
        assertEquals(6, clock.day());
        clock.accept(13000);
        assertEquals(6, clock.day());
        clock.accept(13000);
        assertEquals(6, clock.day());
        clock.accept(1000);
        assertEquals(7, clock.day());
    }

    @Test
    void absoluteResetToLaterHourDoesNotEraseDays() {
        CalendarClock clock = new CalendarClock(5 * 24000L + 12000);
        clock.accept(13000);
        assertEquals(5, clock.day());
        clock.accept(24000);
        assertEquals(6, clock.day());
    }

    @Test
    void naturalRolloverSleepAndMultiDayAddAdvance() {
        CalendarClock clock = new CalendarClock(23999);
        clock.accept(24000);
        assertEquals(1, clock.day());
        clock.accept(37000);
        clock.accept(48000);
        assertEquals(2, clock.day());
        clock.accept(48000 + 3 * 24000);
        assertEquals(5, clock.day());
    }

    @Test
    void annualBoundarySurvivesReset() {
        CalendarClock clock = new CalendarClock(364 * 24000L + 12000);
        clock.accept(1000);
        assertEquals(1, HudMath.year(clock.day()));
        assertEquals(0, HudMath.dayOfYear(clock.day()));
    }

    @Test
    void savesAndRestoresIndependentDates(@TempDir Path directory) throws Exception {
        Path first = directory.resolve("first.txt");
        CalendarClock clock = new CalendarClock(5 * 24000L + 12000);
        clock.accept(1000);
        CalendarStore.save(first, clock);
        assertEquals(6, CalendarStore.load(first, 1020).day());
        assertEquals(0, CalendarStore.load(directory.resolve("other.txt"), 1020).day());
        assertEquals(7, CalendarStore.load(first, 25000).day());
    }
}
