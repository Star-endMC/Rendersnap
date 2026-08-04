package rendersnap.star.end.client;

import net.minecraft.client.Options;
import rendersnap.star.end.client.cfg.Opts;
import rendersnap.star.end.client.render.Cuts;
import rendersnap.star.end.client.render.ShadowReuse;

public final class GraphicsPresetGuard {
    private static int distanceBeforePreset = -1;

    private GraphicsPresetGuard() {
    }

    public static void beforeApply(Options options) {
        distanceBeforePreset = options.renderDistance().get();
        ShadowReuse.clear();
        Cuts.beginRendererTransition();
    }

    public static void afterApply(Options options) {
        int cap = Opts.graphicsPresetDistanceCap;
        int selected = options.renderDistance().get();
        if (cap > 0 && selected > cap) {
            options.renderDistance().set(cap);
        } else if (distanceBeforePreset < 0) {
            return;
        }
        distanceBeforePreset = -1;
    }
}
