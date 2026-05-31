package rendersnap.star.end.mixin.world;

import rendersnap.star.end.client.render.Cuts;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
//? if >=26.1.2 {
import net.minecraft.world.level.BlockAndLightGetter;
//?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelRenderer.class)
public abstract class LightBits {

    //? if >=26.1.2 && <26.2-snapshot-8 {
    /*@Inject(method = "getLightCoords(Lnet/minecraft/world/level/BlockAndLightGetter;Lnet/minecraft/core/BlockPos;)I", at = @At("HEAD"), cancellable = true)
    private static void farLight(BlockAndLightGetter level, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        int light = Cuts.getTrimmedLight(level, pos);
        if (light >= 0) {
            cir.setReturnValue(light);
        }
    }
    *///?}
}
