package rendersnap.star.end.mixin.world;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.minecraft.client.renderer.ItemInHandRenderer")
public abstract class IHSGM {
    @Shadow @Final private Minecraft minecraft;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void rendersnap$skipTickWithoutPlayer(CallbackInfo ci) {
        if (this.minecraft.player == null) {
            ci.cancel();
        }
    }
}
