package rendersnap.star.end.mixin.world;

//? if >=26.3 {
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
//?} else {
/*import com.mojang.blaze3d.buffers.GpuBufferSlice;
*///?}
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.chunk.SectionMesh;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.ArrayList;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rendersnap.star.end.client.render.Cuts;

@Mixin(LevelRenderer.class)
public abstract class CG {
    @Unique private BlockPos rendersnap$sectionOrigin;
    @Unique private BlockPos rendersnap$layerOrigin;
    @Unique private SectionMesh rendersnap$lastMesh;
    @Unique private int rendersnap$visibleIndex;
    @Unique private long rendersnap$renderStarted;
    @Unique private final List<SectionRenderDispatcher.RenderSection> rendersnap$hiddenSections = new ArrayList<>();
    @Unique private final List<Integer> rendersnap$hiddenIndices = new ArrayList<>();
    @Unique private java.util.IdentityHashMap<SectionMesh, BlockPos> rendersnap$meshOrigins;

    //? if >=26.3 {
    @Inject(method = "render", at = @At("HEAD"), require = 0)
    private void rendersnap$updateCamera(
            GraphicsResourceAllocator allocator,
            boolean renderBlockOutline,
            CameraRenderState cameraState,
            GpuBufferSlice fog,
            Vector4f fogColor,
            boolean renderOutline,
            boolean blit,
            CallbackInfo ci
    ) {
        rendersnap$renderStarted = System.nanoTime();
        Cuts.updateCamera(cameraState.pos, cameraState.orientation);
    }
    @Inject(method = "render", at = @At("RETURN"), require = 0)
    private void rendersnap$recordRender(
            GraphicsResourceAllocator allocator,
            boolean renderBlockOutline,
            CameraRenderState cameraState,
            GpuBufferSlice fog,
            Vector4f fogColor,
            boolean renderOutline,
            boolean blit,
            CallbackInfo ci
    ) {
        if (rendersnap$renderStarted != 0L) {
            rendersnap.star.end.client.FrameMet.recordLevelRender(System.nanoTime() - rendersnap$renderStarted);
            rendersnap$renderStarted = 0L;
        }
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

    @Inject(method = "extractSectionDrawGroups", at = @At("HEAD"), require = 0)
    private void rendersnap$resetLayerCursor(boolean respectTranslucentOrder, List<?> sectionInfos, java.util.Map<?, ?> drawGroups, CallbackInfoReturnable<Integer> cir) {
        rendersnap$lastMesh = null;
        rendersnap$layerOrigin = null;
        rendersnap$visibleIndex = 0;
        rendersnap$meshOrigins = null;
    }

    @Redirect(
            method = "extractSectionDrawGroups",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/SectionMesh;getSectionDraw(Lnet/minecraft/client/renderer/chunk/ChunkSectionLayer;)Lnet/minecraft/client/renderer/chunk/SectionMesh$SectionDraw;"),
            require = 0
    )
    private SectionMesh.SectionDraw rendersnap$trimLayer(SectionMesh mesh, ChunkSectionLayer layer) {

        if (layer == ChunkSectionLayer.SOLID || !Cuts.trimsFarLayers()) return mesh.getSectionDraw(layer);
        if (mesh != rendersnap$lastMesh) {
            LevelRenderer renderer = (LevelRenderer) (Object) this;
            List<SectionRenderDispatcher.RenderSection> visible = renderer.visibleSections();
            while (rendersnap$visibleIndex < visible.size()
                    && visible.get(rendersnap$visibleIndex).getSectionMesh() != mesh) {
                rendersnap$visibleIndex++;
            }
            if (rendersnap$visibleIndex < visible.size()) {
                rendersnap$layerOrigin = visible.get(rendersnap$visibleIndex).getRenderOrigin();
            } else {

                if (rendersnap$meshOrigins == null) {
                    rendersnap$meshOrigins = new java.util.IdentityHashMap<>(visible.size() * 2);
                    for (SectionRenderDispatcher.RenderSection section : visible) {
                        rendersnap$meshOrigins.putIfAbsent(section.getSectionMesh(), section.getRenderOrigin());
                    }
                }
                rendersnap$layerOrigin = rendersnap$meshOrigins.get(mesh);
            }
            rendersnap$lastMesh = mesh;
        }
        if (Cuts.shouldSkipSectionLayer(rendersnap$layerOrigin, layer)) return null;
        return mesh.getSectionDraw(layer);
    }

    @Inject(method = "prepareChunkRenders", at = @At("HEAD"), require = 0)
    private void rendersnap$filterCaveSections(Matrix4fc modelViewMatrix, boolean respectTranslucentOrder, CallbackInfoReturnable<ChunkSectionsToRender> cir) {
        rendersnap$filterCaves();
    }

    @Inject(method = "prepareChunkRenders", at = @At("RETURN"), require = 0)
    private void rendersnap$restoreCaveSections(Matrix4fc modelViewMatrix, boolean respectTranslucentOrder, CallbackInfoReturnable<ChunkSectionsToRender> cir) {
        rendersnap$restoreCaves();
    }

    @Inject(method = "prepareChunkRendersIndirect", at = @At("HEAD"), require = 0)
    private void rendersnap$filterCaveSectionsIndirect(Matrix4fc modelViewMatrix, boolean respectTranslucentOrder, CallbackInfoReturnable<ChunkSectionsToRender> cir) {
        rendersnap$filterCaves();
    }

    @Inject(method = "prepareChunkRendersIndirect", at = @At("RETURN"), require = 0)
    private void rendersnap$restoreCaveSectionsIndirect(Matrix4fc modelViewMatrix, boolean respectTranslucentOrder, CallbackInfoReturnable<ChunkSectionsToRender> cir) {
        rendersnap$restoreCaves();
    }

    @Unique
    private void rendersnap$filterCaves() {

        rendersnap$restoreCaves();

        if (!Cuts.isCaveCullingActive()) return;
        LevelRenderer renderer = (LevelRenderer) (Object) this;
        List<SectionRenderDispatcher.RenderSection> visible = renderer.visibleSections();
        for (int i = visible.size() - 1; i >= 0; i--) {
            SectionRenderDispatcher.RenderSection section = visible.get(i);
            if (Cuts.shouldCullCaveSection(section.getRenderOrigin())) {
                rendersnap$hiddenSections.add(visible.remove(i));
                rendersnap$hiddenIndices.add(i);
            }
        }
    }

    @Unique
    private void rendersnap$restoreCaves() {
        if (rendersnap$hiddenSections.isEmpty()) return;
        LevelRenderer renderer = (LevelRenderer) (Object) this;
        List<SectionRenderDispatcher.RenderSection> visible = renderer.visibleSections();

        for (int k = rendersnap$hiddenSections.size() - 1; k >= 0; k--) {
            int index = rendersnap$hiddenIndices.get(k);
            SectionRenderDispatcher.RenderSection section = rendersnap$hiddenSections.get(k);
            if (index <= visible.size()) visible.add(index, section);
            else visible.add(section);
        }
        rendersnap$hiddenSections.clear();
        rendersnap$hiddenIndices.clear();
    }

    //?}
}
