package rendersnap.star.end.client;

import net.fabricmc.loader.api.FabricLoader;
import rendersnap.star.end.Rendersnap;
import rendersnap.star.end.client.cfg.Opts;

public final class Compatibility {
    private static boolean initialized;
    private static String renderer = "Vanilla renderer";
    private static boolean vanillaHooks = true;

    private Compatibility() {
    }

    public static void initialize() {
        if (initialized) return;
        initialized = true;
        FabricLoader loader = FabricLoader.getInstance();
        if (loader.isModLoaded("iris")) {
            renderer = "Iris";
            vanillaHooks = false;
        } else if (loader.isModLoaded("sodium")) {
            renderer = "Sodium";
            vanillaHooks = false;
        } else if (loader.isModLoaded("canvas")) {
            renderer = "Canvas";
            vanillaHooks = false;
        }
        if (!vanillaHooks) {
            Rendersnap.LOGGER.warn("{} detected; vanilla terrain hooks are disabled.", renderer);
        }
    }

    public static boolean allowsVanillaHooks() {
        return vanillaHooks;
    }

    public static String rendererStatus() {
        return vanillaHooks ? "Vanilla renderer active" : renderer + " detected: terrain hooks disabled";
    }

    public static boolean disableConflictingOptions() {
        if (vanillaHooks) return false;

        boolean changed = Opts.farLayerTrim || Opts.fogOcclusion || Opts.behindCamMode != Opts.BEHIND_CAM_OFF;
        Opts.farLayerTrim = false;
        Opts.fogOcclusion = false;
        Opts.behindCamMode = Opts.BEHIND_CAM_OFF;
        return changed;
    }
}
