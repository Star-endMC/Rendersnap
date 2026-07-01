package rendersnap.star.end.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.fabricmc.loader.api.FabricLoader;
import org.admany.quantified.core.common.telemetry.TaskKindTelemetry;
import rendersnap.star.end.Rendersnap;

public final class ChunkBudget {
    private static volatile int lastQueued;
    private static int budget;
    private static int scheduled;

    private ChunkBudget() {
    }

    public static void beginPass(int queued) {
        lastQueued = queued;
        scheduled = 0;
        budget = queued >= 384 ? 6 : queued >= 256 ? 8 : queued >= 128 ? 12 : queued >= 64 ? 16 : 24;
        if (queued > 0 && FabricLoader.getInstance().isModLoaded("quantified")) {
            TaskKindTelemetry.recordMultithreading(Rendersnap.MOD_ID, "chunk-compile-pass");
            TaskKindTelemetry.recordBatch(Rendersnap.MOD_ID, "chunk-compile-pass", queued);
        }
    }

    public static boolean shouldDefer(int queued, boolean playerDirty, BlockPos origin) {
        if (playerDirty) return false;
        if (origin == null) return false;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return false;

        double dx = origin.getX() + 8.0 - mc.player.getX();
        double dy = origin.getY() + 8.0 - mc.player.getY();
        double dz = origin.getZ() + 8.0 - mc.player.getZ();
        double distSq = dx * dx + dy * dy + dz * dz;
        if (distSq <= 48.0 * 48.0) return false;

        int limit = queued >= 384 ? 6 : queued >= 256 ? 8 : queued >= 128 ? 12 : queued >= 64 ? 16 : budget;
        if (scheduled < limit) {
            scheduled++;
            return false;
        }
        return queued > 24;
    }

    public static void appendDebug(StringBuilder out) {
        out.append("lastQueued=").append(lastQueued).append('\n');
        out.append("budget=").append(budget).append('\n');
        out.append("scheduled=").append(scheduled).append('\n');
    }
}
