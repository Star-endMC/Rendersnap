package rendersnap.star.end.client.render;

import it.unimi.dsi.fastutil.longs.Long2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
//? if >=26.1.2 {
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.BlockAndLightGetter;
//?} else {
/*import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.level.BlockAndTintGetter;
*///?}
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
//? if >=26.1.2 {
import net.minecraft.client.renderer.chunk.TranslucencyPointOfView;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import rendersnap.star.end.client.render.zoom.Zoom;

import java.util.List;

public final class Cuts {
    private static boolean blockFaceCulling;
    private static boolean textureLod;
    private static boolean entityCulling;
    private static boolean occlusionCulling;
    private static boolean fogOcclusion;
    private static int behindCamMode;
    private static boolean lightingChunkTrim;
    private static boolean hideWeather;
    private static boolean hideSun;
    private static boolean hideMoon;
    private static boolean fluidOptimizer;

    private static double cameraX;
    private static double cameraY;
    private static double cameraZ;
    private static float forwardX;
    private static float forwardY;
    private static float forwardZ;
    private static boolean hasCamera;
    private static boolean hasSections;
    private static int warmupTicks;

    private static final LongOpenHashSet SEEN_SECTIONS = new LongOpenHashSet(4096);
    private static final ThreadLocal<Long2IntLinkedOpenHashMap> LIGHT_CACHE = ThreadLocal.withInitial(Cuts::newLightCache);
    private static final ThreadLocal<BlockPos.MutableBlockPos> POS1 = ThreadLocal.withInitial(BlockPos.MutableBlockPos::new);
    private static final ThreadLocal<BlockPos.MutableBlockPos> POS2 = ThreadLocal.withInitial(BlockPos.MutableBlockPos::new);
    private static final ThreadLocal<Vector3f> FWD = ThreadLocal.withInitial(Vector3f::new);
    private static final Direction[] FLUID_LIGHT_DIRECTIONS = {
            Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.UP
    };

    private Cuts() {
    }

    public static void setBlockFaceCulling(boolean on) {
        if (blockFaceCulling != on) rebuildChunks();
        blockFaceCulling = on;
    }

    public static void setTextureLod(boolean on) {
        if (textureLod == on) return;
        textureLod = on;
        resetTerrainSampler();
    }

    public static void setEntityCulling(boolean on) {
        entityCulling = on;
    }

    public static void setOcclusionCulling(boolean on) {
        occlusionCulling = on;
    }

    public static void setFogOcclusion(boolean on) {
        fogOcclusion = on;
        Fog.setOcclusionEnabled(on);
    }

    public static void setBehindCamMode(int mode) {
        behindCamMode = Mth.clamp(mode, 0, 2);
    }

    public static void setLightingChunkTrim(boolean on) {
        if (lightingChunkTrim == on) return;
        lightingChunkTrim = on;
        LIGHT_CACHE.remove();
        rebuildChunks();
    }

    public static void setHideWeather(boolean on) {
        hideWeather = on;
    }

    public static boolean shouldHideWeather() {
        return hideWeather;
    }

    public static void setHideSun(boolean on) {
        hideSun = on;
    }

    public static boolean shouldHideSun() {
        return hideSun;
    }

    public static void setHideMoon(boolean on) {
        hideMoon = on;
    }

    public static boolean shouldHideMoon() {
        return hideMoon;
    }

    public static void setFluidOptimizer(boolean on) {
        if (fluidOptimizer != on) rebuildChunks();
        fluidOptimizer = on;
    }

    public static void warmup(int ticks) {
        warmupTicks = Math.max(warmupTicks, ticks);
        LIGHT_CACHE.remove();
        hasSections = false;
    }

    public static void stopWarmup() {
        warmupTicks = 0;
    }

    public static void clearWorldState() {
        warmupTicks = 0;
        hasCamera = false;
        hasSections = false;
        SEEN_SECTIONS.clear();
        LIGHT_CACHE.remove();
    }

    public static void tickWarmup() {
        if (warmupTicks > 0) warmupTicks--;
    }

    public static void updateCamera(Camera camera) {
        //? if >=26.1.2 {
        cameraX = camera.position().x;
        cameraY = camera.position().y;
        cameraZ = camera.position().z;
        Vector3fc f = camera.forwardVector();
        //?} else {
        /*cameraX = camera.getPosition().x;
        cameraY = camera.getPosition().y;
        cameraZ = camera.getPosition().z;
        Vector3fc f = camera.getLookVector();
        *///?}
        forwardX = f.x();
        forwardY = f.y();
        forwardZ = f.z();
        hasCamera = true;
    }

