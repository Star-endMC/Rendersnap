package rendersnap.star.end.mixin.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rendersnap.star.end.client.menu.VideoPage;

import java.util.function.Supplier;

@Mixin(OptionsScreen.class)
public abstract class OptsBtn extends Screen {
    @Shadow @Final
    private static Component VIDEO;

    @Shadow @Final
    private Options options;

    private OptsBtn(Component title) {
        super(title);
    }

    @Inject(method = "openScreenButton", at = @At("HEAD"), cancellable = true)
    private void videoButton(Component label, Supplier<Screen> vanilla, CallbackInfoReturnable<Button> cir) {
        if (label != VIDEO) return;

        cir.setReturnValue(Button.builder(label, btn -> {
            Minecraft mc = this.minecraft;
            if (mc != null) {
                //? if >=26.2-snapshot-8 {
                /*mc.gui.setScreen(new VideoPage((Screen)(Object)this, mc, this.options));
                *///?} else
                mc.setScreen(new VideoPage((Screen)(Object)this, mc, this.options));
            }
        }).build());
    }
}
