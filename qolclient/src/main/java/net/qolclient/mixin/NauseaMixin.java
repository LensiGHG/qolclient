package net.qolclient.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.qolclient.QolClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Zeroes the "nausea factor" (the value that drives the swirly FOV warp
 * you get from the Nausea effect / travelling through a portal) so the
 * effect visually never kicks in.
 *
 * If LivingEntity#getNauseaFactor doesn't exist under this name on
 * 1.21.11's mappings, search for the float-returning method LivingEntity
 * exposes that GameRenderer multiplies into its FOV warp calculation.
 */
@Mixin(LivingEntity.class)
public class NauseaMixin {

    @Inject(method = "getNauseaFactor", at = @At("RETURN"), cancellable = true)
    private void qolclient$noNausea(float tickDelta, CallbackInfoReturnable<Float> cir) {
        if (QolClient.CONFIG.noNausea) {
            Object self = this;
            if (self == MinecraftClient.getInstance().player) {
                cir.setReturnValue(0.0F);
            }
        }
    }
}
