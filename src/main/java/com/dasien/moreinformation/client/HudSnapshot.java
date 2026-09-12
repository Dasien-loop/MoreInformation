package com.dasien.moreinformation.client;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Immutable snapshot consumed by the render callback. The text block width and the status effect
 * strip are both measured once per rebuild, so rendering never has to touch the font or walk the
 * player's effect list.
 */
public record HudSnapshot(List<HudLine> lines, int contentWidth, EffectsStrip effects) {
    /** Icon column plus the widest line; kept in sync with {@link HudManager}'s text offset. */
    public static final int ICON_WIDTH = 14;

    public HudSnapshot {
        lines = List.copyOf(lines);
        effects = effects == null ? EffectsStrip.EMPTY : effects;
        contentWidth = Math.max(ICON_WIDTH, contentWidth);
    }

    public static HudSnapshot empty() {
        return new HudSnapshot(List.of(), ICON_WIDTH, EffectsStrip.EMPTY);
    }

    public boolean isEmpty() {
        return lines.isEmpty() && effects.isEmpty();
    }
}

record HudLine(Component text, HudIcon icon, ItemStack customIcon, int iconLevel) {
}

/** Status effect icons drawn above the HUD text block, already measured in pixels. */
record EffectsStrip(float itemScale, List<EffectEntry> entries, int pixelWidth) {
    static final EffectsStrip EMPTY = new EffectsStrip(1.0F, List.of(), 0);

    EffectsStrip(float itemScale, List<EffectEntry> entries, int pixelWidth) {
        this.itemScale = itemScale <= 0.0F ? 1.0F : itemScale;
        this.entries = List.copyOf(entries);
        this.pixelWidth = Math.max(0, pixelWidth);
    }

    boolean isEmpty() {
        return entries.isEmpty();
    }
}

record EffectEntry(ItemStack stack, Component text, Component duration, int textWidth) {
}
