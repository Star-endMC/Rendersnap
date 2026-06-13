package rendersnap.star.end.client;

import net.fabricmc.loader.api.FabricLoader;
import org.admany.quantified.api.QuantifiedAPI;
import rendersnap.star.end.Rendersnap;

public final class QuantifiedSupport {
    private static final boolean LOADED = FabricLoader.getInstance().isModLoaded("quantified");

    private QuantifiedSupport() {
    }

    public static boolean loaded() {
        return LOADED;
    }

    public static void registerMod() {
        if (LOADED) {
            QuantifiedAPI.register(Rendersnap.MOD_ID);
        }
    }
}
