package de.fabisosc.fabric.mixin;

import de.fabisosc.fabric.FabisOscMod;
import net.minecraft.network.protocol.game.ServerboundPunchPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Linksklick in die Luft schickt seit 26.3 ein eigenes "Punch"-Paket.
 * Fabric API hat dafuer kein Event, also haengen wir uns hier rein.
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class PunchMixin {

    @Shadow
    public ServerPlayer player;

    // Erst nach dem Thread-Check einhaken, dann laufen wir sicher im Server-Thread
    @Inject(method = "handlePunch",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;resetLastActionTime()V"))
    private void fabisosc$onPunch(ServerboundPunchPacket packet, CallbackInfo ci) {
        FabisOscMod.get().onLeftClick(player, false);
    }
}
