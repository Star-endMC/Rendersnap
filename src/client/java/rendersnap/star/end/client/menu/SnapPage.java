package rendersnap.star.end.client.menu;

import com.mojang.serialization.Codec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;
import rendersnap.star.end.client.ChunkUpdatePacing;
import rendersnap.star.end.client.Compatibility;
import rendersnap.star.end.client.McCompat;
import rendersnap.star.end.client.PerformancePreset;
import rendersnap.star.end.client.cfg.Opts;
import rendersnap.star.end.client.render.Cuts;
import rendersnap.star.end.client.render.zoom.Zoom;

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
            return;
        }
        effects();
    }

    private void optimization() {
        this.list.addHeader(Component.translatable("rendersnap.group.preset"));
        this.list.addBig(Button.builder(Component.translatable("rendersnap.action.apply_balanced"), button -> applyBalanced())
                .tooltip(Tooltip.create(Component.translatable("rendersnap.action.apply_balanced.tooltip")))
                .bounds(0, 0, 310, 20)
                .build());

        this.list.addHeader(Component.translatable("rendersnap.group.culling"));
        this.list.addBig(bool("rendersnap.option.entities", Opts.entityCulling, on -> {
            Opts.entityCulling = on;
            Cuts.setEntityCulling(on);
            Opts.save();
        }));
        this.list.addBig(bool("rendersnap.option.shadow_reuse", Opts.shadowReuse, on -> {
            Opts.shadowReuse = on;
            Opts.save();
        }));

        this.list.addHeader(Component.translatable("rendersnap.group.chunks"));
        this.list.addBig(presetDistanceCap());
        this.list.addBig(bool("rendersnap.option.fast_launch", Opts.fastLaunch, on -> {
            Opts.fastLaunch = on;
            if (!on) Cuts.stopWarmup();
            Opts.save();
        }));
        this.list.addBig(bool("rendersnap.option.chunk_pacing", Opts.chunkPacing, on -> {
            Opts.chunkPacing = on;
            if (on) ChunkUpdatePacing.enable(this.options);
            else ChunkUpdatePacing.disable(this.options);
            Opts.save();
        }));
        this.list.addBig(chunkWorkers());

        this.list.addHeader(Component.translatable("rendersnap.group.tradeoffs"));
        this.list.addBig(bool("rendersnap.option.chunk_shade_trim", Opts.chunkShadeTrim, on -> {
            Opts.chunkShadeTrim = on;
            Cuts.setChunkShadeTrim(on);
            refreshChunks();
            Opts.save();
        }));
        this.list.addBig(bool("rendersnap.option.far_layer_trim", Opts.farLayerTrim, on -> {
            Opts.farLayerTrim = on;
            Cuts.setFarLayerTrim(on);
            refreshChunks();
            Opts.save();
        }));
        this.list.addBig(bool("rendersnap.option.fog_occlusion", Opts.fogOcclusion, on -> {
            Opts.fogOcclusion = on;
            Cuts.setFogOcclusion(on);
            refreshChunks();
            Opts.save();
        }));
        this.list.addBig(bool("rendersnap.option.weather", Opts.hideWeather, on -> {
            Opts.hideWeather = on;
            Cuts.setHideWeather(on);
            Opts.save();
        }));

        this.list.addHeader(Component.translatable("rendersnap.group.advanced"));
        this.list.addBig(behindCam());
    }

    private void effects() {
        this.list.addHeader(Component.translatable("rendersnap.group.interface"));
        this.list.addBig(bool("rendersnap.option.fps", Opts.showFpsOverlay, on -> {
            Opts.showFpsOverlay = on;
            Opts.save();
        }));
        this.list.addBig(zoomEase());
    }

    private void applyBalanced() {
        int changed = PerformancePreset.apply(this.options);
        Opts.save();
        SystemToast.addOrUpdate(
                McCompat.toastManager(this.minecraft),
                RELOAD_TOAST,
                Component.translatable("rendersnap.toast.preset.title"),
                Component.translatable("rendersnap.toast.preset.body", changed)
        );
    }

    private OptionInstance<Integer> behindCam() {
        return new OptionInstance<>(
                "rendersnap.option.behind_cam",
                tip("rendersnap.option.behind_cam.tooltip"),
                (caption, mode) -> Component.translatable("rendersnap.option.behind_cam.value", Component.translatable("rendersnap.option.behind_cam." + mode)),
                new OptionInstance.IntRange(Opts.BEHIND_CAM_OFF, Opts.BEHIND_CAM_HIGH),
                Opts.behindCamMode,
                mode -> {
                    Opts.behindCamMode = mode;
                    Cuts.setBehindCamMode(mode);
                    Opts.save();
                }
        );
    }

    private OptionInstance<Integer> presetDistanceCap() {
        return new OptionInstance<>(
                "rendersnap.option.preset_distance_cap",
                tip("rendersnap.option.preset_distance_cap.tooltip"),
                (caption, value) -> value == 0
                        ? Component.translatable("rendersnap.option.preset_distance_cap.off")
                        : Component.translatable("rendersnap.option.preset_distance_cap.value", value),
                new OptionInstance.IntRange(0, 32),
                Opts.graphicsPresetDistanceCap,
                value -> {
                    Opts.graphicsPresetDistanceCap = value;
                    Opts.save();
                }
        );
    }

    private OptionInstance<Integer> chunkWorkers() {
        return new OptionInstance<>(
                "rendersnap.option.chunk_workers",
                tip("rendersnap.option.chunk_workers.tooltip"),
                (caption, value) -> Component.translatable("rendersnap.option.chunk_workers.value", value),
                new OptionInstance.IntRange(2, 8),
                Opts.chunkWorkerLimit,
                value -> {
                    Opts.chunkWorkerLimit = value;
                    Opts.save();
                    restartToast();
                }
        );
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
        return OptionInstance.createBoolean(key, tip(key + ".tooltip"), current, changed::accept);
    }

    private void refreshChunks() {
        SystemToast.addOrUpdate(
                McCompat.toastManager(this.minecraft),
                RELOAD_TOAST,
                Component.translatable("rendersnap.toast.reload.title"),
                Component.translatable("rendersnap.toast.reload.body")
        );
    }

    private void restartToast() {
        SystemToast.addOrUpdate(
                McCompat.toastManager(this.minecraft),
                RELOAD_TOAST,
                Component.translatable("rendersnap.toast.restart.title"),
                Component.translatable("rendersnap.toast.restart.body")
        );
    }

    private static <T> OptionInstance.TooltipSupplier<T> tip(String key) {
        String text = Component.translatable(key).getString() + "\n\n" + Compatibility.rendererStatus();
        return OptionInstance.cachedConstantTooltip(Component.literal(text));
    }
}
