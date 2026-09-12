package com.dasien.moreinformation.client;

import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HudMathTest {
    @Test
    void reservesStableBoundsForEmptySingleRowAndOverflowEffects() {
        for (int width : new int[]{134, 200, 400}) {
            for (int icon : new int[]{8, 16, 48}) {
                int rowHeight = HudMath.effectRowHeight(icon, 10, 2);
                var reserved = HudMath.reservedEffectLayout(true, width, icon, rowHeight);
                assertEquals(2 * rowHeight, reserved.height());
                for (int count : new int[]{0, 1, 3, 6, 7, 20}) {
                    var actual = HudMath.effectLayout(count, width, icon, rowHeight);
                    org.junit.jupiter.api.Assertions.assertTrue(actual.width() <= reserved.width());
                    org.junit.jupiter.api.Assertions.assertTrue(actual.height() <= reserved.height());
                }
            }
        }
        var disabled = HudMath.reservedEffectLayout(false, 400, 16, 22);
        assertEquals(0, disabled.width());
        assertEquals(0, disabled.height());
    }

    @Test
    void limitsEffectsToTwoRowsAndSixEntries() {
        var layout = HudMath.effectLayout(20, 400, 16, 22);
        assertEquals(3, layout.columns());
        assertEquals(6, layout.visible());
        assertEquals(14, layout.hidden());
        assertEquals(44, layout.height());
        org.junit.jupiter.api.Assertions.assertTrue(layout.width() <= 360);
    }

    @Test
    void reducesEffectColumnsOnNarrowScreens() {
        var layout = HudMath.effectLayout(6, 200, 16, 22);
        assertEquals(1, layout.columns());
        assertEquals(2, layout.visible());
        assertEquals(4, layout.hidden());
        org.junit.jupiter.api.Assertions.assertTrue(layout.width() <= 200);
        assertEquals(0, HudMath.effectLayout(0, 200, 16, 22).height());
        assertEquals(22, HudMath.effectLayout(1, 400, 16, 22).height());
    }

    @Test
    void fitsOversizedHudToViewport() {
        assertEquals(0.5F, HudMath.fitScale(2.0, 800, 40, 400, 200));
        assertEquals(0.5F, HudMath.fitScale(2.0, 80, 400, 400, 200));
        assertEquals(2.0F, HudMath.fitScale(2.0, 80, 40, 400, 200));
        assertEquals(0.25F, HudMath.fitScale(0.25, 80, 40, 400, 200));
    }

    @Test
    void alignsEffectIconBesideTwoTextRows() {
        assertEquals(22, HudMath.effectRowHeight(8, 10, 2));
        assertEquals(22, HudMath.effectRowHeight(16, 10, 2));
        assertEquals(50, HudMath.effectRowHeight(48, 10, 2));
    }

    @Test
    void formatsSessionDuration() {
        assertEquals("00:00", HudMath.formatDuration(0));
        assertEquals("02:32", HudMath.formatDuration(152));
        assertEquals("00:00", HudMath.formatDuration(-1));
    }

    @Test
    void formatsRealClockInConfiguredTimeZone() {
        assertEquals("08:00:00", HudMath.formatClock(Instant.parse("2026-01-01T00:00:00Z"), "Asia/Shanghai"));
        assertEquals("00:00:00", HudMath.formatClock(Instant.parse("2026-01-01T00:00:00Z"), "UTC"));
    }

    @Test
    void calculatesMinecraftCalendar() {
        assertEquals(0, HudMath.year(0));
        assertEquals(0, HudMath.dayOfYear(0));
        assertEquals(0, HudMath.year(364));
        assertEquals(364, HudMath.dayOfYear(364));
        assertEquals(1, HudMath.year(365));
        assertEquals(0, HudMath.dayOfYear(365));
        assertEquals(-1, HudMath.year(-1));
        assertEquals(364, HudMath.dayOfYear(-1));
    }

    @Test
    void convertsMsptToTps() {
        assertEquals(20.0F, HudMath.tpsFromMspt(50.0F));
        assertEquals(10.0F, HudMath.tpsFromMspt(100.0F));
        assertEquals(20.0F, HudMath.tpsFromMspt(0.0F));
    }

    @Test
    void estimatesMsptFromTickTiming() {
        assertEquals(50.0F, HudMath.estimateMspt(20, 1_000_000_000L));
        assertEquals(0.0F, HudMath.estimateMspt(0, 1_000_000_000L));
    }

    @Test
    void formatsRamUsage() {
        assertEquals("512/2048MB", HudMath.formatRam(512L * 1024L * 1024L, 2048L * 1024L * 1024L));
        assertEquals("1.0", HudMath.formatOneDecimal(1.04F));
    }

    @Test
    void formatsSmallEmcExactly() {
        assertEquals("0", HudMath.formatEmc(BigInteger.ZERO, true));
        assertEquals("9,999", HudMath.formatEmc(BigInteger.valueOf(9999), true));
        assertEquals("1,234", HudMath.formatEmc(BigInteger.valueOf(1234), false));
    }

    @Test
    void formatsLargeEmcWithUnitsUnlessExactRequested() {
        assertEquals("10K", HudMath.formatEmc(BigInteger.valueOf(10_000), true));
        assertEquals("1.23M", HudMath.formatEmc(BigInteger.valueOf(1_234_567), true));
        assertEquals("1,234,567", HudMath.formatEmc(BigInteger.valueOf(1_234_567), false));
        assertEquals("1Qi", HudMath.formatEmc(new BigInteger("1000000000000000000"), true));
        assertEquals("1,000,000,000,000,000,000", HudMath.formatEmc(new BigInteger("1000000000000000000"), false));
    }

    @Test
    void formatsStatusEffectDurations() {
        assertEquals("00:30", HudMath.formatEffectDuration(600));
        assertEquals("02:00", HudMath.formatEffectDuration(2400));
        assertEquals("00:00", HudMath.formatEffectDuration(19));
        assertEquals("∞", HudMath.formatEffectDuration(-1));
    }

    @Test
    void clampsStatusEffectIconScale() {
        assertEquals(1.0F, HudMath.effectIconScale(1.0D));
        assertEquals(0.5F, HudMath.effectIconScale(0.5D));
        assertEquals(3.0F, HudMath.effectIconScale(5.0D));
        assertEquals(0.5F, HudMath.effectIconScale(0.1D));
        assertEquals(1.0F, HudMath.effectIconScale(0.0D));
        assertEquals(1.0F, HudMath.effectIconScale(-2.0D));
    }

    @Test
    void scalesCoordinatesLikeANetherPortal() {
        assertEquals(125, HudMath.portalCoordinate(1000, 8));
        assertEquals(-125, HudMath.portalCoordinate(-1000, 8));
        assertEquals(-1, HudMath.portalCoordinate(-1, 8));
        assertEquals(-2, HudMath.portalCoordinate(-9, 8));
        assertEquals(1000, HudMath.portalCoordinate(125, -8));
        assertEquals(-1000, HudMath.portalCoordinate(-125, -8));
    }

    @Test
    void portalCoordinatesIncludeHeightAndRemainVisibleNearOrigin() {
        assertEquals("0 64 0", HudMath.portalCoordinates(0, 64, 0, 8));
        assertEquals("0 70 -1", HudMath.portalCoordinates(7, 70, -1, 8));
        assertEquals("1 70 -1", HudMath.portalCoordinates(8, 70, -8, 8));
        assertEquals("1 70 -2", HudMath.portalCoordinates(9, 70, -9, 8));
        assertEquals("0 32 0", HudMath.portalCoordinates(0, 32, 0, -8));
        assertEquals("512 80 -512", HudMath.portalCoordinates(64, 80, -64, -8));
        assertEquals("520 80 -520", HudMath.portalCoordinates(65, 80, -65, -8));
        assertEquals("125 -20 -125", HudMath.portalCoordinates(1000, -20, -1000, 8));
    }

    @Test
    void anchorsHudToConfiguredCorners() {
        assertEquals(4, HudMath.anchorX(false, 400, 80, 4));
        assertEquals(316, HudMath.anchorX(true, 400, 80, 4));
        assertEquals(4, HudMath.anchorY("top", 200, 20, 4));
        assertEquals(90, HudMath.anchorY("middle", 200, 20, 4));
        assertEquals(176, HudMath.anchorY("bottom", 200, 20, 4));
        assertEquals("middle", HudMath.hudVertical("left_center"));
        assertEquals(true, HudMath.hudRight("bottom_right"));
    }

    @Test
    void appliesPixelOffsetsWithoutLeavingTheScreen() {
        assertEquals(14, HudMath.placedX(4, 10, 400, 80));
        assertEquals(306, HudMath.placedX(316, -10, 400, 80));
        assertEquals(0, HudMath.placedX(4, -20, 400, 80));
        assertEquals(320, HudMath.placedX(316, 40, 400, 80));
        assertEquals(14, HudMath.placedY(4, 10, 200, 20));
        assertEquals(180, HudMath.placedY(176, 20, 200, 20));
    }

    @Test
    void clampsAndScalesHudSize() {
        assertEquals(1.0F, HudMath.hudScale(1.0D));
        assertEquals(0.5F, HudMath.hudScale(0.5D));
        assertEquals(2.0F, HudMath.hudScale(2.0D));
        assertEquals(0.25F, HudMath.hudScale(0.1D));
        assertEquals(4.0F, HudMath.hudScale(8.0D));
        assertEquals(1.0F, HudMath.hudScale(0.0D));
        assertEquals(1.0F, HudMath.hudScale(-1.0D));
        assertEquals(40, HudMath.scaledSize(80, 0.5F));
        assertEquals(160, HudMath.scaledSize(80, 2.0F));
        assertEquals(80, HudMath.scaledSize(80, 0.0F));
    }
}
