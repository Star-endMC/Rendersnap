package rendersnap.star.end.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import rendersnap.star.end.Rendersnap;
import rendersnap.star.end.client.cfg.Opts;
import rendersnap.star.end.client.render.Cuts;
import rendersnap.star.end.client.render.ShadReuse;
import rendersnap.star.end.client.render.gpu.Gpu;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class RenderRpt {
    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss").withZone(ZoneId.systemDefault());
    private static final SystemToast.SystemToastId TOAST = new SystemToast.SystemToastId();
    private static KeyMapping key;

    private RenderRpt() {
    }

    public static void registerKeybind() {
        if (key != null) return;
        key = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.rendersnap.render_report",
                //? if >=26.3 {
                InputConstants.Type.KEYBOARD,
                //?} else {
                /*InputConstants.Type.KEYSYM,
                *///?}
                InputConstants.KEY_F8,
                new KeyMapping.Category(Identifier.fromNamespaceAndPath(Rendersnap.MOD_ID, "controls"))
        ));
    }

    public static void onClientTick(Minecraft mc) {
        FrameMet.record(mc.getFrameTimeNs());
        if (key == null) return;
        while (key.consumeClick()) write(mc);
    }

    private static void write(Minecraft mc) {
        try {
            Path dir = mc.gameDirectory.toPath().resolve("rendersnap-reports");
            Files.createDirectories(dir);
            Path report = dir.resolve("render-report-" + FILE_TIME.format(Instant.now()) + ".txt");
            Files.writeString(report, report(mc), StandardCharsets.UTF_8);
            SystemToast.addOrUpdate(McCompat.toastManager(mc), TOAST,
                    Component.literal("Rendersnap report written"), Component.literal(report.getFileName().toString()));
            Rendersnap.LOGGER.info("Rendersnap report written to {}", report);
        } catch (IOException | SecurityException e) {
            Rendersnap.LOGGER.warn("Couldn't write Rendersnap render report", e);
            SystemToast.addOrUpdate(McCompat.toastManager(mc), TOAST,
                    Component.literal("Rendersnap report failed"), Component.literal(e.getClass().getSimpleName()));
        }
    }

    private static String report(Minecraft mc) {
        StringBuilder out = new StringBuilder(2048);
        out.append("Rendersnap Performance Report\n");
        out.append("=============================\n\n");
        out.append("Minecraft: ").append(mc.getLaunchedVersion()).append('\n');
        out.append("Renderer: ").append(Compat.rendererStatus()).append('\n');
        out.append("GPU: ").append(Gpu.renderer()).append('\n');
        out.append("VRAM: ").append(Gpu.vramTotalMiB()).append(" MiB total / ").append(Gpu.vramFreeMiB()).append(" MiB free\n\n");
        out.append("-- Frame Performance --\n\n");
        out.append("Average FPS: ").append(fps(FrameMet.averageNs())).append('\n');
        out.append("Median FPS: ").append(fps(FrameMet.medianNs())).append('\n');
        out.append("95th percentile FPS: ").append(fps(FrameMet.percentileNs(95))).append('\n');
        out.append("99th percentile FPS: ").append(fps(FrameMet.percentileNs(99))).append('\n');
        out.append("Worst FPS: ").append(fps(FrameMet.worstNs())).append("\n\n");
        out.append("-- Frame Pacing (every frame, last ").append(FrameMet.frameCount()).append(" frames) --\n\n");
        out.append("Average FPS: ").append(FrameMet.frameAverageFps()).append('\n');
        out.append("1% low FPS: ").append(FrameMet.frameLowFps(0.01)).append('\n');
        out.append("0.1% low FPS: ").append(FrameMet.frameLowFps(0.001)).append("\n\n");
        out.append("-- Features --\n\n");
        Cuts.appendSummary(out);
        out.append("Liquid Fix: ").append(Opts.liquidFix ? "ENABLED" : "DISABLED").append("\n");
        out.append("Shadow Fix: ").append(Opts.shadowFix ? "ENABLED" : "DISABLED").append("\n\n");
        out.append("-- Bottlenecks --\n\n");
        out.append("World render average: ").append(fps(FrameMet.renderAverageNs())).append(" FPS equivalent\n");
        out.append("Chunk queue: ").append(ChunkO.queuedTasks()).append(" tasks\n");
        out.append("Coalesced chunk tasks: ").append(ChunkO.droppedTasks()).append("\n");
        out.append("Rejected chunk tasks: ").append(ChunkO.rejectedTasks()).append("\n");
        out.append("Chunk worker upload waits (slept instead of spinning): ").append(ChunkO.uploadWaits()).append("\n\n");
        out.append("-- Recommendations --\n\n");
        if (FrameMet.percentileNs(95) > 16_666_667L) {
            out.append("Frame pacing is above the 60 FPS target; inspect the main-thread remainder.\n");
        } else {
            out.append("Frame pacing meets the 60 FPS target in this sample.\n");
        }
        if (ChunkO.droppedTasks() > 0L) {
            out.append("Chunk work was coalesced under pressure; check chunk updates while travelling.\n");
        }
        out.append("\n");
        out.append("-- Machine Data --\n");
        line(out, "timestamp", Instant.now());
        line(out, "minecraft", mc.getLaunchedVersion());
        line(out, "fps", mc.getFps());
        line(out, "frame.currentNs", mc.getFrameTimeNs());
        line(out, "frame.latestNs", FrameMet.latestNs());
        line(out, "frame.averageNs", FrameMet.averageNs());
        line(out, "frame.worstNs", FrameMet.worstNs());
        line(out, "frame.medianNs", FrameMet.medianNs());
        line(out, "frame.p95Ns", FrameMet.percentileNs(95));
        line(out, "frame.p99Ns", FrameMet.percentileNs(99));
        line(out, "render.levelLatestNs", FrameMet.renderLatestNs());
        line(out, "render.levelAverageNs", FrameMet.renderAverageNs());
        line(out, "render.levelP95Ns", FrameMet.renderP95Ns());
        line(out, "renderer", Compat.rendererStatus());
        line(out, "compatibility.disabledFeatures", Compat.disabledFeatures());
        line(out, "gpu.vendor", Gpu.vendor());
        line(out, "gpu.renderer", Gpu.renderer());
        line(out, "gpu.vramTotalMiB", Gpu.vramTotalMiB());
        line(out, "gpu.vramFreeMiB", Gpu.vramFreeMiB());
        line(out, "option.liquidFix", Opts.liquidFix);
        line(out, "option.shadowFix", Opts.shadowFix);
        line(out, "option.entityCulling", Opts.entityCulling);
        line(out, "option.shadowReuse", Opts.shadowReuse);
        line(out, "option.chunkPacing", Opts.chunkPacing);
        line(out, "option.chunkWorkerLimit", Opts.chunkWorkerLimit);
        line(out, "chunk.activeWorkers", ChunkO.activeWorkers());
        line(out, "chunk.runningTasks", ChunkO.runningTasks());
        line(out, "chunk.queuedTasks", ChunkO.queuedTasks());
        line(out, "chunk.completedTasks", ChunkO.completedTasks());
        line(out, "chunk.droppedTasks", ChunkO.droppedTasks());
        line(out, "chunk.coalescedTasks", ChunkO.droppedTasks());
        line(out, "chunk.rejectedTasks", ChunkO.rejectedTasks());
        line(out, "chunk.delayedTasks", ChunkO.delayedTasks());
        line(out, "chunk.uploadWaits", ChunkO.uploadWaits());
        line(out, "frameFull.count", FrameMet.frameCount());
        line(out, "frameFull.averageFps", FrameMet.frameAverageFps());
        line(out, "frameFull.low1Fps", FrameMet.frameLowFps(0.01));
        line(out, "frameFull.low01Fps", FrameMet.frameLowFps(0.001));
        line(out, "option.gentleWorkers", Opts.gentleWorkers);
        line(out, "option.rebuildGuard", Opts.rebuildGuard);
        line(out, "option.lightPacing", Opts.lightPacing);
        line(out, "option.caveCulling", Opts.caveCulling);
        line(out, "option.fastLaunch", Opts.fastLaunch);
        line(out, "option.chunkShadeTrim", Opts.chunkShadeTrim);
        line(out, "option.fogLayerTrim", Opts.fogOcclusion);
        line(out, "option.behindCamera", Opts.behindCamMode);
        line(out, "option.hideWeather", Opts.hideWeather);

        //? if >=26.2 {
        SectionRenderDispatcher dispatcher = mc.levelRenderer.sectionRenderDispatcher();
        //?} else {
        /*SectionRenderDispatcher dispatcher = mc.levelRenderer.getSectionRenderDispatcher();
        *///?}
        if (dispatcher != null) {
            line(out, "chunk.compileQueue", dispatcher.getCompileQueueSize());
            line(out, "chunk.freeBuffers", dispatcher.getFreeBufferCount());
            line(out, "chunk.dispatcher", dispatcher.getStats());
        } else {
            line(out, "chunk.dispatcher", "unavailable (no world loaded)");
        }
        out.append("\n[feature-counters]\n");
        Cuts.appendDebug(out);
        ShadReuse.appendDebug(out);
        ReduceSP.appendDebug(out);
        return out.toString();
    }

    private static void line(StringBuilder out, String key, Object value) {
        out.append(key).append('=').append(value).append('\n');
    }

    private static double fps(long nanos) {
        return nanos <= 0L ? 0.0 : 1_000_000_000.0 / nanos;
    }
}
