package rendersnap.star.end.client.render;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import rendersnap.star.end.client.cfg.Opts;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.LongAdder;

public final class ShadReuse {
    private static final int MAX_ENTRIES = 256;
    private static final Map<Integer, Entry> ENTRIES = new LinkedHashMap<>(MAX_ENTRIES, 0.75f, true);
    private static final LongAdder CHECKS = new LongAdder();
    private static final LongAdder HITS = new LongAdder();
    private static final LongAdder MISSES = new LongAdder();
    private static final LongAdder FAST_SKIPS = new LongAdder();

    private ShadReuse() {
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

        Entry entry = ENTRIES.get(e.getId());
        if (entry == null) {
            if (ENTRIES.size() >= MAX_ENTRIES) {
                ENTRIES.remove(ENTRIES.keySet().iterator().next());
            }
            entry = new Entry();
            ENTRIES.put(e.getId(), entry);
        }
        entry.set(e, state, level);
    }

    public static void clear() {
        ENTRIES.clear();
    }

    public static void skippedForFastCamera() {
        FAST_SKIPS.increment();
    }

    public static void appendDebug(StringBuilder out) {
        out.append("shadowChecks=").append(CHECKS.sum()).append('\n');
        out.append("shadowCacheHits=").append(HITS.sum()).append('\n');
        out.append("shadowCacheMisses=").append(MISSES.sum()).append('\n');
        out.append("shadowCacheEntries=").append(ENTRIES.size()).append('\n');
        out.append("shadowFastCameraSkips=").append(FAST_SKIPS.sum()).append('\n');
    }

    private static final class Entry {
        private Level level;
        private long gameTime;
        private double x;
        private double y;
        private double z;
        private double distanceToCameraSq;
        private float radius;
        private final List<EntityRenderState.ShadowPiece> pieces = new ArrayList<>(4);

        private void set(Entity e, EntityRenderState state, Level level) {
            this.level = level;
            this.gameTime = level.getGameTime();
            this.x = e.getX();
            this.y = e.getY();
            this.z = e.getZ();
            this.distanceToCameraSq = state.distanceToCameraSq;
            this.radius = state.shadowRadius;
            this.pieces.clear();
            this.pieces.addAll(state.shadowPieces);
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
