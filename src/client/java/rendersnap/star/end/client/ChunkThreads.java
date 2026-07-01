package rendersnap.star.end.client;

import rendersnap.star.end.client.cfg.Opts;
import net.minecraft.client.Options;
import net.minecraft.client.PrioritizeChunkUpdates;

public final class ChunkThreads {
    private static final PrioritizeChunkUpdates[] MODES = PrioritizeChunkUpdates.values();

    private ChunkThreads() {
    }

    public static void on(Options options) {
        PrioritizeChunkUpdates old = options.prioritizeChunkUpdates().get();
        Opts.chunkUpdateMode = old == PrioritizeChunkUpdates.NONE ? -1 : old.ordinal();
        if (old != PrioritizeChunkUpdates.NONE) {
            options.prioritizeChunkUpdates().set(PrioritizeChunkUpdates.NONE);
            options.save();
        }
    }

    public static void off(Options options) {
        int slot = Opts.chunkUpdateMode;
        if (slot >= 0 && options.prioritizeChunkUpdates().get() == PrioritizeChunkUpdates.NONE) {
            options.prioritizeChunkUpdates().set(MODES[Math.min(slot, MODES.length - 1)]);
            options.save();
        }
        Opts.chunkUpdateMode = -1;
    }

    public static void sync(Options options) {
        if (!Opts.multiRender) return;

        PrioritizeChunkUpdates old = options.prioritizeChunkUpdates().get();
        if (old != PrioritizeChunkUpdates.NONE) {
            if (Opts.chunkUpdateMode < 0) {
                Opts.chunkUpdateMode = old.ordinal();
                Opts.save();
            }
            options.prioritizeChunkUpdates().set(PrioritizeChunkUpdates.NONE);
            options.save();
        }
    }
}
