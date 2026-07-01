package rendersnap.star.end.client;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.renderer.DynamicUniforms;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.SectionMesh;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Util;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import rendersnap.star.end.client.render.Cuts;
import rendersnap.star.end.client.cfg.Opts;

import java.util.EnumMap;
import java.util.List;

public final class PreparedChunkCache {
    private static final boolean ENABLED = true;
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
    private static long meshFingerprint;
    private static long cameraStamp;
    private static boolean visibleDirty;
    private static int cachedVisibleSections;
    private static long cachedVisibleFingerprint;
    private static long cachedMeshFingerprint;

    private PreparedChunkCache() {
    }

    public static ChunkSectionsToRender get(
            ObjectArrayList<SectionRenderDispatcher.RenderSection> visibleSections,
            Matrix4fc modelViewMatrix,
            SectionRenderDispatcher dispatcher
    ) {
        if (!ENABLED) {
            misses++;
            return null;
        }
        if (visibleDirty) {
            rejectsDirty++;
            misses++;
            return null;
        }
        EligibleSections eligible = collectEligibleSections(visibleSections, dispatcher);
        if (eligible == null) {
            rejectsSections++;
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
            if (entry.meshFingerprint != meshFingerprint) continue;
            if (entry.cameraStamp != cameraStamp) continue;
            ChunkSectionsToRender rebuilt = entry.cachedPrepared.rebuild(modelViewMatrix, eligible);
            if (rebuilt == null) continue;
            if (i != 0) {
                System.arraycopy(entries, 0, entries, 1, i);
                entries[0] = entry;
            }
            cachedVisibleSections = entry.visibleSections;
            cachedVisibleFingerprint = entry.visibleFingerprint;
            cachedMeshFingerprint = entry.meshFingerprint;
            hits++;
            return rebuilt;
        }
        if (entryCount == 0) {
            cachedVisibleSections = 0;
            cachedVisibleFingerprint = 0L;
            cachedMeshFingerprint = 0L;
        } else if (sawSettingsMatch) {
            rejectsSections++;
        } else {
            rejectsSettings++;
        }
        misses++;
        return null;
    }

    public static void put(
            ObjectArrayList<SectionRenderDispatcher.RenderSection> visibleSections,
            ChunkSectionsToRender prepared,
            SectionRenderDispatcher dispatcher
    ) {
        if (!ENABLED) return;
        if (prepared == null) return;
        if (visibleDirty) return;
        EligibleSections eligible = collectEligibleSections(visibleSections, dispatcher);
        if (eligible == null) return;
        CachedPrepared cachedPrepared = CachedPrepared.capture(eligible, prepared);
        if (cachedPrepared == null) return;
        Entry entry = new Entry(
                cachedPrepared,
                PreparedChunkCache.visibleSections,
                visibleFingerprint,
                meshFingerprint,
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
        cachedMeshFingerprint = entry.meshFingerprint;
        stores++;
    }

    public static void captureVisibleSections(List<SectionRenderDispatcher.RenderSection> sections) {
        long fingerprint = 0x9E3779B97F4A7C15L;
        long mesh = 0xC2B2AE3D27D4EB4FL;
        boolean dirty = false;
        int size = sections.size();
        for (int i = 0; i < size; i++) {
            SectionRenderDispatcher.RenderSection section = sections.get(i);
            long id = mix(sectionKey(section));
            fingerprint = mix(fingerprint ^ Long.rotateLeft(id, i & 31) ^ i);
            mesh = mix(mesh ^ sectionMeshState(section, i));
            dirty |= McCompat.sectionDirty(section) || McCompat.sectionUncompiled(section);
        }
        PreparedChunkCache.visibleSections = size;
        visibleFingerprint = fingerprint;
        meshFingerprint = mesh;
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
        cachedMeshFingerprint = 0L;
    }

    public static void appendDebug(StringBuilder out) {
        out.append("enabled=").append(ENABLED).append('\n');
        out.append("cached=").append(entryCount > 0).append('\n');
        out.append("entryCount=").append(entryCount).append('\n');
        out.append("maxEntries=").append(MAX_ENTRIES).append('\n');
        out.append("visibleSections=").append(visibleSections).append('\n');
        out.append("visibleDirty=").append(visibleDirty).append('\n');
        out.append("visibleFingerprint=").append(Long.toUnsignedString(visibleFingerprint, 16)).append('\n');
        out.append("meshFingerprint=").append(Long.toUnsignedString(meshFingerprint, 16)).append('\n');
        out.append("cameraStamp=").append(Long.toUnsignedString(cameraStamp, 16)).append('\n');
        out.append("cachedVisibleSections=").append(cachedVisibleSections).append('\n');
        out.append("cachedVisibleFingerprint=").append(Long.toUnsignedString(cachedVisibleFingerprint, 16)).append('\n');
        out.append("cachedMeshFingerprint=").append(Long.toUnsignedString(cachedMeshFingerprint, 16)).append('\n');
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
            if (entry.meshFingerprint != candidate.meshFingerprint) continue;
            if (entry.cameraStamp != candidate.cameraStamp) continue;
            if (entry.farLayerTrim != candidate.farLayerTrim) continue;
            if (entry.fogOcclusion != candidate.fogOcclusion) continue;
            if (entry.behindCamMode != candidate.behindCamMode) continue;
            return i;
        }
        return -1;
    }

