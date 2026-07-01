package rendersnap.star.end.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import rendersnap.star.end.client.cfg.Opts;
import rendersnap.star.end.client.render.Cuts;

public final class WorldWarmup {
    private static ClientLevel lastLevel;

    private WorldWarmup() {
    }

    public static void tick(Minecraft mc) {
        if (mc.level != lastLevel) {
            joined(mc.level);
        }

        Cuts.tickWorld(mc);
        Cuts.tickWarmup();
    }

    private static void joined(ClientLevel level) {
        lastLevel = level;
        Cuts.clearWorldState();
        if (level == null) {
            return;
        }
        if (!Opts.fastLaunch) return;

        Cuts.warmup(100);
    }
}
