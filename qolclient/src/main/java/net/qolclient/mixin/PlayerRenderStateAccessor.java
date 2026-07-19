package net.qolclient.mixin;

import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Since Minecraft 1.21.2's renderer refactor, per-frame render data for an
 * entity lives on a "RenderState" object computed once and read by feature
 * renderers, rather than feature renderers reading straight off the entity.
 * PlayerEntityRenderState is expected to carry a `capeTexture` field
 * (an Identifier, possibly nullable/Optional depending on exact version).
 *
 * This is the single most likely thing in this mod to need hand-fixing on
 * 1.21.11: open PlayerEntityRenderState in a mapping viewer, find the actual
 * field name/type for the resolved cape texture, and match the accessor
 * below (including switching the type if it turns out to be
 * Optional<Identifier> rather than a plain nullable Identifier).
 */
@Mixin(PlayerEntityRenderState.class)
public interface PlayerRenderStateAccessor {

    @Accessor("capeTexture")
    @Mutable
    void qolclient$setCapeTexture(Identifier id);

    @Accessor("capeTexture")
    Identifier qolclient$getCapeTexture();
}
