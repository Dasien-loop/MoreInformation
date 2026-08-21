package com.dasien.moreinformation.client;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Immutable text and icon snapshot consumed by the render callback. */
public record HudSnapshot(List<HudLine> lines) {
    public HudSnapshot {
        lines = List.copyOf(lines);
    }

    public static HudSnapshot empty() {
        return new HudSnapshot(List.of());
    }
}

record HudLine(Component text, HudIcon icon, ItemStack customIcon, int iconLevel) {
}
