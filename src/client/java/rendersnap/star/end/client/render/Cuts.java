package rendersnap.star.end.client.render;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ByteOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
//? if >=26.2 {
/*import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
*///?} else {
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
//?}
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import rendersnap.star.end.client.McCompat;
//? if >=26.1.2 {
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
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
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.admany.quantified.api.QuantifiedAPI;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import rendersnap.star.end.Rendersnap;
import rendersnap.star.end.client.PreparedChunkCache;
import rendersnap.star.end.client.QuantifiedSupport;
import rendersnap.star.end.client.render.zoom.Zoom;

import java.time.Duration;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;
import org.lwjgl.system.MemoryUtil;

public final class Cuts {
    private static final long DEAD_PATH_SUPPRESSION_THRESHOLD = 512L;
    private static boolean blockFaceCulling;
    private static volatile boolean textureLod;
    private static volatile boolean chunkShadeTrim;
    private static volatile boolean farLayerTrim;
    private static volatile boolean cutoutLeafBoost;
    private static volatile boolean cutoutFarReplace = false;
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
    private static volatile boolean fluidLightSuppressed;
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
    private static int stateSeenSectionCount;
    private static long stateSeenFingerprint;

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
    private static final Int2ByteOpenHashMap entityVisibilityCache = new Int2ByteOpenHashMap();
    private static final Int2LongOpenHashMap entityVisibilityStamp = new Int2LongOpenHashMap();
    private static final Int2ObjectOpenHashMap<ShadowState> entityShadowCache = new Int2ObjectOpenHashMap<>();
    private static final LongAdder entityCullChecks = new LongAdder();
    private static final LongAdder entityCullSkips = new LongAdder();
    private static final LongAdder entityCullCacheHits = new LongAdder();
    private static final LongAdder shadowCacheHits = new LongAdder();
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
    private static final LongAdder fluidLightEligible = new LongAdder();
    private static final LongAdder fluidLightRejectedFar = new LongAdder();
    private static final LongAdder fluidLightSuppressedCalls = new LongAdder();
    private static final LongAdder translucencyResortChecks = new LongAdder();
    private static final LongAdder translucencyResortSkips = new LongAdder();
    private static final LongAdder translucencyResortSuppressedChecks = new LongAdder();
    private static final LongAdder cutoutLeafChecks = new LongAdder();
    private static final LongAdder cutoutLeafForcedOpaque = new LongAdder();
    private static final LongAdder cutoutLeafVanillaOpaque = new LongAdder();
    private static final LongAdder cutoutFarChecks = new LongAdder();
    private static final LongAdder cutoutFarReduced = new LongAdder();
    private static final LongAdder cutoutFarRejectedNear = new LongAdder();
    private static final LongAdder cutoutFarRejectedSmall = new LongAdder();
    private static final LongAdder cutoutFarRejectedShape = new LongAdder();
    private static final LongAdder cutoutFarSourceQuads = new LongAdder();
    private static final LongAdder cutoutFarReducedQuads = new LongAdder();
    private static final LongAdder cutoutAggPasses = new LongAdder();
    private static final LongAdder cutoutAggListsBefore = new LongAdder();
    private static final LongAdder cutoutAggListsAfter = new LongAdder();
    private static final LongAdder falseEmptySectionLevelMisses = new LongAdder();
    private static final LongAdder falseEmptySectionCandidates = new LongAdder();
    private static final LongAdder falseEmptySectionChecks = new LongAdder();
    private static final LongAdder falseEmptySectionRepairs = new LongAdder();
    private static final LongAdder falseEmptySectionResets = new LongAdder();

