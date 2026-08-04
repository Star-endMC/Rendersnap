package rendersnap.star.end.mixin.world;

import net.minecraft.TracingExecutor;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rendersnap.star.end.client.ChunkWorkerPool;

@Mixin(LevelRenderer.class)
public abstract class ChunkWorkerLimitMixin {
    @Redirect(
            method = "invalidateCompiledGeometry",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Util;backgroundExecutor()Lnet/minecraft/TracingExecutor;")
    )
    private TracingExecutor rendersnap$chunkWorkerExecutor() {
        return ChunkWorkerPool.executor();
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void rendersnap$closeChunkWorkers(CallbackInfo ci) {
        ChunkWorkerPool.close();
    }
}
