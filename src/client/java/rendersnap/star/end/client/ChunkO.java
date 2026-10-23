package rendersnap.star.end.client;

import net.minecraft.TracingExecutor;
import net.minecraft.util.Util;
import rendersnap.star.end.Rendersnap;
import rendersnap.star.end.client.cfg.Opts;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public final class ChunkO {
    private static final AtomicInteger THREAD_ID = new AtomicInteger();
    private static final AtomicLong DROPPED_TASKS = new AtomicLong();
    private static final AtomicLong REJECTED_TASKS = new AtomicLong();
    private static final AtomicLong DELAYED_TASKS = new AtomicLong();
    private static final AtomicLong UPLOAD_WAITS = new AtomicLong();

    private static final long UPLOAD_WAIT_NS = 100_000L;
    private static final int MAX_QUEUE = 256;
    private static TracingExecutor executor;
    private static ThreadPoolExecutor pool;
    private static int activeWorkers;

    private static int paceCooldown;

    private ChunkO() {
    }

    public static synchronized TracingExecutor executor() {
        if (executor != null) return executor;

        int workers = Opts.chunkWorkerLimit;
        ThreadFactory factory = task -> {
            Thread thread = new Thread(task, "Rendersnap Chunk Worker " + THREAD_ID.incrementAndGet());
            thread.setDaemon(true);

            if (Opts.gentleWorkers) thread.setPriority(Thread.NORM_PRIORITY - 1);
            return thread;
        };
        pool = new ThreadPoolExecutor(
                workers,
                workers,
                30L,
                TimeUnit.SECONDS,

                new LinkedBlockingQueue<>(),
                factory,
                (task, current) -> {
                    if (!current.isShutdown() && current.getQueue().poll() != null) {
                        DROPPED_TASKS.incrementAndGet();
                        current.execute(task);
                    } else {
                        REJECTED_TASKS.incrementAndGet();
                    }
                }
        );
        pool.allowCoreThreadTimeOut(true);
        executor = new TracingExecutor(pool);
        activeWorkers = workers;
        Rendersnap.LOGGER.info("Rendersnap chunk mesh worker limit: {}", workers);
        return executor;
    }

    public static synchronized void close() {
        if (executor == null) return;
        executor.shutdownAndAwait(2L, TimeUnit.SECONDS);
        executor = null;
        pool = null;
        activeWorkers = 0;
    }

    public static int activeWorkers() {
        ThreadPoolExecutor current = pool;
        return current == null ? activeWorkers : current.getCorePoolSize();
    }

    public static int queuedTasks() {
        ThreadPoolExecutor current = pool;
        if (current == null) return 0;
        int queued = current.getQueue().size();
        if (queued > current.getCorePoolSize() * 4) DELAYED_TASKS.incrementAndGet();
        return queued;
    }

    public static int runningTasks() {
        ThreadPoolExecutor current = pool;
        return current == null ? 0 : current.getActiveCount();
    }

    public static long completedTasks() {
        ThreadPoolExecutor current = pool;
        return current == null ? 0L : current.getCompletedTaskCount();
    }

    public static long droppedTasks() {
        return DROPPED_TASKS.get();
    }

    public static long rejectedTasks() {
        return REJECTED_TASKS.get();
    }

    public static long delayedTasks() {
        return DELAYED_TASKS.get();
    }

    public static long uploadWaits() {
        return UPLOAD_WAITS.get();
    }

    public static void waitForUploadSpace() {
        if (Opts.gentleWorkers) {
            UPLOAD_WAITS.incrementAndGet();
            LockSupport.parkNanos(UPLOAD_WAIT_NS);
        } else {
            Thread.onSpinWait();
        }
    }

    public static void pace(long frameNs) {
        ThreadPoolExecutor current = pool;
        if (current == null || current.isShutdown()) return;
        int target = current.getCorePoolSize();
        if (frameNs > 33_333_334L) {
            target = Math.max(2, target - 1);
        } else if (frameNs < 16_666_667L && current.getQueue().size() > 0) {
            target = Math.min(Opts.chunkWorkerLimit, target + 1);
        }

        target = Math.max(1, Math.min(target, current.getMaximumPoolSize()));
        if (paceCooldown > 0) {
            paceCooldown--;
            return;
        }
        if (target != current.getCorePoolSize()) {
            current.setCorePoolSize(target);
            paceCooldown = 20;
        }
    }

    public static TracingExecutor vanillaExecutor() {
        return Util.backgroundExecutor();
    }
}
