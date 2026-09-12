package com.dasien.moreinformation.mixin.client;

import com.dasien.moreinformation.client.ClientCalendar;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientTimeMixin {
    @Inject(method = "handleSetTime", at = @At("TAIL"))
    private void moreinformation$trackCalendar(ClientboundSetTimePacket packet, CallbackInfo ci) {
        ClientCalendar.acceptTime(packet.getDayTime());
    }
}
