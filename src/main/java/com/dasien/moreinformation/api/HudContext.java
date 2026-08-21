package com.dasien.moreinformation.api;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** Read-only client state exposed to optional HUD providers. */
@OnlyIn(Dist.CLIENT)
public record HudContext(Minecraft minecraft, ClientLevel level, LocalPlayer player, long clientTick) {
}
