package com.dasien.moreinformation.client.compat.neovitae;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NeoVitaeProviderTest {
    @Test
    void usesTheAraVitaeAltarIcon() {
        assertEquals("neovitae:ara_vitae", NeoVitaeProvider.ICON_ID.toString());
    }

    @Test
    void formatsEvWithoutAnApproximationPrefix() {
        assertEquals("0 EV", NeoVitaeProvider.formatEv(0));
        assertEquals("12,500 EV", NeoVitaeProvider.formatEv(12500));
        assertEquals("2,147,483,647 EV", NeoVitaeProvider.formatEv(Integer.MAX_VALUE));
    }
}
