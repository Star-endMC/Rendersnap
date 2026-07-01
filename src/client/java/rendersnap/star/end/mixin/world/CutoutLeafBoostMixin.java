package rendersnap.star.end.mixin.world;

import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import rendersnap.star.end.client.render.Cuts;

@Mixin(SectionCompiler.class)
public abstract class CutoutLeafBoostMixin {

    @Redirect(
            method = "compile",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/block/ModelBlockRenderer;forceOpaque(ZLnet/minecraft/world/level/block/state/BlockState;)Z")
    )
    private boolean rendersnap$boostCutoutLeaves(boolean cutoutLeaves, BlockState state) {
        return Cuts.shouldForceOpaqueCutoutLeaves(cutoutLeaves, state);
    }
}