    public static void updateCamera(Vec3 pos, Quaternionfc rot) {
        cameraX = pos.x;
        cameraY = pos.y;
        cameraZ = pos.z;
        Vector3f f = FWD.get().set(0.0f, 0.0f, -1.0f).rotate(rot);
        forwardX = f.x();
        forwardY = f.y();
        forwardZ = f.z();
        hasCamera = true;
    }

    public static void seeSections(List<SectionRenderDispatcher.RenderSection> sections) {
        SEEN_SECTIONS.clear();
        for (int i = 0, n = sections.size(); i < n; i++) {
            //? if >=26.1.2 {
            BlockPos p = sections.get(i).getRenderOrigin();
            //?} else {
            /*BlockPos p = sections.get(i).getOrigin();
            *///?}
            SEEN_SECTIONS.add(SectionPos.asLong(p.getX() >> 4, p.getY() >> 4, p.getZ() >> 4));
        }
        hasSections = !SEEN_SECTIONS.isEmpty();
    }

    public static boolean shouldCullEntity(Entity e, Camera camera, double camX, double camY, double camZ) {
        if (!entityCulling && !behindCam()) return false;
        if (camera == null || e == null) return false;
        if (warming() || Zoom.isZooming()) return false;

        //? if >=26.1.2 {
        Entity camEnt = camera.entity();
        //?} else {
        /*Entity camEnt = camera.getEntity();
        *///?}
        if (e == camEnt || e instanceof Player) return false;
        if (e.isCurrentlyGlowing()) return false;
        if (e.isVehicle() || e.isPassenger()) return false;

        double dx = e.getX() - camX;
        double dy = e.getY() - camY;
        double dz = e.getZ() - camZ;
        double distSq = dx * dx + dy * dy + dz * dz;
        if (distSq < 1024.0) return false;

        double dist = Math.sqrt(distSq);
        if (behindCam() && distSq >= 1600.0 && behind(dx, dy, dz, dist)) return true;
        if (e instanceof LivingEntity living) {
            if (living.getHealth() <= 0.0f) return true;
        }
        if (!entityCulling || !hasSections) return false;

        int sx = Mth.floor(e.getX()) >> 4;
        int sy = Mth.floor(e.getY()) >> 4;
        int sz = Mth.floor(e.getZ()) >> 4;
        return !SEEN_SECTIONS.contains(SectionPos.asLong(sx, sy, sz));
    }

    public static boolean hidesJoinedFace(BlockState state, BlockState near) {
        if (warming()) return false;
        if (!blockFaceCulling) return false;
        if (near.isAir()) return false;

        if (!state.getFluidState().isEmpty()) return false;
        if (!near.getFluidState().isEmpty()) return false;
        if (!state.canOcclude()) return false;
        if (!near.canOcclude()) return false;
        //? if >=26.1.2 {
        return state.getBlock() == near.getBlock() && state.isSolidRender() && near.isSolidRender();
        //?} else {
        /*return state.getBlock() == near.getBlock();
        *///?}
    }

    public static boolean roughTerrainTextures() {
        if (warming()) return false;
        if (!textureLod) return false;
        return !Zoom.isZooming();
    }

    //? if >=26.1.2 {
    public static int getTrimmedLight(BlockAndLightGetter level, BlockPos pos) {
    //?} else {
    /*public static int getTrimmedLight(BlockAndTintGetter level, BlockPos pos) {
    *///?}
        if (warming()) return -1;
        if (!lightingChunkTrim) return -1;
        if (Zoom.isZooming()) return -1;
        if (!hasCamera) return -1;

        BlockState state = level.getBlockState(pos);
        if (!state.getFluidState().isEmpty()) return -1;
        if (!level.getBlockState(pos.above()).getFluidState().isEmpty()) return -1;

        double dx = pos.getX() + 0.5 - cameraX;
        double dy = pos.getY() + 0.5 - cameraY;
        double dz = pos.getZ() + 0.5 - cameraZ;
        double distSq = dx * dx + dy * dy + dz * dz;
        if (distSq < 36864.0) return -1;

        long key = pos.asLong();
        Long2IntLinkedOpenHashMap cache = LIGHT_CACHE.get();
        int light = cache.getAndMoveToFirst(key);
        if (light != -1) return light;

        if (cache.size() > 4096) cache.removeLastInt();
        int packed = packLight(level.getBrightness(LightLayer.BLOCK, pos), level.getBrightness(LightLayer.SKY, pos));
        cache.putAndMoveToFirst(key, packed);
        return packed;
    }

