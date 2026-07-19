package net.qolclient.mixin;

import net.minecraft.client.render.GameRenderer;
import net.qolclient.QolClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Multiplies the computed FOV while the zoom key is held.
 * Target: GameRenderer#getFov(Camera, float, boolean) in recent mappings.
 */
@Mixin(GameRenderer.class)
public class ZoomMixin {

    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void qolclient$zoom(net.minecraft.client.render.Camera camera, float tickDelta, boolean changingFov,
                                 CallbackInfoReturnable<Double> cir) {
        if (QolClient.isZoomActive()) {
            cir.setReturnValue(cir.getReturnValue() * QolClient.CONFIG.zoomFovMultiplier);
        }
    }
}
