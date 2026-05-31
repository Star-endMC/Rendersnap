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
public abstract class MobRender {

    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private <E extends Entity> void skipHiddenMob(E entity, Frustum culler, double camX, double camY, double camZ, CallbackInfoReturnable<Boolean> cir) {
        EntityRenderDispatcher rd = (EntityRenderDispatcher)(Object)this;
        if (Cuts.shouldCullEntity(entity, rd.camera, camX, camY, camZ)) {
            cir.setReturnValue(false);
        }
    }
}
