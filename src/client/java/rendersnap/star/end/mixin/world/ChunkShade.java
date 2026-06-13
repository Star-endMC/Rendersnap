package rendersnap.star.end.mixin.world;

import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockQuadOutput;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import rendersnap.star.end.client.render.Cuts;

@Mixin(ModelBlockRenderer.class)
public abstract class ChunkShade {

    @Redirect(
            method = "tesselateBlock(Lnet/minecraft/client/renderer/block/BlockQuadOutput;FFFLnet/minecraft/client/renderer/block/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/client/renderer/block/dispatch/BlockStateModel;J)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/block/dispatch/BlockStateModelPart;useAmbientOcclusion()Z")
    )
    private boolean rendersnap$trimChunkAo(
            BlockStateModelPart part,
            BlockQuadOutput output,
            float x,
            float y,
            float z,
            BlockAndTintGetter level,
            BlockPos pos,
            net.minecraft.world.level.block.state.BlockState state,
            BlockStateModel model,
            long seed
    ) {
        return part.useAmbientOcclusion() && Cuts.shouldUseChunkAo(pos);
    }
}
