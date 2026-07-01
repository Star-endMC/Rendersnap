package rendersnap.star.end.client;

import com.mojang.blaze3d.platform.InputConstants;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SectionOcclusionGraph;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.chunk.SectionMesh;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.HitResult;
import org.joml.Matrix4f;
import rendersnap.star.end.Rendersnap;
import rendersnap.star.end.client.cfg.Opts;
import rendersnap.star.end.client.render.Cuts;
import rendersnap.star.end.client.render.gpu.Gpu;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class RenderReport {
    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss_SSS").withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter TEXT_TIME = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneId.systemDefault());
    private static final SystemToast.SystemToastId TOAST = new SystemToast.SystemToastId();
    private static int[] lastVisibleLayerSections = new int[ChunkSectionLayer.values().length];
    private static long[] lastVisibleLayerIndices = new long[ChunkSectionLayer.values().length];
    private static KeyMapping key;

    private RenderReport() {
    }

    public static void registerKeybind() {
        if (key != null) return;
        KeyMapping.Category category = new KeyMapping.Category(Identifier.fromNamespaceAndPath(Rendersnap.MOD_ID, "controls"));
        key = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.rendersnap.render_report",
                InputConstants.Type.KEYSYM,
                InputConstants.KEY_F8,
                category
        ));
    }

    public static void onClientTick(Minecraft client) {
        if (client == null || key == null) return;
        while (key.consumeClick()) {
            generate(client);
        }
    }

    private static void generate(Minecraft mc) {
        try {
            Path path = write(mc);
            Rendersnap.LOGGER.info("Rendersnap render report written to {}", path);
            SystemToast.addOrUpdate(
                    McCompat.toastManager(mc),
                    TOAST,
                    net.minecraft.network.chat.Component.literal("Rendersnap Render Report"),
                    net.minecraft.network.chat.Component.literal(path.getFileName().toString())
            );
        } catch (Throwable t) {
            Rendersnap.LOGGER.warn("Failed to write render report", t);
            SystemToast.addOrUpdate(
                    McCompat.toastManager(mc),
                    TOAST,
                    net.minecraft.network.chat.Component.literal("Rendersnap Render Report Failed"),
                    net.minecraft.network.chat.Component.literal(t.getClass().getSimpleName() + ": " + String.valueOf(t.getMessage()))
            );
        }
    }

    private static Path write(Minecraft mc) throws IOException {
        Path dir = mc.gameDirectory.toPath().resolve("rendersnap-reports");
        Files.createDirectories(dir);
        Path path = dir.resolve("render-report-" + FILE_TIME.format(Instant.now()) + ".txt");
        StringBuilder out = new StringBuilder(1 << 20);

        appendHeader(out, mc);
        appendOptions(out, mc);
        appendRendersnap(out);
        appendWorld(out, mc);
        appendRenderer(out, mc);
        appendHookPressure(out);
        appendVisibleSections(out, mc);
        appendPreparedChunks(out, mc);

        Files.writeString(path, out.toString(), StandardCharsets.UTF_8);
        return path;
    }

    private static void appendHeader(StringBuilder out, Minecraft mc) {
        section(out, "Header");
        line(out, "timestamp", TEXT_TIME.format(Instant.now()));
        line(out, "launchedVersion", mc.getLaunchedVersion());
        line(out, "versionType", McCompat.versionType(mc));
        line(out, "gameDirectory", mc.gameDirectory.getAbsolutePath());
        line(out, "isLocalServer", mc.isLocalServer());
        line(out, "isSingleplayer", McCompat.isSingleplayer(mc));
        line(out, "windowActive", mc.isWindowActive());
        line(out, "paused", mc.isPaused());
        line(out, "fps", mc.getFps());
        line(out, "frameTimeNs", mc.getFrameTimeNs());
        line(out, "gpuUtilization", mc.getGpuUtilization());
        line(out, "wireframe", mc.wireframe);
        line(out, "smartCull", mc.smartCull);
        line(out, "shaderTransparency", safeOption(() -> mc.options.improvedTransparency().get()));
        line(out, "gpu.vendor", Gpu.vendor());
        line(out, "gpu.renderer", Gpu.renderer());
        line(out, "gpu.version", Gpu.version());
        line(out, "window.size", mc.getWindow().getWidth() + "x" + mc.getWindow().getHeight());
        line(out, "window.framebuffer", mc.getWindow().getWidth() + "x" + mc.getWindow().getHeight());
        line(out, "mods.total", FabricLoader.getInstance().getAllMods().size());
        line(out, "mods.renderRelated", renderMods());
    }

    private static void appendOptions(StringBuilder out, Minecraft mc) {
        section(out, "Options");
        int framerateLimit = mc.options.framerateLimit().get();
        int unlimitedCutoff = unlimitedFramerateCutoff(mc);
        line(out, "renderDistance", mc.options.renderDistance().get());
        line(out, "simulationDistance", mc.options.simulationDistance().get());
        line(out, "prioritizeChunkUpdates", mc.options.prioritizeChunkUpdates().get());
        line(out, "graphicsPreset", safeOption(() -> mc.options.graphicsPreset().get()));
        line(out, "ambientOcclusion", mc.options.ambientOcclusion().get());
        line(out, "cloudStatus", mc.options.cloudStatus().get());
        line(out, "cloudRange", safeOption(() -> mc.options.cloudRange().get()));
        line(out, "particles", mc.options.particles().get());
        line(out, "entityShadows", mc.options.entityShadows().get());
        line(out, "entityDistanceScaling", mc.options.entityDistanceScaling().get());
        line(out, "improvedTransparency", safeOption(() -> mc.options.improvedTransparency().get()));
        line(out, "weatherRadius", safeOption(() -> mc.options.weatherRadius().get()));
        line(out, "mipmapLevels", mc.options.mipmapLevels().get());
        line(out, "textureFiltering", mc.options.textureFiltering().get());
        line(out, "maxAnisotropyBit", mc.options.maxAnisotropyBit().get());
        line(out, "framerateLimit", framerateLimit);
        line(out, "framerateLimitRaw", framerateLimit);
        line(out, "framerateLimitUnlimitedCutoff", unlimitedCutoff < 0 ? "<missing>" : unlimitedCutoff);
        line(out, "framerateLimitMode", describeFramerateLimit(framerateLimit, unlimitedCutoff));
        line(out, "inactivityFpsLimit", safeOption(() -> mc.options.inactivityFpsLimit().get()));
        line(out, "vsync", mc.options.enableVsync().get());
        line(out, "fullscreen", mc.options.fullscreen().get());
        line(out, "guiScale", mc.options.guiScale().get());
        line(out, "fov", mc.options.fov().get());
    }

    private static void appendRendersnap(StringBuilder out) {
        section(out, "Rendersnap");
        line(out, "showFpsOverlay", Opts.showFpsOverlay);
        line(out, "zoomTransition", Opts.zoomTransition);
        line(out, "fastLaunch", Opts.fastLaunch);
        line(out, "multiRender", Opts.multiRender);
        line(out, "chunkUpdateMode", Opts.chunkUpdateMode);
        line(out, "blockFaceCulling", Opts.blockFaceCulling);
        line(out, "textureLod", Opts.textureLod);
        line(out, "chunkShadeTrim", Opts.chunkShadeTrim);
        line(out, "farLayerTrim", Opts.farLayerTrim);
        line(out, "cutoutLeafBoost", Opts.cutoutLeafBoost);
        line(out, "entityCulling", Opts.entityCulling);
        line(out, "occlusionCulling", Opts.occlusionCulling);
        line(out, "fogOcclusion", Opts.fogOcclusion);
        line(out, "behindCamMode", Opts.behindCamMode);
        line(out, "lightingChunkTrim", Opts.lightingChunkTrim);
        line(out, "hideWeather", Opts.hideWeather);
        line(out, "fluidOptimizer", Opts.fluidOptimizer);
        out.append('\n').append("[cuts]\n");
        Cuts.appendDebug(out);
        out.append('\n').append("[chunkBudget]\n");
        ChunkBudget.appendDebug(out);
        out.append('\n').append("[preparedChunkCache]\n");
        PreparedChunkCache.appendDebug(out);
    }

    private static void appendWorld(StringBuilder out, Minecraft mc) {
        section(out, "World");
        ClientLevel level = mc.level;
        LocalPlayer player = mc.player;
        if (level == null) {
            out.append("level=null\n");
            return;
        }

        line(out, "dimension", String.valueOf(level.dimension()));
        line(out, "gameTime", level.getGameTime());
        line(out, "dayTime", readField(level.getLevelData(), "dayTime"));
        line(out, "skyDarken", level.getSkyDarken());
        line(out, "seaLevel", level.getSeaLevel());
        line(out, "entities", level.getEntityCount());
        line(out, "players", level.players().size());
        line(out, "globalBlockEntities", level.getGloballyRenderedBlockEntities().size());
        line(out, "chunkStats", level.gatherChunkSourceStats());
        line(out, "serverSimulationDistance", level.getServerSimulationDistance());
        line(out, "cloudColor", String.valueOf(McCompat.cloudRenderer(mc.levelRenderer)));

        if (player != null) {
            line(out, "player.pos", formatVec(player.getX(), player.getY(), player.getZ()));
            line(out, "player.rot", player.getXRot() + ", " + player.getYRot());
            line(out, "player.chunk", new ChunkPos(player.blockPosition().getX() >> 4, player.blockPosition().getZ() >> 4).toString());
        }

        if (mc.getCameraEntity() != null) {
            line(out, "cameraEntity", mc.getCameraEntity().getType().toString());
            line(out, "cameraEntity.pos", formatVec(mc.getCameraEntity().getX(), mc.getCameraEntity().getY(), mc.getCameraEntity().getZ()));
        }

        HitResult hit = mc.hitResult;
        line(out, "hitResult", hit == null ? "null" : hit.getType() + " @ " + String.valueOf(hit.getLocation()));
    }

    private static void appendRenderer(StringBuilder out, Minecraft mc) {
        section(out, "LevelRenderer");
        LevelRenderer renderer = mc.levelRenderer;
        SectionRenderDispatcher dispatcher = McCompat.sectionDispatcher(renderer);
        line(out, "sectionStatistics", String.valueOf(McCompat.invokeValue(renderer, "getSectionStatistics")));
        line(out, "entityStatistics", String.valueOf(McCompat.invokeValue(renderer, "getEntityStatistics")));
        line(out, "renderedSections", String.valueOf(McCompat.invokeValue(renderer, "countRenderedSections")));
        line(out, "totalSections", String.valueOf(McCompat.invokeValue(renderer, "getTotalSections")));
        line(out, "lastViewDistance", String.valueOf(McCompat.invokeValue(renderer, "getLastViewDistance")));
        line(out, "allSectionsRendered", renderer.hasRenderedAllSections());
        line(out, "visibleSections", McCompat.visibleSections(renderer).size());
        line(out, "sectionDispatcher.stats", dispatcher == null ? "<missing>" : dispatcher.getStats());
        line(out, "sectionDispatcher.compileQueue", dispatcher == null ? "<missing>" : dispatcher.getCompileQueueSize());
        line(out, "sectionDispatcher.freeBuffers", dispatcher == null ? "<missing>" : dispatcher.getFreeBufferCount());
        line(out, "translucentTarget", McCompat.invokeValue(renderer, "getTranslucentTarget", "translucentTarget") != null);
        line(out, "itemEntityTarget", McCompat.invokeValue(renderer, "getItemEntityTarget", "itemEntityTarget") != null);
        line(out, "particlesTarget", McCompat.invokeValue(renderer, "getParticlesTarget", "particlesTarget") != null);
        line(out, "weatherTarget", McCompat.invokeValue(renderer, "getWeatherTarget", "weatherTarget") != null);
        line(out, "cloudsTarget", McCompat.invokeValue(renderer, "getCloudsTarget", "cloudsTarget") != null);
        line(out, "lastCameraSection", readField(renderer, "lastCameraSectionX") + ", " + readField(renderer, "lastCameraSectionY") + ", " + readField(renderer, "lastCameraSectionZ"));
        line(out, "prevCameraPos", readField(renderer, "prevCamX") + ", " + readField(renderer, "prevCamY") + ", " + readField(renderer, "prevCamZ"));
        line(out, "prevCameraRot", readField(renderer, "prevCamRotX") + ", " + readField(renderer, "prevCamRotY"));
        SectionOcclusionGraph graph = McCompat.sectionOcclusionGraph(renderer);
        line(out, "occlusionGraph", graph == null ? "<missing>" : graph.getClass().getName());
        line(out, "occlusionOctree", graph == null ? "<missing>" : String.valueOf(graph.getOctree()));
    }

    private static void appendVisibleSections(StringBuilder out, Minecraft mc) {
        section(out, "VisibleSections");
        LevelRenderer renderer = mc.levelRenderer;
        ObjectArrayList<SectionRenderDispatcher.RenderSection> sections = McCompat.visibleSections(renderer);
        if (sections.isEmpty()) {
            out.append("visibleSections=0\n");
            return;
        }

        double camX = mc.getCameraEntity() != null ? mc.getCameraEntity().getX() : 0.0;
        double camY = mc.getCameraEntity() != null ? mc.getCameraEntity().getY() : 0.0;
        double camZ = mc.getCameraEntity() != null ? mc.getCameraEntity().getZ() : 0.0;
        long now = System.currentTimeMillis();
        int dirty = 0;
        int dirtyPlayer = 0;
        int missingNeighbors = 0;
        int translucent = 0;
        int renderable = 0;
        int blockEntitySections = 0;
        int totalBlockEntities = 0;
        int nodeMissing = 0;
        int maxNodeStep = 0;
        double nearest = Double.MAX_VALUE;
        double farthest = 0.0;
        double sumDistance = 0.0;
        int[] bins = new int[8];
        int[] layerSections = new int[ChunkSectionLayer.values().length];
        long[] layerIndices = new long[ChunkSectionLayer.values().length];
        int[] layerCustomIndex = new int[ChunkSectionLayer.values().length];
        @SuppressWarnings("unchecked")
        ArrayList<SectionPressure>[] topSections = new ArrayList[ChunkSectionLayer.values().length];
        for (int i = 0; i < topSections.length; i++) topSections[i] = new ArrayList<>();
        List<String> anomalies = new ArrayList<>();
        SectionOcclusionGraph graph = McCompat.sectionOcclusionGraph(renderer);

        for (int i = 0; i < sections.size(); i++) {
            SectionRenderDispatcher.RenderSection section = sections.get(i);
            BlockPos origin = section.getRenderOrigin();
            double dx = origin.getX() + 8.0 - camX;
            double dy = origin.getY() + 8.0 - camY;
            double dz = origin.getZ() + 8.0 - camZ;
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            nearest = Math.min(nearest, dist);
            farthest = Math.max(farthest, dist);
            sumDistance += dist;
            bins[Math.min(bins.length - 1, (int)(dist / 32.0))]++;

            boolean dirtySection = McCompat.sectionDirty(section);
            boolean dirtyPlayerSection = McCompat.sectionDirtyFromPlayer(section);
            boolean hasAllNeighbors = McCompat.sectionHasAllNeighbors(section);
            if (dirtySection) dirty++;
            if (dirtyPlayerSection) dirtyPlayer++;
            if (!hasAllNeighbors) missingNeighbors++;
            if (section.hasTranslucentGeometry()) translucent++;

            SectionMesh mesh = section.getSectionMesh();
            if (mesh.hasRenderableLayers()) renderable++;
            int blockEntities = mesh.getRenderableBlockEntities().size();
            totalBlockEntities += blockEntities;
            if (blockEntities > 0) blockEntitySections++;

            SectionOcclusionGraph.Node node = graph == null ? null : graph.getNode(section);
            if (node == null) {
                nodeMissing++;
            } else {
                maxNodeStep = Math.max(maxNodeStep, node.step);
            }

            for (ChunkSectionLayer layer : ChunkSectionLayer.values()) {
                SectionMesh.SectionDraw draw = mesh.getSectionDraw(layer);
                if (draw == null) continue;
                int idx = layer.ordinal();
                layerSections[idx]++;
                layerIndices[idx] += draw.indexCount();
                if (draw.hasCustomIndexBuffer()) layerCustomIndex[idx]++;
                pushTopSection(topSections[idx], new SectionPressure(layer, origin, dist, draw.indexCount(), draw.hasCustomIndexBuffer()));
            }

            if (anomalies.size() < 80 && (dirtySection || !hasAllNeighbors || blockEntities > 0 || section.hasTranslucentGeometry())) {
                anomalies.add(
                        origin.getX() + "," + origin.getY() + "," + origin.getZ()
                                + " dist=" + fmt(dist)
                                + " dirty=" + dirtySection
                                + " dirtyPlayer=" + dirtyPlayerSection
                                + " neighbors=" + hasAllNeighbors
                                + " translucent=" + section.hasTranslucentGeometry()
                                + " blockEntities=" + blockEntities
                                + " visibility=" + fmt(section.getVisibility(now))
                                + " nodeStep=" + (node == null ? "null" : node.step)
                );
            }
        }

        line(out, "total", sections.size());
        line(out, "dirty", dirty);
        line(out, "dirtyFromPlayer", dirtyPlayer);
        line(out, "missingNeighbors", missingNeighbors);
        line(out, "translucent", translucent);
        line(out, "renderableLayers", renderable);
        line(out, "blockEntitySections", blockEntitySections);
        line(out, "totalBlockEntities", totalBlockEntities);
        line(out, "occlusionNodeMissing", nodeMissing);
        line(out, "occlusionNodeMaxStep", maxNodeStep);
        line(out, "distance.nearest", fmt(nearest));
        line(out, "distance.average", fmt(sumDistance / sections.size()));
        line(out, "distance.farthest", fmt(farthest));
        line(out, "distance.bins.0_32", bins[0]);
        line(out, "distance.bins.32_64", bins[1]);
        line(out, "distance.bins.64_96", bins[2]);
        line(out, "distance.bins.96_128", bins[3]);
        line(out, "distance.bins.128_160", bins[4]);
        line(out, "distance.bins.160_192", bins[5]);
        line(out, "distance.bins.192_224", bins[6]);
        line(out, "distance.bins.224_plus", bins[7]);
        lastVisibleLayerSections = layerSections.clone();
        lastVisibleLayerIndices = layerIndices.clone();

        for (ChunkSectionLayer layer : ChunkSectionLayer.values()) {
            int idx = layer.ordinal();
            line(out, "layer." + layer + ".sections", layerSections[idx]);
            line(out, "layer." + layer + ".indices", layerIndices[idx]);
            line(out, "layer." + layer + ".customIndex", layerCustomIndex[idx]);
            line(out, "layer." + layer + ".avgIndicesPerDraw", ratio(layerIndices[idx], layerSections[idx]));
        }

        out.append('\n').append("[visibleSectionSample]\n");
        for (String anomaly : anomalies) {
            out.append(anomaly).append('\n');
        }

        out.append('\n').append("[heaviestSectionDraws]\n");
        for (ChunkSectionLayer layer : ChunkSectionLayer.values()) {
            ArrayList<SectionPressure> top = topSections[layer.ordinal()];
            if (top.isEmpty()) continue;
            out.append("layer=").append(layer).append('\n');
            for (int i = 0; i < top.size(); i++) {
                SectionPressure section = top.get(i);
                out.append(i + 1)
                        .append(". ")
                        .append(section.origin.getX()).append(',').append(section.origin.getY()).append(',').append(section.origin.getZ())
                        .append(" indices=").append(section.indices)
                        .append(" dist=").append(fmt(section.distance))
                        .append(" customIndex=").append(section.customIndex)
                        .append('\n');
            }
        }
    }

    private static void appendPreparedChunks(StringBuilder out, Minecraft mc) {
        section(out, "PreparedChunkBatches");
        try {
            Matrix4f matrix = new Matrix4f();
            long cacheHitsBefore = PreparedChunkCacheHits();
            long cacheMissesBefore = PreparedChunkCacheMisses();

            long warmStarted = System.nanoTime();
            ChunkSectionsToRender warm = mc.levelRenderer.prepareChunkRenders(matrix);
            long warmElapsed = System.nanoTime() - warmStarted;
            long cacheHitsAfterWarm = PreparedChunkCacheHits();
            long cacheMissesAfterWarm = PreparedChunkCacheMisses();

            PreparedChunkCache.clear();

            long coldStarted = System.nanoTime();
            ChunkSectionsToRender cold = mc.levelRenderer.prepareChunkRenders(matrix);
            long coldElapsed = System.nanoTime() - coldStarted;
            long cacheHitsAfterCold = PreparedChunkCacheHits();
            long cacheMissesAfterCold = PreparedChunkCacheMisses();

            long hotStarted = System.nanoTime();
            ChunkSectionsToRender hot = mc.levelRenderer.prepareChunkRenders(matrix);
            long hotElapsed = System.nanoTime() - hotStarted;
            long cacheHitsAfterHot = PreparedChunkCacheHits();
            long cacheMissesAfterHot = PreparedChunkCacheMisses();

            line(out, "warmTimeNs", warmElapsed);
            line(out, "coldTimeNs", coldElapsed);
            line(out, "hotTimeNs", hotElapsed);
            line(out, "coldVsHotSpeedup", ratio(coldElapsed, hotElapsed));
            line(out, "warmUsedCache", cacheHitsAfterWarm > cacheHitsBefore);
            line(out, "warmCacheHitDelta", cacheHitsAfterWarm - cacheHitsBefore);
            line(out, "warmCacheMissDelta", cacheMissesAfterWarm - cacheMissesBefore);
            line(out, "coldCacheHitDelta", cacheHitsAfterCold - cacheHitsAfterWarm);
            line(out, "coldCacheMissDelta", cacheMissesAfterCold - cacheMissesAfterWarm);
            line(out, "hotCacheHitDelta", cacheHitsAfterHot - cacheHitsAfterCold);
            line(out, "hotCacheMissDelta", cacheMissesAfterHot - cacheMissesAfterCold);

            appendPreparedDetails(out, "warm", warm == null ? hot : warm);
            appendPreparedDetails(out, "cold", cold);
            appendPreparedDetails(out, "hot", hot);
            appendPreparedHotColdDiff(out, cold, hot);
        } catch (Throwable t) {
            line(out, "prepareChunkRenders.error", t.getClass().getName() + ": " + t.getMessage());
        }
    }

    private static void appendPreparedDetails(StringBuilder out, String prefix, ChunkSectionsToRender prepared) {
        if (prepared == null) {
            line(out, prefix + ".preparedNull", true);
            return;
        }

        line(out, prefix + ".maxIndicesRequired", prepared.maxIndicesRequired());
        line(out, prefix + ".chunkSectionInfos", prepared.chunkSectionInfos().length);

        int totalHashBuckets = 0;
        int totalDrawLists = 0;
        int totalDraws = 0;
        int maxListSize = 0;

        for (var entry : prepared.drawGroupsPerLayer().entrySet()) {
            int hashBuckets = entry.getValue().size();
            int drawLists = 0;
            int draws = 0;
            int maxLayerList = 0;
            for (List<?> list : entry.getValue().values()) {
                drawLists++;
                draws += list.size();
                maxLayerList = Math.max(maxLayerList, list.size());
            }
            totalHashBuckets += hashBuckets;
            totalDrawLists += drawLists;
            totalDraws += draws;
            maxListSize = Math.max(maxListSize, maxLayerList);
            int layerIndex = entry.getKey().ordinal();
            int visibleSections = layerIndex < lastVisibleLayerSections.length ? lastVisibleLayerSections[layerIndex] : -1;
            long visibleIndices = layerIndex < lastVisibleLayerIndices.length ? lastVisibleLayerIndices[layerIndex] : -1L;
            line(out, prefix + ".layer." + entry.getKey() + ".hashBuckets", hashBuckets);
            line(out, prefix + ".layer." + entry.getKey() + ".drawLists", drawLists);
            line(out, prefix + ".layer." + entry.getKey() + ".draws", draws);
            line(out, prefix + ".layer." + entry.getKey() + ".avgDrawsPerList", ratio(draws, drawLists));
            line(out, prefix + ".layer." + entry.getKey() + ".maxListSize", maxLayerList);
            line(out, prefix + ".layer." + entry.getKey() + ".visibleSections", visibleSections);
            line(out, prefix + ".layer." + entry.getKey() + ".submittedVsVisible", ratio(draws, visibleSections));
            line(out, prefix + ".layer." + entry.getKey() + ".avgIndicesPerVisibleDraw", ratio(visibleIndices, visibleSections));
        }

        line(out, prefix + ".totalHashBuckets", totalHashBuckets);
        line(out, prefix + ".totalDrawLists", totalDrawLists);
        line(out, prefix + ".totalDraws", totalDraws);
        line(out, prefix + ".avgDrawsPerList", ratio(totalDraws, totalDrawLists));
        line(out, prefix + ".maxListSize", maxListSize);
    }

    private static void appendPreparedHotColdDiff(StringBuilder out, ChunkSectionsToRender cold, ChunkSectionsToRender hot) {
        if (cold == null || hot == null) return;
        line(out, "coldHot.sameSectionInfos", cold.chunkSectionInfos().length == hot.chunkSectionInfos().length);
        line(out, "coldHot.sameMaxIndicesRequired", cold.maxIndicesRequired() == hot.maxIndicesRequired());
        line(out, "coldHot.sameLayerKeyCount", cold.drawGroupsPerLayer().size() == hot.drawGroupsPerLayer().size());
    }

    private static void appendHookPressure(StringBuilder out) {
        section(out, "HookStats");
        long entityChecks = readStaticCount(Cuts.class, "entityCullChecks");
        long entitySkips = readStaticCount(Cuts.class, "entityCullSkips");
        long aoChecks = readStaticCount(Cuts.class, "chunkAoChecks");
        long aoSkips = readStaticCount(Cuts.class, "chunkAoSkips");
        long layerChecks = readStaticCount(Cuts.class, "sectionLayerChecks");
        long layerSkips = readStaticCount(Cuts.class, "sectionLayerSkips");
        long layerCacheHits = readStaticCount(Cuts.class, "sectionLayerCacheHits");
        long terrainChecks = readStaticCount(Cuts.class, "terrainSectionChecks");
        long terrainSkips = readStaticCount(Cuts.class, "terrainSectionSkips");
        long terrainCacheHits = readStaticCount(Cuts.class, "terrainSectionCacheHits");
        long terrainSuppressedChecks = readStaticCount(Cuts.class, "terrainCullSuppressedChecks");
        long lightCalls = readStaticCount(Cuts.class, "lightTrimCalls");
        long lightEligible = readStaticCount(Cuts.class, "lightTrimEligible");
        long lightNearRejected = readStaticCount(Cuts.class, "lightTrimRejectedNear");
        long fluidCalls = readStaticCount(Cuts.class, "fluidLightCalls");
        long fluidEligible = readStaticCount(Cuts.class, "fluidLightEligible");
        long fluidRejectedFar = readStaticCount(Cuts.class, "fluidLightRejectedFar");
        long fluidSuppressedCalls = readStaticCount(Cuts.class, "fluidLightSuppressedCalls");
        long resortChecks = readStaticCount(Cuts.class, "translucencyResortChecks");
        long resortSkips = readStaticCount(Cuts.class, "translucencyResortSkips");
        long resortSuppressedChecks = readStaticCount(Cuts.class, "translucencyResortSuppressedChecks");
        long cutoutLeafChecks = readStaticCount(Cuts.class, "cutoutLeafChecks");
        long cutoutLeafForcedOpaque = readStaticCount(Cuts.class, "cutoutLeafForcedOpaque");
        long cutoutLeafVanillaOpaque = readStaticCount(Cuts.class, "cutoutLeafVanillaOpaque");
        long cutoutAggPasses = readStaticCount(Cuts.class, "cutoutAggPasses");
        long cutoutAggListsBefore = readStaticCount(Cuts.class, "cutoutAggListsBefore");
        long cutoutAggListsAfter = readStaticCount(Cuts.class, "cutoutAggListsAfter");

        line(out, "entityCull.checks", entityChecks);
        line(out, "entityCull.skips", entitySkips);
        line(out, "chunkAo.checks", aoChecks);
        line(out, "chunkAo.skips", aoSkips);
        line(out, "layerTrim.checks", layerChecks);
        line(out, "layerTrim.skips", layerSkips);
        line(out, "layerTrim.cacheHits", layerCacheHits);
        line(out, "terrainCull.checks", terrainChecks);
        line(out, "terrainCull.skips", terrainSkips);
        line(out, "terrainCull.cacheHits", terrainCacheHits);
        line(out, "terrainCull.suppressedChecks", terrainSuppressedChecks);
        line(out, "lightTrim.calls", lightCalls);
        line(out, "lightTrim.eligible", lightEligible);
        line(out, "lightTrim.nearRejected", lightNearRejected);
        line(out, "fluidLight.calls", fluidCalls);
        line(out, "fluidLight.eligible", fluidEligible);
        line(out, "fluidLight.rejectedFar", fluidRejectedFar);
        line(out, "fluidLight.suppressedCalls", fluidSuppressedCalls);
        line(out, "translucencyResort.checks", resortChecks);
        line(out, "translucencyResort.skips", resortSkips);
        line(out, "translucencyResort.suppressedChecks", resortSuppressedChecks);
        line(out, "cutoutLeaf.checks", cutoutLeafChecks);
        line(out, "cutoutLeaf.forcedOpaque", cutoutLeafForcedOpaque);
        line(out, "cutoutLeaf.vanillaOpaque", cutoutLeafVanillaOpaque);
        line(out, "cutoutAgg.passes", cutoutAggPasses);
        line(out, "cutoutAgg.listsBefore", cutoutAggListsBefore);
        line(out, "cutoutAgg.listsAfter", cutoutAggListsAfter);
        line(out, "chunkAo.skipRate", percent(aoSkips, aoChecks));
        line(out, "layerTrim.skipRate", percent(layerSkips, layerChecks));
        line(out, "terrainCull.skipRate", percent(terrainSkips, terrainChecks));
        line(out, "lightTrim.eligibleRate", percent(lightEligible, lightCalls));
        line(out, "fluidLight.eligibleRate", percent(fluidEligible, fluidCalls));
        line(out, "translucencyResort.skipRate", percent(resortSkips, resortChecks));
        line(out, "cutoutLeaf.forceRate", percent(cutoutLeafForcedOpaque, cutoutLeafChecks));
    }

    private static String renderMods() {
        List<String> hits = new ArrayList<>();
        for (String id : Arrays.asList("sodium", "iris", "canvas", "bobby", "distanthorizons", "immediatelyfast", "lithium", "entityculling")) {
            if (FabricLoader.getInstance().isModLoaded(id)) hits.add(id);
        }
        return hits.isEmpty() ? "none" : String.join(", ", hits);
    }

    private static Object readField(Object target, String name) {
        if (target == null) return "null";
        Class<?> type = target.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(target);
            } catch (ReflectiveOperationException ignored) {
                type = type.getSuperclass();
            }
        }
        return "<missing>";
    }

    private static long readStaticCount(Class<?> owner, String name) {
        try {
            Field field = owner.getDeclaredField(name);
            field.setAccessible(true);
            Object value = field.get(null);
            if (value == null) return -1L;
            Method sum = value.getClass().getMethod("sum");
            Object result = sum.invoke(value);
            return result instanceof Number number ? number.longValue() : -1L;
        } catch (ReflectiveOperationException ignored) {
            return -1L;
        }
    }

    private static long PreparedChunkCacheHits() {
        return readStaticLong(PreparedChunkCache.class, "hits");
    }

    private static long PreparedChunkCacheMisses() {
        return readStaticLong(PreparedChunkCache.class, "misses");
    }

    private static long readStaticLong(Class<?> owner, String name) {
        try {
            Field field = owner.getDeclaredField(name);
            field.setAccessible(true);
            Object value = field.get(null);
            return value instanceof Number number ? number.longValue() : -1L;
        } catch (ReflectiveOperationException ignored) {
            return -1L;
        }
    }

    private static Object safeOption(SupplierEx supplier) {
        try {
            return supplier.get();
        } catch (Throwable t) {
            return "<error:" + t.getClass().getSimpleName() + ">";
        }
    }

    private static void section(StringBuilder out, String title) {
        out.append('\n').append("[").append(title).append("]\n");
    }

    private static void line(StringBuilder out, String key, Object value) {
        out.append(key).append('=').append(value).append('\n');
    }

    private static String formatVec(double x, double y, double z) {
        return fmt(x) + ", " + fmt(y) + ", " + fmt(z);
    }

    private static String fmt(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }

    private static String percent(long part, long total) {
        if (part < 0 || total <= 0) return "n/a";
        return fmt((double)part * 100.0 / (double)total) + "%";
    }

    private static String ratio(long a, long b) {
        if (a < 0 || b <= 0) return "n/a";
        return fmt((double)a / (double)b);
    }

    private static void pushTopSection(List<SectionPressure> top, SectionPressure candidate) {
        int insertAt = top.size();
        for (int i = 0; i < top.size(); i++) {
            if (candidate.indices > top.get(i).indices) {
                insertAt = i;
                break;
            }
        }
        if (insertAt < 5) {
            top.add(insertAt, candidate);
            if (top.size() > 5) top.remove(5);
        } else if (top.size() < 5) {
            top.add(candidate);
        }
    }

    private record SectionPressure(ChunkSectionLayer layer, BlockPos origin, double distance, int indices, boolean customIndex) {
    }

    private static int unlimitedFramerateCutoff(Minecraft mc) {
        Object value = readField(mc.options, "UNLIMITED_FRAMERATE_CUTOFF");
        return value instanceof Number number ? number.intValue() : -1;
    }

    private static String describeFramerateLimit(int value, int unlimitedCutoff) {
        if (unlimitedCutoff >= 0 && value >= unlimitedCutoff) return "UNLIMITED";
        return "LIMITED(" + value + ")";
    }

    @FunctionalInterface
    private interface SupplierEx {
        Object get() throws Exception;
    }
}