    private Cuts() {
        sectionLayerCutoutCache.defaultReturnValue((byte)-1);
        terrainSectionCache.defaultReturnValue((byte)-1);
        lightSectionCache.defaultReturnValue((byte)-1);
        fluidSectionCache.defaultReturnValue((byte)-1);
        entityVisibilityCache.defaultReturnValue((byte)-1);
        entityVisibilityStamp.defaultReturnValue(Long.MIN_VALUE);
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

    public static void setCutoutLeafBoost(boolean on) {
        if (cutoutLeafBoost == on) return;
        cutoutLeafBoost = on;
        rebuildChunks();
    }

    public static boolean trimsFarLayers() {
        return farLayerTrim;
    }

    public static boolean shouldForceOpaqueCutoutLeaves(boolean cutoutLeaves, BlockState state) {
        cutoutLeafChecks.increment();
        if (!(state.getBlock() instanceof LeavesBlock)) return false;
        if (!cutoutLeaves) {
            cutoutLeafVanillaOpaque.increment();
            return true;
        }
        if (!cutoutLeafBoost) return false;
        cutoutLeafForcedOpaque.increment();
        return true;
    }

    public static MeshData reduceFarCutoutMesh(SectionPos pos, MeshData mesh) {
        cutoutFarChecks.increment();
        return mesh;
    }

    public static ChunkSectionsToRender aggregateCutoutDraws(ChunkSectionsToRender prepared) {
        if (prepared == null) {
            return null;
        }
        boolean mergeCutoutDraws = true;
        //? if >=26.2 {
        /*mergeCutoutDraws = false;*/
        //?}
        if (!mergeCutoutDraws) {
            return prepared;
        }
        Int2ObjectOpenHashMap<List<RenderPass.Draw<com.mojang.blaze3d.buffers.GpuBufferSlice[]>>> cutout =
                prepared.drawGroupsPerLayer().get(ChunkSectionLayer.CUTOUT);
        if (cutout == null || cutout.size() <= 1) {
            return prepared;
        }
        ArrayList<RenderPass.Draw<com.mojang.blaze3d.buffers.GpuBufferSlice[]>> merged = new ArrayList<>();
        for (List<RenderPass.Draw<com.mojang.blaze3d.buffers.GpuBufferSlice[]>> list : cutout.values()) {
            merged.addAll(list);
        }
        if (merged.isEmpty()) {
            return prepared;
        }
        var copy = new java.util.EnumMap<ChunkSectionLayer, Int2ObjectOpenHashMap<List<RenderPass.Draw<com.mojang.blaze3d.buffers.GpuBufferSlice[]>>>>(ChunkSectionLayer.class);
        for (var entry : prepared.drawGroupsPerLayer().entrySet()) {
            if (entry.getKey() == ChunkSectionLayer.CUTOUT) continue;
            copy.put(entry.getKey(), entry.getValue());
        }
        Int2ObjectOpenHashMap<List<RenderPass.Draw<com.mojang.blaze3d.buffers.GpuBufferSlice[]>>> one = new Int2ObjectOpenHashMap<>();
        one.put(0, merged);
        copy.put(ChunkSectionLayer.CUTOUT, one);
        cutoutAggPasses.increment();
        cutoutAggListsBefore.add(cutout.size());
        cutoutAggListsAfter.add(1);
        return new ChunkSectionsToRender(prepared.textureView(), copy, prepared.maxIndicesRequired(), prepared.chunkSectionInfos());
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
        stateSeenSectionCount = 0;
        stateSeenFingerprint = 0L;
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
        stateSeenSectionCount = 0;
        stateSeenFingerprint = 0L;
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

    public static void recordFalseEmptySectionCheck(boolean repaired) {
        falseEmptySectionChecks.increment();
        if (repaired) falseEmptySectionRepairs.increment();
    }

    public static void recordFalseEmptySectionReset() {
        falseEmptySectionResets.increment();
    }

    public static void recordFalseEmptySectionLevelMiss() {
        falseEmptySectionLevelMisses.increment();
    }

    public static void recordFalseEmptySectionCandidate() {
        falseEmptySectionCandidates.increment();
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
        int size = sections.size();
        long xor = 0L;
        long sum = 0x9E3779B97F4A7C15L;
        long[] keys = new long[size];
        for (int i = 0; i < size; i++) {
            //? if >=26.1.2 {
            SectionRenderDispatcher.RenderSection section = sections.get(i);
            BlockPos p = section.getRenderOrigin();
            //?} else {
            /*BlockPos p = sections.get(i).getOrigin();
            *///?}
            long key = SectionPos.asLong(p.getX() >> 4, p.getY() >> 4, p.getZ() >> 4);
            keys[i] = key;
            long mixed = mix64(key);
            xor ^= mixed;
            sum += mixed;
        }
        long fingerprint = mix64(xor) ^ Long.rotateLeft(sum, 17);
        if (size == stateSeenSectionCount && fingerprint == stateSeenFingerprint) {
            return;
        }
        LongOpenHashSet seen = new LongOpenHashSet(Math.max(16, size * 2));
        for (int i = 0; i < size; i++) {
            seen.add(keys[i]);
        }
        stateSeenSections = seen;
        stateSeenSectionCount = size;
        stateSeenFingerprint = fingerprint;
        publishView(VIEW.get().hasCamera());
    }

    public static boolean restoreShadowState(Entity entity, EntityRenderState state) {
        if (entity == null || state == null) return false;
        ShadowState cached = entityShadowCache.get(entity.getId());
        if (cached == null) return false;
        if (cached.cameraSection() != cameraSection) return false;
        if (cached.cameraStamp() != cameraPoseStamp()) return false;
        if (cached.entityPos() != entity.blockPosition().asLong()) return false;
        if (cached.distanceBucket() != shadowDistanceBucket(state.distanceToCameraSq)) return false;
        state.shadowRadius = cached.shadowRadius();
        state.shadowPieces.clear();
        state.shadowPieces.addAll(cached.shadowPieces());
        shadowCacheHits.increment();
        return true;
    }

    public static void storeShadowState(Entity entity, EntityRenderState state) {
        if (entity == null || state == null) return;
        entityShadowCache.put(entity.getId(), new ShadowState(
                entity.blockPosition().asLong(),
                cameraSection,
                cameraPoseStamp(),
                shadowDistanceBucket(state.distanceToCameraSq),
                state.shadowRadius,
                List.copyOf(state.shadowPieces)
        ));
    }

    public static boolean shouldCullEntity(Entity e, Camera camera, double camX, double camY, double camZ) {
        entityCullChecks.increment();
        if (!entityCulling && !behindCam()) return false;
        if (camera == null || e == null) return false;
        if (warming() || Zoom.isZooming()) return false;

        long entityStamp = entityCullStamp();
        int entityId = e.getId();
        if (entityVisibilityStamp.get(entityId) == entityStamp) {
            byte cached = entityVisibilityCache.get(entityId);
            if (cached >= 0) {
                entityCullCacheHits.increment();
                if (cached == 1) entityCullSkips.increment();
                return cached == 1;
            }
        }

        //? if >=26.1.2 {
        Entity camEnt = camera.entity();
        //?} else {
        /*Entity camEnt = camera.getEntity();
        *///?}
        if (e == camEnt || e instanceof Player) return rememberEntityCull(entityId, entityStamp, false);
        if (e.isCurrentlyGlowing()) return rememberEntityCull(entityId, entityStamp, false);
        if (e.isVehicle() || e.isPassenger()) return rememberEntityCull(entityId, entityStamp, false);

        double dx = e.getX() - camX;
        double dy = e.getY() - camY;
        double dz = e.getZ() - camZ;
        double distSq = dx * dx + dy * dy + dz * dz;
        if (distSq < 1024.0) return rememberEntityCull(entityId, entityStamp, false);

        ViewState view = VIEW.get();
        if (behindCam() && distSq >= 1600.0 && behind(view, dx, dy, dz, Math.sqrt(distSq))) {
            entityCullSkips.increment();
            return rememberEntityCull(entityId, entityStamp, true);
        }
        if (e instanceof LivingEntity living) {
            if (living.getHealth() <= 0.0f) {
                entityCullSkips.increment();
                return rememberEntityCull(entityId, entityStamp, true);
            }
        }
        if (!entityCulling || !view.hasSections()) return rememberEntityCull(entityId, entityStamp, false);

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
                        return rememberEntityCull(entityId, entityStamp, false);
                    }
                }
            }
        }
        entityCullSkips.increment();
        return rememberEntityCull(entityId, entityStamp, true);
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
            else if (sectionLayerChecks.sum() >= DEAD_PATH_SUPPRESSION_THRESHOLD && sectionLayerSkips.sum() == 0L) sectionLayerSuppressed = true;
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
        else if (sectionLayerChecks.sum() >= DEAD_PATH_SUPPRESSION_THRESHOLD && sectionLayerSkips.sum() == 0L) sectionLayerSuppressed = true;
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
            if (lightTrimCalls.sum() >= DEAD_PATH_SUPPRESSION_THRESHOLD && lightTrimEligible.sum() == 0L) {
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
                if (lightTrimCalls.sum() >= DEAD_PATH_SUPPRESSION_THRESHOLD && lightTrimEligible.sum() == 0L) {
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
        return QuantifiedSupport.lightCache(LIGHT_CACHE_TTL, 4096).get(key, () -> packed);
    }

    public static boolean shouldCheckTrimmedLight(BlockPos pos) {
        if (pos == null) return false;
        if (lightTrimSuppressed || warming() || !lightingChunkTrim || Zoom.isZooming()) return false;
        ViewState view = VIEW.get();
        if (!view.hasCamera()) return false;
        return sectionDistanceSq(view, pos) >= 36864.0;
    }

    public static int getOptimizedFluidLight(BlockAndTintGetter level, BlockPos pos, int original) {
        fluidLightCalls.increment();
        if (warming() || !fluidOptimizer) return original;
        if (Zoom.isZooming()) return original;
        if (fluidLightSuppressed) {
            fluidLightSuppressedCalls.increment();
            return original;
        }

        ViewState view = VIEW.get();
        if (!view.hasCamera()) return original;

        long sectionKey = SectionPos.asLong(pos.getX() >> 4, pos.getY() >> 4, pos.getZ() >> 4);
        byte sectionCached = fluidSectionCache.get(sectionKey);
        if (sectionCached == 0) {
            fluidLightRejectedFar.increment();
        if (fluidLightCalls.sum() >= DEAD_PATH_SUPPRESSION_THRESHOLD && fluidLightEligible.sum() == 0L) {
                fluidLightSuppressed = true;
                fluidLightSuppressedCalls.increment();
            }
            return original;
        }
        if (sectionCached < 0) {
            boolean nearEnough = sectionDistanceSq(view, pos) < 12544.0;
            fluidSectionCache.put(sectionKey, nearEnough ? (byte)1 : (byte)0);
            if (!nearEnough) {
                fluidLightRejectedFar.increment();
                if (fluidLightCalls.sum() >= DEAD_PATH_SUPPRESSION_THRESHOLD && fluidLightEligible.sum() == 0L) {
                    fluidLightSuppressed = true;
                    fluidLightSuppressedCalls.increment();
                }
                return original;
            }
        }
        fluidLightEligible.increment();

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

    public static boolean shouldCheckOptimizedFluidLight(BlockPos pos) {
        if (pos == null) return false;
        if (warming() || !fluidOptimizer || Zoom.isZooming() || fluidLightSuppressed) return false;
        ViewState view = VIEW.get();
        if (!view.hasCamera()) return false;
        return sectionDistanceSq(view, pos) < 12544.0;
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
        else if (translucencyResortChecks.sum() >= DEAD_PATH_SUPPRESSION_THRESHOLD && translucencyResortSkips.sum() == 0L) translucencyResortSuppressed = true;
        return skip;
    }
    //?}

    public static boolean shouldCheckFarTranslucencyResort(boolean near) {
        return !near && !translucencyResortSuppressed && !warming() && behindCam() && !Zoom.isZooming();
    }

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
            } else if (terrainSectionChecks.sum() >= DEAD_PATH_SUPPRESSION_THRESHOLD && terrainSectionSkips.sum() == 0L) {
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
        else if (terrainSectionChecks.sum() >= DEAD_PATH_SUPPRESSION_THRESHOLD && terrainSectionSkips.sum() == 0L) terrainCullSuppressed = true;
        return skip;
    }

    public static boolean cutsTerrainSections() {
        return cutsTerrainSections(VIEW.get());
    }

    public static boolean shouldCheckTerrainSections() {
        return !terrainCullSuppressed && cutsTerrainSections(VIEW.get());
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
        entityVisibilityCache.clear();
        entityVisibilityStamp.clear();
        entityShadowCache.clear();
    }

    private static boolean rememberEntityCull(int entityId, long stamp, boolean culled) {
        entityVisibilityStamp.put(entityId, stamp);
        entityVisibilityCache.put(entityId, culled ? (byte)1 : (byte)0);
        return culled;
    }

    private static long entityCullStamp() {
        return mix64(cameraSection)
                ^ Long.rotateLeft(stateSeenFingerprint, 11)
                ^ Long.rotateLeft(cameraPoseStamp(), 23)
                ^ (LIGHT_CACHE_EPOCH.get() << 1);
    }

    public static long preparedCameraStamp() {
        return mix64(cameraSection)
                ^ Long.rotateLeft(cameraPoseStamp(), 19)
                ^ Long.rotateLeft(stateSeenFingerprint, 7);
    }

    private static long cameraPoseStamp() {
        long x = (long)Mth.floor(stateCameraX * 2.0);
        long y = (long)Mth.floor(stateCameraY * 2.0);
        long z = (long)Mth.floor(stateCameraZ * 2.0);
        long fx = (long)Mth.floor((stateForwardX + 1.0f) * 128.0f);
        long fy = (long)Mth.floor((stateForwardY + 1.0f) * 128.0f);
        long fz = (long)Mth.floor((stateForwardZ + 1.0f) * 128.0f);
        long packed = x;
        packed = mix64(packed ^ Long.rotateLeft(y, 13));
        packed = mix64(packed ^ Long.rotateLeft(z, 27));
        packed ^= fx << 8;
        packed ^= fy << 20;
        packed ^= fz << 32;
        return packed;
    }

    private static int shadowDistanceBucket(double distanceToCameraSq) {
        return Mth.floor(distanceToCameraSq / 16.0);
    }

    private static void clearAdaptiveSuppressions() {
        terrainCullSuppressed = false;
        translucencyResortSuppressed = false;
        lightTrimSuppressed = false;
        fluidLightSuppressed = false;
        sectionLayerSuppressed = false;
    }

    private static double sectionDistanceSq(ViewState view, BlockPos pos) {
        double dx = (SectionPos.blockToSectionCoord(pos.getX()) * 16 + 8.5) - view.cameraX();
        double dy = (SectionPos.blockToSectionCoord(pos.getY()) * 16 + 8.5) - view.cameraY();
        double dz = (SectionPos.blockToSectionCoord(pos.getZ()) * 16 + 8.5) - view.cameraZ();
        return dx * dx + dy * dy + dz * dz;
    }

    private static long mix64(long value) {
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdL;
        value ^= value >>> 33;
        value *= 0xc4ceb9fe1a85ec53L;
        value ^= value >>> 33;
        return value;
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

    private record ShadowState(
            long entityPos,
            long cameraSection,
            long cameraStamp,
            int distanceBucket,
            float shadowRadius,
            List<EntityRenderState.ShadowPiece> shadowPieces
    ) {
    }
    private static void rebuildChunks() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            McCompat.resetLevelRenderer(mc);
        }
        PreparedChunkCache.clear();
    }

    private static void resetTerrainSampler() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.levelRenderer != null) {
            McCompat.resetLevelRenderer(mc);
        }
        PreparedChunkCache.clear();
    }

    public static void appendDebug(StringBuilder out) {
        ViewState view = VIEW.get();
        out.append("blockFaceCulling=").append(blockFaceCulling).append('\n');
        out.append("textureLod=").append(textureLod).append('\n');
        out.append("chunkShadeTrim=").append(chunkShadeTrim).append('\n');
        out.append("farLayerTrim=").append(farLayerTrim).append('\n');
        out.append("cutoutLeafBoost=").append(cutoutLeafBoost).append('\n');
        out.append("cutoutFarReplace=").append(cutoutFarReplace).append('\n');
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
        out.append("entityCullCacheHits=").append(entityCullCacheHits.sum()).append('\n');
        out.append("shadowCacheHits=").append(shadowCacheHits.sum()).append('\n');
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
        out.append("fluidLightEligible=").append(fluidLightEligible.sum()).append('\n');
        out.append("fluidLightRejectedFar=").append(fluidLightRejectedFar.sum()).append('\n');
        out.append("fluidLightSuppressed=").append(fluidLightSuppressed).append('\n');
        out.append("fluidLightSuppressedCalls=").append(fluidLightSuppressedCalls.sum()).append('\n');
        out.append("translucencyResortChecks=").append(translucencyResortChecks.sum()).append('\n');
        out.append("translucencyResortSkips=").append(translucencyResortSkips.sum()).append('\n');
        out.append("translucencyResortSuppressed=").append(translucencyResortSuppressed).append('\n');
        out.append("translucencyResortSuppressedChecks=").append(translucencyResortSuppressedChecks.sum()).append('\n');
        out.append("cutoutLeafChecks=").append(cutoutLeafChecks.sum()).append('\n');
        out.append("cutoutLeafForcedOpaque=").append(cutoutLeafForcedOpaque.sum()).append('\n');
        out.append("cutoutLeafVanillaOpaque=").append(cutoutLeafVanillaOpaque.sum()).append('\n');
        out.append("cutoutFarChecks=").append(cutoutFarChecks.sum()).append('\n');
        out.append("cutoutFarReduced=").append(cutoutFarReduced.sum()).append('\n');
        out.append("cutoutFarRejectedNear=").append(cutoutFarRejectedNear.sum()).append('\n');
        out.append("cutoutFarRejectedSmall=").append(cutoutFarRejectedSmall.sum()).append('\n');
        out.append("cutoutFarRejectedShape=").append(cutoutFarRejectedShape.sum()).append('\n');
        out.append("cutoutFarSourceQuads=").append(cutoutFarSourceQuads.sum()).append('\n');
        out.append("cutoutFarReducedQuads=").append(cutoutFarReducedQuads.sum()).append('\n');
        out.append("cutoutAggPasses=").append(cutoutAggPasses.sum()).append('\n');
        out.append("cutoutAggListsBefore=").append(cutoutAggListsBefore.sum()).append('\n');
        out.append("cutoutAggListsAfter=").append(cutoutAggListsAfter.sum()).append('\n');
        out.append("falseEmptySectionLevelMisses=").append(falseEmptySectionLevelMisses.sum()).append('\n');
        out.append("falseEmptySectionCandidates=").append(falseEmptySectionCandidates.sum()).append('\n');
        out.append("falseEmptySectionChecks=").append(falseEmptySectionChecks.sum()).append('\n');
        out.append("falseEmptySectionRepairs=").append(falseEmptySectionRepairs.sum()).append('\n');
        out.append("falseEmptySectionResets=").append(falseEmptySectionResets.sum()).append('\n');
    }

    private static int dominantFace(float nx, float ny, float nz) {
        float ax = Math.abs(nx);
        float ay = Math.abs(ny);
        float az = Math.abs(nz);
        if (ax >= ay && ax >= az) return nx >= 0.0f ? 0 : 1;
        if (ay >= az) return ny >= 0.0f ? 2 : 3;
        return nz >= 0.0f ? 4 : 5;
    }
}
