package rendersnap.star.end.client.render;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import rendersnap.star.end.client.cfg.Opts;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.LongAdder;

public final class ShadowReuse {
    private static final int MAX_ENTRIES = 256;
    private static final Map<Integer, Entry> ENTRIES = new HashMap<>();
    private static final LongAdder CHECKS = new LongAdder();
    private static final LongAdder HITS = new LongAdder();
    private static final LongAdder MISSES = new LongAdder();

    private ShadowReuse() {
    }

    public static boolean restore(Entity e, EntityRenderState state, Level level) {
        CHECKS.increment();
        if (!Opts.shadowReuse || e == null || state == null || level == null || state.isInvisible) {
            return false;
        }

        Entry entry = ENTRIES.get(e.getId());
        if (entry == null || !entry.matches(e, state, level)) {
            MISSES.increment();
            return false;
        }

        state.shadowRadius = entry.radius;
        state.shadowPieces.clear();
        state.shadowPieces.addAll(entry.pieces);
        HITS.increment();
        return true;
    }

    public static void capture(Entity e, EntityRenderState state, Level level) {
        if (!Opts.shadowReuse || e == null || state == null || level == null || state.isInvisible) {
            return;
        }

        if (ENTRIES.size() >= MAX_ENTRIES && !ENTRIES.containsKey(e.getId())) {
            ENTRIES.remove(ENTRIES.keySet().iterator().next());
        }
        ENTRIES.put(e.getId(), new Entry(e, state, level));
    }

    public static void clear() {
        ENTRIES.clear();
    }

    public static void appendDebug(StringBuilder out) {
        out.append("shadowChecks=").append(CHECKS.sum()).append('\n');
        out.append("shadowCacheHits=").append(HITS.sum()).append('\n');
        out.append("shadowCacheMisses=").append(MISSES.sum()).append('\n');
        out.append("shadowCacheEntries=").append(ENTRIES.size()).append('\n');
    }

    private record Entry(Level level, long gameTime, double x, double y, double z, double distanceToCameraSq,
                         float radius, List<EntityRenderState.ShadowPiece> pieces) {
        private Entry(Entity e, EntityRenderState state, Level level) {
            this(level, level.getGameTime(), e.getX(), e.getY(), e.getZ(), state.distanceToCameraSq,
                    state.shadowRadius, List.copyOf(state.shadowPieces));
        }

        private boolean matches(Entity e, EntityRenderState state, Level level) {
            return this.level == level
                    && this.gameTime == level.getGameTime()
                    && Double.doubleToLongBits(this.x) == Double.doubleToLongBits(e.getX())
                    && Double.doubleToLongBits(this.y) == Double.doubleToLongBits(e.getY())
                    && Double.doubleToLongBits(this.z) == Double.doubleToLongBits(e.getZ())
                    && Double.doubleToLongBits(this.distanceToCameraSq) == Double.doubleToLongBits(state.distanceToCameraSq);
        }
    }
}
