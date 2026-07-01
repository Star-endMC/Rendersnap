package rendersnap.star.end.client;

import com.mojang.blaze3d.platform.Monitor;
import com.mojang.blaze3d.platform.VideoMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SectionOcclusionGraph;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class McCompat {
    private McCompat() {
    }

    public static ToastManager toastManager(Minecraft mc) {
        Object value = call(mc, "getToastManager");
        if (value instanceof ToastManager toast) return toast;
        Object gui = field(mc, "gui");
        value = call(gui, "toastManager");
        return value instanceof ToastManager toast ? toast : null;
    }

    public static void setScreen(Minecraft mc, Screen screen) {
        if (invoke(mc, "setScreenAndShow", screen)) return;
        if (invoke(mc, "setScreen", screen)) return;
        Object gui = field(mc, "gui");
        invoke(gui, "setScreen", screen);
    }

    public static boolean hasScreen(Minecraft mc) {
        Object screen = field(mc, "screen");
        if (screen != null) return true;
        Object gui = field(mc, "gui");
        return call(gui, "screen") != null;
    }

    public static String versionType(Minecraft mc) {
        Object value = call(mc, "getVersionType");
        return value != null ? String.valueOf(value) : mc.getLaunchedVersion();
    }

    public static boolean isSingleplayer(Minecraft mc) {
        Object value = call(mc, "isSingleplayer");
        if (value instanceof Boolean flag) return flag;
        value = call(mc, "hasSingleplayerServer");
        return value instanceof Boolean flag && flag;
    }

    public static int monitorIndexOfMode(Monitor monitor, VideoMode mode) {
        Object value = call(monitor, "getVideoModeIndex", mode);
        if (value instanceof Integer number) return number;
        value = call(monitor, "indexOfMode", mode);
        return value instanceof Integer number ? number : -1;
    }

    public static int monitorModeCount(Monitor monitor) {
        Object value = call(monitor, "getModeCount");
        if (value instanceof Integer number) return number;
        value = call(monitor, "modeCount");
        return value instanceof Integer number ? number : 0;
    }

    public static VideoMode monitorMode(Monitor monitor, int index) {
        Object value = call(monitor, "getMode", index);
        if (value instanceof VideoMode mode) return mode;
        value = call(monitor, "mode", index);
        return value instanceof VideoMode mode ? mode : null;
    }

    public static boolean sectionDirty(SectionRenderDispatcher.RenderSection section) {
        Object value = call(section, "isDirty");
        return value instanceof Boolean flag && flag;
    }

    public static boolean sectionDirtyFromPlayer(SectionRenderDispatcher.RenderSection section) {
        Object value = call(section, "isDirtyFromPlayer");
        return value instanceof Boolean flag && flag;
    }

    public static boolean sectionHasAllNeighbors(SectionRenderDispatcher.RenderSection section) {
        Object value = call(section, "hasAllNeighbors");
        return !(value instanceof Boolean flag) || flag;
    }

    public static void sectionCompileAsync(SectionRenderDispatcher.RenderSection section, SectionRenderDispatcher dispatcher, Object cache) {
        if (invoke(section, "compileAsync", cache)) return;
        if (invoke(section, "rebuildSectionAsync", cache)) return;
        invoke(section, "rebuildSectionAsync", dispatcher, cache);
    }

    public static void sectionCompileSync(SectionRenderDispatcher.RenderSection section, SectionRenderDispatcher dispatcher, Object cache) {
        if (invoke(section, "compileSync", cache)) return;
        if (invoke(section, "rebuildSectionAsync", cache)) return;
        invoke(section, "rebuildSectionAsync", dispatcher, cache);
    }

    public static void sectionSetDirty(SectionRenderDispatcher.RenderSection section, boolean fromPlayer) {
        invoke(section, "setDirty", fromPlayer);
    }

    public static void sectionSetNotDirty(SectionRenderDispatcher.RenderSection section) {
        invoke(section, "setNotDirty");
    }

    @SuppressWarnings("unchecked")
    public static it.unimi.dsi.fastutil.objects.ObjectArrayList<SectionRenderDispatcher.RenderSection> visibleSections(LevelRenderer renderer) {
        Object value = call(renderer, "getVisibleSections");
        if (value instanceof it.unimi.dsi.fastutil.objects.ObjectArrayList<?> list) {
            return (it.unimi.dsi.fastutil.objects.ObjectArrayList<SectionRenderDispatcher.RenderSection>) list;
        }
        value = call(renderer, "visibleSections");
        if (value instanceof it.unimi.dsi.fastutil.objects.ObjectArrayList<?> list) {
            return (it.unimi.dsi.fastutil.objects.ObjectArrayList<SectionRenderDispatcher.RenderSection>) list;
        }
        return new it.unimi.dsi.fastutil.objects.ObjectArrayList<>();
    }

    public static SectionRenderDispatcher sectionDispatcher(LevelRenderer renderer) {
        Object value = call(renderer, "getSectionRenderDispatcher");
        if (value instanceof SectionRenderDispatcher dispatcher) return dispatcher;
        value = call(renderer, "sectionRenderDispatcher");
        return value instanceof SectionRenderDispatcher dispatcher ? dispatcher : null;
    }

    public static SectionOcclusionGraph sectionOcclusionGraph(LevelRenderer renderer) {
        Object value = call(renderer, "getSectionOcclusionGraph");
        if (value instanceof SectionOcclusionGraph graph) return graph;
        value = call(renderer, "sectionOcclusionGraph");
        return value instanceof SectionOcclusionGraph graph ? graph : null;
    }

    public static Object cloudRenderer(LevelRenderer renderer) {
        Object value = call(renderer, "getCloudRenderer");
        return value != null ? value : call(renderer, "cloudRenderer");
    }

    public static Object invokeValue(Object target, String... names) {
        for (String name : names) {
            Object value = call(target, name);
            if (value != null) return value;
        }
        return null;
    }

    public static void resetLevelRenderer(Minecraft mc) {
        if (mc == null || mc.levelRenderer == null) return;
        if (invoke(mc.levelRenderer, "allChanged")) return;
        invoke(mc.levelRenderer, "resetSampler");
    }

    private static boolean invoke(Object target, String name, Object... args) {
        return call(target, name, args) != null;
    }

    private static Object call(Object target, String name, Object... args) {
        if (target == null) return null;
        Method[] methods = target.getClass().getMethods();
        for (Method method : methods) {
            if (!method.getName().equals(name) || method.getParameterCount() != args.length) continue;
            Class<?>[] types = method.getParameterTypes();
            boolean ok = true;
            for (int i = 0; i < types.length; i++) {
                if (!fits(types[i], args[i])) {
                    ok = false;
                    break;
                }
            }
            if (!ok) continue;
            try {
                return method.invoke(target, args);
            } catch (ReflectiveOperationException ignored) {
                return null;
            }
        }
        return null;
    }

    private static Object field(Object target, String name) {
        if (target == null) return null;
        try {
            Field field = target.getClass().getField(name);
            return field.get(target);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static boolean fits(Class<?> type, Object arg) {
        if (arg == null) return !type.isPrimitive();
        if (!type.isPrimitive()) return type.isInstance(arg);
        return (type == boolean.class && arg instanceof Boolean)
                || (type == int.class && arg instanceof Integer)
                || (type == long.class && arg instanceof Long)
                || (type == double.class && arg instanceof Double)
                || (type == float.class && arg instanceof Float);
    }
}
