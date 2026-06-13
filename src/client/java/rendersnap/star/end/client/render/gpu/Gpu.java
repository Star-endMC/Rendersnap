package rendersnap.star.end.client.render.gpu;

import rendersnap.star.end.Rendersnap;
import org.lwjgl.opengl.GL11;

import java.util.Locale;

public final class Gpu {

    private Gpu() {}

    public static void reportActiveAdapter() {
        String vendor = vendor();
        String renderer = renderer();
        String version = version();
        String gpu = (vendor + " " + renderer).toLowerCase(Locale.ROOT);

        if (vendor.isBlank() && renderer.isBlank()) {
            Rendersnap.LOGGER.warn("Couldn't read the active OpenGL adapter.");
            return;
        }

        if (gpu.contains("llvmpipe")
                || gpu.contains("swiftshader")
                || gpu.contains("software")
                || gpu.contains("microsoft basic render")
                || gpu.contains("mesa offscreen")) {
            Rendersnap.LOGGER.warn("Minecraft is using a software renderer: {} {}. Set javaw.exe to the high-performance GPU.", vendor, renderer);
            return;
        }

        if (gpu.contains("intel")
                || gpu.contains("uhd graphics")
                || gpu.contains("iris")
                || gpu.contains("vega")
                || gpu.contains("radeon graphics")
                || gpu.contains("amd radeon(tm) graphics")) {
            Rendersnap.LOGGER.warn("Minecraft looks like it is on integrated graphics: {} {}. Pick the dedicated GPU for javaw.exe.", vendor, renderer);
            return;
        }

        Rendersnap.LOGGER.info("Active graphics adapter: {} {} ({})", vendor, renderer, version);
    }

    public static String vendor() {
        return glString(GL11.GL_VENDOR);
    }

    public static String renderer() {
        return glString(GL11.GL_RENDERER);
    }

    public static String version() {
        return glString(GL11.GL_VERSION);
    }

    private static String glString(int name) {
        try {
            String raw = GL11.glGetString(name);
            return raw == null ? "" : raw.trim();
        } catch (IllegalStateException e) {
            Rendersnap.LOGGER.warn("OpenGL adapter query failed for {}", name, e);
            return "";
        }
    }
}
