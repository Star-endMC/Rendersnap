package rendersnap.star.end.client.menu;

import com.mojang.serialization.Codec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;
import rendersnap.star.end.client.ChunkThreads;
import rendersnap.star.end.client.cfg.Opts;
import rendersnap.star.end.client.render.Cuts;
import rendersnap.star.end.client.render.zoom.Zoom;

import java.util.List;
import java.util.function.Consumer;

public final class SnapPage extends OptionsSubScreen {
    private static final SystemToast.SystemToastId RELOAD_TOAST = new SystemToast.SystemToastId();

    public enum Tab {
        OPTIMIZATION,
        EFFECTS
    }

    private final Tab tab;

    public SnapPage(Screen last, Minecraft mc, Options opts, Tab tab) {
        super(last, opts, Component.translatable(tab == Tab.OPTIMIZATION ? "rendersnap.screen.optimization" : "rendersnap.screen.effects"));
        this.tab = tab;
    }

    @Override
    protected void addOptions() {
        if (this.tab == Tab.OPTIMIZATION) {
            optimization();
        } else {
            effects();
        }
    }

    private void optimization() {
        this.list.addBig(bool("rendersnap.option.fast_launch", Opts.fastLaunch, on -> {
            Opts.fastLaunch = on;
            if (!on) {
                Cuts.stopWarmup();
            }
            Opts.save();
        }));
        this.list.addBig(bool("rendersnap.option.multi_render", Opts.multiRender, on -> {
            Opts.multiRender = on;
            if (on) {
                ChunkThreads.on(this.options);
            } else {
                ChunkThreads.off(this.options);
            }
            Opts.save();
        }));
        this.list.addBig(bool("rendersnap.option.occlusion", Opts.occlusionCulling, on -> {
            Opts.occlusionCulling = on;
            Cuts.setOcclusionCulling(on);
            Opts.save();
        }));
        this.list.addBig(bool("rendersnap.option.entities", Opts.entityCulling, on -> {
            Opts.entityCulling = on;
            Cuts.setEntityCulling(on);
            Opts.save();
        }));
        this.list.addBig(behindCam());
        this.list.addBig(bool("rendersnap.option.faces", Opts.blockFaceCulling, on -> {
            Opts.blockFaceCulling = on;
            Cuts.setBlockFaceCulling(on);
            staleChunks();
            Opts.save();
        }));
        this.list.addBig(bool("rendersnap.option.texture_lod", Opts.textureLod, on -> {
            Opts.textureLod = on;
            Cuts.setTextureLod(on);
            Opts.save();
        }));
        this.list.addBig(bool("rendersnap.option.light_cache", Opts.lightingChunkTrim, on -> {
            Opts.lightingChunkTrim = on;
            Cuts.setLightingChunkTrim(on);
            staleChunks();
            Opts.save();
        }));
    }

    private OptionInstance<Integer> behindCam() {
        return new OptionInstance<>(
                "rendersnap.option.behind_cam",
                tip("rendersnap.option.behind_cam.tooltip"),
                (caption, mode) -> Component.translatable("rendersnap.option.behind_cam." + mode),
                new OptionInstance.Enum<>(List.of(Opts.BEHIND_CAM_OFF, Opts.BEHIND_CAM_NORMAL, Opts.BEHIND_CAM_HIGH), Codec.INT),
                Opts.behindCamMode,
                mode -> {
                    Opts.behindCamMode = mode;
                    Cuts.setBehindCamMode(mode);
                    Opts.save();
                }
        );
    }

    private void effects() {
        this.list.addBig(bool("rendersnap.option.fps", Opts.showFpsOverlay, on -> {
            Opts.showFpsOverlay = on;
            Opts.save();
        }));
        this.list.addBig(zoomEase());
        this.list.addBig(bool("rendersnap.option.fog_occlusion", Opts.fogOcclusion, on -> {
            Opts.fogOcclusion = on;
            Cuts.setFogOcclusion(on);
            Opts.save();
        }));
        this.list.addBig(bool("rendersnap.option.fluid", Opts.fluidOptimizer, on -> {
            Opts.fluidOptimizer = on;
            Cuts.setFluidOptimizer(on);
            if (!on) staleChunks();
            Opts.save();
        }));
        this.list.addBig(bool("rendersnap.option.weather", Opts.hideWeather, on -> {
            Opts.hideWeather = on;
            Cuts.setHideWeather(on);
            Opts.save();
        }));
        this.list.addBig(bool("rendersnap.option.sun", Opts.hideSun, on -> {
            Opts.hideSun = on;
            Cuts.setHideSun(on);
            Opts.save();
        }));
        this.list.addBig(bool("rendersnap.option.moon", Opts.hideMoon, on -> {
            Opts.hideMoon = on;
            Cuts.setHideMoon(on);
            Opts.save();
        }));
    }

    private OptionInstance<Integer> zoomEase() {
        return new OptionInstance<>(
                "rendersnap.option.zoom_ease",
                tip("rendersnap.option.zoom_ease.tooltip"),
                (caption, mode) -> Component.literal(Zoom.transitionLabel(mode)),
                new OptionInstance.Enum<>(Zoom.EASES, Codec.INT),
                Opts.zoomTransition,
                mode -> {
                    Opts.zoomTransition = mode;
                    Zoom.setTransition(mode);
                    Opts.save();
                }
        );
    }

    private static OptionInstance<Boolean> bool(String key, boolean current, Consumer<Boolean> changed) {
        //? if >=26.2-snapshot-8 {
        return OptionInstance.createBoolean(key, tip(key + ".tooltip"), current, changed::accept);
        //?} else
        //return OptionInstance.createBoolean(key, tip(key + ".tooltip"), current, changed);
    }

    private void staleChunks() {
        SystemToast.addOrUpdate(
                /*? if >=26.2-snapshot-8 {*/ this.minecraft.gui.toastManager() /*?} else if >=26.1.2 {*//*this.minecraft.getToastManager()*//*?} else {*//*this.minecraft.getToasts()*//*?}*/,
                RELOAD_TOAST,
                Component.translatable("rendersnap.toast.reload.title"),
                Component.translatable("rendersnap.toast.reload.body")
        );
    }

    private static <T> OptionInstance.TooltipSupplier<T> tip(String key) {
        return OptionInstance.cachedConstantTooltip(Component.translatable(key));
    }
}
