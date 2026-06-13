package rendersnap.star.end.client.render;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
//? if >=26.1.2 {
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.admany.quantified.api.CacheRequest;
import org.admany.quantified.api.QuantifiedAPI;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import rendersnap.star.end.Rendersnap;
import rendersnap.star.end.client.PreparedChunkCache;
import rendersnap.star.end.client.QuantifiedSupport;
import rendersnap.star.end.client.render.zoom.Zoom;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;

public final class Cuts {
    private static boolean blockFaceCulling;
    private static volatile boolean textureLod;
    private static volatile boolean chunkShadeTrim;
    private static volatile boolean farLayerTrim;
    private static volatile boolean entityCulling;
    private static volatile boolean occlusionCulling;
    private static volatile boolean fogOcclusion;
    private static volatile int behindCamMode;
    private static volatile boolean lightingChunkTrim;
    private static volatile boolean hideWeather;
    private static volatile boolean fluidOptimizer;
    private static volatile int warmupTicks;
    private static volatile long lightCacheTick = Long.MIN_VALUE;
    private static volatile long cameraSection = Long.MIN_VALUE;
    private static volatile boolean terrainCullSuppressed;
    private static volatile boolean translucencyResortSuppressed;
    private static volatile boolean lightTrimSuppressed;
    private static volatile boolean sectionLayerSuppressed;
    private static volatile int vanillaVisibleSections;
    private static volatile int trimmedVisibleSections;
    private static volatile int finalVisibleSections;
    private static double stateCameraX;
    private static double stateCameraY;
    private static double stateCameraZ;
    private static float stateForwardX;
    private static float stateForwardY;
    private static float stateForwardZ;
    private static LongOpenHashSet stateSeenSections = new LongOpenHashSet(0);

    private static final AtomicReference<ViewState> VIEW = new AtomicReference<>(ViewState.EMPTY);
    private static final AtomicLong LIGHT_CACHE_EPOCH = new AtomicLong();
    private static final ThreadLocal<BlockPos.MutableBlockPos> POS1 = ThreadLocal.withInitial(BlockPos.MutableBlockPos::new);
    private static final ThreadLocal<BlockPos.MutableBlockPos> POS2 = ThreadLocal.withInitial(BlockPos.MutableBlockPos::new);
    private static final ThreadLocal<Vector3f> FWD = ThreadLocal.withInitial(Vector3f::new);
    private static final Duration LIGHT_CACHE_TTL = Duration.ofSeconds(20);
    private static final Direction[] FLUID_LIGHT_DIRECTIONS = {
            Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.UP
    };
    private static final Long2ByteOpenHashMap sectionLayerCutoutCache = new Long2ByteOpenHashMap();
    private static final Long2ByteOpenHashMap terrainSectionCache = new Long2ByteOpenHashMap();
    private static final Long2ByteOpenHashMap lightSectionCache = new Long2ByteOpenHashMap();
    private static final Long2ByteOpenHashMap fluidSectionCache = new Long2ByteOpenHashMap();
    private static final LongAdder entityCullChecks = new LongAdder();
    private static final LongAdder entityCullSkips = new LongAdder();
    private static final LongAdder chunkAoChecks = new LongAdder();
    private static final LongAdder chunkAoSkips = new LongAdder();
    private static final LongAdder sectionLayerChecks = new LongAdder();
    private static final LongAdder sectionLayerSkips = new LongAdder();
    private static final LongAdder terrainSectionChecks = new LongAdder();
    private static final LongAdder terrainSectionSkips = new LongAdder();
    private static final LongAdder sectionLayerCacheHits = new LongAdder();
    private static final LongAdder sectionLayerSuppressedChecks = new LongAdder();
    private static final LongAdder terrainSectionCacheHits = new LongAdder();
    private static final LongAdder terrainCullSuppressedChecks = new LongAdder();
    private static final LongAdder lightTrimCalls = new LongAdder();
    private static final LongAdder lightTrimEligible = new LongAdder();
    private static final LongAdder lightTrimRejectedNear = new LongAdder();
    private static final LongAdder lightTrimSuppressedCalls = new LongAdder();
    private static final LongAdder fluidLightCalls = new LongAdder();
    private static final LongAdder fluidLightRejectedFar = new LongAdder();
    private static final LongAdder translucencyResortChecks = new LongAdder();
    private static final LongAdder translucencyResortSkips = new LongAdder();
    private static final LongAdder translucencyResortSuppressedChecks = new LongAdder();

