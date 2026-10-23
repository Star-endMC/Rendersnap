package rendersnap.star.end.mixin.ui;

import net.minecraft.client.gui.components.DebugScreenOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rendersnap.star.end.client.FrameMet;

@Mixin(DebugScreenOverlay.class)
public abstract class FC {
    @Inject(method = "logFrameDuration", at = @At("HEAD"))
    private void rendersnap$recordFullFrame(long frameDurationNs, CallbackInfo ci) {
        FrameMet.recordFrame(frameDurationNs);
    }
}
