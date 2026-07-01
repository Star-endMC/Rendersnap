package rendersnap.star.end.client.render;

//? if >=26.1.2 {
import net.minecraft.client.renderer.fog.FogData;
//?}

public final class Fog {

    private Fog() {}

    private static boolean occlusionEnabled = false;
    private static float occlusionDistance = 176.0f;

    public static void setOcclusionEnabled(boolean on) {
        occlusionEnabled = on;
    }

    //? if >=26.1.2 {
    public static void readVanillaFog(FogData fogData) {
        if (!occlusionEnabled || fogData == null) {
            return;
        }

        float end = Math.max(64.0f, fogData.renderDistanceEnd);
        occlusionDistance = end + 24.0f;
    }
    //?} else {
    /*public static void readVanillaFog(Object fogData) {
    }
    *///?}

    public static boolean isOccludingDistance(float distance) {
        if (!occlusionEnabled) {
            return false;
        }
        return distance >= occlusionDistance;
    }
}
