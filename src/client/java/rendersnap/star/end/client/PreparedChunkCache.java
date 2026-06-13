package rendersnap.star.end.client;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.world.entity.Entity;
import rendersnap.star.end.client.cfg.Opts;

public final class PreparedChunkCache {
    private static ChunkSectionsToRender cached;
    private static SectionRenderDispatcher.RenderSection[] sections = new SectionRenderDispatcher.RenderSection[0];
    private static double camX;
    private static double camY;
    private static double camZ;
    private static float rotX;
    private static float rotY;
    private static boolean farLayerTrim;
    private static boolean fogOcclusion;
    private static int behindCamMode;
    private static long hits;
    private static long misses;
    private static long stores;
    private static long rejectsDirty;
    private static long rejectsCamera;
    private static long rejectsSections;
    private static long rejectsSettings;

    private PreparedChunkCache() {
    }

    public static ChunkSectionsToRender get(ObjectArrayList<SectionRenderDispatcher.RenderSection> visibleSections) {
        if (cached == null) {
            misses++;
            return null;
        }
        if (Opts.farLayerTrim != farLayerTrim || Opts.fogOcclusion != fogOcclusion || Opts.behindCamMode != behindCamMode) {
            rejectsSettings++;
            misses++;
            return null;
        }

        Entity cam = Minecraft.getInstance().getCameraEntity();
        if (cam == null) {
            rejectsCamera++;
            misses++;
            return null;
        }
        if (cam.getX() != camX || cam.getY() != camY || cam.getZ() != camZ || cam.getXRot() != rotX || cam.getYRot() != rotY) {
            rejectsCamera++;
            misses++;
            return null;
        }

        int size = visibleSections.size();
        if (sections.length != size) {
            rejectsSections++;
            misses++;
            return null;
        }

        for (int i = 0; i < size; i++) {
            SectionRenderDispatcher.RenderSection section = visibleSections.get(i);
            if (section.isDirty()) {
                rejectsDirty++;
                misses++;
                return null;
            }
            if (sections[i] != section) {
                rejectsSections++;
                misses++;
                return null;
            }
        }

        hits++;
        return cached;
    }

    public static void put(ObjectArrayList<SectionRenderDispatcher.RenderSection> visibleSections, ChunkSectionsToRender prepared) {
        if (prepared == null) return;
        Entity cam = Minecraft.getInstance().getCameraEntity();
        if (cam == null) return;

        int size = visibleSections.size();
        SectionRenderDispatcher.RenderSection[] next = new SectionRenderDispatcher.RenderSection[size];
        for (int i = 0; i < size; i++) {
            SectionRenderDispatcher.RenderSection section = visibleSections.get(i);
            if (section.isDirty()) return;
            next[i] = section;
        }

        sections = next;
        cached = prepared;
        camX = cam.getX();
        camY = cam.getY();
        camZ = cam.getZ();
        rotX = cam.getXRot();
        rotY = cam.getYRot();
        farLayerTrim = Opts.farLayerTrim;
        fogOcclusion = Opts.fogOcclusion;
        behindCamMode = Opts.behindCamMode;
        stores++;
    }

    public static void clear() {
        cached = null;
        sections = new SectionRenderDispatcher.RenderSection[0];
    }

    public static void appendDebug(StringBuilder out) {
        out.append("cached=").append(cached != null).append('\n');
        out.append("sections=").append(sections.length).append('\n');
        out.append("hits=").append(hits).append('\n');
        out.append("misses=").append(misses).append('\n');
        out.append("stores=").append(stores).append('\n');
        out.append("rejectsDirty=").append(rejectsDirty).append('\n');
        out.append("rejectsCamera=").append(rejectsCamera).append('\n');
        out.append("rejectsSections=").append(rejectsSections).append('\n');
        out.append("rejectsSettings=").append(rejectsSettings).append('\n');
    }
}
