package rendersnap.star.end.client;

import net.fabricmc.loader.api.FabricLoader;
import org.admany.quantified.api.CacheRequest;
import org.admany.quantified.api.QuantifiedAPI;
import rendersnap.star.end.Rendersnap;

import java.time.Duration;

public final class QuantifiedSupport {
    private static final boolean LOADED = FabricLoader.getInstance().isModLoaded("quantified");
    private static CacheRequest lightCache;

    private QuantifiedSupport() {
    }

    public static boolean loaded() {
        return LOADED;
    }

    public static void registerMod() {
        if (!LOADED) {
            return;
        }
        var meta = FabricLoader.getInstance()
                .getModContainer(Rendersnap.MOD_ID)
                .map(container -> container.getMetadata())
                .orElse(null);
        String name = meta != null ? meta.getName() : "Rendersnap";
        String version = meta != null ? meta.getVersion().getFriendlyString() : "unknown";
        QuantifiedAPI.registerV2(Rendersnap.MOD_ID, name, version);
    }

    public static CacheRequest lightCache(Duration ttl, long maxEntries) {
        if (!LOADED) {
            return null;
        }
        CacheRequest bucket = lightCache;
        if (bucket != null) {
            return bucket;
        }
        synchronized (QuantifiedSupport.class) {
            if (lightCache == null) {
                lightCache = QuantifiedAPI.cache(Rendersnap.MOD_ID, "light_cache")
                        .memoryOnly()
                        .maxEntries(maxEntries)
                        .ttl(ttl)
                        .fixedTtl();
            }
            return lightCache;
        }
    }
}
