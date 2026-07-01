package rendersnap.star.end.mixin.world;

import rendersnap.star.end.client.render.Cuts;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
//? if >=26.1.2 {
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.chunk.SectionMesh;
import net.minecraft.client.renderer.chunk.TranslucencyPointOfView;
//?}
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
//? if >=26.2 {
import net.minecraft.client.renderer.state.level.CameraRenderState;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import rendersnap.star.end.client.PreparedChunkCache;

@Mixin(LevelRenderer.class)
public abstract class ChunkGraph {

    @Shadow @Final
    private ObjectArrayList<SectionRenderDispatcher.RenderSection> visibleSections;

    private BlockPos rendersnap$sectionOrigin;
    private int rendersnap$vanillaVisibleSections;
    private int rendersnap$trimmedVisibleSections;
    private int rendersnap$finalVisibleSections;

    //? if >=26.1.2 {
    @Inject(method = "cullTerrain", at = @At("RETURN"), require = 0)
    private void cam(Camera camera, net.minecraft.client.renderer.culling.Frustum frustum, boolean captured, CallbackInfo ci) {
        Cuts.updateCamera(camera);
        cutVisibleSections();
    }

    @Inject(method = "scheduleResort", at = @At("HEAD"), cancellable = true)
    private void skipSort(SectionRenderDispatcher.RenderSection section, TranslucencyPointOfView pov, Vec3 cam, boolean moved, boolean near, CallbackInfo ci) {
        if (!Cuts.shouldCheckFarTranslucencyResort(near)) return;
        if (Cuts.shouldSkipFarTranslucencyResort(section, pov, cam, near)) {
            ci.cancel();
        }
    }

    @Inject(method = "render", at = @At("HEAD"), require = 0)
    private void rendersnap$updateCamera26(
            GraphicsResourceAllocator allocator,
            DeltaTracker deltaTracker,
            boolean renderBlockOutline,
            CameraRenderState cameraState,
            Matrix4fc modelViewMatrix,
            GpuBufferSlice fog,
            Vector4f fogColor,
            boolean renderOutline,
            CallbackInfo ci
    ) {
        Cuts.updateCamera(cameraState.pos, cameraState.orientation);
    }

    @org.spongepowered.asm.mixin.injection.Redirect(
            method = "prepareChunkRenders",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/SectionRenderDispatcher$RenderSection;getRenderOrigin()Lnet/minecraft/core/BlockPos;")
    )
    private BlockPos rendersnap$captureSectionOrigin(SectionRenderDispatcher.RenderSection section) {
        this.rendersnap$sectionOrigin = section.getRenderOrigin();
        return this.rendersnap$sectionOrigin;
    }

    @org.spongepowered.asm.mixin.injection.Redirect(
            method = "prepareChunkRenders",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/SectionMesh;getSectionDraw(Lnet/minecraft/client/renderer/chunk/ChunkSectionLayer;)Lnet/minecraft/client/renderer/chunk/SectionMesh$SectionDraw;")
    )
    private SectionMesh.SectionDraw rendersnap$trimFarLayers(SectionMesh mesh, ChunkSectionLayer layer) {
        SectionMesh.SectionDraw vanilla = mesh.getSectionDraw(layer);
        BlockPos origin = this.rendersnap$sectionOrigin;
        if (Cuts.trimsFarLayers() && origin != null && Cuts.shouldSkipSectionLayer(origin, layer)) {
            return null;
        }
        return vanilla;
    }

    @org.spongepowered.asm.mixin.injection.Redirect(
            method = "prepareChunkRenders",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/SectionRenderDispatcher;getRenderSectionSlice(Lnet/minecraft/client/renderer/chunk/SectionMesh;Lnet/minecraft/client/renderer/chunk/ChunkSectionLayer;)Lnet/minecraft/client/renderer/chunk/SectionRenderDispatcher$RenderSectionBufferSlice;")
    )
    private SectionRenderDispatcher.RenderSectionBufferSlice rendersnap$trimFarSlice(SectionRenderDispatcher dispatcher, SectionMesh mesh, ChunkSectionLayer layer) {
        BlockPos origin = this.rendersnap$sectionOrigin;
        if (Cuts.trimsFarLayers() && origin != null && Cuts.shouldSkipSectionLayer(origin, layer)) {
            return null;
        }
        return dispatcher.getRenderSectionSlice(mesh, layer);
    }

    @Inject(method = "prepareChunkRenders", at = @At("HEAD"), cancellable = true)
    private void rendersnap$usePreparedCache(Matrix4fc frustumMatrix, CallbackInfoReturnable<ChunkSectionsToRender> cir) {
        cutVisibleSections();
        Cuts.publishVisibleCounts(this.rendersnap$vanillaVisibleSections, this.rendersnap$trimmedVisibleSections, this.rendersnap$finalVisibleSections);
        ChunkSectionsToRender prepared = PreparedChunkCache.get(this.visibleSections);
        if (prepared != null) {
            cir.setReturnValue(prepared);
        }
    }

    @Inject(method = "prepareChunkRenders", at = @At("RETURN"), cancellable = true)
    private void rendersnap$storePreparedCache(Matrix4fc frustumMatrix, CallbackInfoReturnable<ChunkSectionsToRender> cir) {
        ChunkSectionsToRender prepared = Cuts.aggregateCutoutDraws(cir.getReturnValue());
        PreparedChunkCache.put(this.visibleSections, prepared);
        if (prepared != cir.getReturnValue()) {
            cir.setReturnValue(prepared);
        }
    }

    private void cutVisibleSections() {
        this.rendersnap$vanillaVisibleSections = this.visibleSections.size();
        this.rendersnap$trimmedVisibleSections = 0;
        if (Cuts.shouldCheckTerrainSections()) {
            for (int i = this.visibleSections.size() - 1; i >= 0; i--) {
                SectionRenderDispatcher.RenderSection section = this.visibleSections.get(i);
                if (Cuts.shouldSkipTerrainSection(section)) {
                    this.visibleSections.remove(i);
                    this.rendersnap$trimmedVisibleSections++;
                }
            }
        }

        this.rendersnap$finalVisibleSections = this.visibleSections.size();
        Cuts.seeSections(this.visibleSections);
        PreparedChunkCache.captureVisibleSections(this.visibleSections);
    }
}
