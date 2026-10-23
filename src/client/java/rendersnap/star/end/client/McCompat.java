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
        //? if >=26.2 {
        return mc.gui.toastManager();
        //?} else {
        /*return mc.getToastManager();
        *///?}
    }

    public static void setScreen(Minecraft mc, Screen screen) {

        //? if >=26.2 {
        mc.gui.setScreen(screen);
        //?} else {
        /*mc.setScreen(screen);
        *///?}
    }

    public static boolean hasScreen(Minecraft mc) {
        //? if >=26.2 {
        return mc.gui.screen() != null;
        //?} else {
        /*return mc.screen != null;
        *///?}
    }

    public static int monitorIndexOfMode(Monitor monitor, VideoMode mode) {
        //? if >=26.2 {
        return monitor.indexOfMode(mode);
        //?} else {
        /*return monitor.getVideoModeIndex(mode);
        *///?}
    }

    public static int monitorModeCount(Monitor monitor) {
        //? if >=26.2 {
        return monitor.modeCount();
        //?} else {
        /*return monitor.getModeCount();
        *///?}
    }

    public static VideoMode monitorMode(Monitor monitor, int index) {
        //? if >=26.2 {
        return monitor.mode(index);
        //?} else {
        /*return monitor.getMode(index);
        *///?}
    }
}
