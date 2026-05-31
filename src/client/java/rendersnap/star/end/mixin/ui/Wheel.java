package rendersnap.star.end.mixin.ui;

import rendersnap.star.end.client.render.zoom.Zoom;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class Wheel {

    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void zoomWheel(long handle, double xoffset, double yoffset, CallbackInfo ci) {
        //? if >=26.1.2 {
        if (handle != this.minecraft.getWindow().handle()) return;
        //?} else {
        /*if (handle != this.minecraft.getWindow().getWindow()) return;
        *///?}
        //? if >=26.2-snapshot-8 {
        /*if (this.minecraft.gui.screen() != null) return;
        *///?} else
        if (this.minecraft.screen != null) return;
        if (!Zoom.handleScroll(yoffset)) return;

        ci.cancel();
    }
}
