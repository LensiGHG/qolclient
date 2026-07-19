package net.qolclient.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.qolclient.CapeManager;
import net.qolclient.QolClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * See PlayerRenderStateAccessor's javadoc first - this class depends on that
 * one being correct for 1.21.11.
 *
 * "updateRenderState" is the name of PlayerEntityRenderer's per-frame state
 * population method as of the 1.21.2 renderer refactor; if it's been
 * renamed, look for the method that takes an AbstractClientPlayerEntity and
 * a PlayerEntityRenderState and copies fields from the former to the latter.
 */
@Mixin(PlayerEntityRenderer.class)
public class CapeMixin {

    @Inject(method = "updateRenderState", at = @At("TAIL"))
    private void qolclient$applyCustomCape(AbstractClientPlayerEntity player, PlayerEntityRenderState state,
                                            float tickDelta, CallbackInfo ci) {
        if (!QolClient.CONFIG.customCapeEnabled) return;
        if (player != MinecraftClient.getInstance().player) return; // local player only, see CapeManager javadoc
        if (!CapeManager.isRegistered()) return;

        ((PlayerRenderStateAccessor) state).qolclient$setCapeTexture(CapeManager.CUSTOM_CAPE_ID);
    }
}
