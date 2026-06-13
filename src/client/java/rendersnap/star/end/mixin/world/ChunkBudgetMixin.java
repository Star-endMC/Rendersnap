package rendersnap.star.end.mixin.world;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.RenderRegionCache;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rendersnap.star.end.client.ChunkBudget;

@Mixin(LevelRenderer.class)
public abstract class ChunkBudgetMixin {
    @Shadow @Final private SectionRenderDispatcher sectionRenderDispatcher;

    @Unique
    private boolean rendersnap$deferredRebuild;

    @Inject(method = "compileSections", at = @At("HEAD"))
    private void rendersnap$beginCompileBudget(CallbackInfo ci) {
        ChunkBudget.beginPass(this.sectionRenderDispatcher.getCompileQueueSize());
    }

    @Redirect(
            method = "compileSections",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/SectionRenderDispatcher$RenderSection;rebuildSectionAsync(Lnet/minecraft/client/renderer/chunk/SectionRenderDispatcher;Lnet/minecraft/client/renderer/chunk/RenderRegionCache;)V"),
            require = 0
    )
    private void rendersnap$budgetAsyncCompile(SectionRenderDispatcher.RenderSection section, SectionRenderDispatcher dispatcher, RenderRegionCache cache) {
        this.rendersnap$deferredRebuild = ChunkBudget.shouldDefer(
                this.sectionRenderDispatcher.getCompileQueueSize(),
                section.isDirtyFromPlayer(),
                section.getRenderOrigin()
        );
        if (!this.rendersnap$deferredRebuild) {
            section.rebuildSectionAsync(cache);
        }
    }

    @Redirect(
            method = "compileSections",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/SectionRenderDispatcher$RenderSection;rebuildSectionAsync(Lnet/minecraft/client/renderer/chunk/RenderRegionCache;)V"),
            require = 0
    )
    private void rendersnap$budgetAsyncCompile(SectionRenderDispatcher.RenderSection section, RenderRegionCache cache) {
        this.rendersnap$deferredRebuild = ChunkBudget.shouldDefer(
                this.sectionRenderDispatcher.getCompileQueueSize(),
                section.isDirtyFromPlayer(),
                section.getRenderOrigin()
        );
        if (!this.rendersnap$deferredRebuild) {
            section.rebuildSectionAsync(cache);
        }
    }

    @Redirect(
            method = "compileSections",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/SectionRenderDispatcher$RenderSection;setNotDirty()V"),
            require = 0
    )
    private void rendersnap$keepDeferredSectionsDirty(SectionRenderDispatcher.RenderSection section) {
        if (this.rendersnap$deferredRebuild) {
            this.rendersnap$deferredRebuild = false;
            section.setDirty(false);
            return;
        }
        section.setNotDirty();
    }
}