    private static long sectionKey(SectionRenderDispatcher.RenderSection section) {
        BlockPos origin = section.getRenderOrigin();
        return ((long) origin.getX() & 0x3FFFFFFL) << 38
                | ((long) origin.getZ() & 0x3FFFFFFL) << 12
                | ((long) origin.getY() & 0xFFFL);
    }

    private static long sectionMeshState(SectionRenderDispatcher.RenderSection section, int order) {
        SectionMesh mesh = section.getSectionMesh();
        long state = mix(System.identityHashCode(mesh) & 0xffffffffL);
        for (ChunkSectionLayer layer : ChunkSectionLayer.values()) {
            SectionMesh.SectionDraw draw = mesh.getSectionDraw(layer);
            long layerState = draw == null
                    ? mix(layer.ordinal() + 1L)
                    : ((long) draw.indexCount() << 8)
                    ^ (draw.hasCustomIndexBuffer() ? 0x9E37L : 0L)
                    ^ layer.ordinal();
            state = mix(state ^ Long.rotateLeft(layerState, layer.ordinal() * 7));
        }
        return state ^ Long.rotateLeft(order, 19);
    }

    private static EligibleSections collectEligibleSections(
            ObjectArrayList<SectionRenderDispatcher.RenderSection> visibleSections,
            SectionRenderDispatcher dispatcher
    ) {
        if (dispatcher == null) return null;
        dispatcher.lock();
        try {
            long[] keys = new long[visibleSections.size()];
            SectionRenderDispatcher.RenderSection[] sections = new SectionRenderDispatcher.RenderSection[visibleSections.size()];
            int count = 0;
            for (int i = 0; i < visibleSections.size(); i++) {
                SectionRenderDispatcher.RenderSection section = visibleSections.get(i);
                SectionMesh mesh = section.getSectionMesh();
                BlockPos origin = section.getRenderOrigin();
                boolean eligible = false;
                for (ChunkSectionLayer layer : ChunkSectionLayer.values()) {
                    if (Cuts.trimsFarLayers() && Cuts.shouldSkipSectionLayer(origin, layer)) continue;
                    SectionMesh.SectionDraw draw = mesh.getSectionDraw(layer);
                    if (draw == null) continue;
                    SectionRenderDispatcher.RenderSectionBufferSlice slice = dispatcher.getRenderSectionSlice(mesh, layer);
                    if (slice == null) continue;
                    if (draw.hasCustomIndexBuffer() && slice.indexBuffer() == null) continue;
                    eligible = true;
                    break;
                }
                if (!eligible) continue;
                keys[count] = sectionKey(section);
                sections[count] = section;
                count++;
            }
            return new EligibleSections(keys, sections, count);
        } finally {
            dispatcher.unlock();
        }
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
            CachedPrepared cachedPrepared,
            int visibleSections,
            long visibleFingerprint,
            long meshFingerprint,
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

    private record CachedPrepared(
            GpuTextureView textureView,
            EnumMap<ChunkSectionLayer, Int2ObjectOpenHashMap<List<RenderPass.Draw<GpuBufferSlice[]>>>> drawGroupsPerLayer,
            int maxIndicesRequired,
            long[] renderOrigins,
            int textureAtlasWidth,
            int textureAtlasHeight
    ) {
        private static CachedPrepared capture(EligibleSections eligible, ChunkSectionsToRender prepared) {
            int expected = prepared.chunkSectionInfos().length;
            long[] renderOrigins = new long[expected];
            if (eligible.count != expected) return null;
            System.arraycopy(eligible.keys, 0, renderOrigins, 0, expected);
            GpuTextureView textureView = prepared.textureView();
            return new CachedPrepared(
                    textureView,
                    prepared.drawGroupsPerLayer(),
                    prepared.maxIndicesRequired(),
                    renderOrigins,
                    textureView.getWidth(0),
                    textureView.getHeight(0)
            );
        }

        private ChunkSectionsToRender rebuild(Matrix4fc modelViewMatrix, EligibleSections eligible) {
            if (eligible.count != this.renderOrigins.length) return null;
            DynamicUniforms.ChunkSectionInfo[] infos = new DynamicUniforms.ChunkSectionInfo[this.renderOrigins.length];
            long now = Util.getMillis();
            for (int index = 0; index < eligible.count; index++) {
                if (this.renderOrigins[index] != eligible.keys[index]) return null;
                SectionRenderDispatcher.RenderSection section = eligible.sections[index];
                BlockPos origin = section.getRenderOrigin();
                infos[index] = new DynamicUniforms.ChunkSectionInfo(
                        new Matrix4f(modelViewMatrix),
                        origin.getX(),
                        origin.getY(),
                        origin.getZ(),
                        section.getVisibility(now),
                        this.textureAtlasWidth,
                        this.textureAtlasHeight
                );
            }
            GpuBufferSlice[] chunkSectionInfos = RenderSystem.getDynamicUniforms().writeChunkSections(infos);
            return new ChunkSectionsToRender(this.textureView, this.drawGroupsPerLayer, this.maxIndicesRequired, chunkSectionInfos);
        }
    }

    private record EligibleSections(
            long[] keys,
            SectionRenderDispatcher.RenderSection[] sections,
            int count
    ) {
    }
}
