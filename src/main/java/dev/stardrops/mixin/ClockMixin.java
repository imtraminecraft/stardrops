package dev.stardrops.mixin;

import dev.stardrops.NightLock;
import net.minecraft.client.ClientClockManager;
import net.minecraft.core.Holder;
import net.minecraft.world.clock.WorldClock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Always night (Visuals > Cosmos > Night sky): every question the client asks about the world's clock is answered "midnight".
 * The game then draws its own night (no sun, a dark sky, the moon) and our sky box sits in it. The answer is one constant, so
 * nothing can change from frame to frame (standing rule: no flickering); the server's real time is never touched.
 */
@Mixin(ClientClockManager.class)
public abstract class ClockMixin {
    @Inject(method = "getTotalTicks", at = @At("HEAD"), cancellable = true)
    private void stardrops$alwaysNight(Holder<WorldClock> definition, CallbackInfoReturnable<Long> cir) {
        if (NightLock.on()) cir.setReturnValue(NightLock.MIDNIGHT);
    }
}
