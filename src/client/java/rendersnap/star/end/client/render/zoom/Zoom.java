package rendersnap.star.end.client.render.zoom;

import rendersnap.star.end.Rendersnap;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import rendersnap.star.end.client.McCompat;
import rendersnap.star.end.client.cfg.Opts;

import java.util.List;

public final class Zoom {

    private Zoom() {}

    private static final Ease[] MODES = Ease.values();

    public static final List<Integer> EASES = List.of(0, 1, 2, 3);

    private static KeyMapping key;
    private static boolean on;
    private static int steps;
    private static float baseFov = -1.0f;
    private static float progress;
    private static int transition = 2;
    private static double oldSens;
    private static boolean oldSmooth;
    private static boolean saved;

    private static boolean held;
    private static long lastFrameNs;
    private static float smoothZoomFov = -1.0f;

    public static void registerKeybind() {
        if (key != null) {
            return;
        }
        KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(Rendersnap.MOD_ID, "controls"));
        key = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.rendersnap.zoom",
                //? if >=26.3 {
                InputConstants.Type.KEYBOARD,
                //?} else {
                /*InputConstants.Type.KEYSYM,
                *///?}
                InputConstants.KEY_C,
                category
        ));
    }

    public static void setTransition(int mode) {
        transition = Mth.clamp(mode, 0, MODES.length - 1);
    }

    public static void onClientTick(Minecraft client) {
        if (client == null) return;
        if (client.options == null || key == null) return;

        boolean down = key.isDown();
        if (client.player == null) down = false;
        if (hasScreen(client)) down = false;

        if (down) {
            if (!on) {
                on = true;
                steps = 0;
                progress = 0.0f;
                baseFov = client.options.fov().get();
                oldSens = client.options.sensitivity().get();
                oldSmooth = client.options.smoothCamera;
                saved = true;
                smoothZoomFov = -1.0f;
                lastFrameNs = System.nanoTime();
            }
            held = true;
            if (transition == 0) progress = 1.0f;
            applyZoomControls(client);
            return;
        }
        held = false;
        if (on) {

            if (transition == 0 || System.nanoTime() - lastFrameNs > 250_000_000L) progress = 0.0f;
            applyZoomControls(client);
            if (progress <= 0.0f) {
                on = false;
                steps = 0;
                baseFov = -1.0f;
                smoothZoomFov = -1.0f;
                restoreControls(client);
            }
        }
    }

    public static boolean isZooming() {
        return on || progress > 0.0f;
    }

    public static void release(Minecraft client) {
        if (client == null || client.options == null) return;
        restoreControls(client);
        on = false;
        steps = 0;
        progress = 0.0f;
        baseFov = -1.0f;
        held = false;
        smoothZoomFov = -1.0f;
    }

    public static void beforeOptionsSave(Minecraft client) {
        if (!saved || client == null || client.options == null) return;
        client.options.sensitivity().set(oldSens);
        client.options.smoothCamera = oldSmooth;
    }

    public static void afterOptionsSave(Minecraft client) {
        if (!saved || client == null || client.options == null) return;
        applyZoomControls(client);
    }

    public static float modifyFov(float vanillaFov) {
        if (!isZooming()) {
            return vanillaFov;
        }
        if (baseFov < 0.0f) {
            baseFov = vanillaFov;
        }
        long now = System.nanoTime();
        float dt = lastFrameNs == 0L ? 0.0f : Math.min(0.1f, Math.max(0.0f, (now - lastFrameNs) / 1_000_000_000.0f));
        lastFrameNs = now;
        float goal = held ? 1.0f : 0.0f;
        if (transition == 0) {
            progress = goal;
        } else if (progress < goal) {
            progress = Math.min(goal, progress + dt / 0.22f);
        } else if (progress > goal) {
            progress = Math.max(goal, progress - dt / 0.22f);
        }

        float wanted = zoomFov();
        if (smoothZoomFov < 0.0f) {
            smoothZoomFov = wanted;
        } else {
            smoothZoomFov += (wanted - smoothZoomFov) * (1.0f - (float) Math.exp(-14.0f * dt));
        }
        float target = Math.min(vanillaFov, smoothZoomFov);
        float eased = ease(progress);
        return Mth.lerp(eased, vanillaFov, target);
    }

    public static boolean handleScroll(double amount) {

        if (!on || !held) {
            return false;
        }
        if (amount > 0.0) {
            steps = Math.min(11, steps + 1);
            return true;
        }
        if (amount < 0.0) {
            steps = Math.max(0, steps - 1);
            return true;
        }
        return false;
    }

    public static String transitionLabel(int mode) {
        return ease(mode).label;
    }

    private static void applyZoomControls(Minecraft client) {
        float base = Math.max(1.0f, client.options.fov().get());
        float zoom = zoomFov();
        double factor = Mth.clamp(zoom / base, 0.05f, 1.0f);

        client.options.smoothCamera = Opts.zoomCinematic || oldSmooth;
        if (saved) {

            double speed = oldSens * 0.6 + 0.2;
            double scaled = speed * Math.cbrt(factor);
            client.options.sensitivity().set(Mth.clamp((scaled - 0.2) / 0.6, 0.0, oldSens));
        }
    }

    private static void restoreControls(Minecraft client) {
        if (saved) {
            client.options.sensitivity().set(oldSens);
            client.options.smoothCamera = oldSmooth;
            saved = false;
        }
    }

    private static float zoomFov() {

        return Math.max(7.5f, 30.0f / (float) Math.pow(1.13f, steps));
    }

    private static float ease(float t) {
        t = Mth.clamp(t, 0.0f, 1.0f);
        return ease(transition).at(t);
    }

    private static Ease ease(int id) {
        return id >= 0 && id < MODES.length ? MODES[id] : Ease.SMOOTH;
    }

    private static boolean hasScreen(Minecraft client) {
        return McCompat.hasScreen(client);
    }

    private enum Ease {
        INSTANT("Instant") {
            float at(float t) { return t; }
        },
        LINEAR("Linear") {
            float at(float t) { return t; }
        },
        SMOOTH("Smooth") {
            float at(float t) { return t * t * (3.0f - 2.0f * t); }
        },
        CUBIC("Cubic") {
            float at(float t) { return 1.0f - (float)Math.pow(1.0f - t, 3.0); }
        };

        final String label;

        Ease(String label) {
            this.label = label;
        }

        abstract float at(float t);
    }
}
