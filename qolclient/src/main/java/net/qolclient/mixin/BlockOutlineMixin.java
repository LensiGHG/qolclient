package net.qolclient.mixin;

import net.minecraft.client.render.WorldRenderer;
import net.qolclient.QolClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Redirects the RenderSystem.lineWidth(float) call made right before the
 * block-selection outline is drawn, so we can make it thicker/thinner.
 *
 * "drawBlockOutline" is WorldRenderer's long-standing name for this method
 * across recent versions; if it's been renamed on 1.21.11, search
 * WorldRenderer for the method that draws the black box around the block
 * you're looking at and point this @Redirect there instead.
 */
@Mixin(WorldRenderer.class)
public class BlockOutlineMixin {

    @Redirect(
        method = "drawBlockOutline",
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;lineWidth(F)V")
    )
    private void qolclient$outlineWidth(float width) {
        if (QolClient.CONFIG.blockOutlineEnabled) {
            com.mojang.blaze3d.systems.RenderSystem.lineWidth(QolClient.CONFIG.blockOutlineWidth);
        } else {
            com.mojang.blaze3d.systems.RenderSystem.lineWidth(width);
        }
    }
}
