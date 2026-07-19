package net.qolclient.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.qolclient.QolClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * "renderCrosshair" is InGameHud's long-standing name for the method that
 * draws the center crosshair; signature (DrawContext, RenderTickCounter) has
 * been stable across recent versions. If it's been renamed/reshaped on
 * 1.21.11, open InGameHud in a mapping viewer and look for the method that
 * draws HudElement.CROSSHAIR / the plus-sign texture at screen center.
 *
 * Scale is done by pushing a matrix scale around the screen center before
 * the vanilla draw calls run, then popping it after - same technique as
 * TotemScaleMixin, just on the HUD matrix stack instead of the world one.
 * Color tint uses RenderSystem.setShaderColor, reset to white afterward so
 * it doesn't leak into whatever the game draws next frame.
 */
@Mixin(InGameHud.class)
public class CrosshairMixin {

    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void qolclient$head(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();

        if (QolClient.CONFIG.crosshairCustomEnabled && QolClient.CONFIG.crosshairDynamicHide
            && client.crosshairTarget == null) {
            ci.cancel();
            return;
        }

        if (!QolClient.CONFIG.crosshairCustomEnabled) return;

        float scale = QolClient.CONFIG.crosshairScale;
        int centerX = client.getWindow().getScaledWidth() / 2;
        int centerY = client.getWindow().getScaledHeight() / 2;

        context.getMatrices().push();
        context.getMatrices().translate(centerX, centerY, 0);
        context.getMatrices().scale(scale, scale, 1.0f);
        context.getMatrices().translate(-centerX, -centerY, 0);

        int rgb = QolClient.CONFIG.crosshairColor;
        float r = ((rgb >> 16) & 0xFF) / 255.0f;
        float g = ((rgb >> 8) & 0xFF) / 255.0f;
        float b = (rgb & 0xFF) / 255.0f;
        RenderSystem.setShaderColor(r, g, b, 1.0f);
    }

    @Inject(method = "renderCrosshair", at = @At("TAIL"))
    private void qolclient$tail(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (!QolClient.CONFIG.crosshairCustomEnabled) return;
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        context.getMatrices().pop();
    }
}
