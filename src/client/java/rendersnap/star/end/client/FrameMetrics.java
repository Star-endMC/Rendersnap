package rendersnap.star.end.client;

import java.util.Arrays;

public final class FrameMetrics {
    private static final long[] SAMPLES = new long[600];
    private static int size;
    private static int next;

    private FrameMetrics() {
    }

    public static void record(long nanos) {
        if (nanos <= 0L) return;
        SAMPLES[next] = nanos;
        next = (next + 1) % SAMPLES.length;
        if (size < SAMPLES.length) size++;
    }

    public static long medianNs() {
        return percentileNs(50);
    }

    public static long percentileNs(int percentile) {
        if (size == 0) return 0L;
        long[] copy = Arrays.copyOf(SAMPLES, size);
        Arrays.sort(copy);
        int index = Math.min(copy.length - 1, Math.max(0, (int)Math.ceil(copy.length * (percentile / 100.0)) - 1));
        return copy[index];
    }
}
