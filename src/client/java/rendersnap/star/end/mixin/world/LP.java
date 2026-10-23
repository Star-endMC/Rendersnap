package rendersnap.star.end.mixin.world;

import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rendersnap.star.end.client.ReduceSP;

import java.util.Deque;

@Mixin(ClientLevel.class)
public abstract class LP {
    @Shadow @Final private Deque<Runnable> lightUpdateQueue;

    @Inject(method = "pollLightUpdates", at = @At("HEAD"), cancellable = true, require = 0)
    private void rendersnap$paceLightFlood(CallbackInfo ci) {
        if (ReduceSP.pollLight(this.lightUpdateQueue)) ci.cancel();
    }
}