    private Cuts() {
        sectionLayerCutoutCache.defaultReturnValue((byte)-1);
        terrainSectionCache.defaultReturnValue((byte)-1);
        lightSectionCache.defaultReturnValue((byte)-1);
        fluidSectionCache.defaultReturnValue((byte)-1);
    }

    public static void setBlockFaceCulling(boolean on) {
        blockFaceCulling = on;
    }

    public static void setTextureLod(boolean on) {
        if (textureLod == on) return;
        textureLod = on;
        resetTerrainSampler();
    }

    public static void setChunkShadeTrim(boolean on) {
        if (chunkShadeTrim == on) return;
        chunkShadeTrim = on;
        rebuildChunks();
    }

    public static void setFarLayerTrim(boolean on) {
        if (farLayerTrim == on) return;
        farLayerTrim = on;
        rebuildChunks();
    }

    public static boolean trimsFarLayers() {
        return farLayerTrim;
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
        clearSectionCaches();
        clearAdaptiveSuppressions();
    }

    public static void setBehindCamMode(int mode) {
        behindCamMode = Mth.clamp(mode, 0, 2);
        clearSectionCaches();
        clearAdaptiveSuppressions();
    }

    public static void setLightingChunkTrim(boolean on) {
        if (lightingChunkTrim == on) return;
        lightingChunkTrim = on;
        bumpLightCacheEpoch();
        rebuildChunks();
    }

    public static void setHideWeather(boolean on) {
        hideWeather = on;
    }

    public static boolean shouldHideWeather() {
        return hideWeather;
    }

    public static void setFluidOptimizer(boolean on) {
        if (fluidOptimizer != on) rebuildChunks();
        fluidOptimizer = on;
    }

    public static void warmup(int ticks) {
        warmupTicks = Math.max(warmupTicks, ticks);
        stateSeenSections = new LongOpenHashSet(0);
        publishView(VIEW.get().hasCamera());
        bumpLightCacheEpoch();
        PreparedChunkCache.clear();
        clearSectionCaches();
        clearAdaptiveSuppressions();
    }

    public static void stopWarmup() {
        warmupTicks = 0;
    }

    public static void clearWorldState() {
        warmupTicks = 0;
        lightCacheTick = Long.MIN_VALUE;
        cameraSection = Long.MIN_VALUE;
        stateCameraX = 0.0;
        stateCameraY = 0.0;
        stateCameraZ = 0.0;
        stateForwardX = 0.0f;
        stateForwardY = 0.0f;
        stateForwardZ = 0.0f;
        stateSeenSections = new LongOpenHashSet(0);
        publishView(false);
        bumpLightCacheEpoch();
        PreparedChunkCache.clear();
        clearSectionCaches();
        clearAdaptiveSuppressions();
        vanillaVisibleSections = 0;
        trimmedVisibleSections = 0;
        finalVisibleSections = 0;
    }

    public static void publishVisibleCounts(int vanillaVisible, int trimmedVisible, int finalVisible) {
        vanillaVisibleSections = Math.max(0, vanillaVisible);
        trimmedVisibleSections = Math.max(0, trimmedVisible);
        finalVisibleSections = Math.max(0, finalVisible);
    }

    public static void tickWorld(Minecraft mc) {
        if (mc.level == null) {
            lightCacheTick = Long.MIN_VALUE;
            return;
        }

        long tick = mc.level.getGameTime() >> 4;
        if (tick != lightCacheTick) {
            lightCacheTick = tick;
            bumpLightCacheEpoch();
        }
    }

    public static void tickWarmup() {
        if (warmupTicks > 0) warmupTicks--;
    }

    public static void updateCamera(Camera camera) {
        //? if >=26.1.2 {
        stateCameraX = camera.position().x;
        stateCameraY = camera.position().y;
        stateCameraZ = camera.position().z;
        Vector3fc f = camera.forwardVector();
        //?} else {
        /*cameraX = camera.getPosition().x;
        cameraY = camera.getPosition().y;
        cameraZ = camera.getPosition().z;
        Vector3fc f = camera.getLookVector();
        *///?}
        stateForwardX = f.x();
        stateForwardY = f.y();
        stateForwardZ = f.z();
        updateCameraSection();
        publishView(true);
    }

