package rendersnap.star.end.client.cfg;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Mth;
import rendersnap.star.end.Rendersnap;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

public final class Opts {
    private static final Path CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("rendersnap.properties");
    private static final Path OLD_CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("SnapshotRender.properties");
    private static final Path LEGACY_CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("arch-optimize.properties");

    public static final boolean DEFAULT_SHOW_FPS_OVERLAY = true;
    public static final int DEFAULT_FPS_OVERLAY_X = 6;
    public static final int DEFAULT_FPS_OVERLAY_Y = 6;
    public static final int DEFAULT_ZOOM_TRANSITION = 2;
    public static final boolean DEFAULT_ZOOM_CINEMATIC = false;
    public static final boolean DEFAULT_GENTLE_WORKERS = true;
    public static final boolean DEFAULT_REBUILD_GUARD = true;
    public static final boolean DEFAULT_LIGHT_PACING = true;
    public static final boolean DEFAULT_ENTITY_CULLING = true;
    public static final boolean DEFAULT_SHADOW_REUSE = true;
    public static final boolean DEFAULT_SHADOW_FIX = true;
    public static final boolean DEFAULT_CHUNK_SHADE_TRIM = false;
    public static final boolean DEFAULT_FOG_OCCLUSION = false;
    public static final boolean DEFAULT_CAVE_CULLING = true;
    public static final boolean DEFAULT_LIQUID_FIX = false;
    public static final int BEHIND_CAM_OFF = 0;
    public static final int BEHIND_CAM_NORMAL = 1;
    public static final int BEHIND_CAM_HIGH = 2;
    public static final int DEFAULT_BEHIND_CAM_MODE = BEHIND_CAM_OFF;
    public static final boolean DEFAULT_FAST_LAUNCH = true;
    public static final boolean DEFAULT_HIDE_WEATHER = false;
    public static final boolean DEFAULT_CHUNK_PACING = false;
    public static final int DEFAULT_CHUNK_WORKER_LIMIT = Math.max(2, Math.min(8, Runtime.getRuntime().availableProcessors() - 1));

    public static boolean showFpsOverlay = DEFAULT_SHOW_FPS_OVERLAY;
    public static int fpsOverlayX = DEFAULT_FPS_OVERLAY_X;
    public static int fpsOverlayY = DEFAULT_FPS_OVERLAY_Y;
    public static int zoomTransition = DEFAULT_ZOOM_TRANSITION;
    public static boolean zoomCinematic = DEFAULT_ZOOM_CINEMATIC;
    public static boolean gentleWorkers = DEFAULT_GENTLE_WORKERS;
    public static boolean rebuildGuard = DEFAULT_REBUILD_GUARD;
    public static boolean lightPacing = DEFAULT_LIGHT_PACING;
    public static boolean entityCulling = DEFAULT_ENTITY_CULLING;
    public static boolean shadowReuse = DEFAULT_SHADOW_REUSE;
    public static boolean shadowFix = DEFAULT_SHADOW_FIX;
    public static boolean chunkShadeTrim = DEFAULT_CHUNK_SHADE_TRIM;
    public static boolean fogOcclusion = DEFAULT_FOG_OCCLUSION;
    public static boolean caveCulling = DEFAULT_CAVE_CULLING;
    public static boolean liquidFix = DEFAULT_LIQUID_FIX;
    public static int behindCamMode = DEFAULT_BEHIND_CAM_MODE;
    public static boolean fastLaunch = DEFAULT_FAST_LAUNCH;
    public static boolean hideWeather = DEFAULT_HIDE_WEATHER;
    public static boolean chunkPacing = DEFAULT_CHUNK_PACING;
    public static int chunkWorkerLimit = DEFAULT_CHUNK_WORKER_LIMIT;
    public static int chunkUpdateMode = -1;

    private Opts() {
    }

