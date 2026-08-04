package rendersnap.star.end.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import rendersnap.star.end.client.render.zoom.Zoom;
import rendersnap.star.end.client.Compatibility;

import java.util.concurrent.atomic.LongAdder;

public final class Cuts {
    private static final LongAdder ENTITY_CHECKS = new LongAdder();
    private static final LongAdder ENTITY_CULLED = new LongAdder();
    private static final LongAdder AO_CHECKS = new LongAdder();
    private static final LongAdder AO_TRIMMED = new LongAdder();
    private static final LongAdder LAYER_CHECKS = new LongAdder();
    private static final LongAdder LAYER_SKIPPED = new LongAdder();
    private static final LongAdder RESORT_CHECKS = new LongAdder();
    private static final LongAdder RESORT_SKIPPED = new LongAdder();
    private static final Vector3f FORWARD = new Vector3f();

    private static volatile boolean chunkShadeTrim;
    private static volatile boolean farLayerTrim;
    private static volatile boolean entityCulling;
    private static volatile boolean fogOcclusion;
    private static volatile boolean hideWeather;
    private static volatile int behindCamMode;
    private static volatile int warmupTicks;
    private static double cameraX;
    private static double cameraY;
    private static double cameraZ;
    private static float forwardX;
    private static float forwardY;
    private static float forwardZ;

    private Cuts() {
    }

    public static void setChunkShadeTrim(boolean on) {
        chunkShadeTrim = on;
    }

    public static void setFarLayerTrim(boolean on) {
        farLayerTrim = on;
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
        ShadowReuse.clear();
        cameraX = 0.0;
        cameraY = 0.0;
        cameraZ = 0.0;
        forwardX = 0.0f;
        forwardY = 0.0f;
        forwardZ = 0.0f;
    }

    public static void tickWarmup() {
        if (warmupTicks > 0) warmupTicks--;
    }

    public static void updateCamera(Vec3 pos, Quaternionfc orientation) {
        cameraX = pos.x;
        cameraY = pos.y;
        cameraZ = pos.z;
        FORWARD.set(0.0f, 0.0f, -1.0f).rotate(orientation);
        forwardX = FORWARD.x;
        forwardY = FORWARD.y;
        forwardZ = FORWARD.z;
    }

    public static boolean shouldCullEntity(Entity e, Frustum frustum, double camX, double camY, double camZ) {
        ENTITY_CHECKS.increment();
        if (!Compatibility.allowsVanillaHooks() || !entityCulling || warming() || e == null || e instanceof Player || e.isPassenger()) return false;
        if (frustum == null || e.distanceToSqr(camX, camY, camZ) < 64.0 * 64.0) return false;

        AABB box = e.getBoundingBox();
        if (!frustum.isVisible(box)) return false;
        Minecraft mc = Minecraft.getInstance();
        if (mc.levelRenderer == null || mc.levelRenderer.isSectionCompiledAndVisible(e.blockPosition())) return false;

        ENTITY_CULLED.increment();
        return true;
    }

    public static boolean shouldUseChunkAo(BlockPos pos) {
        AO_CHECKS.increment();
        if (!chunkShadeTrim || warming() || pos == null || Zoom.isZooming()) return true;
        if (distanceSq(pos) < 128.0 * 128.0) return true;
        AO_TRIMMED.increment();
        return false;
    }

    public static boolean trimsFarLayers() {
        return farLayerTrim || fogOcclusion;
    }

    public static boolean shouldSkipSectionLayer(BlockPos pos, ChunkSectionLayer layer) {
        LAYER_CHECKS.increment();
        if (!Compatibility.allowsVanillaHooks() || warming() || pos == null || layer == ChunkSectionLayer.SOLID || Zoom.isZooming()) return false;

        double distSq = distanceSq(pos);
        boolean skip = fogOcclusion && Fog.isOccludingDistance((float)Math.sqrt(distSq));
        if (!skip && farLayerTrim) {
            skip = distSq >= 160.0 * 160.0;
        }
        if (skip) LAYER_SKIPPED.increment();
        return skip;
    }

    public static boolean shouldSkipFarTranslucencyResort(SectionRenderDispatcher.RenderSection section, Vec3 cameraPos, boolean near) {
        RESORT_CHECKS.increment();
        if (!Compatibility.allowsVanillaHooks() || warming() || behindCamMode == 0 || near || section == null || cameraPos == null || Zoom.isZooming()) return false;

        BlockPos pos = section.getRenderOrigin();
        double dx = pos.getX() + 8.0 - cameraPos.x;
        double dy = pos.getY() + 8.0 - cameraPos.y;
        double dz = pos.getZ() + 8.0 - cameraPos.z;
        double distSq = dx * dx + dy * dy + dz * dz;
        if (distSq < 40.0 * 40.0 || !behind(dx, dy, dz, Math.sqrt(distSq))) return false;

        RESORT_SKIPPED.increment();
        return true;
    }

    public static void appendDebug(StringBuilder out) {
        out.append("entityChecks=").append(ENTITY_CHECKS.sum()).append('\n');
        out.append("entityCulled=").append(ENTITY_CULLED.sum()).append('\n');
        out.append("chunkAoChecks=").append(AO_CHECKS.sum()).append('\n');
        out.append("chunkAoTrimmed=").append(AO_TRIMMED.sum()).append('\n');
        out.append("layerChecks=").append(LAYER_CHECKS.sum()).append('\n');
        out.append("layersSkipped=").append(LAYER_SKIPPED.sum()).append('\n');
        out.append("resortChecks=").append(RESORT_CHECKS.sum()).append('\n');
        out.append("resortsSkipped=").append(RESORT_SKIPPED.sum()).append('\n');
        out.append("warmupTicks=").append(warmupTicks).append('\n');
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

    private static boolean behind(double dx, double dy, double dz, double distance) {
        if (distance <= 0.001) return false;
        return (dx * forwardX + dy * forwardY + dz * forwardZ) / distance < -0.35;
    }

}