    public static void updateCamera(Vec3 pos, Quaternionfc rot) {
        stateCameraX = pos.x;
        stateCameraY = pos.y;
        stateCameraZ = pos.z;
        Vector3f f = FWD.get().set(0.0f, 0.0f, -1.0f).rotate(rot);
        stateForwardX = f.x();
        stateForwardY = f.y();
        stateForwardZ = f.z();
        updateCameraSection();
        publishView(true);
    }

    public static void seeSections(List<SectionRenderDispatcher.RenderSection> sections) {
        LongOpenHashSet seen = new LongOpenHashSet(Math.max(16, sections.size() * 2));
        for (int i = 0, n = sections.size(); i < n; i++) {
            //? if >=26.1.2 {
            SectionRenderDispatcher.RenderSection section = sections.get(i);
            BlockPos p = section.getRenderOrigin();
            //?} else {
            /*BlockPos p = sections.get(i).getOrigin();
            *///?}
            seen.add(SectionPos.asLong(p.getX() >> 4, p.getY() >> 4, p.getZ() >> 4));
        }
        stateSeenSections = seen;
        publishView(VIEW.get().hasCamera());
    }

    public static boolean shouldCullEntity(Entity e, Camera camera, double camX, double camY, double camZ) {
        entityCullChecks.increment();
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

        ViewState view = VIEW.get();
        if (behindCam() && distSq >= 1600.0 && behind(view, dx, dy, dz, Math.sqrt(distSq))) {
            entityCullSkips.increment();
            return true;
        }
        if (e instanceof LivingEntity living) {
            if (living.getHealth() <= 0.0f) {
                entityCullSkips.increment();
                return true;
            }
        }
        if (!entityCulling || !view.hasSections()) return false;

        AABB box = e.getBoundingBox();
        int minX = Mth.floor(box.minX) >> 4;
        int minY = Mth.floor(box.minY) >> 4;
        int minZ = Mth.floor(box.minZ) >> 4;
        int maxX = Mth.floor(box.maxX - 1.0E-5) >> 4;
        int maxY = Mth.floor(box.maxY - 1.0E-5) >> 4;
        int maxZ = Mth.floor(box.maxZ - 1.0E-5) >> 4;

        for (int sx = minX; sx <= maxX; sx++) {
            for (int sy = minY; sy <= maxY; sy++) {
                for (int sz = minZ; sz <= maxZ; sz++) {
                    if (view.seenSections().contains(SectionPos.asLong(sx, sy, sz))) {
                        return false;
                    }
                }
            }
        }
        entityCullSkips.increment();
        return true;
    }

    public static boolean hidesJoinedFace(BlockState state, BlockState near) {
        return false;
    }

    public static boolean roughTerrainTextures() {
        if (warming()) return false;
        if (!textureLod) return false;
        return !Zoom.isZooming();
    }

    public static boolean shouldUseChunkAo(BlockPos pos) {
        chunkAoChecks.increment();
        if (warming()) return true;
        if (!chunkShadeTrim) return true;
        if (Zoom.isZooming()) return true;
        ViewState view = VIEW.get();
        if (!view.hasCamera()) return true;

        double dx = pos.getX() + 0.5 - view.cameraX();
        double dy = pos.getY() + 0.5 - view.cameraY();
        double dz = pos.getZ() + 0.5 - view.cameraZ();
        boolean keep = dx * dx + dy * dy + dz * dz < 64.0 * 64.0;
        if (!keep) chunkAoSkips.increment();
        return keep;
    }

