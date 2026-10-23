package rendersnap.star.end.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionfc;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import org.joml.Vector3f;
import rendersnap.star.end.client.FrameMet;
import rendersnap.star.end.client.render.zoom.Zoom;
import rendersnap.star.end.client.Compat;

import java.util.concurrent.atomic.LongAdder;

public final class Cuts {
    private static final LongAdder ENTITY_CHECKS = new LongAdder();
    private static final LongAdder ENTITY_CULLED = new LongAdder();
    private static final LongAdder AO_CHECKS = new LongAdder();
    private static final LongAdder AO_TRIMMED = new LongAdder();
    private static final LongAdder LAYER_CHECKS = new LongAdder();
    private static final LongAdder LAYER_SKIPPED = new LongAdder();
    private static final LongAdder CAVE_CHECKS = new LongAdder();
    private static final LongAdder CAVE_CULLED = new LongAdder();
    private static final LongAdder RESORT_CHECKS = new LongAdder();
    private static final LongAdder RESORT_SKIPPED = new LongAdder();
    private static final Vector3f FORWARD = new Vector3f();

    private static final double ENTITY_CULL_MIN_DISTANCE_SQ = 128.0 * 128.0;

    private static final LongOpenHashSet VISIBLE_SECTIONS = new LongOpenHashSet(4096);
    private static int visibleSectionsFrame = -1;

    private static volatile boolean chunkShadeTrim;
    private static volatile boolean entityCulling;
    private static volatile boolean fogOcclusion;
    private static volatile boolean caveCulling;
    private static volatile boolean hideWeather;
    private static volatile int behindCamMode;
    private static volatile int warmupTicks;

    private static volatile double cameraX;
    private static volatile double cameraY;
    private static volatile double cameraZ;
    private static volatile float forwardX;
    private static volatile float forwardY;
    private static volatile float forwardZ;
    private static volatile boolean cameraReady;
    private static volatile boolean fastCamera;
    private static long lastCameraNs;

    private static final double FAST_TURN_RAD_PER_SEC = Math.toRadians(720.0);
    private static final double FAST_MOVE_BLOCKS_PER_SEC = 60.0;

    private Cuts() {
    }

    public static void setChunkShadeTrim(boolean on) {
        chunkShadeTrim = on;
    }

    public static void setEntityCulling(boolean on) {
        entityCulling = on;
    }

    public static void setFogOcclusion(boolean on) {
        fogOcclusion = on;
        Fog.setOcclusionEnabled(on);
    }

    public static void setBehindCamMode(int mode) {
        behindCamMode = Mth.clamp(mode, 0, 2);
    }

    public static void setCaveCulling(boolean on) {
        caveCulling = on;
    }

    public static void setHideWeather(boolean on) {
        hideWeather = on;
    }

    public static boolean shouldHideWeather() {
        return hideWeather;
    }

    public static void warmup(int ticks) {
        warmupTicks = Math.max(warmupTicks, ticks);
    }

    public static void stopWarmup() {
        warmupTicks = 0;
    }

    public static void beginRendererTransition() {
        warmupTicks = Math.max(warmupTicks, 200);
    }

    public static void clearWorldState() {
        warmupTicks = 0;
        ShadReuse.clear();
        cameraX = 0.0;
        cameraY = 0.0;
        cameraZ = 0.0;
        forwardX = 0.0f;
        forwardY = 0.0f;
        forwardZ = 0.0f;
        cameraReady = false;
        fastCamera = false;
        lastCameraNs = 0L;
    }

    public static void tickWarmup() {
        if (warmupTicks > 0) warmupTicks--;
    }

