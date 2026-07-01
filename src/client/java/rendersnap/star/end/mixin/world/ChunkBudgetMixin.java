package rendersnap.star.end.mixin.world;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.RenderRegionCache;
import net.minecraft.client.renderer.chunk.RenderSectionRegion;
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
import rendersnap.star.end.client.McCompat;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

@Mixin(LevelRenderer.class)
public abstract class ChunkBudgetMixin {
    @Shadow @Final private SectionRenderDispatcher sectionRenderDispatcher;

    @Unique
    private final Set<SectionRenderDispatcher.RenderSection> rendersnap$deferredRebuilds =
            Collections.newSetFromMap(new IdentityHashMap<>());

    @Inject(method = "compileSections", at = @At("HEAD"))
    private void rendersnap$beginCompileBudget(CallbackInfo ci) {
        this.rendersnap$deferredRebuilds.clear();
        ChunkBudget.beginPass(this.sectionRenderDispatcher.getCompileQueueSize());
    }

    @Redirect(
            method = "compileSections",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/SectionRenderDispatcher$RenderSection;compileSync(Lnet/minecraft/client/renderer/chunk/RenderSectionRegion;)V"),
            require = 0
    )
    private void rendersnap$budgetSyncCompile26(SectionRenderDispatcher.RenderSection section, RenderSectionRegion cache) {
        McCompat.sectionCompileSync(section, this.sectionRenderDispatcher, cache);
    }
    
    @Redirect(
            method = "compileSections",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/SectionRenderDispatcher$RenderSection;rebuildSectionAsync(Lnet/minecraft/client/renderer/chunk/SectionRenderDispatcher;Lnet/minecraft/client/renderer/chunk/RenderRegionCache;)V"),
            require = 0
    )
    private void rendersnap$budgetSyncCompileLegacy(SectionRenderDispatcher.RenderSection section, SectionRenderDispatcher dispatcher, RenderRegionCache cache) {
        boolean deferred = ChunkBudget.shouldDefer(
                this.sectionRenderDispatcher.getCompileQueueSize(),
                McCompat.sectionDirtyFromPlayer(section),
                section.getRenderOrigin()
        );
        if (deferred) {
            this.rendersnap$deferredRebuilds.add(section);
            return;
        }
        this.rendersnap$deferredRebuilds.remove(section);
        McCompat.sectionCompileSync(section, dispatcher, cache);
    }

    @Redirect(
            method = "compileSections",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/SectionRenderDispatcher$RenderSection;compileAsync(Lnet/minecraft/client/renderer/chunk/RenderSectionRegion;)V"),
            require = 0
    )
    private void rendersnap$budgetAsyncCompile26(SectionRenderDispatcher.RenderSection section, RenderSectionRegion cache) {
        McCompat.sectionCompileAsync(section, this.sectionRenderDispatcher, cache);
    }

    @Redirect(
            method = "compileSections",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/SectionRenderDispatcher$RenderSection;rebuildSectionAsync(Lnet/minecraft/client/renderer/chunk/RenderRegionCache;)V"),
            require = 0
    )
    private void rendersnap$budgetAsyncCompileLegacy(SectionRenderDispatcher.RenderSection section, RenderRegionCache cache) {
        boolean deferred = ChunkBudget.shouldDefer(
                this.sectionRenderDispatcher.getCompileQueueSize(),
                McCompat.sectionDirtyFromPlayer(section),
                section.getRenderOrigin()
        );
        if (deferred) {
            this.rendersnap$deferredRebuilds.add(section);
            return;
        }
        this.rendersnap$deferredRebuilds.remove(section);
        McCompat.sectionCompileAsync(section, this.sectionRenderDispatcher, cache);
    }

    @Redirect(
            method = "compileSections",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/SectionRenderDispatcher$RenderSection;setNotDirty()V"),
            require = 0
    )
    private void rendersnap$keepDeferredSectionsDirty(SectionRenderDispatcher.RenderSection section) {
        if (this.rendersnap$deferredRebuilds.remove(section)) {
            McCompat.sectionSetDirty(section, false);
            return;
        }
        McCompat.sectionSetNotDirty(section);
    }

    @Inject(method = "compileSections", at = @At("RETURN"))
    private void rendersnap$endCompileBudget(CallbackInfo ci) {
        this.rendersnap$deferredRebuilds.clear();
    }
}
