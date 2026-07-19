package net.qolclient.mixin;

import net.minecraft.client.render.GameRenderer;
import net.qolclient.QolClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Cancels the camera shake/tilt that plays when the player takes damage.
 * Target method name has changed across versions ("bobViewWhenHurt" in
 * recent Yarn). If this fails to apply on 1.21.11, open the GameRenderer
 * class in a mapping viewer (e.g. https://mappings.dev) and find the method
 * called right before the world is rendered that reacts to
 * LivingEntity#hurtTime / damage tilt, then update @Inject's method value.
 */
@Mixin(GameRenderer.class)
public class HurtCamMixin {

    @Inject(method = "bobViewWhenHurt", at = @At("HEAD"), cancellable = true)
    private void qolclient$cancelHurtCam(net.minecraft.client.util.math.MatrixStack matrices, float tickDelta, CallbackInfo ci) {
        if (QolClient.CONFIG.noHurtCam) {
            ci.cancel();
        }
    }
}
