package net.qolclient.mixin;

import net.minecraft.client.render.FogRenderer;
import net.qolclient.QolClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Pushes fog distances out to effectively remove render-distance/biome fog.
 *
 * NOTE: fog handling was reworked around 1.21 into FogRenderer + a FogData
 * parameter object. The exact method name/signature (applyFog vs
 * computeFog, param order, etc.) can drift between snapshots - if this
 * mixin fails to apply, check FogRenderer in a mapping viewer for the
 * method that receives a FogData/FogType and mutates near/far distance,
 * and point the @Inject at that instead.
 */
@Mixin(FogRenderer.class)
public class FogMixin {

    @Inject(method = "applyFog", at = @At("RETURN"))
    private static void qolclient$pushFogOut(
        net.minecraft.client.render.Camera camera,
        net.minecraft.client.render.FogRenderer.FogData fogData,
        boolean thickFog,
        float tickDelta,
        boolean thirdPerson,
        float viewDistance,
        CallbackInfo ci
    ) {
        if (QolClient.CONFIG.noFog) {
            fogData.fogStart = viewDistance * 100.0F;
            fogData.fogEnd = viewDistance * 200.0F;
        }
    }
}
