package rendersnap.star.end.mixin.render;

import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rendersnap.star.end.client.cfg.Opts;

@Mixin(TextureAtlas.class)
public abstract class LA {
    private static final Identifier BLOCK_ATLAS = Identifier.withDefaultNamespace("textures/atlas/blocks.png");
    @Shadow @Final private Identifier location;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true, require = 0)
    private void rendersnap$freezeLiquidFrames(CallbackInfo ci) {
        if (Opts.liquidFix && BLOCK_ATLAS.equals(location)) ci.cancel();
    }
}
