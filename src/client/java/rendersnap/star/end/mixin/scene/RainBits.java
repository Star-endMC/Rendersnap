package rendersnap.star.end.mixin.scene;

import rendersnap.star.end.client.render.Cuts;
//? if >=26.1.2 {
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.WeatherEffectRenderer;
import net.minecraft.client.renderer.state.level.WeatherRenderState;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.world.phys.Vec3;
//?} else {
/*import net.minecraft.client.renderer.LevelRenderer;
*///?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//? if >=26.1.2 {
@Mixin(WeatherEffectRenderer.class)
//?} else {
/*@Mixin(LevelRenderer.class)
*///?}
public abstract class RainBits {

    @Inject(method = "extractRenderState", at = @At("RETURN"), require = 0)
    private void hideWeather(ClientLevel level, float partialTicks, Vec3 cameraPos, WeatherRenderState renderState, CallbackInfo ci) {
        if (Cuts.shouldHideWeather()) {
            renderState.intensity = 0.0f;
            renderState.rainColumns.clear();
            renderState.snowColumns.clear();
        }
    }

    //? if >=26.1.2 && <26.2 {
    @Inject(method = "tickRainParticles", at = @At("HEAD"), cancellable = true, require = 0)
    private void hideWeatherParticles(ClientLevel level, Camera camera, int ticks, ParticleStatus particleStatus, int weatherRadius, CallbackInfo ci) {
        if (Cuts.shouldHideWeather()) {
            ci.cancel();
        }
    }
    //?}
}
