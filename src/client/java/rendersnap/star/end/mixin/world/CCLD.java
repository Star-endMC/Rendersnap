package rendersnap.star.end.mixin.world;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import rendersnap.star.end.client.ChunkO;

//? if >=26.2 {
@Mixin(targets = {
        "net.minecraft.client.renderer.chunk.SectionRenderDispatcher$RenderSection$CompileTask",
        "net.minecraft.client.renderer.chunk.SectionRenderDispatcher$RenderSection$ResortTransparencyTask"
})
//?} else {
/*@Mixin(targets = {
        "net.minecraft.client.renderer.chunk.SectionRenderDispatcher$RenderSection$RebuildTask",
        "net.minecraft.client.renderer.chunk.SectionRenderDispatcher$RenderSection$ResortTransparencyTask"
})
*///?}
public abstract class CCLD {
    @Redirect(method = "doTask", at = @At(value = "INVOKE", target = "Ljava/lang/Thread;onSpinWait()V"), require = 0)
    private void rendersnap$sleepInsteadOfSpin() {
        ChunkO.waitForUploadSpace();
    }
}
