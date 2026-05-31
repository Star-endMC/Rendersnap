package rendersnap.star.end.mixin.scene;

import rendersnap.star.end.client.render.Cuts;
//? if >=26.1.2 {
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.FluidRenderer;
//?} else {
/*import net.minecraft.client.renderer.block.LiquidBlockRenderer;
import net.minecraft.world.level.BlockAndTintGetter;
*///?}
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

//? if >=26.1.2 {
@Mixin(FluidRenderer.class)
//?} else {
/*@Mixin(LiquidBlockRenderer.class)
*///?}
public abstract class WaterLight {

    //? if >=26.1.2 {
    @Inject(method = "getLightCoords", at = @At("RETURN"), cancellable = true)
    //?} else {
    /*@Inject(method = "getLightColor", at = @At("RETURN"), cancellable = true)
    *///?}
    private void smoothFluidLight(BlockAndTintGetter level, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(Cuts.getOptimizedFluidLight(level, pos, cir.getReturnValue()));
    }
}
