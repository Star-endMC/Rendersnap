package rendersnap.star.end.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import rendersnap.star.end.Rendersnap;
import rendersnap.star.end.client.render.Cuts;
import rendersnap.star.end.client.render.ShadReuse;
import rendersnap.star.end.client.render.gpu.Gpu;
import rendersnap.star.end.client.cfg.Opts;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadMXBean;
import java.util.List;

public final class Bench {
    private static final int SAMPLE_LIMIT = 600;
    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss").withZone(ZoneId.systemDefault());
    private static final SystemToast.SystemToastId TOAST = new SystemToast.SystemToastId();
    private static KeyMapping key;
    private static long[] samples = new long[SAMPLE_LIMIT];
    private static int size;
    private static boolean running;
    private static int below60;
    private static int below30;
    private static int below20;
    private static int below10;
    private static int chunkPressureFrames;
    private static int chunkSpikeFrames;
    private static int otherSpikeFrames;
    private static long queueTotal;
    private static int queuePeak;
    private static int runningPeak;
    private static long startedAt;
    private static long cpuStarted;
    private static long gcStarted;
    private static long gcTimeStarted;
    private static long heapStarted;
    private static final int FRAME_LIMIT = 65536;
    private static final long[] frameSamples = new long[FRAME_LIMIT];

    private static final boolean[] frameHadGc = new boolean[FRAME_LIMIT];
    private static int frameCount;
    private static long lastFrameGcCount;

    private Bench() {
    }

