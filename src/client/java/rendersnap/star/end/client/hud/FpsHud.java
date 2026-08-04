package rendersnap.star.end.client.hud;

import rendersnap.star.end.client.cfg.Opts;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
//? if >=26.1.2 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} else {
/*import net.minecraft.client.gui.GuiGraphics;
*///?}

public final class FpsHud {
    private FpsHud() {}

    //? if >=26.1.2 {
    public static void draw(GuiGraphicsExtractor g, DeltaTracker tick) {
        draw0(g, tick);
    }
    //?} else {
    /*public static void draw(GuiGraphics g, DeltaTracker tick) {
        draw0(g, tick);
    }
    *///?}

    //? if >=26.1.2 {
    private static void draw0(GuiGraphicsExtractor g, DeltaTracker tick) {
    //?} else {
    /*private static void draw0(GuiGraphics g, DeltaTracker tick) {
    *///?}
        if (!Opts.showFpsOverlay) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.getDebugOverlay().showDebugScreen()) return;

        int fps = mc.getFps();
        int x = Opts.fpsOverlayX;
        int y = Opts.fpsOverlayY;
        String txt = fps + " FPS";
        int w = mc.font.width(txt);

        g.fill(x - 3, y - 2, x + w + 4, y + 10, 0x90000000);
        //? if >=26.1.2 {
        g.text(mc.font, txt, x, y, col(fps), false);
        //?} else {
        /*g.drawString(mc.font, txt, x, y, col(fps), false);
        *///?}
    }

    private static int col(int fps) {
        if (fps <= 0) return 0xFFAAAAAA;
        if (fps < 45) return 0xFFFF5555;
        return fps < 90 ? 0xFFFFAA00 : 0xFF55FF55;
    }
}
