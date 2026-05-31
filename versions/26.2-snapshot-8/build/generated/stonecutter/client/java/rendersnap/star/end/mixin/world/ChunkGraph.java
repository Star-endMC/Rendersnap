package rendersnap.star.end.mixin.world;

import rendersnap.star.end.client.render.Cuts;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
//? if >=26.1.2 {
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.chunk.TranslucencyPointOfView;
//?}
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
//? if >=26.2-snapshot-8 {
import net.minecraft.client.renderer.state.level.CameraRenderState;
//?}
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.joml.Matrix4fc;

@Mixin(LevelRenderer.class)
public abstract class ChunkGraph {

    @Shadow @Final
    private ObjectArrayList<SectionRenderDispatcher.RenderSection> visibleSections;

    @Unique
    private final ObjectArrayList<SectionRenderDispatcher.RenderSection> hiddenSections = new ObjectArrayList<>();
    @Unique
    private final IntArrayList hiddenSectionSlots = new IntArrayList();

    //? if >=26.1.2 && <26.2-snapshot-8 {
    /*@Inject(method = "cullTerrain", at = @At("RETURN"))
    private void cam(Camera camera, net.minecraft.client.renderer.culling.Frustum frustum, boolean captured, CallbackInfo ci) {
        Cuts.updateCamera(camera);
    }
    *///?}

    //? if >=26.2-snapshot-8 {
    @Inject(method = "compileSections", at = @At("RETURN"))
    private void cam(CameraRenderState cam, CallbackInfo ci) {
        Cuts.updateCamera(cam.pos, cam.orientation);
        cam.smartCull = Cuts.useSectionOcclusion(cam.smartCull);
    }
    //?}

    //? if >=26.1.2 {
    @Inject(method = "prepareChunkRenders", at = @At("HEAD"))
    private void cutTerrain(Matrix4fc mv, CallbackInfoReturnable<ChunkSectionsToRender> cir) {
        cutVisibleSections();
    }

    @Inject(method = "prepareChunkRenders", at = @At("RETURN"))
    private void putBack(Matrix4fc mv, CallbackInfoReturnable<ChunkSectionsToRender> cir) {
        restoreSections();
    }
    //?}

    //? if >=26.1.2 {
    @Inject(method = "scheduleResort", at = @At("HEAD"), cancellable = true)
    private void skipSort(SectionRenderDispatcher.RenderSection section, TranslucencyPointOfView pov, Vec3 cam, boolean moved, boolean near, CallbackInfo ci) {
        if (Cuts.shouldSkipFarTranslucencyResort(section, pov, cam, near)) {
            ci.cancel();
        }
    }
    //?}

    //? if >=26.1.2 && <26.2-snapshot-8 {
    /*@ModifyArg(
            method = "cullTerrain",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SectionOcclusionGraph;update(ZLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/culling/Frustum;Ljava/util/List;Lit/unimi/dsi/fastutil/longs/LongOpenHashSet;)V"),
            index = 0
    )
    private boolean smartCull(boolean smart) {
        return Cuts.useSectionOcclusion(smart);
    }
    *///?}

    //? if >=26.1.2 {
    @Unique
    private void cutVisibleSections() {
        this.hiddenSections.clear();
        this.hiddenSectionSlots.clear();
        Cuts.seeSections(this.visibleSections);
        if (!Cuts.cutsTerrainSections()) return;

        for (int i = this.visibleSections.size() - 1; i >= 0; i--) {
            SectionRenderDispatcher.RenderSection section = this.visibleSections.get(i);
            if (Cuts.shouldSkipTerrainSection(section)) {
                this.hiddenSectionSlots.add(i);
                this.hiddenSections.add(this.visibleSections.remove(i));
            }
        }
    }

    @Unique
    private void restoreSections() {
        for (int i = this.hiddenSections.size() - 1; i >= 0; i--) {
            this.visibleSections.add(this.hiddenSectionSlots.getInt(i), this.hiddenSections.get(i));
        }
        this.hiddenSections.clear();
        this.hiddenSectionSlots.clear();
    }
    //?}
}
