package net.qolclient.mixin;

import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.util.math.BlockPos;
import net.qolclient.QolClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Purely a local counter for the HUD ("blocks mined this session") - reads
 * the return value of the client's own break-attempt call, doesn't send
 * anything extra to the server or change what gets sent.
 *
 * ClientPlayerInteractionManager#breakBlock(BlockPos) -> boolean (true on a
 * successful break, e.g. creative-mode instant break or survival finishing
 * the break) is a long-standing method on recent versions. If the name or
 * return type has shifted on 1.21.11, open ClientPlayerInteractionManager in
 * a mapping viewer and find the method called when the client decides a
 * block has finished breaking (as opposed to attackBlock, which is the
 * "start/continue mining" call and fires far more often than once).
 */
@Mixin(ClientPlayerInteractionManager.class)
public class BlockBreakCounterMixin {

    @Inject(method = "breakBlock", at = @At("RETURN"))
    private void qolclient$countBreak(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (QolClient.CONFIG.blocksMinedCounterEnabled && Boolean.TRUE.equals(cir.getReturnValue())) {
            QolClient.incrementBlocksMined();
        }
    }
}
