package rendersnap.star.end.mixin.client;

import net.minecraft.client.GraphicsPreset;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rendersnap.star.end.client.GraphicsPresetGuard;

@Mixin(Options.class)
public abstract class GraphicsPresetGuardMixin {
    @Inject(method = "applyGraphicsPreset", at = @At("HEAD"))
    private void rendersnap$prepareGraphicsTransition(GraphicsPreset preset, CallbackInfo ci) {
        GraphicsPresetGuard.beforeApply((Options) (Object) this);
    }

    @Inject(method = "applyGraphicsPreset", at = @At("RETURN"))
    private void rendersnap$capGraphicsPresetDistance(GraphicsPreset preset, CallbackInfo ci) {
        GraphicsPresetGuard.afterApply((Options) (Object) this);
    }
}
