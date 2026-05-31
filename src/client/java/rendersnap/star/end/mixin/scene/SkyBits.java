package rendersnap.star.end.mixin.scene;

import rendersnap.star.end.client.render.Cuts;
//? if >=26.1.2 {
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.world.level.MoonPhase;
//?} else {
/*import net.minecraft.client.renderer.LevelRenderer;
*///?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//? if >=26.1.2 {
@Mixin(SkyRenderer.class)
//?} else {
/*@Mixin(LevelRenderer.class)
*///?}
public abstract class SkyBits {

    //? if >=26.1.2 {
    @Inject(method = "renderSun", at = @At("HEAD"), cancellable = true)
    private void hideSun(float rainBrightness, PoseStack poseStack, CallbackInfo ci) {
        if (Cuts.shouldHideSun()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderMoon", at = @At("HEAD"), cancellable = true)
    private void hideMoon(MoonPhase moonPhase, float rainBrightness, PoseStack poseStack, CallbackInfo ci) {
        if (Cuts.shouldHideMoon()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderSunriseAndSunset", at = @At("HEAD"), cancellable = true)
    private void hideSunrise(PoseStack poseStack, float sunAngle, int sunriseAndSunsetColor, CallbackInfo ci) {
        if (Cuts.shouldHideSun()) {
            ci.cancel();
        }
    }
    //?}
}
