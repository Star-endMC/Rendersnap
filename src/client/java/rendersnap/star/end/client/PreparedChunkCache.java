package rendersnap.star.end.client;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import rendersnap.star.end.client.render.Cuts;
import rendersnap.star.end.client.cfg.Opts;

import java.util.List;

public final class PreparedChunkCache {
    private static final int MAX_ENTRIES = 8;
    private static final Entry[] entries = new Entry[MAX_ENTRIES];
    private static int entryCount;
    private static long hits;
    private static long misses;
    private static long stores;
    private static long rejectsDirty;
    private static long rejectsSections;
    private static long rejectsSettings;
    private static long captures;
    private static int visibleSections;
    private static long visibleFingerprint;
    private static long cameraStamp;
    private static boolean visibleDirty;
    private static int cachedVisibleSections;
    private static long cachedVisibleFingerprint;

    private PreparedChunkCache() {
    }

    public static ChunkSectionsToRender get(ObjectArrayList<SectionRenderDispatcher.RenderSection> visibleSections) {
        if (visibleDirty) {
            rejectsDirty++;
            misses++;
            return null;
        }
        boolean sawSettingsMatch = false;
        for (int i = 0; i < entryCount; i++) {
            Entry entry = entries[i];
            if (entry == null) continue;
            if (!entry.matchesSettings()) continue;
            sawSettingsMatch = true;
            if (entry.visibleSections != PreparedChunkCache.visibleSections) continue;
            if (entry.visibleFingerprint != visibleFingerprint) continue;
            if (entry.cameraStamp != cameraStamp) continue;
            if (i != 0) {
                System.arraycopy(entries, 0, entries, 1, i);
                entries[0] = entry;
            }
            cachedVisibleSections = entry.visibleSections;
            cachedVisibleFingerprint = entry.visibleFingerprint;
            hits++;
            return entry.prepared;
        }
        if (entryCount == 0) {
            cachedVisibleSections = 0;
            cachedVisibleFingerprint = 0L;
        } else if (sawSettingsMatch) {
            rejectsSections++;
        } else {
            rejectsSettings++;
        }
        misses++;
        return null;
    }

    public static void put(ObjectArrayList<SectionRenderDispatcher.RenderSection> visibleSections, ChunkSectionsToRender prepared) {
        if (prepared == null) return;
        if (visibleDirty) return;
        Entry entry = new Entry(
                prepared,
                PreparedChunkCache.visibleSections,
                visibleFingerprint,
                cameraStamp,
                Opts.farLayerTrim,
                Opts.fogOcclusion,
                Opts.behindCamMode
        );
        int existing = find(entry);
        if (existing >= 0) {
            if (existing > 0) {
                System.arraycopy(entries, 0, entries, 1, existing);
            }
        } else {
            int moved = Math.min(entryCount, MAX_ENTRIES - 1);
            if (moved > 0) {
                System.arraycopy(entries, 0, entries, 1, moved);
            }
            if (entryCount < MAX_ENTRIES) {
                entryCount++;
            }
        }
        entries[0] = entry;
        cachedVisibleSections = entry.visibleSections;
        cachedVisibleFingerprint = entry.visibleFingerprint;
        stores++;
    }

    public static void captureVisibleSections(List<SectionRenderDispatcher.RenderSection> sections) {
        long xor = 0L;
        long sum = 0x9E3779B97F4A7C15L;
        boolean dirty = false;
        int size = sections.size();
        for (int i = 0; i < size; i++) {
            SectionRenderDispatcher.RenderSection section = sections.get(i);
            long id = mix(System.identityHashCode(section) & 0xffffffffL);
            xor ^= id;
            sum += id;
            dirty |= McCompat.sectionDirty(section);
        }
        PreparedChunkCache.visibleSections = size;
        visibleFingerprint = mix(xor) ^ Long.rotateLeft(sum, 17);
        cameraStamp = Cuts.preparedCameraStamp();
        visibleDirty = dirty;
        captures++;
    }

    public static void clear() {
        for (int i = 0; i < entryCount; i++) {
            entries[i] = null;
        }
        entryCount = 0;
        cachedVisibleSections = 0;
        cachedVisibleFingerprint = 0L;
    }

    public static void appendDebug(StringBuilder out) {
        out.append("cached=").append(entryCount > 0).append('\n');
        out.append("entryCount=").append(entryCount).append('\n');
        out.append("maxEntries=").append(MAX_ENTRIES).append('\n');
        out.append("visibleSections=").append(visibleSections).append('\n');
        out.append("visibleDirty=").append(visibleDirty).append('\n');
        out.append("visibleFingerprint=").append(Long.toUnsignedString(visibleFingerprint, 16)).append('\n');
        out.append("cameraStamp=").append(Long.toUnsignedString(cameraStamp, 16)).append('\n');
        out.append("cachedVisibleSections=").append(cachedVisibleSections).append('\n');
        out.append("cachedVisibleFingerprint=").append(Long.toUnsignedString(cachedVisibleFingerprint, 16)).append('\n');
        out.append("captures=").append(captures).append('\n');
        out.append("hits=").append(hits).append('\n');
        out.append("misses=").append(misses).append('\n');
        out.append("stores=").append(stores).append('\n');
        out.append("rejectsDirty=").append(rejectsDirty).append('\n');
        out.append("rejectsSections=").append(rejectsSections).append('\n');
        out.append("rejectsSettings=").append(rejectsSettings).append('\n');
    }

    private static int find(Entry candidate) {
        for (int i = 0; i < entryCount; i++) {
            Entry entry = entries[i];
            if (entry == null) continue;
            if (entry.visibleSections != candidate.visibleSections) continue;
            if (entry.visibleFingerprint != candidate.visibleFingerprint) continue;
            if (entry.cameraStamp != candidate.cameraStamp) continue;
            if (entry.farLayerTrim != candidate.farLayerTrim) continue;
            if (entry.fogOcclusion != candidate.fogOcclusion) continue;
            if (entry.behindCamMode != candidate.behindCamMode) continue;
            return i;
        }
        return -1;
    }

    private static long mix(long value) {
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdL;
        value ^= value >>> 33;
        value *= 0xc4ceb9fe1a85ec53L;
        value ^= value >>> 33;
        return value;
    }

    private record Entry(
            ChunkSectionsToRender prepared,
            int visibleSections,
            long visibleFingerprint,
            long cameraStamp,
            boolean farLayerTrim,
            boolean fogOcclusion,
            int behindCamMode
    ) {
        private boolean matchesSettings() {
            return Opts.farLayerTrim == farLayerTrim
                    && Opts.fogOcclusion == fogOcclusion
                    && Opts.behindCamMode == behindCamMode;
        }
    }
}
