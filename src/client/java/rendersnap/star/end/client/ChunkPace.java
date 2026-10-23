package rendersnap.star.end.client;

import net.minecraft.client.Options;
import net.minecraft.client.PrioritizeChunkUpdates;
import rendersnap.star.end.client.cfg.Opts;

public final class ChunkPace {
    private static final PrioritizeChunkUpdates[] MODES = PrioritizeChunkUpdates.values();

    private ChunkPace() {
    }

    public static void enable(Options options) {
        PrioritizeChunkUpdates old = options.prioritizeChunkUpdates().get();

        if (old != PrioritizeChunkUpdates.NONE) {
            Opts.chunkUpdateMode = old.ordinal();
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
