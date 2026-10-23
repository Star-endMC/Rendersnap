package rendersnap.star.end.mixin.world;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
//? if >=26.2 {
import net.minecraft.client.renderer.chunk.RenderSectionRegion;
//?} else {
/*import net.minecraft.client.renderer.chunk.RenderRegionCache;
*///?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rendersnap.star.end.client.ReduceSP;

@Mixin(LevelRenderer.class)
public abstract class RCH {
    @Inject(method = "compileSections", at = @At("HEAD"), require = 0)
    private void rendersnap$resetRebuildBudget(CallbackInfo ci) {
        ReduceSP.beginRebuildFrame();
    }

    //? if >=26.2 {
    @Redirect(
            method = "compileSections",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/SectionRenderDispatcher$RenderSection;compileSync(Lnet/minecraft/client/renderer/chunk/RenderSectionRegion;)V"),
            require = 0
    )
    private void rendersnap$guardSyncRebuild(SectionRenderDispatcher.RenderSection section, RenderSectionRegion region) {
        if (ReduceSP.allowSyncRebuild()) {
            long start = System.nanoTime();
            section.compileSync(region);
            ReduceSP.syncRebuildDone(System.nanoTime() - start);
        } else {
            section.compileAsync(region);
        }
    }
    //?} else {
    /*@Redirect(
            method = "compileSections",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/SectionRenderDispatcher;rebuildSectionSync(Lnet/minecraft/client/renderer/chunk/SectionRenderDispatcher$RenderSection;Lnet/minecraft/client/renderer/chunk/RenderRegionCache;)V"),
            require = 0
    )
    private void rendersnap$guardSyncRebuild(SectionRenderDispatcher dispatcher, SectionRenderDispatcher.RenderSection section, RenderRegionCache cache) {
        if (ReduceSP.allowSyncRebuild()) {
            long start = System.nanoTime();
            dispatcher.rebuildSectionSync(section, cache);
            ReduceSP.syncRebuildDone(System.nanoTime() - start);
        } else {
            section.rebuildSectionAsync(cache);
        }
    }
    *///?}
}
