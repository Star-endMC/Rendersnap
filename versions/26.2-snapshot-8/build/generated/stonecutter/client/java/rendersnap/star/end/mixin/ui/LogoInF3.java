package rendersnap.star.end.mixin.ui;

import net.fabricmc.loader.api.FabricLoader;
//? if >=26.1.2 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} else {
/*import net.minecraft.client.gui.GuiGraphics;
*///?}
import net.minecraft.client.gui.components.DebugScreenOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(DebugScreenOverlay.class)
public abstract class LogoInF3 {
    private static final String TEXT = "\u00A7c\u00A7lRendersnap " + FabricLoader.getInstance()
            .getModContainer("rendersnap")
            .map(m -> m.getMetadata().getVersion().getFriendlyString())
            .orElse("");

    //? if >=26.1.2 {
    @Inject(method = "extractLines", at = @At("HEAD"))
    private void add(GuiGraphicsExtractor graphics, List<String> lines, boolean left, CallbackInfo ci) {
    //?} else {
    /*@Inject(method = "renderLines", at = @At("HEAD"))
    private void add(GuiGraphics graphics, List<String> lines, boolean left, CallbackInfo ci) {
    *///?}
        if (!left) return;
        lines.add(lines.isEmpty() ? 0 : 1, TEXT);
    }
}
