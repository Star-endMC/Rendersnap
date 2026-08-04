package rendersnap.star.end.client;

import net.minecraft.TracingExecutor;
import net.minecraft.util.Util;
import rendersnap.star.end.Rendersnap;
import rendersnap.star.end.client.cfg.Opts;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class ChunkWorkerPool {
    private static final AtomicInteger THREAD_ID = new AtomicInteger();
    private static TracingExecutor executor;
    private static int activeWorkers;

    private ChunkWorkerPool() {
    }

    public static synchronized TracingExecutor executor() {
        if (executor != null) return executor;

        int workers = Opts.chunkWorkerLimit;
        ThreadFactory factory = task -> {
            Thread thread = new Thread(task, "Rendersnap Chunk Worker " + THREAD_ID.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
        ExecutorService service = Executors.newFixedThreadPool(workers, factory);
        executor = new TracingExecutor(service);
        activeWorkers = workers;
        Rendersnap.LOGGER.info("Rendersnap chunk mesh worker limit: {}", workers);
        return executor;
    }

    public static synchronized void close() {
        if (executor == null) return;
        executor.shutdownAndAwait(2L, TimeUnit.SECONDS);
        executor = null;
        activeWorkers = 0;
    }

    public static int activeWorkers() {
        return activeWorkers;
    }

    public static TracingExecutor vanillaExecutor() {
        return Util.backgroundExecutor();
    }
}
