package net.qolclient.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.util.Identifier;
import net.qolclient.QolClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Vanilla renders both the pumpkin-on-head blur and the on-fire screen
 * overlay through one shared helper, historically named
 * InGameHud#renderOverlay(DrawContext, Identifier, float opacity).
 * This mixin cancels it entirely for the pumpkin texture, and halves the
 * opacity for the fire texture.
 *
 * If the method/constant names below don't match 1.21.11 (InGameHud gets
 * refactored occasionally), open InGameHud in a mapping viewer, find where
 * PUMPKIN_BLUR_TEXTURE / the fire overlay texture get drawn, and update the
 * @Mixin target + identifier comparisons accordingly.
 */
@Mixin(InGameHud.class)
public class OverlayMixin {

    @Inject(method = "renderOverlay", at = @At("HEAD"), cancellable = true)
    private void qolclient$cancelPumpkin(DrawContext context, Identifier texture, float opacity, CallbackInfo ci) {
        if (QolClient.CONFIG.noPumpkinOverlay && texture.getPath().contains("pumpkin")) {
            ci.cancel();
        }
    }

    @ModifyVariable(method = "renderOverlay", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float qolclient$dimFire(float opacity, DrawContext context, Identifier texture) {
        if (QolClient.CONFIG.lowFireOverlay && texture.getPath().contains("fire")) {
            return opacity * 0.35F;
        }
        return opacity;
    }
}