    public static boolean shouldSkipSectionLayer(BlockPos pos, ChunkSectionLayer layer) {
        sectionLayerChecks.increment();
        if (warming()) return false;
        if (!farLayerTrim) return false;
        if (Zoom.isZooming()) return false;
        ViewState view = VIEW.get();
        if (!view.hasCamera()) return false;
        if (layer != ChunkSectionLayer.CUTOUT) return false;
        if (sectionLayerSuppressed) {
            sectionLayerSuppressedChecks.increment();
            return false;
        }

        long key = SectionPos.asLong(pos.getX() >> 4, pos.getY() >> 4, pos.getZ() >> 4);
        byte cached = sectionLayerCutoutCache.get(key);
        if (cached >= 0) {
            sectionLayerCacheHits.increment();
            if (cached == 1) sectionLayerSkips.increment();
            else if (sectionLayerChecks.sum() >= 4096L && sectionLayerSkips.sum() == 0L) sectionLayerSuppressed = true;
            return cached == 1;
        }

        double dx = pos.getX() + 8.0 - view.cameraX();
        double dy = pos.getY() + 8.0 - view.cameraY();
        double dz = pos.getZ() + 8.0 - view.cameraZ();
        double distSq = dx * dx + dy * dy + dz * dz;
        boolean skip = false;
        if (distSq >= 160.0 * 160.0) {
            if (fogOcclusion && Fog.isOccludingDistance((float)Math.sqrt(distSq))) {
                skip = true;
            } else if (cutsTerrainBehindCam(view)) {
                skip = behind(view, dx, dy, dz, Math.sqrt(distSq));
            }
        }
        sectionLayerCutoutCache.put(key, skip ? (byte)1 : (byte)0);
        if (skip) sectionLayerSkips.increment();
        else if (sectionLayerChecks.sum() >= 4096L && sectionLayerSkips.sum() == 0L) sectionLayerSuppressed = true;
        return skip;
    }

    //? if >=26.1.2 {
    public static int getTrimmedLight(BlockAndLightGetter level, BlockPos pos) {
    //?} else {
    /*public static int getTrimmedLight(BlockAndTintGetter level, BlockPos pos) {
    *///?}
        lightTrimCalls.increment();
        if (warming()) return -1;
        if (!lightingChunkTrim) return -1;
        if (Zoom.isZooming()) return -1;
        if (lightTrimSuppressed) {
            lightTrimSuppressedCalls.increment();
            return -1;
        }
        ViewState view = VIEW.get();
        if (!view.hasCamera()) return -1;

        double dx = pos.getX() + 0.5 - view.cameraX();
        double dy = pos.getY() + 0.5 - view.cameraY();
        double dz = pos.getZ() + 0.5 - view.cameraZ();
        double distSq = dx * dx + dy * dy + dz * dz;
        if (distSq < 36864.0) {
            lightTrimRejectedNear.increment();
            return -1;
        }

        long sectionKey = SectionPos.asLong(pos.getX() >> 4, pos.getY() >> 4, pos.getZ() >> 4);
        byte sectionCached = lightSectionCache.get(sectionKey);
        if (sectionCached == 0) {
            lightTrimRejectedNear.increment();
            if (lightTrimCalls.sum() >= 32768L && lightTrimEligible.sum() == 0L) {
                lightTrimSuppressed = true;
                lightTrimSuppressedCalls.increment();
            }
            return -1;
        }
        if (sectionCached < 0) {
            double sx = (SectionPos.sectionToBlockCoord(SectionPos.x(sectionKey)) + 8.5) - view.cameraX();
            double sy = (SectionPos.sectionToBlockCoord(SectionPos.y(sectionKey)) + 8.5) - view.cameraY();
            double sz = (SectionPos.sectionToBlockCoord(SectionPos.z(sectionKey)) + 8.5) - view.cameraZ();
            boolean farEnough = sx * sx + sy * sy + sz * sz >= 36864.0;
            lightSectionCache.put(sectionKey, farEnough ? (byte)1 : (byte)0);
            if (!farEnough) {
                lightTrimRejectedNear.increment();
                if (lightTrimCalls.sum() >= 32768L && lightTrimEligible.sum() == 0L) {
                    lightTrimSuppressed = true;
                    lightTrimSuppressedCalls.increment();
                }
                return -1;
            }
        }

        BlockState state = level.getBlockState(pos);
        if (!state.getFluidState().isEmpty()) return -1;
        if (!level.getBlockState(pos.above()).getFluidState().isEmpty()) return -1;

        lightTrimEligible.increment();
        int packed = packLight(level.getBrightness(LightLayer.BLOCK, pos), level.getBrightness(LightLayer.SKY, pos));
        if (!QuantifiedSupport.loaded()) {
            return packed;
        }
        String key = lightCacheKey(pos);
        return lightCache().get(key, () -> packed);
    }

