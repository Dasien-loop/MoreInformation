package com.dasien.moreinformation.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VanillaEffectsVisibilityTest {
    @Test
    void iconQueriesOnlyHideTheLocalPlayersOriginallyVisibleIcons() {
        for (int flags = 0; flags < 8; flags++) {
            boolean original = (flags & 1) != 0;
            boolean replacement = (flags & 2) != 0;
            boolean local = (flags & 4) != 0;
            assertEquals(original && flags != 7,
                    VanillaEffectsVisibility.showIcon(original, replacement, local),
                    "icon flags=" + flags);
        }
    }

    @Test
    void visibilityRestoresImmediatelyWithoutChangingTheOriginalFlag() {
        for (boolean original : new boolean[]{false, true}) {
            assertEquals(original, VanillaEffectsVisibility.showIcon(original, false, true));
            assertEquals(false, VanillaEffectsVisibility.showIcon(original, true, true));
            assertEquals(original, VanillaEffectsVisibility.showIcon(original, false, true));
            // Queries for remote entities, item effects and detached copies keep the original flag.
            assertEquals(original, VanillaEffectsVisibility.showIcon(original, true, false));
        }
    }

    @Test
    void hidesIconsAndAvoidanceOnlyWhenHudReplacesVanillaEffects() {
        for (int flags = 0; flags < 32; flags++) {
            boolean hud = (flags & 1) != 0;
            boolean effects = (flags & 2) != 0;
            boolean hideGui = (flags & 4) != 0;
            boolean debug = (flags & 8) != 0;
            boolean container = (flags & 16) != 0;
            assertEquals(flags == 3,
                    VanillaEffectsVisibility.shouldHide(hud, effects, hideGui, debug, container),
                    "visibility flags=" + flags);
        }
    }
}
