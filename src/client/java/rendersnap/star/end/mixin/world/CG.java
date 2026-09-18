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

    //?}
}
