package rendersnap.star.end.mixin.world;

import rendersnap.star.end.client.render.Cuts;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ModelBlockRenderer.class)
public abstract class BlockFaces {

    //? if >=26.1.2 {
    @Redirect(
            method = "shouldRenderFace",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;shouldRenderFace(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;)Z")
    )
    private boolean sameBlockFace(BlockState state, BlockState neighbor, Direction side) {
        return Block.shouldRenderFace(state, neighbor, side) && !Cuts.hidesJoinedFace(state, neighbor);
    }
    //?}
}
