package rendersnap.star.end.client;

import java.util.Arrays;

public final class FrameMet {
    private static final long[] SAMPLES = new long[600];
    private static final long[] RENDER_SAMPLES = new long[600];
    private static int size;
    private static int next;
    private static long latest;
    private static int renderSize;
    private static int renderNext;
    private static long renderLatest;

    private static final long[] FRAMES = new long[8192];
    private static int frameSize;
    private static int frameNext;
    private static int frameSerial;

    private FrameMet() {
    }

    public static void record(long nanos) {
        if (nanos <= 0L) return;
        latest = nanos;
        SAMPLES[next] = nanos;
        next = (next + 1) % SAMPLES.length;
        if (size < SAMPLES.length) size++;
    }

    public static long medianNs() {
        return percentileNs(50);
    }

    public static void recordLevelRender(long nanos) {
        if (nanos <= 0L) return;
        renderLatest = nanos;
        RENDER_SAMPLES[renderNext] = nanos;
        renderNext = (renderNext + 1) % RENDER_SAMPLES.length;
        if (renderSize < RENDER_SAMPLES.length) renderSize++;
    }

    public static long renderLatestNs() {
        return renderLatest;
    }

    public static long renderAverageNs() {
        if (renderSize == 0) return 0L;
        long total = 0L;
        for (int i = 0; i < renderSize; i++) total += RENDER_SAMPLES[i];
        return total / renderSize;
    }

    public static long renderP95Ns() {
        if (renderSize == 0) return 0L;
        long[] copy = Arrays.copyOf(RENDER_SAMPLES, renderSize);
        Arrays.sort(copy);
        int index = Math.min(copy.length - 1, Math.max(0, (int) Math.ceil(copy.length * 0.95) - 1));
        return copy[index];
    }

    public static long latestNs() {
        return latest;
    }

    public static long averageNs() {
        if (size == 0) return 0L;
        long total = 0L;
        for (int i = 0; i < size; i++) total += SAMPLES[i];
        return total / size;
    }

    public static long worstNs() {
        long worst = 0L;
        for (int i = 0; i < size; i++) worst = Math.max(worst, SAMPLES[i]);
        return worst;
    }

    public static void recordFrame(long nanos) {
        if (nanos <= 0L) return;
        FRAMES[frameNext] = nanos;
        frameNext = (frameNext + 1) % FRAMES.length;
        if (frameSize < FRAMES.length) frameSize++;
        frameSerial++;
        Bench.onFrame(nanos);
    }

    public static int frameCount() {
        return frameSize;
    }

    public static double frameLowFps(double fraction) {
        return lowFps(recentFrames(frameSize), fraction);
    }

    public static double frameAverageFps() {
        long[] frames = recentFrames(frameSize);
        if (frames.length == 0) return 0.0;
        long total = 0L;
        for (long frame : frames) total += frame;
        return total <= 0L ? 0.0 : frames.length * 1_000_000_000.0 / total;
    }

    public static int frameSerial() {
        return frameSerial;
    }

    public static double lowFps(long[] frameTimesNs, double fraction) {
        if (frameTimesNs == null || frameTimesNs.length == 0) return 0.0;
        long[] copy = Arrays.copyOf(frameTimesNs, frameTimesNs.length);
        Arrays.sort(copy);
        int worst = Math.max(1, (int) Math.ceil(copy.length * fraction));
        long total = 0L;
        for (int i = copy.length - worst; i < copy.length; i++) total += copy[i];
        double average = total / (double) worst;
        return average <= 0.0 ? 0.0 : 1_000_000_000.0 / average;
    }

    private static long[] recentFrames(int count) {
        int n = Math.max(0, Math.min(count, frameSize));
        long[] out = new long[n];
        for (int i = 0; i < n; i++) {
            int index = Math.floorMod(frameNext - n + i, FRAMES.length);
            out[i] = FRAMES[index];
        }
        return out;
    }

    public static long percentileNs(int percentile) {
        if (size == 0) return 0L;
        long[] copy = Arrays.copyOf(SAMPLES, size);
        Arrays.sort(copy);
        int index = Math.min(copy.length - 1, Math.max(0, (int)Math.ceil(copy.length * (percentile / 100.0)) - 1));
        return copy[index];
    }
}
