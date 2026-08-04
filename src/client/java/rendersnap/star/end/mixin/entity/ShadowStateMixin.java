package rendersnap.star.end.mixin.entity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rendersnap.star.end.client.render.ShadowReuse;

@Mixin(EntityRenderer.class)
public abstract class ShadowStateMixin {
    @Unique private Entity rendersnap$entity;

    @Inject(
            method = "createRenderState(Lnet/minecraft/world/entity/Entity;F)Lnet/minecraft/client/renderer/entity/state/EntityRenderState;",
            at = @At("HEAD")
    )
    private void rendersnap$captureEntity(Entity e, float partialTick, CallbackInfoReturnable<EntityRenderState> cir) {
        this.rendersnap$entity = e;
    }

    @Inject(method = "extractShadow", at = @At("HEAD"), cancellable = true)
    private void rendersnap$reuseSameTickShadow(EntityRenderState state, Minecraft mc, Level level, CallbackInfo ci) {
        if (ShadowReuse.restore(this.rendersnap$entity, state, level)) {
            ci.cancel();
        }
    }

    @Inject(method = "extractShadow", at = @At("TAIL"))
    private void rendersnap$storeSameTickShadow(EntityRenderState state, Minecraft mc, Level level, CallbackInfo ci) {
        ShadowReuse.capture(this.rendersnap$entity, state, level);
    }
}
