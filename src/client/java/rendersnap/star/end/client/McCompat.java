package rendersnap.star.end.client;

import com.mojang.blaze3d.platform.Monitor;
import com.mojang.blaze3d.platform.VideoMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.gui.screens.Screen;

public final class McCompat {
    private McCompat() {
    }

    public static ToastManager toastManager(Minecraft mc) {
        return mc.gui.toastManager();
    }

    public static void setScreen(Minecraft mc, Screen screen) {
        mc.setScreenAndShow(screen);
    }

    public static boolean hasScreen(Minecraft mc) {
        return mc.gui.screen() != null;
    }

    public static int monitorIndexOfMode(Monitor monitor, VideoMode mode) {
        return monitor.indexOfMode(mode);
    }

    public static int monitorModeCount(Monitor monitor) {
        return monitor.modeCount();
    }

    public static VideoMode monitorMode(Monitor monitor, int index) {
        return monitor.mode(index);
    }
}
