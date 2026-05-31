package rendersnap.star.end.client;

import rendersnap.star.end.client.cfg.Opts;
import net.minecraft.client.Options;
import net.minecraft.client.PrioritizeChunkUpdates;

public final class ChunkThreads {
    private ChunkThreads() {
    }

    public static void on(Options options) {
        PrioritizeChunkUpdates old = options.prioritizeChunkUpdates().get();
        if (old != PrioritizeChunkUpdates.NONE) {
            Opts.chunkUpdateMode = old.ordinal();
            options.prioritizeChunkUpdates().set(PrioritizeChunkUpdates.NONE);
            options.save();
        }
    }

    public static void off(Options options) {
        PrioritizeChunkUpdates[] modes = PrioritizeChunkUpdates.values();
        int slot = Opts.chunkUpdateMode;
        if (slot >= 0 && slot < modes.length) {
            options.prioritizeChunkUpdates().set(modes[slot]);
            options.save();
        }
        Opts.chunkUpdateMode = -1;
    }

    public static void sync(Options options) {
        if (!Opts.multiRender) return;

        PrioritizeChunkUpdates old = options.prioritizeChunkUpdates().get();
        if (old != PrioritizeChunkUpdates.NONE) {
            options.prioritizeChunkUpdates().set(PrioritizeChunkUpdates.NONE);
        }
    }
}
