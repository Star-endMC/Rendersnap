package rendersnap.star.end.mixin.world;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.SectionMesh;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rendersnap.star.end.client.render.Cuts;

@Mixin(LevelRenderer.class)
public abstract class ChunkGraph {
    @Unique private BlockPos rendersnap$sectionOrigin;

    @Inject(method = "render", at = @At("HEAD"))
    private void rendersnap$updateCamera(
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

    @Inject(method = "scheduleResort", at = @At("HEAD"), cancellable = true)
    private void rendersnap$skipFarResort(
            SectionRenderDispatcher.RenderSection section,
            net.minecraft.client.renderer.chunk.TranslucencyPointOfView pov,
            Vec3 camera,
            boolean moved,
            boolean near,
            CallbackInfo ci
    ) {
        if (Cuts.shouldSkipFarTranslucencyResort(section, camera, near)) {
            ci.cancel();
        }
    }

    @Redirect(
            method = "prepareChunkRenders",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/SectionRenderDispatcher$RenderSection;getRenderOrigin()Lnet/minecraft/core/BlockPos;")
    )
    private BlockPos rendersnap$captureSectionOrigin(SectionRenderDispatcher.RenderSection section) {
        this.rendersnap$sectionOrigin = section.getRenderOrigin();
        return this.rendersnap$sectionOrigin;
    }

    @Redirect(
            method = "prepareChunkRenders",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/SectionMesh;getSectionDraw(Lnet/minecraft/client/renderer/chunk/ChunkSectionLayer;)Lnet/minecraft/client/renderer/chunk/SectionMesh$SectionDraw;")
    )
    private SectionMesh.SectionDraw rendersnap$trimFarLayers(SectionMesh mesh, ChunkSectionLayer layer) {
        if (Cuts.trimsFarLayers() && Cuts.shouldSkipSectionLayer(this.rendersnap$sectionOrigin, layer)) {
            return null;
        }
        return mesh.getSectionDraw(layer);
    }

    @Redirect(
            method = "prepareChunkRenders",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/SectionRenderDispatcher;getRenderSectionSlice(Lnet/minecraft/client/renderer/chunk/SectionMesh;Lnet/minecraft/client/renderer/chunk/ChunkSectionLayer;)Lnet/minecraft/client/renderer/chunk/SectionRenderDispatcher$RenderSectionBufferSlice;")
    )
    private SectionRenderDispatcher.RenderSectionBufferSlice rendersnap$trimFarSlice(
            SectionRenderDispatcher dispatcher,
            SectionMesh mesh,
            ChunkSectionLayer layer
    ) {
        if (Cuts.trimsFarLayers() && Cuts.shouldSkipSectionLayer(this.rendersnap$sectionOrigin, layer)) {
            return null;
        }
        return dispatcher.getRenderSectionSlice(mesh, layer);
    }
}