    public static void save() {
        Properties p = new Properties();
        put(p);
        try {
            Files.createDirectories(CONFIG_FILE.getParent());

            Path tmp = CONFIG_FILE.resolveSibling(CONFIG_FILE.getFileName() + ".tmp");
            try (OutputStream os = Files.newOutputStream(tmp)) {
                p.store(os, "Rendersnap 26.2");
            }
            try {
                Files.move(tmp, CONFIG_FILE, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, CONFIG_FILE, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            Rendersnap.LOGGER.warn("Couldn't save rendersnap.properties", e);
        }
    }

    public static void load() {
        Path path = configPath();
        if (!Files.exists(path)) return;

        Properties p = new Properties();
        try (InputStream is = Files.newInputStream(path)) {
            p.load(is);
        } catch (IOException e) {
            Rendersnap.LOGGER.warn("Couldn't load {}", path.getFileName(), e);
            return;
        }

        showFpsOverlay = bool(p, "showFpsOverlay", showFpsOverlay);
        fpsOverlayX = Math.max(0, intV(p, "fpsOverlayX", fpsOverlayX));
        fpsOverlayY = Math.max(0, intV(p, "fpsOverlayY", fpsOverlayY));
        zoomTransition = Mth.clamp(intV(p, "zoomTransition", zoomTransition), 0, 3);
        zoomCinematic = bool(p, "zoomCinematic", zoomCinematic);
        gentleWorkers = bool(p, "gentleWorkers", gentleWorkers);
        rebuildGuard = bool(p, "rebuildGuard", rebuildGuard);
        lightPacing = bool(p, "lightPacing", lightPacing);
        entityCulling = bool(p, "entityCulling", entityCulling);
        shadowReuse = bool(p, "shadowReuse", shadowReuse);
        shadowFix = bool(p, "shadowFix", shadowFix);
        chunkShadeTrim = bool(p, "chunkShadeTrim", chunkShadeTrim);
        fogOcclusion = bool(p, "fogOcclusion", fogOcclusion);
        caveCulling = bool(p, "caveCulling", caveCulling);
        liquidFix = bool(p, "liquidFix", liquidFix);
        int legacyBehindCam = bool(p, "behindCamDrawCut", false) ? BEHIND_CAM_NORMAL : behindCamMode;
        behindCamMode = Mth.clamp(intV(p, "behindCamMode", legacyBehindCam), BEHIND_CAM_OFF, BEHIND_CAM_HIGH);
        fastLaunch = bool(p, "fastLaunch", fastLaunch);
        hideWeather = bool(p, "hideWeather", hideWeather);
        chunkPacing = bool(p, "chunkPacing", bool(p, "multiRender", chunkPacing));
        chunkWorkerLimit = Mth.clamp(intV(p, "chunkWorkerLimit", chunkWorkerLimit), 2, 8);
        chunkUpdateMode = Mth.clamp(intV(p, "chunkUpdateMode", chunkUpdateMode), -1, 2);
    }

    private static void put(Properties p) {
        p.setProperty("showFpsOverlay", String.valueOf(showFpsOverlay));
        p.setProperty("fpsOverlayX", String.valueOf(fpsOverlayX));
        p.setProperty("fpsOverlayY", String.valueOf(fpsOverlayY));
        p.setProperty("zoomTransition", String.valueOf(zoomTransition));
        p.setProperty("zoomCinematic", String.valueOf(zoomCinematic));
        p.setProperty("gentleWorkers", String.valueOf(gentleWorkers));
        p.setProperty("rebuildGuard", String.valueOf(rebuildGuard));
        p.setProperty("lightPacing", String.valueOf(lightPacing));
        p.setProperty("entityCulling", String.valueOf(entityCulling));
        p.setProperty("shadowReuse", String.valueOf(shadowReuse));
        p.setProperty("shadowFix", String.valueOf(shadowFix));
        p.setProperty("chunkShadeTrim", String.valueOf(chunkShadeTrim));
        p.setProperty("fogOcclusion", String.valueOf(fogOcclusion));
        p.setProperty("caveCulling", String.valueOf(caveCulling));
        p.setProperty("liquidFix", String.valueOf(liquidFix));
        p.setProperty("behindCamMode", String.valueOf(behindCamMode));
        p.setProperty("fastLaunch", String.valueOf(fastLaunch));
        p.setProperty("hideWeather", String.valueOf(hideWeather));
        p.setProperty("chunkPacing", String.valueOf(chunkPacing));
        p.setProperty("chunkWorkerLimit", String.valueOf(chunkWorkerLimit));
        p.setProperty("chunkUpdateMode", String.valueOf(chunkUpdateMode));
    }

    private static Path configPath() {
        if (Files.exists(CONFIG_FILE)) return CONFIG_FILE;
        if (Files.exists(OLD_CONFIG_FILE)) return OLD_CONFIG_FILE;
        return LEGACY_CONFIG_FILE;
    }

    private static boolean bool(Properties p, String key, boolean fallback) {
        String raw = p.getProperty(key);
        return raw == null ? fallback : Boolean.parseBoolean(raw);
    }

    private static int intV(Properties p, String key, int fallback) {
        try {
            return Integer.parseInt(p.getProperty(key, String.valueOf(fallback)));
        } catch (NumberFormatException e) {
            Rendersnap.LOGGER.warn("Bad number for {} in rendersnap.properties, using {}", key, fallback, e);
            return fallback;
        }
    }
}
