package rendersnap.star.end.mixin.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rendersnap.star.end.client.render.zoom.Zoom;

@Mixin(Options.class)
public abstract class ZS {
    @Inject(method = "save", at = @At("HEAD"), require = 0)
    private void rendersnap$realControlsBeforeSave(CallbackInfo ci) {
        Zoom.beforeOptionsSave(Minecraft.getInstance());
    }

    @Inject(method = "save", at = @At("RETURN"), require = 0)
    private void rendersnap$zoomControlsAfterSave(CallbackInfo ci) {
        Zoom.afterOptionsSave(Minecraft.getInstance());
    }
}
