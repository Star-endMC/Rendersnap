package rendersnap.star.end.client;

import net.minecraft.client.Options;
import net.minecraft.client.PrioritizeChunkUpdates;
import rendersnap.star.end.client.cfg.Opts;

public final class ChunkUpdatePacing {
    private static final PrioritizeChunkUpdates[] MODES = PrioritizeChunkUpdates.values();

    private ChunkUpdatePacing() {
    }

    public static void enable(Options options) {
        PrioritizeChunkUpdates old = options.prioritizeChunkUpdates().get();
        Opts.chunkUpdateMode = old == PrioritizeChunkUpdates.NONE ? -1 : old.ordinal();
        if (old != PrioritizeChunkUpdates.NONE) {
            options.prioritizeChunkUpdates().set(PrioritizeChunkUpdates.NONE);
            options.save();
        }
    }

    public static void disable(Options options) {
        int slot = Opts.chunkUpdateMode;
        if (slot >= 0 && options.prioritizeChunkUpdates().get() == PrioritizeChunkUpdates.NONE) {
            options.prioritizeChunkUpdates().set(MODES[Math.min(slot, MODES.length - 1)]);
            options.save();
        }
        Opts.chunkUpdateMode = -1;
    }

    public static void sync(Options options) {
        if (!Opts.chunkPacing) return;
        enable(options);
    }
}
