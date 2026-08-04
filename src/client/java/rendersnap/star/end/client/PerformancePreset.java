package rendersnap.star.end.client;

import net.minecraft.client.Options;
import rendersnap.star.end.client.cfg.Opts;
import rendersnap.star.end.client.render.Cuts;

public final class PerformancePreset {
    private PerformancePreset() {
    }

    public static int apply(Options options) {
        int changed = 0;
        if (!Opts.entityCulling) changed++;
        if (!Opts.shadowReuse) changed++;
        if (!Opts.fastLaunch) changed++;
        if (Opts.chunkPacing) changed++;
        if (Opts.chunkWorkerLimit != Opts.DEFAULT_CHUNK_WORKER_LIMIT) changed++;
        if (Opts.graphicsPresetDistanceCap != Opts.DEFAULT_GRAPHICS_PRESET_DISTANCE_CAP) changed++;
        if (Opts.chunkShadeTrim) changed++;
        if (Opts.farLayerTrim) changed++;
        if (Opts.fogOcclusion) changed++;
        if (Opts.behindCamMode != Opts.BEHIND_CAM_OFF) changed++;
        if (Opts.hideWeather) changed++;

        Opts.entityCulling = true;
        Opts.shadowReuse = true;
        Opts.fastLaunch = true;
        Opts.chunkPacing = false;
        Opts.chunkWorkerLimit = Opts.DEFAULT_CHUNK_WORKER_LIMIT;
        Opts.graphicsPresetDistanceCap = Opts.DEFAULT_GRAPHICS_PRESET_DISTANCE_CAP;
        Opts.chunkShadeTrim = false;
        Opts.farLayerTrim = false;
        Opts.fogOcclusion = false;
        Opts.behindCamMode = Opts.BEHIND_CAM_OFF;
        Opts.hideWeather = false;
        Cuts.setEntityCulling(true);
        Cuts.setChunkShadeTrim(false);
        Cuts.setFarLayerTrim(false);
        Cuts.setFogOcclusion(false);
        Cuts.setBehindCamMode(Opts.BEHIND_CAM_OFF);
        Cuts.setHideWeather(false);
        ChunkUpdatePacing.disable(options);
        return changed;
    }
}
