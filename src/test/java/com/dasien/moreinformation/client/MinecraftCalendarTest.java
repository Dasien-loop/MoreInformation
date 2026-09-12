package com.dasien.moreinformation.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MinecraftCalendarTest {
    @Test
    void startsAtZeroYearAndDay() {
        assertEquals(0, HudMath.year(0));
        assertEquals(0, HudMath.dayOfYear(0));
    }

    @Test
    void advancesAtTheEndOfEach365DayYear() {
        assertEquals(0, HudMath.year(364));
        assertEquals(364, HudMath.dayOfYear(364));
        assertEquals(1, HudMath.year(365));
        assertEquals(0, HudMath.dayOfYear(365));
    }
}