    public static int getOptimizedFluidLight(BlockAndTintGetter level, BlockPos pos, int original) {
        if (warming() || !fluidOptimizer) return original;

        int block = blockLight(original) * 2;
        int sky = skyLight(original) * 2;
        int samples = 2;
        BlockPos.MutableBlockPos p = POS1.get();
        BlockPos.MutableBlockPos up = POS2.get();

        for (Direction dir : FLUID_LIGHT_DIRECTIONS) {
            p.setWithOffset(pos, dir);
            up.setWithOffset(p, Direction.UP);
            int light = Math.max(levelLight(level, p), levelLight(level, up));
            block += blockLight(light);
            sky += skyLight(light);
            samples++;
        }
        return packLight(Math.max(blockLight(original), block / samples), Math.max(skyLight(original), sky / samples));
    }

    //? if >=26.1.2 {
    public static boolean shouldSkipFarTranslucencyResort(SectionRenderDispatcher.RenderSection section, TranslucencyPointOfView pov, Vec3 cam, boolean near) {
        if (warming()) return false;
        if (near) return false;
        if (!behindCam()) return false;
        if (cam == null || pov == null) return false;
        if (Zoom.isZooming()) return false;

        BlockPos p = section.getRenderOrigin();
        double dx = p.getX() + 8.0 - cam.x;
        double dy = p.getY() + 8.0 - cam.y;
        double dz = p.getZ() + 8.0 - cam.z;
        double distSq = dx * dx + dy * dy + dz * dz;
        return distSq >= 1600.0 && behind(dx, dy, dz, Math.sqrt(distSq));
    }
    //?}

    public static boolean shouldSkipTerrainSection(SectionRenderDispatcher.RenderSection section) {
        if (!cutsTerrainSections()) return false;

        //? if >=26.1.2 {
        BlockPos p = section.getRenderOrigin();
        //?} else {
        /*BlockPos p = section.getOrigin();
        *///?}
        double dx = p.getX() + 8.0 - cameraX;
        double dy = p.getY() + 8.0 - cameraY;
        double dz = p.getZ() + 8.0 - cameraZ;
        double distSq = dx * dx + dy * dy + dz * dz;
        double dist = Math.sqrt(distSq);

        if (fogOcclusion && Fog.isOccludingDistance((float)dist)) return true;
        return cutsTerrainBehindCam() && distSq >= 1600.0 && behind(dx, dy, dz, dist);
    }

    public static boolean cutsTerrainSections() {
        return !warming() && hasCamera && !Zoom.isZooming() && (fogOcclusion || behindCamMode >= 2);
    }

    public static boolean cutsTerrainBehindCam() {
        return !warming() && hasCamera && !Zoom.isZooming() && behindCamMode >= 2;
    }

    public static boolean useSectionOcclusion(boolean vanilla) {
        return !warming() && occlusionCulling && vanilla;
    }

    private static Long2IntLinkedOpenHashMap newLightCache() {
        Long2IntLinkedOpenHashMap map = new Long2IntLinkedOpenHashMap(512);
        map.defaultReturnValue(-1);
        return map;
    }

    //? if >=26.1.2 {
    private static int levelLight(BlockAndLightGetter level, BlockPos pos) {
    //?} else {
    /*private static int levelLight(BlockAndTintGetter level, BlockPos pos) {
    *///?}
        return packLight(level.getBrightness(LightLayer.BLOCK, pos), level.getBrightness(LightLayer.SKY, pos));
    }

    private static int packLight(int block, int sky) {
        //? if >=26.1.2 {
        return LightCoordsUtil.pack(block, sky);
        //?} else {
        /*return LightTexture.pack(block, sky);
        *///?}
    }

    private static int blockLight(int packed) {
        //? if >=26.1.2 {
        return LightCoordsUtil.block(packed);
        //?} else {
        /*return LightTexture.block(packed);
        *///?}
    }

    private static int skyLight(int packed) {
        //? if >=26.1.2 {
        return LightCoordsUtil.sky(packed);
        //?} else {
        /*return LightTexture.sky(packed);
        *///?}
    }

    private static boolean warming() {
        return warmupTicks > 0;
    }

    private static boolean behindCam() {
        return behindCamMode > 0;
    }

    private static boolean behind(double dx, double dy, double dz, double dist) {
        if (dist <= 0.001) return false;
        return (dx * forwardX + dy * forwardY + dz * forwardZ) / dist < -0.35;
    }

    private static void rebuildChunks() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            //? if >=26.2-snapshot-8 {
            mc.levelRenderer.resetLevelRenderData();
            //?} else
            //mc.levelRenderer.allChanged();
        }
    }

    private static void resetTerrainSampler() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.levelRenderer != null) {
            //? if >=26.2-snapshot-8 {
            mc.levelRenderer.resetLevelRenderData();
            //?} else if >=26.1.2 {
            /*mc.levelRenderer.resetSampler();
            *///?} else {
            /*mc.levelRenderer.allChanged();
            *///?}
        }
    }
}
