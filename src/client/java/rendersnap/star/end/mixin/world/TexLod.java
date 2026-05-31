package rendersnap.star.end.mixin.world;

import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
//? if >=26.1.2 {
import com.mojang.blaze3d.textures.FilterMode;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import rendersnap.star.end.client.render.Cuts;
//?}

@Mixin(LevelRenderer.class)
public abstract class TexLod {
    //? if >=26.1.2 {
    @ModifyArg(
            method = "lambda$addMainPass$0",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/GpuDevice;createSampler(Lcom/mojang/blaze3d/textures/AddressMode;Lcom/mojang/blaze3d/textures/AddressMode;Lcom/mojang/blaze3d/textures/FilterMode;Lcom/mojang/blaze3d/textures/FilterMode;ILjava/util/OptionalDouble;)Lcom/mojang/blaze3d/textures/GpuSampler;"),
            index = 2
    )
    private FilterMode roughMinFilter(FilterMode vanilla) {
        return Cuts.roughTerrainTextures() ? FilterMode.NEAREST : vanilla;
    }

    @ModifyArg(
            method = "lambda$addMainPass$0",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/GpuDevice;createSampler(Lcom/mojang/blaze3d/textures/AddressMode;Lcom/mojang/blaze3d/textures/AddressMode;Lcom/mojang/blaze3d/textures/FilterMode;Lcom/mojang/blaze3d/textures/FilterMode;ILjava/util/OptionalDouble;)Lcom/mojang/blaze3d/textures/GpuSampler;"),
            index = 4
    )
    private int roughAniso(int vanilla) {
        return Cuts.roughTerrainTextures() ? 1 : vanilla;
    }
    //?}
}
