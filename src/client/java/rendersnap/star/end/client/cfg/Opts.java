package rendersnap.star.end.client.cfg;

import rendersnap.star.end.Rendersnap;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Mth;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class Opts {

    private Opts() {}

    private static final Path CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("rendersnap.properties");
    private static final Path OLD_CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("SnapshotRender.properties");
    private static final Path LEGACY_CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("arch-optimize.properties");

    public static final boolean DEFAULT_SHOW_FPS_OVERLAY = true;
    public static final int DEFAULT_FPS_OVERLAY_X = 6;
    public static final int DEFAULT_FPS_OVERLAY_Y = 6;
    public static final int DEFAULT_ZOOM_TRANSITION = 2;
    public static final boolean DEFAULT_BLOCK_FACE_CULLING = true;
    public static final boolean DEFAULT_TEXTURE_LOD = false;
    public static final boolean DEFAULT_CHUNK_SHADE_TRIM = true;
    public static final boolean DEFAULT_FAR_LAYER_TRIM = false;
    public static final boolean DEFAULT_ENTITY_CULLING = true;
    public static final boolean DEFAULT_OCCLUSION_CULLING = true;
    public static final boolean DEFAULT_FOG_OCCLUSION = true;
    public static final int BEHIND_CAM_OFF = 0;
    public static final int BEHIND_CAM_NORMAL = 1;
    public static final int BEHIND_CAM_HIGH = 2;
    public static final int DEFAULT_BEHIND_CAM_MODE = BEHIND_CAM_OFF;
    public static final boolean DEFAULT_LIGHTING_CHUNK_TRIM = true;
    public static final boolean DEFAULT_FAST_LAUNCH = true;
    public static final boolean DEFAULT_HIDE_WEATHER = false;
    public static final boolean DEFAULT_FLUID_OPTIMIZER = true;
    public static final boolean DEFAULT_MULTI_RENDER = false;

    public static boolean showFpsOverlay = DEFAULT_SHOW_FPS_OVERLAY;
    public static int fpsOverlayX = DEFAULT_FPS_OVERLAY_X;
    public static int fpsOverlayY = DEFAULT_FPS_OVERLAY_Y;
    public static int zoomTransition = DEFAULT_ZOOM_TRANSITION;

    public static boolean blockFaceCulling = DEFAULT_BLOCK_FACE_CULLING;
    public static boolean textureLod = DEFAULT_TEXTURE_LOD;
    public static boolean chunkShadeTrim = DEFAULT_CHUNK_SHADE_TRIM;
    public static boolean farLayerTrim = DEFAULT_FAR_LAYER_TRIM;
    public static boolean entityCulling = DEFAULT_ENTITY_CULLING;
    public static boolean occlusionCulling = DEFAULT_OCCLUSION_CULLING;
    public static boolean fogOcclusion = DEFAULT_FOG_OCCLUSION;
    public static int behindCamMode = DEFAULT_BEHIND_CAM_MODE;
    public static boolean lightingChunkTrim = DEFAULT_LIGHTING_CHUNK_TRIM;

    public static boolean fastLaunch = DEFAULT_FAST_LAUNCH;
    public static boolean hideWeather = DEFAULT_HIDE_WEATHER;
    public static boolean fluidOptimizer = DEFAULT_FLUID_OPTIMIZER;
    public static boolean multiRender = DEFAULT_MULTI_RENDER;
    public static int chunkUpdateMode = -1;

    public static void resetAllToDefaults() {
        defaults();
    }

    public static void save() {
        Properties p = new Properties();
        put(p);

        try {
            Files.createDirectories(CONFIG_FILE.getParent());
            try (OutputStream os = Files.newOutputStream(CONFIG_FILE)) {
                p.store(os, "Rendersnap");
            }
        } catch (IOException e) {
            Rendersnap.LOGGER.warn("Couldn't save rendersnap.properties", e);
        }
    }

    public static void load() {
        Path loadFile = configPath();
        if (!Files.exists(loadFile)) {
            return;
        }

        Properties p = new Properties();
        try (InputStream is = Files.newInputStream(loadFile)) {
            p.load(is);
        } catch (IOException e) {
            Rendersnap.LOGGER.warn("Couldn't load {}", loadFile.getFileName(), e);
            return;
        }

        showFpsOverlay = bool(p, "showFpsOverlay", showFpsOverlay);
        fpsOverlayX = intV(p, "fpsOverlayX", fpsOverlayX);
        fpsOverlayY = intV(p, "fpsOverlayY", fpsOverlayY);
        zoomTransition = Mth.clamp(intV(p, "zoomTransition", zoomTransition), 0, 3);

        blockFaceCulling = bool(p, "blockFaceCulling", blockFaceCulling);
        textureLod = bool(p, "textureLod", textureLod);
        chunkShadeTrim = bool(p, "chunkShadeTrim", chunkShadeTrim);
        farLayerTrim = bool(p, "farLayerTrim", farLayerTrim);
        entityCulling = bool(p, "entityCulling", entityCulling);
        occlusionCulling = bool(p, "occlusionCulling", occlusionCulling);
        fogOcclusion = bool(p, "fogOcclusion", fogOcclusion);
        int oldBehindCam = bool(p, "behindCamDrawCut", false) ? BEHIND_CAM_NORMAL : DEFAULT_BEHIND_CAM_MODE;
        behindCamMode = Mth.clamp(intV(p, "behindCamMode", oldBehindCam), BEHIND_CAM_OFF, BEHIND_CAM_HIGH);
        lightingChunkTrim = bool(p, "lightingChunkTrim", lightingChunkTrim);

        fastLaunch = bool(p, "fastLaunch", fastLaunch);
        hideWeather = bool(p, "hideWeather", hideWeather);
        fluidOptimizer = bool(p, "fluidOptimizer", fluidOptimizer);
        multiRender = bool(p, "multiRender", multiRender);
        chunkUpdateMode = Mth.clamp(intV(p, "chunkUpdateMode", chunkUpdateMode), -1, 2);
    }

    private static boolean bool(Properties p, String key, boolean fallback) {
        String raw = p.getProperty(key);
        return raw != null ? Boolean.parseBoolean(raw) : fallback;
    }

    private static int intV(Properties p, String key, int fallback) {
        try {
            return Integer.parseInt(p.getProperty(key, String.valueOf(fallback)));
        } catch (NumberFormatException e) {
            Rendersnap.LOGGER.warn("Bad number for {} in rendersnap.properties, using {}", key, fallback, e);
            return fallback;
        }
    }

    private static Path configPath() {
        if (Files.exists(CONFIG_FILE)) return CONFIG_FILE;
        if (Files.exists(OLD_CONFIG_FILE)) return OLD_CONFIG_FILE;
        return LEGACY_CONFIG_FILE;
    }

    private static void put(Properties p) {
        p.setProperty("showFpsOverlay", String.valueOf(showFpsOverlay));
        p.setProperty("fpsOverlayX", String.valueOf(fpsOverlayX));
        p.setProperty("fpsOverlayY", String.valueOf(fpsOverlayY));
        p.setProperty("zoomTransition", String.valueOf(zoomTransition));

        p.setProperty("blockFaceCulling", String.valueOf(blockFaceCulling));
        p.setProperty("textureLod", String.valueOf(textureLod));
        p.setProperty("chunkShadeTrim", String.valueOf(chunkShadeTrim));
        p.setProperty("farLayerTrim", String.valueOf(farLayerTrim));
        p.setProperty("entityCulling", String.valueOf(entityCulling));
        p.setProperty("occlusionCulling", String.valueOf(occlusionCulling));
        p.setProperty("fogOcclusion", String.valueOf(fogOcclusion));
        p.setProperty("behindCamMode", String.valueOf(behindCamMode));
        p.setProperty("lightingChunkTrim", String.valueOf(lightingChunkTrim));
        p.setProperty("fastLaunch", String.valueOf(fastLaunch));

        p.setProperty("hideWeather", String.valueOf(hideWeather));
        p.setProperty("fluidOptimizer", String.valueOf(fluidOptimizer));
        p.setProperty("multiRender", String.valueOf(multiRender));
        p.setProperty("chunkUpdateMode", String.valueOf(chunkUpdateMode));
    }

    private static void defaults() {
        showFpsOverlay = DEFAULT_SHOW_FPS_OVERLAY;
        fpsOverlayX = DEFAULT_FPS_OVERLAY_X;
        fpsOverlayY = DEFAULT_FPS_OVERLAY_Y;
        zoomTransition = DEFAULT_ZOOM_TRANSITION;

        blockFaceCulling = DEFAULT_BLOCK_FACE_CULLING;
        textureLod = DEFAULT_TEXTURE_LOD;
        chunkShadeTrim = DEFAULT_CHUNK_SHADE_TRIM;
        farLayerTrim = DEFAULT_FAR_LAYER_TRIM;
        entityCulling = DEFAULT_ENTITY_CULLING;
        occlusionCulling = DEFAULT_OCCLUSION_CULLING;
        fogOcclusion = DEFAULT_FOG_OCCLUSION;
        behindCamMode = DEFAULT_BEHIND_CAM_MODE;
        lightingChunkTrim = DEFAULT_LIGHTING_CHUNK_TRIM;
        fastLaunch = DEFAULT_FAST_LAUNCH;

        hideWeather = DEFAULT_HIDE_WEATHER;
        fluidOptimizer = DEFAULT_FLUID_OPTIMIZER;
        multiRender = DEFAULT_MULTI_RENDER;
        chunkUpdateMode = -1;
    }
}
