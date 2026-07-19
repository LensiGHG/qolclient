package net.qolclient.mixin;

import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.qolclient.QolClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Scales the Totem of Undying model (both when held and during the
 * "you got saved" pop animation, since vanilla renders both through the
 * same first-person item path) up or down.
 *
 * HeldItemRenderer#renderFirstPersonItem is a long-standing, commonly
 * targeted method name for first-person item rendering, so this one is
 * fairly safe - but the exact parameter list (which one is the ItemStack,
 * which is the MatrixStack) can shift release to release. If this doesn't
 * compile against 1.21.11, open HeldItemRenderer in a mapping viewer and
 * match the injected method's parameter types/order to what's actually
 * there, then adjust the parameter list below.
 */
@Mixin(HeldItemRenderer.class)
public class TotemScaleMixin {

    @Inject(method = "renderFirstPersonItem", at = @At("HEAD"))
    private void qolclient$scaleTotem(
        net.minecraft.client.network.AbstractClientPlayerEntity player,
        float tickDelta,
        float pitch,
        net.minecraft.util.Hand hand,
        float swingProgress,
        ItemStack item,
        float equipProgress,
        MatrixStack matrices,
        net.minecraft.client.render.VertexConsumerProvider vertexConsumers,
        int light,
        CallbackInfo ci
    ) {
        if (QolClient.CONFIG.totemScaleEnabled && item.getItem() == Items.TOTEM_OF_UNDYING) {
            float s = QolClient.CONFIG.totemScale;
            matrices.translate(0.5, 0.5, 0.5);
            matrices.scale(s, s, s);
            matrices.translate(-0.5, -0.5, -0.5);
        }
    }
}