    public static int getOptimizedFluidLight(BlockAndTintGetter level, BlockPos pos, int original) {
        fluidLightCalls.increment();
        if (warming() || !fluidOptimizer) return original;
        if (Zoom.isZooming()) return original;

        ViewState view = VIEW.get();
        if (!view.hasCamera()) return original;

        long sectionKey = SectionPos.asLong(pos.getX() >> 4, pos.getY() >> 4, pos.getZ() >> 4);
        byte sectionCached = fluidSectionCache.get(sectionKey);
        if (sectionCached == 0) {
            fluidLightRejectedFar.increment();
            return original;
        }
        if (sectionCached < 0) {
            double dx = (SectionPos.sectionToBlockCoord(SectionPos.x(sectionKey)) + 8.5) - view.cameraX();
            double dy = (SectionPos.sectionToBlockCoord(SectionPos.y(sectionKey)) + 8.5) - view.cameraY();
            double dz = (SectionPos.sectionToBlockCoord(SectionPos.z(sectionKey)) + 8.5) - view.cameraZ();
            boolean nearEnough = dx * dx + dy * dy + dz * dz < 12544.0;
            fluidSectionCache.put(sectionKey, nearEnough ? (byte)1 : (byte)0);
            if (!nearEnough) {
                fluidLightRejectedFar.increment();
                return original;
            }
        }

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
        translucencyResortChecks.increment();
        if (translucencyResortSuppressed) {
            translucencyResortSuppressedChecks.increment();
            return false;
        }
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
        boolean skip = distSq >= 1600.0 && behind(VIEW.get(), dx, dy, dz, Math.sqrt(distSq));
        if (skip) translucencyResortSkips.increment();
        else if (translucencyResortChecks.sum() >= 4096L && translucencyResortSkips.sum() == 0L) translucencyResortSuppressed = true;
        return skip;
    }
    //?}

    public static boolean shouldSkipTerrainSection(SectionRenderDispatcher.RenderSection section) {
        terrainSectionChecks.increment();
        if (terrainCullSuppressed) {
            terrainCullSuppressedChecks.increment();
            return false;
        }
        ViewState view = VIEW.get();
        if (!cutsTerrainSections(view)) return false;

        //? if >=26.1.2 {
        BlockPos p = section.getRenderOrigin();
        //?} else {
        /*BlockPos p = section.getOrigin();
        *///?}
        long key = SectionPos.asLong(p.getX() >> 4, p.getY() >> 4, p.getZ() >> 4);
        byte cached = terrainSectionCache.get(key);
        if (cached >= 0) {
            terrainSectionCacheHits.increment();
            if (cached == 1) {
                terrainSectionSkips.increment();
            } else if (terrainSectionChecks.sum() >= 4096L && terrainSectionSkips.sum() == 0L) {
                terrainCullSuppressed = true;
            }
            return cached == 1;
        }

        double dx = p.getX() + 8.0 - view.cameraX();
        double dy = p.getY() + 8.0 - view.cameraY();
        double dz = p.getZ() + 8.0 - view.cameraZ();
        double distSq = dx * dx + dy * dy + dz * dz;
        boolean skip = false;
        if (fogOcclusion && Fog.isOccludingDistance((float)Math.sqrt(distSq))) {
            skip = true;
        } else if (distSq >= 1600.0 && cutsTerrainBehindCam(view)) {
            skip = behind(view, dx, dy, dz, Math.sqrt(distSq));
        }
        terrainSectionCache.put(key, skip ? (byte)1 : (byte)0);
        if (skip) terrainSectionSkips.increment();
        else if (terrainSectionChecks.sum() >= 4096L && terrainSectionSkips.sum() == 0L) terrainCullSuppressed = true;
        return skip;
    }

    public static boolean cutsTerrainSections() {
        return cutsTerrainSections(VIEW.get());
    }

    public static int currentSectionTier(BlockPos pos) {
        ViewState view = VIEW.get();
        if (!view.hasCamera()) return 0;
        double dx = pos.getX() + 8.0 - view.cameraX();
        double dy = pos.getY() + 8.0 - view.cameraY();
        double dz = pos.getZ() + 8.0 - view.cameraZ();
        double distSq = dx * dx + dy * dy + dz * dz;
        if (distSq < 96.0 * 96.0) return 0;
        if (distSq < 160.0 * 160.0) return 1;
        if (distSq < 224.0 * 224.0) return 2;
        return 3;
    }

