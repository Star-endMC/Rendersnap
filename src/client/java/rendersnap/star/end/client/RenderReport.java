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
import rendersnap.star.end.client.render.ShadowReuse;
import rendersnap.star.end.client.render.gpu.Gpu;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class RenderReport {
    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss").withZone(ZoneId.systemDefault());
    private static final SystemToast.SystemToastId TOAST = new SystemToast.SystemToastId();
    private static KeyMapping key;

    private RenderReport() {
    }

    public static void registerKeybind() {
        if (key != null) return;
        key = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.rendersnap.render_report",
                InputConstants.Type.KEYSYM,
                InputConstants.KEY_F8,
                new KeyMapping.Category(Identifier.fromNamespaceAndPath(Rendersnap.MOD_ID, "controls"))
        ));
    }

    public static void onClientTick(Minecraft mc) {
        FrameMetrics.record(mc.getFrameTimeNs());
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
        } catch (IOException e) {
            Rendersnap.LOGGER.warn("Couldn't write Rendersnap render report", e);
            SystemToast.addOrUpdate(McCompat.toastManager(mc), TOAST,
                    Component.literal("Rendersnap report failed"), Component.literal(e.getClass().getSimpleName()));
        }
    }

    private static String report(Minecraft mc) {
        StringBuilder out = new StringBuilder(2048);
        line(out, "timestamp", Instant.now());
        line(out, "minecraft", mc.getLaunchedVersion());
        line(out, "fps", mc.getFps());
        line(out, "frame.currentNs", mc.getFrameTimeNs());
        line(out, "frame.medianNs", FrameMetrics.medianNs());
        line(out, "frame.p95Ns", FrameMetrics.percentileNs(95));
        line(out, "frame.p99Ns", FrameMetrics.percentileNs(99));
        line(out, "renderer", Compatibility.rendererStatus());
        line(out, "gpu.vendor", Gpu.vendor());
        line(out, "gpu.renderer", Gpu.renderer());
        line(out, "option.entityCulling", Opts.entityCulling);
        line(out, "option.shadowReuse", Opts.shadowReuse);
        line(out, "option.chunkPacing", Opts.chunkPacing);
        line(out, "option.chunkWorkerLimit", Opts.chunkWorkerLimit);
        line(out, "chunk.activeWorkers", ChunkWorkerPool.activeWorkers());
        line(out, "option.graphicsPresetDistanceCap", Opts.graphicsPresetDistanceCap);
        line(out, "option.fastLaunch", Opts.fastLaunch);
        line(out, "option.chunkShadeTrim", Opts.chunkShadeTrim);
        line(out, "option.farLayerTrim", Opts.farLayerTrim);
        line(out, "option.fogLayerTrim", Opts.fogOcclusion);
        line(out, "option.behindCamera", Opts.behindCamMode);
        line(out, "option.hideWeather", Opts.hideWeather);

        SectionRenderDispatcher dispatcher = mc.levelRenderer.sectionRenderDispatcher();
        line(out, "chunk.compileQueue", dispatcher.getCompileQueueSize());
        line(out, "chunk.freeBuffers", dispatcher.getFreeBufferCount());
        line(out, "chunk.dispatcher", dispatcher.getStats());
        out.append("\n[feature-counters]\n");
        Cuts.appendDebug(out);
        ShadowReuse.appendDebug(out);
        return out.toString();
    }

    private static void line(StringBuilder out, String key, Object value) {
        out.append(key).append('=').append(value).append('\n');
    }
}
