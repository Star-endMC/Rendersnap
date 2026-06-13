package rendersnap.star.end.client;

import rendersnap.star.end.Rendersnap;
import rendersnap.star.end.client.cfg.Opts;
import rendersnap.star.end.client.hud.FpsHud;
import rendersnap.star.end.client.render.Cuts;
import rendersnap.star.end.client.render.gpu.Gpu;
import rendersnap.star.end.client.render.zoom.Zoom;
import net.fabricmc.api.ClientModInitializer;
//? if >=26.1.2 {
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.resources.Identifier;
//?}
import net.fabricmc.loader.api.FabricLoader;

public final class RendersnapClient implements ClientModInitializer {
    //? if >=26.1.2 {
    private static final Identifier FPS_HUD =
            Identifier.fromNamespaceAndPath(Rendersnap.MOD_ID, "fps_overlay");
    //?}

    @Override
    public void onInitializeClient() {
        Opts.load();
        QuantifiedSupport.registerMod();
        readOptions();
        //? if >=26.1.2 {
        Zoom.registerKeybind();
        RenderReport.registerKeybind();
        hud();
        clientStart();
        ticks();
        //?}
    }

    //? if >=26.1.2 {
    private static void hud() {
        HudElementRegistry.attachElementAfter(
                VanillaHudElements.MISC_OVERLAYS,
                FPS_HUD,
                FpsHud::draw
        );
    }

    private static void clientStart() {
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            readOptions();
            ChunkThreads.sync(client.options);
            client.execute(Gpu::reportActiveAdapter);

            FabricLoader mods = FabricLoader.getInstance();
            if (mods.isModLoaded("iris") || mods.isModLoaded("canvas") || mods.isModLoaded("sodium")) {
                Rendersnap.LOGGER.warn("Renderer mod detected. If chunks flicker, try disabling Section Occlusion or Fog Culling.");
            }
        });
    }

    private static void ticks() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            WorldWarmup.tick(client);
            Zoom.onClientTick(client);
            RenderReport.onClientTick(client);
        });
    }
    //?}

    private static void readOptions() {
        Zoom.setTransition(Opts.zoomTransition);
        Cuts.setBlockFaceCulling(Opts.blockFaceCulling);
        Cuts.setTextureLod(Opts.textureLod);
        Cuts.setChunkShadeTrim(Opts.chunkShadeTrim);
        Cuts.setFarLayerTrim(Opts.farLayerTrim);
        Cuts.setEntityCulling(Opts.entityCulling);
        Cuts.setOcclusionCulling(Opts.occlusionCulling);
        Cuts.setFogOcclusion(Opts.fogOcclusion);
        Cuts.setBehindCamMode(Opts.behindCamMode);
        Cuts.setLightingChunkTrim(Opts.lightingChunkTrim);
        Cuts.setHideWeather(Opts.hideWeather);
        Cuts.setFluidOptimizer(Opts.fluidOptimizer);
    }
}
