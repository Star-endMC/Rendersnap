package rendersnap.star.end.client;

import rendersnap.star.end.Rendersnap;
import rendersnap.star.end.client.cfg.Opts;
import rendersnap.star.end.client.hud.FpsUI;
import rendersnap.star.end.client.render.Cuts;
import rendersnap.star.end.client.render.gpu.Gpu;
import rendersnap.star.end.client.render.zoom.Zoom;
import net.fabricmc.api.ClientModInitializer;
//? if >=26.1 {
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.resources.Identifier;
//?}

public final class SnapClient implements ClientModInitializer {
    //? if >=26.1 {
    private static final Identifier FPS_HUD =
            Identifier.fromNamespaceAndPath(Rendersnap.MOD_ID, "fps_overlay");
    //?}

    @Override
    public void onInitializeClient() {
        Opts.load();
        Compat.initialize();
        if (Compat.disableConflictingOptions()) {
            Opts.save();
        }
        readOptions();
        //? if >=26.1 {
        Zoom.registerKeybind();
        RenderRpt.registerKeybind();
        Bench.registerKeybind();
        hud();
        clientStart();
        ticks();
        //?}
    }

    //? if >=26.1 {
    private static void hud() {
        HudElementRegistry.attachElementAfter(
                VanillaHudElements.MISC_OVERLAYS,
                FPS_HUD,
                FpsUI::draw
        );
    }

    private static void clientStart() {
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            readOptions();
            ChunkPace.sync(client.options);
            client.execute(Gpu::reportActiveAdapter);

            Rendersnap.LOGGER.info("Rendersnap compatibility: {}", Compat.rendererStatus());
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(Zoom::release);
    }

    private static void ticks() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            WorldWU.tick(client);
            ChunkO.pace(FrameMet.latestNs());
            Zoom.onClientTick(client);
            RenderRpt.onClientTick(client);
            Bench.tick(client);
        });
    }
    //?}

    private static void readOptions() {

        Zoom.setTransition(Opts.zoomTransition);
        Cuts.setChunkShadeTrim(Opts.chunkShadeTrim);
        Cuts.setEntityCulling(Opts.entityCulling);
        Cuts.setFogOcclusion(Opts.fogOcclusion);
        Cuts.setCaveCulling(Opts.caveCulling);
        Cuts.setBehindCamMode(Opts.behindCamMode);
        Cuts.setHideWeather(Opts.hideWeather);
    }
}
