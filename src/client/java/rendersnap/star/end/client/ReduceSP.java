package rendersnap.star.end.client;

import rendersnap.star.end.client.cfg.Opts;

import java.util.Deque;
import java.util.concurrent.atomic.LongAdder;

public final class ReduceSP {

    private static final long REBUILD_BUDGET_NS = 2_000_000L;

    private static final long LIGHT_BUDGET_NS = 3_000_000L;

    private static final int VANILLA_LIGHT_FLOOD = 1000;

    private static final LongAdder SYNC_REBUILDS = new LongAdder();
    private static final LongAdder DEFERRED_REBUILDS = new LongAdder();
    private static final LongAdder LIGHT_FLOODS = new LongAdder();
    private static final LongAdder LIGHT_SPREAD = new LongAdder();

    private static long rebuildSpentNs;
    private static int rebuildsThisFrame;

    private ReduceSP() {
    }

    public static void beginRebuildFrame() {
        rebuildSpentNs = 0L;
        rebuildsThisFrame = 0;
    }

    public static boolean allowSyncRebuild() {
        if (!Opts.rebuildGuard) return true;
        if (rebuildsThisFrame == 0 || rebuildSpentNs < REBUILD_BUDGET_NS) return true;
        DEFERRED_REBUILDS.increment();
        return false;
    }

    public static void syncRebuildDone(long nanos) {
        rebuildsThisFrame++;
        rebuildSpentNs += Math.max(0L, nanos);
        SYNC_REBUILDS.increment();
    }

    public static boolean pollLight(Deque<Runnable> queue) {
        if (!Opts.lightPacing || queue == null) return false;
        int size = queue.size();
        if (size < VANILLA_LIGHT_FLOOD) return false;

        LIGHT_FLOODS.increment();

        int quota = Math.max(10, size / 10);
        long start = System.nanoTime();
        int done = 0;
        while (done < size) {
            if (done >= quota && System.nanoTime() - start >= LIGHT_BUDGET_NS) break;
            Runnable update = queue.poll();
            if (update == null) break;
            update.run();
            done++;
        }
        LIGHT_SPREAD.add(Math.max(0, size - done));
        return true;
    }

    public static void appendDebug(StringBuilder out) {
        out.append("rebuildGuard.syncRebuilds=").append(SYNC_REBUILDS.sum()).append('\n');
        out.append("rebuildGuard.movedToWorkers=").append(DEFERRED_REBUILDS.sum()).append('\n');
        out.append("lightPacing.floods=").append(LIGHT_FLOODS.sum()).append('\n');
        out.append("lightPacing.updatesSpreadToLaterFrames=").append(LIGHT_SPREAD.sum()).append('\n');
    }
}
