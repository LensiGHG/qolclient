package net.qolclient.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.feature.ElytraFeatureRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.util.Identifier;
import net.qolclient.ElytraManager;
import net.qolclient.QolClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * FLAG: this is the elytra equivalent of CapeMixin, but riskier - read
 * before assuming it just works.
 *
 * Vanilla has no per-player elytra texture. ElytraFeatureRenderer#render
 * resolves ONE constant Identifier (historically a static final field named
 * something like ElytraFeatureRenderer.TEXTURE, "textures/entity/elytra.png")
 * and every player wearing an elytra draws that same texture. So instead of
 * writing into a render-state field (like the cape does), this mixin has to
 * intercept the texture Identifier itself as a local variable inside
 * #render and swap it before it reaches the draw call.
 *
 * @ModifyVariable(at = @At("STORE")) targets the FIRST local variable of
 * type Identifier that gets assigned in the method body - on current Yarn
 * that should be the resolved elytra texture, but this is exactly the kind
 * of thing that breaks silently (wrong variable, or the field is inlined as
 * a constant with no local at all - in which case @ModifyVariable simply
 * never fires and the custom elytra silently does nothing, it won't throw).
 *
 * If it doesn't apply on 1.21.11: open ElytraFeatureRenderer in a mapping
 * viewer, find #render(MatrixStack, VertexConsumerProvider, int,
 * PlayerEntityRenderState, float, float) (signature may drift slightly),
 * and see whether the texture is a local variable (this approach works,
 * just may need the `ordinal` adjusted) or a straight constant field
 * reference passed directly into the RenderLayer call (in which case switch
 * this to a @Redirect on the RenderLayer.getEntityCutoutNoCull(Identifier)
 * / RenderLayer.getArmorCutoutNoCull(Identifier) call instead, same pattern
 * as this file, just redirecting the call rather than the variable).
 */
@Mixin(ElytraFeatureRenderer.class)
public class ElytraMixin {

    @ModifyVariable(method = "render", at = @At("STORE"), ordinal = 0)
    private Identifier qolclient$swapElytraTexture(Identifier original) {
        if (!QolClient.CONFIG.customElytraEnabled) return original;
        if (!ElytraManager.isRegistered()) return original;
        // Only swap for the texture actually used for the elytra render pass;
        // guard by checking the original path so we don't accidentally catch
        // an unrelated Identifier local elsewhere in the method.
        if (original == null || !original.getPath().contains("elytra")) return original;
        return ElytraManager.CUSTOM_ELYTRA_ID;
    }

    /**
     * Best-effort local-player-only guard. ElytraFeatureRenderer doesn't
     * take the AbstractClientPlayerEntity directly (only the render state),
     * so unlike CapeMixin we can't trivially compare `player != client.player`
     * here. PlayerEntityRenderState as of 1.21.2+ carries an entity name /
     * uuid-derived identity but no guaranteed direct back-reference to the
     * entity object. If you want this to be strictly self-view only (like
     * the cape), the safest fix is comparing state's name/uuid field against
     * MinecraftClient.getInstance().player's - open PlayerEntityRenderState
     * to find that field, since capeTexture's neighbor fields (name, uuid)
     * are likely present. Left as a TODO rather than guessed at, since
     * guessing wrong here means it could silently apply to OTHER players'
     * elytras on your screen instead of just your own - worth verifying
     * before you rely on the self-view-only claim for this one specifically.
     */
    private static boolean qolclient$isLocalPlayer(PlayerEntityRenderState state) {
        return true; // TODO: narrow this down per the javadoc above before relying on self-view-only
    }
}