    public static void updateCamera(Vec3 pos, Quaternionfc orientation) {
        double moveX = pos.x - cameraX;
        double moveY = pos.y - cameraY;
        double moveZ = pos.z - cameraZ;
        float oldForwardX = forwardX;
        float oldForwardY = forwardY;
        float oldForwardZ = forwardZ;
        cameraX = pos.x;
        cameraY = pos.y;
        cameraZ = pos.z;
        FORWARD.set(0.0f, 0.0f, -1.0f).rotate(orientation);
        forwardX = FORWARD.x;
        forwardY = FORWARD.y;
        forwardZ = FORWARD.z;
        long now = System.nanoTime();
        double dt = lastCameraNs == 0L ? 0.0 : (now - lastCameraNs) / 1_000_000_000.0;
        lastCameraNs = now;
        if (cameraReady && dt > 0.0) {

            double turn = Mth.clamp(oldForwardX * forwardX + oldForwardY * forwardY + oldForwardZ * forwardZ, -1.0, 1.0);
            double turnSpeed = Math.acos(turn) / dt;
            double moveSpeed = Math.sqrt(moveX * moveX + moveY * moveY + moveZ * moveZ) / dt;
            fastCamera = turnSpeed > FAST_TURN_RAD_PER_SEC || moveSpeed > FAST_MOVE_BLOCKS_PER_SEC;
        } else {
            cameraReady = true;
            fastCamera = false;
        }
    }

    public static boolean isCameraMovingFast() {
        return fastCamera;
    }

    public static boolean shouldCullEntity(Entity e) {
        if (!entityCulling || !Compat.allowsVanillaHooks() || warming() || e == null || e instanceof Player || e.isPassenger()) return false;
        ENTITY_CHECKS.increment();
        Minecraft mc = Minecraft.getInstance();
        if (mc.levelRenderer == null || mc.level == null) return false;

        double cx;
        double cy;
        double cz;
        if (cameraReady) {
            cx = cameraX;
            cy = cameraY;
            cz = cameraZ;
        } else if (mc.player != null) {
            cx = mc.player.getX();
            cy = mc.player.getY();
            cz = mc.player.getZ();
        } else {
            return false;
        }
        if (e.distanceToSqr(cx, cy, cz) < ENTITY_CULL_MIN_DISTANCE_SQ) return false;
        BlockPos pos = e.blockPosition();
        if (mc.level.isOutsideBuildHeight(pos.getY())) return false;
        //? if >=26.2 {

        if (!isSectionVisible(mc, pos)) {
            ENTITY_CULLED.increment();
            return true;
        }
        return false;
        //?} else {
        /*if (mc.levelRenderer.isSectionCompiledAndVisible(pos)) return false;
        ENTITY_CULLED.increment();
        return true;
        *///?}
    }

    //? if >=26.2 {
    private static boolean isSectionVisible(Minecraft mc, BlockPos pos) {
        int frame = FrameMet.frameSerial();
        if (frame != visibleSectionsFrame) {
            visibleSectionsFrame = frame;
            VISIBLE_SECTIONS.clear();
            for (SectionRenderDispatcher.RenderSection section : mc.levelRenderer.visibleSections()) {
                VISIBLE_SECTIONS.add(section.getSectionNode());
            }
        }

        return VISIBLE_SECTIONS.isEmpty() || VISIBLE_SECTIONS.contains(SectionPos.asLong(pos));
    }
    //?}

    public static boolean shouldUseChunkAo(BlockPos pos) {

        if (!chunkShadeTrim || warming() || pos == null || Zoom.isZooming()) return true;
        AO_CHECKS.increment();

        if (!cameraReady) return true;
        if (distanceSq(pos) < 128.0 * 128.0) return true;
        AO_TRIMMED.increment();
        return false;
    }

    public static boolean trimsFarLayers() {
        return fogOcclusion;
    }

    public static boolean shouldSkipSectionLayer(BlockPos pos, ChunkSectionLayer layer) {
        if (!fogOcclusion || !Compat.allowsVanillaHooks() || warming() || pos == null || layer == ChunkSectionLayer.SOLID || Zoom.isZooming()) return false;
        if (!cameraReady) return false;
        LAYER_CHECKS.increment();

        double distSq = distanceSq(pos);
        boolean skip = Fog.isOccludingDistanceSq(distSq);
        if (skip) LAYER_SKIPPED.increment();
        return skip;
    }

