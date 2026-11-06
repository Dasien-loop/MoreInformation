package com.dasien.moreinformation.client.compat.neovitae;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NeoVitaeClientDataTest {
    private final UUID player = UUID.randomUUID();
    private final NeoVitaeClientData data = NeoVitaeClientData.forCache(FakeCache.class);

    @BeforeEach
    void reset() {
        FakeCache.owner = null;
        FakeCache.amount = 0;
        FakeCache.fail = false;
    }

    @Test
    void waitsForSyncedDataForTheCurrentPlayer() {
        assertTrue(data.currentEv(player).isEmpty());
        FakeCache.owner = UUID.randomUUID();
        FakeCache.amount = 5000;
        assertTrue(data.currentEv(player).isEmpty());
        FakeCache.owner = player;
        assertEquals(5000, data.currentEv(player).orElseThrow());
    }

    @Test
    void preservesZeroAndRejectsInvalidAmounts() {
        FakeCache.owner = player;
        assertEquals(0, data.currentEv(player).orElseThrow());
        FakeCache.amount = -1;
        assertTrue(data.currentEv(player).isEmpty());
        FakeCache.amount = Integer.MAX_VALUE;
        assertEquals(Integer.MAX_VALUE, data.currentEv(player).orElseThrow());
    }

    @Test
    void missingChangedOrFailingApisAreOptional() {
        assertTrue(NeoVitaeClientData.unavailable().currentEv(player).isEmpty());
        assertTrue(NeoVitaeClientData.forCache(Object.class).currentEv(player).isEmpty());
        assertTrue(NeoVitaeClientData.forCache(NonStaticCache.class).currentEv(player).isEmpty());
        FakeCache.fail = true;
        assertTrue(data.currentEv(player).isEmpty());
        assertTrue(data.currentEv(null).isEmpty());
    }

    public static final class FakeCache {
        static UUID owner;
        static int amount;
        static boolean fail;

        public static boolean has(UUID candidate) {
            if (fail) throw new IllegalStateException("unavailable");
            return candidate.equals(owner);
        }

        public static int getCurrentEV() {
            return amount;
        }
    }

    public static final class NonStaticCache {
        public boolean has(UUID candidate) {
            return true;
        }

        public int getCurrentEV() {
            return 1000;
        }
    }
}
