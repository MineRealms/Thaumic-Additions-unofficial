package dev.tc4port.thaumadditions.mixin.client;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Dev-only: keeps the integrated server ticking while the pause menu is open or the window
 * lost focus. Vanilla derives the pause state in {@code Minecraft.runTick} from the open
 * screen, and {@code IntegratedServer.tickServer} then switches to {@code tickPaused()},
 * which stalls every world-side probe until the screen is dismissed.
 *
 * <p>Inert unless {@code -Dthaumadds.dev.noPause=true} is set (the dev run configs set it),
 * so shipped builds behave exactly like vanilla. {@code require = 0} keeps an unmapped
 * production name from ever failing mod loading.</p>
 */
@Mixin(Minecraft.class)
public abstract class MinecraftNoPauseMixin {
    @Inject(method = "isPaused", at = @At("HEAD"), cancellable = true, require = 0)
    private void thaumadds$neverPause(CallbackInfoReturnable<Boolean> cir) {
        if (Boolean.getBoolean("thaumadds.dev.noPause"))
            cir.setReturnValue(false);
    }
}
