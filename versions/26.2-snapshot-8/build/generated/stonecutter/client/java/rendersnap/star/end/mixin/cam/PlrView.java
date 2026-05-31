package rendersnap.star.end.mixin.cam;

import rendersnap.star.end.client.render.zoom.Zoom;
//? if >=26.1.2 {
import net.minecraft.client.Camera;
//?} else {
/*import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
*///?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

//? if >=26.1.2 {
@Mixin(Camera.class)
//?} else {
/*@Mixin(GameRenderer.class)
*///?}
public abstract class PlrView {

    //? if >=26.1.2 {
    @Inject(method = "calculateFov", at = @At("RETURN"), cancellable = true)
    private void zoomFov(float partialTicks, CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(Zoom.modifyFov(cir.getReturnValue()));
    }
    //?} else {
    /*@Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void zoomFov(Camera camera, float tickDelta, boolean changingFov, CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(Zoom.modifyFov(cir.getReturnValue()));
    }
    *///?}
}
