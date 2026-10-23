package rendersnap.star.end.client.menu;

import com.mojang.serialization.Codec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import rendersnap.star.end.client.ChunkPace;
import rendersnap.star.end.client.Compat;
import rendersnap.star.end.client.McCompat;
import rendersnap.star.end.client.cfg.Opts;
import rendersnap.star.end.client.render.Cuts;
import rendersnap.star.end.client.render.zoom.Zoom;

import java.util.function.Consumer;

public final class SnapPg extends OptionsSubScreen {
    private static final SystemToast.SystemToastId RELOAD_TOAST = new SystemToast.SystemToastId();

    public enum Tab {
        OPTIMIZATION,
        EFFECTS
    }

    private final Tab tab;

    public SnapPg(Screen last, Minecraft mc, Options opts, Tab tab) {
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
        this.list.addBig(bool("rendersnap.option.fast_launch", Opts.fastLaunch, on -> {
            Opts.fastLaunch = on;
            if (!on) Cuts.stopWarmup();
            Opts.save();
        }));
        this.list.addBig(bool("rendersnap.option.chunk_pacing", Opts.chunkPacing, on -> {
            Opts.chunkPacing = on;
            if (on) ChunkPace.enable(this.options);
            else ChunkPace.disable(this.options);
            Opts.save();
        }));
        this.list.addBig(chunkWorkers());
        this.list.addBig(bool("rendersnap.option.gentle_workers", Opts.gentleWorkers, on -> {
            Opts.gentleWorkers = on;
            Opts.save();
            restartToast();
        }));
        this.list.addBig(bool("rendersnap.option.rebuild_guard", Opts.rebuildGuard, on -> {
            Opts.rebuildGuard = on;
            Opts.save();
        }));
        this.list.addBig(bool("rendersnap.option.light_pacing", Opts.lightPacing, on -> {
            Opts.lightPacing = on;
            Opts.save();
        }));

        this.list.addHeader(Component.translatable("rendersnap.group.tradeoffs"));
        this.list.addBig(bool("rendersnap.option.chunk_shade_trim", Opts.chunkShadeTrim, on -> {
            Opts.chunkShadeTrim = on;
            Cuts.setChunkShadeTrim(on);
            refreshChunks();
            Opts.save();
        }));
        this.list.addBig(bool("rendersnap.option.fog_occlusion", Opts.fogOcclusion, on -> {
            Opts.fogOcclusion = on;
            Cuts.setFogOcclusion(on);
            refreshChunks();
            Opts.save();
        }));
        this.list.addBig(bool("rendersnap.option.cave_culling", Opts.caveCulling, on -> {
            Opts.caveCulling = on;
            Cuts.setCaveCulling(on);
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
        this.list.addBig(bool("rendersnap.option.liquid_fix", Opts.liquidFix, on -> {
            Opts.liquidFix = on;
            Opts.save();
        }));
        this.list.addBig(bool("rendersnap.option.shadow_fix", Opts.shadowFix, on -> {
            Opts.shadowFix = on;
            Opts.save();
        }));
        this.list.addBig(zoomEase());
        this.list.addBig(bool("rendersnap.option.zoom_cinematic", Opts.zoomCinematic, on -> {
            Opts.zoomCinematic = on;
            Opts.save();
        }));
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
        ChatFormatting color;
        String impact;
        if (key.contains("fps") || key.contains("zoom")) {
            color = ChatFormatting.GREEN;
            impact = "Low";
        } else if (key.contains("entities") || key.contains("cave") || key.contains("chunk")) {
            color = ChatFormatting.RED;
            impact = "High";
        } else {
            color = ChatFormatting.YELLOW;
            impact = "Medium";
        }
        Component text = Component.empty()
                .append(Component.literal("Performance Impact: " + impact).withStyle(color))
                .append(Component.literal("\n\n"))
                .append(Component.translatable(key))
                .append(Component.literal("\n\n"))
                .append(Component.literal(Compat.rendererStatus()).withStyle(ChatFormatting.GRAY));
        return OptionInstance.cachedConstantTooltip(text);
    }
}
