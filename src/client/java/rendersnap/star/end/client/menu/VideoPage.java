package rendersnap.star.end.client.menu;

import com.mojang.blaze3d.platform.Monitor;
import com.mojang.blaze3d.platform.VideoMode;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
//? if >=26.1.2 {
import net.minecraft.client.TextureFilteringMethod;
//?}
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;
import rendersnap.star.end.client.McCompat;

import java.util.Optional;

public final class VideoPage extends OptionsSubScreen {
    private static final Component TITLE = Component.translatable("rendersnap.screen.video");
    private static final Component DISPLAY = Component.translatable("options.video.display.header");
    private static final Component QUALITY = Component.translatable("options.video.quality.header");
    private static final Component PREFS = Component.translatable("options.video.preferences.header");
    private static final Component SNAP = Component.translatable("rendersnap.group.snap");

    private final int mips0;
    //? if >=26.1.2 {
    private final int aniso0;
    private final TextureFilteringMethod filter0;
    //?}

    public VideoPage(Screen last, Minecraft mc, Options opts) {
        super(last, opts, TITLE);
        this.mips0 = opts.mipmapLevels().get();
        //? if >=26.1.2 {
        this.aniso0 = opts.maxAnisotropyBit().get();
        this.filter0 = opts.textureFiltering().get();
        //?}
    }

    @Override
    protected void addOptions() {
        //? if >=26.1.2 {
        this.list.addHeader(DISPLAY);
        this.list.addBig(this.fullscreen());
        //? if >=26.1.2 {
        this.list.addSmall(new OptionInstance[]{
                this.options.framerateLimit(), this.options.enableVsync(), this.options.inactivityFpsLimit(),
                this.options.guiScale(), this.options.fullscreen(), this.options.exclusiveFullscreen(), this.options.gamma()
        });
        //?} else {
        /*this.list.addSmall(new OptionInstance[]{
                this.options.framerateLimit(), this.options.enableVsync(), this.options.inactivityFpsLimit(),
                this.options.guiScale(), this.options.fullscreen(), this.options.gamma()
        });
        *///?}

        this.list.addHeader(QUALITY);
        this.list.addBig(this.options.graphicsPreset());
        this.list.addSmall(new OptionInstance[]{
                this.options.biomeBlendRadius(), this.options.renderDistance(), this.options.prioritizeChunkUpdates(),
                this.options.simulationDistance(), this.options.ambientOcclusion(), this.options.cloudStatus(),
                this.options.particles(), this.options.mipmapLevels(), this.options.entityShadows(),
                this.options.entityDistanceScaling(), this.options.menuBackgroundBlurriness(), this.options.cloudRange(),
                this.options.cutoutLeaves(), this.options.improvedTransparency(), this.options.textureFiltering(),
                this.options.maxAnisotropyBit(), this.options.weatherRadius()
        });

        this.list.addHeader(PREFS);
        this.list.addSmall(new OptionInstance[]{
                this.options.showAutosaveIndicator(), this.options.vignette(), this.options.attackIndicator(),
                this.options.chunkSectionFadeInTime()
        });

        this.list.addHeader(SNAP);
        this.list.addSmall(
                pageButton("rendersnap.screen.optimization", SnapPage.Tab.OPTIMIZATION),
                pageButton("rendersnap.screen.effects", SnapPage.Tab.EFFECTS)
        );
        //?} else {
        /*this.list.addBig(this.fullscreen());
        this.list.addSmall(new OptionInstance[]{
                this.options.framerateLimit(), this.options.enableVsync(), this.options.guiScale(),
                this.options.fullscreen(), this.options.gamma()
        });
        this.list.addSmall(new OptionInstance[]{
                this.options.biomeBlendRadius(), this.options.renderDistance(), this.options.prioritizeChunkUpdates(),
                this.options.simulationDistance(), this.options.graphicsMode(), this.options.ambientOcclusion(),
                this.options.cloudStatus(), this.options.particles(), this.options.mipmapLevels(),
                this.options.entityShadows(), this.options.entityDistanceScaling(), this.options.menuBackgroundBlurriness()
        });
        this.list.addSmall(new OptionInstance[]{
                this.options.showAutosaveIndicator(), this.options.attackIndicator()
        });
        this.list.addSmall(
                pageButton("rendersnap.screen.optimization", SnapPage.Tab.OPTIMIZATION),
                pageButton("rendersnap.screen.effects", SnapPage.Tab.EFFECTS)
        );
        *///?}
    }