    public static void registerKeybind() {
        if (key != null) return;
        key = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.rendersnap.benchmark",
                //? if >=26.3 {
                InputConstants.Type.KEYBOARD,
                //?} else {
                /*InputConstants.Type.KEYSYM,
                *///?}
                InputConstants.KEY_F9,
                new KeyMapping.Category(Identifier.fromNamespaceAndPath(Rendersnap.MOD_ID, "controls"))
        ));
    }

    public static void onFrame(long nanos) {
        if (!running || nanos <= 0L || frameCount >= FRAME_LIMIT) return;
        long gc = gcCount();
        frameHadGc[frameCount] = gc != lastFrameGcCount;
        lastFrameGcCount = gc;
        frameSamples[frameCount++] = nanos;
    }

    public static void tick(Minecraft mc) {
        if (key == null) return;
        while (key.consumeClick()) toggle(mc);
        if (!running) return;
        long frame = FrameMet.latestNs();
        if (frame <= 0L) return;
        samples[size++] = frame;
        int queued = ChunkO.queuedTasks();
        int workers = Math.max(1, ChunkO.activeWorkers());
        queueTotal += queued;
        queuePeak = Math.max(queuePeak, queued);
        runningPeak = Math.max(runningPeak, ChunkO.runningTasks());
        if (frame > 16_666_667L) below60++;
        if (frame > 33_333_334L) below30++;
        if (frame > 50_000_000L) below20++;
        if (frame > 100_000_000L) below10++;
        if (queued >= Math.max(32, workers * 8)) {
            chunkPressureFrames++;
            if (frame > 33_333_334L) chunkSpikeFrames++;
        } else if (frame > 33_333_334L) {
            otherSpikeFrames++;
        }
        if (size >= SAMPLE_LIMIT) finish(mc);
    }

    private static void toggle(Minecraft mc) {
        if (running) {
            finish(mc);
            return;
        }
        Arrays.fill(samples, 0L);
        size = 0;
        frameCount = 0;
        lastFrameGcCount = gcCount();
        below60 = 0;
        below30 = 0;
        below20 = 0;
        below10 = 0;
        chunkPressureFrames = 0;
        chunkSpikeFrames = 0;
        otherSpikeFrames = 0;
        queueTotal = 0L;
        queuePeak = 0;
        runningPeak = 0;
        startedAt = System.nanoTime();
        cpuStarted = threadCpuTime();
        gcStarted = gcCount();
        gcTimeStarted = gcTime();
        heapStarted = heapUsed();
        running = true;
        toast(mc, "rendersnap.toast.benchmark.start", "rendersnap.toast.benchmark.start.body");
    }

    private static void finish(Minecraft mc) {
        if (!running) return;
        running = false;
        if (size == 0) return;
        long[] copy = Arrays.copyOf(samples, size);
        Arrays.sort(copy);
        try {
            Path dir = mc.gameDirectory.toPath().resolve("rendersnap-reports");
            Files.createDirectories(dir);
            Path report = dir.resolve("benchmark-" + FILE_TIME.format(Instant.now()) + ".txt");
            StringBuilder out = new StringBuilder(2048);
            out.append("Rendersnap Performance Bench\n");
            out.append("================================\n\n");
            out.append("Minecraft: ").append(mc.getLaunchedVersion()).append('\n');
            out.append("Renderer: ").append(Compat.rendererStatus()).append('\n');
            out.append("GPU: ").append(Gpu.renderer()).append("\n\n");
            out.append("-- Frame Performance --\n\n");
            out.append("Average FPS: ").append(fps(average(copy))).append('\n');
            out.append("Median FPS: ").append(fps(percentile(copy, 50))).append('\n');
            out.append("95th percentile FPS: ").append(fps(percentile(copy, 95))).append('\n');
            out.append("99th percentile FPS: ").append(fps(percentile(copy, 99))).append('\n');
            out.append("Worst FPS: ").append(fps(copy[copy.length - 1])).append("\n\n");
            appendFramePacing(out);
            out.append("-- Features --\n\n");
            Cuts.appendSummary(out);
            out.append("Liquid Fix: ").append(Opts.liquidFix ? "ENABLED" : "DISABLED").append('\n');
            out.append("Shadow Fix: ").append(Opts.shadowFix ? "ENABLED" : "DISABLED").append("\n\n");
            out.append("-- Machine Data --\n");
            line(out, "minecraft", mc.getLaunchedVersion());
            line(out, "renderer", Compat.rendererStatus());
            line(out, "gpu.vendor", Gpu.vendor());
            line(out, "gpu.renderer", Gpu.renderer());
            line(out, "gpu.vramTotalMiB", Gpu.vramTotalMiB());
            line(out, "gpu.vramFreeMiB", Gpu.vramFreeMiB());
            line(out, "gpu.load", Gpu.loadStatus());
            line(out, "option.liquidFix", Opts.liquidFix);
            line(out, "option.shadowFix", Opts.shadowFix);
            line(out, "option.entityCulling", Opts.entityCulling);
            line(out, "option.chunkShadeTrim", Opts.chunkShadeTrim);
            line(out, "option.fogOcclusion", Opts.fogOcclusion);
            line(out, "option.behindCamMode", Opts.behindCamMode);
            line(out, "frames", size);
            line(out, "frame.averageNs", average(copy));
            line(out, "frame.medianNs", percentile(copy, 50));
            line(out, "frame.p95Ns", percentile(copy, 95));
            line(out, "frame.p99Ns", percentile(copy, 99));
            line(out, "frame.worstNs", copy[copy.length - 1]);
            line(out, "render.levelLatestNs", FrameMet.renderLatestNs());
            line(out, "render.levelAverageNs", FrameMet.renderAverageNs());
            line(out, "render.levelP95Ns", FrameMet.renderP95Ns());
            line(out, "fps.average", fps(average(copy)));
            line(out, "fps.median", fps(percentile(copy, 50)));
            line(out, "drops.below60fps", below60);
            line(out, "drops.below30fps", below30);
            line(out, "drops.below20fps", below20);
            line(out, "drops.below10fps", below10);
            line(out, "drops.below60fpsPercent", percent(below60, size));
            line(out, "drops.below30fpsPercent", percent(below30, size));
            line(out, "lag.chunkPressureFrames", chunkPressureFrames);
            line(out, "lag.chunkSpikeFrames", chunkSpikeFrames);
            line(out, "lag.otherSpikeFrames", otherSpikeFrames);
            line(out, "lag.primary", primaryLag());
            line(out, "lag.chunkPressurePercent", percent(chunkPressureFrames, size));
            line(out, "chunk.averageQueuedTasks", queueTotal / (double) size);
            line(out, "chunk.peakQueuedTasks", queuePeak);
            line(out, "chunk.peakRunningTasks", runningPeak);
            line(out, "runtime.elapsedMs", (System.nanoTime() - startedAt) / 1_000_000L);
            line(out, "runtime.clientCpuMs", Math.max(0L, threadCpuTime() - cpuStarted) / 1_000_000L);
            line(out, "runtime.gcCollections", Math.max(0L, gcCount() - gcStarted));
            line(out, "runtime.gcTimeMs", Math.max(0L, gcTime() - gcTimeStarted));
            line(out, "runtime.heapStartMiB", heapStarted / 1048576L);
            line(out, "runtime.heapEndMiB", heapUsed() / 1048576L);
            line(out, "chunk.activeWorkers", ChunkO.activeWorkers());
            line(out, "chunk.runningTasks", ChunkO.runningTasks());
            line(out, "chunk.queuedTasks", ChunkO.queuedTasks());
            line(out, "chunk.completedTasks", ChunkO.completedTasks());
            line(out, "chunk.droppedTasks", ChunkO.droppedTasks());
            line(out, "chunk.coalescedTasks", ChunkO.droppedTasks());
            line(out, "chunk.rejectedTasks", ChunkO.rejectedTasks());
            line(out, "chunk.delayedTasks", ChunkO.delayedTasks());
            line(out, "chunk.uploadWaits", ChunkO.uploadWaits());
            line(out, "option.caveCulling", Opts.caveCulling);
            line(out, "option.gentleWorkers", Opts.gentleWorkers);
            line(out, "option.rebuildGuard", Opts.rebuildGuard);
            line(out, "option.lightPacing", Opts.lightPacing);
            appendFrameMachineData(out);
            out.append("\n[feature-counters]\n");
            Cuts.appendDebug(out);
            ShadReuse.appendDebug(out);
            ReduceSP.appendDebug(out);
            Files.writeString(report, out.toString(), StandardCharsets.UTF_8);
            SystemToast.addOrUpdate(McCompat.toastManager(mc), TOAST,
                    Component.translatable("rendersnap.toast.benchmark.done"), Component.literal(report.getFileName().toString()));
            Rendersnap.LOGGER.info("Rendersnap benchmark written to {}", report);
        } catch (IOException | SecurityException e) {
            Rendersnap.LOGGER.warn("Couldn't write Rendersnap benchmark", e);
            toast(mc, "rendersnap.toast.benchmark.failed", "rendersnap.toast.benchmark.failed.body");
        }
    }

    private static void appendFramePacing(StringBuilder out) {
        out.append("-- Frame Pacing (every frame) --\n\n");
        if (frameCount == 0) {
            out.append("No frames were captured.\n\n");
            return;
        }
        long[] frames = Arrays.copyOf(frameSamples, frameCount);
        long[] sorted = Arrays.copyOf(frames, frames.length);
        Arrays.sort(sorted);
        long median = percentile(sorted, 50);
        int stutters = 0;
        for (long frame : frames) if (frame > median * 2L) stutters++;
        out.append("Frames measured: ").append(frameCount).append('\n');
        out.append("Average FPS: ").append(fps(average(frames))).append('\n');
        out.append("1% low FPS: ").append(FrameMet.lowFps(frames, 0.01)).append('\n');
        out.append("0.1% low FPS: ").append(FrameMet.lowFps(frames, 0.001));
        out.append(frameCount < 1000 ? " (needs 1000+ frames to be reliable)\n" : "\n");
        out.append("Worst frame: ").append(sorted[sorted.length - 1] / 1_000_000.0).append(" ms\n");
        out.append("Stutters (frames over 2x the median frame time): ").append(stutters).append('\n');
        appendSpikeTimeline(out, frames, median);
        out.append('\n');
    }

    private static void appendSpikeTimeline(StringBuilder out, long[] frames, long median) {
        long threshold = Math.max(median * 2L, 25_000_000L);
        int spikes = 0;
        int spikesWithGc = 0;
        StringBuilder timeline = new StringBuilder();
        long elapsed = 0L;
        for (int i = 0; i < frames.length; i++) {
            elapsed += frames[i];
            if (frames[i] < threshold) continue;
            spikes++;
            if (frameHadGc[i]) spikesWithGc++;
            if (spikes <= 40) {
                timeline.append(String.format(java.util.Locale.ROOT, "  t=%6.2fs  %6.1f ms%s%n",
                        elapsed / 1_000_000_000.0, frames[i] / 1_000_000.0, frameHadGc[i] ? "  [GC]" : ""));
            }
        }
        out.append("Big frames (over ").append(threshold / 1_000_000L).append(" ms): ").append(spikes)
                .append(", of which during garbage collection: ").append(spikesWithGc).append('\n');
        if (spikes > 0) out.append("Spike timeline (first 40):\n").append(timeline);
    }

    private static void appendFrameMachineData(StringBuilder out) {
        if (frameCount == 0) return;
        long[] frames = Arrays.copyOf(frameSamples, frameCount);
        long[] sorted = Arrays.copyOf(frames, frames.length);
        Arrays.sort(sorted);
        line(out, "frameFull.count", frameCount);
        line(out, "frameFull.averageFps", fps(average(frames)));
        line(out, "frameFull.low1Fps", FrameMet.lowFps(frames, 0.01));
        line(out, "frameFull.low01Fps", FrameMet.lowFps(frames, 0.001));
        line(out, "frameFull.medianNs", percentile(sorted, 50));
        line(out, "frameFull.p99Ns", percentile(sorted, 99));
        line(out, "frameFull.worstNs", sorted[sorted.length - 1]);
    }

    private static long average(long[] values) {
        long total = 0L;
        for (long value : values) total += value;
        return total / values.length;
    }

    private static long percentile(long[] values, int percentile) {
        int index = Math.min(values.length - 1, Math.max(0, (int) Math.ceil(values.length * (percentile / 100.0)) - 1));
        return values[index];
    }

    private static double fps(long nanos) {
        return nanos <= 0L ? 0.0 : 1_000_000_000.0 / nanos;
    }

    private static double percent(int count, int total) {
        return total <= 0 ? 0.0 : count * 100.0 / total;
    }

    private static String primaryLag() {
        if (chunkSpikeFrames == 0 && otherSpikeFrames == 0) return "none";
        return chunkSpikeFrames >= otherSpikeFrames ? "chunk-work" : "render-or-other";
    }

    private static long threadCpuTime() {
        ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        return bean.isCurrentThreadCpuTimeSupported() ? Math.max(0L, bean.getCurrentThreadCpuTime()) : 0L;
    }

    private static long gcCount() {
        long total = 0L;
        List<GarbageCollectorMXBean> beans = ManagementFactory.getGarbageCollectorMXBeans();
        for (GarbageCollectorMXBean bean : beans) if (bean.getCollectionCount() > 0) total += bean.getCollectionCount();
        return total;
    }

    private static long gcTime() {
        long total = 0L;
        List<GarbageCollectorMXBean> beans = ManagementFactory.getGarbageCollectorMXBeans();
        for (GarbageCollectorMXBean bean : beans) if (bean.getCollectionTime() > 0) total += bean.getCollectionTime();
        return total;
    }

    private static long heapUsed() {
        MemoryUsage usage = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        return Math.max(0L, usage.getUsed());
    }

    private static void line(StringBuilder out, String key, Object value) {
        out.append(key).append('=').append(value).append('\n');
    }

    private static void toast(Minecraft mc, String title, String body) {
        SystemToast.addOrUpdate(McCompat.toastManager(mc), TOAST,
                Component.translatable(title), Component.translatable(body));
    }
}
