package rendersnap.star.end.mixin.cam;

import rendersnap.star.end.client.render.Fog;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
//? if >=26.1.2 {
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.FogRenderer;
//?} else {
/*import net.minecraft.client.renderer.FogRenderer;
*///?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FogRenderer.class)
public abstract class FogPass {

    //? if >=26.1.2 {
    @Inject(method = "setupFog", at = @At("RETURN"))
    private void pullFog(
            Camera camera,
            int viewDistance,
            DeltaTracker deltaTracker,
            float skyDarken,
            ClientLevel level,
            CallbackInfoReturnable<FogData> cir
    ) {
        Fog.readVanillaFog(cir.getReturnValue());
    }
    //?}
}