    @Override
    public void tick() {
        //? if >=26.1.2 {
        if (this.list.findOption(this.options.maxAnisotropyBit()) instanceof AbstractSliderButton slider) {
            slider.active = this.options.textureFiltering().get() == TextureFilteringMethod.ANISOTROPIC;
        }
        //?}
        super.tick();
    }

    @Override
    public void onClose() {
        this.minecraft.getWindow().changeFullscreenVideoMode();
        super.onClose();
    }

    @Override
    public void removed() {
        //? if >=26.1.2 {
        if (this.options.mipmapLevels().get() != this.mips0
                || this.options.maxAnisotropyBit().get() != this.aniso0
                || this.options.textureFiltering().get() != this.filter0) {
            this.minecraft.updateMaxMipLevel(this.options.mipmapLevels().get());
            this.minecraft.delayTextureReload();
        }
        //?} else {
        /*if (this.options.mipmapLevels().get() != this.mips0) {
            this.minecraft.updateMaxMipLevel(this.options.mipmapLevels().get());
            this.minecraft.delayTextureReload();
        }
        *///?}
        super.removed();
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        //? if >=26.1.2 {
        if (!this.minecraft.hasControlDown()) {
        //?} else {
        /*if (!Screen.hasControlDown()) {
        *///?}
            return super.mouseScrolled(x, y, scrollX, scrollY);
        }

        OptionInstance<Integer> gui = this.options.guiScale();
        if (gui.values() instanceof OptionInstance.ClampingLazyMaxIntRange range) {
            int old = gui.get();
            int next = (old == 0 ? range.maxInclusive() + 1 : old) + (int) Math.signum(scrollY);
            if (next == 0) return false;
            if (next > range.maxInclusive()) return false;
            if (next < range.minInclusive()) return false;

            AbstractWidget widget = this.list.findOption(gui);
            if (!(widget instanceof CycleButton<?> cycle)) return false;

            gui.set(next);
            ((CycleButton<Integer>) cycle).setValue(next);
            this.list.setScrollAmount(0.0);
            return true;
        }
        return false;
    }

    private OptionInstance<Integer> fullscreen() {
        Window win = this.minecraft.getWindow();
        Monitor monitor = win.findBestMonitor();
        int current = monitor == null ? -1 : win.getPreferredFullscreenVideoMode().map(mode -> McCompat.monitorIndexOfMode(monitor, mode)).orElse(-1);

        return new OptionInstance<>(
                "options.fullscreen.resolution",
                OptionInstance.noTooltip(),
                (caption, mode) -> {
                    if (monitor == null) return Component.translatable("options.fullscreen.unavailable");
                    if (mode == -1) return Options.genericValueLabel(caption, Component.translatable("options.fullscreen.current"));
                    VideoMode vm = McCompat.monitorMode(monitor, mode);
                    if (vm == null) return Component.translatable("options.fullscreen.unavailable");
                    return Options.genericValueLabel(caption, Component.translatable("options.fullscreen.entry",
                            vm.getWidth(), vm.getHeight(), vm.getRefreshRate(), vm.getRedBits() + vm.getGreenBits() + vm.getBlueBits()));
                },
                new OptionInstance.IntRange(-1, monitor != null ? McCompat.monitorModeCount(monitor) - 1 : -1),
                current,
                mode -> {
                    if (monitor != null) {
                        VideoMode selected = McCompat.monitorMode(monitor, mode);
                        win.setPreferredFullscreenVideoMode(mode == -1 || selected == null ? Optional.empty() : Optional.of(selected));
                    }
                }
        );
    }

    private Button pageButton(String key, SnapPage.Tab tab) {
        return Button.builder(
                        Component.translatable(key),
                        button -> open(new SnapPage(this, this.minecraft, this.options, tab))
                )
                .bounds(0, 0, 150, 20)
                .build();
    }

    private void open(Screen screen) {
        McCompat.setScreen(this.minecraft, screen);
    }
}
