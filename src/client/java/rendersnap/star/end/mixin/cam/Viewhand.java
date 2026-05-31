package rendersnap.star.end.mixin.cam;

import rendersnap.star.end.client.render.zoom.Zoom;
import net.minecraft.client.renderer.GameRenderer;
//? if >=26.2-snapshot-8 {
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4fc;
//?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class Viewhand {

    //? if >=26.2-snapshot-8 {
    @Inject(method = "renderItemInHand", at = @At("HEAD"), cancellable = true)
    private void hideHand(CameraRenderState cameraState, float deltaPartialTick, Matrix4fc modelViewMatrix, CallbackInfo ci) {
        if (Zoom.isZooming()) {
            ci.cancel();
        }
    }
    //?}
}
