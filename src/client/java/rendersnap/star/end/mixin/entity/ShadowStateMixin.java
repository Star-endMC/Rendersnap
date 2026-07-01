package rendersnap.star.end.mixin.entity;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rendersnap.star.end.client.render.Cuts;

@Mixin(EntityRenderer.class)
public abstract class ShadowStateMixin {

    @Inject(method = "finalizeRenderState", at = @At("HEAD"), cancellable = true)
    private <T extends Entity, S extends EntityRenderState> void rendersnap$reuseShadowState(T entity, S state, CallbackInfo ci) {
        if (Cuts.restoreShadowState(entity, state)) {
            ci.cancel();
        }
    }

    @Inject(method = "finalizeRenderState", at = @At("RETURN"))
    private <T extends Entity, S extends EntityRenderState> void rendersnap$storeShadowState(T entity, S state, CallbackInfo ci) {
        Cuts.storeShadowState(entity, state);
    }
}
