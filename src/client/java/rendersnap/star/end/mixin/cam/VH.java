package rendersnap.star.end.mixin.cam;

import rendersnap.star.end.client.render.zoom.Zoom;
import net.minecraft.client.renderer.GameRenderer;
//? if >=26.3 {
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import com.mojang.renderpearl.api.textures.GpuTextureView;
//?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class VH {

    //? if >=26.3 {
    @Inject(method = "renderItemInHand", at = @At("HEAD"), cancellable = true)
    private void hideHand(CameraRenderState cameraState, PlayerRenderState playerState, GpuTextureView textureView, CallbackInfo ci) {
        if (Zoom.isZooming()) {
            ci.cancel();
        }
    }
    //?} else {
    /*@Inject(method = "renderItemInHand", at = @At("HEAD"), cancellable = true)
    private void hideHand(net.minecraft.client.renderer.state.level.CameraRenderState cameraState, float deltaPartialTick, org.joml.Matrix4fc modelViewMatrix, CallbackInfo ci) {
        if (Zoom.isZooming()) {
            ci.cancel();
        }
    }
    *///?}
}