    public static boolean cutsTerrainBehindCam() {
        return cutsTerrainBehindCam(VIEW.get());
    }

    public static boolean useSectionOcclusion(boolean vanilla) {
        return !warming() && occlusionCulling && vanilla;
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

    private static boolean behind(ViewState view, double dx, double dy, double dz, double dist) {
        if (dist <= 0.001) return false;
        return (dx * view.forwardX() + dy * view.forwardY() + dz * view.forwardZ()) / dist < -0.35;
    }

    private static void updateCameraSection() {
        long now = SectionPos.asLong(Mth.floor(stateCameraX) >> 4, Mth.floor(stateCameraY) >> 4, Mth.floor(stateCameraZ) >> 4);
        if (now != cameraSection) {
            cameraSection = now;
            bumpLightCacheEpoch();
            clearSectionCaches();
            clearAdaptiveSuppressions();
        }
    }

    private static boolean cutsTerrainSections(ViewState view) {
        return !warming() && view.hasCamera() && !Zoom.isZooming() && (fogOcclusion || behindCamMode >= 2);
    }

    private static boolean cutsTerrainBehindCam(ViewState view) {
        return !warming() && view.hasCamera() && !Zoom.isZooming() && behindCamMode >= 2;
    }

    private static void publishView(boolean hasCamera) {
        LongOpenHashSet seen = stateSeenSections;
        VIEW.set(new ViewState(
                hasCamera,
                !seen.isEmpty(),
                stateCameraX,
                stateCameraY,
                stateCameraZ,
                stateForwardX,
                stateForwardY,
                stateForwardZ,
                seen
        ));
    }

    private static void bumpLightCacheEpoch() {
        LIGHT_CACHE_EPOCH.incrementAndGet();
    }

    private static void clearSectionCaches() {
        sectionLayerCutoutCache.clear();
        terrainSectionCache.clear();
        lightSectionCache.clear();
        fluidSectionCache.clear();
    }

    private static void clearAdaptiveSuppressions() {
        terrainCullSuppressed = false;
        translucencyResortSuppressed = false;
        lightTrimSuppressed = false;
        sectionLayerSuppressed = false;
    }

    private static CacheRequest lightCache() {
        return QuantifiedAPI.cache(Rendersnap.MOD_ID, "light_cache")
                .memoryOnly()
                .maxEntries(4096)
                .ttl(LIGHT_CACHE_TTL)
                .fixedTtl();
    }

    private static String lightCacheKey(BlockPos pos) {
        return Long.toHexString(LIGHT_CACHE_EPOCH.get()) + ':' + Long.toUnsignedString(pos.asLong(), 16);
    }

    private record ViewState(
            boolean hasCamera,
            boolean hasSections,
            double cameraX,
            double cameraY,
            double cameraZ,
            float forwardX,
            float forwardY,
            float forwardZ,
            LongOpenHashSet seenSections
    ) {
        private static final ViewState EMPTY = new ViewState(false, false, 0.0, 0.0, 0.0, 0.0f, 0.0f, 0.0f, new LongOpenHashSet(0));
    }
    private static void rebuildChunks() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            //? if >=26.2-snapshot-8 {
            /*mc.levelRenderer.resetLevelRenderData();
            *///?} else
            mc.levelRenderer.allChanged();
        }
        PreparedChunkCache.clear();
    }

    private static void resetTerrainSampler() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.levelRenderer != null) {
            //? if >=26.2-snapshot-8 {
            /*mc.levelRenderer.resetLevelRenderData();
            *///?} else if >=26.1.2 {
            mc.levelRenderer.resetSampler();
            //?} else {
            /*mc.levelRenderer.allChanged();
            *///?}
        }
        PreparedChunkCache.clear();
    }

    public static void appendDebug(StringBuilder out) {
        ViewState view = VIEW.get();
        out.append("blockFaceCulling=").append(blockFaceCulling).append('\n');
        out.append("textureLod=").append(textureLod).append('\n');
        out.append("chunkShadeTrim=").append(chunkShadeTrim).append('\n');
        out.append("farLayerTrim=").append(farLayerTrim).append('\n');
        out.append("entityCulling=").append(entityCulling).append('\n');
        out.append("occlusionCulling=").append(occlusionCulling).append('\n');
        out.append("fogOcclusion=").append(fogOcclusion).append('\n');
        out.append("behindCamMode=").append(behindCamMode).append('\n');
        out.append("lightingChunkTrim=").append(lightingChunkTrim).append('\n');
        out.append("hideWeather=").append(hideWeather).append('\n');
        out.append("fluidOptimizer=").append(fluidOptimizer).append('\n');
        out.append("warmupTicks=").append(warmupTicks).append('\n');
        out.append("lightCacheTick=").append(lightCacheTick).append('\n');
        out.append("lightCacheEpoch=").append(LIGHT_CACHE_EPOCH.get()).append('\n');
        out.append("cameraSection=").append(cameraSection).append('\n');
        out.append("hasCamera=").append(view.hasCamera()).append('\n');
        out.append("hasSections=").append(view.hasSections()).append('\n');
        out.append("camera=(").append(view.cameraX()).append(", ").append(view.cameraY()).append(", ").append(view.cameraZ()).append(")\n");
        out.append("forward=(").append(view.forwardX()).append(", ").append(view.forwardY()).append(", ").append(view.forwardZ()).append(")\n");
        out.append("seenSections=").append(view.seenSections().size()).append('\n');
        out.append("vanillaVisibleSections=").append(vanillaVisibleSections).append('\n');
        out.append("trimmedVisibleSections=").append(trimmedVisibleSections).append('\n');
        out.append("finalVisibleSections=").append(finalVisibleSections).append('\n');
        out.append("entityCullChecks=").append(entityCullChecks.sum()).append('\n');
        out.append("entityCullSkips=").append(entityCullSkips.sum()).append('\n');
        out.append("chunkAoChecks=").append(chunkAoChecks.sum()).append('\n');
        out.append("chunkAoSkips=").append(chunkAoSkips.sum()).append('\n');
        out.append("sectionLayerChecks=").append(sectionLayerChecks.sum()).append('\n');
        out.append("sectionLayerSkips=").append(sectionLayerSkips.sum()).append('\n');
        out.append("sectionLayerCacheHits=").append(sectionLayerCacheHits.sum()).append('\n');
        out.append("sectionLayerSuppressed=").append(sectionLayerSuppressed).append('\n');
        out.append("sectionLayerSuppressedChecks=").append(sectionLayerSuppressedChecks.sum()).append('\n');
        out.append("terrainSectionChecks=").append(terrainSectionChecks.sum()).append('\n');
        out.append("terrainSectionSkips=").append(terrainSectionSkips.sum()).append('\n');
        out.append("terrainSectionCacheHits=").append(terrainSectionCacheHits.sum()).append('\n');
        out.append("terrainCullSuppressed=").append(terrainCullSuppressed).append('\n');
        out.append("terrainCullSuppressedChecks=").append(terrainCullSuppressedChecks.sum()).append('\n');
        out.append("lightTrimCalls=").append(lightTrimCalls.sum()).append('\n');
        out.append("lightTrimEligible=").append(lightTrimEligible.sum()).append('\n');
        out.append("lightTrimRejectedNear=").append(lightTrimRejectedNear.sum()).append('\n');
        out.append("lightTrimSuppressed=").append(lightTrimSuppressed).append('\n');
        out.append("lightTrimSuppressedCalls=").append(lightTrimSuppressedCalls.sum()).append('\n');
        out.append("fluidLightCalls=").append(fluidLightCalls.sum()).append('\n');
        out.append("fluidLightRejectedFar=").append(fluidLightRejectedFar.sum()).append('\n');
        out.append("translucencyResortChecks=").append(translucencyResortChecks.sum()).append('\n');
        out.append("translucencyResortSkips=").append(translucencyResortSkips.sum()).append('\n');
        out.append("translucencyResortSuppressed=").append(translucencyResortSuppressed).append('\n');
        out.append("translucencyResortSuppressedChecks=").append(translucencyResortSuppressedChecks.sum()).append('\n');
    }
}