    public static boolean shouldSkipFarTranslucencyResort(SectionRenderDispatcher.RenderSection section, Vec3 cameraPos, boolean near) {
        if (behindCamMode == 0 || !Compat.allowsVanillaHooks() || warming() || near || section == null || cameraPos == null || Zoom.isZooming()) return false;
        RESORT_CHECKS.increment();

        BlockPos pos = section.getRenderOrigin();
        double dx = pos.getX() + 8.0 - cameraPos.x;
        double dy = pos.getY() + 8.0 - cameraPos.y;
        double dz = pos.getZ() + 8.0 - cameraPos.z;
        double distSq = dx * dx + dy * dy + dz * dz;
        if (distSq < 40.0 * 40.0 || !behind(dx, dy, dz, distSq)) return false;

        RESORT_SKIPPED.increment();
        return true;
    }

    public static boolean isCaveCullingActive() {
        return caveCulling && cameraReady && Compat.allowsVanillaHooks() && !warming() && !Zoom.isZooming();
    }

    public static boolean shouldCullCaveSection(BlockPos pos) {
        if (!caveCulling || !Compat.allowsVanillaHooks() || warming() || pos == null || Zoom.isZooming()) return false;
        if (!cameraReady) return false;
        CAVE_CHECKS.increment();
        double distSq = distanceSq(pos);
        if (distSq < 48.0 * 48.0 || pos.getY() + 8.0 >= cameraY - 8.0) return false;
        double dx = pos.getX() + 8.0 - cameraX;
        double dy = pos.getY() + 8.0 - cameraY;
        double dz = pos.getZ() + 8.0 - cameraZ;
        if (!behind(dx, dy, dz, distSq)) return false;
        CAVE_CULLED.increment();
        return true;
    }

    public static void appendDebug(StringBuilder out) {
        out.append("entityChecks=").append(ENTITY_CHECKS.sum()).append('\n');
        out.append("entityCulled=").append(ENTITY_CULLED.sum()).append('\n');
        out.append("chunkAoChecks=").append(AO_CHECKS.sum()).append('\n');
        out.append("chunkAoTrimmed=").append(AO_TRIMMED.sum()).append('\n');
        out.append("layerChecks=").append(LAYER_CHECKS.sum()).append('\n');
        out.append("layersSkipped=").append(LAYER_SKIPPED.sum()).append('\n');
        out.append("caveChecks=").append(CAVE_CHECKS.sum()).append('\n');
        out.append("caveCulled=").append(CAVE_CULLED.sum()).append('\n');
        out.append("resortChecks=").append(RESORT_CHECKS.sum()).append('\n');
        out.append("resortsSkipped=").append(RESORT_SKIPPED.sum()).append('\n');
        out.append("warmupTicks=").append(warmupTicks).append('\n');
    }

    public static void appendSummary(StringBuilder out) {
        out.append("Entity culling: ").append(entityCulling ? "ENABLED" : "DISABLED").append('\n');
        out.append("  Removed ").append(ENTITY_CULLED.sum()).append(" entities outside visible terrain.\n");
        out.append("Cave culling: ").append(caveCulling ? "ENABLED" : "DISABLED").append('\n');
        out.append("  Checked ").append(CAVE_CHECKS.sum()).append(" sections and removed ").append(CAVE_CULLED.sum()).append(".\n");
        out.append("Layer culling: ").append(LAYER_CHECKS.sum() == 0 ? "INACTIVE" : "ACTIVE").append('\n');
        out.append("  Checked ").append(LAYER_CHECKS.sum()).append(" layers and skipped ").append(LAYER_SKIPPED.sum()).append(".\n");
    }

    private static boolean warming() {
        return warmupTicks > 0;
    }

    private static double distanceSq(BlockPos pos) {
        double dx = pos.getX() + 8.0 - cameraX;
        double dy = pos.getY() + 8.0 - cameraY;
        double dz = pos.getZ() + 8.0 - cameraZ;
        return dx * dx + dy * dy + dz * dz;
    }

    private static boolean behind(double dx, double dy, double dz, double distanceSq) {
        if (distanceSq <= 0.001) return false;
        double dot = dx * forwardX + dy * forwardY + dz * forwardZ;
        return dot < 0.0 && dot * dot > distanceSq * 0.1225;
    }

}
