package rendersnap.star.end.mixin.entity;

import rendersnap.star.end.client.render.Cuts;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderDispatcher.class)
public abstract class MR {

    //? if >=26.3 {
    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private <E extends Entity> void skipHiddenMob(E entity, Frustum culler, double camX, double camY, double camZ, float shadowDistance, CallbackInfoReturnable<Boolean> cir) {
        if (Cuts.shouldCullEntity(entity)) {
            cir.setReturnValue(false);
        }
    }
    //?} else {
    /*@Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private <E extends Entity> void skipHiddenMob(E entity, Frustum culler, double camX, double camY, double camZ, CallbackInfoReturnable<Boolean> cir) {
        if (Cuts.shouldCullEntity(entity)) {
            cir.setReturnValue(false);
        }
    }
    *///?}
}
